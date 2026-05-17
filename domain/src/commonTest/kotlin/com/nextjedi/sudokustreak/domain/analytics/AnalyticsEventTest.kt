package com.nextjedi.sudokustreak.domain.analytics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Multiplatform tests for the [AnalyticsService] contract:
 *  - [NoOpAnalyticsService] never reports initialized and never throws.
 *  - [AnalyticsEvent] names match the wire-format names used in the PostHog dashboard.
 *  - Event properties never contain PII keys (email, name, id, etc.) — this is the
 *    invariant guard that fails the build if a new event accidentally adds a leaky
 *    property name.
 */
class AnalyticsEventTest {

    @Test
    fun noOp_isNotInitialized() {
        val svc = NoOpAnalyticsService()
        assertFalse(svc.isInitialized, "NoOp must never report itself initialized")
    }

    @Test
    fun noOp_doesNotThrow_onAnyMethod() {
        val svc = NoOpAnalyticsService()
        svc.initialize()
        svc.track(AnalyticsEvent.FirstLaunch)
        svc.track(AnalyticsEvent.GameStarted("HARD"))
        svc.screen("home")
        svc.shutdown()
        // If we got here, none of them threw.
        assertFalse(svc.isInitialized)
    }

    @Test
    fun event_names_areStable() {
        // These wire-format strings are referenced in dashboards / alerts — any rename
        // here is a breaking change.
        assertEquals("first_launch", AnalyticsEvent.FirstLaunch.name)
        assertEquals("game_started", AnalyticsEvent.GameStarted("EASY").name)
        assertEquals("game_completed", AnalyticsEvent.GameCompleted("EASY", 60, 0).name)
        assertEquals("stylus_first_use", AnalyticsEvent.StylusFirstUse.name)
        assertEquals("settings_changed", AnalyticsEvent.SettingsChanged("soundEnabled").name)
        assertEquals("analytics_opted_in", AnalyticsEvent.AnalyticsOptedIn.name)
        assertEquals("analytics_opted_out", AnalyticsEvent.AnalyticsOptedOut.name)
    }

    @Test
    fun gameStarted_propertiesCarryDifficultyOnly() {
        val props = AnalyticsEvent.GameStarted("HARD").properties
        assertEquals(1, props.size)
        assertEquals("HARD", props["difficulty"])
    }

    @Test
    fun gameCompleted_propertiesCarryAggregateMetricsOnly() {
        val props = AnalyticsEvent.GameCompleted("EXPERT", 423, 2).properties
        assertEquals(3, props.size)
        assertEquals("EXPERT", props["difficulty"])
        assertEquals(423, props["time_s"])
        assertEquals(2, props["mistakes"])
    }

    @Test
    fun settingsChanged_carriesKeyButNotValue() {
        // Privacy invariant: we record THAT a setting changed, not its new value.
        val props = AnalyticsEvent.SettingsChanged("themeMode").properties
        assertEquals(1, props.size)
        assertEquals("themeMode", props["key"])
        assertNull(props["value"], "SettingsChanged must never carry a `value` property")
    }

    @Test
    fun objects_haveEmptyProperties() {
        assertTrue(AnalyticsEvent.FirstLaunch.properties.isEmpty())
        assertTrue(AnalyticsEvent.StylusFirstUse.properties.isEmpty())
        assertTrue(AnalyticsEvent.AnalyticsOptedIn.properties.isEmpty())
        assertTrue(AnalyticsEvent.AnalyticsOptedOut.properties.isEmpty())
    }

    @Test
    fun noEvent_carriesPiiKeys() {
        // Exhaustive scan of every event in the sealed hierarchy. New events must be
        // added here (the compiler can't enforce sealed-class exhaustiveness for
        // iteration), but if anyone adds a PII property key the test fails.
        val all: List<AnalyticsEvent> = listOf(
            AnalyticsEvent.FirstLaunch,
            AnalyticsEvent.GameStarted("EASY"),
            AnalyticsEvent.GameCompleted("EASY", 1, 0),
            AnalyticsEvent.StylusFirstUse,
            AnalyticsEvent.SettingsChanged("soundEnabled"),
            AnalyticsEvent.AnalyticsOptedIn,
            AnalyticsEvent.AnalyticsOptedOut
        )
        val forbidden = setOf(
            "email", "name", "first_name", "last_name", "phone", "phone_number",
            "user_id", "userId", "uid", "account_id", "device_id", "deviceId",
            "android_id", "androidId", "imei", "idfa", "idfv", "ad_id", "adId",
            "ip", "ip_address", "mac", "mac_address", "location", "lat", "lng",
            "address", "value" // free-text values forbidden for SettingsChanged
        )
        for (event in all) {
            for (key in event.properties.keys) {
                assertFalse(
                    forbidden.contains(key.lowercase()),
                    "Event '${event.name}' carries forbidden PII property '$key'"
                )
            }
        }
    }
}
