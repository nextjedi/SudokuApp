package com.nextjedi.sudokustreak.domain.board

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Smoke tests for [BoardConstants] and its helper functions.
 *
 * Tiny by design — the constants are documentary, but a regression test exists
 * for two reasons:
 * 1. To document the invariant `BOX_SIZE * BOX_SIZE == SIZE` (any 4×4 / 16×16
 *    variant that wants to reuse this object will fail this test and either
 *    derive a separate variants-specific object or expose the dimensions as a
 *    constructor parameter, not as compile-time constants).
 * 2. To exercise [boxIndexOf] and [arePeers] which non-trivially carry the
 *    audit-recommended de-duplication of the `r / 3` peer math.
 */
class BoardConstantsTest {

    // ---- Constant invariants ----

    @Test
    fun size_isNine() {
        assertEquals(9, BoardConstants.SIZE)
    }

    @Test
    fun boxSize_isThree() {
        assertEquals(3, BoardConstants.BOX_SIZE)
    }

    @Test
    fun boxCount_equalsSize() {
        assertEquals(BoardConstants.SIZE, BoardConstants.BOX_COUNT)
    }

    @Test
    fun cellCount_isEightyOne() {
        assertEquals(81, BoardConstants.CELL_COUNT)
        assertEquals(BoardConstants.SIZE * BoardConstants.SIZE, BoardConstants.CELL_COUNT)
    }

    @Test
    fun valueRange_isOneToNine() {
        assertEquals(1..9, BoardConstants.VALUE_RANGE)
    }

    @Test
    fun indexRange_isZeroToEight() {
        assertEquals(0..8, BoardConstants.INDEX_RANGE)
    }

    @Test
    fun boxSize_squared_equalsSize() {
        // The canonical Sudoku-board invariant: a SIZE×SIZE grid is tiled by
        // SIZE boxes, each BOX_SIZE×BOX_SIZE.
        assertEquals(
            BoardConstants.SIZE,
            BoardConstants.BOX_SIZE * BoardConstants.BOX_SIZE,
        )
    }

    // ---- boxIndexOf ----

    @Test
    fun boxIndexOf_topLeft_isZero() {
        assertEquals(0, boxIndexOf(0, 0))
    }

    @Test
    fun boxIndexOf_topRight_isTwo() {
        assertEquals(2, boxIndexOf(0, 8))
    }

    @Test
    fun boxIndexOf_centerCell_isFour() {
        assertEquals(4, boxIndexOf(4, 4))
    }

    @Test
    fun boxIndexOf_bottomRight_isEight() {
        assertEquals(8, boxIndexOf(8, 8))
    }

    @Test
    fun boxIndexOf_outOfRangeRow_throws() {
        assertFailsWith<IllegalArgumentException> { boxIndexOf(9, 0) }
        assertFailsWith<IllegalArgumentException> { boxIndexOf(-1, 0) }
    }

    @Test
    fun boxIndexOf_outOfRangeCol_throws() {
        assertFailsWith<IllegalArgumentException> { boxIndexOf(0, 9) }
        assertFailsWith<IllegalArgumentException> { boxIndexOf(0, -1) }
    }

    // ---- arePeers ----

    @Test
    fun arePeers_sameRow_isTrue() {
        assertTrue(arePeers(0, 0, 0, 5))
    }

    @Test
    fun arePeers_sameColumn_isTrue() {
        assertTrue(arePeers(0, 4, 6, 4))
    }

    @Test
    fun arePeers_sameBox_isTrue() {
        // Both inside the top-left 3×3 box.
        assertTrue(arePeers(0, 0, 2, 2))
        assertTrue(arePeers(1, 1, 0, 2))
    }

    @Test
    fun arePeers_sameCell_isFalse() {
        assertFalse(arePeers(4, 4, 4, 4))
    }

    @Test
    fun arePeers_differentRowColumnBox_isFalse() {
        // (0, 0) and (3, 3) — different row, different column, different box.
        assertFalse(arePeers(0, 0, 3, 3))
    }
}
