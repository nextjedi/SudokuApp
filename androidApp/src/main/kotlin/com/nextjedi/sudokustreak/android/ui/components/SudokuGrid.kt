package com.nextjedi.sudokustreak.android.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nextjedi.sudokustreak.android.a11y.BorderStyle
import com.nextjedi.sudokustreak.android.a11y.borderStyleFor
import com.nextjedi.sudokustreak.android.a11y.cellSemantics
import com.nextjedi.sudokustreak.android.a11y.dashIntervalsPx
import com.nextjedi.sudokustreak.android.a11y.gridContainerSemantics
import com.nextjedi.sudokustreak.android.a11y.largeTextScaledSp
import com.nextjedi.sudokustreak.android.ui.theme.BrandBorderLightFaint
import com.nextjedi.sudokustreak.android.ui.theme.BrandErrorRed
import com.nextjedi.sudokustreak.android.ui.theme.BrandGridBoxBorder
import com.nextjedi.sudokustreak.android.ui.theme.BrandNavyText
import com.nextjedi.sudokustreak.android.ui.theme.BrandNotesGray
import com.nextjedi.sudokustreak.android.ui.theme.BrandPrimary
import com.nextjedi.sudokustreak.android.ui.theme.BrandPrimaryContainerLight
import com.nextjedi.sudokustreak.android.ui.theme.BrandSolverFillingYellow
import com.nextjedi.sudokustreak.android.ui.theme.BrandSolverHintAmber
import com.nextjedi.sudokustreak.model.SudokuCell

/**
 * Phase 5.5 rewrite of the Sudoku grid with **full per-cell screen-reader
 * semantics** + **dashed border colour-blind overlay** + **dynamic-type
 * scaling**.
 *
 * Replaces the Canvas-only legacy grid that Lena Hoffmann's audit flagged as
 * "the worst a11y bug in the app" (no per-cell semantic tree → blind users
 * could not navigate the puzzle). Per Lena's J-7:
 *
 *     The previous Canvas-only grid had no per-cell semantic tree at all — a
 *     blind user could not navigate the puzzle.
 *
 * Accepts the **shared-module `SudokuCell`** directly so GameScreen.kt can
 * pass `state.grid` without translation. The internal renderer projects each
 * cell into a tiny [Cell] view-model for the per-cell composable; that nested
 * shape is kept private so external call sites depend on `SudokuCell` only.
 *
 * The `currentSolverPhase` / `currentStep` parameters are accepted but not
 * read by the Phase-5.5 a11y rewrite — they are kept on the signature so the
 * upstream `GameScreen` keeps compiling while the Wave-3 solver-animation
 * agent migrates the elimination / fill animations onto the new grid. Adding
 * them now also means the next agent only has to touch this file once.
 *
 * ## Semantics layout
 *
 *  - Container: `gridContainerSemantics(label = "Sudoku grid")` →
 *    `LiveRegionMode.Polite` so pause / completion announcements ride the same
 *    semantic channel.
 *  - Each Box: `cellSemantics(...)` → contentDescription "Row R, Column C, Box
 *    B" + stateDescription + Role.Button + onClick action label "Place digit".
 *
 * ## Dashed-border overlay (color-blind safety)
 *
 *  - Selected → solid 2 dp.
 *  - Hint cell → 2 dp dash (8 px / 4 px gap).
 *  - Error cell → 2 dp dash (3 px / 3 px gap, tighter = urgency).
 *  - Neutral → 0.5 dp solid (the standard cell grid).
 *
 * Per Lena's J-13 + audit row 2: hue-only state indication fails the WCAG
 * 1.4.1 Use of Color SC, even when our deuteranopia/protanopia palettes are
 * active. The dashes carry the state information via shape, not colour.
 *
 * ## Dynamic-type
 *
 * The digit font size scales by both (a) the system fontScale (capped at
 * 1.5×) and (b) `AppSettings.largeText` (×1.2 when on). See
 * [largeTextScaledSp] in the a11y module.
 */
