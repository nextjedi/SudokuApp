package com.nextjedi.sudokustreak.engine.strategies

import com.nextjedi.sudokustreak.engine.SudokuValidator
import com.nextjedi.sudokustreak.model.SolverStep
import com.nextjedi.sudokustreak.model.SudokuGrid
import com.nextjedi.sudokustreak.model.toIntGrid

class NakedSingleStrategy : SolvingStrategy {

    override val name = "Naked Single"

    override fun findStep(grid: SudokuGrid, validator: SudokuValidator): SolverStep? {
        val intGrid = grid.toIntGrid()
        for (row in 0 until 9) {
            for (col in 0 until 9) {
                val cell = grid[row][col]
                if (cell.value == 0 && !cell.isInitial) {
                    val candidates = validator.getCandidates(intGrid, row, col)
                    if (candidates.size == 1) {
                        return SolverStep(
                            row = row,
                            col = col,
                            value = candidates[0],
                            strategy = name,
                            reasoning = "[$name] Cell (${row + 1},${col + 1}) can only be ${candidates[0]} — it's the only valid number here."
                        )
                    }
                }
            }
        }
        return null
    }
}
