//
//  SettingsView.swift
//  Sudoku Brain Gym — Settings screen
//
//  Form(.grouped) per Apple HIG. Native Toggle/Picker/Slider — NO custom
//  glass on Section backgrounds (iOS reviewer P1-7: Apple's own Settings.app
//  does not glass sections; let Form ship native).
//
//  Accessibility (Lena):
//   - .searchable for fast row finding (P1-18).
//   - Section headers are headings automatically via `Section("…")`.
//   - Toggle rows use native Toggle so Role.Switch is set automatically.
//   - Sliders are replaced with 3-stop Pickers where "confidence" UX is
//     user-hostile (iOS reviewer §14).
//   - Disabled rows (e.g., Tilt parallax when Reduce Motion is on) include
//     a footer explaining WHY (Lena P1-A11Y-009).
//
//  Data & Privacy section (Camila P0-13):
//   - Analytics opt-in toggle (default OFF).
//   - "Replay tutorial" row (Maggie P1-19).
//   - "Reset stylus calibration" row.
//   - "Delete All My Data" destructive button with confirmation.
//
//  References:
//  - Form & .formStyle: https://developer.apple.com/documentation/swiftui/form
//  - .searchable:       https://developer.apple.com/documentation/swiftui/view/searchable(text:placement:prompt:)
//

import SwiftUI

struct SettingsView: View {

    @State private var vm = SettingsViewModel()
    @Environment(AppSettingsStore.self) private var store
    @Environment(AccessibilityFlags.self) private var a11y

    var body: some View {
        NavigationStack {
            Form {
                gameplaySection
                stylusSection
                sensorsSection
                presentationSection
                solverSection
                accessibilitySection
                audioHapticsSection
                dataPrivacySection
                aboutSection
            }
            .formStyle(.grouped)
            .navigationTitle("Settings")
            .searchable(text: $vm.searchQuery)
            .scrollDismissesKeyboard(.interactively)
            .sheet(isPresented: $vm.showStylusTest) {
                StylusTestSheet()
            }
            .confirmationDialog(
                "Delete All My Data?",
                isPresented: $vm.showDeleteAllConfirm,
                titleVisibility: .visible
            ) {
                Button("Delete Everything",
                       role: .destructive,
                       action: vm.deleteAllMyData)
                Button("Cancel", role: .cancel) { }
            } message: {
                Text("This removes saved games, stats, settings, and any " +
                     "calibration data. The app returns to first-launch state.")
            }
        }
    }

    // MARK: - Sections

    private var gameplaySection: some View {
        Section("Gameplay") {
            Picker("Mistake limit", selection: Binding(
                get: { store.settings.mistakeLimit },
                set: { vm.setMistakeLimit($0) }
            )) {
                Text("1").tag(MistakeLimit.one)
                Text("3").tag(MistakeLimit.three)
                Text("5").tag(MistakeLimit.five)
                Text("Unlimited").tag(MistakeLimit.unlimited)
            }
            Toggle("Auto-check mistakes",
                   isOn: bindBool(\.autoCheckMistakes))
            Toggle("Highlight peers (row, column, box)",
                   isOn: bindBool(\.highlightPeers))
            Toggle("Highlight same value",
                   isOn: bindBool(\.highlightSameValue))
            Toggle("Auto-erase candidates",
                   isOn: bindBool(\.autoEraseCandidates))
            Toggle("Show timer",
                   isOn: bindBool(\.showTimer))
        }
    }

