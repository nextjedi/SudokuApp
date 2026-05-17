package com.nextjedi.sudokustreak.android.ui.components

import android.graphics.Rect as AndroidRect
import android.view.MotionEvent
import android.view.View
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalView
import com.nextjedi.sudokustreak.domain.input.StylusInputManager

/**
 * Compose integration for [StylusInputManager].
 *
 * This file exposes three modifiers used by `SudokuGrid` (Wave 2 sibling agent) to
 * wire the cross-platform stylus pipeline into the Android Compose surface without
 * leaking ML Kit / MotionEvent types into the UI layer:
 *
 *  - [stylusInput] feeds every [MotionEvent] hitting the grid into the manager.
 *    Finger touches still pass to the default tap handler (the manager itself
 *    drops non-stylus events — see TC-AS1 in test-plan/01-test-scenarios.md).
 *  - [stylusGestureExclusion] calls `View.setSystemGestureExclusionRects` so
 *    edge strokes near the left/right grid border don't trigger Android's
 *    back-swipe gesture (TC-AS18 / P1-25 in REVAMP_PLAN.md §8).
 *  - [stylusPulseBorder] renders the pulsing 2px primary-color border that
 *    indicates "pen-down on this cell" — placeholder hook; the real grid wiring
 *    arrives in the Wave 2 SudokuGrid revamp.
 *
 * ## Why `pointerInteropFilter` and not `pointerInput { awaitPointerEvent() }`
 *
 * Android-specific historical-point capture (`getHistoricalX`/Y/Pressure) only
 * exists on the raw [MotionEvent], not on Compose's `PointerInputChange`. The
 * Compose pointer-events API drops historical samples — at a 240Hz S-Pen we'd lose
 * ~half of every move batch. `Modifier.pointerInteropFilter` is the official Android
 * Compose API for tapping the raw MotionEvent stream and is exactly what
 * REVAMP_PLAN.md §8 calls for ("Capture historical points via
 * `MotionEvent.getHistoricalX/Y/Pressure`"). We return `false` from the filter so
 * finger taps still propagate to the cell's regular `onTap` handler beneath us.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.stylusInput(
    manager: StylusInputManager,
    enabled: Boolean = true
): Modifier {
    if (!enabled) return this
    return this.pointerInteropFilter { motion: MotionEvent ->
        manager.onMotionEvent(motion)
        // Returning false lets the event continue to downstream pointer handlers
        // (e.g., Compose's click detection on the cell). The manager itself drops
        // non-stylus events (TC-AS1), so finger taps still reach `onTap`.
        false
    }
}

/**
 * Apply system-gesture-exclusion rectangles equal to the supplied cell bounds so
 * the OS back-swipe (and 3-button-nav drag) is suppressed wherever the grid lives.
 *
 * Coordinates in [cellBounds] are interpreted in this composable's local space and
 * translated to window space inside [onGloballyPositioned].
 *
 * @param cellBounds rectangles in this composable's local coordinate space (pixels).
 *                   Pass `emptyList()` to clear.
 */
fun Modifier.stylusGestureExclusion(cellBounds: List<Rect>): Modifier = composed {
    val view: View = LocalView.current
    onGloballyPositioned { coords: LayoutCoordinates ->
        val origin: Offset = coords.positionInWindow()
        val rects = if (cellBounds.isEmpty()) {
            // No explicit cell rects → exclude the entire composable bounds.
            val w = coords.size.width
            val h = coords.size.height
            listOf(
                AndroidRect(
                    origin.x.toInt(),
                    origin.y.toInt(),
                    (origin.x + w).toInt(),
                    (origin.y + h).toInt()
                )
            )
        } else {
            cellBounds.map { r ->
                AndroidRect(
                    (r.left + origin.x).toInt(),
                    (r.top + origin.y).toInt(),
                    (r.right + origin.x).toInt(),
                    (r.bottom + origin.y).toInt()
                )
            }
        }
        // setSystemGestureExclusionRects is API 29+. On API 24..28 it is a no-op
        // via the framework's compat path. P1-25 (gesture-exclusion) is therefore
        // only effective on Android 10+.
        view.systemGestureExclusionRects = rects
    }
}

/**
 * Pulsing 2px primary-color border placeholder. Real wiring happens in Wave 2 via
 * the redesigned `SudokuGrid` cell composable. The border animates opacity 0.4 ↔ 1.0
 * over 800ms when [active]; when inactive, the modifier is a no-op.
 *
 * Implemented via [drawWithContent] (not [androidx.compose.foundation.border]) so the
 * stroke draws *over* the cell digit without contributing to layout — the cell's
 * content sizing remains unchanged whether or not the pulse is shown.
 */
@Composable
fun Modifier.stylusPulseBorder(active: Boolean): Modifier {
    if (!active) return this
    val transition = rememberInfiniteTransition(label = "stylus_pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "stylus_pulse_alpha"
    )
    val border = MaterialTheme.colorScheme.primary
    return this.drawWithContent {
        drawContent()
        drawRect(
            color = border.copy(alpha = alpha),
            topLeft = Offset.Zero,
            size = Size(size.width, size.height),
            style = Stroke(width = 2f)
        )
    }
}

/**
 * Convenience composable that combines the three modifiers above into a single
 * overlay box. Wave 2's `SudokuGrid` will likely embed [stylusInput] and
 * [stylusPulseBorder] directly into each cell instead of using this overlay, but
 * shipping the wrapper here gives the alpha tester a one-line wiring path.
 */
@Composable
fun StylusInputOverlay(
    manager: StylusInputManager,
    active: Boolean = true,
    cellBounds: List<Rect> = emptyList(),
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .stylusInput(manager = manager, enabled = active)
            .stylusGestureExclusion(cellBounds = cellBounds)
            .stylusPulseBorder(active = active)
    )
}
