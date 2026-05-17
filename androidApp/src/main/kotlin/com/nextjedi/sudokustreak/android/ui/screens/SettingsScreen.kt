package com.nextjedi.sudokustreak.android.ui.screens

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.nextjedi.sudokustreak.android.a11y.sectionHeadingTag
import com.nextjedi.sudokustreak.android.a11y.sliderIntegerStateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nextjedi.sudokustreak.android.viewmodel.SettingsViewModel
import com.nextjedi.sudokustreak.android.viewmodel.StatsViewModel
import com.nextjedi.sudokustreak.domain.settings.AppSettings
import com.nextjedi.sudokustreak.domain.settings.ColorBlindMode
import com.nextjedi.sudokustreak.domain.settings.StylusMode
import com.nextjedi.sudokustreak.domain.settings.ThemeMode
import kotlinx.coroutines.launch

/**
 * Brain-Gym Settings screen — Wave 2 rewrite.
 *
 * Replaces the 4-section quick screen with the full 8-section M3 layout from
 * REVAMP_PLAN.md §4 + Data & Privacy (P0-13, P0-14) + a searchable filter
 * (TC-A9, TC-A11) + Test-stylus modal (TC-A7) + Delete All My Data confirmation
 * (Camila / GDPR Art. 17).
 *
 * Row implementations:
 * - Toggle  → [ToggleRow] backed by M3 `Switch`.
 * - Slider  → [SliderRow] backed by M3 `Slider` with discrete `steps`.
 * - Stepper → [StepperRow] (current ± pair, preserved for Help cells row).
 * - Segmented → [SegmentedRow] backed by M3 `SingleChoiceSegmentedButtonRow`.
 * - Chip group (mistake limits, solver speed) → [ChipRow] (single-choice).
 * - Button → [ButtonRow] (Test stylus, Replay tutorial, Delete All My Data, About).
 *
 * Search filtering: case-insensitive substring match on each row's label OR
 * description. Sections render only when at least one of their rows matches.
 * Search query is local state (`rememberSaveable`-style behaviour via plain
 * `remember`), which means navigating away + back resets to empty (TC-A11).
 *
 * Accessibility: every interactive row carries a `contentDescription` via
 * `Modifier.semantics` so TalkBack reads label + state + hint. The a11y agent
 * refines the strings in their commit — this is the baseline (TC-A10).
 *
 * @param settingsViewModel typed VM backed by [com.nextjedi.sudokustreak.domain.settings.AppSettingsRepository].
 * @param statsViewModel    legacy `DataStore<Preferences>` VM — kept on the constructor
 *                          for API parity with the prior screen even though the typed
 *                          deletion flow now routes through [SettingsViewModel.deleteAllMyData].
 * @param onBack            navigation callback (TopAppBar back arrow + Back to Home button).
 * @param onReplayTutorial  optional callback wired by Wave 3 onboarding agent; until then
 *                          a no-op shows a toast-style hint via the placeholder dialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    statsViewModel: StatsViewModel,
    onBack: () -> Unit,
    onReplayTutorial: () -> Unit = {},
    onOpenAbout: () -> Unit = {},
) {
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()

    // ---- Local UI state ----
    // remember (not rememberSaveable) — TC-A11 demands the search clears on screen leave.
    var searchQuery by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showStylusTestSheet by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showReplayHint by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    // Match check — pre-compute once per recomposition so each row decides cheaply.
    val matches = remember(searchQuery) { SearchMatcher(searchQuery) }

    // ---- Delete confirmation ----
    if (showDeleteDialog) {
        DeleteAllDataDialog(
            onConfirm = {
                settingsViewModel.deleteAllMyData()
                // Stats VM call is redundant — deleteAllMyData() already routes through
                // PreferencesStatsResetter — but we keep this no-op chain for the legacy
                // statsViewModel reference (avoids "unused parameter" warnings + makes
                // intent crystal clear in MainActivity wiring).
                @Suppress("UNUSED_EXPRESSION") statsViewModel
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false },
        )
    }

    if (showReplayHint) {
        AlertDialog(
            onDismissRequest = { showReplayHint = false },
            title = { Text("Replay tutorial") },
            text = { Text("Onboarding will be re-shown the next time you start a game. (Hook wired in Wave 3.)") },
            confirmButton = {
                TextButton(onClick = {
                    onReplayTutorial()
                    showReplayHint = false
                }) { Text("OK") }
            },
        )
    }

    // ---- Test-stylus modal ----
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    if (showStylusTestSheet) {
        ModalBottomSheet(
            onDismissRequest = { showStylusTestSheet = false },
            sheetState = sheetState,
            modifier = Modifier.testTag(TAG_STYLUS_TEST_OVERLAY),
        ) {
            StylusTestSheetContent(
                onClose = {
                    scope.launch { sheetState.hide() }
                    showStylusTestSheet = false
                },
            )
        }
    }

    // ---- Scaffold ----
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        CenterAlignedTopAppBar(
            title = { Text("Settings", fontWeight = FontWeight.SemiBold) },
            navigationIcon = {
                IconButton(onClick = onBack, modifier = Modifier.testTag(TAG_BACK_BUTTON)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = { showOverflowMenu = !showOverflowMenu }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                }
            },
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            // ---- Search field ----
            // Plain OutlinedTextField with a Search leading icon; SearchBar is overkill
            // for an inline filter and ships a docked overlay we don't want.
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search settings…") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 12.dp)
                    .testTag(TAG_SEARCH_FIELD),
            )

            // ---- Sections ----
            val visibleSectionCount = listOfNotNull(
                GameplaySection(settings, settingsViewModel, matches),
                StylusSection(settings, settingsViewModel, matches, onTestStylus = {
                    showStylusTestSheet = true
                }),
                SensorsSection(settings, settingsViewModel, matches),
                PresentationSection(settings, settingsViewModel, matches),
                SolverSection(settings, settingsViewModel, matches),
                AccessibilitySection(settings, settingsViewModel, matches),
                AudioSection(settings, settingsViewModel, matches),
                DataPrivacySection(
                    settings = settings,
                    vm = settingsViewModel,
                    matches = matches,
                    onDeleteAll = { showDeleteDialog = true },
                    onReplayTutorial = {
                        // Wave-3 hook — show a hint dialog until the onboarding agent lands.
                        showReplayHint = true
                    },
                    onAbout = onOpenAbout,
                ),
            ).count()

            // Empty state — only show when nothing matches AND a query is active.
            if (visibleSectionCount == 0 && searchQuery.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "No settings match \"$searchQuery\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag(TAG_EMPTY_SEARCH),
                    )
                }
            }

            // ---- Footer ----
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Sudoku Brain Gym v1.0.0  ·  Built with care",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(horizontal = 8.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Back to Home", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // The overflow menu is intentionally an inline branch (no real PopupMenu) — we don't
    // ship any overflow actions in Wave 2. Reserved for "Reset to defaults" / "Diagnostics"
    // in Wave 3. Reading the flag in a no-op suppresses unused-state warnings.
    if (showOverflowMenu) showOverflowMenu = false
}

// =========================================================================
//                              S E C T I O N S
// =========================================================================
//
// Each section is implemented as a Composable that returns Unit when it has at least
// one matching row (so callers can count visible sections via `listOfNotNull(...)`).
// The matcher is passed in; rows that don't match are simply not emitted.
//
// A section returns `Unit` (so it appears in `listOfNotNull(...)`) only when at least
// one row matched. When nothing matches, it returns `null`.

@Composable
private fun GameplaySection(
    settings: AppSettings,
    vm: SettingsViewModel,
    matches: SearchMatcher,
): Unit? {
    val rows = SectionBuilder(matches)
        .toggle("Auto notes", "Auto-fill pencil marks as you play", settings.autoNotesEnabled,
            tag = "toggle_autoNotesEnabled", rowTag = "row_autoNotesEnabled") {
            vm.setAutoNotes(it)
        }
        .toggle("Fast pencil mode", "Long-press to drop a pencil mark", settings.fastPencilEnabled,
            tag = "toggle_fastPencilEnabled", rowTag = "row_fastPencilEnabled") {
            vm.setFastPencil(it)
        }
        .toggle("Number First mode", "Pick a digit, then tap empty cells", settings.numberFirstModeEnabled,
            tag = "toggle_numberFirstModeEnabled", rowTag = "row_numberFirstModeEnabled") {
            vm.setNumberFirstMode(it)
        }
        .toggle("Mid-Game Boost", "Daily nudge if you stall — OFF by default", settings.midGameBoostEnabled,
            tag = "toggle_midGameBoostEnabled", rowTag = "row_midGameBoostEnabled") {
            vm.setMidGameBoost(it)
        }
        // Mistake limit chip row — special because it's a 4-option chip group with ∞.
        .chip(
            label = "Mistake limit",
            description = "Game ends after this many mistakes",
            rowTag = "row_mistakeLimit",
            options = MISTAKE_OPTIONS,
            selectedValue = settings.mistakeLimit,
            optionLabel = { if (it == Int.MAX_VALUE) "∞" else it.toString() },
            tagFor = { "chip_mistake_${if (it == Int.MAX_VALUE) "inf" else it}" },
        ) { vm.setMistakeLimit(it) }
        .build()

    if (rows.isEmpty()) return null
    SectionShell(title = "Gameplay", rows = rows)
    return Unit
}

@Composable
private fun StylusSection(
    settings: AppSettings,
    vm: SettingsViewModel,
    matches: SearchMatcher,
    onTestStylus: () -> Unit,
): Unit? {
    val rows = SectionBuilder(matches)
        .segmented(
            label = "Use stylus",
            description = stylusSubStatus(settings),
            rowTag = "row_stylusMode",
            options = StylusMode.values().toList(),
            selected = settings.stylusMode,
            optionLabel = { mode ->
                when (mode) {
                    StylusMode.AUTO -> "Auto"
                    StylusMode.ALWAYS -> "Always"
                    StylusMode.NEVER -> "Off"
                }
            },
            tagFor = { "seg_stylus_${it.name}" },
        ) { vm.setStylusMode(it) }
        .slider(
            label = "Recognition confidence",
            description = "Higher = stricter (default 75%)",
            value = settings.stylusConfidenceThreshold,
            valueRange = 0.6f..0.9f,
            steps = 5,  // 0.60 / 0.65 / 0.70 / 0.75 / 0.80 / 0.85 / 0.90
            tag = "slider_stylusConfidence",
            rowTag = "row_stylusConfidence",
            // Phase 5.5 a11y: render as percentage rather than float so TalkBack
            // announces "75 percent" instead of "0.75". Matches J-4 audit row.
            valueLabel = { "${(it * 100).toInt()} percent" },
        ) { vm.setStylusConfidence(it) }
        .toggle(
            label = "Pressure → bold notes",
            description = "Press harder to mark a confident note",
            checked = settings.stylusPressureToBoldNotes,
            tag = "toggle_stylusPressureToBoldNotes",
            rowTag = "row_stylusPressureToBoldNotes",
        ) { vm.setStylusPressureBold(it) }
        .toggle(
            label = "Wrist rejection",
            description = "Ignore palm touches while drawing",
            checked = settings.stylusWristRejection,
            tag = "toggle_stylusWristRejection",
            rowTag = "row_stylusWristRejection",
        ) { vm.setStylusWristRejection(it) }
        .sliderInt(
            label = "Stroke commit delay",
            description = "Wait this long after the last stroke before recognising",
            value = settings.stylusDebounceMs,
            valueRange = 250..400,
            stepSize = 25,
            tag = "slider_stylusDebounceMs",
            rowTag = "row_stylusDebounceMs",
            valueLabel = { "${it} ms" },
        ) { vm.setStylusDebounceMs(it) }
        .button(
            label = "Test stylus",
            description = "Open a 320dp canvas to verify pen recognition",
            buttonLabel = "Open test",
            tag = "button_test_stylus",
            rowTag = "row_test_stylus",
        ) { onTestStylus() }
        .build()

    if (rows.isEmpty()) return null
    SectionShell(title = "Stylus & Pencil", rows = rows)
    return Unit
}

@Composable
private fun SensorsSection(
    settings: AppSettings,
    vm: SettingsViewModel,
    matches: SearchMatcher,
): Unit? {
    val rows = SectionBuilder(matches)
        .toggle(
            label = "Auto-pause face down",
            description = "Pause the timer when the device is flipped over",
            checked = settings.proximityAutoPauseEnabled,
            tag = "toggle_proximityAutoPauseEnabled",
            rowTag = "row_proximityAutoPauseEnabled",
        ) { vm.setProximityAutoPause(it) }
        .toggle(
            label = "Auto-dim in dark room",
            description = "Match the ambient-light sensor",
            checked = settings.ambientLightAutoThemeEnabled,
            tag = "toggle_ambientLightAutoThemeEnabled",
            rowTag = "row_ambientLightAutoThemeEnabled",
        ) { vm.setAmbientLightAutoTheme(it) }
        .toggle(
            label = "Tilt parallax",
            description = "Subtle 3D depth on the grid (off when Reduce Motion is on)",
            checked = settings.tiltParallaxEnabled,
            tag = "toggle_tiltParallaxEnabled",
            rowTag = "row_tiltParallaxEnabled",
            enabled = !settings.reduceMotion,
        ) { vm.setTiltParallax(it) }
        .build()

    if (rows.isEmpty()) return null
    SectionShell(title = "Sensors", rows = rows)
    return Unit
}

@Composable
private fun PresentationSection(
    settings: AppSettings,
    vm: SettingsViewModel,
    matches: SearchMatcher,
): Unit? {
    val rows = SectionBuilder(matches)
        .segmented(
            label = "Theme",
            description = "Match the system, or pick a fixed look",
            rowTag = "row_themeMode",
            options = ThemeMode.values().toList(),
            selected = settings.themeMode,
            optionLabel = {
                when (it) {
                    ThemeMode.SYSTEM -> "System"
                    ThemeMode.LIGHT -> "Light"
                    ThemeMode.DARK -> "Dark"
                    ThemeMode.AMOLED -> "AMOLED"
                }
            },
            tagFor = { "chip_theme_${it.name}" },
        ) { vm.setThemeMode(it) }
        // Dynamic colour — Android 12+ only AND off when colour-blind mode is set.
        .toggle(
            label = "Use dynamic color",
            description = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                "Requires Android 12 or newer"
            } else if (settings.colorBlindMode != ColorBlindMode.NONE) {
                "Disabled while a colour-blind palette is active"
            } else {
                "Match the system Material You palette"
            },
            checked = settings.useDynamicColor,
            tag = "toggle_useDynamicColor",
            rowTag = "row_useDynamicColor",
            enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                settings.colorBlindMode == ColorBlindMode.NONE,
        ) { vm.setUseDynamicColor(it) }
        .segmented(
            label = "Color-blind mode",
            description = "Swap amber / teal for higher-contrast palettes",
            rowTag = "row_colorBlindMode",
            options = ColorBlindMode.values().toList(),
            selected = settings.colorBlindMode,
            optionLabel = {
                when (it) {
                    ColorBlindMode.NONE -> "None"
                    ColorBlindMode.DEUTERANOPIA -> "Deuteranopia"
                    ColorBlindMode.PROTANOPIA -> "Protanopia"
                }
            },
            tagFor = { "chip_cb_${it.name}" },
        ) { vm.setColorBlindMode(it) }
        .toggle(
            label = "Highlight cells",
            description = "Tint the active row + column + box",
            checked = settings.highlightEnabled,
            tag = "toggle_highlightEnabled",
            rowTag = "row_highlightEnabled",
        ) { vm.setHighlight(it) }
        .toggle(
            label = "Show remaining count",
            description = "Display how many of each digit remain",
            checked = settings.showRemainingCount,
            tag = "toggle_showRemainingCount",
            rowTag = "row_showRemainingCount",
        ) { vm.setShowRemainingCount(it) }
        .toggle(
            label = "Show timer",
            description = "Display the elapsed time during a game",
            checked = settings.showTimer,
            tag = "toggle_showTimer",
            rowTag = "row_showTimer",
        ) { vm.setShowTimer(it) }
        .toggle(
            label = "Animated digits",
            description = "Bounce + crossfade when filling a cell (off when Reduce Motion is on)",
            checked = settings.animatedDigits,
            tag = "toggle_animatedDigits",
            rowTag = "row_animatedDigits",
            enabled = !settings.reduceMotion,
        ) { vm.setAnimatedDigits(it) }
        .build()

    if (rows.isEmpty()) return null
    SectionShell(title = "Presentation", rows = rows)
    return Unit
}

@Composable
private fun SolverSection(
    settings: AppSettings,
    vm: SettingsViewModel,
    matches: SearchMatcher,
): Unit? {
    val rows = SectionBuilder(matches)
        .sliderInt(
            label = "Hint depth",
            description = "How aggressive solver hints get (1 = subtle, 5 = full reasoning)",
            value = settings.hintDepth,
            valueRange = 1..5,
            stepSize = 1,
            tag = "slider_hintDepth",
            rowTag = "row_hintDepth",
            // Phase 5.5 a11y: "3 of 5" is announced by TalkBack via SliderRow's
            // stateDescription. Lena's J-4 audit row: "Level 3" did not convey the
            // scale endpoint to a blind user (level out of how many?).
            valueLabel = { "$it of 5" },
        ) { vm.setHintDepth(it) }
        .chip(
            label = "Solver speed",
            description = "Animation cadence between solver steps",
            rowTag = "row_solverSpeedMs",
            options = SOLVER_SPEED_OPTIONS.map { it.first },
            selectedValue = settings.solverSpeedMs,
            optionLabel = { ms -> SOLVER_SPEED_OPTIONS.first { it.first == ms }.second },
            tagFor = { ms -> "chip_speed_${ms}" },
        ) { vm.setSolverSpeed(it) }
        .stepper(
            label = "Help cells",
            description = "How many cells the solver fills on demand",
            value = settings.solverHelpCells,
            range = 1..9,
            tag = "stepper_solverHelpCells",
            rowTag = "row_solverHelpCells",
        ) { vm.setSolverHelpCells(it) }
        .build()

    if (rows.isEmpty()) return null
    SectionShell(title = "Solver & Hints", rows = rows)
    return Unit
}

@Composable
private fun AccessibilitySection(
    settings: AppSettings,
    vm: SettingsViewModel,
    matches: SearchMatcher,
): Unit? {
    val rows = SectionBuilder(matches)
        .toggle(
            label = "Reduce motion",
            description = "Skip transitions; dim animated effects",
            checked = settings.reduceMotion,
            tag = "toggle_reduceMotion",
            rowTag = "row_reduceMotion",
        ) { vm.setReduceMotion(it) }
        .toggle(
            label = "High contrast",
            description = "Strengthen borders and text contrast",
            checked = settings.highContrast,
            tag = "toggle_highContrast",
            rowTag = "row_highContrast",
        ) { vm.setHighContrast(it) }
        .toggle(
            label = "Large text",
            description = "Boost the body type scale by one step",
            checked = settings.largeText,
            tag = "toggle_largeText",
            rowTag = "row_largeText",
        ) { vm.setLargeText(it) }
        .build()

    if (rows.isEmpty()) return null
    SectionShell(title = "Accessibility", rows = rows)
    return Unit
}

@Composable
private fun AudioSection(
    settings: AppSettings,
    vm: SettingsViewModel,
    matches: SearchMatcher,
): Unit? {
    val rows = SectionBuilder(matches)
        .toggle(
            label = "Sound effects",
            description = "Tap, fill, completion chimes",
            checked = settings.soundEnabled,
            tag = "toggle_soundEnabled",
            rowTag = "row_soundEnabled",
        ) { vm.setSound(it) }
        .toggle(
            label = "Haptics",
            description = "Tactile feedback on input",
            checked = settings.hapticsEnabled,
            tag = "toggle_hapticsEnabled",
            rowTag = "row_hapticsEnabled",
        ) { vm.setHaptics(it) }
        .toggle(
            label = "Music",
            description = "Ambient background music — OFF by default",
            checked = settings.musicEnabled,
            tag = "toggle_musicEnabled",
            rowTag = "row_musicEnabled",
        ) { vm.setMusic(it) }
        .build()

    if (rows.isEmpty()) return null
    SectionShell(title = "Audio & Haptics", rows = rows)
    return Unit
}

@Composable
private fun DataPrivacySection(
    settings: AppSettings,
    vm: SettingsViewModel,
    matches: SearchMatcher,
    onDeleteAll: () -> Unit,
    onReplayTutorial: () -> Unit,
    onAbout: () -> Unit,
): Unit? {
    val rows = SectionBuilder(matches)
        .toggle(
            label = "Analytics (PostHog)",
            description = "Helps us improve the app. No PII collected. Self-hosted PostHog.",
            checked = settings.analyticsOptIn,
            tag = "toggle_analyticsOptIn",
            rowTag = "row_analyticsOptIn",
        ) { vm.setAnalyticsOptIn(it) }
        .button(
            label = "Replay tutorial",
            description = "Re-show onboarding next launch",
            buttonLabel = "Replay",
            tag = "button_replay_tutorial",
            rowTag = "row_replay_tutorial",
        ) { onReplayTutorial() }
        .button(
            label = "Delete all my data",
            description = "Erases settings, stats, saved games, and replays from this device",
            buttonLabel = "Delete",
            tag = "button_delete_all",
            rowTag = "row_delete_all",
            danger = true,
        ) { onDeleteAll() }
        .button(
            label = "About",
            description = "Version, ML model SHA, links",
            buttonLabel = "Open",
            tag = "button_about",
            rowTag = "row_about",
        ) { onAbout() }
        .build()

    if (rows.isEmpty()) return null
    SectionShell(title = "Data & Privacy", rows = rows)
    return Unit
}

// =========================================================================
//                       S H E L L  +  R E U S A B L E S
// =========================================================================

@Composable
private fun SectionShell(title: String, rows: List<@Composable () -> Unit>) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp)) {
        // Phase 5.5 a11y: mark each section title as a heading (Compose `heading()`
        // semantic) so VoiceOver's rotor + TalkBack's "navigate by heading" gesture
        // jumps section-to-section. The testTag swaps to `heading_*` produced by
        // [sectionHeadingTag] so tests can enumerate the 8 headings deterministically.
        // See AccessibilityFeaturesTest.kt `settingsSection_headersHaveRoleHeading`.
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, top = 4.dp, bottom = 8.dp)
                .semantics {
                    heading()
                    contentDescription = title
                }
                .testTag(sectionHeadingTag(title)),
        )
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                rows.forEachIndexed { idx, row ->
                    row()
                    if (idx != rows.lastIndex) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    toggleTag: String,
    rowTag: String,
) {
    val rowAlpha = if (enabled) 1f else 0.4f
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(rowAlpha)
            .testTag(rowTag)
            .semantics {
                contentDescription = "$label. $description. " +
                    if (checked) "On" else "Off"
            },
        headlineContent = { Text(label) },
        supportingContent = { Text(description, style = MaterialTheme.typography.bodySmall) },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = { if (enabled) onCheckedChange(it) },
                enabled = enabled,
                modifier = Modifier.testTag(toggleTag),
            )
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> SegmentedRow(
    label: String,
    description: String,
    options: List<T>,
    selected: T,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    rowTag: String,
    tagFor: (T) -> String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag(rowTag),
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        Text(
            description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { idx, option ->
                SegmentedButton(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    shape = SegmentedButtonDefaults.itemShape(idx, options.size),
                    modifier = Modifier
                        .testTag(tagFor(option))
                        .semantics {
                            contentDescription = "$label. ${optionLabel(option)}. " +
                                if (option == selected) "Selected" else "Not selected"
                        },
                ) { Text(optionLabel(option)) }
            }
        }
    }
}

@Composable
private fun <T> ChipRow(
    label: String,
    description: String,
    options: List<T>,
    selected: T,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    rowTag: String,
    tagFor: (T) -> String,
) {
    // ChipRow is a drop-in for SegmentedRow but renders as outlined buttons; visually
    // distinguishes "select-one of many" controls like mistake limit or solver speed
    // from binary segmented controls (Auto / Always / Off).
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag(rowTag),
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        Text(
            description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            options.forEach { option ->
                val active = option == selected
                OutlinedButton(
                    onClick = { onSelect(option) },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (active) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surface,
                        contentColor = if (active) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.primary,
                    ),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag(tagFor(option))
                        .semantics {
                            contentDescription = "$label. ${optionLabel(option)}. " +
                                if (active) "Selected" else "Not selected"
                        },
                ) {
                    Text(optionLabel(option), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun SliderRow(
    label: String,
    description: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueLabel: (Float) -> String,
    onValueChange: (Float) -> Unit,
    tag: String,
    rowTag: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag(rowTag),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                valueLabel(value),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(tag)
                .semantics {
                    contentDescription = "$label. ${valueLabel(value)}. $description"
                    // Phase 5.5 a11y: override M3 Slider's default float announcement
                    // ("0.6") with the integer phrasing from valueLabel (e.g. "3 of 5").
                    // Per Lena's J-4 audit row: blind users cannot map 0.6 to its real
                    // meaning. The valueLabel already produces user-friendly text.
                    stateDescription = valueLabel(value)
                    role = Role.Button
                },
        )
    }
}

@Composable
private fun StepperRow(
    label: String,
    description: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    tag: String,
    rowTag: String,
) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(rowTag)
            .semantics { contentDescription = "$label. $value. $description" },
        headlineContent = { Text(label) },
        supportingContent = { Text(description, style = MaterialTheme.typography.bodySmall) },
        trailingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.testTag(tag),
            ) {
                IconButton(
                    onClick = { onValueChange((value - 1).coerceIn(range)) },
                    enabled = value > range.first,
                ) { Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
                Text(
                    "$value",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                IconButton(
                    onClick = { onValueChange((value + 1).coerceIn(range)) },
                    enabled = value < range.last,
                ) { Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
            }
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    )
}

@Composable
private fun ButtonRow(
    label: String,
    description: String,
    buttonLabel: String,
    onClick: () -> Unit,
    danger: Boolean = false,
    tag: String,
    rowTag: String,
) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(rowTag)
            .semantics { contentDescription = "$label. $description. Activates $buttonLabel" },
        headlineContent = { Text(label) },
        supportingContent = { Text(description, style = MaterialTheme.typography.bodySmall) },
        trailingContent = {
            val content = if (danger) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.primary
            OutlinedButton(
                onClick = onClick,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = content),
                modifier = Modifier.testTag(tag),
            ) { Text(buttonLabel) }
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    )
}

@Composable
private fun DeleteAllDataDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(TAG_DELETE_DIALOG),
        title = { Text("Delete all data?") },
        text = {
            Text(
                "This permanently removes settings, stats, saved games, and replays from this device. " +
                    "Cloud backups (if any) are not affected.",
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
                modifier = Modifier.testTag(TAG_DELETE_CONFIRM),
            ) { Text("Delete") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun StylusTestSheetContent(onClose: () -> Unit) {
    // Placeholder content — the stylus agent wires the real canvas + recogniser in
    // their commit. The shell is enough for TC-A7: tap "Test stylus" → modal exists
    // in the semantics tree.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .wrapContentHeight(),
    ) {
        Text(
            "Test stylus",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Write a digit, then lift",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(12.dp))
        // 320dp canvas placeholder.
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 320.dp)
                .testTag("stylus_test_canvas"),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(320.dp)) {
                Text(
                    "(Stylus canvas wired by the stylus agent — TC-AS11)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        // Mocked result row.
        Text(
            "Recognized: 5  ·  Confidence 92%  ·  Latency 84 ms",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.testTag("stylus_test_result"),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
        ) { Text("Close") }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

// =========================================================================
//                              H E L P E R S
// =========================================================================

/**
 * Captured at the call site so each row can ask "do I match?" without re-scanning the
 * query string. When [query] is blank, [matchesText] short-circuits to true so all rows
 * render.
 */
