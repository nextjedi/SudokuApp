package com.nextjedi.sudokustreak.domain.settings

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for [AppSettings], [AppSettingsMigrator], and the JSON envelope contract.
 *
 * Maps to:
 * - TC-S1 serialize_defaultInstance_roundTripsEqual
 * - TC-S2 defaults_freshInstance_matchSpec
 * - TC-S2c decode_unknownFutureKey_doesNotThrow
 * - TC-S5c version field present (here: `schemaVersion` is the first field)
 * - TC-S6 version mismatch → defaults
 * - TC-S7 missing field uses default
 * - TC-S8 invalid enum string falls back
 * - TC-S9 null safety
 *
 * Plus migration-specific tests from test-plan/00-SYNTHESIS.md P0-2:
 * - migrate_v1_blob_returns_same
 * - migrate_missing_schemaVersion_uses_defaults
 * - migrate_unknown_future_version_falls_back
 * - migrate_corrupt_json_returns_defaults
 * - migrate_partial_blob_uses_defaults_for_missing
 */
class AppSettingsTest {

    private val json = Json {
        encodeDefaults = true
        prettyPrint = false
        ignoreUnknownKeys = true
    }

    // ---------- TC-S2: defaults match spec ----------

    @Test
    fun defaults_match_spec() {
        val s = AppSettings()
        // Schema
        assertEquals(1, s.schemaVersion, "schemaVersion must default to 1 (current)")
        assertEquals(AppSettings.CURRENT_SCHEMA_VERSION, s.schemaVersion)

        // Gameplay
        assertEquals(3, s.mistakeLimit)
        assertFalse(s.autoNotesEnabled)
        assertFalse(s.fastPencilEnabled)
        assertFalse(s.numberFirstModeEnabled)
        assertFalse(s.midGameBoostEnabled, "midGameBoostEnabled MUST default false")

        // Presentation
        assertEquals(ThemeMode.SYSTEM, s.themeMode)
        assertTrue(s.highlightEnabled)
        assertTrue(s.showRemainingCount)
        assertTrue(s.showTimer)
        assertTrue(s.animatedDigits)

        // Solver / Hints
        assertEquals(3, s.hintDepth)
        assertEquals(1500, s.solverSpeedMs)
        assertEquals(3, s.solverHelpCells)

        // Stylus
        assertEquals(StylusMode.AUTO, s.stylusMode)
        assertFalse(s.stylusAutoDetected)
        assertEquals(0.75f, s.stylusConfidenceThreshold)
        assertTrue(s.stylusPressureToBoldNotes)
        assertTrue(s.stylusWristRejection)
        assertEquals(300, s.stylusDebounceMs, "stylusDebounceMs MUST default 300 (multi-stroke)")

        // Sensors
        assertTrue(s.proximityAutoPauseEnabled)
        assertTrue(s.ambientLightAutoThemeEnabled)
        assertFalse(s.tiltParallaxEnabled, "tiltParallaxEnabled MUST default false (motion sickness)")

        // Accessibility
        assertFalse(s.reduceMotion)
        assertFalse(s.highContrast)
        assertEquals(ColorBlindMode.NONE, s.colorBlindMode)
        assertFalse(s.largeText)

        // Audio / Haptics / Data
        assertTrue(s.soundEnabled)
        assertTrue(s.hapticsEnabled)
        assertFalse(s.musicEnabled)
        assertFalse(s.analyticsOptIn, "analyticsOptIn MUST default false (privacy opt-in)")
    }

    // ---------- TC-S1: round-trip equality ----------