@Composable
fun SudokuGrid(
    grid: List<List<SudokuCell>>,
    selectedCell: Pair<Int, Int>?,
    onCellClick: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
    highlightEnabled: Boolean = true,
    solverHintCells: List<Pair<Int, Int>> = emptyList(),
    solverFillingCell: Pair<Int, Int>? = null,
    errorCells: List<Pair<Int, Int>> = emptyList(),
    largeText: Boolean = false,
    @Suppress("UNUSED_PARAMETER") currentSolverPhase: Any? = null,
    @Suppress("UNUSED_PARAMETER") currentStep: Any? = null,
) {
    require(grid.size == 9 && grid.all { it.size == 9 }) {
        "SudokuGrid expects a 9x9 grid, got ${grid.size}x${grid.firstOrNull()?.size}"
    }

    // Capture the hint / error / filling lookups once per recomposition so the
    // per-cell loop doesn't re-scan the list 81 times.
    val hintSet = remember(solverHintCells) { solverHintCells.toHashSet() }
    val errorSet = remember(errorCells) { errorCells.toHashSet() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .gridContainerSemantics()
            // Outer border bumped 2dp → 3dp solid navy so the grid frame reads
            // as the strongest line in the hierarchy.
            .border(3.dp, BrandNavyText)
            .testTag(TAG_GRID)
    ) {
        for (row in 0 until 9) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 9) {
                    val sharedCell = grid[row][col]
                    val cell = sharedCell.toRenderCell()
                    val isSelected = selectedCell?.first == row && selectedCell.second == col
                    val isInHint = hintSet.contains(row to col)
                    val isError = errorSet.contains(row to col)
                    val isFilling = solverFillingCell?.first == row && solverFillingCell.second == col
                    val isSameRowCol = highlightEnabled && selectedCell != null &&
                        (selectedCell.first == row || selectedCell.second == col)
                    val isSameBox = highlightEnabled && selectedCell != null &&
                        (row / 3 == selectedCell.first / 3 && col / 3 == selectedCell.second / 3)

                    val bgColor = when {
                        isSelected -> BrandPrimary.copy(alpha = 0.35f)
                        isFilling -> BrandSolverFillingYellow
                        isInHint -> BrandSolverHintAmber.copy(alpha = 0.6f)
                        isError -> BrandErrorRed.copy(alpha = 0.18f)
                        isSameRowCol || isSameBox -> BrandPrimaryContainerLight.copy(alpha = 0.5f)
                        else -> Color.Transparent
                    }

                    SudokuCellBox(
                        row = row,
                        col = col,
                        cell = cell,
                        bgColor = bgColor,
                        isSelected = isSelected,
                        isInHint = isInHint,
                        isError = isError,
                        largeText = largeText,
                        onCellClick = onCellClick,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/**
 * One individual cell. Split out for testability — `gridCell_*` tests can
 * inflate a single cell without booting the full 9 × 9 grid.
 */
@Composable
fun SudokuCellBox(
    row: Int,
    col: Int,
    cell: Cell,
    bgColor: Color,
    isSelected: Boolean,
    isInHint: Boolean,
    isError: Boolean,
    largeText: Boolean,
    onCellClick: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cellTag = cellTestTag(row, col)
    val borderStyle = borderStyleFor(isSelected, isInHint, isError)

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(bgColor)
            // Cell-level border bumped from 0.5dp → 1dp for clarity on real
            // devices; #E0E0E0 stays as the faint line that separates cells
            // WITHIN a 3×3 box.
            .border(1.dp, BrandBorderLightFaint)
            // 3×3 box separators drawn as a single secondary-coloured line on
            // the right edge of col 2 / col 5 and bottom edge of row 2 / row 5.
            // This replaces the previous Modifier.border(2.dp) approach that
            // was drawing a thick frame around the WHOLE cell (all 4 sides),
            // not just the box-boundary side.
            .gridBoxSeparator(row, col)
            .cellSemantics(
                row = row,
                col = col,
                value = cell.value,
                isGiven = cell.isGiven,
                notes = cell.notes,
                onCellClick = if (!cell.isGiven) {
                    { onCellClick(row, col) }
                } else null,
            )
            .let { base ->
                if (!cell.isGiven) base.clickable { onCellClick(row, col) } else base
            }
            .testTag(cellTag),
        contentAlignment = Alignment.Center,
    ) {
        // Dashed-border overlay — see [BorderStyle]. We draw on top of the
        // already-applied solid border so the pattern is visible even when the
        // hue layer (BrandPrimary on selection, BrandErrorRed on error) is
        // suppressed by the user's color-blind palette.
        if (borderStyle is BorderStyle.LongDash || borderStyle is BorderStyle.ShortDash) {
            val intervals = borderStyle.dashIntervalsPx()
            val strokeColor = if (isError) BrandErrorRed else BrandPrimary
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(
                    color = strokeColor,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = intervals?.let { PathEffect.dashPathEffect(it, 0f) },
                    ),
                )
            }
        }

        // Selected — solid 2 dp outline drawn via Canvas (matches dash overlay
        // layout) so selection is visible above all other layers.
        if (isSelected) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(
                    color = BrandPrimary,
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
        }

        when {
            cell.value != 0 -> {
                Text(
                    text = cell.value.toString(),
                    fontSize = largeTextScaledSp(22.sp, largeText),
                    fontWeight = if (cell.isGiven) FontWeight.Bold else FontWeight.Normal,
                    color = if (cell.isGiven) BrandNavyText else BrandPrimary,
                )
            }
            cell.notes.isNotEmpty() -> {
                NotesGrid(cell.notes, largeText)
            }
        }
    }
}

