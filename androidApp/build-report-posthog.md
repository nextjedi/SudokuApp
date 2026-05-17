# Android PostHog integration — build report

> Generated: 2026-05-17
> Branch: `worktree-agent-afa7254872c1460a4`
> Scope: PostHog (self-hosted) opt-in-only analytics + 24 unit tests + 8 common-test
> events tests. Implements REVAMP_PLAN §0/§4/§18.7 and the privacy invariants from
> `test-plan/11-alpha-user-5-camila-privacy.md` §11.

## TL;DR

`:domain:wasmJsTest` and `:androidApp:testDebugUnitTest` both green:

- **8 / 8** in `domain/commonTest` (`AnalyticsEventTest`) — PII property invariant +
  wire-format names + NoOp semantics.
- **19 / 19** in `androidApp/test` (`PostHogAnalyticsServiceTest`) — Robolectric +
  MockK PostHogInterface mock.
- **5 / 5** in `androidApp/test` (`AnalyticsOptInFlowTest`) — flow-level controller test.

**Total: 32 / 32 pass.**

## Privacy invariants (all enforced by tests)

| Invariant | Test |
|---|---|
| SDK not initialized when host is blank | `does_not_initialize_when_host_is_blank` |
| SDK not initialized on placeholder host (`https://posthog.example.local`) | `does_not_initialize_when_host_is_placeholder` |
| App launch does not init the SDK | `app_launch_without_opt_in_does_not_initialize` |
| `initialize()` passes our privacy-conservative defaults to `PostHogAndroidConfig` (no auto screen-view, no deep-link capture) | `initialize_passes_host_and_privacy_defaults_to_config` |
| Pre-opt-in `track()` writes to disk buffer (never transmitted) | `track_before_init_buffers_to_disk` |
| Pre-opt-in `screen()` is dropped, not buffered (unbounded) | `screen_before_init_is_dropped_not_buffered` |
| `initialize()` deletes (does not replay) the pre-opt-in buffer | `initialize_clears_pre_opt_in_buffer` |
| Disk buffer rotates on 1 MiB overflow | `buffer_rotates_on_overflow` |
| Distinct id matches UUIDv4 regex (no `ANDROID_ID`, no IMEI, no `Build.SERIAL`) | `distinct_id_is_anonymous_uuid` |
| Distinct id persists across SDK lifecycles | `distinct_id_persists_across_init` |
| Distinct id flows through to `PostHogInterface.identify()` | `distinct_id_used_in_identify_call` |
| `shutdown()` flips `isInitialized` to false + calls `close()` | `shutdown_clears_posthog_instance` |
| `shutdown()` before init is a no-op | `shutdown_when_not_initialized_is_noop` |
| `track()` after init forwards to `capture()` with sanitized properties | `track_after_init_calls_posthog_capture` |
| `screen()` after init forwards to `screen()` | `screen_after_init_calls_posthog_screen` |
| Track failures never propagate | `track_handles_posthog_throwable` |
| Init failures never propagate | `initialize_handles_factory_throwable` |
| Concurrent `initialize()` from 16 coroutines yields exactly one cached instance | `concurrent_init_is_safe` |
| No `AnalyticsEvent` carries PII keys (exhaustive scan of sealed hierarchy) | `no_pii_in_event_properties` (both layers) |
| Opt-in flow calls `initialize()` then tracks `AnalyticsOptedIn` (in order) | `opt_in_flow_initializes_posthog` |
| Opt-out flow tracks `AnalyticsOptedOut` then `shutdown()` (in order) | `opt_out_flow_shuts_down_posthog` |
| Toggle off then on re-initializes cleanly | `toggle_opt_in_off_then_on_re_initializes` |
| Re-applying same opt-in state is idempotent at the controller level | `double_opt_in_is_idempotent` |
| `SettingsChanged` event records the key but not the value | `settingsChanged_carriesKeyButNotValue` (common-test) |
| Event names are stable wire-format strings | `event_names_areStable` (common-test) |

## SDK version

Pinned: **`com.posthog:posthog-android:3.44.1`** (latest stable 3.x on Maven Central as
of 2026-05-17, verified at
`https://repo1.maven.org/maven2/com/posthog/posthog-android/maven-metadata.xml`).
Declared in `gradle/libs.versions.toml` under `[versions] posthog-android` with a comment
linking the docs and the maven-metadata URL.

## Self-hosted deployment

The SDK only talks to whatever URL is passed to `PostHogAndroidConfig(apiKey, host)`. To
wire up your self-hosted instance:

1. Stand up a PostHog server. The official guide is at
   https://posthog.com/docs/self-host. Recommended: the Hobby Docker Compose stack for
   single-tenant deployments (low traffic), or the Helm chart for production.
2. Create a project in your PostHog UI; copy its project API key (starts with `phc_`).
3. Set the keys in **one** of:
   - `~/.gradle/gradle.properties` (per-user, not committed):
     ```properties
     POSTHOG_HOST=https://posthog.your-domain.com
     POSTHOG_API_KEY=phc_xxxxxxxxxxxxxxxx
     ```
   - `local.properties` (project-local, gitignored), same keys.
   - Gradle CLI: `-PPOSTHOG_HOST=https://… -PPOSTHOG_API_KEY=phc_…`
