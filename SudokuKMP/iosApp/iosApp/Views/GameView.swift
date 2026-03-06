import SwiftUI

struct GameView: View {
    let onExit: () -> Void
    @EnvironmentObject var gameViewModel: GameViewModel
    @EnvironmentObject var settingsViewModel: SettingsViewModel
    @State private var showExitConfirm = false
    @State private var showNewGameConfirm = false

    var body: some View {
        let state = gameViewModel.state
        let settings = settingsViewModel.settings

        ScrollView {
            VStack(spacing: 0) {
                // Header
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(state.difficulty.rawValue)
                            .font(.system(size: 22, weight: .bold))
                            .foregroundColor(Color(hex: "#2C3E50"))
                        Text("❌ \(state.mistakes)/\(state.maxMistakes)")
                            .font(.system(size: 15))
                            .foregroundColor(.red)
                    }
                    Spacer()
                    if settings.timerEnabled {
                        Text(formatTime(state.elapsedSeconds))
                            .font(.system(size: 24, weight: .bold, design: .monospaced))
                            .foregroundColor(Color(hex: "#2C3E50"))
                    }
                }
                .padding(.horizontal, 24)
                .padding(.vertical, 16)

                // Grid
                SudokuGridView(
                    grid: state.grid,
                    selectedCell: state.selectedCell,
                    highlightEnabled: settings.highlightEnabled,
                    solverFillingCell: state.solverFillingCell,
                    onCellTap: { r, c in gameViewModel.selectCell(row: r, col: c) }
                )
                .padding(8)

                Spacer().frame(height: 16)

                // Number pad
                NumberPadView(
                    onNumberTap: { gameViewModel.enterNumber($0) },
                    onErase: { gameViewModel.eraseCell() },
                    disabled: state.isCompleted || state.isGameOver
                )

                Spacer().frame(height: 16)

                // Solver reasoning panel
                if !state.solverSteps.isEmpty || state.isSolverActive {
                    VStack(alignment: .leading, spacing: 4) {
                        if !state.currentSolverReasoning.isEmpty {
                            Text(state.currentSolverReasoning)
                                .font(.system(size: 13))
                                .foregroundColor(Color(hex: "#856404"))
                        }
                        ForEach(state.solverSteps.suffix(3).indices, id: \.self) { i in
                            Text(state.solverSteps.suffix(3)[i].reasoning)
                                .font(.system(size: 12))
                                .foregroundColor(.gray)
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(12)
                    .background(Color.yellow.opacity(0.2))
                    .overlay(RoundedRectangle(cornerRadius: 10).stroke(Color.yellow.opacity(0.5), lineWidth: 1))
                    .cornerRadius(10)
                    .padding(.horizontal, 16)
                }

                Spacer().frame(height: 12)

                // Solver controls
                VStack(spacing: 8) {
                    HStack {
                        Text("Speed:").font(.system(size: 14)).foregroundColor(.gray)
                        Spacer()
                        HStack(spacing: 6) {
                            ForEach([(500,"0.5s"),(1500,"1.5s"),(3000,"3s")], id: \.0) { ms, label in
                                let active = settings.solverSpeedMs == ms
                                Button(label) { settingsViewModel.setSolverSpeed(ms) }
                                    .font(.system(size: 12, weight: .semibold))
                                    .padding(.horizontal, 10)
                                    .frame(height: 34)
                                    .foregroundColor(active ? .white : Color(hex: "#2C3E50"))
                                    .background(active ? Color.blue : Color.white)
                                    .cornerRadius(6)
                                    .overlay(RoundedRectangle(cornerRadius: 6).stroke(Color.gray.opacity(0.3), lineWidth: 1))
                            }
                        }
                    }
                    HStack {
                        Text("Cells:").font(.system(size: 14)).foregroundColor(.gray)
                        Spacer()
                        HStack(spacing: 6) {
                            ForEach([1,3,5,9], id: \.self) { n in
                                let active = settings.solverHelpCells == n
                                Button("\(n)") { settingsViewModel.setSolverHelpCells(n) }
                                    .font(.system(size: 12, weight: .semibold))
                                    .padding(.horizontal, 10)
                                    .frame(height: 34)
                                    .foregroundColor(active ? .white : Color(hex: "#2C3E50"))
                                    .background(active ? Color.blue : Color.white)
                                    .cornerRadius(6)
                                    .overlay(RoundedRectangle(cornerRadius: 6).stroke(Color.gray.opacity(0.3), lineWidth: 1))
                            }
                        }
                    }
                }
                .padding(.horizontal, 16)

                Spacer().frame(height: 16)

                // Action buttons
                HStack(spacing: 10) {
                    Button(state.isSolverActive ? "Stop" : "Hint") {
                        gameViewModel.startSolver(
                            stepDelayMs: settings.solverSpeedMs,
                            maxCells: settings.solverHelpCells
                        )
                    }
                    .buttonStyle(ActionButtonStyle(color: state.isSolverActive ? .red : .blue))
                    .disabled(state.isCompleted || state.isGameOver)

                    Button("New Game") { showNewGameConfirm = true }
                        .buttonStyle(ActionButtonStyle(color: .clear, outlined: true))

                    Button("Exit") { showExitConfirm = true }
                        .buttonStyle(ActionButtonStyle(color: .clear, outlined: true, textColor: .red))
                }
                .padding(.horizontal, 16)

                Spacer().frame(height: 24)
            }
        }
        .background(Color(UIColor.systemGroupedBackground))
        .alert("New Game?", isPresented: $showNewGameConfirm) {
            Button("Start New", role: .destructive) { gameViewModel.startNewGame() }
            Button("Cancel", role: .cancel) {}
        } message: { Text("Current progress will be lost.") }
        .alert("Exit Game?", isPresented: $showExitConfirm) {
            Button("Exit", role: .destructive, action: onExit)
            Button("Cancel", role: .cancel) {}
        } message: { Text("Current progress will be lost.") }
        .alert("Congratulations! 🎉", isPresented: .init(
            get: { state.showCompletionDialog },
            set: { _ in gameViewModel.dismissCompletionDialog() }
        )) {
            Button("Home") { gameViewModel.dismissCompletionDialog(); onExit() }
            Button("New Game") { gameViewModel.dismissCompletionDialog(); gameViewModel.startNewGame() }
        } message: { Text("Puzzle completed in \(formatTime(state.elapsedSeconds))!") }
        .alert("Game Over", isPresented: .init(
            get: { state.isGameOver },
            set: { _ in }
        )) {
            Button("Try Again") { gameViewModel.startNewGame() }
            Button("Home") { onExit() }
        } message: { Text("You made \(state.maxMistakes) mistakes.") }
    }
}

struct ActionButtonStyle: ButtonStyle {
    var color: Color
    var outlined: Bool = false
    var textColor: Color = .primary

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.system(size: 15, weight: .semibold))
            .foregroundColor(outlined ? textColor : .white)
            .frame(maxWidth: .infinity)
            .frame(height: 48)
            .background(outlined ? Color.clear : color)
            .overlay(outlined ? RoundedRectangle(cornerRadius: 10).stroke(textColor == .red ? Color.red : Color.blue.opacity(0.5), lineWidth: 1.5) : nil)
            .cornerRadius(10)
            .opacity(configuration.isPressed ? 0.8 : 1.0)
    }
}

func formatTime(_ seconds: Int) -> String {
    String(format: "%02d:%02d", seconds / 60, seconds % 60)
}
