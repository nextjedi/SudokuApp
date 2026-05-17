//
//  KMPEngineAdapter.swift
//  Sudoku Brain Gym — bridge to the shared.xcframework KMP engine
//
//  This is the SINGLE funnel into the KMP engine. Every Swift caller must
//  go through this adapter. Direct imports of `shared` types in views or
//  view-models are forbidden (per architecture review C6 + iOS reviewer P0).
//
//  ============================================================
//  CRITICAL: @MainActor at TYPE level — Kotlin/Native rule
//  ============================================================
//
//  Kotlin/Native enforces strict thread confinement. Even with the new memory
//  model (default since Kotlin 1.7.20), Kotlin Flow collected from Swift
//  delivers on whatever dispatcher the Kotlin side declared, not necessarily
//  Main. Hopping at every leaf is error-prone, so we isolate the adapter at
//  the type level.
//
//  Forbidden:
//      Task.detached { adapter.findNextStep() }   // crashes with
//                                                 // IncorrectDereferenceException
//
//  Required:
//      Task { @MainActor in adapter.findNextStep() }
//
//  Reference:
//   - Kotlin/Native concurrency: https://kotlinlang.org/docs/native-memory-manager.html
//   - @MainActor:                https://developer.apple.com/documentation/swift/mainactor
//   - Apple async-await:         https://developer.apple.com/documentation/swift/asynchronous-functions
//   - WWDC21 "Meet async/await": https://developer.apple.com/videos/play/wwdc2021/10132/
//

import Foundation
import Observation

// NOTE: we deliberately do NOT `import shared` in this scaffold so the
// file compiles on machines without the XCFramework built yet. Once the
// XCFramework wiring (P0-4 in 00-SYNTHESIS.md) lands, swap the typealiases
// below to point at the real KMP types, and remove the local stubs in
// `#if !KMP_AVAILABLE`.

// MARK: - Public domain types (Swift value-types crossing the boundary)

/// 9×9 cell coordinate, 0-indexed.
struct CellCoord: Hashable, Codable, Sendable {
    let row: Int
    let col: Int

    var box: Int { (row / 3) * 3 + (col / 3) }

    static let zero = CellCoord(row: 0, col: 0)
}

/// Snapshot of a cell at one moment. Value-type, safe to send to off-main.
struct CellState: Equatable, Sendable {
    let value: Int              // 0 = empty
    let isGiven: Bool           // locked clue
    let candidates: [Int]       // pencil notes
    let isError: Bool           // currently in mistake state (UI flag)
}

/// Snapshot of the full board + meta. Mirrors KMP `BoardSnapshot`.
struct BoardSnapshot: Equatable, Sendable {
    let cells: [CellState]      // 81 entries, row-major
    let mistakes: Int
    let mistakeLimit: Int
    let score: Int
    let elapsedSeconds: Int
    let difficulty: Difficulty
    let phase: GamePhase
    let selectedCell: CellCoord?

    static let empty = BoardSnapshot(
        cells: Array(repeating:
            CellState(value: 0, isGiven: false, candidates: [], isError: false),
            count: 81),
        mistakes: 0,
        mistakeLimit: 3,
        score: 0,
        elapsedSeconds: 0,
        difficulty: .easy,
        phase: .idle,
        selectedCell: nil
    )

    func cell(at coord: CellCoord) -> CellState {
        cells[coord.row * 9 + coord.col]
    }
}

enum Difficulty: String, Codable, CaseIterable, Sendable {
    case easy, medium, hard, expert, master

    var label: String { rawValue.capitalized }
}

enum GamePhase: String, Codable, Sendable {
    case idle
    case playing
    case paused
    case won
    case lost
}

/// Result of a `place` call.
struct MoveResult: Sendable {
    let snapshot: BoardSnapshot
    let kind: Kind

    enum Kind: Sendable {
        case accepted
        case mistake(MistakeInfo)
        case ignored                // e.g., placed on a "given" cell
    }
}

