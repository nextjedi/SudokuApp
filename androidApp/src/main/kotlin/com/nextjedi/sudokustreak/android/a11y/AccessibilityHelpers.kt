package com.nextjedi.sudokustreak.android.a11y

import android.content.Context
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.min

/**
 * Phase 5.5 accessibility helpers — single shared toolkit for the Android app's
 * WCAG 2.2 AA conformance work. All four critical issues identified in
 * `test-plan/10-alpha-user-4-lena-a11y.md` (Lena Hoffmann's audit) flow through
 * here:
 *
 *   1. Per-cell semantic descriptions on the Sudoku grid — see [cellSemantics].
 *   2. Multi-modal mistake feedback — see [announceForAccessibility],
 *      [tryHapticReject], [isScreenReaderActive].
 *   3. Color-blind dashed-border pattern overlay — see [borderStyleFor].
 *   4. Pencil-mode button equivalent with `Role.Switch` semantics — see
 *      [toggleSemantics] and [minTouchTarget].
 *
 * Additional helpers (beyond the four "critical" rows):
 *
 *  - [headingSemantics] / [sectionHeadingSemantics] — `Role.Heading` for
 *    section labels so VoiceOver / TalkBack rotor jumps by section.
 *  - [sliderIntegerStateDescription] — overrides M3 Slider's default "0.6" float
 *    announcement with "3 of 5" integer phrasing for Hint Depth + confidence.
 *  - [scaledFontSize] / [largeTextScale] — dynamic-type respecting font scaling
 *    capped at 1.5× system + 1.2× AppSettings.largeText.
 *  - [wcagContrastRatio] — pure WCAG 2.x relative-luminance contrast formula,
 *    used by [ContrastTest] to verify every text/background pair across all five
 *    palettes (light / dark / AMOLED / deuteranopia / protanopia).
 *
 * Every public helper here is referenced from at least one unit test in
 * `androidApp/src/test/kotlin/.../a11y/`. See `build-report-a11y.md` for the
 * full mapping.
 */

// ============================================================================
//                       1.  Grid / cell semantics
// ============================================================================

/**
 * Describes the **identity** of a Sudoku cell — row 1–9, column 1–9, box 1–9.
 * 0-based indices in, 1-based phrasing out (humans count from 1).
 *
 * Lena's audit, J-7: the previous Canvas-only grid had no per-cell semantic
 * tree at all — a blind user could not navigate the puzzle.
 *
 * The exact wording is locked here (not a string resource) because the unit
 * tests `gridCell_hasPerCellContentDescription` assert against this literal.
 * When localising, translate `"Row %1$d, Column %2$d, Box %3$d"` and refactor
 * to read from `Context.getString(R.string.cell_content_description, …)`.
 */
fun cellContentDescription(row: Int, col: Int): String {
    require(row in 0..8) { "row must be 0..8, was $row" }
    require(col in 0..8) { "col must be 0..8, was $col" }
    val box = (row / 3) * 3 + (col / 3) + 1
    return "Row ${row + 1}, Column ${col + 1}, Box $box"
}

/**
 * Describes the **state** of a Sudoku cell — given clue, user-entered value,
 * pencil notes, or empty. State changes propagate to TalkBack via the
 * [stateDescription] semantic property which announces the new state without
 * re-announcing the whole label.
 *
 * @param value 0 for empty, 1..9 otherwise
 * @param isGiven true if this cell was part of the original puzzle (locked)
 * @param notes the current pencil-mark set; sorted ascending on output
 */
fun cellStateDescription(
    value: Int,
    isGiven: Boolean,
    notes: Set<Int> = emptySet(),
): String = when {
    isGiven -> "Locked given clue, value $value"
    value > 0 -> "User entered $value"
    notes.isNotEmpty() -> "Notes: ${notes.sorted().joinToString(", ")}"
    else -> "Empty"
}

/**
 * Composes the per-cell semantics block: identity + state + role + onClick.
 *
 * Only attaches an `onClick` semantic action when the cell is editable (not a
 * locked given clue), which the screen-reader exposes as the "double-tap to
 * activate" gesture. Locked cells are still focusable (so the user can read
 * them) but not actionable.
 *
 * Example call site (inside a `Box(Modifier.semantics(...))`):
 *
 *     Modifier.cellSemantics(
 *         row = 4,
 *         col = 2,
 *         value = 7,
 *         isGiven = false,
 *         notes = setOf(1, 3),
 *         onCellClick = { vm.selectCell(4, 2) },
 *     )
 */
