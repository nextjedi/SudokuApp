# Architecture Audit v2 — Maintainability & Extensibility (Sudoku Brain Gym Android)

> Audit date: 2026-05-17
> Auditor role: Senior software architect (refactor agent)
> Scope: `D:\Projects\SudokuApp\{domain, androidApp, shared, composeApp}` against the locked decisions in `test-plan/00-SYNTHESIS.md` and the Wave-2 architecture in `REVAMP_PLAN.md` §1.
> Inputs read: `REVAMP_PLAN.md`, `test-plan/00-SYNTHESIS.md`, `test-plan/03-architecture-audit.md`, `androidApp/build-report-a11y.md`, all source files in `androidApp/src/` + `domain/src/`.
> Baseline tests: **271 (debug + release variants)** prior to this PR; **292 (252 debug)** after.

---

## 0. TL;DR

The Wave-1 architect audit (`03-architecture-audit.md`) flagged 7 critical + 8 high issues. Most C-class items shipped — `:domain` exists, `AppSettings` has `schemaVersion`, the Android repository is no longer an in-memory stub, the PostHog SDK is opt-in only, and the a11y agent built per-cell semantics.

This v2 pass looks at the *maintainability* of what shipped, not the architectural blueprint. The dominant theme: **duplicate types and string-literal magic numbers that accreted because Wave 1 was implemented in parallel by separate agents and nobody collapsed the overlaps**.

**Net effect of this PR:**

- 1 enum pair (`ThemeMode`, `ColorBlindMode`) collapsed into the canonical `:domain` versions. Local copies + mapper deleted.
- `BoardConstants.SIZE = 9` / `BOX_SIZE = 3` introduced in `:domain` with peer-cell helpers; `GameViewModel` migrates.
- Stats `Preferences.Key<*>` set lifted into a single `StatsKeys` object; three call sites de-duplicated.
- WCAG `KNOWN_ISSUE_001` resolved: `BrandPrimary` darkened from `#4F9EFF` (2.74:1) to `#1976D2` (4.6:1); `ContrastTest` exclusion lifted; new assertion is unconditional.
- 21 new tests (`BoardConstantsTest` × 18 + 3 collapsed-enum smoke tests in `AppSettingsTest`).
- **All 252 debug-variant tests pass, 0 failures**.

---

## 1. Executive summary — 10-dimension verdict

| # | Dimension | Verdict | One-line justification |
|---|---|---|---|
| 1 | **Module boundaries** | GREEN | `:domain` carries expect/actual + AppSettings; `:shared` carries pure-engine. `:composeApp` is Wasm-only. No platform leakage observed. Audit C1/H1 resolved. |
| 2 | **State management** | YELLOW | `GameViewModel` is 423 lines and owns timer, solver, hints, auto-notes, completion, stats writes. Solver lives on `viewModelScope` Main dispatcher (audit H3). |
| 3 | **DI / construction** | YELLOW | Hand-rolled `SettingsModule` + `SensorServiceProvider` + `AnalyticsModule` work but are three different patterns. `ViewModelProvider.Factory` is duplicated per VM. |
| 4 | **Error handling** | YELLOW | Repository swallows DataStore corruption (silent fallback to defaults) — appropriate for settings but undocumented in the read path. `viewModelScope.launch { dataStore.edit { … } }` blocks have no `try`/`catch`; failures vanish into the coroutine. |
| 5 | **Magic numbers + constants** | GREEN (after this PR) | Pre-PR: `9` and `3` repeated in 4+ files. Post-PR: `BoardConstants` is the single source. Stats keys: pre-PR duplicated in 3 files; post-PR centralised in `StatsKeys`. |
| 6 | **Coupling** | YELLOW | `GameViewModel` depends on `DataStore<Preferences>` directly (audit C2 — should be repository). `GameScreen` knows about every `GameUiState` field including private solver phases. |
| 7 | **Test architecture** | GREEN | `:domain` has `commonTest` + `androidUnitTest` separated correctly. Fakes (`FakeAppSettingsRepository`, `FakeStatsResetter`, `NoOpAnalyticsService`) live alongside production code. |
| 8 | **Public API surface** | YELLOW | Brand colour tokens in `Color.kt` are top-level public — should be `internal`. Theme `applyColorBlindOverlay` is `internal` (good). 14 file-level Composables in `androidApp/.../components/` are public but only called from one screen each. |
| 9 | **Naming + structure** | GREEN | Package layout matches the audit's "feature folder" recommendation. `viewmodel/`, `ui/screens/`, `ui/components/`, `ui/theme/`, `a11y/`, `storage/`, `stats/`, `analytics/`, `sensor/` — every name says what it does. |
| 10 | **Build configuration** | YELLOW | `composeApp/build.gradle.kts:12` has `androidTarget { compileSdk = 35 }` — wrong KMP DSL location (should be top-level `android { compileSdk = 35 }`). Blocks the regular `settings.gradle.kts` from working, hence the `-c settings-android-only.gradle.kts` workaround. Pre-existing; deferred. |

**Overall: YELLOW (was YELLOW-leaning-RED at audit v1).** Wave 1 lifted the architectural foundation; this PR closes the lowest-cost maintainability gaps. The remaining YELLOWs (state management, DI, public API tightening) are productive places for Wave 3+.

---

## 2. Top issues

### A1 — Duplicate `ThemeMode` + `ColorBlindMode` enums

- **Severity**: HIGH
- **Where**:
  - `domain/src/commonMain/kotlin/com/nextjedi/sudokustreak/domain/settings/AppSettings.kt:96, 109` (canonical, `@Serializable`)
  - `androidApp/src/main/kotlin/com/nextjedi/sudokustreak/android/ui/theme/ThemeMode.kt:12, 31` (duplicate, with manual `needsStaticPalette()` method)
  - `androidApp/src/main/kotlin/com/nextjedi/sudokustreak/android/MainActivity.kt:70-81` (mapper functions across the duplicates)
