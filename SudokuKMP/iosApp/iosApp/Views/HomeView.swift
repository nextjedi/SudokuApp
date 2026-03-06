import SwiftUI

struct HomeView: View {
    let onStartGame: () -> Void
    let onOpenSettings: () -> Void
    let onOpenStats: () -> Void

    @EnvironmentObject var gameViewModel: GameViewModel
    @EnvironmentObject var statsViewModel: StatsViewModel

    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                Spacer().frame(height: 48)

                // Title
                VStack(spacing: 4) {
                    Text("Sudoku")
                        .font(.system(size: 48, weight: .bold))
                        .foregroundColor(Color("Navy"))
                    Text("A Brain Gym")
                        .font(.system(size: 22, weight: .medium))
                        .foregroundColor(Color.blue)
                    Text("Train your mind, one puzzle at a time")
                        .font(.system(size: 15))
                        .foregroundColor(.gray)
                        .padding(.top, 4)
                }

                // Streak badge
                if statsViewModel.stats.currentStreak > 0 {
                    HStack(spacing: 8) {
                        Text("🔥")
                            .font(.system(size: 20))
                        Text("\(statsViewModel.stats.currentStreak) day streak!")
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundColor(Color.orange)
                    }
                    .padding(.horizontal, 20)
                    .padding(.vertical, 8)
                    .background(Color.orange.opacity(0.1))
                    .cornerRadius(20)
                    .padding(.top, 16)
                }

                Spacer().frame(height: 36)

                // Difficulty selector
                VStack(spacing: 12) {
                    Text("Select Difficulty")
                        .font(.system(size: 20, weight: .semibold))
                        .frame(maxWidth: .infinity, alignment: .center)
                        .padding(.bottom, 4)

                    ForEach(DifficultyUI.allCases) { diff in
                        let isSelected = gameViewModel.state.difficulty == diff
                        HStack {
                            Text(diff.rawValue)
                                .font(.system(size: 20, weight: .semibold))
                                .foregroundColor(isSelected ? .blue : Color("Navy"))
                            Spacer()
                            Text("\(diff.clues) clues")
                                .font(.system(size: 14))
                                .foregroundColor(.gray)
                        }
                        .padding(.horizontal, 24)
                        .padding(.vertical, 18)
                        .background(isSelected ? Color.blue.opacity(0.08) : Color.white)
                        .overlay(
                            RoundedRectangle(cornerRadius: 12)
                                .stroke(isSelected ? Color.blue : Color.gray.opacity(0.3), lineWidth: 2)
                        )
                        .cornerRadius(12)
                        .onTapGesture {
                            gameViewModel.state.difficulty = diff
                        }
                    }
                }
                .padding(.horizontal, 24)

                Spacer().frame(height: 32)

                // Start button
                Button(action: {
                    gameViewModel.startNewGame(difficulty: gameViewModel.state.difficulty)
                    onStartGame()
                }) {
                    Text("New Game")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .frame(height: 56)
                        .background(Color.blue)
                        .cornerRadius(12)
                }
                .padding(.horizontal, 24)

                HStack(spacing: 12) {
                    Button(action: {
                        statsViewModel.refresh()
                        onOpenStats()
                    }) {
                        Text("Statistics")
                            .font(.system(size: 15, weight: .medium))
                            .frame(maxWidth: .infinity)
                            .frame(height: 48)
                            .overlay(RoundedRectangle(cornerRadius: 10).stroke(Color.blue, lineWidth: 1.5))
                    }
                    Button(action: onOpenSettings) {
                        Text("Settings")
                            .font(.system(size: 15, weight: .medium))
                            .frame(maxWidth: .infinity)
                            .frame(height: 48)
                            .overlay(RoundedRectangle(cornerRadius: 10).stroke(Color.blue, lineWidth: 1.5))
                    }
                }
                .padding(.horizontal, 24)
                .padding(.top, 12)

                Spacer().frame(height: 48)
            }
        }
        .background(Color(UIColor.systemGroupedBackground))
    }
}