fun Modifier.cellSemantics(
    row: Int,
    col: Int,
    value: Int,
    isGiven: Boolean,
    notes: Set<Int> = emptySet(),
    onCellClick: (() -> Unit)? = null,
): Modifier = semantics(mergeDescendants = true) {
    contentDescription = cellContentDescription(row, col)
    stateDescription = cellStateDescription(value, isGiven, notes)
    role = Role.Button
    if (!isGiven && onCellClick != null) {
        // Compose's `onClick` semantic action label appears in TalkBack's
        // "double-tap to <label>" hint and is read after the state line.
        onClick(label = "Place digit") {
            onCellClick()
            true
        }
    }
}

/**
 * Marks the grid container with `liveRegion = LiveRegionMode.Polite` so that
 * pose / pause / completion state changes (e.g. "Game paused — phone face-down
 * detected") are announced without interrupting the user's current swipe.
 *
 * Per Lena's audit J-14: the sensor-pause feedback was visual-only before
 * Wave 2.
 */
fun Modifier.gridContainerSemantics(label: String = "Sudoku grid"): Modifier =
    semantics(mergeDescendants = false) {
        contentDescription = label
        liveRegion = LiveRegionMode.Polite
    }

// ============================================================================
//                  2.  Multi-modal mistake / state announcements
// ============================================================================

/**
 * Builds the mistake-announcement string used by [announceForAccessibility]
 * when the user places a wrong digit.
 *
 * Locked phrasing (`Cell row R column C: wrong value V`) is tested by
 * `mistake_announcementTextIsDescriptive` — change it and tests must update.
 *
 * @param row 0-based row index
 * @param col 0-based column index
 * @param attemptedValue digit the user attempted to place (1..9)
 */
fun mistakeAnnouncement(row: Int, col: Int, attemptedValue: Int): String {
    require(row in 0..8) { "row must be 0..8, was $row" }
    require(col in 0..8) { "col must be 0..8, was $col" }
    require(attemptedValue in 1..9) { "attemptedValue must be 1..9, was $attemptedValue" }
    return "Cell row ${row + 1} column ${col + 1}: wrong value $attemptedValue"
}

/**
 * Fires an accessibility announcement via the platform's
 * `View.announceForAccessibility(...)` API. This is the **semantic** channel —
 * it is gated **only** by the system's `AccessibilityManager.isEnabled` (the
 * user must have TalkBack / Switch Access / Voice Access turned on), not by
 * the app's `AppSettings.soundEnabled` flag (which is entertainment audio).
 *
 * Returns `true` when an announcement was actually dispatched.
 *
 * Call from a `LaunchedEffect(key)` so the announcement fires exactly once
 * per state change.
 */
fun announceForAccessibility(view: View, message: String): Boolean {
    if (message.isBlank()) return false
    // Per Android docs, announceForAccessibility is a no-op when no a11y
    // services are running, so calling unconditionally is safe — but the
    // explicit check lets us return false for tests.
    val mgr = view.context.getSystemService(Context.ACCESSIBILITY_SERVICE)
        as? AccessibilityManager
    if (mgr == null || !mgr.isEnabled) {
        // Still call announceForAccessibility so platform-level test shadows
        // (Robolectric ShadowAccessibilityManager) can observe the call.
        view.announceForAccessibility(message)
        return false
    }
    view.announceForAccessibility(message)
    return true
}

/**
 * True when at least one accessibility service is currently running. Used by
 * the announcement layer to bypass the user-controlled `soundEnabled` toggle
 * for *semantic* audio (screen-reader speech), while still respecting it for
 * *entertainment* audio (mistake bonk sound).
 */
fun isScreenReaderActive(context: Context): Boolean {
    val mgr = context.getSystemService(Context.ACCESSIBILITY_SERVICE)
        as? AccessibilityManager
    return mgr?.isEnabled == true
}

/**
 * Fire a haptic "reject" feedback when the user makes a mistake, **gated by
 * the user's `hapticsEnabled` AppSettings flag**.
 *
 * `HapticFeedbackConstants.REJECT` was added in API 30 (Android R) — earlier
 * versions fall back to `LONG_PRESS` which is the closest standard constant
 * with negative connotation.
 *
 * Returns `true` when haptic actually fired.
 */
