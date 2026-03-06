package com.nextjedi.sudokustreak.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nextjedi.sudokustreak.android.ui.theme.*
import com.nextjedi.sudokustreak.android.viewmodel.GameViewModel
import com.nextjedi.sudokustreak.android.viewmodel.StatsViewModel
import com.nextjedi.sudokustreak.model.Difficulty

@Composable
fun HomeScreen(
    gameViewModel: GameViewModel,
    statsViewModel: StatsViewModel,
    onStartGame: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenStats: () -> Unit
) {
    val gameState by gameViewModel.uiState.collectAsStateWithLifecycle()
    val stats by statsViewModel.stats.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        // Title
        Text(
            text = "Sudoku",
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = Navy
        )
        Text(
            text = "A Brain Gym",
            fontSize = 20.sp,
            color = Blue500,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "Train your mind, one puzzle at a time",
            fontSize = 15.sp,
            color = SlateGray,
            modifier = Modifier.padding(top = 8.dp)
        )

        // Streak badge
        if (stats.currentStreak > 0) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .background(StreakOrangeLight, RoundedCornerShape(20.dp))
                    .padding(vertical = 8.dp, horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🔥", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${stats.currentStreak} day streak!",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = StreakOrange
                )
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        // Difficulty selector
        Text(
            text = "Select Difficulty",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = Navy,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Difficulty.entries.forEach { diff ->
            val isSelected = gameState.difficulty == diff
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) Blue100 else Color.White)
                    .border(
                        width = 2.dp,
                        color = if (isSelected) Blue500 else BorderLight,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { gameViewModel.startNewGame(diff) }
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = diff.label,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) Blue500 else Navy
                )
                Text(
                    text = "${diff.clues} clues",
                    fontSize = 14.sp,
                    color = SlateGray
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Start game button
        Button(
            onClick = {
                gameViewModel.startNewGame(gameState.difficulty)
                onStartGame()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Blue500)
        ) {
            Text("New Game", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Secondary buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onOpenStats,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Statistics", fontSize = 15.sp)
            }
            OutlinedButton(
                onClick = onOpenSettings,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Settings", fontSize = 15.sp)
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}
