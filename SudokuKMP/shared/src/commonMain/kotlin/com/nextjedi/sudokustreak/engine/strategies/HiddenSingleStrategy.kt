package com.nextjedi.sudokustreak.engine.strategies

import com.nextjedi.sudokustreak.engine.SudokuValidator
import com.nextjedi.sudokustreak.model.SolverStep
import com.nextjedi.sudokustreak.model.SudokuGrid
import com.nextjedi.sudokustreak.model.toIntGrid

class HiddenSingleStrategy : SolvingStrategy {

    override val name = "Hidden Single"

    override fun findStep(grid: SudokuGrid, validator: SudokuValidator): SolverStep? {
        val intGrid = grid.toIntGrid()

        // Check rows
        for (row in 0 until 9) {
            val candidateMap = Array(10) { mutableListOf<Int>() }
            for (col in 0 until 9) {
                if (grid[row][col].value == 0 && !grid[row][col].isInitial) {
                    validator.getCandidates(intGrid, row, col).forEach { candidateMap[it].add(col) }
                }
            }
            for (num in 1..9) {
                if (candidateMap[num].size == 1) {
                    val col = candidateMap[num][0]
                    if (!grid[row][col].isInitial) {
                        return SolverStep(row, col, num, name,
                            "[$name] $num can only go in cell (${row + 1},${col + 1}) within its row.")
                    }
                }
            }
        }

        // Check columns
        for (col in 0 until 9) {
            val candidateMap = Array(10) { mutableListOf<Int>() }
            for (row in 0 until 9) {
                if (grid[row][col].value == 0 && !grid[row][col].isInitial) {
                    validator.getCandidates(intGrid, row, col).forEach { candidateMap[it].add(row) }
                }
            }
            for (num in 1..9) {
                if (candidateMap[num].size == 1) {
                    val row = candidateMap[num][0]
                    if (!grid[row][col].isInitial) {
                        return SolverStep(row, col, num, name,
                            "[$name] $num can only go in cell (${row + 1},${col + 1}) within its column.")
                    }
                }
            }
        }

        // Check 3x3 boxes
        for (box in 0 until 9) {
            val boxRow = (box / 3) * 3
            val boxCol = (box % 3) * 3
            val candidateMap = Array(10) { mutableListOf<Int>() } // stores index 0..8 in box
            var index = 0
            for (i in 0 until 3) {
                for (j in 0 until 3) {
                    val r = boxRow + i; val c = boxCol + j
                    if (grid[r][c].value == 0 && !grid[r][c].isInitial) {
                        validator.getCandidates(intGrid, r, c).forEach { candidateMap[it].add(index) }
                    }
                    index++
                }
            }
            for (num in 1..9) {
                if (candidateMap[num].size == 1) {
                    val idx = candidateMap[num][0]
                    val r = boxRow + idx / 3; val c = boxCol + idx % 3
                    if (!grid[r][c].isInitial) {
                        return SolverStep(r, c, num, name,
                            "[$name] $num can only go in cell (${r + 1},${c + 1}) within its 3×3 box.")
                    }
                }
            }
        }

        return null
    }
}
