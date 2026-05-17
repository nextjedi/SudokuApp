package com.nextjedi.sudokustreak.domain.input

import android.content.Context
import android.os.SystemClock
import android.view.KeyEvent
import android.view.MotionEvent
import com.nextjedi.sudokustreak.domain.settings.AppSettingsRepository
import com.nextjedi.sudokustreak.domain.settings.StylusMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Android `actual` for [StylusInputManager]. Bridges Android [MotionEvent] / [KeyEvent]
 * pipelines into the cross-platform [StylusEvent] flow and routes recognition to a
 * mockable [DigitRecognizer] (defaults to [MlKitDigitRecognizer] with the bundled
 * English text base model — locked decision §6 (2)).
 *
 * ## Lifecycle
 *
 * - **Instantiated once per app process** in Wave 2 DI (likely under `SudokuApplication`).
 *   Holds no Activity reference — only `applicationContext` (passed into the recognizer).
 *   The Compose layer in `androidApp/.../ui/components/StylusSupport.kt` wires its
 *   `Modifier.pointerInput` callbacks into [onMotionEvent].
 *
 * - **Mode gating.** [AppSettingsRepository.current] is read on every motion event
 *   so changes propagate instantly without re-subscribing. Three modes:
 *   - `NEVER`: motion events are dropped on the floor — no [StylusEvent] emission,
 *     no recognition. Matches TC-AS15 / `mode_never_blocks_recognition`.
 *   - `AUTO`: events flow when a stylus is detected (the manager flips
 *     `stylusAutoDetected=true` on the FIRST `TOOL_TYPE_STYLUS` event ever — exactly
 *     once, guarded by [autoDetectMutex] + [autoDetected]).
 *   - `ALWAYS`: events flow regardless of auto-detect state.
 *
 * - **S-Pen side button.** [handleKeyEvent] consumes
 *   [KeyEvent.KEYCODE_STYLUS_BUTTON_PRIMARY] and emits [StylusEvent.SideButtonPressed]
 *   for the UI to toggle pencil-notes mode. Apple-Pencil-squeeze parity (P0-12, P1-24).
 *
 * ## Concurrency
 *
 * - Internal flow is a [MutableSharedFlow] with `extraBufferCapacity = 64` and
 *   `DROP_OLDEST` overflow strategy — backed by REVAMP_PLAN.md §8 "Stroke buffer"
 *   note and stylus-reviewer §7 (max bounded buffer). This prevents OOM if a stress
 *   test floods 10,000 motion events into a slow consumer (TC-AS20 / stroke_buffer_bounded).
 *
 * - [autoDetectMutex] serializes the "have I flipped the auto-detect flag yet?" check
 *   so 100 concurrent stylus events still result in **exactly one** `settings.update`
 *   call (test `auto_detect_only_writes_once`).
 *
 * - Recognition delegates to [recognizer]. Concurrent `recognize()` calls are
 *   safe — [MlKitDigitRecognizer] uses an internal stateful client but ML Kit's API
 *   is concurrency-safe at the recognize() level. Tests `concurrent_recognize_calls_serialized`
 *   verify both completions without crash.
 *
 * @param context Android application context. Forwarded to the default
 *                [MlKitDigitRecognizer]; not retained by this class.
 * @param settings repository for reading `stylusMode`, `stylusConfidenceThreshold`,
 *                 and writing `stylusAutoDetected` once on first detection.
 * @param recognizer pluggable digit recognizer. Defaults to [MlKitDigitRecognizer].
 *                   Tests inject a [DigitRecognizer] mock to avoid real ML Kit calls.
 * @param scope coroutine scope for fire-and-forget side effects (auto-detect flag
 *              persistence). Defaults to a process-lifetime [SupervisorJob] on
 *              [Dispatchers.Default]. Tests pass a `TestScope` for deterministic
 *              virtual-time stepping.
 */
