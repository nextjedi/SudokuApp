package com.nextjedi.sudokustreak.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nextjedi.sudokustreak.android.ui.theme.Blue100
import com.nextjedi.sudokustreak.android.ui.theme.Blue500
import com.nextjedi.sudokustreak.android.ui.theme.Navy
import com.nextjedi.sudokustreak.android.ui.theme.SolverYellow
import com.nextjedi.sudokustreak.model.SudokuGrid

@Composable
fun SudokuGrid(
    grid: SudokuGrid,
    selectedCell: Pair<Int, Int>?,
    onCellClick: (Int, Int) -> Unit,
    highlightEnabled: Boolean,
    solverHintCells: List<Pair<Int, Int>>,
    solverFillingCell: Pair<Int, Int>?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .border(2.dp, Navy)
    ) {
        for (row in 0 until 9) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 9) {
                    val cell = grid[row][col]
                    val isSelected = selectedCell?.first == row && selectedCell.second == col
                    val isHinted = solverHintCells.any { it.first == row && it.second == col }
                    val isFilling = solverFillingCell?.first == row && solverFillingCell.second == col
                    val isSameRowCol = highlightEnabled && selectedCell != null &&
                            (selectedCell.first == row || selectedCell.second == col)
                    val isSameBox = highlightEnabled && selectedCell != null &&
                            (row / 3 == selectedCell.first / 3 && col / 3 == selectedCell.second / 3)

                    val bgColor = when {
                        isSelected -> Blue500.copy(alpha = 0.35f)
                        isFilling -> SolverYellow
                        isHinted -> SolverYellow.copy(alpha = 0.6f)
                        isSameRowCol || isSameBox -> Blue100.copy(alpha = 0.5f)
                        else -> Color.White
                    }

                    val rightBorder: Dp = if (col % 3 == 2 && col != 8) 2.dp else 0.5.dp
                    val bottomBorder: Dp = if (row % 3 == 2 && row != 8) 2.dp else 0.5.dp

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .background(bgColor)
                            .border(
                                width = 0.5.dp,
                                color = Navy.copy(alpha = 0.3f)
                            )
                            .then(
                                if (col % 3 == 2 && col != 8)
                                    Modifier.border(
                                        width = rightBorder,
                                        color = Navy
                                    ) else Modifier
                            )
                            .then(
                                if (row % 3 == 2 && row != 8)
                                    Modifier.border(
                                        width = bottomBorder,
                                        color = Navy
                                    ) else Modifier
                            )
                            .clickable { onCellClick(row, col) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (cell.value != 0) {
                            Text(
                                text = cell.value.toString(),
                                fontSize = 18.sp,
                                fontWeight = if (cell.isInitial) FontWeight.Bold else FontWeight.Normal,
                                color = when {
                                    cell.isSolverFilled -> Blue500
                                    cell.isInitial -> Navy
                                    else -> Blue500.copy(alpha = 0.8f)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
