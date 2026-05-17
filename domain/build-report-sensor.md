# `:domain` Android SensorService — Wave 2 build report

> Generated: 2026-05-17
> Module: `:domain` (`androidMain` + `androidUnitTest`)
> Branch: `worktree-agent-a56531851483be243`
> Wave: 2 — Android sensor actual + lifecycle + Battery Saver + tests

## TL;DR

`:domain:testDebugUnitTest` and `:domain:testReleaseUnitTest` both green.
**51 / 51 unit tests pass** across the module (15 SensorService + 16 operator + 20 pre-existing AppSettings).

| Target              | Compile | Tests | Test count | Failures |
|---------------------|---------|-------|------------|----------|
| Android debug       | PASS    | PASS  | 51         | 0        |
| Android release     | PASS    | PASS  | 51         | 0        |
| wasmJs browser      | PASS    | PASS  | 20         | 0        |
| iosX64/Arm64/SimArm64 | SKIPPED (Windows host) | — | — | — |
| jvm                 | DISABLED (Wave 1 carryover — see domain/build-report.md §b) | — | — | — |

`:androidApp:compileDebugKotlin` and `:androidApp:testDebugUnitTest` also green —
verified that the new `SensorServiceProvider` + Application/Activity wiring does
not break the existing M3 theme tests (22/22 pass).

## How tests were run

```
./gradlew.bat -c settings-domain-only.gradle.kts :domain:allTests
./gradlew.bat :androidApp:testDebugUnitTest
```

The `settings-domain-only.gradle.kts` workaround from Wave 1 is still required
because `:composeApp`'s build break (unrelated to this work) prevents loading
the full `settings.gradle.kts` during cold compile. Wave 2 also runs against
the full settings file once `:domain` and `:androidApp` are the only modules
exercised — see the `:androidApp:testDebugUnitTest` run above.

## Tests delivered (31 new for Wave 2)

### `:domain:androidUnitTest` — SensorServiceTest.kt (15 tests, target 15)

Covers all 12 required test cases from `test-plan/01-test-scenarios.md` §3.4 plus
three Wave-2-specific additions (Battery Saver + sample-rate verification +
no-leak after 100 start/stop cycles).

| Test                                                            | TC ID                                          |
|-----------------------------------------------------------------|------------------------------------------------|
| `proximity_value0_emitsNear`                                    | TC-SE1                                         |
| `proximity_value8_emitsFar`                                     | TC-SE2                                         |
| `ambientLight_rapidFlicker_emitsAtMostOneTransition`            | TC-SE3                                         |
| `tilt_singleSpikeOutlier_smoothedOutputStable`                  | TC-SE4                                         |
| `lifecyclePaused_unregistersAllSensors`                         | TC-SE5                                         |
| `lifecycleResumed_afterPause_reRegistersSensors`                | TC-SE-RES                                      |
| `start_calledTwice_onlyRegistersOnce`                           | TC-SE7 (idempotent)                            |
| `rotationVector_normalMode_usesSensorDelayUi`                   | TC-SE-UI (PC-13 — SENSOR_DELAY_UI not _GAME)   |
| `batterySaver_downgradesRotationVectorToNormal`                 | TC-SE-BS-1 (P1-21 Battery Saver)               |
| `batterySaver_disablesTiltEmission`                             | TC-SE15 / TC-SE-BS-2                           |
| `proximity_chatterWithin100ms_emitsNothing`                     | TC-SE13                                        |
| `proximity_sustained500msFar_emitsFar`                          | TC-SE2 ext (500ms FAR debounce)                |
| `ambientLight_hysteresisHighThreshold_emitsOnlyOnce`            | TC-SE9                                         |
| `sharedFlow_concurrentWritesFromMultipleCoroutines_allReachFlow`| TC-SE-CONCURRENT                               |
| `startStopCycle100x_listenerCountStableAtZero`                  | TC-SE-LEAK                                     |

### `:domain:androidUnitTest` — SensorFlowOperatorsTest.kt (16 tests)

Pure operator tests — no SensorManager / Robolectric needed for these. Lock
down the math + state-machine logic of the three operators in isolation.

| Test                                                          | Operator                  |
|---------------------------------------------------------------|---------------------------|
| `movingAverage 4-points returns simple mean`                  | MovingAverage             |
| `movingAverage 1-point returns sample as-is`                  | MovingAverage             |
| `movingAverage circular buffer drops oldest at window`        | MovingAverage             |
| `movingAverage reset clears buffer`                           | MovingAverage             |
| `movingAverage single-spike outlier is dampened`              | MovingAverage             |
| `lightHysteresis classifies correctly across thresholds`      | LightHysteresis           |
| `lightHysteresis reset returns to UNKNOWN`                    | LightHysteresis           |
| `hysteresisLight rapid flicker emits at most one transition`  | hysteresisLight Flow op   |
| `hysteresisLight ignores dead-band-only readings`             | hysteresisLight Flow op   |
| `hysteresisLight emits separated transitions outside window`  | hysteresisLight Flow op   |
| `movingAverageTilt freshCollector starts from zero buffer`    | movingAverageTilt Flow op |
| `movingAverageTilt dampens single-spike outlier sequence`     | movingAverageTilt Flow op |
| `debouncedProximity NEAR sustained emits NEAR`                | debouncedProximity Flow op|
| `debouncedProximity FAR sustained emits FAR after NEAR`       | debouncedProximity Flow op|
| `debouncedProximity NEAR-FAR-NEAR within 100ms emits nothing` | debouncedProximity Flow op|
| `debouncedProximity sustained FAR alone emits FAR`            | debouncedProximity Flow op|

