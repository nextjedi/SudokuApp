package com.nextjedi.sudokustreak.android.analytics

import android.app.Application
import android.content.Context
import android.util.Log
import com.nextjedi.sudokustreak.domain.analytics.AnalyticsEvent
import com.nextjedi.sudokustreak.domain.analytics.AnalyticsService
import com.posthog.PostHogInterface
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

/**
 * Android implementation of [AnalyticsService] backed by a **self-hosted** PostHog
 * instance (locked decision §18.7 in `REVAMP_PLAN.md`).
 *
 * ## Privacy contract (tested — see `PostHogAnalyticsServiceTest.kt`)
 *
 * 1. **No initialization before opt-in.** [initialize] is a no-op until the host is
 *    a non-placeholder URL AND the caller (the Settings ViewModel) invokes it after
 *    `AppSettings.analyticsOptIn` flips to `true`.
 * 2. **Refuses to fall back to posthog.com.** If the configured [host] is blank or
 *    equal to the placeholder `https://posthog.example.local`, [initialize] logs a
 *    warning and returns without instantiating the SDK. A forgotten `gradle.properties`
 *    therefore cannot accidentally leak events.
 * 3. **Anonymous distinct id.** The distinct id is a UUID generated on first init and
 *    persisted to `filesDir/analytics-distinct-id`. We **never** read `Settings.Secure.ANDROID_ID`,
 *    IMEI, MAC, ad-id, or any other device fingerprint.
 * 4. **Pre-opt-in events buffered to disk.** Any [track] call made before [initialize]
 *    succeeds appends a JSON line to `filesDir/analytics-buffer/buffer.jsonl`. The buffer
 *    is **deleted** (not replayed) on a subsequent successful [initialize] — events made
 *    before consent must never be uploaded after consent.
 * 5. **Silent failures.** All SDK calls are wrapped in `try/catch`; analytics MUST NOT
 *    propagate exceptions into UI code.
 *
 * ## Lifecycle wiring
 *
 *  - **App start:** `SudokuApplication.onCreate()` constructs an instance via
 *    [AnalyticsModule.get] — but does NOT call [initialize].
 *  - **Opt-in:** `SettingsViewModel.setAnalyticsOptIn(true)` calls
 *    [initialize]; on success it tracks [AnalyticsEvent.AnalyticsOptedIn].
 *  - **Opt-out:** `SettingsViewModel.setAnalyticsOptIn(false)` tracks
 *    [AnalyticsEvent.AnalyticsOptedOut] (still permitted while the SDK is up), then
 *    calls [shutdown].
 *
 * ## Threading
 *
 * [initialize] uses a CAS guard on [posthog] so that concurrent calls from multiple
 * coroutines result in **one** SDK instance, with all but one losing the race becoming
 * no-ops (verified by `concurrent_init_is_safe`). [track] / [screen] read the volatile
 * reference and short-circuit if null — no lock on the hot path.
 *
 * @param context any [Context] — only `applicationContext` is retained.
 * @param apiKey self-hosted PostHog project API key (`phc_…`); typically read from
 *   `BuildConfig.POSTHOG_API_KEY`. Empty string is permitted (PostHog SDK will not
 *   transmit, but we still short-circuit on the placeholder host check first).
 * @param host self-hosted PostHog URL (e.g. `https://posthog.example.com`); typically
 *   read from `BuildConfig.POSTHOG_HOST`. The placeholder `https://posthog.example.local`
 *   is treated as "unconfigured" and refused.
 * @param bufferDir directory for the disk-only pre-opt-in event buffer. Defaults to
 *   `filesDir/analytics-buffer`. Overridable for tests.
 * @param distinctIdFile file holding the persistent anonymous distinct id. Defaults to
 *   `filesDir/analytics-distinct-id`. Overridable for tests.
 * @param postHogFactory test seam — production code passes the real
 *   [PostHogAndroid.with] factory; tests pass a lambda returning a MockK mock.
 * @param logger test seam — defaults to [Log.w]; tests can capture the warning.
 */
