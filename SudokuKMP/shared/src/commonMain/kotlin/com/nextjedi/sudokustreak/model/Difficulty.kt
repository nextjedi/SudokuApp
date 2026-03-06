package com.nextjedi.sudokustreak.model

enum class Difficulty(val label: String, val clues: Int, val cellsToRemove: Int) {
    EASY("Easy", 46, 35),
    MEDIUM("Medium", 36, 45),
    HARD("Hard", 26, 55)
}