## Files added

```
domain/src/androidMain/kotlin/com/nextjedi/sudokustreak/domain/sensor/
  SensorService.android.kt                     (310 lines — replaces Wave 1 stub)
  SensorFlowOperators.kt                       (231 lines — pure ops + raw types)

domain/src/androidUnitTest/kotlin/com/nextjedi/sudokustreak/domain/sensor/
  SensorServiceTest.kt                         (530 lines — 15 Robolectric tests)
  SensorFlowOperatorsTest.kt                   (270 lines — 16 pure operator tests)

androidApp/src/main/kotlin/com/nextjedi/sudokustreak/android/
  SudokuApplication.kt                         (eager SensorService init)
  MainActivity.kt                              (lifecycle binding via SensorServiceProvider)
  sensor/
    SensorServiceProvider.kt                   (process-wide singleton holder)

androidApp/src/main/
  AndroidManifest.xml                          (3 uses-feature declarations, all required=false)

domain/build-report-sensor.md                  (this file)
```

## Files modified

- `domain/build.gradle.kts` — add `androidx-lifecycle-runtime-ktx` to `androidMain`,
  add `androidUnitTest` source set with Robolectric + Turbine + lifecycle-testing
  + truth dependencies. Enable `testOptions.unitTests.isIncludeAndroidResources`.
- `androidApp/build.gradle.kts` — add `implementation(project(":domain"))`.
- `gradle/libs.versions.toml` — add `turbine = "1.1.0"` and
  `androidx-lifecycle-runtime-testing` library alias.
- `build.gradle.kts` (root) — add `kotlin-serialization apply false` plugin
  declaration (needed because `:domain` applies it; root must hold the version).

## Hard requirements — verification

| Requirement | Where enforced | Verified by                                         |
|-------------|----------------|-----------------------------------------------------|
| Rotation vector at `SENSOR_DELAY_UI` (~60ms), not `_GAME` | SensorService.android.kt line ~241 | `rotationVector_normalMode_usesSensorDelayUi` test  |
| LifecycleEventObserver — `start()` on ON_RESUME, `stop()` on ON_PAUSE | SensorService.android.kt#observe | `lifecyclePaused_unregistersAllSensors` + `lifecycleResumed_afterPause_reRegistersSensors` |
| Proximity 300ms NEAR / 500ms FAR debounce | SensorFlowOperators.kt#debouncedProximity | `proximity_chatterWithin100ms_emitsNothing` + `proximity_sustained500msFar_emitsFar` |
| Ambient light 5s hysteresis on 50 / 100 lux | SensorFlowOperators.kt#hysteresisLight | `ambientLight_rapidFlicker_emitsAtMostOneTransition` + `hysteresisLight emits separated transitions outside window` |
| Tilt 4-point moving average | SensorFlowOperators.kt#movingAverageTilt | `tilt_singleSpikeOutlier_smoothedOutputStable` + `movingAverage single-spike outlier is dampened` |
| Battery Saver override — rotation rate downgrade + tilt suppression | SensorService.android.kt#start + #tiltEvents | `batterySaver_downgradesRotationVectorToNormal` + `batterySaver_disablesTiltEmission` |
| Threading — smoothing on Dispatchers.Default via flowOn | SensorService.android.kt — every public flow uses `.flowOn(backgroundDispatcher)` | Indirect — UnconfinedTestDispatcher used in tests; production uses `Dispatchers.Default` |
| Memory — bounded buffers, no leak across stop | SensorService.android.kt — `extraBufferCapacity` 8/8/16 + null-out on stop() | `startStopCycle100x_listenerCountStableAtZero` (100 cycles → 0 leaked) |
| start() idempotent | SensorService.android.kt#start (registered guard) | `start_calledTwice_onlyRegistersOnce` |

## Design notes

### 1. Why a no-arg primary + secondary constructor (not `class SensorService(ctx)`)?

