# Android Theme rewrite — build report

> Generated: 2026-05-16
> Branch: master (worktree: `agent-a0f93179393a224cd`)
> Scope: M3 1.4.0 stable + dark/AMOLED themes + glass extensions + Color audit

## TL;DR

`:androidApp:assembleDebug` and `:androidApp:testDebugUnitTest` both green.
22 / 22 unit tests pass (20 in `ThemeTest`, 2 in `HardcodedColorAuditTest`).

## Version bumps

| Component | Before | After | Why |
|---|---|---|---|
| AGP | 8.4.2 | **8.6.0** | Compose BoM 2026.05.00 pulls in `androidx.core:1.16.0` + compose 1.11.x which require AGP 8.6+. Gradle 8.7 (already shipped) + JDK 17+ (we have 21) satisfy 8.6's prerequisites. |
| Kotlin | 2.0.0 | **2.0.21** | M3E reviewer §1 — 2.0.0 has a known JVM-target bug with M3 1.4 compose-compiler metrics. |
| Compose BoM | 2024.06.00 | **2026.05.00** | Ships M3 1.4.0 stable. |
| `compileSdk` / `targetSdk` | 34 | **35** | M3 1.4 predictive-back specs need API 35. RequirES Android SDK 35 installed locally + CI. |
| Java toolchain | 1.8 | **17** | Required by AGP 8.6 + Kotlin 2.0.21 compose-compiler. |

## Added dependencies

- `androidx.compose.material3.adaptive:adaptive:1.2.0`
- `androidx.compose.material3:material3-adaptive-navigation-suite` (via BoM)
- Test-only: `junit:junit:4.13.2`, `org.robolectric:robolectric:4.13`,
  `androidx.test.ext:junit:1.2.1`, `androidx.compose.ui:ui-test-junit4`,
  `com.google.truth:truth:1.4.4`, `kotlinx-coroutines-test`.
- `compose-ui-test-manifest` as a `debugImplementation` (required by Robolectric
  Compose UI tests).

## Composable API surface

| API | Status | Decision |
|---|---|---|
| `MotionScheme.expressive()` | **`internal`** in M3 1.4.0 (verified by inspecting `material3-android-1.4.0.aar` Kotlin metadata on 2026-05-16). Public only in 1.5.0-alpha. | **Skip for now**. Use plain `MaterialTheme(...)`. Expose `LocalReduceMotion` so call sites can opt into standard `tween` / `spring` and respect reduce-motion at the spec level. Re-evaluate when 1.5.0 stable lands. |
| `MaterialExpressiveTheme(...)` | Same — `internal` in 1.4.0. | Same — skip. |
| `MaterialShapes.Cookie9Sided` | Experimental in 1.4.0, reverted to Experimental in 1.5.0-alpha19. | Not used in Theme.kt. Number-pad uses `RoundedCornerShape` 18% → 50% morph per the M3E review §3 recommendation. Cookie9 reserved for celebrations. |
| `BlurEffect` API 31+ | Public, stable. Robolectric does not implement the native shader op → no-op under unit tests. | Wrapped via `glassyTint()` in `GlassExtensions.kt`. SDK gating swappable via `GlassSdkProvider.testOverride()` for unit-test determinism. |
| `dynamicLightColorScheme` / `dynamicDarkColorScheme` | Public, stable. API 31+ only. | Opt-IN via `useDynamicColor = false` default. Forced off when `colorBlindMode != NONE` to preserve luminance separation. |
| `MaterialTheme(...)` with `colorScheme + typography + shapes` | Public, stable. | Used. |

## Test results (22 / 22 pass)

```
ThemeTest:
  amoledColors_haveBlackSurface                                  PASS
  amoledColors_inheritDarkPrimary                                PASS
  amoledColors_textOverBlackSurface_passesAA                     PASS
  colorBlindDeuteranopia_amberAndTealHaveLuminanceGap            PASS
  colorBlindMode_needsStaticPalette_trueForNonNone               PASS
  colorBlindOverlay_doesNotChangePrimaryOrError                  PASS
  colorBlindOverlay_noneIsIdentity                               PASS
  colorBlindProtanopia_amberAndTealHaveLuminanceGap              PASS
  darkColors_textOverBackground_passesAA                         PASS
  darkColors_textOverSurface_passesAA                            PASS
  glassSdkProvider_testOverride_returnsOverriddenValue           PASS
  glassyTint_blursOnApi31Plus                                    PASS
  glassyTint_fallsBackBelowApi31                                 PASS
  glassyTint_glassDisabledForcesFallback                         PASS
  glassyTint_reduceTransparencyFallback                          PASS
  lightColors_onPrimary_overPrimary_passesLargeTextAA            PASS
  lightColors_textOverBackground_passesAA                        PASS
  lightColors_textOverSurface_passesAA                           PASS
  sudokuExtendedColors_swapsAmberForDeuteranopiaMode             PASS
  sudokuExtendedColors_swapsTealForProtanopiaMode                PASS
HardcodedColorAuditTest:
  no_hardcoded_Color_hex_literals_outside_ui_theme               PASS (baseline tripwire = 7)
  report_Color_Named_usage_outside_ui_theme_as_a_soft_warning    PASS (baseline tripwire = 16)
```

