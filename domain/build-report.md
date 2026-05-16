# `:domain` module — Wave 1 build report

> Generated: 2026-05-16
> Module: `:domain`
> Branch: worktree-agent-ae81678980c3fb2d4

## Summary

| Target              | Compile      | Tests       | Test count | Failures |
|---------------------|--------------|-------------|------------|----------|
| Android (debug)     | PASS         | PASS        | 20         | 0        |
| Android (release)   | PASS         | PASS        | 20         | 0        |
| wasmJs (browser)    | PASS         | PASS        | 20         | 0        |
| iosX64              | SKIPPED (a)  | SKIPPED (a) | —          | —        |
| iosArm64            | SKIPPED (a)  | SKIPPED (a) | —          | —        |
| iosSimulatorArm64   | SKIPPED (a)  | SKIPPED (a) | —          | —        |
| jvm                 | DISABLED (b) | DISABLED (b)| —          | —        |

(a) Kotlin/Native iOS targets cannot build on Windows; Gradle disables them automatically.
    Test the iOS actuals on macOS (or CI) in Wave 2.

(b) Plain `jvm()` target temporarily disabled in `domain/build.gradle.kts`. The K2 JVM-IR
    backend in Kotlin 2.0.21 hits an internal compiler error
    (`IrFakeOverrideSymbolBase.shouldNotBeCalled`) when compiling the
    `kotlinx-serialization` generated code for plain JVM under this exact tool combo
    (Gradle 8.7 + AGP 8.6.0 + Kotlin 2.0.21 + kotlinx-serialization 1.7.3). Android
    targets do not hit this. Re-enable the `jvm()` target after upgrading Kotlin (the
    fix is tracked upstream; see the stack trace below).

## How tests were run

```
./gradlew.bat -c settings-domain-only.gradle.kts :domain:allTests
```

