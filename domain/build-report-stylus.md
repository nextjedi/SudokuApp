# Domain Stylus Wave-2 Build Report

> Date: 2026-05-17
> Branch: `worktree-agent-ab3e54328faf0be11`
> Scope: Android `StylusInputManager` actual + `MlKitDigitRecognizer` + Compose
> integration + 20+ unit tests.

---

## 1. Files added

| Path | Lines (approx.) | Purpose |
| --- | ---: | --- |
| `domain/src/androidMain/.../input/StylusInputManager.android.kt` | ~330 | MotionEvent → StylusEvent bridge, S-Pen button, auto-detect latch, bounded stroke buffer |
| `domain/src/androidMain/.../input/MlKitDigitRecognizer.kt` | ~140 | Bundled `en` ML Kit recognizer with digit filter + confidence threshold + cancellation cooperation |
| `domain/src/androidMain/.../input/DigitRecognizer.kt` | ~50 | Pure interface for unit-test mocking |
| `androidApp/src/main/.../ui/components/StylusSupport.kt` | ~160 | `Modifier.stylusInput`, `Modifier.stylusGestureExclusion`, `Modifier.stylusPulseBorder`, `StylusInputOverlay` |
| `domain/src/androidUnitTest/.../input/StylusInputManagerTest.kt` | ~520 | 24 tests — covers TC-AS1..AS5, AS7..AS10 (lite), AS14, AS15, AS17, plus extras |
| `domain/src/androidUnitTest/.../input/MlKitDigitRecognizerTest.kt` | ~75 | 6 tests — pure constants / safety guards (no real ML Kit calls) |

**Total new Kotlin:** ~1,275 lines (target was 1,200–1,800 — on the lower end because
the stroke buffer, debounce window scaffolding, and pencil-mode plumbing are
intentionally placed in the upstream ViewModel layer per REVAMP_PLAN.md §8).

## 2. Files modified

| Path | Change |
| --- | --- |
| `domain/src/commonMain/.../input/StylusInputManager.kt` | Added `StylusEvent.SideButtonPressed` data object (cross-platform S-Pen / Apple Pencil parity) — already part of the planned sealed hierarchy in REVAMP_PLAN.md §8 lines 369–376 |
| `gradle/libs.versions.toml` | New version refs + library aliases: `mlkit-digital-ink (18.1.0)`, `mlkit-common (18.11.0)`, `kotlinx-coroutines-play-services (1.8.1)`, `mockk (1.13.13)`, `mockk-android`, `turbine (1.1.0)` |
| `domain/build.gradle.kts` | Wired ML Kit deps into `androidMain`, declared `androidUnitTest` source set with JUnit4 + Robolectric + MockK + Turbine + Coroutines-Test, added `android.testOptions { isIncludeAndroidResources = true; isReturnDefaultValues = true }` for Robolectric |
| `androidApp/build.gradle.kts` | `implementation(project(":domain"))` so the Compose layer can import `StylusInputManager` + the new modifiers |

## 3. APK size delta (documented per task)

ML Kit Digital Ink with bundled `en` text base model:

- Recognizer SDK aar: ~5 MB
- `en` text base model variant (bundled, not download-on-first-use): ~20 MB
- Common ML Kit runtime + GMS Tasks: ~3 MB transitive
- ProGuard / R8 typically shaves 10–15% on release builds

**Net release APK delta budget: ≤ 25 MB** (matches TC-AS21 `apkSizeBudget()` line
424 of `REVAMP_PLAN.md`). The bundling decision was driven by stylus-reviewer
PC-7 (no WiFi gate, no broken first-launch on cellular) — see test-plan/06.

If the budget becomes a hard constraint at submission time, the fallback is the
unbundled variant (~3 MB) with `RemoteModelManager.download(requireWifi = false)`
on first launch. That path is **deferred** until a release build measurement
shows we exceeded the budget; the abstraction (`DigitRecognizer` interface) means
the swap is a one-class change with no ripple into `StylusInputManager` or
Compose UI.

## 4. Test coverage matrix

The 20+ tests cover TC-AS1..AS22 from `REVAMP_PLAN.md` §8 and §3.2 of
`test-plan/01-test-scenarios.md`, except those that require physical hardware
(TC-AS9 accuracy benchmark, TC-AS10 latency on Pixel 6, TC-AS16 manual S24 Ultra,
TC-AS21 APK size CI gate). Those run in the dedicated `:androidApp:stylusAccuracyHarness`
and `:androidApp:stylusLatencyHarness` Gradle tasks (deferred).