class PostHogAnalyticsService(
    context: Context,
    private val apiKey: String,
    private val host: String,
    private val bufferDir: File = File(context.applicationContext.filesDir, "analytics-buffer"),
    private val distinctIdFile: File = File(context.applicationContext.filesDir, "analytics-distinct-id"),
    private val postHogFactory: (Application, PostHogAndroidConfig) -> PostHogInterface =
        { app, cfg -> PostHogAndroid.with(app, cfg) },
    private val logger: (String, String) -> Unit = { tag, msg -> Log.w(tag, msg) }
) : AnalyticsService {

    // ApplicationContext kept so that initialize() can call PostHogAndroid.with(app, …)
    // off the main thread without leaking an Activity. We never read user state from
    // the context — only filesDir on construction.
    private val app: Application = context.applicationContext as Application

    // Atomic so that initialize() / shutdown() / track() are thread-safe without a lock
    // on the hot path. CAS in initialize() guarantees a single SDK instance even under
    // concurrent calls.
    private val posthog = AtomicReference<PostHogInterface?>(null)

    override val isInitialized: Boolean
        get() = posthog.get() != null

    override fun initialize() {
        // Already up? Idempotent — bail.
        if (posthog.get() != null) return

        // Refuse to initialize on the placeholder host. This is the FIRST check so a
        // forgotten gradle.properties cannot accidentally hit posthog.com via a bug
        // in default-value handling further down.
        if (host.isBlank() || host == PLACEHOLDER_HOST) {
            logger(
                TAG,
                "PostHog not initialized: POSTHOG_HOST is unset or set to the " +
                    "placeholder ($PLACEHOLDER_HOST). Override in gradle.properties or " +
                    "via -PPOSTHOG_HOST=https://posthog.your.tld."
            )
            return
        }

        // Build PostHog config. Notes:
        //  - We bypass the global singleton (PostHog.with) and own the instance ourselves
        //    so that tests don't share global state.
        //  - captureScreenViews = false because we call screen() explicitly from MainActivity
        //    (so the screen-name set is exactly what we declare, not whatever activity
        //    class names happen to be).
        //  - captureDeepLinks = false because deep-link URLs can contain query-string PII
        //    (referral codes etc.). We can opt in selectively later.
        //  - sessionReplay = false (default) — recording user screens is explicitly out
        //    of scope for a privacy-first app.
        //  - debug = false — no SDK-side logging in production to avoid log-PII leaks.
        val config = PostHogAndroidConfig(apiKey = apiKey, host = host).apply {
            captureApplicationLifecycleEvents = true
            captureScreenViews = false
            captureDeepLinks = false
        }

        // Build the SDK instance off the global singleton. If PostHog throws here, we
        // log and stay uninitialized — analytics failures MUST NOT propagate.
        val newInstance: PostHogInterface = try {
            postHogFactory(app, config)
        } catch (t: Throwable) {
            logger(TAG, "PostHog initialization failed: ${t.message}")
            return
        }

        // CAS guards against a concurrent init winning the race. If we lose, close the
        // instance we just created and use the existing one.
        if (!posthog.compareAndSet(null, newInstance)) {
            try { newInstance.close() } catch (_: Throwable) { /* swallow */ }
            return
        }

        // After successful init, identify ourselves with the anonymous UUID distinct id
        // so that retention/funnel charts in PostHog can dedupe a single device.
        val distinctId = getOrCreateDistinctId()
        try {
            newInstance.identify(distinctId)
        } catch (t: Throwable) {
            logger(TAG, "PostHog identify failed (non-fatal): ${t.message}")
        }

        // Privacy invariant: delete the pre-opt-in buffer. The user has consented to
        // analytics going forward, but they have NOT consented to retroactive upload
        // of events recorded before they consented. Delete, do not replay.
        bufferDir.deleteRecursively()
    }

    override fun shutdown() {
        // Atomically take ownership of the current instance and release the AtomicReference,
        // so isInitialized flips false immediately even if close() blocks briefly.
        val instance = posthog.getAndSet(null) ?: return
        try {
            instance.close()
        } catch (t: Throwable) {
            logger(TAG, "PostHog close failed (non-fatal): ${t.message}")
        }
        // NOTE: we deliberately do NOT delete the distinctIdFile. The id is an anonymous
        // UUID — keeping it means a future opt-in by the same user produces continuous
        // (rather than a brand-new) distinct id, which improves retention dashboards
        // without leaking anything personal.
    }

    override fun track(event: AnalyticsEvent) {
        val instance = posthog.get()
        if (instance == null) {
            // Not initialized — buffer to disk. Buffer is rotated/capped at MAX_BUFFER_BYTES
            // and deleted on the next successful initialize() (privacy: not replayed).
            buffer(event)
            return
        }
        // Filter null values; PostHog typings disallow them and treating a null as a
        // real property value would confuse downstream funnels. Map<String, Any?> on
        // our side is for ergonomic data classes; the wire is Map<String, Any>.
        val sanitized: Map<String, Any> = event.properties
            .filterValues { it != null }
            .mapValues { it.value as Any }
        try {
            instance.capture(event = event.name, properties = sanitized.ifEmpty { null })
        } catch (t: Throwable) {
            // Analytics failures MUST NOT propagate.
            logger(TAG, "PostHog capture failed (non-fatal): ${t.message}")
        }
    }

    override fun screen(name: String, properties: Map<String, String>) {
        // Screen events are NOT buffered. They are unbounded (a frequent navigator could
        // generate thousands per session) and would dominate the on-disk buffer with
        // little analytical value pre-opt-in.
        val instance = posthog.get() ?: return
        val props: Map<String, Any>? = properties.takeIf { it.isNotEmpty() }
            ?.mapValues { it.value as Any }
        try {
            instance.screen(screenTitle = name, properties = props)
        } catch (t: Throwable) {
            logger(TAG, "PostHog screen failed (non-fatal): ${t.message}")
        }
    }

    // ---------- internal helpers ----------

    /**
     * Append a single JSON line for [event] to the on-disk buffer. Caps the buffer at
     * [MAX_BUFFER_BYTES]; once exceeded the oldest events are dropped (truncate-and-rewrite)
     * to bound disk usage.
     */
    private fun buffer(event: AnalyticsEvent) {
        try {
            if (!bufferDir.exists()) bufferDir.mkdirs()
            val file = File(bufferDir, BUFFER_FILE_NAME)
            // Minimal hand-rolled JSON. We deliberately avoid kotlinx.serialization here
            // because (a) the buffer is never transmitted, only deleted; (b) we don't want
            // to pull a serialization runtime into the analytics critical path.
            val line = """{"event":"${escape(event.name)}","ts":${System.currentTimeMillis()}}""" + "\n"
            file.appendText(line)
            // Cheap size check — rotate by truncating if we cross the cap.
            if (file.length() > MAX_BUFFER_BYTES) {
                file.writeText(line) // keep only the latest line on overflow
            }
        } catch (t: Throwable) {
            logger(TAG, "PostHog buffer write failed (non-fatal): ${t.message}")
        }
    }

    /**
     * Return the persisted anonymous distinct id, generating + persisting a fresh UUID
     * on first invocation. This is the only identifier we ever attach to events. We
     * deliberately do NOT use `Settings.Secure.ANDROID_ID`, IMEI, MAC, ad-id, or
     * `Build.SERIAL` — those are device fingerprints that survive uninstall.
     */
    internal fun getOrCreateDistinctId(): String {
        return try {
            if (distinctIdFile.exists() && distinctIdFile.length() > 0L) {
                distinctIdFile.readText().trim().ifEmpty { writeNewId() }
            } else {
                writeNewId()
            }
        } catch (t: Throwable) {
            logger(TAG, "PostHog distinct-id read failed; using ephemeral UUID: ${t.message}")
            UUID.randomUUID().toString()
        }
    }

    private fun writeNewId(): String {
        val id = UUID.randomUUID().toString()
        try {
            distinctIdFile.parentFile?.mkdirs()
            distinctIdFile.writeText(id)
        } catch (t: Throwable) {
            logger(TAG, "PostHog distinct-id write failed (non-fatal): ${t.message}")
        }
        return id
    }

    private fun escape(s: String): String = s
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")

    internal companion object {
        internal const val TAG = "PostHog"
        // The placeholder host that gradle.properties ships with. PostHogAnalyticsService
        // refuses to initialize on this value so a forgotten config can never leak to
        // posthog.com. KEEP IN SYNC WITH gradle.properties (POSTHOG_HOST).
        internal const val PLACEHOLDER_HOST = "https://posthog.example.local"
        // 1 MiB cap on the disk-only pre-opt-in buffer. Beyond this we drop oldest events
        // to bound disk usage on devices where the user never opts in but plays a lot.
        internal const val MAX_BUFFER_BYTES: Long = 1L * 1024L * 1024L
        internal const val BUFFER_FILE_NAME = "buffer.jsonl"
    }
}
