//
//  GameView.swift
//  Sudoku Brain Gym — Game screen
//
//  Layout (top→bottom):
//   1. HUD (glass capsule): difficulty · mistakes · timer · score
//   2. SudokuGridCanvas + PencilCellOverlay on iPad
//   3. NumberPadView
//   4. Bottom toolbar: [Undo] [Erase] [Notes] [Hint]
//
//  Mistake feedback parity (Lena P0-A11Y-002) handled in GameViewModel:
//   - Visual: red fill on mistake cell + dashed border (color-blind cue).
//   - Audio:  via `soundEnabled` (TODO Phase 2 sound files).
//   - Haptic: via `hapticsEnabled`.
//   - Spoken: UIAccessibility announcement — UNCONDITIONAL.
//
//  Tilt parallax suppression (Lena P1-A11Y-015):
//   - Disabled when Reduce Motion is on.
//   - Disabled when VoiceOver is running.
//   - Disabled when AppSettings.tiltParallaxEnabled is false.
//
//  References:
//  - ScenePhase:   https://developer.apple.com/documentation/swiftui/scenephase
//  - keyboard shortcuts (iPad): https://developer.apple.com/documentation/swiftui/view/keyboardshortcut(_:modifiers:)
//

import SwiftUI

struct GameView: View {

    @State private var vm = GameViewModel()
    @State private var sensors = SensorService.shared
    @Environment(AccessibilityFlags.self) private var a11y
    @Environment(AppSettingsStore.self) private var settingsStore
    @Environment(\.scenePhase) private var scenePhase

    var body: some View {
        VStack(spacing: 12) {
            HUDView(snapshot: vm.snapshot,
                    highContrast: settingsStore.settings.highContrast)

            grid
                .padding(.horizontal, 8)

            if let status = vm.recognizerStatus {
                Text(status)
                    .font(AppTypography.footnote)
                    .foregroundStyle(.secondary)
                    .accessibilityAddTraits(.isStaticText)
                    // Live region: announce recognizer state changes.
                    .accessibilityElement(children: .combine)
            }

            NumberPadView(
                remainingCounts: remainingCounts,
                onDigit: { d in Task { await vm.place(digit: d) } },
                onErase: { Task { await vm.clearSelectedCell() } }
            )
            .padding(.horizontal, 12)

            bottomToolbar
                .padding(.horizontal, 12)
                .padding(.bottom, 8)
        }
        .background(Color(.systemGroupedBackground))
        .onAppear { vm.onAppear() }
        .onDisappear { vm.onDisappear() }
        .onChange(of: scenePhase) { _, phase in
            // Aggressively stop sensors on background.
            if phase != .active { sensors.stopAll() }
            else { vm.onAppear() }
        }
        // Hardware keyboard shortcuts (Maggie P1-13 + iPad reviewer):
        //   1..9 → place digit  · Backspace → erase  · U → undo
        //   H → hint            · N → toggle notes mode
        .modifier(GameKeyboardShortcuts(vm: vm))
    }

    // MARK: - Grid

    private var grid: some View {
        ZStack {
            SudokuGridCanvas(
                snapshot: vm.snapshot,
                highContrast: settingsStore.settings.highContrast,
                colorBlindMode: settingsStore.settings.colorBlindMode,
                verboseAnnouncements:
                    settingsStore.settings.verboseAnnouncements,
                onSelect: { coord in vm.select(cell: coord) }
            )
            // Tilt parallax — gated on (setting + Reduce Motion + VoiceOver).
            .offset(parallaxOffset)
            .animation(.easeOut(duration: 0.15), value: sensors.roll)

            // PencilKit overlay (iPad only)
            if UIDevice.current.userInterfaceIdiom == .pad,
               settingsStore.settings.stylusMode != .never
            {
                PencilCellOverlay(
                    stylusMode: settingsStore.settings.stylusMode,
                    stylusAutoDetected:
                        settingsStore.settings.stylusAutoDetected,
                    endOfStrokeMs:
                        settingsStore.settings.stylusEndOfStrokeMs,
                    onStrokeCommit: { _ in
                        Task { await vm.commitPendingDrawing() }
                    },
                    pendingDrawing: $vm.pendingDrawing
                )
                .allowsHitTesting(vm.snapshot.selectedCell != nil)
            }
        }
    }

