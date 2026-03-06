import SwiftUI

struct SettingsView: View {
    let onBack: () -> Void
    @EnvironmentObject var settingsViewModel: SettingsViewModel
    @EnvironmentObject var statsViewModel: StatsViewModel
    @State private var showResetConfirm = false

    var body: some View {
        let settings = settingsViewModel.settings
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                Spacer().frame(height: 20)
                Text("Settings")
                    .font(.system(size: 36, weight: .bold))
                    .foregroundColor(Color(hex: "#2C3E50"))
                    .padding(.horizontal, 24)
                Text("Customize Your Experience")
                    .font(.system(size: 16))
                    .foregroundColor(.gray)
                    .padding(.horizontal, 24)
                    .padding(.bottom, 32)

                // Game Settings
                sectionHeader("Game Settings")
                toggleRow("Sound Effects", "Enable sound feedback",
                          settings.soundEnabled) { settingsViewModel.toggleSound() }
                toggleRow("Cell Highlighting", "Highlight related cells",
                          settings.highlightEnabled) { settingsViewModel.toggleHighlight() }
                toggleRow("Show Timer", "Display game timer",
                          settings.timerEnabled) { settingsViewModel.toggleTimer() }

                // Solver Settings
                sectionHeader("Solver Settings")
                settingRow(label: "Speed", description: "Delay between steps") {
                    HStack(spacing: 6) {
                        ForEach([(500,"0.5s"),(1500,"1.5s"),(3000,"3s")], id: \.0) { ms, label in
                            let active = settings.solverSpeedMs == ms
                            Button(label) { settingsViewModel.setSolverSpeed(ms) }
                                .font(.system(size: 13, weight: .semibold))
                                .padding(.horizontal, 10)
                                .frame(height: 36)
                                .foregroundColor(active ? .white : Color(hex: "#2C3E50"))
                                .background(active ? Color.blue : Color.white)
                                .cornerRadius(6)
                                .overlay(RoundedRectangle(cornerRadius: 6).stroke(Color.gray.opacity(0.3), lineWidth: 1))
                        }
                    }
                }
                settingRow(label: "Help Cells", description: "Cells: \(settings.solverHelpCells)") {
                    HStack(spacing: 8) {
                        Button("-") { settingsViewModel.setSolverHelpCells(settings.solverHelpCells - 1) }
                            .font(.system(size: 22, weight: .bold))
                            .foregroundColor(settings.solverHelpCells > 1 ? .blue : .gray)
                            .disabled(settings.solverHelpCells <= 1)
                        Text("\(settings.solverHelpCells)")
                            .font(.system(size: 18, weight: .bold))
                            .frame(minWidth: 24)
                        Button("+") { settingsViewModel.setSolverHelpCells(settings.solverHelpCells + 1) }
                            .font(.system(size: 22, weight: .bold))
                            .foregroundColor(settings.solverHelpCells < 9 ? .blue : .gray)
                            .disabled(settings.solverHelpCells >= 9)
                    }
                }

                // Data
                sectionHeader("Data")
                Button("Reset All Statistics") { showResetConfirm = true }
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(.red)
                    .frame(maxWidth: .infinity)
                    .frame(height: 52)
                    .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.red, lineWidth: 1.5))
                    .padding(.horizontal, 24)

                Spacer().frame(height: 24)
                Text("Sudoku — A Brain Gym  v1.0.0")
                    .font(.system(size: 13))
                    .foregroundColor(.gray)
                    .frame(maxWidth: .infinity, alignment: .center)
                Spacer().frame(height: 24)

                Button("Back to Home") { onBack() }
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 52)
                    .background(Color.blue)
                    .cornerRadius(12)
                    .padding(.horizontal, 24)
                Spacer().frame(height: 32)
            }
        }
        .background(Color(UIColor.systemGroupedBackground))
        .alert("Reset Statistics", isPresented: $showResetConfirm) {
            Button("Reset", role: .destructive) { statsViewModel.resetStats() }
            Button("Cancel", role: .cancel) {}
        } message: { Text("This cannot be undone.") }
    }

    @ViewBuilder
    private func sectionHeader(_ title: String) -> some View {
        Text(title)
            .font(.system(size: 20, weight: .semibold))
            .foregroundColor(Color(hex: "#2C3E50"))
            .padding(.horizontal, 24)
            .padding(.top, 20)
            .padding(.bottom, 10)
    }

    @ViewBuilder
    private func toggleRow(_ label: String, _ description: String,
                            _ isOn: Bool, action: @escaping () -> Void) -> some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text(label).font(.system(size: 16, weight: .semibold))
                    .foregroundColor(Color(hex: "#2C3E50"))
                Text(description).font(.system(size: 14)).foregroundColor(.gray)
            }
            Spacer()
            Toggle("", isOn: Binding(get: { isOn }, set: { _ in action() }))
                .labelsHidden()
                .tint(.blue)
        }
        .padding(16)
        .background(Color.white)
        .cornerRadius(12)
        .padding(.horizontal, 24)
        .padding(.bottom, 10)
    }

    @ViewBuilder
    private func settingRow<Content: View>(label: String, description: String,
                                           @ViewBuilder control: () -> Content) -> some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text(label).font(.system(size: 16, weight: .semibold))
                    .foregroundColor(Color(hex: "#2C3E50"))
                Text(description).font(.system(size: 14)).foregroundColor(.gray)
            }
            Spacer()
            control()
        }
        .padding(16)
        .background(Color.white)
        .cornerRadius(12)
        .padding(.horizontal, 24)
        .padding(.bottom, 10)
    }
}
