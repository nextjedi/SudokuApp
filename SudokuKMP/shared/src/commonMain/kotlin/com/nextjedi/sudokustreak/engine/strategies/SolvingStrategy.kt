package com.nextjedi.sudokustreak.engine.strategies

import com.nextjedi.sudokustreak.engine.SudokuValidator
import com.nextjedi.sudokustreak.model.SolverStep
import com.nextjedi.sudokustreak.model.SudokuGrid

interface SolvingStrategy {
    val name: String
    fun findStep(grid: SudokuGrid, validator: SudokuValidator): SolverStep?
}