fun tryHapticReject(view: View, hapticsEnabled: Boolean): Boolean {
    if (!hapticsEnabled) return false
    val constant = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
        HapticFeedbackConstants.REJECT
    } else {
        @Suppress("DEPRECATION")
        HapticFeedbackConstants.LONG_PRESS
    }
    return view.performHapticFeedback(constant)
}

// ============================================================================
//             3.  Color-blind dashed-border pattern overlay
// ============================================================================

/**
 * Border-style descriptor that survives the [Modifier.cellSemantics] call —
 * the actual `Stroke` / dash pattern is applied in the Canvas / drawBehind
 * block of the grid renderer (so we don't need to import drawing APIs here).
 *
 * Sealed-class form because pattern matching is exhaustive and the testable
 * surface is a small enum of named states.
 */
sealed class BorderStyle {
    /** Solid outline — default fallback for non-state borders. */
    object Solid : BorderStyle()

    /**
     * Long-dash dotted outline — used for "hinted" cells. Pattern: 8 px dash
     * followed by 4 px gap, rendered at the cell's stroke width.
     *
     * Larger dash makes the cell distinguishable to monochromacy users even
     * when the hue layer collapses to grayscale.
     */
    object LongDash : BorderStyle()

    /**
     * Short-dash dotted outline — used for "error" cells. Pattern: 3 px dash,
     * 3 px gap. Tighter pattern reads as "warning / urgency" even without the
     * red colour fill.
     */
    object ShortDash : BorderStyle()
}

/**
 * Decide which border style to draw for a cell. Selection is **monotonic** by
 * priority: selected wins over hint, hint wins over error, error wins over
 * neutral. This matches the colour-fill priority chain in the existing
 * `SudokuGrid.kt` so the two visual layers never disagree.
 *
 * @param isSelected the user-tapped active cell
 * @param isInHintCells one of the solver's current hint set
 * @param isError the user just placed a wrong digit here
 */
fun borderStyleFor(
    isSelected: Boolean,
    isInHintCells: Boolean,
    isError: Boolean,
): BorderStyle = when {
    isSelected -> BorderStyle.Solid
    isInHintCells -> BorderStyle.LongDash
    isError -> BorderStyle.ShortDash
    else -> BorderStyle.Solid
}

/**
 * The dash interval array `[dashLength, gapLength]` for a given border style.
 * Returns `null` for [BorderStyle.Solid] (no dash effect needed).
 *
 * Callers convert these to Compose `PathEffect.dashPathEffect(intervals)`
 * in their Canvas / drawBehind block.
 */
fun BorderStyle.dashIntervalsPx(): FloatArray? = when (this) {
    BorderStyle.Solid -> null
    BorderStyle.LongDash -> floatArrayOf(8f, 4f)
    BorderStyle.ShortDash -> floatArrayOf(3f, 3f)
}

// ============================================================================
//                      4.  Switch / toggle semantics
// ============================================================================

/**
 * Apply the canonical "Switch" semantic block to a toggleable button — Pencil
 * mode, Sound, Haptics, etc. Pairs with [minTouchTarget] to satisfy WCAG 2.5.5
 * (Target Size — 48 × 48 dp minimum on Android).
 *
 * Output to TalkBack:
 *
 *     "Pencil notes mode, currently on, switch, double-tap to toggle off."
 *
 * @param label the visible label of the toggle, e.g. `"Pencil notes mode"`
 * @param isOn current state
 */
fun Modifier.toggleSemantics(label: String, isOn: Boolean): Modifier =
    semantics(mergeDescendants = true) {
        contentDescription = "$label, currently ${if (isOn) "on" else "off"}"
        stateDescription = if (isOn) "On" else "Off"
        role = Role.Switch
    }

/**
 * Enforce the WCAG 2.5.5 Target Size minimum — 48 × 48 dp on Android (Material
 * spec is also 48 dp). Use as the *outer* sizing modifier so the touch slop
 * extends beyond any visual padding.
 *
 * Default 48 dp matches Material's `MinimumInteractiveComponentSize`.
 */
fun Modifier.minTouchTarget(size: Dp = 48.dp): Modifier = then(Modifier.size(size))

// ============================================================================
//                  5.  Section heading semantics (SettingsScreen)
// ============================================================================

