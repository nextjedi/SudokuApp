package com.nextjedi.sudokustreak.engine

import com.nextjedi.sudokustreak.model.Difficulty
import com.nextjedi.sudokustreak.model.SudokuCell
import com.nextjedi.sudokustreak.model.SudokuGrid
import kotlin.random.Random

class PuzzleGenerator(private val validator: SudokuValidator) {

    fun generatePuzzle(difficulty: Difficulty): SudokuGrid {
        val complete = generateCompleteGrid()
        return createPuzzleFromComplete(complete, difficulty)
    }

    private fun generateCompleteGrid(): Array<IntArray> {
        val grid = Array(9) { IntArray(9) }
        // Fill diagonal 3x3 boxes first (independent of each other)
        for (box in 0 until 9 step 3) {
            fillBox(grid, box, box)
        }
        solveSudoku(grid)
        return grid
    }

    private fun solveSudoku(grid: Array<IntArray>): Boolean {
        for (row in 0 until 9) {
            for (col in 0 until 9) {
                if (grid[row][col] == 0) {
                    val nums = (1..9).shuffled()
                    for (num in nums) {
                        if (validator.isValidMove(grid, row, col, num)) {
                            grid[row][col] = num
                            if (solveSudoku(grid)) return true
                            grid[row][col] = 0
                        }
                    }
                    return false
                }
            }
        }
        return true
    }

    private fun fillBox(grid: Array<IntArray>, row: Int, col: Int) {
        val nums = (1..9).toMutableList().also { it.shuffle() }
        var index = 0
        for (i in 0 until 3) {
            for (j in 0 until 3) {
                grid[row + i][col + j] = nums[index++]
            }
        }
    }

    private fun createPuzzleFromComplete(
        complete: Array<IntArray>,
        difficulty: Difficulty
    ): SudokuGrid {
        val grid: Array<Array<SudokuCell>> = Array(9) { r ->
            Array(9) { c ->
                SudokuCell(value = complete[r][c], isInitial = true)
            }
        }

        var removed = 0
        while (removed < difficulty.cellsToRemove) {
            val row = Random.nextInt(9)
            val col = Random.nextInt(9)
            if (grid[row][col].value != 0) {
                grid[row][col] = grid[row][col].copy(value = 0, isInitial = false)
                removed++
            }
        }

        return List(9) { r -> List(9) { c -> grid[r][c] } }
    }
}