    @Test
    fun serialization_round_trip_equals_original() {
        val original = AppSettings(
            themeMode = ThemeMode.AMOLED,
            midGameBoostEnabled = true,
            stylusMode = StylusMode.ALWAYS,
            stylusConfidenceThreshold = 0.82f,
            stylusDebounceMs = 350,
            colorBlindMode = ColorBlindMode.DEUTERANOPIA,
            hintDepth = 5
        )
        val encoded = json.encodeToString(AppSettings.serializer(), original)
        val decoded = json.decodeFromString(AppSettings.serializer(), encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun migrator_round_trip_equals_original() {
        val original = AppSettings(
            mistakeLimit = 5,
            themeMode = ThemeMode.DARK,
            stylusAutoDetected = true,
            tiltParallaxEnabled = true,
            analyticsOptIn = true
        )
        val encoded = AppSettingsMigrator.encode(original)
        val decoded = AppSettingsMigrator.migrate(encoded)
        assertEquals(original, decoded)
    }

    // ---------- schemaVersion is the first field ----------

    @Test
    fun schemaVersion_is_first_field_in_serialized_output() {
        val s = AppSettings(mistakeLimit = 4)
        val encoded = AppSettingsMigrator.encode(s)
        // The JSON must literally start with the schemaVersion key.
        assertTrue(
            encoded.startsWith("{\"schemaVersion\":"),
            "schemaVersion must be the first key in the serialized JSON; got: " +
                encoded.take(60)
        )
    }

    // ---------- TC-S2c: unknown future keys ignored ----------

    @Test
    fun ignoreUnknownKeys_allows_forward_compat() {
        val blob = """{
            "schemaVersion": 1,
            "unknownFutureField": "value",
            "anotherUnknown": 42,
            "mistakeLimit": 5
        }""".trimIndent()
        val out = AppSettingsMigrator.migrate(blob)
        assertEquals(5, out.mistakeLimit)
        assertEquals(1, out.schemaVersion)
    }

    // ---------- Migration tests (P0-2) ----------

    @Test
    fun migrate_v1_blob_returns_same() {
        val original = AppSettings(mistakeLimit = 5, themeMode = ThemeMode.DARK)
        val encoded = AppSettingsMigrator.encode(original)
        val decoded = AppSettingsMigrator.migrate(encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun migrate_missing_schemaVersion_uses_v0_path() {
        // No schemaVersion key → treated as v0 → migrated to v1 with the values present.
        val blob = """{"mistakeLimit": 5}"""
        val out = AppSettingsMigrator.migrate(blob)
        assertEquals(1, out.schemaVersion, "v0→v1 migration must stamp schemaVersion=1")
        assertEquals(5, out.mistakeLimit, "user values survive v0→v1 migration")
        // Other fields should default.
        assertFalse(out.midGameBoostEnabled)
        assertEquals(StylusMode.AUTO, out.stylusMode)
    }

    @Test
    fun migrate_unknown_future_version_falls_back_to_defaults() {
        // A future version we can't understand must NOT inherit the user's mistakeLimit.
        // The whole blob is discarded in favor of safe defaults — see TC-S6.
        val blob = """{"schemaVersion": 99, "mistakeLimit": 5, "midGameBoostEnabled": true}"""
        val out = AppSettingsMigrator.migrate(blob)
        assertEquals(AppSettings(), out, "future schemaVersion → full defaults, no partial trust")
        // Specifically, mistakeLimit must NOT be 5.
        assertNotEquals(5, out.mistakeLimit)
        assertFalse(out.midGameBoostEnabled, "future blob's flipped flag must NOT leak through")
    }

    @Test
    fun migrate_corrupt_json_returns_defaults_without_exception() {
        val out = AppSettingsMigrator.migrate("not json")
        assertEquals(AppSettings(), out)
    }

    @Test
    fun migrate_empty_blob_returns_defaults() {
        val out = AppSettingsMigrator.migrate("")
        assertEquals(AppSettings(), out)
    }

    @Test
    fun migrate_partial_blob_uses_defaults_for_missing_fields() {
        // 3 fields supplied, rest default.
        val blob = """{
            "schemaVersion": 1,
            "mistakeLimit": 5,
            "themeMode": "DARK",
            "hapticsEnabled": false
        }""".trimIndent()
        val out = AppSettingsMigrator.migrate(blob)
        assertEquals(5, out.mistakeLimit)
        assertEquals(ThemeMode.DARK, out.themeMode)
        assertFalse(out.hapticsEnabled)
        // Untouched fields default.
        assertEquals(3, out.hintDepth)
        assertEquals(StylusMode.AUTO, out.stylusMode)
        assertFalse(out.midGameBoostEnabled)
        assertEquals(300, out.stylusDebounceMs)
    }

    // ---------- TC-S2b: copy preserves immutability ----------

    @Test
    fun copy_partial_update_preserves_other_fields() {
        val a = AppSettings(themeMode = ThemeMode.DARK)
        val b = a.copy(midGameBoostEnabled = true)
        assertEquals(ThemeMode.DARK, b.themeMode)
        assertTrue(b.midGameBoostEnabled)
        assertFalse(a.midGameBoostEnabled, "original must be immutable")
    }

    // ---------- TC-S8: invalid enum string ----------

    @Test
    fun migrate_invalid_enum_string_falls_back_to_defaults() {
        // Whole-blob decode fails when the enum is invalid; we fall back to defaults
        // rather than trying to salvage individual fields. Documented behavior.
        val blob = """{"schemaVersion": 1, "themeMode": "ULTRAVIOLET"}"""
        val out = AppSettingsMigrator.migrate(blob)
        assertEquals(AppSettings(), out)
    }

    // ---------- TC-S2d: confidence threshold preserved as-is ----------

    @Test
    fun stylusConfidenceThreshold_preserved_through_round_trip() {
        val s = AppSettings(stylusConfidenceThreshold = 0.6f)
        val out = AppSettingsMigrator.migrate(AppSettingsMigrator.encode(s))
        assertEquals(0.6f, out.stylusConfidenceThreshold)
    }

    // ---------- TC-S9: top-level non-object input ----------

    @Test
    fun migrate_non_object_top_level_returns_defaults() {
        // Valid JSON but not an object.
        assertEquals(AppSettings(), AppSettingsMigrator.migrate("[1,2,3]"))
        assertEquals(AppSettings(), AppSettingsMigrator.migrate("42"))
        assertEquals(AppSettings(), AppSettingsMigrator.migrate("\"a string\""))
        assertEquals(AppSettings(), AppSettingsMigrator.migrate("null"))
    }

    // ---------- Encoding always emits all keys ----------

    @Test
    fun encode_emits_all_fields_for_downgrade_safety() {
        // encodeDefaults=true so a future version reading this blob sees every key
        // and can use them or ignore them based on its own schema.
        val encoded = AppSettingsMigrator.encode(AppSettings())
        listOf(
            "schemaVersion", "mistakeLimit", "autoNotesEnabled", "fastPencilEnabled",
            "numberFirstModeEnabled", "midGameBoostEnabled", "themeMode", "highlightEnabled",
            "showRemainingCount", "showTimer", "animatedDigits", "hintDepth", "solverSpeedMs",
            "solverHelpCells", "stylusMode", "stylusAutoDetected", "stylusConfidenceThreshold",
            "stylusPressureToBoldNotes", "stylusWristRejection", "stylusDebounceMs",
            "proximityAutoPauseEnabled", "ambientLightAutoThemeEnabled", "tiltParallaxEnabled",
            "reduceMotion", "highContrast", "colorBlindMode", "largeText",
            "soundEnabled", "hapticsEnabled", "musicEnabled", "analyticsOptIn"
        ).forEach { key ->
            assertTrue(encoded.contains("\"$key\""), "encoded blob must contain $key")
        }
    }

    // ---------- Migrator's defaultJson is the canonical config ----------

    @Test
    fun migrator_defaultJson_ignores_unknown_keys() {
        // Sanity: the shared Json must tolerate forward-compat unknown keys.
        val blob = """{"schemaVersion": 1, "futureField": "x", "mistakeLimit": 7}"""
        val out = AppSettingsMigrator.defaultJson.decodeFromString(AppSettings.serializer(), blob)
        assertEquals(7, out.mistakeLimit)
    }

    // ---------- Round-trip preserves enum identity, not just value ----------

    @Test
    fun all_enums_round_trip_through_migration() {
        ThemeMode.values().forEach { mode ->
            val src = AppSettings(themeMode = mode)
            val out = AppSettingsMigrator.migrate(AppSettingsMigrator.encode(src))
            assertEquals(mode, out.themeMode, "ThemeMode.$mode must round-trip")
        }
        StylusMode.values().forEach { mode ->
            val src = AppSettings(stylusMode = mode)
            val out = AppSettingsMigrator.migrate(AppSettingsMigrator.encode(src))
            assertEquals(mode, out.stylusMode, "StylusMode.$mode must round-trip")
        }
        ColorBlindMode.values().forEach { mode ->
            val src = AppSettings(colorBlindMode = mode)
            val out = AppSettingsMigrator.migrate(AppSettingsMigrator.encode(src))
            assertEquals(mode, out.colorBlindMode, "ColorBlindMode.$mode must round-trip")
        }
    }

    // ---------- Migrator handles whitespace and extra padding ----------

    @Test
    fun migrate_blob_with_whitespace_padding_decodes_correctly() {
        val blob = "\n\n   " + AppSettingsMigrator.encode(AppSettings(mistakeLimit = 5)) + "   \n"
        val out = AppSettingsMigrator.migrate(blob)
        assertEquals(5, out.mistakeLimit)
    }

    // ---------- Defaults sanity: CURRENT_SCHEMA_VERSION constant is honored ----------

    @Test
    fun migrate_current_schema_version_constant_matches_default_field() {
        assertEquals(AppSettings.CURRENT_SCHEMA_VERSION, AppSettings().schemaVersion)
        assertNotNull(AppSettings.CURRENT_SCHEMA_VERSION)
        assertTrue(AppSettings.CURRENT_SCHEMA_VERSION >= 1)
    }

    // ---------- Collapsed enum smoke tests (architecture audit v2) ----------
    //
    // These tests pin the contract for the `:domain`-owned enums after the v2
    // architecture audit collapsed the duplicate `androidApp/ui/theme/ThemeMode.kt`
    // and `androidApp/ui/theme/ColorBlindMode.kt` copies into this module. Any future
    // attempt to re-introduce a parallel enum should break a downstream import; these
    // tests catch the upstream invariants the call sites depend on.

    @Test
    fun themeMode_defaultsToSystem() {
        assertEquals(ThemeMode.SYSTEM, AppSettings().themeMode)
        // Verify the four-mode shape used by SudokuTheme's `when (themeMode)` block.
        assertEquals(4, ThemeMode.values().size)
        assertTrue(ThemeMode.values().toList().containsAll(
            listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.AMOLED),
        ))
    }

    @Test
    fun colorBlindMode_defaultsToNone() {
        assertEquals(ColorBlindMode.NONE, AppSettings().colorBlindMode)
        // Verify the three-mode shape used by `applyColorBlindOverlay`.
        assertEquals(3, ColorBlindMode.values().size)
    }

    @Test
    fun colorBlindMode_needsStaticPalette_trueForNonNone() {
        // Wired into `SudokuTheme` to gate Android 12+ Material You dynamic colour.
        // Replaces the old `androidApp/ui/theme/ColorBlindMode.needsStaticPalette()`
        // member function — same semantics, lives in :domain now.
        assertFalse(ColorBlindMode.NONE.needsStaticPalette())
        assertTrue(ColorBlindMode.DEUTERANOPIA.needsStaticPalette())
        assertTrue(ColorBlindMode.PROTANOPIA.needsStaticPalette())
    }
}