/**
 * Draws the secondary 3×3-box separator on the right edge (when col == 2 or 5)
 * and/or the bottom edge (when row == 2 or 5) of a cell.
 *
 * Uses [Modifier.drawBehind] with explicit [drawLine] calls so the line lands
 * **only on the box-boundary side**, not all four sides of the cell. The
 * previous implementation used [Modifier.border] which draws a full frame
 * around the cell, producing a ghost-frame on cells at the box corners.
 *
 * Layout choice rationale:
 *  - 2.5dp width gives the box line ~2.5× the visual weight of the 1dp cell
 *    line ([BrandBorderLightFaint]) — enough contrast at typical device DPI
 *    without overpowering the 3dp outer frame ([BrandNavyText]).
 *  - [BrandGridBoxBorder] (#455A75) is a saturated blue-gray distinct from
 *    both the cell border (light gray) and the outer/selection border (navy /
 *    primary blue) — passes WCAG AA Non-text contrast (≥3:1) on white.
 */
private fun Modifier.gridBoxSeparator(row: Int, col: Int): Modifier = drawBehind {
    val strokePx = 2.5.dp.toPx()
    // Right-edge: between box-columns 0|1 (after col 2) and 1|2 (after col 5).
    if (col % 3 == 2 && col != 8) {
        drawLine(
            color = BrandGridBoxBorder,
            start = Offset(size.width - strokePx / 2f, 0f),
            end = Offset(size.width - strokePx / 2f, size.height),
            strokeWidth = strokePx,
        )
    }
    // Bottom-edge: between box-rows 0|1 (after row 2) and 1|2 (after row 5).
    if (row % 3 == 2 && row != 8) {
        drawLine(
            color = BrandGridBoxBorder,
            start = Offset(0f, size.height - strokePx / 2f),
            end = Offset(size.width, size.height - strokePx / 2f),
            strokeWidth = strokePx,
        )
    }
}

/**
 * 3×3 pencil-mark grid rendered inside a cell. Each note digit is its own
 * tiny Text so screen readers can theoretically read them, though the parent
 * cell's [cellSemantics] already includes them in its state description so
 * we suppress the children with `clearAndSetSemantics` indirectly via the
 * parent's `mergeDescendants = false` setting.
 */
@Composable
private fun NotesGrid(notes: Set<Int>, largeText: Boolean) {
    val noteSize = largeTextScaledSp(9.sp, largeText)
    Column(modifier = Modifier.fillMaxSize().padding(2.dp)) {
        for (rowIdx in 0 until 3) {
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                for (colIdx in 0 until 3) {
                    val n = rowIdx * 3 + colIdx + 1
                    Box(
                        modifier = Modifier.weight(1f).fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (notes.contains(n)) {
                            Text(
                                text = n.toString(),
                                fontSize = noteSize,
                                color = BrandNotesGray,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---- Value objects ----

/**
 * Render-side projection of a Sudoku cell — what the grid needs to draw, no
 * less and no more. Callers translate their `GameViewModel`'s domain cell into
 * this shape via [SudokuCell.toRenderCell]; the decoupling keeps the grid
 * free of solver-state fields it doesn't render and gives unit tests a
 * minimal-dependency value object.
 *
 * @param value 0 = empty, 1..9 = filled
 * @param isGiven true if part of the original puzzle (locked)
 * @param notes pencil marks for an empty cell
 */
data class Cell(
    val value: Int = 0,
    val isGiven: Boolean = false,
    val notes: Set<Int> = emptySet(),
) {
    init {
        require(value in 0..9) { "value must be 0..9, was $value" }
        require(notes.all { it in 1..9 }) { "notes must be in 1..9, was $notes" }
    }
}

/**
 * Project the shared-module's [SudokuCell] onto the render-side [Cell]. We
 * map `isInitial` (the legacy "given clue" flag) → `isGiven` so the a11y
 * stateDescription reads naturally ("Locked given clue, value 7").
 */
fun SudokuCell.toRenderCell(): Cell =
    Cell(value = value, isGiven = isInitial, notes = notes)

/** Synonym for an empty 9×9 grid — convenient for unit-test fixtures. */
fun emptyRenderGrid(): List<List<Cell>> = List(9) { List(9) { Cell() } }

// ---- testTags ----

/** Test tag for the outer grid container. */
const val TAG_GRID: String = "sudoku_grid"

/**
 * Stable test tag for a single cell — used by `gridCell_*` tests in
 * `AccessibilityFeaturesTest.kt` and by Lena's hand-curated TalkBack walk.
 */
fun cellTestTag(row: Int, col: Int): String = "cell_${row}_${col}"
