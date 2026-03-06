package com.nextjedi.sudokustreak.engine

import com.nextjedi.sudokustreak.model.SudokuGrid

class SudokuValidator {

    fun isValidMove(grid: Array<IntArray>, row: Int, col: Int, num: Int): Boolean {
        // Check row
        for (x in 0 until 9) {
            if (grid[row][x] == num) return false
        }
        // Check column
        for (x in 0 until 9) {
            if (grid[x][col] == num) return false
        }
        // Check 3x3 box
        val startRow = (row / 3) * 3
        val startCol = (col / 3) * 3
        for (i in 0 until 3) {
            for (j in 0 until 3) {
                if (grid[startRow + i][startCol + j] == num) return false
            }
        }
        return true
    }

    fun getCandidates(grid: Array<IntArray>, row: Int, col: Int): List<Int> =
        (1..9).filter { isValidMove(grid, row, col, it) }

    fun isComplete(grid: SudokuGrid): Boolean =
        grid.all { row -> row.all { cell -> cell.value != 0 } }

    fun isValidSolution(grid: SudokuGrid): Boolean {
        val nums = (1..9).toSet()
        // Check rows
        for (row in grid) {
            if (row.map { it.value }.toSet() != nums) return false
        }
        // Check columns
        for (col in 0 until 9) {
            if ((0 until 9).map { grid[it][col].value }.toSet() != nums) return false
        }
        // Check 3x3 boxes
        for (boxRow in 0 until 3) {
            for (boxCol in 0 until 3) {
                val values = mutableSetOf<Int>()
                for (i in 0 until 3) {
                    for (j in 0 until 3) {
                        values.add(grid[boxRow * 3 + i][boxCol * 3 + j].value)
                    }
                }
                if (values != nums) return false
            }
        }
        return true
    }
}