    private var stylusSection: some View {
        Section {
            Picker("Use Pencil", selection: Binding(
                get: { store.settings.stylusMode },
                set: { vm.setStylusMode($0) }
            )) {
                Text("Auto").tag(StylusMode.auto)
                Text("Always").tag(StylusMode.always)
                Text("Never").tag(StylusMode.never)
            }
            .pickerStyle(.segmented)
            .accessibilityHint(
                "Auto: pencil and finger both work. " +
                "Always: only pencil draws digits. " +
                "Never: pencil disabled.")

            Picker("Recognition confidence", selection: Binding(
                get: { store.settings.stylusConfidence },
                set: { vm.setConfidence($0) }
            )) {
                Text("Low").tag(ConfidenceTier.low)
                Text("Medium").tag(ConfidenceTier.medium)
                Text("High").tag(ConfidenceTier.high)
            }
            .pickerStyle(.segmented)

            Toggle("Bold notes from pressure",
                   isOn: bindBool(\.stylusPressureBoldNotes))
            Toggle("Wrist rejection",
                   isOn: bindBool(\.stylusWristRejection))

            Button("Test Pencil…") { vm.showStylusTest = true }
                .accessibilityHint("Opens a drawing sheet to test recognition.")
        } header: {
            Text("Apple Pencil")
        } footer: {
            if UIDevice.current.userInterfaceIdiom == .phone {
                Text("Apple Pencil is not supported on iPhone. " +
                     "Use the number pad to enter digits.")
            } else if !store.settings.stylusAutoDetected {
                Text("No Pencil detected yet. Once you write with one, " +
                     "additional pencil-only features unlock here.")
            }
        }
        .disabled(UIDevice.current.userInterfaceIdiom == .phone)
    }

    private var sensorsSection: some View {
        let isProxAvailable = SensorService.shared.isProximityAvailable
        return Section {
            Toggle("Proximity auto-pause",
                   isOn: Binding(
                    get: { store.settings.proximityAutoPauseEnabled },
                    set: { vm.setProximityAutoPause($0) }))
                .disabled(!isProxAvailable)
                .accessibilityHint(isProxAvailable
                    ? "Pauses the game when you cover the earpiece sensor."
                    : "Not available on this device — no proximity sensor.")

            Toggle("Tilt parallax",
                   isOn: Binding(
                    get: { store.settings.tiltParallaxEnabled },
                    set: { vm.setTiltParallax($0) }))
                .disabled(a11y.reduceMotion)
                .accessibilityHint(a11y.reduceMotion
                    ? "Disabled — Reduce Motion is on. " +
                      "Turn off Reduce Motion in Accessibility to enable."
                    : "Adds a subtle depth effect to pencil notes.")
        } header: {
            Text("Sensors")
        } footer: {
            if !isProxAvailable {
                Text("Proximity auto-pause requires an iPhone with an " +
                     "earpiece sensor — not available on iPad.")
            }
        }
    }

    private var presentationSection: some View {
        Section("Presentation") {
            Picker("Theme", selection: Binding(
                get: { store.settings.themeMode },
                set: { vm.setTheme($0) }
            )) {
                Text("System").tag(ThemeMode.system)
                Text("Light").tag(ThemeMode.light)
                Text("Dark").tag(ThemeMode.dark)
                Text("AMOLED").tag(ThemeMode.amoled)
            }
            Picker("Color-blind mode", selection: Binding(
                get: { store.settings.colorBlindMode },
                set: { vm.setColorBlindMode($0) }
            )) {
                Text("None").tag(AppColorBlindMode.none)
                Text("Deuteranopia").tag(AppColorBlindMode.deuteranopia)
                Text("Protanopia").tag(AppColorBlindMode.protanopia)
                Text("Tritanopia").tag(AppColorBlindMode.tritanopia)
            }
        }
    }

    private var solverSection: some View {
        Section("Solver & Hints") {
            Picker("Hint depth", selection: Binding(
                get: { store.settings.hintDepth },
                set: { d in store.update { $0.hintDepth = d } }
            )) {
                Text("Minimal").tag(HintDepth.minimal)
                Text("Basic").tag(HintDepth.basic)
                Text("Medium").tag(HintDepth.medium)
                Text("Deep").tag(HintDepth.deep)
                Text("Full").tag(HintDepth.full)
            }
            Toggle("Explain techniques",
                   isOn: bindBool(\.explainTechniques))
        }
    }

