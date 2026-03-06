package com.nextjedi.sudokustreak.engine

import com.nextjedi.sudokustreak.engine.strategies.HiddenSingleStrategy
import com.nextjedi.sudokustreak.engine.strategies.NakedSingleStrategy
import com.nextjedi.sudokustreak.engine.strategies.SolvingStrategy
import com.nextjedi.sudokustreak.model.SolverStep
import com.nextjedi.sudokustreak.model.SudokuGrid

class SudokuSolver(
    private val validator: SudokuValidator,
    private val strategies: List<SolvingStrategy> = listOf(
        NakedSingleStrategy(),
        HiddenSingleStrategy()
    )
) {
    fun findNextStep(grid: SudokuGrid): SolverStep? {
        for (strategy in strategies) {
            val step = strategy.findStep(grid, validator)
            if (step != null) return step
        }
        return null
    }
}