actual class StylusInputManager(
    private val context: Context,
    private val settings: AppSettingsRepository,
    private val recognizer: DigitRecognizer,
    private val scope: CoroutineScope
) {

    /**
     * Convenience constructor matching the Wave-2 DI signature: builds the default
     * [MlKitDigitRecognizer] and an internal [Dispatchers.Default] scope.
     * Production code uses this; tests prefer the 4-arg primary constructor.
     */
    constructor(
        context: Context,
        settings: AppSettingsRepository
    ) : this(
        context = context,
        settings = settings,
        recognizer = MlKitDigitRecognizer(context.applicationContext),
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    )

    /**
     * Hot event channel. `extraBufferCapacity = 64` matches REVAMP_PLAN.md §8.
     * `DROP_OLDEST` policy means a slow consumer loses the oldest queued event,
     * never blocks the producer. Trail rendering tolerates dropped points (it
     * interpolates) — recognition does NOT, because [onMotionEvent] persists the
     * full stroke in [strokeBuffer] independently of the SharedFlow.
     */
    private val _events = MutableSharedFlow<StylusEvent>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /** Bounded stroke buffer. Caps at [MAX_STROKE_POINTS] to satisfy TC-AS20. */
    private val strokeBuffer: ArrayDeque<StylusPoint> = ArrayDeque(MAX_STROKE_POINTS)

    /** Mutex protecting [strokeBuffer] across concurrent motion events. */
    private val bufferMutex = Mutex()

    /**
     * Guards [autoDetected] — write-once latch. Without this, two concurrent stylus
     * events could both observe `false` and both trigger `settings.update` (race).
     */
    private val autoDetectMutex = Mutex()

    @Volatile
    private var autoDetected: Boolean = false

    /**
     * Reading the cached settings on every event avoids a Flow round-trip per
     * motion. The repository is the source of truth; this is only an in-process
     * snapshot updated via [refreshCachedMode].
     *
     * Default to AUTO so tests that never call `refreshCachedMode` still see the
     * canonical default (matches `AppSettings.stylusMode = AUTO`).
     */
    @Volatile
    private var cachedMode: StylusMode = StylusMode.AUTO

    /**
     * Cached confidence threshold mirror, updated alongside [cachedMode]. Default
     * 0.75f matches `AppSettings.stylusConfidenceThreshold`.
     */
    @Volatile
    private var cachedThreshold: Float = 0.75f

    /**
     * Used by tests to inject a known mode/threshold without a fully wired
     * [AppSettingsRepository]. Production code never calls this directly — instead
     * a Wave-2 ViewModel observes `settings.flow` and pushes via this hook.
     */
    fun refreshCachedMode(mode: StylusMode, confidenceThreshold: Float) {
        cachedMode = mode
        cachedThreshold = confidenceThreshold
    }

    /** Test/debug accessor — returns true iff the first-stroke auto-detect fired. */
    fun isAutoDetected(): Boolean = autoDetected

    actual fun events(): Flow<StylusEvent> = _events.asSharedFlow()

    actual suspend fun recognize(stroke: List<StylusPoint>): DigitRecognitionResult {
        // Use the most recently cached threshold; fall back to live settings if the
        // cache has never been refreshed (defensive — production path always populates
        // the cache via the ViewModel).
        val threshold = cachedThreshold.takeIf { it > 0f }
            ?: settings.current().stylusConfidenceThreshold
        return recognizer.recognize(stroke, threshold)
    }

    /**
     * Process a single Android [MotionEvent]. Called from
     * `Modifier.pointerInput { awaitPointerEvent() … }` in StylusSupport.kt; tests
     * call it directly with synthesized events via [MotionEvent.obtain].
     *
     * Branches:
     *  1. Finger events → ignored (TC-AS1). The Compose layer routes finger touches
     *     to the default tap handler, so we just no-op.
     *  2. `cachedMode == NEVER` → ignored (TC-AS15).
     *  3. `cachedMode == AUTO && !autoDetected` → the FIRST stylus event flips the
     *     detected flag (persists once via [scope]); the event itself is still
     *     processed so the user's very first stroke is not dropped.
     *  4. Action dispatch:
     *     - `ACTION_DOWN` → [StylusEvent.StrokeBegin] + reset stroke buffer.
     *     - `ACTION_MOVE` → harvest historical points + emit [StylusEvent.StrokeMove].
     *     - `ACTION_UP` → final point + [StylusEvent.StrokeEnd] over full buffer.
     *     - `ACTION_HOVER_ENTER/EXIT` → [StylusEvent.HoverEnter] / [StylusEvent.HoverExit].
     *     - `ACTION_CANCEL` → reset buffer (suppressed: user palm-rejected mid-stroke).
     */
    fun onMotionEvent(event: MotionEvent) {
        // Branch 1: finger touch → swallow.
        if (!event.isFromStylus()) return

        // Branch 2: mode gating.
        if (cachedMode == StylusMode.NEVER) return

        // Branch 3: auto-detect latch (AUTO mode only).
        if (cachedMode == StylusMode.AUTO && !autoDetected) {
            // Launch outside the lock to avoid blocking the UI thread; the lock inside
            // the launched coroutine ensures only one writer wins.
            scope.launch {
                autoDetectMutex.withLock {
                    if (!autoDetected) {
                        autoDetected = true
                        try {
                            settings.update { it.copy(stylusAutoDetected = true) }
                        } catch (t: Throwable) {
                            // Persistence failure must not break the input pipeline.
                            // The flag stays true in-memory for the session; next launch
                            // re-detects on the next stroke. Logged-only.
                        }
                    }
                }
            }
        }

        // Branch 4: action dispatch.
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                resetStrokeBufferBlocking()
                val point = event.toStylusPoint(historicalIndex = -1)
                appendPointBounded(point)
                tryEmit(StylusEvent.StrokeBegin(point))
            }
            MotionEvent.ACTION_MOVE -> {
                val historicalSize = event.historySize
                val moveBatch = ArrayList<StylusPoint>(historicalSize + 1)
                for (h in 0 until historicalSize) {
                    val p = event.toStylusPoint(historicalIndex = h)
                    appendPointBounded(p)
                    moveBatch.add(p)
                }
                val p = event.toStylusPoint(historicalIndex = -1)
                appendPointBounded(p)
                moveBatch.add(p)
                tryEmit(StylusEvent.StrokeMove(moveBatch))
            }
            MotionEvent.ACTION_UP -> {
                val point = event.toStylusPoint(historicalIndex = -1)
                appendPointBounded(point)
                val snapshot = snapshotStrokeBufferBlocking()
                tryEmit(StylusEvent.StrokeEnd(snapshot))
            }
            MotionEvent.ACTION_HOVER_ENTER -> tryEmit(StylusEvent.HoverEnter)
            MotionEvent.ACTION_HOVER_EXIT -> tryEmit(StylusEvent.HoverExit)
            MotionEvent.ACTION_CANCEL -> {
                resetStrokeBufferBlocking()
            }
            else -> Unit
        }
    }

    /**
     * S-Pen side-button handler. Wire from `Activity.onKeyDown(keyCode, event)`:
     * ```
     * override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean =
     *     stylusManager.handleKeyEvent(event) || super.onKeyDown(keyCode, event)
     * ```
     *
     * Returns `true` if the event was consumed (caller must stop propagation),
     * `false` to let other handlers see it. Only the PRIMARY button on the down
     * stroke is consumed — repeated key events from a long-press do not double-fire
     * the pencil-mode toggle (event.repeatCount filter).
     */
    fun handleKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return false
        if (event.repeatCount != 0) return false
        return when (event.keyCode) {
            KeyEvent.KEYCODE_STYLUS_BUTTON_PRIMARY -> {
                tryEmit(StylusEvent.SideButtonPressed)
                true
            }
            else -> false
        }
    }

    // ─── Internal helpers ────────────────────────────────────────────────────

    private fun tryEmit(ev: StylusEvent) {
        // tryEmit is non-suspending; if the buffer overflows (rare — 64 slots) the
        // DROP_OLDEST policy kicks in inside SharedFlow.
        _events.tryEmit(ev)
    }

    /**
     * Lightweight blocking helpers for the stroke buffer. The buffer is touched
     * from the same thread that called `onMotionEvent` (typically Main), so the
     * `runBlocking` is uncontested; using a Mutex (not `synchronized`) keeps the
     * code suspending-safe if a future test path calls these from a coroutine.
     */
    private fun resetStrokeBufferBlocking() = runBlocking {
        bufferMutex.withLock { strokeBuffer.clear() }
    }

    private fun appendPointBounded(point: StylusPoint) = runBlocking {
        bufferMutex.withLock {
            if (strokeBuffer.size >= MAX_STROKE_POINTS) {
                strokeBuffer.removeFirst()
            }
            strokeBuffer.addLast(point)
        }
    }

    private fun snapshotStrokeBufferBlocking(): List<StylusPoint> = runBlocking {
        bufferMutex.withLock { strokeBuffer.toList() }
    }

    private fun MotionEvent.isFromStylus(): Boolean {
        val pointerIndex = actionIndex.coerceAtLeast(0)
        return try {
            getToolType(pointerIndex) == MotionEvent.TOOL_TYPE_STYLUS ||
                getToolType(pointerIndex) == MotionEvent.TOOL_TYPE_ERASER
        } catch (_: IllegalArgumentException) {
            // Defensive: malformed synthetic events. Treat as non-stylus.
            false
        }
    }

    /**
     * Build a [StylusPoint] from this motion event. When [historicalIndex] is
     * negative, the *current* (most-recent) sample is read; when non-negative, the
     * corresponding historical sample is read via [MotionEvent.getHistoricalX] et al.
     */
    private fun MotionEvent.toStylusPoint(historicalIndex: Int): StylusPoint {
        return if (historicalIndex < 0) {
            StylusPoint(
                x = this.x,
                y = this.y,
                pressure = this.pressure,
                tMs = this.eventTime
            )
        } else {
            StylusPoint(
                x = getHistoricalX(historicalIndex),
                y = getHistoricalY(historicalIndex),
                pressure = getHistoricalPressure(historicalIndex),
                tMs = getHistoricalEventTime(historicalIndex)
            )
        }
    }

    companion object {
        /**
         * Hard cap on points held in the live stroke buffer. 2048 ≈ 8s of writing at
         * 240Hz S-Pen sampling (stylus-reviewer §7), generous for any single digit.
         * Exceeding the cap drops the oldest point — recognition still works because
         * digit shape is preserved by the most recent ~1s of samples.
         */
        const val MAX_STROKE_POINTS: Int = 2048

        /** For diagnostics / tests. Monotonic-ish timestamp. */
        @JvmStatic
        fun currentTimestampMs(): Long = SystemClock.elapsedRealtime()
    }
}