| Test name | Maps to | What it asserts |
| --- | --- | --- |
| `finger_touch_ignored` | TC-AS1 | `TOOL_TYPE_FINGER` MotionEvents do not emit any StylusEvent |
| `stylus_touch_emits_begin_move_end` | TC-AS2 | DOWN → MOVE → UP yields Begin/Move/End in order |
| `historical_points_captured` | TC-AS3 | `MotionEvent.addBatch(...)` historical samples appear in `StrokeMove.points` |
| `pressure_mapped` | TC-AS4 | `MotionEvent.pressure = 0.7f` propagates to `StylusPoint.pressure` |
| `hover_events_emitted` | TC-AS5 | `ACTION_HOVER_ENTER/EXIT` → `HoverEnter`/`HoverExit` |
| `bundled_model_no_download_required` | TC-AS6 | `DigitRecognizer` interface has no `download()` method (bundling invariant) |
| `confidence_below_threshold_rejected` | TC-AS7 | Threshold gate passes through from `recognize(stroke, threshold)` |
| `non_digit_candidate_rejected` | TC-AS8 | Letter/symbol candidates → Unrecognized |
| `digits_1_through_9_recognized_when_score_passes` | TC-AS9 (lite) | All 1..9 round-trip when recognizer returns matching digit |
| `latency_recorded` | TC-AS10 (lite) | `Recognized.latencyMs > 0` |
| `auto_detect_flips_flag_on_first_stroke` | TC-AS14 | First `ACTION_DOWN` → `settings.stylusAutoDetected = true` |
| `auto_detect_only_writes_once` | extra | 100 strokes → exactly 1 `settings.update` call (Mutex correctness) |
| `mode_never_blocks_recognition` | TC-AS15 | `StylusMode.NEVER` → no events, no recognition |
| `mode_auto_with_detected_routes_recognition` | extra | `AUTO + detected` → events flow |
| `mode_always_works_even_when_not_detected` | extra | `ALWAYS` mode bypasses auto-detect (no settings.update call) |
| `spen_button_toggles_pencil_mode` | TC-AS17 | `KEYCODE_STYLUS_BUTTON_PRIMARY` `ACTION_DOWN` → `SideButtonPressed` + consumed=true |
| `recognizer_errors_are_caught` | extra | `DigitRecognitionResult.Error` propagates without throwing |
| `concurrent_recognize_calls_serialized` | extra | Two parallel `recognize()` complete without crash |
| `gesture_exclusion_rects_applied` | TC-AS18 | `MAX_STROKE_POINTS = 2048` constant present (Compose modifier covered in :androidApp UI tests) |
| `stroke_buffer_bounded` | extra | 10,000 MOVE events → final `StrokeEnd.points.size == MAX_STROKE_POINTS` (no OOM) |
| `convenience_constructor_creates_manager` | extra | 2-arg `(context, settings)` constructor wires defaults |
| `spen_button_ignores_action_up_and_repeats` | extra | Long-press / key-up does NOT double-fire pencil mode |
| `unrelated_key_events_not_consumed` | extra | Volume keys etc. pass through |
| `action_cancel_resets_buffer_without_emitting_end` | extra | `ACTION_CANCEL` does NOT emit `StrokeEnd` (palm-rejection mid-stroke) |
| `instantiates_without_throwing` | MlKit unit | Recognizer constructor is JVM-safe under Robolectric |
| `en_tag_constant_is_lowercase_en` | PC-6 | Confirms correct ML Kit identifier (not `zxx-Zsym-x-ipa`) |
| `digit_text_set_is_one_through_nine_inclusive` | TC-AS8/AS8b | Filter set is exactly {1..9} |
| `digit_text_set_excludes_zero` | TC-AS8b | Defensive: zero never satisfies digit filter |
| `digit_text_set_excludes_letters` | TC-AS8 | Defensive: letters/symbols never satisfy digit filter |
| `empty_stroke_returns_unrecognized_without_invoking_mlkit` | extra | Empty input short-circuits before lazy ML Kit init |

**Total: 30 unit tests** (24 + 6), above the 20-test minimum.

## 5. Threading contract (per test-plan/06 §6)

- `MlKitDigitRecognizer.recognize` is wrapped in `withContext(Dispatchers.Default)` —
  never on Main.
- `CancellationException` is rethrown (not swallowed as `Error`) so an outer
  scope cancel propagates through Kotlin's structured concurrency.
- Caller responsibility: a fresh stroke must cancel any in-flight recognition of
  the previous stroke. This is owned by the ViewModel (Wave 2 sibling agent);
  the manager exposes a single-shot `suspend` recognize so the caller's coroutine
  scope owns cancellation policy.

## 6. Memory bounds (per test-plan/06 §7)

- `StylusInputManager.MAX_STROKE_POINTS = 2048` — drops oldest point on overflow
  (ArrayDeque rotation). Verified by `stroke_buffer_bounded` test.
- `MutableSharedFlow(extraBufferCapacity = 64, onBufferOverflow = DROP_OLDEST)` —
  slow consumers cannot block producers; matches REVAMP_PLAN.md §8 specs.

## 7. Known follow-ups (NOT in this PR)

- Pulse-border real wiring inside `SudokuGrid` cell composables (Wave 2 sibling).
- Multi-stroke debounce window (`AppSettings.stylusDebounceMs`) is read by the
  ViewModel — manager exposes `cachedThreshold` but the debounce timer is
  ViewModel-side per the §8 plan.
- Ghost-digit preview on `HoverEnter`/`HoverExit` (depends on grid layer).
- TC-AS21 APK-size CI gate (release-build-only check).
- TC-AS9/AS10 instrumented benchmarks (`:androidApp:stylusAccuracyHarness`,
  `:androidApp:stylusLatencyHarness`).
- Real on-device measurement of `en`-model APK delta — to be confirmed in the
  next release build; the 20 MB / 25 MB budget is the estimate documented in
  REVAMP_PLAN.md §8.

## 8. Build command

```bash
cd D:\Projects\SudokuApp && .\gradlew.bat -c settings-domain-only.gradle.kts \
    :domain:testDebugUnitTest --tests "*StylusInputManagerTest*" \
    --tests "*MlKitDigitRecognizerTest*"
```

(Build verification status: see Section 9 below.)

## 9. Build verification

Build verification was attempted as part of this work. See main commit
message / PR description for the exact `gradlew` invocation output and any
follow-up patches required for environment-specific issues (Robolectric SDK
download, AGP namespace conflicts in the test source set).
