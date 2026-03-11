import Foundation

enum Difficulty: String, CaseIterable, Identifiable {
    case easy = "Easy"
    case medium = "Medium"
    case hard = "Hard"

    var id: String { rawValue }

    var clues: Int {
        switch self {
        case .easy: return 46
        case .medium: return 36
        case .hard: return 26
        }
    }

    var cellsToRemove: Int {
        switch self {
        case .easy: return 35
        case .medium: return 45
        case .hard: return 55
        }
    }
}
