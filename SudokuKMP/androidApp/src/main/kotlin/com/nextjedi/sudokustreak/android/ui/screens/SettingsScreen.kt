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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nextjedi.sudokustreak.android.ui.theme.*
import com.nextjedi.sudokustreak.android.viewmodel.SettingsViewModel
import com.nextjedi.sudokustreak.android.viewmodel.StatsViewModel

@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    statsViewModel: StatsViewModel,
    onBack: () -> Unit
) {
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    var showResetDialog by remember { mutableStateOf(false) }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Statistics") },
            text = { Text("Are you sure? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        statsViewModel.resetStats()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed)
                ) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Cancel") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Text("Settings", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Navy)
        Text("Customize Your Experience", fontSize = 16.sp, color = SlateGray,
            modifier = Modifier.padding(bottom = 32.dp))

        // Game Settings
        SettingsSection("Game Settings") {
            ToggleSetting("Sound Effects", "Enable sound feedback",
                settings.soundEnabled) { settingsViewModel.toggleSound() }
            ToggleSetting("Cell Highlighting", "Highlight related cells",
                settings.highlightEnabled) { settingsViewModel.toggleHighlight() }
            ToggleSetting("Show Timer", "Display game timer",
                settings.timerEnabled) { settingsViewModel.toggleTimer() }
        }

        // Solver Settings
        SettingsSection("Solver Settings") {
            SettingRow(label = "Solver Speed", description = "Delay between steps") {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(500 to "0.5s", 1500 to "1.5s", 3000 to "3s").forEach { (ms, label) ->
                        val active = settings.solverSpeedMs == ms
                        OutlinedButton(
                            onClick = { settingsViewModel.setSolverSpeed(ms) },
                            modifier = Modifier.height(36.dp),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (active) Blue500 else Color.White,
                                contentColor = if (active) Color.White else Navy
                            )
                        ) { Text(label, fontSize = 13.sp) }
                    }
                }
            }
            SettingRow(label = "Help Cells", description = "Cells: ${settings.solverHelpCells}") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { settingsViewModel.setSolverHelpCells(settings.solverHelpCells - 1) },
                        enabled = settings.solverHelpCells > 1
                    ) {
                        Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                            color = if (settings.solverHelpCells > 1) Blue500 else BorderMedium)
                    }
                    Text("${settings.solverHelpCells}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Navy)
                    IconButton(
                        onClick = { settingsViewModel.setSolverHelpCells(settings.solverHelpCells + 1) },
                        enabled = settings.solverHelpCells < 9
                    ) {
                        Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                            color = if (settings.solverHelpCells < 9) Blue500 else BorderMedium)
                    }
                }
            }
        }

        // Data
        SettingsSection("Data") {
            OutlinedButton(
                onClick = { showResetDialog = true },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(ErrorRed)
                )
            ) {
                Text("Reset All Statistics", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Sudoku — A Brain Gym  v1.0.0", fontSize = 13.sp, color = SlateGray,
            modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(modifier = Modifier.height(24.dp))

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
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Navy,
        modifier = Modifier.padding(bottom = 12.dp))
    Column(
        modifier = Modifier.padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}

@Composable
private fun ToggleSetting(label: String, description: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Navy)
            Text(description, fontSize = 14.sp, color = SlateGray)
        }
        Switch(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Blue500)
        )
    }
}

@Composable
private fun SettingRow(label: String, description: String, control: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Navy)
            Text(description, fontSize = 14.sp, color = SlateGray)
        }
        control()
    }
}
