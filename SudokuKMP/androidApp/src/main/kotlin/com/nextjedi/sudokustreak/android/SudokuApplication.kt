package com.nextjedi.sudokustreak.android

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

val Context.dataStore by preferencesDataStore(name = "sudoku_prefs")

class SudokuApplication : Application()
