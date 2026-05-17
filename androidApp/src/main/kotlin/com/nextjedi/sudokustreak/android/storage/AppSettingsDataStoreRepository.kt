package com.nextjedi.sudokustreak.android.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.nextjedi.sudokustreak.domain.settings.AppSettings
import com.nextjedi.sudokustreak.domain.settings.AppSettingsMigrator
import com.nextjedi.sudokustreak.domain.settings.AppSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.io.InputStream
import java.io.OutputStream

/**
 * Production Android implementation of [AppSettingsRepository].
 *
 * Backed by a **typed** `DataStore<AppSettings>` (NOT `DataStore<Preferences>`) so the
 * on-disk representation matches the cross-platform JSON envelope and the schema
 * version handshake travels with the data. Stats remain in the legacy
 * `DataStore<Preferences>` per locked decision §6 (3).
 *
 * ## Wave-1 architect note (resolved here)
 *
 * The pre-existing `composeApp/.../AndroidAppSettings.kt` was an in-memory `mutableMapOf`
 * that silently swallowed writes — the Architect's C7 flag in test-plan/00-SYNTHESIS.md.
 * This repository is the *correct* sink for Wave-2 typed settings. The composeApp KV
 * facade is a separate concern (it bridges a different KV interface used by webApp /
 * iosApp paths) and is left untouched in this commit so we don't conflict with the
 * compose-app agent's pending work.
 *
 * ## Serializer behaviour
 *
 * - **Read:** delegates to [AppSettingsMigrator.migrate] so old / corrupt / future blobs
 *   fall back to [AppSettings] defaults instead of crashing.
 * - **Write:** delegates to [AppSettingsMigrator.encode] so `schemaVersion` is always the
 *   first key in the persisted JSON (pinned by the
 *   `schemaVersion_is_first_field_in_serialized_output` unit test in :domain).
 *
 * ## File layout
 *
 * Stored at `${filesDir}/datastore/app_settings.json`. Lives in the standard DataStore
 * directory so platform backup rules apply automatically. The filename is a constant
 * documented in [SETTINGS_FILE_NAME] so the "Delete All My Data" tests can wipe it from
 * Robolectric's emulated filesystem if they want a deterministic cold-start.
 *
 * @param context preferably the application context. We capture only `applicationContext`
 *   internally so the repository can outlive the originating Activity safely.
 */
class AppSettingsDataStoreRepository(
    context: Context,
) : AppSettingsRepository {

    /**
     * Captured once at construction. Using `applicationContext` is critical here — the
     * `dataStore` property delegate caches a singleton keyed by the receiver, and we
     * never want an Activity reference to leak into that cache.
     */
    private val appContext: Context = context.applicationContext

    /**
     * The actual `DataStore<AppSettings>` instance. Lazily produced by the `dataStore`
     * delegate the first time we read [appContext]'s extension property.
     */
    private val store: DataStore<AppSettings>
        get() = appContext.appSettingsDataStore

    // ---- AppSettingsRepository contract ----

    override val flow: Flow<AppSettings>
        get() = store.data

    override suspend fun current(): AppSettings = flow.first()

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        // DataStore.updateData is atomic + queued single-writer. It re-invokes the
        // transform under contention, which matches the AppSettingsRepository contract
        // ("the transform MUST be pure — it may be invoked more than once").
        store.updateData { current -> transform(current) }
    }

    override suspend fun reset() {
        // Don't call `store.edit { }` — there's no such API for typed DataStore.
        // Write defaults explicitly so the Migrator round-trips a known-good blob
        // (and so the schemaVersion key is re-anchored to the current version).
        store.updateData { AppSettings() }
    }

    companion object {
        /**
         * File name under `${filesDir}/datastore/`. Exposed for "Delete All My Data"
         * smoke tests + manual support flows that wipe the on-disk blob.
         */
        const val SETTINGS_FILE_NAME: String = "app_settings.json"
    }
}

/**
 * Module-private serializer. Lives at top-level (not inside the repository class) so
 * the `dataStore` property delegate below can reference it without leaking the repository
 * instance into the delegate's singleton cache.
 *
 * - `defaultValue` is the pristine `AppSettings()` — what a fresh install or a
 *   corrupted-file recovery returns.
 * - `readFrom` decodes via [AppSettingsMigrator.migrate], which absorbs every parse +
 *   schema-version failure mode (empty blob, corrupt JSON, future version, partial keys).
 * - `writeTo` encodes via [AppSettingsMigrator.encode], which guarantees `schemaVersion`
 *   appears first in the serialized output.
 *
 * **Thread safety:** the serializer is stateless aside from delegating to the Migrator,
 * which is itself stateless (`object`).
 */
private object AppSettingsJsonSerializer : Serializer<AppSettings> {
    override val defaultValue: AppSettings = AppSettings()

    override suspend fun readFrom(input: InputStream): AppSettings {
        // readBytes() is safe here because settings blobs are tiny (<8 KB). If the file
        // is missing or empty, the Migrator returns defaults.
        val text = input.readBytes().decodeToString()
        return AppSettingsMigrator.migrate(text)
    }

    override suspend fun writeTo(t: AppSettings, output: OutputStream) {
        val encoded = AppSettingsMigrator.encode(t)
        output.write(encoded.encodeToByteArray())
    }
}

/**
 * `Context.appSettingsDataStore` — the canonical typed DataStore instance for the app.
 *
 * Using the `dataStore` property delegate (NOT a manual `DataStoreFactory.create()`
 * call) buys us:
 * - **Singleton-per-context guarantee** — preventing the "two DataStores at the same
 *   file" crash that hits manual creation if you forget to memoise.
 * - **Implicit corruption handler** — DataStore falls back to defaults rather than
 *   throwing on a bad blob (paired with our Migrator's `migrate()` returning defaults).
 */
private val Context.appSettingsDataStore: DataStore<AppSettings> by dataStore(
    fileName = AppSettingsDataStoreRepository.SETTINGS_FILE_NAME,
    serializer = AppSettingsJsonSerializer,
)
