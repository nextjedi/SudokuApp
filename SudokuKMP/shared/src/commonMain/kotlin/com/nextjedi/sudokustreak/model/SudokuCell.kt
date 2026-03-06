package com.nextjedi.sudokustreak.model

data class SudokuCell(
    val value: Int = 0,
    val isInitial: Boolean = false,
    val notes: List<Int> = emptyList(),
    val isSolverFilled: Boolean = false
)

typealias SudokuGrid = List<List<SudokuCell>>

fun emptyGrid(): SudokuGrid = List(9) { List(9) { SudokuCell() } }

fun SudokuGrid.toMutableGrid(): Array<Array<SudokuCell>> =
    Array(9) { r -> Array(9) { c -> this[r][c] } }

fun Array<Array<SudokuCell>>.toGrid(): SudokuGrid =
    List(9) { r -> List(9) { c -> this[r][c] } }

fun SudokuGrid.toIntGrid(): Array<IntArray> =
    Array(9) { r -> IntArray(9) { c -> this[r][c].value } }