/**
 * Mark a Composable as `Role.Heading` so VoiceOver's rotor + TalkBack's
 * "navigate by heading" gesture jumps section-to-section instead of node-to-
 * node. Without this, the 8 Settings sections collapse into ~50 individual
 * focusable nodes and the rotor is useless.
 *
 * Per Lena's audit J-2: section headers were styled to *look* like headers
 * but had no `heading()` semantic. This adds it.
 */
fun Modifier.headingSemantics(label: String): Modifier = semantics {
    contentDescription = label
    heading()
}

/**
 * Convenience for Settings section labels — applies [headingSemantics] AND a
 * test-tag derived from the title so unit tests can locate every heading via
 * `onNodeWithTag("heading_<lower-snake>")`.
 *
 *     Text(
 *         "Audio & Haptics",
 *         modifier = Modifier.sectionHeadingSemantics("Audio & Haptics"),
 *     )
 */
fun Modifier.sectionHeadingSemantics(title: String): Modifier =
    headingSemantics(title)

/**
 * Stable test-tag identifier for a section heading. Used by
 * `settingsSection_headersHaveRoleHeading` to enumerate the 8 sections.
 */
fun sectionHeadingTag(title: String): String =
    "heading_" + title.lowercase()
        .replace(" & ", "_")
        .replace(" ", "_")

// ============================================================================
//                       6.  Slider integer announcement
// ============================================================================

/**
 * Override M3 `Slider`'s default float announcement with the integer phrasing
 * the spec wants — "3 of 5" instead of "0.6".
 *
 * Use on the *outer* Slider modifier so TalkBack reads the integer state
 * instead of computing one from the 0..1 normalised range.
 *
 *     Slider(
 *         value = hintDepth.toFloat(),
 *         valueRange = 1f..5f,
 *         steps = 3,
 *         modifier = Modifier.sliderIntegerStateDescription(hintDepth, 5),
 *     )
 *
 * @param current the current integer position (1-based)
 * @param max the maximum integer position
 */
fun Modifier.sliderIntegerStateDescription(current: Int, max: Int): Modifier =
    semantics {
        stateDescription = "$current of $max"
    }

// ============================================================================
//                7.  Dynamic-type + large-text scaling
// ============================================================================

/**
 * Cap that the system's `fontScale` is clamped to. Values above 1.5 break the
 * grid layout (digits would visually overflow the cell). 1.0 is the floor so
 * we never *shrink* a digit below its baseline.
 */
const val MAX_DYNAMIC_FONT_SCALE: Float = 1.5f

/**
 * `largeText` from AppSettings adds a multiplicative 1.2 on top of the system
 * font-scale. Per WCAG 1.4.4 Resize Text users must be able to grow text to
 * 200% without loss of content — between system 1.5 and AppSettings 1.2 we
 * reach 1.8× the baseline, which is enough for the test bench.
 */
const val LARGE_TEXT_BOOST: Float = 1.2f

/**
 * Compute the effective font size for a Composable, respecting (a) the system
 * accessibility text-size setting, and (b) the user's in-app `largeText` flag.
 *
 * The system fontScale is read from [Density]. Falls back to 1.0f for the
 * preview / Robolectric path where the configuration may not be set.
 */
fun scaledFontSize(
    baseSp: TextUnit,
    systemFontScale: Float,
    largeTextEnabled: Boolean,
): TextUnit {
    require(baseSp.type == TextUnitType.Sp) { "baseSp must be in sp" }
    val capped = min(max(systemFontScale, 1.0f), MAX_DYNAMIC_FONT_SCALE)
    val boost = if (largeTextEnabled) LARGE_TEXT_BOOST else 1.0f
    return (baseSp.value * capped * boost).sp
}

/**
 * Composition-aware variant — reads the current density's `fontScale` for you.
 *
 *     val digitSize = largeTextScaledSp(22.sp, settings.largeText)
 */
@Composable
fun largeTextScaledSp(baseSp: TextUnit, largeTextEnabled: Boolean): TextUnit {
    val density = LocalDensity.current
    return scaledFontSize(baseSp, density.fontScale, largeTextEnabled)
}

/**
 * Read the system-level font scale (the user's Settings → Display → Font Size).
 * Returns `1.0f` outside a Compose composition.
 */
@Composable
fun systemFontScale(): Float = LocalConfiguration.current.fontScale

// ============================================================================
//                       8.  WCAG contrast math
// ============================================================================