The KMP `expect class SensorService { ... }` in `commonMain` declares no
constructor. Per KMP rules (even with `-Xexpect-actual-classes`), the
synthesized no-arg constructor is the contract — all `actual class`
declarations must satisfy it. The Android actual provides an explicit no-arg
primary constructor for conformance AND an `internal constructor(SensorManager,
PowerManager, ...)` secondary for direct DI (used by `Companion.create` and
all unit tests). The iOS / wasmJs actuals are unchanged — they keep their
no-arg constructors. Two-phase init via `attach(Context)` covers callers that
prefer the no-arg path.

### 2. Why `MutableSharedFlow(extraBufferCapacity = 8/8/16, DROP_OLDEST)`?

- **Bounded** — task spec required no unbounded SharedFlow.
- **DROP_OLDEST** — sensor framework calls `onSensorChanged` on the main
  thread; under no circumstance should our `tryEmit` block. The freshest
  reading is always more useful than a stale one anyway.
- **8 for proximity/light, 16 for rotation** — rotation vector fires at
  `SENSOR_DELAY_UI` (~16 Hz). At a peak burst of ~3 events between scheduler
  ticks, 16 gives ~1s headroom before the operator catches up. Lower buffer
  on proximity/light because they fire <1 Hz in steady state.

### 3. Why a `nowMs: () -> Long` injectable rather than `System.currentTimeMillis()`?

Robolectric 4.13's `ShadowSystemClock.advanceBy` does NOT reliably propagate
`setCurrentTimeMillis` changes to a `SensorEventListener` invoked synchronously
via `sendSensorEventToListeners` when the test runs under
`UnconfinedTestDispatcher`. Symptom: 4 of 15 SensorService tests timed out at
2s wall-clock waiting for an event that should have emitted immediately.
Switching to an injectable `nowMs` clock made every debounce/hysteresis test
deterministic. Production still uses `System.currentTimeMillis()`.

### 4. Why `LifecycleEventObserver` not `DefaultLifecycleObserver`?

The task spec called out `LifecycleEventObserver` explicitly. Functionally
equivalent for our use case (we only care about ON_RESUME / ON_PAUSE) but
`DefaultLifecycleObserver` would let us drop the `when` block. Sticking with
the spec keeps reviewability simple.

### 5. Battery Saver tilt suppression — per-emit vs per-subscription?

The `tiltEvents()` Flow re-checks `PowerManager.isPowerSaveMode` on every
single emit. Cost: one boolean read per tilt event (~16 Hz). Benefit: when
the user enables Battery Saver mid-game, tilt parallax stops immediately
without us needing a `BroadcastReceiver` on `ACTION_POWER_SAVE_MODE_CHANGED`.
Acceptable trade-off; revisit if profiling shows the read dominates.

### 6. AndroidManifest sensor uses-feature

All three `<uses-feature>` declarations are `required="false"`. Rationale:

- Tablets without proximity sensors should still be installable; we just
  silently skip the proximity-pause feature.
- Devices without gyroscope (still common in low-end emerging-market segments)
  should install fine; tilt parallax becomes a no-op.
- Light sensor is universal but we mark it `required="false"` for symmetry.

No `<uses-permission>` required — Android sensor APIs are
permission-free for these three sensor types. Documented inline in the manifest
and in the `SensorService.android.kt` class kdoc.

## Known caveats / out of scope

1. **Real-device battery validation** — TC-SE11 (Battery Historian <2% drain
   over 10 min) is a macrobenchmark that runs on a CI'd Pixel 4a, not in
   Robolectric. Wire it up when the macrobench harness lands.

2. **MainActivity is a skeleton** — `MainActivity.onCreate` currently only
   binds the sensor service. Compose `setContent { SudokuTheme { ... } }` and
   navigation wiring stay in `:composeApp`'s `App.kt` until the ViewModels
   migrate to `:domain` in a later wave. The `class MainActivity : ComponentActivity()`
   stub exists so `SensorServiceProvider.bind(this, this)` has a real Activity
   lifecycle to bind to.

3. **No AGP / Kotlin / Compose BoM bumps** — kept the Wave 1 toolchain
   versions exactly. The `kotlin.mpp.androidGradlePluginCompatibility.nowarn`
   warning is still expected; ignore for now (AGP 8.6 > KGP 8.5 tested ceiling).

4. **AndroidManifest is minimal** — only `<uses-feature>` declarations, no
   `<application>` element. AGP 8.6 manifest merger synthesizes the rest.
   When `:androidApp` becomes the production app entry point, the manifest
   will need a full `<application android:name=".SudokuApplication">` +
   `<activity android:name=".MainActivity">` with launcher intent-filter —
   that's separate work tracked under the UI-migration ticket.

## Verifying locally

```pwsh
# From repo root in a fresh checkout, after Wave 1 prerequisites are in place
# (root build.gradle.kts, gradle wrapper, shared/ module copied in if untracked):

./gradlew.bat -c settings-domain-only.gradle.kts :domain:allTests
# → BUILD SUCCESSFUL, 51 tests pass

./gradlew.bat :androidApp:testDebugUnitTest
# → BUILD SUCCESSFUL, existing 22 theme tests still green
```
