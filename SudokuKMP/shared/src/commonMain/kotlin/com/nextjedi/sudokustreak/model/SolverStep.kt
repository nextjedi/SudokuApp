package com.nextjedi.sudokustreak.model

data class SolverStep(
    val row: Int,
    val col: Int,
    val value: Int,
    val strategy: String,
    val reasoning: String
)
