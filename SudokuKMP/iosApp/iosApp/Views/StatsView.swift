import SwiftUI

struct StatsView: View {
    let onBack: () -> Void
    @EnvironmentObject var statsViewModel: StatsViewModel

    var body: some View {
        let stats = statsViewModel.stats
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                Spacer().frame(height: 20)
                Text("Statistics")
                    .font(.system(size: 36, weight: .bold))
                    .foregroundColor(Color(hex: "#2C3E50"))
                    .padding(.horizontal, 24)
                Text("Your Sudoku Journey")
                    .font(.system(size: 16))
                    .foregroundColor(.gray)
                    .padding(.horizontal, 24)
                    .padding(.bottom, 32)

                // Stats grid
                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                    StatCardView(systemImage: "flame.fill", imageColor: .orange,
                                 value: "\(stats.currentStreak)", label: "Streak", subtext: "days")
                    StatCardView(systemImage: "gamecontroller.fill", imageColor: .blue,
                                 value: "\(stats.gamesPlayed)", label: "Played", subtext: "total")
                    StatCardView(systemImage: "trophy.fill", imageColor: .yellow,
                                 value: "\(stats.gamesWon)", label: "Won", subtext: "games")
                    StatCardView(systemImage: "chart.bar.fill", imageColor: .green,
                                 value: "\(stats.winRate)%", label: "Win Rate", subtext: "success")
                }
                .padding(.horizontal, 24)

                Spacer().frame(height: 28)

                HStack(spacing: 6) {
                    Image(systemName: "timer")
                        .foregroundColor(Color(hex: "#2C3E50"))
                    Text("Best Times")
                        .font(.system(size: 22, weight: .semibold))
                        .foregroundColor(Color(hex: "#2C3E50"))
                }
                .padding(.horizontal, 24)
                .padding(.bottom, 12)

                ForEach([("Easy", stats.bestEasySec),
                         ("Medium", stats.bestMediumSec),
                         ("Hard", stats.bestHardSec)], id: \.0) { label, sec in
                    HStack {
                        Text(label)
                            .font(.system(size: 18, weight: .semibold))
                            .foregroundColor(Color(hex: "#2C3E50"))
                        Spacer()
                        Text(sec == 0 ? "N/A" : formatTime(sec))
                            .font(.system(size: 20, weight: .bold, design: .monospaced))
                            .foregroundColor(.blue)
                    }
                    .padding(16)
                    .background(Color.white)
                    .cornerRadius(12)
                    .padding(.horizontal, 24)
                    .padding(.bottom, 6)
                }

                if stats.averageTimeSec > 0 {
                    VStack(spacing: 6) {
                        Text("Average Completion Time")
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundColor(Color(hex: "#2C3E50"))
                        Text(formatTime(stats.averageTimeSec))
                            .font(.system(size: 28, weight: .bold, design: .monospaced))
                            .foregroundColor(.blue)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(20)
                    .background(Color.blue.opacity(0.08))
                    .cornerRadius(12)
                    .padding(.horizontal, 24)
                    .padding(.top, 8)
                }

                Spacer().frame(height: 32)

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
    }
}

struct StatCardView: View {
    let systemImage: String
    let imageColor: Color
    let value: String
    let label: String
    let subtext: String

    var body: some View {
        VStack(spacing: 4) {
            Image(systemName: systemImage)
                .font(.system(size: 24))
                .foregroundColor(imageColor)
            Text(value)
                .font(.system(size: 32, weight: .bold))
                .foregroundColor(.blue)
            Text(label)
                .font(.system(size: 14, weight: .semibold))
                .foregroundColor(Color(hex: "#2C3E50"))
            Text(subtext)
                .font(.system(size: 12))
                .foregroundColor(.gray)
        }
        .frame(maxWidth: .infinity)
        .padding(16)
        .background(Color.white)
        .cornerRadius(12)
    }
}