private class SearchMatcher(query: String) {
    private val normalised: String = query.trim().lowercase()
    val isActive: Boolean get() = normalised.isNotEmpty()

    fun matchesText(label: String, description: String): Boolean {
        if (normalised.isEmpty()) return true
        return label.lowercase().contains(normalised) ||
            description.lowercase().contains(normalised)
    }
}

/**
 * Fluent builder for the per-section row list. The builder pattern keeps section
 * composables readable (no nested if-branches per row) and centralises the
 * "if this row matches the search, emit it; otherwise skip" logic.
 */
private class SectionBuilder(private val matches: SearchMatcher) {
    private val rows = mutableListOf<@Composable () -> Unit>()

    fun toggle(
        label: String,
        description: String,
        checked: Boolean,
        tag: String,
        rowTag: String,
        enabled: Boolean = true,
        onChange: (Boolean) -> Unit,
    ): SectionBuilder = apply {
        if (matches.matchesText(label, description)) {
            rows += {
                ToggleRow(label, description, checked, onChange, enabled, tag, rowTag)
            }
        }
    }

    fun <T> segmented(
        label: String,
        description: String,
        rowTag: String,
        options: List<T>,
        selected: T,
        optionLabel: (T) -> String,
        tagFor: (T) -> String,
        onSelect: (T) -> Unit,
    ): SectionBuilder = apply {
        if (matches.matchesText(label, description)) {
            rows += {
                SegmentedRow(label, description, options, selected, optionLabel,
                    onSelect, rowTag, tagFor)
            }
        }
    }

