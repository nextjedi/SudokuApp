//
//  AppSettingsStore.swift
//  Sudoku Brain Gym — Settings persistence
//
//  Persists the entire `AppSettings` struct as one JSON blob in
//  `UserDefaults`, mirroring the Android `DataStore<AppSettings>` design.
//
//  Why one blob, not key-per-field:
//   - Atomic upgrade path (schemaVersion can be bumped without
//     coordinating 30 individual key migrations).
//   - Matches KMP's typed `AppSettings` data class semantics.
//   - Camila (Privacy reviewer): "Delete All My Data" is a single
//     `defaults removeObject(forKey:)` call.
//
//  PrivacyManifest note: writing to UserDefaults triggers the required-
//  reason API category `NSPrivacyAccessedAPICategoryUserDefaults`. We
//  declare reason `CA92.1` ("Access info from same app") in
//  PrivacyInfo.xcprivacy.
//
//  References:
//  - UserDefaults:          https://developer.apple.com/documentation/foundation/userdefaults
//  - Privacy Manifest:      https://developer.apple.com/documentation/bundleresources/privacy-manifest-files
//  - TN3183 required reason API: https://developer.apple.com/documentation/technotes/tn3183
//  - @Observable:           https://developer.apple.com/documentation/observation
//

import Foundation
import Observation

@MainActor
@Observable
final class AppSettingsStore {

    // MARK: - Singleton-ish access for convenience.
    //
    // We don't make this a true singleton because tests can construct a
    // separate `AppSettingsStore(suiteName: "test-XYZ")` instance.
    static let shared = AppSettingsStore()

    // MARK: - Published state

    /// The single source of truth in memory. Re-renders trigger via @Observable.
    private(set) var settings: AppSettings

    /// Set on every successful load attempt. `nil` until first load completes.
    private(set) var lastLoadError: Error?

    // MARK: - Private

    private let userDefaults: UserDefaults
    private let storageKey: String = "AppSettings.v1"      // bumped with schemaVersion
    private let encoder: JSONEncoder
    private let decoder: JSONDecoder

    // MARK: - Init

    /// `suiteName` allows tests to use an isolated UserDefaults; production
    /// passes `nil` to use `UserDefaults.standard`.
    init(suiteName: String? = nil) {
        self.userDefaults = UserDefaults(suiteName: suiteName) ?? .standard
        self.encoder = JSONEncoder()
        self.encoder.outputFormatting = [.sortedKeys]
        self.decoder = JSONDecoder()
        // Load now so the first SwiftUI evaluation has the user's preferences.
        self.settings = Self.loadOrDefault(
            from: userDefaults, key: storageKey, decoder: decoder
        )
    }

    // MARK: - Public API

    /// Apply a mutation atomically. Use the closure form so we can later
    /// throttle disk writes if needed.
    func update(_ block: (inout AppSettings) -> Void) {
        var copy = settings
        block(&copy)
        settings = copy
        persist()
    }

    /// Camila P0-13 "Delete All My Data" hook. Resets settings AND clears
    /// other user-data UserDefaults keys owned by the app.
    func deleteAllUserData() {
        // Reset to defaults
        settings = AppSettings()
        userDefaults.removeObject(forKey: storageKey)
        // The stats key co-tenancy issue (PC-4) is handled here by enumerating
        // the few app-owned keys we know about and removing them. We do NOT
        // call `removePersistentDomain(forName:)` because that would clobber
        // OS-managed entries.
        for key in Self.appOwnedKeys {
            userDefaults.removeObject(forKey: key)
        }
    }

    /// Keys the app owns. Listed explicitly so we never sweep system keys.
    /// Bump this list when a new persisted key is introduced.
    static let appOwnedKeys: [String] = [
        "AppSettings.v1",
        "AppStats.v1",           // mirror of KMP AppStats (separate file)
        "SavedGame.v1",
        "DailyChallenge.v1"
    ]

    // MARK: - Persistence

    private func persist() {
        do {
            let data = try encoder.encode(settings)
            userDefaults.set(data, forKey: storageKey)
        } catch {
            lastLoadError = error
        }
    }

    private static func loadOrDefault(
        from defaults: UserDefaults,
        key: String,
        decoder: JSONDecoder
    ) -> AppSettings {
        guard let data = defaults.data(forKey: key) else {
            return AppSettings()
        }
        do {
            let raw = try decoder.decode(AppSettings.self, from: data)
            return migrate(raw)
        } catch {
            // Decoding failed (most likely a schema bump made the blob
            // unreadable). Don't crash — just fall back to defaults so the
            // user's app still works.
            return AppSettings()
        }
    }

    // MARK: - Migration
    //
    // Add cases here when KMP bumps `AppSettings.schemaVersion`. The migration
    // contract: each version-step is a pure transform that takes the previous
    // shape and returns the next shape with sane defaults for any new fields.
    private static func migrate(_ raw: AppSettings) -> AppSettings {
        var s = raw
        switch s.schemaVersion {
        case 1:
            // Current — no migration needed.
            break
        case ..<1:
            // Older than v1 — coerce to v1 defaults but preserve recognizable
            // fields.
            s.schemaVersion = 1
        default:
            // Newer schema we don't understand (downgrade scenario). Keep the
            // fields we can read; rely on Codable defaults to fill anything new.
            s.schemaVersion = 1
        }
        return s
    }
}
