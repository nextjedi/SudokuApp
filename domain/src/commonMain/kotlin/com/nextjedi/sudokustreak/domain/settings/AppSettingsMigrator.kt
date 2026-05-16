package com.nextjedi.sudokustreak.domain.settings

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * Decodes and encodes [AppSettings] blobs and migrates older schema versions
 * to the current [AppSettings.CURRENT_SCHEMA_VERSION].
 *
 * ## Envelope format
 *
 * The blob is just the [AppSettings] JSON — the `schemaVersion` lives **inside** the
 * object as its first field. This avoids wrapping every persisted value in a
 * `{"v": N, "data": {…}}` envelope while still giving us a clean version handshake.
 *
 * ## Behavior matrix
 *
 * | Input                                  | Result                                                            |
 * |----------------------------------------|-------------------------------------------------------------------|
 * | Empty string / null                    | [AppSettings] defaults                                            |
 * | Corrupt JSON                           | [AppSettings] defaults + log warning                              |
 * | `schemaVersion == 1`                   | Decode as v1 (current)                                            |
 * | `schemaVersion == 0` or missing        | Treat as v0 → call [migrateV0ToV1] (currently identity)           |
 * | `schemaVersion > 1` (future)           | [AppSettings] defaults + log warning ("rolled back from future")  |
 * | Partial blob (missing keys)            | Missing keys filled from defaults via `encodeDefaults = true`     |
 * | Unknown future keys                    | Silently ignored via `ignoreUnknownKeys = true`                   |
 *
 * ## Why not crash on future versions?
 *
 * Forward-rollback support is more important than fidelity here. A user who downgrades
 * (or installs an older APK over a newer one) should still get a working app with
 * default settings rather than a crash loop.
 */
object AppSettingsMigrator {

    /**
     * Shared [Json] configuration. Kept tolerant so platform stores can pass any blob:
     * - `ignoreUnknownKeys = true` — forward-compatibility for future fields.
     * - `encodeDefaults = true` — every field appears in the output (including
     *   `schemaVersion`) so downgrades can read it.
     * - `prettyPrint = false` — DataStore / UserDefaults / localStorage don't need pretty.
     */
    val defaultJson: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
        isLenient = false
    }

    /**
     * Migrate a JSON [blob] to a [AppSettings] at [AppSettings.CURRENT_SCHEMA_VERSION].
     *
     * - On any parse failure, returns [AppSettings] defaults (no exception escapes).
     * - On a known older version, runs the appropriate `migrateVNToVN1` chain.
     * - On an unknown future version, returns [AppSettings] defaults.
     */
    fun migrate(blob: String, json: Json = defaultJson): AppSettings {
        if (blob.isBlank()) {
            logWarn("AppSettingsMigrator.migrate: empty blob → defaults")
            return AppSettings()
        }
        // 1. Parse permissively into a JsonObject so we can inspect schemaVersion before
        //    committing to a strongly-typed decode.
        val root: JsonObject = try {
            val element = json.parseToJsonElement(blob)
            element as? JsonObject ?: run {
                logWarn("AppSettingsMigrator.migrate: top-level is not an object → defaults")
                return AppSettings()
            }
        } catch (e: SerializationException) {
            logWarn("AppSettingsMigrator.migrate: parse failed (${e.message}) → defaults")
            return AppSettings()
        } catch (e: IllegalArgumentException) {
            logWarn("AppSettingsMigrator.migrate: malformed JSON (${e.message}) → defaults")
            return AppSettings()
        }

        // 2. Dispatch on schemaVersion. Treat missing/null as v0.
        val versionElement = root["schemaVersion"]
        val version: Int = (versionElement as? JsonPrimitive)?.intOrNull ?: 0

        return when {
            version == AppSettings.CURRENT_SCHEMA_VERSION -> decodeV1(root, json)
            version == 0 -> migrateV0ToV1(root, json)
            version < 0 -> {
                logWarn("AppSettingsMigrator.migrate: negative schemaVersion=$version → defaults")
                AppSettings()
            }
            version > AppSettings.CURRENT_SCHEMA_VERSION -> {
                logWarn(
                    "AppSettingsMigrator.migrate: future schemaVersion=$version " +
                        "(this build supports up to ${AppSettings.CURRENT_SCHEMA_VERSION}) → defaults"
                )
                AppSettings()
            }
            else -> {
                logWarn("AppSettingsMigrator.migrate: unhandled schemaVersion=$version → defaults")
                AppSettings()
            }
        }
    }

    /**
     * Encode [settings] to a JSON string suitable for persistence.
     *
     * The resulting string starts with `{"schemaVersion":N,…` because the data class
     * declares `schemaVersion` as the first constructor parameter. The
     * `schemaVersion_is_first_field` unit test pins that invariant.
     */
    fun encode(settings: AppSettings, json: Json = defaultJson): String {
        return json.encodeToString(AppSettings.serializer(), settings)
    }

    /**
     * Migrate a pre-v1 blob (or any blob without a `schemaVersion` field).
     *
     * Currently a no-op delegation to v1 decoding because v0 has the same schema as v1
     * minus the `schemaVersion` field itself. Concretely:
     * - Missing fields get default values via `encodeDefaults`/`isLenient` interplay.
     * - Unknown fields are dropped.
     *
     * **When v2 lands**, add a `migrateV1ToV2(v1: JsonObject): AppSettings` stub here
     * and chain through it.
     */
    private fun migrateV0ToV1(v0: JsonObject, json: Json): AppSettings {
        // v0 → v1: schemaVersion is the only addition. Decoding v1 with the v0 payload
        // succeeds because `schemaVersion` has a default of 1.
        return decodeV1(v0, json)
    }

    /**
     * Strongly-typed decode at the current schema. Wrapped to absorb partial blobs
     * that fail decoding (e.g., a field has an invalid enum string).
     */
    private fun decodeV1(root: JsonObject, json: Json): AppSettings {
        return try {
            json.decodeFromJsonElement(AppSettings.serializer(), root)
        } catch (e: SerializationException) {
            logWarn("AppSettingsMigrator.decodeV1: invalid v1 blob (${e.message}) → defaults")
            AppSettings()
        } catch (e: IllegalArgumentException) {
            logWarn("AppSettingsMigrator.decodeV1: malformed v1 blob (${e.message}) → defaults")
            AppSettings()
        }
    }

    /**
     * Lightweight log hook. Production builds wire this to platform logging (Logcat
     * on Android, OSLog on iOS). For now it prints to stderr so unit-test runs surface
     * migration regressions without pulling a logging dependency into `:domain`.
     */
    private fun logWarn(message: String) {
        // Intentional println to stderr — replaceable by a logger injection later.
        // Tests assert behavior, not log output, so this is safe to leave verbose.
        @Suppress("ForbiddenComment")
        // TODO: inject a platform logger in Wave 2 once :domain has a Logger interface.
        println("[WARN] $message")
    }
}