    private var accessibilitySection: some View {
        Section("Accessibility") {
            Toggle("High contrast",
                   isOn: Binding(
                    get: { store.settings.highContrast },
                    set: { vm.setHighContrast($0) }))
                .accessibilityHint(
                    "Disables glass effects, thickens borders, and forces " +
                    "AAA contrast.")
            Toggle("Reduce motion (in-app override)",
                   isOn: Binding(
                    get: { store.settings.reduceMotion },
                    set: { vm.setReduceMotion($0) }))
                .accessibilityHint(
                    "Disables animations independent of the system flag.")
            Toggle("Dynamic Type for grid digits",
                   isOn: bindBool(\.dynamicTypeDigits))
            Toggle("Verbose VoiceOver announcements",
                   isOn: bindBool(\.verboseAnnouncements))
            Toggle("Keyboard shortcuts (iPad / Mac)",
                   isOn: bindBool(\.keyboardShortcuts))
        }
    }

    private var audioHapticsSection: some View {
        Section("Audio & Haptics") {
            Toggle("Sound effects",
                   isOn: Binding(
                    get: { store.settings.soundEnabled },
                    set: { vm.setSoundEnabled($0) }))
            Toggle("Haptics",
                   isOn: Binding(
                    get: { store.settings.hapticsEnabled },
                    set: { vm.setHapticsEnabled($0) }))
            Toggle("Background music",
                   isOn: bindBool(\.musicEnabled))
        }
    }

    private var dataPrivacySection: some View {
        Section {
            Toggle("Share anonymous analytics",
                   isOn: Binding(
                    get: { store.settings.analyticsOptIn },
                    set: { vm.setAnalyticsOptIn($0) }))
                .accessibilityHint(
                    "Off by default. Sends anonymized usage events to help " +
                    "improve the app. No personal data, no advertising.")

            Toggle("Send crash reports",
                   isOn: bindBool(\.crashReportingOptIn))

            Button("Replay tutorial") { vm.replayTutorial() }
            Button("Reset stylus calibration") { vm.resetStylusCalibration() }

            Button("Delete All My Data", role: .destructive) {
                vm.showDeleteAllConfirm = true
            }
        } header: {
            Text("Data & Privacy")
        } footer: {
            Text("All puzzle solving, recognition, and sensors run " +
                 "on-device. Nothing leaves your device unless you " +
                 "opt in to analytics above.")
        }
    }

    private var aboutSection: some View {
        Section("About") {
            HStack {
                Text("Version")
                Spacer()
                Text(Bundle.main
                        .object(forInfoDictionaryKey:
                                "CFBundleShortVersionString")
                        as? String ?? "1.0")
                    .foregroundStyle(.secondary)
            }
            Link("Privacy Policy",
                 destination: URL(string:
                    "https://example.com/sudoku/privacy")!)
            Link("Help & Contact",
                 destination: URL(string:
                    "mailto:hello@example.com?subject=Sudoku%20Help")!)
        }
    }

    // MARK: - Binding helper

    private func bindBool(_ keyPath: WritableKeyPath<AppSettings, Bool>)
        -> Binding<Bool>
    {
        Binding(
            get: { store.settings[keyPath: keyPath] },
            set: { newValue in
                store.update { $0[keyPath: keyPath] = newValue }
            }
        )
    }
}

// MARK: - Stylus test sheet

private struct StylusTestSheet: View {
    @Environment(\.dismiss) private var dismiss
    var body: some View {
        NavigationStack {
            VStack {
                Text("Pencil Test")
                    .font(.largeTitle).bold()
                    .accessibilityAddTraits(.isHeader)
                Text("Write a digit 1–9 below.")
                    .foregroundStyle(.secondary)
                // A real implementation would embed PencilCellOverlay here
                // and surface the recognized digit + confidence + latency.
                // Hidden on iPhone since Pencil doesn't pair with iPhone.
                if UIDevice.current.userInterfaceIdiom == .pad {
                    Rectangle()
                        .stroke(Color.secondary, lineWidth: 1)
                        .frame(height: 320)
                        .padding()
                        .accessibilityLabel("Drawing area")
                        .accessibilityHint(
                            "Write a digit with Apple Pencil to test " +
                            "the recognizer.")
                } else {
                    Text("Apple Pencil is not supported on iPhone.")
                        .foregroundStyle(.secondary)
                        .padding()
                }
                Spacer()
            }
            .padding()
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") { dismiss() }
                }
            }
        }
    }
}