The repo root `settings.gradle.kts` includes `:composeApp` whose `build.gradle.kts`
fails to configure under Kotlin 2.0.21 (`Unresolved reference: compileSdk` inside
`androidTarget { compileSdk = 35 }` — a 2.0.21 syntax break introduced by another
agent's `libs.versions.toml` bump). To unblock the `:domain` work without modifying
`:composeApp` (out of scope per task instructions), a sibling settings file
`settings-domain-only.gradle.kts` was added at repo root and used via `-c`. It
includes only `:shared` and `:domain`. The canonical `settings.gradle.kts` is
unchanged except for the `include(":domain")` addition.

This sibling settings file can be deleted once `:composeApp`'s configuration error
is fixed (separately).

## Tests delivered (20)

All in `domain/src/commonTest/kotlin/com/nextjedi/sudokustreak/domain/settings/AppSettingsTest.kt`.

Mapped to plan / synthesis IDs:

| Test                                                         | TC ID          |
|--------------------------------------------------------------|----------------|
| `defaults_match_spec`                                        | TC-S2          |
| `serialization_round_trip_equals_original`                   | TC-S1          |
| `migrator_round_trip_equals_original`                        | TC-S1 + P0-2   |
| `schemaVersion_is_first_field_in_serialized_output`          | PC-5 / TC-S5c  |
| `ignoreUnknownKeys_allows_forward_compat`                    | TC-S2c         |
| `migrate_v1_blob_returns_same`                               | P0-2           |
| `migrate_missing_schemaVersion_uses_v0_path`                 | P0-2 + TC-S6   |
| `migrate_unknown_future_version_falls_back_to_defaults`      | P0-2 + TC-S6   |
| `migrate_corrupt_json_returns_defaults_without_exception`    | P0-2           |
| `migrate_empty_blob_returns_defaults`                        | P0-2           |
| `migrate_partial_blob_uses_defaults_for_missing_fields`      | TC-S7          |
| `copy_partial_update_preserves_other_fields`                 | TC-S2b         |
| `migrate_invalid_enum_string_falls_back_to_defaults`         | TC-S8          |
| `stylusConfidenceThreshold_preserved_through_round_trip`     | TC-S2d         |
| `migrate_non_object_top_level_returns_defaults`              | TC-S9          |
| `encode_emits_all_fields_for_downgrade_safety`               | (envelope)     |
| `migrator_defaultJson_ignores_unknown_keys`                  | TC-S2c         |
| `all_enums_round_trip_through_migration`                     | (enums)        |
| `migrate_blob_with_whitespace_padding_decodes_correctly`     | (robustness)   |
| `migrate_current_schema_version_constant_matches_default_field` | (invariant) |

Total: **20 tests, 0 failures** (vs. ≥ 12 required by task spec).

## Files added (16 source + 1 build + 1 test settings + 1 report)

```
domain/build.gradle.kts                                                                              (build)
domain/src/commonMain/kotlin/com/nextjedi/sudokustreak/domain/settings/AppSettings.kt                (data class + enums)
domain/src/commonMain/kotlin/com/nextjedi/sudokustreak/domain/settings/AppSettingsRepository.kt      (interface)
domain/src/commonMain/kotlin/com/nextjedi/sudokustreak/domain/settings/AppSettingsMigrator.kt        (migrator + Json)
domain/src/commonMain/kotlin/com/nextjedi/sudokustreak/domain/input/StylusInputManager.kt            (expect + sealed events)
domain/src/commonMain/kotlin/com/nextjedi/sudokustreak/domain/sensor/SensorService.kt                (expect + sealed events)
domain/src/commonMain/kotlin/com/nextjedi/sudokustreak/domain/haptics/PlatformHaptics.kt             (expect)
domain/src/androidMain/kotlin/com/nextjedi/sudokustreak/domain/input/StylusInputManager.android.kt   (TODO Wave 2)
domain/src/androidMain/kotlin/com/nextjedi/sudokustreak/domain/sensor/SensorService.android.kt       (TODO Wave 2)
domain/src/androidMain/kotlin/com/nextjedi/sudokustreak/domain/haptics/PlatformHaptics.android.kt    (TODO Wave 2)
domain/src/iosMain/kotlin/com/nextjedi/sudokustreak/domain/input/StylusInputManager.ios.kt           (TODO Wave 2)
domain/src/iosMain/kotlin/com/nextjedi/sudokustreak/domain/sensor/SensorService.ios.kt               (TODO Wave 2)
domain/src/iosMain/kotlin/com/nextjedi/sudokustreak/domain/haptics/PlatformHaptics.ios.kt            (TODO Wave 2)
domain/src/wasmJsMain/kotlin/com/nextjedi/sudokustreak/domain/input/StylusInputManager.wasmJs.kt     (no-op)
domain/src/wasmJsMain/kotlin/com/nextjedi/sudokustreak/domain/sensor/SensorService.wasmJs.kt         (no-op)
domain/src/wasmJsMain/kotlin/com/nextjedi/sudokustreak/domain/haptics/PlatformHaptics.wasmJs.kt      (no-op)
domain/src/commonTest/kotlin/com/nextjedi/sudokustreak/domain/settings/AppSettingsTest.kt            (20 tests)
domain/build-report.md                                                                               (this file)
settings-domain-only.gradle.kts                                                                      (test scaffold)
```

## Files modified

- `settings.gradle.kts` — added `include(":domain")`.
- `gradle/libs.versions.toml` — added `kotlinx-serialization-json`, `kotlinx-datetime`,
  `kotlinx-coroutines-core`, `kotlinx-coroutines-test` libraries and the
  `kotlin-serialization` plugin alias. (Another concurrent agent additionally bumped
  `agp`, `kotlin`, `compose-bom` — those edits are not from this agent.)

## Known caveats / Wave 2 follow-ups

1. **`:shared` dependency.** `domain/build.gradle.kts` defers `api(project(":shared"))`
   on `commonMain` until `:shared` declares a `jvm()` target (currently it does not).
   Today the dependency is added on `androidMain` + `iosMain` instead so Android / iOS
   consumers still get `:shared` types transitively. None of the Wave-1 code in
   `:domain` references `:shared` types, so the deferred `api` is harmless for now.
2. **JVM target disabled** pending the Kotlin 2.0.21 JVM-IR + kotlinx-serialization fix.
3. **Actuals are TODO stubs** for Android + iOS + wasmJs. Each `actual class` body
   contains a `// TODO actual: implement in Wave 2` comment and returns
   `emptyFlow()` / `Unrecognized` so the module compiles and tests on all reachable
   targets without coupling Wave 1 to ML Kit, CoreMotion, PencilKit, etc.
4. **Composer app build break** (pre-existing, not introduced by this work). After
   the `:composeApp` `androidTarget { compileSdk = 35 }` line is fixed, the bespoke
   `settings-domain-only.gradle.kts` and `-c` workaround can be removed.

## Internal compiler error (jvm target) — log snippet for reference

```
e: org.jetbrains.kotlin.backend.common.CompilationException: Back-end: Please report this problem https://kotl.in/issue
Details: Internal error in file lowering: java.lang.IllegalStateException: should not be called
    at IrFakeOverrideSymbolBase.getOwner(IrFakeOverrideSymbol.kt:41)
    at ExternalPackageParentPatcherLowering$Visitor.visitMemberAccess(ExternalPackageParentPatcherLowering.kt:39)
    ...
File: AppSettings.kt
Triggered by: kotlinx.serialization generated `AppSettings.$serializer` on the JVM-IR
              backend under Kotlin 2.0.21.
```

The Android target uses the same compiler pipeline but with the AGP-supplied
classpath shape (different `metadataApiElements`), and does not hit this bug.