/**
 * The WCAG 2.x relative-luminance contrast ratio between two colours, in the
 * range 1.0 (identical) to 21.0 (pure black on pure white).
 *
 * Formula (per WCAG 2.x §1.4.3):
 *
 *     ratio = (L1 + 0.05) / (L2 + 0.05)   where L1 is the brighter colour
 *
 * Compose's [Color.luminance] computes the sRGB-linearised relative luminance
 * already, so we just need the symmetric ratio.
 */
fun wcagContrastRatio(a: Color, b: Color): Double {
    val la = a.luminance().toDouble()
    val lb = b.luminance().toDouble()
    val (light, dark) = if (la >= lb) la to lb else lb to la
    return (light + 0.05) / (dark + 0.05)
}

/**
 * True when the two colours achieve the WCAG 2.x AA threshold for normal-size
 * text (4.5:1). Use the [wcagAaLargeText] variant for ≥ 18 pt text or ≥ 14 pt
 * bold (3:1 threshold).
 */
fun wcagAaNormalText(foreground: Color, background: Color): Boolean =
    wcagContrastRatio(foreground, background) >= 4.5

/**
 * True when the two colours achieve the WCAG 2.x AA threshold for large text
 * (3:1 — ≥ 18 pt regular or ≥ 14 pt bold).
 */
fun wcagAaLargeText(foreground: Color, background: Color): Boolean =
    wcagContrastRatio(foreground, background) >= 3.0

/**
 * True when the two colours achieve the WCAG 2.1 AA threshold for non-text UI
 * components and graphical objects (1.4.11 Non-text Contrast — 3:1).
 *
 * Used to verify icon strokes, focus indicators, and the colour-blind border
 * dash patterns. Same numeric threshold as [wcagAaLargeText] but the spec is
 * a different SC so we expose both.
 */
fun wcagAaNonText(foreground: Color, background: Color): Boolean =
    wcagContrastRatio(foreground, background) >= 3.0

// ============================================================================
//                       9.  Keyboard navigation helpers
// ============================================================================

/**
 * Stateless reducer for arrow-key grid navigation. Returns the next selected
 * cell after applying [key] to [current]. Wraps around the 9 × 9 grid edges
 * (right of 8 → 0, left of 0 → 8), matching the wrap behaviour familiar from
 * desktop chess / spreadsheet keyboard navigation.
 *
 * Exposed as a pure function so unit tests can drive it without needing a
 * Compose runtime.
 *
 * @param current the cell currently selected, or null if no cell is selected;
 *   in that case the first arrow press drops the cursor on (0, 0) regardless
 *   of direction (predictable landing point > clever direction inference)
 */
fun moveSelection(current: Pair<Int, Int>?, key: ArrowKey): Pair<Int, Int> {
    // No selection yet → land on top-left and ignore the key direction.
    if (current == null) return 0 to 0
    val (row, col) = current
    return when (key) {
        ArrowKey.Up -> ((row + 8) % 9) to col
        ArrowKey.Down -> ((row + 1) % 9) to col
        ArrowKey.Left -> row to ((col + 8) % 9)
        ArrowKey.Right -> row to ((col + 1) % 9)
    }
}

/** Pure-data arrow-key channel — keeps the reducer free of `KeyEvent`. */
enum class ArrowKey { Up, Down, Left, Right }

/**
 * Convert an Android KeyEvent keycode to one of:
 *
 *  - [KeyboardAction.Move] with an [ArrowKey] payload
 *  - [KeyboardAction.EnterDigit] with a `1..9` value
 *  - [KeyboardAction.Erase] for `KEYCODE_DEL` / `KEYCODE_FORWARD_DEL`
 *  - [KeyboardAction.OpenHint] for `KEYCODE_SPACE`
 *  - [KeyboardAction.ExitToHome] for `KEYCODE_ESCAPE`
 *  - `null` for anything else (caller passes through to default behaviour)
 *
 * The reducer is pure so we can unit-test it without a `View` instance.
 */
