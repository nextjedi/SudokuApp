package com.nextjedi.sudokustreak.android.a11y

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Bluetooth-keyboard navigation for the Sudoku grid + bottom toolbar.
 *
 * The mapping itself is implemented as a pure reducer in
 * [keyboardActionFor] / [moveSelection] inside `AccessibilityHelpers.kt`, so
 * this test file is pure-JVM (no Robolectric runner required).
 *
 * Covers every key the spec calls out:
 *
 *   - Arrow keys → move grid selection (wrap on edges)
 *   - Digits 1–9 → enter the digit at the selected cell
 *   - DEL / FORWARD_DEL → erase
 *   - SPACE → open hint
 *   - ESC → return to home
 *   - Tab → handled by Compose's focus system (no action needed here)
 *   - WASD aliases → same as arrow keys, for game-controller users
 *   - Unknown keys → fall through to default Compose handling
 *
 * Lena's J-15 audit: motor-impaired switch-control users rely entirely on
 * keyboard / switch focus management. Every interactive node on the grid +
 * bottom toolbar MUST be reachable without a touch screen.
 */
class KeyboardNavigationTest {

    // ============================================================================
    //                       Arrow-key grid navigation
    // ============================================================================

    @Test
    fun arrowRight_fromTopLeft_movesToColumn1() {
        assertEquals(0 to 1, moveSelection(0 to 0, ArrowKey.Right))
    }

    @Test
    fun arrowRight_wrapsAtRightEdge() {
        assertEquals(3 to 0, moveSelection(3 to 8, ArrowKey.Right))
    }

    @Test
    fun arrowLeft_fromTopLeft_wrapsToColumn8() {
        assertEquals(0 to 8, moveSelection(0 to 0, ArrowKey.Left))
    }

    @Test
    fun arrowDown_incrementsRow() {
        assertEquals(1 to 4, moveSelection(0 to 4, ArrowKey.Down))
    }

    @Test
    fun arrowDown_wrapsAtBottomEdge() {
        assertEquals(0 to 4, moveSelection(8 to 4, ArrowKey.Down))
    }

    @Test
    fun arrowUp_fromTopRow_wrapsToRow8() {
        assertEquals(8 to 4, moveSelection(0 to 4, ArrowKey.Up))
    }

    @Test
    fun nullSelection_lands_atOrigin() {
        // First arrow press when no cell is selected should drop the cursor on
        // (0,0). Direction is ignored for the initial drop — predictability >
        // cleverness.
        assertEquals(0 to 0, moveSelection(null, ArrowKey.Right))
        assertEquals(0 to 0, moveSelection(null, ArrowKey.Down))
    }

    // ============================================================================
    //                       Digit entry
    // ============================================================================

    @Test
    fun digits1Through9_allMapToEnterDigit() {
        val codes = mapOf(
            1 to KeyEvent.KEYCODE_1,
            2 to KeyEvent.KEYCODE_2,
            3 to KeyEvent.KEYCODE_3,
            4 to KeyEvent.KEYCODE_4,
            5 to KeyEvent.KEYCODE_5,
            6 to KeyEvent.KEYCODE_6,
            7 to KeyEvent.KEYCODE_7,
            8 to KeyEvent.KEYCODE_8,
            9 to KeyEvent.KEYCODE_9,
        )
        for ((digit, code) in codes) {
            val action = keyboardActionFor(code)
            assertTrue(
                "Digit $digit (keycode $code) must map to EnterDigit, was $action",
                action is KeyboardAction.EnterDigit,
            )
            assertEquals(digit, (action as KeyboardAction.EnterDigit).value)
        }
    }

    @Test
    fun digit0_doesNotMap() {
        // 0 is intentionally not a valid Sudoku entry (it's encoded as "empty"
        // in the model). Pressing 0 must fall through.
        assertNull(keyboardActionFor(KeyEvent.KEYCODE_0))
    }

    // ============================================================================
    //                       Special-key bindings
    // ============================================================================

    @Test
    fun backspace_maps_toErase() {
        assertEquals(KeyboardAction.Erase, keyboardActionFor(KeyEvent.KEYCODE_DEL))
    }

    @Test
    fun forwardDelete_maps_toErase() {
        assertEquals(KeyboardAction.Erase, keyboardActionFor(KeyEvent.KEYCODE_FORWARD_DEL))
    }

    @Test
    fun escape_maps_toExitToHome() {
        assertEquals(KeyboardAction.ExitToHome, keyboardActionFor(KeyEvent.KEYCODE_ESCAPE))
    }

    @Test
    fun space_maps_toOpenHint() {
        assertEquals(KeyboardAction.OpenHint, keyboardActionFor(KeyEvent.KEYCODE_SPACE))
    }

    // ============================================================================
    //                       Game-controller WASD aliases
    // ============================================================================

    @Test
    fun wasd_aliasesArrowKeys() {
        assertEquals(
            KeyboardAction.Move(ArrowKey.Up),
            keyboardActionFor(KeyEvent.KEYCODE_W),
        )
        assertEquals(
            KeyboardAction.Move(ArrowKey.Down),
            keyboardActionFor(KeyEvent.KEYCODE_S),
        )
        assertEquals(
            KeyboardAction.Move(ArrowKey.Left),
            keyboardActionFor(KeyEvent.KEYCODE_A),
        )
        assertEquals(
            KeyboardAction.Move(ArrowKey.Right),
            keyboardActionFor(KeyEvent.KEYCODE_D),
        )
    }

    // ============================================================================
    //                       Fall-through behaviour
    // ============================================================================

    @Test
    fun unknown_keys_returnNull() {
        // F1..F12, modifier keys, media keys — all must fall through so
        // Compose's default focus / navigation behaviour can handle them.
        assertNull(keyboardActionFor(KeyEvent.KEYCODE_F1))
        assertNull(keyboardActionFor(KeyEvent.KEYCODE_SHIFT_LEFT))
        assertNull(keyboardActionFor(KeyEvent.KEYCODE_CTRL_LEFT))
        assertNull(keyboardActionFor(KeyEvent.KEYCODE_MEDIA_PLAY))
        assertNull(keyboardActionFor(KeyEvent.KEYCODE_TAB))
    }

    // ============================================================================
    //                       Multi-step scenarios
    // ============================================================================

    @Test
    fun arrowRightThreeTimes_lands_atColumn3() {
        var cell: Pair<Int, Int>? = 0 to 0
        repeat(3) { cell = moveSelection(cell, ArrowKey.Right) }
        assertEquals(0 to 3, cell)
    }

    @Test
    fun fullRowSweep_doesNotEscapeRow() {
        var cell: Pair<Int, Int>? = 5 to 0
        for (i in 0 until 8) {
            cell = moveSelection(cell, ArrowKey.Right)
            assertEquals(
                "Row must not change while sweeping horizontally",
                5,
                cell!!.first,
            )
        }
    }

    @Test
    fun fullColumnSweep_doesNotEscapeColumn() {
        var cell: Pair<Int, Int>? = 0 to 4
        for (i in 0 until 8) {
            cell = moveSelection(cell, ArrowKey.Down)
            assertEquals(
                "Column must not change while sweeping vertically",
                4,
                cell!!.second,
            )
        }
    }
}
