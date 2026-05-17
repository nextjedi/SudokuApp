package com.nextjedi.sudokustreak.domain.board

/**
 * Sudoku board dimensions — the **single source of truth** for `9`-as-board-size
 * and `3`-as-box-size magic numbers that previously appeared in dozens of call
 * sites across `:androidApp`, `:domain`, and (informational only) `:shared`.
 *
 * ## Why this lives in `:domain`, not `:shared`
 *
 * `:shared` is the engine layer (solver / validator / generator) and intentionally
 * stays free of cross-layer constants so its puzzle algorithms remain self-contained.
 * UI / ViewModel / domain-service code is the bigger consumer of these constants
 * (the grid renderer, the keyboard reducer, peer-cell math in `GameViewModel`),
 * so the canonical home is `:domain`. `:shared` will adopt them in a follow-up
 * pass when the engine module gains a `:domain` dependency (currently rejected by
 * the audit's H1 rule — see `test-plan/03-architecture-audit.md`).
 *
 * ## Why not just `const val SIZE = 9` at top-level?
 *
 * 1. Discoverability — `BoardConstants.SIZE` documents intent at the call site.
 * 2. KDoc grouping — the relationship between `SIZE`, `BOX_SIZE`, and `BOX_COUNT`
 *    is captured here, not scattered.
 * 3. Future variants — if X-Sudoku, Killer Sudoku, or 16×16 variants land they get
 *    their own constants object (e.g. `XSudokuBoardConstants`) and the call site
 *    grep tells you which variant a particular file targets.
 *
 * ## Properties
 *
 * - [SIZE] = 9: rows, columns, candidate digits 1..[SIZE].
 * - [BOX_SIZE] = 3: a box is `BOX_SIZE × BOX_SIZE` cells.
 * - [BOX_COUNT] = 9: the grid is `BOX_SIZE × BOX_SIZE` boxes laid out
 *   `BOX_SIZE`-wide × `BOX_SIZE`-tall (= [SIZE] total boxes).
 * - [CELL_COUNT] = 81: total cells = [SIZE] × [SIZE].
 * - [VALUE_RANGE] = 1..9: legal user-entered digit values (0 means empty).
 * - [INDEX_RANGE] = 0..8: legal row / column / box indices.
 *
 * ## Invariant
 *
 * `BOX_SIZE * BOX_SIZE == SIZE`. Verified by [com.nextjedi.sudokustreak.domain.board.BoardConstantsTest].
 */
object BoardConstants {
    /** Row and column count of a standard Sudoku grid. */
    const val SIZE: Int = 9

    /** Side length of a sub-box (a 3×3 region within the grid). */
    const val BOX_SIZE: Int = 3

    /** Total number of sub-boxes in the grid (= [SIZE]). */
    const val BOX_COUNT: Int = SIZE

    /** Total number of cells (= [SIZE] × [SIZE] = 81). */
    const val CELL_COUNT: Int = SIZE * SIZE

    /** The valid digit values a user can enter (1..[SIZE]). 0 represents an empty cell. */
    val VALUE_RANGE: IntRange = 1..SIZE

    /** The valid row / column / box-index range (0..[SIZE] − 1). */
    val INDEX_RANGE: IntRange = 0 until SIZE
}

/**
 * Compute the 0-based box index a cell belongs to. Box 0 is top-left,
 * box 8 is bottom-right, in row-major order.
 *
 * Stand-alone function (not a method on a `Cell` type) because the board
 * representation differs by layer — ViewModels use `SudokuGrid`, the
 * a11y helpers use a flat `(row, col)` pair, the iOS bridge uses Swift
 * structs. All three converge on this lookup.
 */
fun boxIndexOf(row: Int, col: Int): Int {
    require(row in BoardConstants.INDEX_RANGE) { "row must be in ${BoardConstants.INDEX_RANGE}, was $row" }
    require(col in BoardConstants.INDEX_RANGE) { "col must be in ${BoardConstants.INDEX_RANGE}, was $col" }
    return (row / BoardConstants.BOX_SIZE) * BoardConstants.BOX_SIZE + (col / BoardConstants.BOX_SIZE)
}

/**
 * Whether two cells share a row, column, or box. Used by the auto-notes
 * "remove peer notes" logic in `GameViewModel` and by the hint scanner.
 * A cell is **not** a peer of itself.
 */
fun arePeers(r1: Int, c1: Int, r2: Int, c2: Int): Boolean {
    if (r1 == r2 && c1 == c2) return false
    if (r1 == r2 || c1 == c2) return true
    return (r1 / BoardConstants.BOX_SIZE == r2 / BoardConstants.BOX_SIZE) &&
        (c1 / BoardConstants.BOX_SIZE == c2 / BoardConstants.BOX_SIZE)
}