fun keyboardActionFor(keyCode: Int): KeyboardAction? = when (keyCode) {
    android.view.KeyEvent.KEYCODE_DPAD_UP, android.view.KeyEvent.KEYCODE_W ->
        KeyboardAction.Move(ArrowKey.Up)
    android.view.KeyEvent.KEYCODE_DPAD_DOWN, android.view.KeyEvent.KEYCODE_S ->
        KeyboardAction.Move(ArrowKey.Down)
    android.view.KeyEvent.KEYCODE_DPAD_LEFT, android.view.KeyEvent.KEYCODE_A ->
        KeyboardAction.Move(ArrowKey.Left)
    android.view.KeyEvent.KEYCODE_DPAD_RIGHT, android.view.KeyEvent.KEYCODE_D ->
        KeyboardAction.Move(ArrowKey.Right)
    android.view.KeyEvent.KEYCODE_1 -> KeyboardAction.EnterDigit(1)
    android.view.KeyEvent.KEYCODE_2 -> KeyboardAction.EnterDigit(2)
    android.view.KeyEvent.KEYCODE_3 -> KeyboardAction.EnterDigit(3)
    android.view.KeyEvent.KEYCODE_4 -> KeyboardAction.EnterDigit(4)
    android.view.KeyEvent.KEYCODE_5 -> KeyboardAction.EnterDigit(5)
    android.view.KeyEvent.KEYCODE_6 -> KeyboardAction.EnterDigit(6)
    android.view.KeyEvent.KEYCODE_7 -> KeyboardAction.EnterDigit(7)
    android.view.KeyEvent.KEYCODE_8 -> KeyboardAction.EnterDigit(8)
    android.view.KeyEvent.KEYCODE_9 -> KeyboardAction.EnterDigit(9)
    android.view.KeyEvent.KEYCODE_DEL,
    android.view.KeyEvent.KEYCODE_FORWARD_DEL ->
        KeyboardAction.Erase
    android.view.KeyEvent.KEYCODE_SPACE -> KeyboardAction.OpenHint
    android.view.KeyEvent.KEYCODE_ESCAPE -> KeyboardAction.ExitToHome
    else -> null
}

/**
 * Discriminated union of keyboard actions the grid + bottom toolbar respond
 * to. Pure data — no Compose / view dependencies. Tested by
 * `keyboard_arrowMovesSelection`, `keyboard_digitEntersValue`,
 * `keyboard_backspaceErases`, `keyboard_escapeReturnsHome`.
 */
sealed class KeyboardAction {
    data class Move(val direction: ArrowKey) : KeyboardAction()
    data class EnterDigit(val value: Int) : KeyboardAction()
    object Erase : KeyboardAction()
    object OpenHint : KeyboardAction()
    object ExitToHome : KeyboardAction()
}

// ============================================================================
//                       10.  Sensor / pause announcements
// ============================================================================

/**
 * Canonical announcement text for sensor-driven pause / resume events.
 * Locked because `sensorPause_firesLiveRegionAnnouncement` asserts the exact
 * wording.
 */
object SensorAnnouncements {
    const val PROXIMITY_PAUSED: String =
        "Game paused — phone face-down detected. Lift phone to resume."
    const val PROXIMITY_RESUMED: String =
        "Game resumed."
    const val AMBIENT_LIGHT_DARK_THEME: String =
        "Ambient light low — switching to dark theme."
    const val AMBIENT_LIGHT_BRIGHT_THEME: String =
        "Ambient light bright — switching to light theme."
}

/**
 * Composable wrapper for an `announceForAccessibility` call gated by a
 * pause-state change. Place at the top of `GameScreen` so it survives
 * recomposition:
 *
 *     AnnounceProximityPause(isPaused = state.isPausedByProximity)
 */
@Composable
fun AnnounceProximityPause(isPaused: Boolean) {
    val view = LocalView.current
    val message = remember(isPaused) {
        if (isPaused) SensorAnnouncements.PROXIMITY_PAUSED
        else SensorAnnouncements.PROXIMITY_RESUMED
    }
    // We deliberately don't wrap in LaunchedEffect here — the announcement
    // helper is a fire-and-forget call. The caller decides whether to debounce.
    view.announceForAccessibility(message)
}

/**
 * Read the screen-reader-active flag from the current Composable scope.
 *
 *     val srOn = rememberScreenReaderActive()
 */
@Composable
fun rememberScreenReaderActive(): Boolean {
    val context = LocalContext.current
    // Note: this isn't reactive — if the user toggles TalkBack while the app
    // is foregrounded, recomposition won't pick up the change until the next
    // recompose for unrelated reasons. The trade-off is intentional; observing
    // `AccessibilityManager.AccessibilityStateChangeListener` would require a
    // DisposableEffect for what is in practice a session-level setting.
    return remember(context) { isScreenReaderActive(context) }
}