    fun <T> chip(
        label: String,
        description: String,
        rowTag: String,
        options: List<T>,
        selectedValue: T,
        optionLabel: (T) -> String,
        tagFor: (T) -> String,
        onSelect: (T) -> Unit,
    ): SectionBuilder = apply {
        if (matches.matchesText(label, description)) {
            rows += {
                ChipRow(label, description, options, selectedValue, optionLabel,
                    onSelect, rowTag, tagFor)
            }
        }
    }

    fun slider(
        label: String,
        description: String,
        value: Float,
        valueRange: ClosedFloatingPointRange<Float>,
        steps: Int,
        tag: String,
        rowTag: String,
        valueLabel: (Float) -> String,
        onChange: (Float) -> Unit,
    ): SectionBuilder = apply {
        if (matches.matchesText(label, description)) {
            rows += {
                SliderRow(label, description, value, valueRange, steps,
                    valueLabel, onChange, tag, rowTag)
            }
        }
    }

    fun sliderInt(
        label: String,
        description: String,
        value: Int,
        valueRange: IntRange,
        stepSize: Int,
        tag: String,
        rowTag: String,
        valueLabel: (Int) -> String,
        onChange: (Int) -> Unit,
    ): SectionBuilder = apply {
        if (matches.matchesText(label, description)) {
            val floatRange = valueRange.first.toFloat()..valueRange.last.toFloat()
            val span = valueRange.last - valueRange.first
            val stepCount = (span / stepSize) - 1 // M3 Slider exposes steps BETWEEN ends
            rows += {
                SliderRow(
                    label = label,
                    description = description,
                    value = value.toFloat(),
                    valueRange = floatRange,
                    steps = stepCount.coerceAtLeast(0),
                    valueLabel = { valueLabel(it.toInt()) },
                    onValueChange = { f ->
                        // Snap to the nearest stepSize before notifying the VM so we never
                        // persist fractional values.
                        val snapped = (((f - valueRange.first) / stepSize).toInt() * stepSize) +
                            valueRange.first
                        onChange(snapped.coerceIn(valueRange))
                    },
                    tag = tag,
                    rowTag = rowTag,
                )
            }
        }
    }