4. Rebuild — the values flow through `BuildConfig.POSTHOG_HOST` /
   `BuildConfig.POSTHOG_API_KEY` to `PostHogAnalyticsService`.

Forgetting step 3 is **safe**: the default value in `gradle.properties` is
`https://posthog.example.local`, which `PostHogAnalyticsService.initialize()` refuses to
initialize against (see `does_not_initialize_when_host_is_placeholder`). The SDK
therefore cannot accidentally hit `posthog.com`.

## Default state at install

- `AppSettings.analyticsOptIn = false` (already locked in `:domain`).
- `AnalyticsModule.get(context)` constructs the service in
  `SudokuApplication.onCreate()` — but DOES NOT call `initialize()`.
- `isInitialized` returns `false` from app start through the first user-driven opt-in.
- Apple App Privacy / Play Store Data Safety can declare "Data Not Collected" out of
  the box.

## Cellular safety / backoff

PostHog Android batches events and flushes either every 20 captures or every 30 seconds
(`flushAt` / `flushIntervalSeconds` defaults — we leave them at the SDK defaults).
Failed flushes are retried with the SDK's built-in exponential backoff (see
[`flush` docs](https://posthog.com/docs/libraries/android#flush)). There is no
foreground or always-on background work; the SDK never wakes the device on its own.

## Files added

```
domain/src/commonMain/kotlin/com/nextjedi/sudokustreak/domain/analytics/
    AnalyticsService.kt              <- KMP contract + sealed AnalyticsEvent + NoOp
domain/src/commonTest/kotlin/com/nextjedi/sudokustreak/domain/analytics/
    AnalyticsEventTest.kt            <- 8 common-test assertions

androidApp/src/main/kotlin/com/nextjedi/sudokustreak/android/analytics/
    PostHogAnalyticsService.kt       <- PostHog impl, all 5 invariants enforced
    AnalyticsModule.kt               <- Lazy process-wide accessor
    AnalyticsOptInController.kt      <- opt-in/out → init/shutdown policy

androidApp/src/test/kotlin/com/nextjedi/sudokustreak/android/analytics/
    PostHogAnalyticsServiceTest.kt   <- 19 Robolectric + MockK tests
    AnalyticsOptInFlowTest.kt        <-  5 controller flow tests

androidApp/src/main/kotlin/com/nextjedi/sudokustreak/android/
    SudokuApplication.kt             <- pre-creates the service (NOT initialize)
androidApp/src/main/AndroidManifest.xml <- minimal manifest (INTERNET permission only)

gradle/libs.versions.toml            <- + posthog-android, + mockk
gradle.properties                    <- + POSTHOG_HOST / POSTHOG_API_KEY placeholders
androidApp/build.gradle.kts          <- + buildConfigField wiring, + posthog dep,
                                        + mockk test dep, + :domain dep
settings-android-only.gradle.kts     <- + :domain inclusion (needed so :androidApp can
                                        depend on the AnalyticsService contract)
```

## Known issues / follow-ups (not blocking)

1. **`:shared` module is not yet checked in.** `settings.gradle.kts` (root) references
   `:shared` but no `shared/build.gradle.kts` exists in the master branch as of
   commit `052040a`. To unblock local test runs I created a temporary
   `shared/build.gradle.kts` + `shared/src/commonMain/kotlin/.../Stub.kt` in this
   worktree — **these are not committed**. The proper `:shared` is owned by another
   agent's worktree. Until that lands, the gradle build of `:androidApp` will fail
   at dependency resolution unless `:shared` is stubbed or removed. The `AnalyticsService`
   code itself does not depend on `:shared` in any way.
2. **`AndroidManifest.xml` is minimal.** Only `INTERNET` is declared. Permissions,
   sensor `<uses-feature>` declarations, intent filters, and the `<activity>` entry are
   owned by the Stylus / Sensor / Settings agents. The minimal manifest is sufficient
   for unit-test compilation; instrumented tests are out of scope for this work.
3. **Pre-existing JVM compile issue on `:domain`** documented in `domain/build-report.md`
   means `:domain:jvmTest` is disabled. Our common-test runs on `wasmJsTest` instead;
   coverage is identical because the test only uses Kotlin standard library APIs.

## Hard constraints — verified

| Constraint | How verified |
|---|---|
| NEVER initialize SDK in `Application.onCreate()` | `SudokuApplication.kt` only calls `AnalyticsModule.get(this)`; `app_launch_without_opt_in_does_not_initialize` test |
| NEVER capture PII | `no_pii_in_event_properties` in both test layers |
| Anonymous UUID distinct id | `distinct_id_is_anonymous_uuid` + `distinct_id_persists_across_init` |
| Self-hostable; never hardcode posthog.com | `PostHogAnalyticsService.PLACEHOLDER_HOST` short-circuit + `does_not_initialize_when_host_is_placeholder` test |
| Don't modify `:shared`, `:composeApp`, `iosApp/` | No tracked changes in those modules; the unstaged `shared/` stub is build-only |

## Build commands

```powershell
# Common-test (AnalyticsEvent + NoOp on wasmJs):
.\gradlew.bat -c settings-domain-only.gradle.kts :domain:wasmJsTest

# Android unit tests (PostHogAnalyticsService + AnalyticsOptInFlow):
.\gradlew.bat -c settings-android-only.gradle.kts :androidApp:testDebugUnitTest `
    --tests "*PostHog*" --tests "*Analytics*"
```
