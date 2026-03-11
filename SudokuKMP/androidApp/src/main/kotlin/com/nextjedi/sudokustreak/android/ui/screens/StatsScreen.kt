package com.nextjedi.sudokustreak.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nextjedi.sudokustreak.android.ui.theme.*
import com.nextjedi.sudokustreak.android.viewmodel.StatsViewModel

@Composable
fun StatsScreen(
    statsViewModel: StatsViewModel,
    onBack: () -> Unit
) {
    val stats by statsViewModel.stats.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Statistics", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Navy)
        Text("Your Sudoku Journey", fontSize = 16.sp, color = SlateGray,
            modifier = Modifier.padding(bottom = 32.dp))

        // Stats grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard("🔥", "${stats.currentStreak}", "Streak", "days", Modifier.weight(1f))
            StatCard("🎮", "${stats.gamesPlayed}", "Played", "total", Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard("🏆", "${stats.gamesWon}", "Won", "games", Modifier.weight(1f))
            StatCard("📊", "${stats.winRate}%", "Win Rate", "success", Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Best times
        Text("⏱ Best Times", fontSize = 22.sp, fontWeight = FontWeight.SemiBold,
            color = Navy, modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp))

        listOf(
            "Easy" to stats.bestEasySec,
            "Medium" to stats.bestMediumSec,
            "Hard" to stats.bestHardSec
        ).forEach { pair: Pair<String, Long> ->
            val label = pair.first
            val sec = pair.second
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Navy)
                Text(
                    text = if (sec == 0L) "N/A" else formatTime(sec.toInt()),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Blue500
                )
            }
        }

        // Average
        if (stats.averageTimeSec > 0) {
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Blue100, RoundedCornerShape(12.dp))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Average Completion Time", fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold, color = Navy)
                Spacer(modifier = Modifier.height(6.dp))
                Text(formatTime(stats.averageTimeSec.toInt()),
                    fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Blue500)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Back to Home", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun StatCard(
    emoji: String,
    value: String,
    label: String,
    subtext: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji, fontSize = 24.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Blue500,
            textAlign = TextAlign.Center)
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Navy,
            textAlign = TextAlign.Center)
        Text(subtext, fontSize = 12.sp, color = SlateGray, textAlign = TextAlign.Center)
    }
}