    private var parallaxOffset: CGSize {
        guard settingsStore.settings.tiltParallaxEnabled,
              !a11y.reduceMotion,
              !a11y.voiceOver else { return .zero }
        let max: CGFloat = 8
        let dx = CGFloat(sensors.roll).clamped(-max / 4, max / 4) * 4
        let dy = CGFloat(sensors.pitch).clamped(-max / 4, max / 4) * 4
        return CGSize(width: dx, height: dy)
    }

    // MARK: - Toolbar

    private var bottomToolbar: some View {
        HStack(spacing: 16) {
            toolbarButton(icon: "arrow.uturn.backward",
                          label: "Undo",
                          action: { Task { await vm.undo() } })
            toolbarButton(icon: "delete.left",
                          label: "Erase",
                          action: { Task { await vm.clearSelectedCell() } })
            // Pencil-mode parity button (WCAG 2.5.1 + Lena P0-4):
            // the squeeze/double-tap gesture MUST have a visible button
            // equivalent. This is it.
            toolbarButton(icon: vm.notesMode
                                 ? "pencil.circle.fill"
                                 : "pencil.circle",
                          label: "Notes",
                          isOn: vm.notesMode,
                          action: { vm.notesMode.toggle() })
            toolbarButton(icon: "lightbulb",
                          label: "Hint",
                          action: { Task { await vm.requestHint() } })
        }
        .adaptiveGlass(.regular,
                       in: RoundedRectangle(cornerRadius: 28, style: .continuous),
                       flags: a11y,
                       highContrast: settingsStore.settings.highContrast)
    }

    private func toolbarButton(icon: String,
                               label: String,
                               isOn: Bool = false,
                               action: @escaping () -> Void) -> some View
    {
        Button(action: action) {
            VStack(spacing: 2) {
                Image(systemName: icon)
                    .imageScale(.large)
                Text(label)
                    .font(.caption)
            }
            .frame(maxWidth: .infinity, minHeight: 44)
            .padding(.vertical, 6)
        }
        .accessibilityLabel(label)
        .accessibilityValue(isOn ? "on" : "off")
    }

    // MARK: - Remaining counts (for number pad)

    private var remainingCounts: [Int: Int] {
        var counts: [Int: Int] = [:]
        for digit in 1...9 {
            let placed = vm.snapshot.cells.filter { $0.value == digit }.count
            counts[digit] = max(0, 9 - placed)
        }
        return counts
    }
}

// MARK: - Hardware keyboard shortcuts

private struct GameKeyboardShortcuts: ViewModifier {
    let vm: GameViewModel

    func body(content: Content) -> some View {
        // Each `keyboardShortcut` registers an invisible button that captures
        // the keystroke. Visible buttons are hidden via `.opacity(0)` but
        // remain accessible to the keyboard.
        content
            .background(
                Group {
                    ForEach(1...9, id: \.self) { d in
                        Button("") { Task { await vm.place(digit: d) } }
                            .keyboardShortcut(KeyEquivalent(
                                Character("\(d)")), modifiers: [])
                            .opacity(0)
                            .accessibilityHidden(true)
                    }
                    Button("") { Task { await vm.clearSelectedCell() } }
                        .keyboardShortcut(.delete, modifiers: [])
                        .opacity(0)
                        .accessibilityHidden(true)
                    Button("") { Task { await vm.undo() } }
                        .keyboardShortcut("z", modifiers: [.command])
                        .opacity(0)
                        .accessibilityHidden(true)
                    Button("") { Task { await vm.requestHint() } }
                        .keyboardShortcut("h", modifiers: [])
                        .opacity(0)
                        .accessibilityHidden(true)
                    Button("") { vm.notesMode.toggle() }
                        .keyboardShortcut("n", modifiers: [])
                        .opacity(0)
                        .accessibilityHidden(true)
                }
            )
    }
}

// MARK: - Small numeric helper

private extension CGFloat {
    func clamped(_ low: CGFloat, _ high: CGFloat) -> CGFloat {
        Swift.min(Swift.max(self, low), high)
    }
}