    fun stepper(
        label: String,
        description: String,
        value: Int,
        range: IntRange,
        tag: String,
        rowTag: String,
        onChange: (Int) -> Unit,
    ): SectionBuilder = apply {
        if (matches.matchesText(label, description)) {
            rows += {
                StepperRow(label, description, value, range, onChange, tag, rowTag)
            }
        }
    }

    fun button(
        label: String,
        description: String,
        buttonLabel: String,
        tag: String,
        rowTag: String,
        danger: Boolean = false,
        onClick: () -> Unit,
    ): SectionBuilder = apply {
        if (matches.matchesText(label, description)) {
            rows += {
                ButtonRow(label, description, buttonLabel, onClick, danger, tag, rowTag)
            }
        }
    }

    fun build(): List<@Composable () -> Unit> = rows.toList()
}

private fun stylusSubStatus(settings: AppSettings): String = when (settings.stylusMode) {
    StylusMode.AUTO -> if (settings.stylusAutoDetected) {
        "Auto-detected: ✓ Active"
    } else {
        "Auto: not detected yet"
    }
    StylusMode.ALWAYS -> "Always on"
    StylusMode.NEVER -> "Off"
}

// =========================================================================
//                              C O N S T A N T S
// =========================================================================

/**
 * Mistake-limit chip options. Sentinel `Int.MAX_VALUE` renders as "∞" and persists as
 * `Int.MAX_VALUE` in the AppSettings field. The migrator preserves this verbatim — the
 * value isn't special-cased at the persistence layer; only the UI maps it to a glyph.
 */
private val MISTAKE_OPTIONS = listOf(1, 3, 5, Int.MAX_VALUE)

private val SOLVER_SPEED_OPTIONS = listOf(
    500 to "Fast",
    1500 to "Normal",
    3000 to "Slow",
)

// ---- testTags exposed as constants so tests don't have to copy the strings ----

internal const val TAG_SEARCH_FIELD = "settings_search"
internal const val TAG_BACK_BUTTON = "settings_back"
internal const val TAG_EMPTY_SEARCH = "settings_empty_search"
internal const val TAG_STYLUS_TEST_OVERLAY = "stylus_test_overlay"
internal const val TAG_DELETE_DIALOG = "dialog_delete_all"
internal const val TAG_DELETE_CONFIRM = "btn_delete_all_confirm"