struct MistakeInfo: Sendable {
    let coord: CellCoord
    let attemptedValue: Int
    let mistakeIndex: Int           // 1-based count
    let limit: Int
}

/// Result of a `hint` call.
struct HintResult: Sendable {
    let coord: CellCoord?
    let value: Int?
    let techniqueName: String?
    let explanation: String?
    let remainingHints: Int
}

// MARK: - The adapter

@MainActor
final class KMPEngineAdapter {

    // MARK: Singleton — shared engine per process.
    //
    // Engine state is long-lived (saved game across launches). One shared
    // instance is the simplest model. Tests can construct a fresh instance.
    static let shared = KMPEngineAdapter()

    // The engine is a Kotlin object behind the XCFramework. We store it as
    // `Any?` here until the framework is wired so this file compiles.
    private var engineHandle: Any?

    // Flow cancellables — opaque tokens we'd get back from the KMP side.
    private var subscriptions: [() -> Void] = []

    init() {
        // Lazy engine creation. Calling `start()` initializes the engine
        // on first use rather than at app launch — keeps cold-start fast.
    }

    deinit {
        // Cancel any leftover Kotlin subscriptions. We cannot directly
        // access MainActor in deinit safely on iOS 17, so dispatch async.
        let subs = subscriptions
        Task { @MainActor in
            subs.forEach { $0() }
        }
    }

    // MARK: Lifecycle

    /// Lazily construct the engine. Idempotent.
    func start() {
        guard engineHandle == nil else { return }
        // TODO(P0-4): replace with
        //     engineHandle = Shared_engineGameEngineCompanion.shared.create()
        // once the XCFramework is wired. For now we use a placeholder so the
        // VMs have something to call into.
        engineHandle = PlaceholderEngine()
    }

    func cancelAll() {
        subscriptions.forEach { $0() }
        subscriptions.removeAll()
    }

    // MARK: Commands (writes)

    /// Place a digit at the selected cell. Suspending — runs the heavy KMP
    /// work on `Dispatchers.Default` and resolves back to the main actor.
    func place(at coord: CellCoord, digit: Int) async -> MoveResult {
        start()
        // TODO(P0-4): replace with real KMP call:
        //   let k = await (engineHandle as! Shared_engineGameEngine)
        //                     .place(row: Int32(coord.row), col: Int32(coord.col),
        //                            digit: Int32(digit))
        //   return MoveResult(k)
        return await (engineHandle as? PlaceholderEngine)?
            .place(at: coord, digit: digit)
            ?? MoveResult(snapshot: .empty, kind: .ignored)
    }

    func toggleCandidate(at coord: CellCoord, digit: Int) async -> BoardSnapshot {
        start()
        return await (engineHandle as? PlaceholderEngine)?
            .toggleCandidate(at: coord, digit: digit)
            ?? .empty
    }

    func clearCell(at coord: CellCoord) async -> BoardSnapshot {
        start()
        return await (engineHandle as? PlaceholderEngine)?
            .clearCell(at: coord) ?? .empty
    }

    func undo() async -> BoardSnapshot {
        start()
        return await (engineHandle as? PlaceholderEngine)?.undo() ?? .empty
    }

    func newGame(difficulty: Difficulty) async -> BoardSnapshot {
        start()
        return await (engineHandle as? PlaceholderEngine)?
            .newGame(difficulty: difficulty) ?? .empty
    }

    func pause() async { await (engineHandle as? PlaceholderEngine)?.pause() }
    func resume() async { await (engineHandle as? PlaceholderEngine)?.resume() }

    // MARK: Reads (observations)

    /// Current snapshot, synchronous fast-path. Main-actor only.
    var currentSnapshot: BoardSnapshot {
        (engineHandle as? PlaceholderEngine)?.snapshot ?? .empty
    }

    /// Hint request. KMP returns a typed explanation.
    func hint() async -> HintResult {
        start()
        return await (engineHandle as? PlaceholderEngine)?.hint()
            ?? HintResult(coord: nil, value: nil,
                          techniqueName: nil, explanation: nil,
                          remainingHints: 0)
    }