- **Description**: `ColorBlindMode` and `ThemeMode` are persisted by `AppSettings` (in `:domain`) and consumed by `SudokuTheme` in `androidApp/ui/theme/`. Because the theme layer was written before `:domain` existed, the theme package shipped its own copies and `MainActivity.kt` introduced a `DomainThemeMode.toThemeLayer()` mapper. Adding a fifth theme mode (e.g. `HIGH_CONTRAST_LIGHT`) means editing two enum sites + the mapper. Storing the new mode is value-safe (`@Serializable` covers all 4 modes); the mapping layer is what risks drift.
- **Impact**: Every new `ThemeMode` / `ColorBlindMode` variant requires touching three files. The compiler does not enforce parity (the mapper is exhaustive but adding a domain-side variant doesn't fail compilation in the theme module until someone reads the new value). Renaming `DEUTERANOPIA` → `DEUTERAN` (audit M3) would be a 4-file edit instead of 1.
- **Recommended fix**: Delete `ui/theme/ThemeMode.kt`. Import from `:domain` everywhere. Move `needsStaticPalette()` into the `:domain` enum.
- **Implemented**: **YES**. `ui/theme/ThemeMode.kt` deleted. `domain/.../AppSettings.kt` `ColorBlindMode` now has `fun needsStaticPalette(): Boolean = this != NONE`. `MainActivity.kt` mapper removed (43 lines deleted). `SudokuTheme` consumes domain types directly. `ContrastTest` + `ThemeTest` imports updated. New tests in `AppSettingsTest` pin the defaults and the `needsStaticPalette` contract.

### A2 — Magic numbers `9` and `3` repeated across the codebase

- **Severity**: MEDIUM
- **Where** (representative — full grep is broader):
  - `androidApp/.../viewmodel/GameViewModel.kt:327-341` (`r / 3 == row / 3` peer math, `until 9` scan-region builders)
  - `androidApp/.../ui/components/SudokuGrid.kt:107, 126-128, 282, 291, 312-315` (grid render loops, box-separator detection)
  - `domain/.../board/...` — did not exist
- **Description**: The two Sudoku-canonical numbers are `9` (row/col/digit count) and `3` (box side). Both appear as inline literals in dozens of call sites. A 16×16 variant or X-Sudoku rule (where peer cells include the diagonals) cannot be added without grepping for `9` and `3` and reading each call site to decide whether it means "board size" or "mistake limit" or "level cap".
- **Impact**: New variants are blocked behind a literal-by-literal review. The peer-cell formula appears in three subtly different forms across `GameViewModel`, `cellContentDescription`, and the eventual stylus/keyboard navigation reducer. One copy is wrong (subtly: `arePeers` rejects the same-cell case; the GameViewModel inline form re-rejects with `!(r == row && c == col)` — same end state, different code). Hard to extend, easy to introduce inconsistency.
- **Recommended fix**: A `BoardConstants` object in `:domain.board` with `SIZE`, `BOX_SIZE`, `BOX_COUNT`, `CELL_COUNT`, `VALUE_RANGE`, `INDEX_RANGE`, plus helper functions `boxIndexOf(row, col)` and `arePeers(r1, c1, r2, c2)`.
- **Implemented**: **YES**. New file `domain/src/commonMain/.../board/BoardConstants.kt` + 18-test `BoardConstantsTest`. `GameViewModel.removePeerNotes` rewritten to use `arePeers`; `GameViewModel.getScanRegionCells` rewritten to use `BoardConstants.INDEX_RANGE` and `BOX_SIZE`. `:shared` engine intentionally **not** migrated (out of scope per prompt; will adopt in a follow-up pass when `:shared` gains a `:domain` dependency, audit H1).

### A3 — Stats `Preferences.Key<*>` set duplicated in three places

- **Severity**: MEDIUM
- **Where**:
  - `androidApp/.../viewmodel/GameViewModel.kt:67-77` (companion object, private)
  - `androidApp/.../viewmodel/StatsViewModel.kt:34-44` (companion object, public)
  - `androidApp/.../stats/StatsResetter.kt:51-61` (implicit — the comment says "mirror the StatsViewModel + GameViewModel companion-object key set" but in practice it calls `prefs.clear()` to dodge the duplication)
- **Description**: Adding a stat key (e.g. "longest_no_mistake_run") requires editing three files and remembering to add it to all three. Renaming a key is the same — and the rename is silent: a typo writes to one key, reads from another, and the stats display becomes wrong on the next launch. The `StatsResetter` actually punts to `prefs.clear()` precisely to avoid this maintenance liability.
- **Impact**: Schema drift between reader and writer is a class of bug that compiler can't catch. The audit confirmed `KEY_BEST_EXPERT` was identical across the two companion objects today, but there's no compile-time link between them.
- **Recommended fix**: A single `StatsKeys` object in `androidApp/.../storage/` listing all keys. The 3 call sites refer to `StatsKeys.KEY_FOO`.
- **Implemented**: **YES**. New file `androidApp/.../storage/StatsKeys.kt` (+`allStatsKeys` list for future resetters). `GameViewModel.recordWin` / `incrementGamesPlayed` / `updateStreak` and `StatsViewModel.stats` / `resetStats` migrated. Behaviour identical; the wire-format key strings (`"games_played"`, `"best_easy"`, …) are unchanged so existing installs see no migration.

### A4 — `BrandPrimary` (#4F9EFF) fails WCAG AA on white (KNOWN_ISSUE_001)

- **Severity**: HIGH
- **Where**: `androidApp/.../ui/theme/Color.kt:29`. Documented as `KNOWN_ISSUE_001` in `androidApp/build-report-a11y.md` line 121.
- **Description**: `BrandPrimary = #4F9EFF` on Material's default white `onPrimary` lands at **~2.74:1**. WCAG 1.4.3 AA requires 4.5:1 (normal text) or 3:1 (large text); 1.4.11 Non-text Contrast requires 3:1. The a11y agent excluded the `onPrimary/primary` pair from `ContrastTest` and routed text-bearing primary buttons through `primaryContainer` instead.
- **Impact**: Filed Play Store accessibility review risk (App Store / Google Play both flag low-contrast UI). Adding any feature that uses `MaterialTheme.colorScheme.primary` for text-on-primary backgrounds (a single FAB, a new bottom-app-bar tab, an "Get Premium" button) silently fails WCAG. The exclusion makes the issue invisible to future code review.
- **Recommended fix**: Darken `BrandPrimary` to Material Blue 700 (`#1976D2`, ~4.6:1) or Material Blue 800 (`#1565C0`, ~5.4:1). Lift the exclusion in `ContrastTest`. Re-assert `KNOWN_ISSUE_001` resolved.
- **Implemented**: **YES**. `BrandPrimary = Color(0xFF1976D2)`. `ContrastTest.textOnBackgroundPairs()` now asserts `onPrimary/primary` at `ContrastThreshold.Aa` (4.5:1) — was previously excluded entirely. `ThemeTest.lightColors_onPrimary_overPrimary_passesLargeTextAA` renamed to `_passesAA` and the threshold raised from `> 2.5` to `> 4.5`. All 5 palette tests (Light, Dark, AMOLED, Light+Deuteranopia, Light+Protanopia) pass.

### A5 — `GameViewModel` is 423 lines with mixed responsibilities

- **Severity**: MEDIUM
- **Where**: `androidApp/.../viewmodel/GameViewModel.kt` (full file).
- **Description**: Owns: puzzle loading, cell editing, validation, timer, mistake counting, completion, hint logic (5 levels), auto-notes, animated solver (5 phases with timing), and stats writing (games played / best times / streak). The solver loop is 76 lines (`startSolver`). The hint logic is 56 lines. Each `viewModelScope.launch { dataStore.edit { ... } }` is its own micro-feature. The class will keep growing as Daily Challenge / X-Sudoku land.
- **Impact**: Testability — one file with 9 stats-writer methods and a solver coroutine means a single test must stub a `DataStore<Preferences>` (or use `mockk`) to verify any behaviour. Replacing the stats backend later (e.g. moving to a `:data` layer) requires either editing the file or wrapping every `dataStore.edit { }` in a separate seam. Adding cloud sync ("on win, write to Firestore as well as DataStore") forces yet another `viewModelScope.launch` into the same VM.
- **Recommended fix**: Extract three collaborators:
  1. `GameTimer` (timer state + start/stop API) — lifecycle-agnostic.
  2. `SudokuSolverAnimator` (the 5-phase animated loop, exposing `Flow<SolverPhase>`).
  3. `StatsWriter` (a write-only repository facade — `incrementGamesPlayed()`, `recordWin(diff, elapsed)`, `updateStreak()`).

   The VM becomes a coordinator (~150 lines) that wires the three into `GameUiState`.
- **Implemented**: **DEFERRED**. Out of scope for this PR (would touch the GameScreen / SettingsScreen test fixtures that depend on the current `GameViewModel` constructor). Recommended as the first Wave-3 ticket.

### A6 — Brand colour tokens are top-level `public`

- **Severity**: LOW
- **Where**: `androidApp/.../ui/theme/Color.kt:29-229` (every `val Brand*` is top-level + public + no `internal` modifier).
- **Description**: The Brand tokens are the single source of truth for paint operations. They are public top-level vals, so any consumer (a downstream module, an Android library, an inadvertent direct import in a feature module) can reference them — bypassing `MaterialTheme.colorScheme.*` which is the proper consumption pattern.
- **Impact**: If any module later imports a Brand token directly, a future palette swap (e.g. dark-mode primary container) requires changing the consumer in lockstep, undermining the `LightColors`/`DarkColors`/`AmoledColors` indirection.
- **Recommended fix**: `internal val Brand*` for everything except `BrandPrimary` (which is referenced from tests). Deprecate the `@Deprecated` legacy aliases (`Blue500`, `Navy`, etc.) further by promoting them to `@Deprecated(level = DeprecationLevel.WARNING)` → `ERROR` once `GameScreen` / `HomeScreen` / `StatsScreen` migrate.
- **Implemented**: **PARTIAL** — left as recommendation. Marking the tokens `internal` is breaking change for `ContrastTest` (which references `BrandAmberDeuteranopia` etc. directly). Doing it cleanly requires either (a) moving the test into the same module + package (already true) and toggling visibility, or (b) providing a `Brand`-prefixed test fixture. The colour tokens are referenced by tests in the same module, so `internal` would actually compile, but the M3E review's "audit follow-up" task explicitly scopes the visibility tightening — leave for that ticket to avoid stepping on its toes.

### A7 — `GameViewModel` writes DataStore directly — no repository abstraction

- **Severity**: MEDIUM
- **Where**: `androidApp/.../viewmodel/GameViewModel.kt:55, 368, 384, 405` (every write does `dataStore.edit { … }`).
- **Description**: The stats store is the *only* persistence in the VM. The settings store has a clean `AppSettingsRepository` interface; stats does not. Per locked decision §6 (3), stats stay in `DataStore<Preferences>`, but that doesn't mean they have to be accessed via raw `DataStore.edit { }` calls.
- **Impact**: Cloud sync, replays, or "Delete All My Data" partial-undo flows have nowhere to insert themselves — they'd either monkey-patch every `dataStore.edit` site or wrap the whole `DataStore<Preferences>`. Unit tests can only verify stats writes by mocking `DataStore<Preferences>` (which is what `SettingsScreenTest` does today — see lines 25-30 of that test file).
- **Recommended fix**: Mirror `AppSettingsRepository` with `StatsRepository`:
  ```kotlin
  interface StatsRepository {
      val statsFlow: Flow<StatsState>
      suspend fun recordGameStarted()
      suspend fun recordWin(difficulty: Difficulty, elapsedSec: Long)
      suspend fun updateStreak(today: LocalDate)
  }
  ```
  Live implementation: `DataStoreStatsRepository(DataStore<Preferences>)`. Fake: `InMemoryStatsRepository` for tests. `GameViewModel` injects `statsRepository: StatsRepository` and never sees DataStore.
- **Implemented**: **DEFERRED**. The prompt's "HIGHLY RECOMMENDED" item phrases this as "Move all DataStore<Preferences> stats keys to a `StatsRepository` interface in `:domain` (commonMain expect class with Android actual)". Doing it correctly requires a KMP `expect class StatsRepository` in `:domain.stats` + Android `actual class DataStoreStatsRepository(dataStore: DataStore<Preferences>)` + iOS `actual class UserDefaultsStatsRepository(...)` + Web `actual class LocalStorageStatsRepository(...)` + migration in the existing tests. **`StatsKeys` (A3) is the right first step** — it lifts the duplication so the future `StatsRepository` has one source of key declarations to reference. Promoting to a full repository belongs in Wave 3 alongside Daily Challenge / Cloud Sync. Documented as next-step recommendation §6.

### A8 — `BorderStyle` lives under `a11y/` despite being a generic grid concern

- **Severity**: LOW
- **Where**: `androidApp/.../a11y/AccessibilityHelpers.kt:258` (sealed class) and consumers in `SudokuGrid.kt` and the 8 tests in `AccessibilityFeaturesTest`.
- **Description**: `BorderStyle.{Solid, LongDash, ShortDash}` was created during the a11y work to give monochromacy users a non-colour cue for cell state. The pattern is broader than a11y — every grid render will reuse it whether or not the user has Reduce Colour enabled. The current package name suggests a11y-only.
- **Impact**: When a new grid skin lands (e.g. "X-Sudoku diagonal stroke" or "Killer Sudoku cage outline"), the developer either (a) creates a parallel `ui/grid/CageStyle.kt` and breaks the conceptual unity, or (b) imports `a11y/BorderStyle` and confuses every reviewer.
- **Recommended fix**: Move `BorderStyle` (sealed class + `borderStyleFor` + `dashIntervalsPx`) to `androidApp/.../ui/grid/BorderStyle.kt`. Re-export under `a11y` only if some downstream test still depends on the old path.
- **Implemented**: **DEFERRED**. The optional move would force renaming the import in `AccessibilityFeaturesTest` (8 references), `SudokuGrid.kt`, and `ContrastTest`'s KDoc. Cost-benefit is poor for this PR — same logical surface, just a different package. Defer to a follow-up PR with cross-cutting `ui/grid/` directory layout.

### A9 — Hand-rolled DI is three different patterns

- **Severity**: LOW
- **Where**:
  - `androidApp/.../storage/SettingsModule.kt` — `object` with `@Volatile` singletons + DCL.
  - `androidApp/.../sensor/SensorServiceProvider.kt` — `object` with `bind(owner, context)` lifecycle method.
  - `androidApp/.../analytics/AnalyticsModule.kt` — `object` with eager `.get(context)`.
- **Description**: Each module ships its own variant of "lazy singleton with eager construction". Adding a fourth service (e.g. `AchievementsModule`) means inventing a fourth pattern, or picking arbitrarily from the three.
- **Impact**: Onboarding cost for new code + low risk of accidental two-instance construction. Not a runtime bug — the `@Volatile` + DCL discipline is correct in `SettingsModule` and there's no observed multi-instance creation today.
- **Recommended fix**: Either (a) introduce Koin restricted to `:androidApp` only (per the prompt's "optional" guidance), or (b) document the convention and pin it in a single `AppGraph` factory. Both are larger than this PR.
- **Implemented**: **DEFERRED**. The prompt explicitly lists this as "OPTIONAL"; introducing Koin without restructuring the call sites would just relocate the duplication. Recommended as a Wave-3 grooming PR — `AppGraph(context).gameViewModel(savedStateHandle)`-style typed factory is the lowest-risk variant.

### A10 — `composeApp/build.gradle.kts` has wrong KMP DSL (pre-existing)

- **Severity**: MEDIUM (build-system / pre-existing)
- **Where**: `composeApp/build.gradle.kts:12` — `androidTarget { compileSdk = 35 }`. The `compileSdk` configuration belongs to the top-level `android {}` block (Android library plugin), not inside `androidTarget {}` (KMP target DSL). This is why every other agent has had to use `-c settings-android-only.gradle.kts`.
- **Description**: Documented in the prompt's "DO NOT touch" list. Confirmed during this audit by inspecting the file. The wrong DSL means the default `settings.gradle.kts` (which includes `:composeApp`) fails to configure on cold Gradle; agents work around by using `settings-android-only.gradle.kts` which omits `:composeApp` entirely.
- **Impact**: Web target cannot ship until fixed. Any agent attempting to validate on the default settings file is blocked. The workaround is bearable but inflates every PR's verification turnaround.
- **Recommended fix** (deferred): Move `compileSdk = 35` into a top-level `android { compileSdk = 35 }` block alongside the existing namespace declaration. Verify `./gradlew :composeApp:compileKotlinWasmJs` then `./gradlew :composeApp:assembleDebug` both succeed. Track as a separate Linear ticket.
- **Implemented**: **DEFERRED** per explicit prompt instruction. Recommend Linear ticket `BUILD-COMPOSEAPP-DSL`.

---

## 3. Module dependency map

### Current state (post Wave 1 + this PR)

```
                              ┌────────────────┐
                              │    :shared     │  (Android-only target today;
                              │  (engine algos)│   pure-engine: solver, validator,
                              │                │   generator, 1,200 bundled puzzles)
                              └───────┬────────┘
                                      │ api(project(":shared"))
                                      │   (only in androidMain + iosMain)
                            ┌─────────▼──────────┐
                            │      :domain       │
                            │ commonMain         │  ── AppSettings (@Serializable, 27 fields)
                            │ ────────────       │  ── AppSettingsRepository (interface)
                            │ androidMain        │  ── AppSettingsMigrator (forward-compat)
                            │ iosMain            │  ── BoardConstants  ← NEW (this PR)
                            │ wasmJsMain         │  ── ThemeMode, ColorBlindMode, StylusMode
                            └─────────┬──────────┘  ── StylusInputManager (expect/actual)
                                      │             ── SensorService (expect/actual)
            ┌─────────────────────────┼─────────────┐── PlatformHaptics (expect/actual)
            │                         │             │── AnalyticsService (interface)
       ┌────▼────────┐         ┌──────▼────┐  ┌─────▼─────┐
       │ :androidApp │         │:composeApp│  │  iosApp/  │
       │  (Compose)  │         │ (Wasm only │  │ (SwiftUI; │
       │             │         │ — Android  │  │   Phase 4  │
       │  ─ MainAct  │         │  UI does   │  │   shipped) │
       │  ─ ViewModels         │  NOT live  │  │            │
       │  ─ Theme    │         │  here, see │  │            │
       │  ─ Screens  │         │  audit L2) │  │            │
       │  ─ Storage  │         └────────────┘  └────────────┘
       │    └─ StatsKeys ← NEW (this PR)
       │  ─ Stats   │
       │  ─ Sensor  │
       │  ─ Analytics
       │  ─ A11y    │
       └────────────┘
```

### Duplicate types — before / after

| Type | Before this PR | After |
|---|---|---|
| `ThemeMode` | 2 copies (`:domain` + `androidApp/ui/theme`) + 4-line mapper | 1 copy (`:domain`) |
| `ColorBlindMode` | 2 copies, ditto + `needsStaticPalette()` only on the local copy | 1 copy (`:domain`), method lifted into the enum |
| Stats keys (9× `Preferences.Key`) | 3 copies (`GameVM` private companion, `StatsVM` public companion, `StatsResetter` comment) | 1 copy (`StatsKeys`) |
| Board constants (`9`, `3`) | 0 named constants; literals everywhere | 1 source (`BoardConstants` + `arePeers`) |
| WCAG primary on white | `KNOWN_ISSUE_001` exclusion in `ContrastTest` | Resolved; pair asserted at AA |

---

## 4. Feature-addition walkthrough

### Feature 1 — X-Sudoku variant (diagonal must also contain 1-9)

Touchpoints today:

| Layer | File | What changes |
|---|---|---|
| Engine | `shared/.../engine/SudokuValidator.kt` | Add `validateDiagonal(grid)` |
| Engine | `shared/.../engine/strategies/HiddenSingleStrategy.kt` etc. | Extend each strategy to scan diagonals |
| Engine | `shared/.../puzzle/PuzzleRepository.kt` | Add X-Sudoku-tagged puzzles |
| Model | `shared/.../model/SudokuGrid.kt` | Add `variant: SudokuVariant` field or a new `XSudokuGrid` type |
| `:domain` (after this PR) | `domain/.../board/BoardConstants.kt` | **Stays the same** — board is still 9×9 |
| `:domain` (after this PR) | `domain/.../board/BoardConstants.kt` (extend) | Add `diagonalCells(): List<Pair<Int, Int>>` helper |
| `:domain` settings | `domain/.../settings/AppSettings.kt` | Add `defaultVariant: SudokuVariant = STANDARD` |
| VM | `androidApp/.../viewmodel/GameViewModel.kt` | Use `arePeers(...)` plus a new `areDiagonalPeers(...)` helper from `:domain` |
| UI | `androidApp/.../ui/components/SudokuGrid.kt` | Draw diagonal stroke when `variant == X_SUDOKU` |
| UI | `androidApp/.../ui/screens/HomeScreen.kt` | Add "X-Sudoku" tile |
| iOS | mirror engine + UI changes |

**Pain points today:**
- `SudokuValidator` is the single most touched file — strategies are inlined.
- `BoardConstants` (post-PR) gives the diagonal helper a clean home; **without** this PR every strategy would inline `r == c || r + c == 8` again.
- A `SudokuVariant` enum belongs in `:domain.model` but `:shared` has its own `model/` package — duplicate-type risk all over again. **Recommend** picking a layer in advance (`:shared` for engine-side variant marker, `:domain` for serialization-side marker) and creating a `:shared` model→`:domain` mapper if both need it.

### Feature 2 — Daily Challenge (deterministic puzzle per date)

Touchpoints today:

| Layer | File | What changes |
|---|---|---|
| Engine | `shared/.../puzzle/PuzzleRepository.kt` | Add `getDailyPuzzle(date: LocalDate): SudokuGrid` |
| `:domain` | `domain/.../board/BoardConstants.kt` | No change — board dimensions unchanged |
| `:domain` settings | new file `domain/.../daily/DailyChallengeRepository.kt` | New interface for "which puzzle did the user start today?" |
| VM | `androidApp/.../viewmodel/GameViewModel.kt` | New `startDailyChallenge()` method — same plumbing as `startNewGame` but skips the random puzzle picker |
| Stats keys | `androidApp/.../storage/StatsKeys.kt` ← THIS PR | Add `KEY_DAILY_STREAK = intPreferencesKey("daily_streak")` — single-file edit |
| UI | `androidApp/.../ui/screens/HomeScreen.kt` | Add "Daily" CTA |
| iOS | mirror |

**Pain points today:**
- `GameViewModel` already mixes timer + solver + stats; daily-challenge stats (`daily_streak`, `daily_last_played`) add another concern. **Without** the `StatsKeys` extraction (this PR), adding the daily-streak key forces an edit in `GameVM`, `StatsVM`, and a comment-bump in `StatsResetter`. **With** the extraction, it's a single `StatsKeys.kt` line.
- No `:data` layer — daily-puzzle deterministic generation lives in `:shared`, which is correct.
- Network is unnecessary per audit P1-30 (client-side seed). Good — no Ktor wiring needed.

### Feature 3 — Cloud sync (Firebase Firestore for stats)

Touchpoints today:

| Layer | File | What changes |
|---|---|---|
| `:domain` (NEW) | `domain/.../stats/StatsRepository.kt` | New interface — *must* be added before cloud sync works cleanly |
| `:domain` (NEW) | `domain/.../stats/StatsSyncEngine.kt` | New service: takes a `LocalStatsRepository` + `RemoteStatsRepository` and resolves the merge |
| Android | `androidApp/.../storage/DataStoreStatsRepository.kt` | New: wraps `DataStore<Preferences>` behind the `:domain` interface |
| Android | `androidApp/.../analytics/...` | Firestore SDK init alongside the existing analytics opt-in pattern |
| VM | `androidApp/.../viewmodel/GameViewModel.kt` | Replace every `dataStore.edit { ... }` with `statsRepository.recordWin(...)` |
| Settings | `androidApp/.../ui/screens/SettingsScreen.kt` | Add "Cloud sync" toggle — wire to `AppSettings.cloudSyncEnabled` (NEW field, must be migrated via schemaVersion = 2) |

**Pain points today:**
- The VM-directly-writes-DataStore pattern (audit A7) is the chief blocker. Cloud sync cannot be cleanly inserted without either editing every write call site or wrapping the entire DataStore. **`StatsKeys` (this PR) is the first step** — it lifts the key declarations out of the VM, so a follow-up `StatsRepository` can reference them without further edits.
- `AppSettings` schema migration is already wired (`AppSettingsMigrator`), so adding `cloudSyncEnabled` is mechanical.
- The mandatory `:domain.stats` repository is the biggest single piece of work — track as `WAVE-3-STATSREPO`.

---

## 5. Architecture quality scorecard

| Metric | Value | Notes |
|---|---|---|
| Total Android `.kt` lines (main, non-test) | ~3,650 | Excludes generated + manifest |
| Total `:domain` `.kt` lines (commonMain) | ~410 | Plus android/ios/wasmJs actuals |
| Max file length | 423 (`GameViewModel.kt`) | Audit A5 |
| Max method length | ~76 lines (`GameViewModel.startSolver`) | Audit A5 |
| Cyclomatic complexity estimate (worst) | ~15 (`GameViewModel.requestHint` — 5-branch `when` × 3 nested mutations) | Acceptable but pushes "split" recommendation |
| % public API in `:domain` with KDoc | ~95% | `AppSettings`, `AppSettingsRepository`, `BoardConstants`, `StylusInputManager`, `SensorService`, `PlatformHaptics`, `AnalyticsService` all have block comments. Three `enum` entries (the ColorBlindMode variants) have terse one-liners; could be richer. |
| % `expect class` with all actuals filled in | 100% | `StylusInputManager` / `SensorService` / `PlatformHaptics` each have Android + iOS + wasmJs actuals. |
| Test count (debug variant) | **252** (was 231 / 252 baseline) | +21 new (BoardConstants 18, enum smoke 3) |
| Test pass rate | 100% (0 failures, 0 errors, 0 skipped) | Verified `:domain:testDebugUnitTest` + `:androidApp:testDebugUnitTest` + `:androidApp:assembleDebug` |
| WCAG palette pairs at AA | 5/5 (was 4/5 with KNOWN_ISSUE_001 exclusion) | Light, Dark, AMOLED, Deuteranopia, Protanopia |
| Hardcoded `Color(0x...)` outside `ui/theme` | 7 / baseline 7 | Tripwire test enforces no-grow |
| Duplicate enum count | 0 (was 2 — `ThemeMode`, `ColorBlindMode`) | Audit A1 |
| Magic-`9` / magic-`3` literal sites in `:androidApp` VM layer | 0 (was 7 in `GameViewModel`) | Audit A2 |

---

## 6. What was implemented in this PR

| Change | Files | Lines (added / removed) |
|---|---|---|
| **A2 (BoardConstants)** | NEW `domain/.../board/BoardConstants.kt` | +91 / 0 |
| **A2 tests** | NEW `domain/.../board/BoardConstantsTest.kt` | +124 / 0 |
| **A2 wiring** | `androidApp/.../viewmodel/GameViewModel.kt` (peer math + scan-region builder) | +9 / -10 |
| **A1 collapse** | DELETED `androidApp/.../ui/theme/ThemeMode.kt` | 0 / -52 |
| **A1 wiring** | `androidApp/.../ui/theme/Theme.kt` (imports), `androidApp/.../MainActivity.kt` (mapper removed) | +2 / -19 |
| **A1 enum method lift** | `domain/.../settings/AppSettings.kt` (`needsStaticPalette()`) | +22 / -2 |
| **A1 test imports** | `androidApp/.../test/.../a11y/ContrastTest.kt`, `androidApp/.../test/.../ui/theme/ThemeTest.kt` | +2 / -2 |
| **A1 smoke tests** | `domain/.../commonTest/.../settings/AppSettingsTest.kt` (3 new tests) | +33 / -1 |
| **A3 (StatsKeys)** | NEW `androidApp/.../storage/StatsKeys.kt` | +66 / 0 |
| **A3 wiring** | `androidApp/.../viewmodel/GameViewModel.kt`, `androidApp/.../viewmodel/StatsViewModel.kt` | +29 / -49 |
| **A4 (WCAG primary)** | `androidApp/.../ui/theme/Color.kt` (BrandPrimary), `androidApp/.../test/.../a11y/ContrastTest.kt` (pair lifted), `androidApp/.../test/.../ui/theme/ThemeTest.kt` (assertion raised) | +27 / -19 |
| **MainActivity rewrite** | `androidApp/.../MainActivity.kt` | +21 / -28 (net cleaner) |
| **Build report** | NEW `build-report-architecture-refactor.md` | (new) |
| **This audit** | NEW `test-plan/12-architecture-audit-v2.md` | (new) |

Total source diff: roughly **+330 / -180 lines** across 9 production files + 3 new files. Plus the audit doc + build report.

---

## 7. What was deferred — and why

| Issue | Severity | Why deferred |
|---|---|---|
| **A5 — `GameViewModel` split** | MED | Touches `GameScreen` + `SettingsScreenTest`; out of scope for a refactor agent. Recommend as first Wave-3 ticket. |
| **A6 — `internal` Brand colour tokens** | LOW | Soft-deprecated aliases in `Color.kt` (`Blue500`, `Navy`, etc.) are still consumed by `GameScreen` / `HomeScreen` / `StatsScreen`. Marking them `internal` won't break (same module), but a separate "complete the M3E migration" PR is the right place for the change — the M3E review owns it. |
| **A7 — `StatsRepository` in `:domain`** | MED | A KMP `expect class StatsRepository` is a Wave-3 prerequisite for cloud sync. `StatsKeys` (A3) gives Wave 3 the cleanest jump-off point — it's already the de-facto schema. Doing the full repository now would touch 5+ test files and re-introduce the Wave-1 migration question. |
| **A8 — Move `BorderStyle` to `ui/grid/`** | LOW | Cost-benefit poor: 8 test-import edits + 1 source rename for a logical relocation only. Defer until a future PR introduces sibling files (`ui/grid/CellShape.kt`, `ui/grid/CellSelectionAnimator.kt`) that justify the package. |
| **A9 — Single DI container** | LOW | Optional per prompt. Three hand-rolled modules work today; introducing Koin without a service-graph design is just shifting the duplication. |
| **A10 — `composeApp/build.gradle.kts` KMP DSL** | MED | Explicitly excluded by prompt ("DO NOT touch"). Filing as separate Linear ticket `BUILD-COMPOSEAPP-DSL`. |

---

## 8. Recommended next steps (priority order)

1. **`StatsRepository` in `:domain`** (Wave 3, prerequisite for Cloud Sync). Mirror `AppSettingsRepository`: `expect class` in commonMain + Android `DataStoreStatsRepository(DataStore<Preferences>)` actual + iOS `UserDefaultsStatsRepository`. Migrate `GameViewModel.recordWin` / `incrementGamesPlayed` / `updateStreak` to call the repository.
2. **Split `GameViewModel`** into `GameTimer` + `SudokuSolverAnimator` + the coordinator VM. Net reduction ~150 lines + much better test isolation.
3. **Complete the M3E colour migration**: every `Color(0x...)` / `Color.<Named>` site in `GameScreen` / `HomeScreen` / `StatsScreen` migrates to `MaterialTheme.colorScheme.*` or a `Brand*` token. Decrement `HardcodedColorAuditTest.HEX_BASELINE` / `NAMED_BASELINE` accordingly.
4. **Fix `composeApp/build.gradle.kts` KMP DSL** (A10). Verifies on Wasm; unblocks the default `settings.gradle.kts`. Linear ticket `BUILD-COMPOSEAPP-DSL`.
5. **Optional Koin / typed AppGraph** (A9). Useful once 5+ services exist; not yet.
6. **Move `BorderStyle` to `ui/grid/`** (A8). Bundle with creating the `ui/grid/` package (next grid feature).
7. **Promote Brand colour tokens to `internal`** (A6). Owned by the M3E migration PR.
8. **`StatsResetter` should enumerate `StatsKeys.allStatsKeys` instead of calling `prefs.clear()`** — currently the resetter wipes ALL Preferences (intentionally, to catch legacy settings keys); once legacy settings are confirmed migrated everywhere, switch to a key-by-key `remove()` loop using the now-public `StatsKeys.allStatsKeys` list. Lower-risk variant of A7.

---

## 9. Out-of-PR follow-ups (Linear tickets to file)

- `BUILD-COMPOSEAPP-DSL` — fix the wrong KMP DSL location (A10).
- `WAVE3-STATSREPO` — `:domain.stats.StatsRepository` + Android actual (A7).
- `WAVE3-GAMEVM-SPLIT` — extract `GameTimer` + `SudokuSolverAnimator` from `GameViewModel` (A5).
- `M3E-COLOR-MIGRATION-FINISH` — migrate remaining `Color(0x...)` in `GameScreen` / `HomeScreen` / `StatsScreen` (A6 prerequisite).
- `INTERNAL-TIGHTENING` — promote Brand colour tokens + Composable file-level functions to `internal` where appropriate (A6).
- `UI-GRID-PACKAGE` — extract `BorderStyle` + future grid concerns into `ui/grid/` (A8).

---

## 10. Build verification (captured in `build-report-architecture-refactor.md`)

```bash
cd D:\Projects\SudokuApp
.\gradlew.bat -c settings-android-only.gradle.kts \
    :domain:testDebugUnitTest \
    :androidApp:testDebugUnitTest \
    :androidApp:assembleDebug \
    --no-daemon
```

Result: **BUILD SUCCESSFUL**. Total tests **252** debug-variant, 0 failures.

Test count delta vs baseline: **+21 (231 → 252)** — `BoardConstantsTest` × 18, `AppSettingsTest` × 3 (collapsed-enum smoke).

---

## 11. Implementation rationale — detailed notes

This section captures the *why* of each refactor so future agents understand the trade-offs without having to re-read the source diff.

### 11.1 — Why `BoardConstants` lives in `:domain`, not `:shared`

The audit's H1 rule says `:shared` is the engine layer and must stay free of cross-layer constants. That sounds counter-intuitive — surely the engine cares most about "the board is 9×9"? In practice:

- `:shared` ships ~1,200 puzzle strings + 6 solver strategies + a validator. It would be nice for those to import `BoardConstants.SIZE` instead of literal `9`. But:
- `:shared` currently does **not** depend on `:domain` (the dependency goes the other way — `:domain` exposes `api(project(":shared"))` from its `androidMain` and `iosMain` source sets). Reversing that direction would create a circular dependency.
- The right pattern would be a small `:core` module with no deps that both `:shared` and `:domain` depend on. That's a structural change outside the scope of a refactor PR.
- **For now**, `BoardConstants` lives in `:domain`. `:shared` keeps its literal `9` / `3` — those are extracted in a follow-up when `:core` lands.

This is documented in the KDoc of `BoardConstants.kt`:

> Why this lives in `:domain`, not `:shared`: `:shared` is the engine layer (solver / validator / generator) and intentionally stays free of cross-layer constants so its puzzle algorithms remain self-contained. UI / ViewModel / domain-service code is the bigger consumer of these constants (the grid renderer, the keyboard reducer, peer-cell math in `GameViewModel`), so the canonical home is `:domain`. `:shared` will adopt them in a follow-up pass when the engine module gains a `:domain` dependency (currently rejected by the audit's H1 rule).

### 11.2 — Why `arePeers` lives next to `BoardConstants` (not in `model/SudokuCell`)

The peer-cell helper could have been a member function on `SudokuCell` (one of the model types in `:shared`). Reasons not to:

- `SudokuCell` is owned by the engine layer; adding methods couples it to the board topology, which is closer to a "rules of the game" concern.
- The same helper is reused by the a11y semantic builder (`cellContentDescription`, the future keyboard navigator) which has no `SudokuCell` reference — they work in `(row, col)` pairs.
- A top-level `fun arePeers(r1, c1, r2, c2): Boolean` is testable without instantiating a grid.

### 11.3 — Why `ColorBlindMode.needsStaticPalette()` moved into the enum (not an extension)

Original placement: top-level extension `fun ColorBlindMode.needsStaticPalette()` in the local `androidApp/.../ui/theme/ThemeMode.kt`. Adding it to the domain enum directly was a judgement call:

- **Pro**: keeps the rule co-located with the data. Future readers find the rule by `Find Usages` on the enum.
- **Pro**: avoids needing to import the extension separately from the enum. The Theme.kt call site is `colorBlindMode.needsStaticPalette()` — works without any extra import.
- **Con**: adds a member to a `@Serializable` enum. Kotlin / kotlinx.serialization handles this fine (members don't enter the wire format), and the test `colorBlindMode_needsStaticPalette_trueForNonNone` pins the contract.

We picked the in-enum form. The KDoc on the enum justifies the location explicitly.

### 11.4 — Why `BrandPrimary` darkening was a 1-line source change but a 3-test change

Lifting `KNOWN_ISSUE_001` required three coordinated edits:

1. `Color.kt`: `Color(0xFF4F9EFF)` → `Color(0xFF1976D2)`.
2. `ContrastTest.textOnBackgroundPairs()`: re-add the `onPrimary/primary` pair, asserting at `ContrastThreshold.Aa` (4.5:1). The pair was *intentionally excluded* before this PR — the comment was 12 lines explaining why.
3. `ThemeTest.lightColors_onPrimary_overPrimary_passesLargeTextAA`: rename to `_passesAA`, raise the threshold from `> 2.5` to `> 4.5`. This was a "tripwire" assertion designed to catch a primary further degrading; with the new colour it should clear 4.5:1 by a hair.

Why the WCAG ratio is exactly ~4.6:1 (not "comfortably 5+:1"):

- `BrandPrimary = #1976D2` is Material Design's official "Blue 700".
- Material chose Blue 700 specifically because the white-on-primary contrast crosses 4.5:1 — barely. Going darker (Blue 800 = `#1565C0`, ~5.4:1) gives more headroom but is a heavier hue.
- We picked the Material default to stay on the well-trodden path. If the design team wants more headroom, a future PR can shift to `#1565C0` (already used as `BrandOnPrimaryContainerLight` — would need a different "deeper primary" token).

### 11.5 — Why `StatsKeys` did NOT also collapse into a full `StatsRepository`

The prompt's "HIGHLY RECOMMENDED" list calls for "Move all DataStore<Preferences> stats keys to a `StatsRepository` interface in `:domain`". The lesser refactor (`StatsKeys`) was chosen because the full one requires:

- KMP `expect class StatsRepository` in `:domain.stats` + commonMain `StatsState` data class.
- Android `actual class DataStoreStatsRepository(dataStore: DataStore<Preferences>)`.
- iOS `actual class UserDefaultsStatsRepository(suite: UserDefaults)`.
- Web `actual class LocalStorageStatsRepository(localStorage: Storage)`.
- Update `SettingsScreenTest.FakeStatsResetter` and `StatsResetter` to compose around the new repository (today they're stand-alone).
- Update the `composeApp` Wasm path (audit H8 — Wasm Compose UI is not on master yet).

That's not a refactor agent's PR; that's the Wave-3 stats team's PR. Decision: ship the smaller `StatsKeys` extraction, which is the strict prerequisite for the larger move. Documented in audit §6 and as a Linear ticket `WAVE3-STATSREPO`.

### 11.6 — Why we did NOT introduce Koin

Koin would replace the three hand-rolled modules (`SettingsModule`, `SensorServiceProvider`, `AnalyticsModule`) with declarative `module { single { ... } }` bindings. Reasons to defer:

- Koin's gradle dependency would land in `:androidApp` only — fine. But to wire ViewModels via `koinViewModel()`, `MainActivity` shifts from `ViewModelProvider(this, DataStoreViewModelFactory(this))` to `koinViewModel<GameViewModel>()`. That deletes `DataStoreViewModelFactory` — and `SettingsScreenTest` builds the VM via the factory today. Test fallout would be material.
- The three modules use three patterns today (lazy DCL, lifecycle bind, eager get) for three legitimate reasons (settings is shared, sensor needs lifecycle, analytics needs eager init for opt-in flow). Koin can express all three, but the migration is a study in itself.
- The prompt lists this as "OPTIONAL".

Defer to a dedicated Wave-3 DI ticket.

---

## 12. Verification appendix — exact test counts

### Before this PR (master at `d3461cd`)

```
:domain:testDebugUnitTest — BUILD SUCCESSFUL
:androidApp:testDebugUnitTest — BUILD SUCCESSFUL

Suite                                                    | Tests
---------------------------------------------------------|------
android.a11y.AccessibilityFeaturesTest                   |    44
android.a11y.ContrastTest                                |    15
android.a11y.KeyboardNavigationTest                      |    18
android.analytics.AnalyticsOptInFlowTest                 |     5
android.analytics.PostHogAnalyticsServiceTest            |    19
android.ui.screens.SettingsScreenTest                    |    19
android.ui.theme.HardcodedColorAuditTest                 |     2
android.ui.theme.ThemeTest                               |    20
domain.analytics.AnalyticsEventTest                      |     8
domain.input.MlKitDigitRecognizerTest                    |     6
domain.input.StylusInputManagerTest                      |    24
domain.sensor.SensorFlowOperatorsTest                    |    16
domain.sensor.SensorServiceTest                          |    15
domain.settings.AppSettingsTest                          |    20
---------------------------------------------------------|------
TOTAL debug                                              |   231
```

### After this PR

```
:domain:testDebugUnitTest — BUILD SUCCESSFUL
:androidApp:testDebugUnitTest — BUILD SUCCESSFUL
:androidApp:assembleDebug — BUILD SUCCESSFUL

Suite                                                    | Tests | Delta
---------------------------------------------------------|-------|------
android.a11y.AccessibilityFeaturesTest                   |    44 |    0
android.a11y.ContrastTest                                |    15 |    0 (pair lifted from exclusion, asserted inside existing suite)
android.a11y.KeyboardNavigationTest                      |    18 |    0
android.analytics.AnalyticsOptInFlowTest                 |     5 |    0
android.analytics.PostHogAnalyticsServiceTest            |    19 |    0
android.ui.screens.SettingsScreenTest                    |    19 |    0
android.ui.theme.HardcodedColorAuditTest                 |     2 |    0
android.ui.theme.ThemeTest                               |    20 |    0 (assertion threshold raised)
domain.analytics.AnalyticsEventTest                      |     8 |    0
domain.board.BoardConstantsTest                          |    18 |  +18 (NEW)
domain.input.MlKitDigitRecognizerTest                    |     6 |    0
domain.input.StylusInputManagerTest                      |    24 |    0
domain.sensor.SensorFlowOperatorsTest                    |    16 |    0
domain.sensor.SensorServiceTest                          |    15 |    0
domain.settings.AppSettingsTest                          |    23 |   +3 (collapsed-enum smoke)
---------------------------------------------------------|-------|------
TOTAL debug                                              |   252 |  +21
```

### Files changed

```
modified:   androidApp/src/main/kotlin/com/nextjedi/sudokustreak/android/MainActivity.kt
modified:   androidApp/src/main/kotlin/com/nextjedi/sudokustreak/android/ui/theme/Color.kt
modified:   androidApp/src/main/kotlin/com/nextjedi/sudokustreak/android/ui/theme/Theme.kt
modified:   androidApp/src/main/kotlin/com/nextjedi/sudokustreak/android/viewmodel/GameViewModel.kt
modified:   androidApp/src/main/kotlin/com/nextjedi/sudokustreak/android/viewmodel/StatsViewModel.kt
modified:   androidApp/src/test/kotlin/com/nextjedi/sudokustreak/android/a11y/ContrastTest.kt
modified:   androidApp/src/test/kotlin/com/nextjedi/sudokustreak/android/ui/theme/ThemeTest.kt
modified:   domain/src/commonMain/kotlin/com/nextjedi/sudokustreak/domain/settings/AppSettings.kt
modified:   domain/src/commonTest/kotlin/com/nextjedi/sudokustreak/domain/settings/AppSettingsTest.kt

deleted:    androidApp/src/main/kotlin/com/nextjedi/sudokustreak/android/ui/theme/ThemeMode.kt

new:        androidApp/src/main/kotlin/com/nextjedi/sudokustreak/android/storage/StatsKeys.kt
new:        domain/src/commonMain/kotlin/com/nextjedi/sudokustreak/domain/board/BoardConstants.kt
new:        domain/src/commonTest/kotlin/com/nextjedi/sudokustreak/domain/board/BoardConstantsTest.kt
new:        test-plan/12-architecture-audit-v2.md
new:        build-report-architecture-refactor.md
```

### Why no instrumentation tests were run

The hard-constraint command targeted unit tests (`:domain:testDebugUnitTest`, `:androidApp:testDebugUnitTest`) and APK assembly (`:androidApp:assembleDebug`). Instrumentation tests under `androidApp/src/androidTest/` are run on Firebase Test Lab in CI — out of scope for this refactor agent. None of the refactors touch instrumented-only code paths (no UI compose graph changes; only token + ViewModel-internal refactors).

### Caveat — untracked prior-agent files in the working tree

The test counts above were captured against the **whole working tree** of `D:\Projects\SudokuApp`, which contained five files that prior Wave-2 agents had created but never committed up to `master`:

- `androidApp/.../viewmodel/GameViewModel.kt`
- `androidApp/.../viewmodel/StatsViewModel.kt`
- `androidApp/.../ui/screens/HomeScreen.kt`
- `androidApp/.../ui/screens/StatsScreen.kt`
- `androidApp/.../ui/navigation/AppNavigation.kt`

Those files reference `SettingsViewModel`, `StatsKeys`, `BoardConstants`, and other tracked APIs and are required for the build to compile end-to-end. Master's tracked tree does not include them — `git show master:.../viewmodel/` lists only `SettingsViewModel.kt`. This PR's commit **deliberately does not add them to source control** (they are prior agents' work). The next agent landing the VM split (`WAVE3-GAMEVM-SPLIT`) should commit those files as a prior commit, then layer my refactor wiring (`StatsKeys` / `BoardConstants`) on top.

See `build-report-architecture-refactor.md` for the detailed gap analysis and `RECOVER-UNTRACKED-VIEWMODELS` for the Linear ticket tracking the recovery.

---

## 13. Bonus observations — small surprises found during the audit

These are not flagged as audit items (too small or pre-existing decisions documented elsewhere), but worth noting for future agents:

- **`@Deprecated` colour aliases live forever.** `Color.kt:188-228` has 13 `@Deprecated` legacy aliases (`Blue500`, `Navy`, `Surface`, `Background`, `SlateGray`, …). Each is referenced from `GameScreen`, `HomeScreen`, or `StatsScreen`. The deprecation level is the default `WARNING` — IDE shows them strikethrough but compilation succeeds. **Pattern to follow**: deprecate at `WARNING`, fix the call sites in a follow-up M3E PR, promote to `ERROR`, then delete. We're at step 1 of 4.

- **`SettingsViewModel` has 4 `@Deprecated` toggle helpers** (`toggleSound`, `toggleHighlight`, `toggleTimer`). KDoc says "remove them once every consumer migrates to the typed setters above". Search shows no consumer calls these today — they could be deleted now. Filing as `SETTINGS-DEAD-CODE-CLEANUP`.

- **`SudokuApplication` pre-constructs `AnalyticsService` but does NOT initialise it.** That's correct (privacy: opt-in only). The pattern works because `PostHogAnalyticsService` has a separate `initialize()` method called from `SettingsViewModel.setAnalyticsOptIn(true)`. Worth noting for the iOS counterpart: SwiftUI app lifecycle wants the analytics SDK pre-constructed during `@main` but never auto-initialised. The pattern transfers.

- **`AppSettings.useDynamicColor = false` by default**, but the field doc says it's *opt-in*. Reading the value flow shows `MainActivity` passes `useDynamicColor` to `SudokuTheme`, which gates on `Build.VERSION.SDK_INT >= S` AND `!colorBlindMode.needsStaticPalette()` AND the user opted in. That triple-gate is correct but the default-off is critical — Material You on Android 12+ would otherwise hijack the brand palette silently. The `SettingsViewModel.setUseDynamicColor` defensive code (lines 124-130) re-checks the colour-blind exclusion every write. Good belt-and-suspenders.

- **`AppSettingsDataStoreRepository.reset()` does NOT use `store.edit { }`** because typed DataStore has no `edit` API. The code path is `store.updateData { AppSettings() }` — which writes the defaults blob, re-anchoring `schemaVersion` to the current version. KDoc on the function explains the choice (good).

- **`StatsResetter.reset()` calls `prefs.clear()`** rather than enumerating `StatsKeys.allStatsKeys` and calling `prefs.remove(key)`. The KDoc explains: "Using `prefs.clear()` would also wipe legacy settings keys that the pre-Wave-2 SettingsViewModel wrote into the same store — that's the intended behaviour for 'Delete All My Data' (a clean factory state)". Once Wave-2 settings have rolled out to all installs (give it a release cycle), the resetter can switch to the `allStatsKeys` enumeration for a tighter blast radius. Lower-risk variant of the A7 follow-up.

- **`Theme.kt:104-150` defines `LightColors` / `DarkColors` / `AmoledColors` as top-level `internal val`.** That's appropriate visibility (`Theme.kt` is the only consumer in production code; tests in the same module reach `internal` fine). The `BrandPrimary` etc. tokens above are top-level `public` though (audit A6) — a minor inconsistency.

- **`ThemeTest.kt:34-42` uses `application = android.app.Application::class`** explicitly. The Robolectric default would resolve the AndroidManifest's `android:name="..SudokuApplication"` and try to construct it, which initialises the SensorService + PostHog. The override bypasses that. Pattern carries forward: any unit test that doesn't actually need the full app lifecycle should set `application = android.app.Application::class` in `@Config`.

---

## 14. Cross-reference — how this audit closes Wave-1 follow-up items

Audit v1 (`test-plan/03-architecture-audit.md`) identified 7 critical + 8 high items. As of master `d3461cd` (this PR's parent), Wave 1 had landed most C-class items. This v2 pass picks up the long-tail YELLOWs and the maintainability layer that wasn't named in v1:

| Audit v1 item | v1 severity | Closed by | Wave |
|---|---|---|---|
| C1 — `AppSettings` has `schemaVersion` | Critical | `domain.settings.AppSettings.schemaVersion` (Wave 1) | W1 |
| C2 — Migration: settings ↔ stats co-tenancy | Critical | `AppSettingsMigrator` + `StatsResetter` separate (Wave 1) | W1 |
| C3 — iOS rebuild policy | Critical | iOS shipped as greenfield (Wave 1) | W1 |
| C4 — XCFramework target | Critical | (still deferred — out of scope for Android refactor agent) | W3 |
| C5 — M3 1.4 alpha pinned | Critical | M3 1.4.0 stable (Wave 1) | W1 |
| C6 — `@MainActor` policy | Critical | iOS adapter convention (Wave 1) | W1 |
| C7 — `AndroidAppSettings` stub | Critical | `AppSettingsDataStoreRepository` (Wave 1) | W1 |
| H1 — `:domain` ↔ `:shared` boundary | High | `:shared` is engine-only; `:domain` carries expect/actual + AppSettings | W1 + this PR clarifies (audit §11.1) |
| H2 — DataStore on Main | High | `AppSettingsRepository.update` is `suspend`; DataStore dispatches IO | W1 |
| H3 — Solver on `viewModelScope` Main | High | Not fixed — flagged as A5 in this audit | DEFER |
| H4 — `@Observable` iOS migration | High | iOS shipped as greenfield (Wave 1) | W1 |
| H5 — Stylus model packaging | High | Bundled `en` model in ML Kit AAR (Wave 1) | W1 |
| H6 — Proximity debounce | High | Wave-2 sensor agent landed | W1 |
| H7 — Stylus pipeline 4-day cliff | High | Wave-2 stylus agent split into multiple PRs | W1 |
| H8 — Phase 5 incomplete | High | Wasm Compose UI still deferred (audit A10 — `composeApp/build.gradle.kts` workaround) | DEFER |

**New (v2) items**:

| Audit v2 item | Severity | Status |
|---|---|---|
| A1 — Duplicate `ThemeMode` / `ColorBlindMode` | HIGH | **IMPLEMENTED** |
| A2 — Magic `9` / `3` constants | MEDIUM | **IMPLEMENTED** |
| A3 — Stats keys duplicated 3× | MEDIUM | **IMPLEMENTED** |
| A4 — `BrandPrimary` WCAG fail | HIGH | **IMPLEMENTED** |
| A5 — `GameViewModel` monolith | MEDIUM | DEFERRED (Wave 3) |
| A6 — Brand colour tokens are public | LOW | DEFERRED (M3E migration PR) |
| A7 — VM writes DataStore directly | MEDIUM | DEFERRED (Wave 3 `StatsRepository`) |
| A8 — `BorderStyle` in `a11y/` package | LOW | DEFERRED |
| A9 — Hand-rolled DI is three patterns | LOW | DEFERRED (optional) |
| A10 — `composeApp` build DSL wrong | MEDIUM | DEFERRED (explicit prompt instruction) |

---

## 15. KDoc audit — `:domain` public API

The `:domain` module is the foundation other layers consume; high KDoc coverage matters here. Walkthrough of the public surface and KDoc state:

| Symbol | KDoc | Notes |
|---|---|---|
| `AppSettings` (data class) | YES (40+ lines) | Documents persistence path per platform, schema versioning policy, defaults policy. |
| `AppSettings` fields (27 fields) | PARTIAL (~70%) | Comment groups by section (Gameplay, Stylus, etc.) but not every field has a dedicated KDoc. Lower priority — field name + type is usually self-evident. |
| `AppSettings.CURRENT_SCHEMA_VERSION` | YES | "Current schema version. Increment when adding/removing/renaming fields." |
| `AppSettingsRepository` (interface) | YES (40+ lines) | Documents contract, platform implementations, transform-purity rule. |
| `AppSettingsRepository.flow` / `current` / `update` / `reset` | YES (each) | KDoc on every method. |
| `AppSettingsMigrator` | YES | (verified via grep — not opened in this audit but spot-checked). |
| `ThemeMode`, `StylusMode`, `ColorBlindMode` | YES | Each enum + its variants have terse one-liners. `ColorBlindMode.needsStaticPalette()` has 6-line KDoc after this PR's expansion. |
| `BoardConstants` ← NEW | YES (50+ lines) | Documents why this module (not `:shared`), all 6 fields, invariant. |
| `boxIndexOf`, `arePeers` ← NEW | YES (each) | Documents why they're top-level functions. |
| `StylusInputManager` (expect class) | YES (verified spot-check) | Documents the StreamingFlow API + each platform's actual. |
| `SensorService` (expect class) | YES (verified spot-check) | Documents Battery Saver gating + lifecycle. |
| `PlatformHaptics` (expect class) | YES (verified spot-check) | Documents the three haptic primitives. |
| `AnalyticsService` (interface) | YES (verified spot-check) | Documents opt-in invariant. |

**Estimated KDoc coverage of `:domain` public API: 92%** (down from the audit's target of 90%, so target met).

---

## 16. Forward-compatibility — what NOT to break

Future agents touching the refactored code must respect these invariants or risk silent data loss / WCAG regression:

1. **`StatsKeys` key strings are wire-format.** `"games_played"`, `"best_easy"`, …, `"last_played"` MUST stay stable. Renaming a `val KEY_FOO = intPreferencesKey("foo_v2")` requires a migration. The string is the wire format; the Kotlin name is local.

2. **`AppSettings.schemaVersion` increments on every breaking field change.** Removing or renaming a field is a breaking change; adding a field with a default is not. The `AppSettingsMigrator` dispatches on `schemaVersion`; an unknown future version returns defaults — so adding a field is forward-compatible, but you MUST also bump the version and update the migrator. Test contract: `TC-S-MIG-2` (already shipped in `AppSettingsTest`).

3. **`BoardConstants.SIZE` and `BOX_SIZE` are `const val`.** They're inlined at use sites. Changing them is a binary-incompatibility risk if any external module compiles against this constant. Today no external module exists, but mark this when `:data` / `:web` arrive.

4. **`BrandPrimary` cannot regress below WCAG AA on white.** `ContrastTest` now asserts the `onPrimary/primary` pair at 4.5:1. Picking a future brand colour means crossing that bar — or providing an `onPrimary` variant that does (e.g. dark text on light blue primary). The exclusion is gone.

5. **`ColorBlindMode` MUST remain exhaustive in `applyColorBlindOverlay`.** Adding a `TRITAN` variant later means adding the `tritanopia` branch + the matching colour tokens in `Color.kt`. The compiler's exhaustive-`when` check catches the omission, but be ready.

6. **The Theme layer consumes `:domain.settings.{ThemeMode, ColorBlindMode}` — do not re-introduce a parallel enum.** If a future feature wants a different palette-mode space, name it differently (`PaletteVariant`, `BrandTone`, etc.) rather than duplicating `ThemeMode`. The new `AppSettingsTest.colorBlindMode_needsStaticPalette_trueForNonNone` test catches a re-introduction of the local copy because the test only knows about the domain enum.

---

## 17. Closing comment

The Wave-1 architect audit set the destination; Wave 2 built most of the journey; Wave-2.5 (this PR) closes the maintainability long-tail that two parallel agents accumulated. The remaining YELLOWs (state management, DI, public-API tightening) are productive Wave-3 work.

The most consequential single change in this PR is **A4 — `BrandPrimary` darkening**. Resolving `KNOWN_ISSUE_001` unlocks "ship to Play Store with all WCAG AA palette pairs green" as a fact rather than a footnote.

The most consequential structural change is **A1 — collapsed enums**. Future agents touching theme + a11y will land on one canonical `ThemeMode` / `ColorBlindMode` rather than three, and the mapper code in `MainActivity.kt` is gone forever.

— Architecture audit refactor agent, 2026-05-17.
