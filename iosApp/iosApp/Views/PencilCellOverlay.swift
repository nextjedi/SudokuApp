//
//  PencilCellOverlay.swift
//  Sudoku Brain Gym — UIViewRepresentable wrapping PKCanvasView
//
//  Edge cases handled (iOS reviewer §6):
//   - iPhone: Apple Pencil does NOT pair with iPhone (as of iOS 26). The
//     overlay returns `EmptyView` on iPhone. iPad: full PKCanvasView.
//   - drawingPolicy is computed from `AppSettings.stylusMode` AND whether
//     a Pencil has been observed paired (`stylusAutoDetected`).
//   - Stroke-end is detected via a 400 ms inactivity timer (configurable
//     via `AppSettings.stylusEndOfStrokeMs`) — see Stylus reviewer §5,
//     the right debounce for multi-stroke 4/5/7 with crossbar.
//
//  References:
//  - PKCanvasView:                  https://developer.apple.com/documentation/pencilkit/pkcanvasview
//  - PKCanvasViewDelegate:          https://developer.apple.com/documentation/pencilkit/pkcanvasviewdelegate
//  - PKDrawing:                     https://developer.apple.com/documentation/pencilkit/pkdrawing
//  - PKCanvasViewDrawingPolicy:     https://developer.apple.com/documentation/pencilkit/pkcanvasviewdrawingpolicy
//  - UIViewRepresentable:           https://developer.apple.com/documentation/swiftui/uiviewrepresentable
//

import SwiftUI
import PencilKit

struct PencilCellOverlay: View {

    let stylusMode: StylusMode
    let stylusAutoDetected: Bool
    let endOfStrokeMs: Int

    /// Called after the inactivity timer expires post stroke-end.
    /// `PKDrawing` is the cumulative drawing in the cell so far.
    let onStrokeCommit: (PKDrawing) -> Void

    /// Mirrors changes in `pendingDrawing` so the cell can re-render on
    /// each stroke. Two-way binding because the canvas owns the actual
    /// PKDrawing object.
    @Binding var pendingDrawing: PKDrawing

    var body: some View {
        // Apple Pencil does not pair with iPhone — overlay is iPad-only.
        if UIDevice.current.userInterfaceIdiom == .pad {
            PencilCanvasRepresentable(
                stylusMode: stylusMode,
                stylusAutoDetected: stylusAutoDetected,
                endOfStrokeMs: endOfStrokeMs,
                drawing: $pendingDrawing,
                onStrokeCommit: onStrokeCommit
            )
            .accessibilityLabel("Pencil drawing area")
            .accessibilityHint("Write a digit with Apple Pencil. " +
                               "Or tap a number button below.")
        } else {
            EmptyView()
        }
    }
}

// MARK: - UIKit bridge

private struct PencilCanvasRepresentable: UIViewRepresentable {

    let stylusMode: StylusMode
    let stylusAutoDetected: Bool
    let endOfStrokeMs: Int
    @Binding var drawing: PKDrawing
    let onStrokeCommit: (PKDrawing) -> Void

    func makeUIView(context: Context) -> PKCanvasView {
        let canvas = PKCanvasView()
        canvas.drawingPolicy = Self.resolvePolicy(
            mode: stylusMode, hasPaired: stylusAutoDetected)
        canvas.delegate = context.coordinator
        canvas.backgroundColor = .clear
        canvas.isOpaque = false
        canvas.tool = PKInkingTool(.pen, color: .label, width: 3)
        return canvas
    }

    func updateUIView(_ canvas: PKCanvasView, context: Context) {
        canvas.drawing = drawing
        canvas.drawingPolicy = Self.resolvePolicy(
            mode: stylusMode, hasPaired: stylusAutoDetected)
        context.coordinator.debounceMs = endOfStrokeMs
        context.coordinator.onStrokeCommit = onStrokeCommit
    }

    func makeCoordinator() -> Coordinator {
        Coordinator(debounceMs: endOfStrokeMs,
                    onStrokeCommit: onStrokeCommit,
                    onDrawingChanged: { drawing = $0 })
    }

    static func resolvePolicy(mode: StylusMode, hasPaired: Bool)
        -> PKCanvasViewDrawingPolicy
    {
        switch mode {
        case .never:    return .pencilOnly       // overlay is typically hidden
        case .always:   return .pencilOnly
        case .auto:     return hasPaired ? .pencilOnly : .anyInput
        }
    }

    final class Coordinator: NSObject, PKCanvasViewDelegate {
        var debounceMs: Int
        var onStrokeCommit: (PKDrawing) -> Void
        let onDrawingChanged: (PKDrawing) -> Void

        private var debounceWorkItem: DispatchWorkItem?

        init(debounceMs: Int,
             onStrokeCommit: @escaping (PKDrawing) -> Void,
             onDrawingChanged: @escaping (PKDrawing) -> Void) {
            self.debounceMs = debounceMs
            self.onStrokeCommit = onStrokeCommit
            self.onDrawingChanged = onDrawingChanged
        }

        func canvasViewDrawingDidChange(_ canvasView: PKCanvasView) {
            // Fires on EVERY change — including in-progress strokes.
            // We debounce: only fire `onStrokeCommit` after no changes for
            // `debounceMs` (default 400 ms — see AppSettings).
            onDrawingChanged(canvasView.drawing)
            debounceWorkItem?.cancel()
            let work = DispatchWorkItem { [weak canvasView, weak self] in
                guard let cv = canvasView, let self else { return }
                self.onStrokeCommit(cv.drawing)
            }
            debounceWorkItem = work
            DispatchQueue.main.asyncAfter(
                deadline: .now() + .milliseconds(debounceMs), execute: work)
        }
    }
}