    /// Subscribe to a board-change stream. The handler is delivered on
    /// @MainActor (we hop via `Task { @MainActor in ... }` to enforce).
    func observeBoard(_ handler: @escaping @MainActor (BoardSnapshot) -> Void)
        -> Cancellable
    {
        start()
        // TODO(P0-4): replace with:
        //   let token = engine.boardFlow.subscribe(scope: MainScope()) { snap in
        //       Task { @MainActor in handler(BoardSnapshot(snap)) }
        //   }
        //   subscriptions.append { token.cancel() }
        let id = UUID()
        let placeholder = engineHandle as? PlaceholderEngine
        placeholder?.subscribers[id] = handler
        let cancel: () -> Void = { [weak placeholder] in
            placeholder?.subscribers[id] = nil
        }
        subscriptions.append(cancel)
        return Cancellable(cancel: cancel)
    }
}

// MARK: - Cancellable token

struct Cancellable {
    let cancel: () -> Void
}

// MARK: - Placeholder engine (DELETE once shared.xcframework is wired)
//
// This exists so the scaffold compiles in the absence of the KMP framework.
// All real logic lives in the shared module. Methods return sane defaults so
// view models can render without crashing during a smoke-test build.
@MainActor
private final class PlaceholderEngine {
    var snapshot: BoardSnapshot = .empty
    var subscribers: [UUID: (BoardSnapshot) -> Void] = [:]

    func place(at coord: CellCoord, digit: Int) async -> MoveResult {
        // Apply naïvely and notify subscribers.
        var cells = snapshot.cells
        let idx = coord.row * 9 + coord.col
        let existing = cells[idx]
        cells[idx] = CellState(
            value: digit,
            isGiven: existing.isGiven,
            candidates: [],
            isError: false
        )
        let next = BoardSnapshot(
            cells: cells,
            mistakes: snapshot.mistakes,
            mistakeLimit: snapshot.mistakeLimit,
            score: snapshot.score,
            elapsedSeconds: snapshot.elapsedSeconds,
            difficulty: snapshot.difficulty,
            phase: .playing,
            selectedCell: coord
        )
        snapshot = next
        broadcast(next)
        return MoveResult(snapshot: next, kind: .accepted)
    }

    func toggleCandidate(at coord: CellCoord, digit: Int) async -> BoardSnapshot {
        snapshot
    }
    func clearCell(at coord: CellCoord) async -> BoardSnapshot { snapshot }
    func undo() async -> BoardSnapshot { snapshot }

    func newGame(difficulty: Difficulty) async -> BoardSnapshot {
        snapshot = BoardSnapshot(
            cells: snapshot.cells,
            mistakes: 0,
            mistakeLimit: snapshot.mistakeLimit,
            score: 0,
            elapsedSeconds: 0,
            difficulty: difficulty,
            phase: .playing,
            selectedCell: nil
        )
        broadcast(snapshot)
        return snapshot
    }

    func pause() async {
        snapshot = BoardSnapshot(
            cells: snapshot.cells, mistakes: snapshot.mistakes,
            mistakeLimit: snapshot.mistakeLimit, score: snapshot.score,
            elapsedSeconds: snapshot.elapsedSeconds,
            difficulty: snapshot.difficulty, phase: .paused,
            selectedCell: snapshot.selectedCell
        )
        broadcast(snapshot)
    }
    func resume() async {
        snapshot = BoardSnapshot(
            cells: snapshot.cells, mistakes: snapshot.mistakes,
            mistakeLimit: snapshot.mistakeLimit, score: snapshot.score,
            elapsedSeconds: snapshot.elapsedSeconds,
            difficulty: snapshot.difficulty, phase: .playing,
            selectedCell: snapshot.selectedCell
        )
        broadcast(snapshot)
    }

    func hint() async -> HintResult {
        HintResult(coord: nil, value: nil, techniqueName: nil,
                   explanation: nil, remainingHints: 3)
    }

    private func broadcast(_ snap: BoardSnapshot) {
        for h in subscribers.values { h(snap) }
    }
}
