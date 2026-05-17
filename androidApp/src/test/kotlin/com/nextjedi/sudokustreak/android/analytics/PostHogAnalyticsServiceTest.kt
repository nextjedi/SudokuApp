package com.nextjedi.sudokustreak.android.analytics

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.nextjedi.sudokustreak.domain.analytics.AnalyticsEvent
import com.posthog.PostHogInterface
import com.posthog.android.PostHogAndroidConfig
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

/**
 * Unit tests for [PostHogAnalyticsService] covering the privacy invariants from
 * `REVAMP_PLAN.md` §4 and `test-plan/11-alpha-user-5-camila-privacy.md`:
 *
 *  1. No init before opt-in (verified by the host-placeholder short-circuit).
 *  2. No PII in any event property.
 *  3. Anonymous UUID distinct id (no Android ID / IMEI / ad-id).
 *  4. Pre-opt-in events go to disk; deleted (not replayed) on init.
 *  5. Shutdown clears SDK state.
 *  6. Concurrent init is safe.
 *  7. Track failures never propagate.
 *
 * Robolectric `manifest = Config.NONE` + `application = Application::class` mirrors the
 * theme test pattern (see ThemeTest.kt §40) — the production manifest references the
 * SudokuApplication class that lives in a different package than the namespace; for
 * pure-unit tests we don't need that.
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    manifest = Config.NONE,
    application = Application::class,
)
class PostHogAnalyticsServiceTest {

    private lateinit var ctx: Context
    private lateinit var bufferDir: File
    private lateinit var distinctIdFile: File
    private val warnings = mutableListOf<Pair<String, String>>()
    private val captureLogger: (String, String) -> Unit = { tag, msg ->
        warnings += tag to msg
    }

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
        // Per-test scratch dirs in cache — avoids leaking state between tests.
        bufferDir = File(ctx.cacheDir, "ph-buffer-${UUID.randomUUID()}")
        distinctIdFile = File(ctx.cacheDir, "ph-id-${UUID.randomUUID()}")
        warnings.clear()
    }

    @After
    fun tearDown() {
        bufferDir.deleteRecursively()
        distinctIdFile.delete()
    }

    // Convenience builder. Default factory throws if invoked — the host-placeholder /
    // blank tests must short-circuit BEFORE reaching the factory. Tests that allow init
    // pass a real factory returning a MockK PostHogInterface.
    private fun service(
        host: String = "https://posthog.example.com",
        apiKey: String = "phc_test",
        factory: (Application, PostHogAndroidConfig) -> PostHogInterface = { _, _ ->
            error("postHogFactory should not be called when initialize() short-circuits")
        }
    ): PostHogAnalyticsService = PostHogAnalyticsService(
        context = ctx,
        apiKey = apiKey,
        host = host,
        bufferDir = bufferDir,
        distinctIdFile = distinctIdFile,
        postHogFactory = factory,
        logger = captureLogger
    )

    // ---------- 1. Refusal-to-init cases ----------

    @Test
    fun does_not_initialize_when_host_is_blank() {
        val svc = service(host = "") // factory throws if called
        svc.initialize()
        assertThat(svc.isInitialized).isFalse()
        // Warning logged for diagnosability.
        assertThat(warnings.any { it.second.contains("POSTHOG_HOST") }).isTrue()
    }

    @Test
    fun does_not_initialize_when_host_is_placeholder() {
        val svc = service(host = PostHogAnalyticsService.PLACEHOLDER_HOST)
        svc.initialize()
        assertThat(svc.isInitialized).isFalse()
        assertThat(warnings.any { it.second.contains("posthog.example.local") }).isTrue()
    }

    // ---------- 2. Happy path ----------

    @Test
    fun initialize_creates_posthog_instance_when_configured() {
        val mockPh = mockk<PostHogInterface>(relaxed = true)
        val svc = service(factory = { _, _ -> mockPh })
        svc.initialize()
        assertThat(svc.isInitialized).isTrue()
        // identify() called with the anonymous UUID — verified separately below.
        verify { mockPh.identify(any(), any(), any()) }
    }

    @Test
    fun initialize_passes_host_and_privacy_defaults_to_config() {
        var capturedConfig: PostHogAndroidConfig? = null
        val mockPh = mockk<PostHogInterface>(relaxed = true)
        val svc = PostHogAnalyticsService(
            context = ctx,
            apiKey = "phc_acme",
            host = "https://posthog.acme.test",
            bufferDir = bufferDir,
            distinctIdFile = distinctIdFile,
            postHogFactory = { _, cfg ->
                capturedConfig = cfg
                mockPh
            },
            logger = captureLogger
        )
        svc.initialize()
        val cfg = checkNotNull(capturedConfig) { "factory was not invoked" }
        assertThat(cfg.host).isEqualTo("https://posthog.acme.test")
        assertThat(cfg.apiKey).isEqualTo("phc_acme")
        // Privacy posture: we do NOT enable screen-view or deep-link capture by default.
        assertThat(cfg.captureScreenViews).isFalse()
        assertThat(cfg.captureDeepLinks).isFalse()
    }

    // ---------- 3. Pre-opt-in buffer ----------

    @Test
    fun track_before_init_buffers_to_disk() {
        val svc = service(host = PostHogAnalyticsService.PLACEHOLDER_HOST) // never inits
        svc.track(AnalyticsEvent.GameStarted("HARD"))
        svc.track(AnalyticsEvent.FirstLaunch)
        val bufferFile = File(bufferDir, PostHogAnalyticsService.BUFFER_FILE_NAME)
        assertThat(bufferFile.exists()).isTrue()
        val contents = bufferFile.readText()
        assertThat(contents).contains("\"event\":\"game_started\"")
        assertThat(contents).contains("\"event\":\"first_launch\"")
        // Two lines, one per event.
        assertThat(contents.split('\n').filter { it.isNotBlank() }).hasSize(2)
    }

    @Test
    fun initialize_clears_pre_opt_in_buffer() {
        // Buffer some events first.
        val refusing = service(host = "") // factory not called
        refusing.track(AnalyticsEvent.FirstLaunch)
        refusing.track(AnalyticsEvent.GameStarted("EASY"))
        refusing.track(AnalyticsEvent.GameStarted("MEDIUM"))
        val bufferFile = File(bufferDir, PostHogAnalyticsService.BUFFER_FILE_NAME)
        assertThat(bufferFile.exists()).isTrue()

        // Now initialize a NEW service (same buffer dir, valid host) — buffer must be
        // deleted, NOT replayed (privacy: pre-consent events shouldn't be uploaded).
        val mockPh = mockk<PostHogInterface>(relaxed = true)
        val svc = service(factory = { _, _ -> mockPh })
        svc.initialize()
        assertThat(bufferFile.exists()).isFalse()
        assertThat(bufferDir.exists()).isFalse()
        // No capture() calls for the buffered events — they were dropped, not replayed.
        verify(exactly = 0) { mockPh.capture(event = "first_launch", any(), any(), any(), any(), any(), any()) }
        verify(exactly = 0) { mockPh.capture(event = "game_started", any(), any(), any(), any(), any(), any()) }
    }

    // ---------- 4. Shutdown ----------

    @Test
    fun shutdown_clears_posthog_instance() {
        val mockPh = mockk<PostHogInterface>(relaxed = true)
        val svc = service(factory = { _, _ -> mockPh })
        svc.initialize()
        assertThat(svc.isInitialized).isTrue()
        svc.shutdown()
        assertThat(svc.isInitialized).isFalse()
        verify { mockPh.close() }
    }

    @Test
    fun shutdown_when_not_initialized_is_noop() {
        val svc = service(host = PostHogAnalyticsService.PLACEHOLDER_HOST)
        // Should not throw.
        svc.shutdown()
        assertThat(svc.isInitialized).isFalse()
    }

    // ---------- 5. Distinct id ----------

    @Test
    fun distinct_id_is_anonymous_uuid() {
        val svc = service(host = PostHogAnalyticsService.PLACEHOLDER_HOST)
        val id = svc.getOrCreateDistinctId()
        // UUID v4 format: 8-4-4-4-12 lowercase hex.
        assertThat(id).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
        // No leakage of common fingerprint sources.
        val androidId: String? = try {
            android.provider.Settings.Secure.getString(
                ctx.contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            )
        } catch (_: Throwable) {
            null
        }
        if (!androidId.isNullOrEmpty()) {
            assertThat(id).isNotEqualTo(androidId)
        }
        // Build.SERIAL is deprecated and returns "unknown" on API 29+; we intentionally
        // do NOT compare against it — the structural UUID regex above is sufficient
        // proof that we are not leaking a device fingerprint.
    }

    @Test
    fun distinct_id_persists_across_init() {
        val svc1 = service(host = PostHogAnalyticsService.PLACEHOLDER_HOST)
        val id1 = svc1.getOrCreateDistinctId()
        // New service instance — should read the SAME id from disk.
        val svc2 = service(host = PostHogAnalyticsService.PLACEHOLDER_HOST)
        val id2 = svc2.getOrCreateDistinctId()
        assertThat(id2).isEqualTo(id1)
    }

    @Test
    fun distinct_id_used_in_identify_call() {
        val mockPh = mockk<PostHogInterface>(relaxed = true)
        val svc = service(factory = { _, _ -> mockPh })
        val expectedId = svc.getOrCreateDistinctId()
        svc.initialize()
        verify { mockPh.identify(expectedId, null, null) }
    }

    // ---------- 6. Tracking after init ----------

    @Test
    fun track_after_init_calls_posthog_capture() {
        val mockPh = mockk<PostHogInterface>(relaxed = true)
        val svc = service(factory = { _, _ -> mockPh })
        svc.initialize()
        svc.track(AnalyticsEvent.GameCompleted("HARD", 240, 1))
        verify {
            mockPh.capture(
                event = "game_completed",
                distinctId = any(),
                properties = match {
                    it["difficulty"] == "HARD" &&
                        it["time_s"] == 240 &&
                        it["mistakes"] == 1
                },
                userProperties = any(),
                userPropertiesSetOnce = any(),
                groups = any(),
                timestamp = any()
            )
        }
    }

    @Test
    fun screen_after_init_calls_posthog_screen() {
        val mockPh = mockk<PostHogInterface>(relaxed = true)
        val svc = service(factory = { _, _ -> mockPh })
        svc.initialize()
        svc.screen("home")
        verify { mockPh.screen(screenTitle = "home", properties = null) }
        svc.screen("game", mapOf("difficulty" to "EASY"))
        verify {
            mockPh.screen(
                screenTitle = "game",
                properties = match { it["difficulty"] == "EASY" }
            )
        }
    }

    @Test
    fun screen_before_init_is_dropped_not_buffered() {
        val svc = service(host = PostHogAnalyticsService.PLACEHOLDER_HOST)
        svc.screen("home")
        val bufferFile = File(bufferDir, PostHogAnalyticsService.BUFFER_FILE_NAME)
        // Screen events are NOT written to the buffer (would be unbounded).
        assertThat(bufferFile.exists()).isFalse()
    }

    // ---------- 7. PII / robustness ----------

    @Test
    fun no_pii_in_event_properties() {
        // Exhaustive scan of every event type — fails if anyone adds a PII key.
        val all = listOf(
            AnalyticsEvent.FirstLaunch,
            AnalyticsEvent.GameStarted("EASY"),
            AnalyticsEvent.GameCompleted("EASY", 30, 0),
            AnalyticsEvent.StylusFirstUse,
            AnalyticsEvent.SettingsChanged("soundEnabled"),
            AnalyticsEvent.AnalyticsOptedIn,
            AnalyticsEvent.AnalyticsOptedOut
        )
        val forbidden = setOf(
            "email", "name", "first_name", "last_name", "phone", "phone_number",
            "user_id", "userid", "uid", "account_id", "device_id", "deviceid",
            "android_id", "androidid", "imei", "idfa", "idfv", "ad_id", "adid",
            "ip", "ip_address", "mac", "mac_address", "location", "lat", "lng",
            "address"
        )
        for (event in all) {
            for ((key, value) in event.properties) {
                assertThat(forbidden.contains(key.lowercase())).isFalse()
                // Values themselves are constrained: primitives only — no PII-shaped strings.
                if (value is String) {
                    assertThat(value).doesNotContain("@") // crude email-substring check
                }
            }
        }
    }

    @Test
    fun concurrent_init_is_safe() = runBlocking {
        val callCount = AtomicInteger(0)
        val mockPh = mockk<PostHogInterface>(relaxed = true)
        val svc = PostHogAnalyticsService(
            context = ctx,
            apiKey = "phc_test",
            host = "https://posthog.example.com",
            bufferDir = bufferDir,
            distinctIdFile = distinctIdFile,
            postHogFactory = { _, _ ->
                callCount.incrementAndGet()
                mockPh
            },
            logger = captureLogger
        )
        // Fire 16 concurrent inits.
        val jobs = (1..16).map { async { svc.initialize() } }
        jobs.awaitAll()
        assertThat(svc.isInitialized).isTrue()
        // CAS guarantees the cached instance is set once. Factory MAY be called
        // multiple times under high contention (each loser closes its own instance),
        // but only one survives in the AtomicReference and the SDK was close()'d for
        // the losers — verified by mock interactions.
        assertThat(callCount.get()).isAtLeast(1)
        // close() called once per losing race (=callCount - 1).
        verify(atLeast = (callCount.get() - 1)) { mockPh.close() }
    }

    @Test
    fun track_handles_posthog_throwable() {
        val mockPh = mockk<PostHogInterface>(relaxed = false).apply {
            every {
                capture(any(), any(), any(), any(), any(), any(), any())
            } throws RuntimeException("simulated transport failure")
            every { identify(any(), any(), any()) } returns Unit
            every { close() } returns Unit
        }
        val svc = service(factory = { _, _ -> mockPh })
        svc.initialize()
        // Must NOT propagate.
        svc.track(AnalyticsEvent.GameStarted("EXPERT"))
        // Warning was logged for diagnosability.
        assertThat(warnings.any { it.second.contains("simulated transport failure") }).isTrue()
    }

    @Test
    fun initialize_handles_factory_throwable() {
        val svc = service(factory = { _, _ -> throw RuntimeException("init blew up") })
        // Must NOT propagate.
        svc.initialize()
        assertThat(svc.isInitialized).isFalse()
        assertThat(warnings.any { it.second.contains("init blew up") }).isTrue()
    }

    @Test
    fun buffer_rotates_on_overflow() {
        val svc = service(host = PostHogAnalyticsService.PLACEHOLDER_HOST)
        // Track enough events to exceed the 1 MiB cap. Each line is ~40 bytes; 30k of
        // them is ~1.2 MB. We use a smaller loop and assert the cap behavior via a
        // direct length check after a manual overflow.
        repeat(50) { svc.track(AnalyticsEvent.GameStarted("EASY")) }
        val bufferFile = File(bufferDir, PostHogAnalyticsService.BUFFER_FILE_NAME)
        assertThat(bufferFile.exists()).isTrue()
        // Manually inflate the file past the cap and verify the next write truncates.
        bufferFile.writeText("X".repeat((PostHogAnalyticsService.MAX_BUFFER_BYTES + 1).toInt()))
        svc.track(AnalyticsEvent.FirstLaunch)
        // After overflow, file holds only the latest line.
        val after = bufferFile.readText()
        assertThat(after.split('\n').filter { it.isNotBlank() }).hasSize(1)
        assertThat(after).contains("first_launch")
    }
}
