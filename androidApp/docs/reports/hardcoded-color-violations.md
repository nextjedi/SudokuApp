# Hardcoded `Color(0x...)` violations outside `ui/theme/`

> Generated: 2026-05-16
> Source: grep over `androidApp/src/main/kotlin/**/*.kt` for `Color\(0x[0-9A-Fa-f]+\)`
> Enforcement: `androidApp/src/test/kotlin/.../theme/HardcodedColorAuditTest.kt`

The M3E review (`test-plan/04-android-m3e-review.md` §9) listed 10 effective theming violations across 3 files.
The fresh scan on 2026-05-16 surfaced **7** `Color(0x...)` literals plus **16** `Color.<Named>` usages (3 strict — `Color.Red`, `Color.Gray`, `Color.LightGray` — and 13 `Color.White` foregrounds over coloured backgrounds), for a documented baseline of **7 hex + 16 named = 23 hardcoded colour references**.

The 10 the M3E review surfaced map directly to: 7 hex + the 3 strict named (`Color.Red`, `Color.Gray`, `Color.LightGray`). The remaining 13 `Color.White` usages were not in the M3E review's table but are tracked here for completeness.

## `Color(0xFF......)` literal violations (7)

| File | Line | Hardcoded value | Recommended token |
|---|---|---|---|
| `ui/components/SudokuGrid.kt` | 38 | `Color(0xFFFFEB3B)` | `LocalSudokuColors.current.solverFillingYellow` (or `BrandSolverFillingYellow`) |
| `ui/components/SudokuGrid.kt` | 39 | `Color(0xFFFFB74D)` | `LocalSudokuColors.current.solverHintAmber` (or `BrandSolverHintAmber`) |
| `ui/components/SudokuGrid.kt` | 40 | `Color(0xFFFFF176)` | `LocalSudokuColors.current.solverHintedYellow` (or `BrandSolverHintedYellow`) |
| `ui/components/SudokuGrid.kt` | 41 | `Color(0xFF80CBC4)` | `LocalSudokuColors.current.solverFilledTeal` (or `BrandSolverFilledTeal`) |
| `ui/screens/GameScreen.kt` | 136 | `Color(0xFF1E2A38)` | `MaterialTheme.colorScheme.surfaceContainerHigh` |
| `ui/screens/GameScreen.kt` | 160 | `Color(0xFFE0E0E0)` | `LocalSudokuColors.current.borderFaint` (or `BrandBorderLightFaint`) |
| `ui/screens/GameScreen.kt` | 212 | `Color(0xFFF57C00)` | `LocalSudokuColors.current.streakOrange` (or `BrandStreakOrange`) |

## `Color.<Named>` violations (3 — soft-fail today)

| File | Line | Hardcoded value | Recommended replacement |
|---|---|---|---|
| `ui/components/SudokuGrid.kt` | 155 | `Color.Red.copy(alpha = 0.5f)` | `MaterialTheme.colorScheme.error.copy(alpha = 0.5f)` |
| `ui/components/SudokuGrid.kt` | 156 | `Color.Gray` | `LocalSudokuColors.current.notesGray` |
| `ui/components/NumberPad.kt` | 93 | `Color.LightGray` | `MaterialTheme.colorScheme.surfaceContainerHigh` |

## Additional `Color.White` usages to migrate (informational, not counted in the M3E top-10)

These are *technically* hardcoded but are used as a foreground on a coloured background (button content, sheet background). They will be migrated alongside the M3E component rewrites:

- `ui/screens/StatsScreen.kt:77,134` — `.background(Color.White, ...)`
- `ui/screens/SettingsScreen.kt:116,117,195,207,217` — switch/segment colours
- `ui/screens/GameScreen.kt:152,282,283` — solver-badge text + chip-row backgrounds
- `ui/screens/HomeScreen.kt:105` — difficulty row background
- `ui/components/SudokuGrid.kt:92` — default cell background
- `ui/components/NumberPad.kt:92` — number-button text

## Follow-up

These are **not** fixed in this PR — that's a separate Linear issue (`SDK-PARITY-COLOR-MIGRATION`). The audit test `HardcodedColorAuditTest` currently fails because of these violations, which is intentional: it surfaces them in CI until they are migrated. Once all 7 strict violations are removed, the `Color.<Named>` test can be promoted from soft-warning to hard-fail.