## Known issues / follow-ups (not blocking)

1. **`composeApp/build.gradle.kts` line 12** has a pre-existing config-time error
   (`compileSdk = 35` inside `androidTarget {}` for KMP — wrong DSL). Another agent
   owns `composeApp/`. To work around this for the theme build, I added a temporary
   `settings-android-only.gradle.kts` that includes only `:shared` + `:androidApp`.
   That file should be removed once the composeApp DSL is fixed; production builds
   continue to use the default `settings.gradle.kts`. The theme work itself does
   not depend on composeApp.

2. **`androidApp/src/main/AndroidManifest.xml:5`** declares
   `android:name=".SudokuApplication"`, which resolves against the
   `applicationId = com.nextjedi.sudoku` namespace but the actual Application
   subclass lives at `com.nextjedi.sudokustreak.android.SudokuApplication`. This
   is a pre-existing bug — Robolectric tests bypass it via
   `@Config(application = android.app.Application::class)`. A separate Linear
   issue should fix the manifest reference to the fully-qualified class name.

3. **7 `Color(0x...)` literal + 16 `Color.<Named>` violations** outside `ui/theme/`
   surfaced in `androidApp/docs/reports/hardcoded-color-violations.md`.
   `HardcodedColorAuditTest` enforces a tripwire baseline — the count cannot grow.
   Migrating them to brand tokens is `SDK-PARITY-COLOR-MIGRATION`.

4. **`MaterialExpressiveTheme` + `MotionScheme.expressive()`** are still `internal`
   in M3 1.4.0 stable as of 2026-05-16 (verified by inspecting the resolved AAR).
   The Theme.kt currently uses plain `MaterialTheme` + a `LocalReduceMotion`
   CompositionLocal. When M3 1.5.0 stable lands, swap in `MaterialExpressiveTheme`
   inside `SudokuTheme` (the call site is structured for a 1-line drop-in).

## Files changed / added

### Modified
- `gradle/libs.versions.toml` — version bumps + new libraries.
- `androidApp/build.gradle.kts` — JDK 17, SDK 35, Lint block, M3 adaptive deps, test deps.
- `androidApp/src/main/kotlin/.../ui/theme/Color.kt` — full Brand* palette + color-blind variants + deprecated aliases for migration.
- `androidApp/src/main/kotlin/.../ui/theme/Theme.kt` — `SudokuTheme(...)` honours `themeMode`, dark/AMOLED palettes, dynamic color opt-in, color-blind overlay.

### Added
- `androidApp/src/main/kotlin/.../ui/theme/ThemeMode.kt` — `ThemeMode` + `ColorBlindMode` enums.
- `androidApp/src/main/kotlin/.../ui/theme/GlassExtensions.kt` — `Modifier.glassyTint()` + SDK-override hook.
- `androidApp/src/test/kotlin/.../ui/theme/ThemeTest.kt` — 20 Robolectric tests.
- `androidApp/src/test/kotlin/.../ui/theme/HardcodedColorAuditTest.kt` — JUnit grep tripwire.
- `androidApp/docs/reports/hardcoded-color-violations.md` — surface list.
- `androidApp/build-report.md` — this file.
- `settings-android-only.gradle.kts` (temporary; remove after composeApp fix).

## Build command used

```bash
cd D:/Projects/SudokuApp && ./gradlew.bat -c settings-android-only.gradle.kts \
  :androidApp:assembleDebug :androidApp:testDebugUnitTest --no-daemon
```

Once composeApp is fixed, the same task graph should work without `-c`:

```bash
cd D:/Projects/SudokuApp && ./gradlew.bat \
  :androidApp:assembleDebug :androidApp:testDebugUnitTest
```
