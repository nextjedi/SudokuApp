//
//  StylusInputService.swift
//  Sudoku Brain Gym — PencilKit + Core ML MNIST primary + Vision .fast fallback
//
//  Recognizer architecture (per 06-stylus-sensors-privacy-review.md §2 + §17):
//
//   Primary:  Core ML MNIST classifier (28×28 grayscale, bundled `.mlmodel`)
//             ~5 ms on A15 Neural Engine.
//
//   Fallback: Vision `VNRecognizeTextRequest` with `recognitionLevel = .fast`
//             AND `usesLanguageCorrection = false`, invoked ONLY when Core ML
//             confidence < settings.stylusConfidence AND stroke area exceeds
//             a minimum (rules out scribbles where Vision also fails).
//
//             Note: Apple's documented behavior is that `.accurate` is
//             BETTER for words but WORSE for isolated characters; `.fast`
//             is the right recognitionLevel for single digits.
//             (Apple Dev Forums 657934)
//
//   Final:    Return `.unrecognized`.
//
//  Why we mirror the order in code:
//   - Latency: MNIST runs <5 ms vs Vision OCR's 80–200 ms.
//   - Accuracy: MNIST is purpose-built for the digit task; Vision OCR is
//     trained on printed text and underperforms on isolated handwritten chars.
//   - Privacy: both pipelines are 100% on-device.
//
//  Threading contract (per §6 of the review):
//   - Recognition runs on a detached, user-initiated Task.
//   - A new `recognize(...)` call cancels any in-flight recognition.
//
//  References:
//  - PencilKit:                    https://developer.apple.com/documentation/pencilkit
//  - PKCanvasView.drawingPolicy:   https://developer.apple.com/documentation/pencilkit/pkcanvasviewdrawingpolicy
//  - Core ML:                      https://developer.apple.com/documentation/coreml
//  - VNCoreMLRequest:              https://developer.apple.com/documentation/vision/vncoremlrequest
//  - VNRecognizeTextRequest:       https://developer.apple.com/documentation/vision/vnrecognizetextrequest
//  - VNRequestTextRecognitionLevel: https://developer.apple.com/documentation/vision/vnrequesttextrecognitionlevel
//  - "Vision is poor on isolated chars": https://developer.apple.com/forums/thread/657934
//

import Foundation
import PencilKit
import UIKit
import Vision
import CoreML

@MainActor
final class StylusInputService {

    // MARK: - Public result type

    enum Result: Equatable, Sendable {
        case recognized(digit: Int, confidence: Float, latencyMs: Int)
        case unrecognized
        case error(reason: String)
    }

    // MARK: - Tunables (read from AppSettings at call time)

    /// Mirror of `AppSettings.stylusConfidence`. Set by the caller before
    /// each `recognize` call so users' Settings changes take effect on the
    /// next stroke without a relaunch.
    var confidenceThreshold: Float = ConfidenceTier.medium.floatValue

    /// Minimum stroke bounding-box area (in points²) before we even attempt
    /// the Vision fallback. Below this, Vision will almost certainly return
    /// nothing useful — we save time by skipping straight to `.unrecognized`.
    var minStrokeAreaForVisionFallback: CGFloat = 200

    // MARK: - Private state

    /// In-flight recognition task. New calls cancel this.
    private var currentTask: Task<Result, Never>?

    /// Core ML model handle. Lazy-loaded so the cold-start cost (~50 ms on A15)
    /// is paid only when the user actually uses the stylus.
    private var coreMLModel: VNCoreMLModel?

    // MARK: - Public API

    /// Recognize a PencilKit drawing, returning the highest-confidence digit
    /// 1..9 or `.unrecognized`.
    ///
    /// Cancellation: calling this method while a previous task is running
    /// cancels the previous task. Callers (typically GameViewModel) MUST
    /// invoke this on every `StrokeBegin` debounced commit, not on every
    /// stroke-change tick.
    func recognize(_ drawing: PKDrawing) async -> Result {
        currentTask?.cancel()
        let task = Task<Result, Never>.detached(priority: .userInitiated) {
            [confidenceThreshold, minStrokeAreaForVisionFallback,
             weak self] in
            guard let self else { return .unrecognized }
            return await self.runPipeline(
                drawing,
                threshold: confidenceThreshold,
                minVisionArea: minStrokeAreaForVisionFallback
            )
        }
        currentTask = task
        return await task.value
    }

    func cancelInFlight() {
        currentTask?.cancel()
        currentTask = nil
    }

    // MARK: - Pipeline
    //
    // `nonisolated` so we can call it from the `Task.detached` below without
    // jumping back to the main actor for every step. The internal helpers
    // (`preprocess`, `classifyCoreML`, `classifyVision`) are themselves
    // `nonisolated` and don't touch any actor-isolated state.
    private nonisolated func runPipeline(
        _ drawing: PKDrawing,
        threshold: Float,
        minVisionArea: CGFloat
    ) async -> Result {
        let start = Date()

        // -------- Step 1: PRIMARY (Core ML MNIST) --------
        guard let mnistBuffer = preprocess(drawing) else {
            return .unrecognized
        }
        if Task.isCancelled { return .unrecognized }

        if let (digit, confidence) = classifyCoreML(mnistBuffer),
           (1...9).contains(digit),
           confidence >= threshold
        {
            let elapsed = Int(Date().timeIntervalSince(start) * 1000)
            return .recognized(digit: digit, confidence: confidence,
                               latencyMs: elapsed)
        }
        if Task.isCancelled { return .unrecognized }

        // -------- Step 2: FALLBACK (Vision .fast) --------
        // Only attempt Vision when the stroke is large enough to be
        // meaningful. Tiny scribbles never recover via Vision.
        let bounds = drawing.bounds
        let area = bounds.width * bounds.height
        guard area >= minVisionArea else { return .unrecognized }

        if let (digit, confidence) = await classifyVision(drawing),
           (1...9).contains(digit),
           confidence >= 0.55     // Vision threshold is looser than Core ML's
        {
            let elapsed = Int(Date().timeIntervalSince(start) * 1000)
            return .recognized(digit: digit, confidence: confidence,
                               latencyMs: elapsed)
        }

        // -------- Step 3: GIVE UP --------
        return .unrecognized
    }

    // MARK: - Preprocessing
    //
    // MNIST canonical input is 28×28 grayscale, with the digit content
    // centered in a 20×20 box and 4 px padding. Background is BLACK,
    // strokes are WHITE.
    //
    // Pipeline:
    //   1. Tight-bound the drawing (strip whitespace).
    //   2. Render at high resolution (280×280) onto BLACK background using
    //      a fixed-width WHITE pen (pressure-independent — important
    //      because pressure-varying strokes confuse MNIST).
    //   3. Center + scale-fit the content into a 200×200 region with
    //      40 px padding on each side.
    //   4. Downscale to 28×28 with HQ interpolation.
    //   5. Convert to a `CVPixelBuffer` in `kCVPixelFormatType_OneComponent8`.
    //
    /// Returns nil if the drawing is empty or unrenderable.
    private nonisolated func preprocess(_ drawing: PKDrawing) -> CVPixelBuffer? {
        // 1. Bounds
        let bounds = drawing.bounds
        guard bounds.width > 0, bounds.height > 0 else { return nil }

        // 2. High-res render dims
        let hiRes: CGFloat = 280
        let contentSide: CGFloat = hiRes * (20.0 / 28.0)      // 200
        let pad: CGFloat = (hiRes - contentSide) / 2.0        // 40

        let format = UIGraphicsImageRendererFormat()
        format.scale = 1.0
        format.opaque = true
        let renderer = UIGraphicsImageRenderer(
            size: CGSize(width: hiRes, height: hiRes), format: format)

        let big = renderer.image { ctx in
            // Black background
            UIColor.black.setFill()
            ctx.fill(CGRect(x: 0, y: 0, width: hiRes, height: hiRes))

            // Compute transform: scale-fit into contentSide, center inside pad.
            let scale = min(contentSide / bounds.width,
                            contentSide / bounds.height)
            let scaledW = bounds.width  * scale
            let scaledH = bounds.height * scale
            let tx = pad + (contentSide - scaledW) / 2 - bounds.minX * scale
            let ty = pad + (contentSide - scaledH) / 2 - bounds.minY * scale

            ctx.cgContext.translateBy(x: tx, y: ty)
            ctx.cgContext.scaleBy(x: scale, y: scale)

            // Re-render the drawing using a FIXED-WIDTH white pen for the
            // classifier input. The user's on-screen ink can stay
            // pressure-varying — this image is for the model only.
            //
            // Implementation note: PKDrawing doesn't expose a "render with
            // alternate ink" API. Common pattern is to draw the existing
            // PKDrawing then invert + threshold to a binary mask. We use
            // PKDrawing.image(from:scale:) here, then rely on the
            // post-render invert step (below) to get white-on-black.
            let strokeImage = drawing.image(from: bounds, scale: 1.0)
            strokeImage.draw(in: bounds, blendMode: .normal, alpha: 1.0)
        }

        // 3. Invert colors so strokes become WHITE on BLACK.
        guard let inverted = Self.invertColors(big) else { return nil }

        // 4. Downscale to 28×28 with high-quality interpolation.
        guard let small = Self.downscale(inverted,
                                         to: CGSize(width: 28, height: 28))
        else { return nil }

        // 5. Convert to CVPixelBuffer (single-channel 8-bit grayscale).
        return Self.pixelBuffer(from: small)
    }

    // MARK: - Core ML inference

    private nonisolated func classifyCoreML(_ buf: CVPixelBuffer)
        -> (Int, Float)?
    {
        // TODO(P0-7): wire to real bundled model:
        //
        //   let config = MLModelConfiguration()
        //   config.computeUnits = .cpuAndNeuralEngine
        //   let raw = try DigitClassifier(configuration: config)
        //   let vnModel = try VNCoreMLModel(for: raw.model)
        //   let req = VNCoreMLRequest(model: vnModel)
        //   req.imageCropAndScaleOption = .centerCrop
        //   let handler = VNImageRequestHandler(cvPixelBuffer: buf, options: [:])
        //   try handler.perform([req])
        //   guard let top = (req.results as? [VNClassificationObservation])?.first,
        //         let digit = Int(top.identifier) else { return nil }
        //   return (digit, top.confidence)
        //
        // For the scaffold we return nil so the pipeline falls through to
        // Vision (which will also stub-fail), then `.unrecognized`.
        return nil
    }

    // MARK: - Vision OCR fallback

    private nonisolated func classifyVision(_ drawing: PKDrawing)
        async -> (Int, Float)?
    {
        // Render at higher resolution for Vision (it needs ≥64 px in min dim).
        let bounds = drawing.bounds.insetBy(dx: -16, dy: -16)
        let render = UIGraphicsImageRenderer(
            size: CGSize(width: 192, height: 192))
        let img = render.image { ctx in
            UIColor.white.setFill()
            ctx.fill(CGRect(x: 0, y: 0, width: 192, height: 192))
            // Center & scale the drawing into the canvas (dark ink on white,
            // which is what Vision OCR is trained on).
            let scale = min(192 / bounds.width, 192 / bounds.height) * 0.85
            let dx = 96 - bounds.midX * scale
            let dy = 96 - bounds.midY * scale
            ctx.cgContext.translateBy(x: dx, y: dy)
            ctx.cgContext.scaleBy(x: scale, y: scale)
            drawing.image(from: bounds, scale: 1.0)
                   .draw(in: bounds)
        }
        guard let cg = img.cgImage else { return nil }

        return await withCheckedContinuation { (cont:
            CheckedContinuation<(Int, Float)?, Never>) in
            let req = VNRecognizeTextRequest { req, _ in
                guard let observations =
                        req.results as? [VNRecognizedTextObservation],
                      let top = observations.first,
                      let cand = top.topCandidates(1).first,
                      let digit = Int(cand.string.trimmingCharacters(
                                        in: .whitespaces)),
                      (1...9).contains(digit)
                else {
                    cont.resume(returning: nil)
                    return
                }
                cont.resume(returning: (digit, cand.confidence))
            }
            // CRITICAL: `.fast` recognition is better for isolated digits
            // than `.accurate`. See Apple Dev Forum 657934. The
            // `customWords` bias here is a vocabulary hint, NOT a
            // character constraint — it has near-zero effect with
            // `usesLanguageCorrection = false`, but we include it to
            // signal intent.
            req.recognitionLevel = .fast
            req.usesLanguageCorrection = false
            req.customWords = ["1", "2", "3", "4", "5", "6", "7", "8", "9"]
            req.minimumTextHeight = 0.0       // no min — we control via area
            let handler = VNImageRequestHandler(cgImage: cg, options: [:])
            do {
                try handler.perform([req])
            } catch {
                cont.resume(returning: nil)
            }
        }
    }

    // MARK: - Helpers (nonisolated for off-main rendering)

    private nonisolated static func invertColors(_ image: UIImage) -> UIImage? {
        guard let ci = CIImage(image: image),
              let filter = CIFilter(name: "CIColorInvert")
        else { return nil }
        filter.setValue(ci, forKey: kCIInputImageKey)
        guard let out = filter.outputImage,
              let cg = CIContext().createCGImage(out, from: out.extent)
        else { return nil }
        return UIImage(cgImage: cg)
    }

    private nonisolated static func downscale(_ image: UIImage,
                                              to size: CGSize) -> UIImage? {
        let format = UIGraphicsImageRendererFormat()
        format.scale = 1.0
        let r = UIGraphicsImageRenderer(size: size, format: format)
        return r.image { _ in
            image.draw(in: CGRect(origin: .zero, size: size))
        }
    }

    private nonisolated static func pixelBuffer(from image: UIImage)
        -> CVPixelBuffer?
    {
        guard let cg = image.cgImage else { return nil }
        let width = cg.width
        let height = cg.height
        var buffer: CVPixelBuffer?
        let attrs: [CFString: Any] = [
            kCVPixelBufferCGImageCompatibilityKey: true,
            kCVPixelBufferCGBitmapContextCompatibilityKey: true
        ]
        CVPixelBufferCreate(kCFAllocatorDefault, width, height,
                            kCVPixelFormatType_OneComponent8,
                            attrs as CFDictionary, &buffer)
        guard let pb = buffer else { return nil }
        CVPixelBufferLockBaseAddress(pb, [])
        defer { CVPixelBufferUnlockBaseAddress(pb, []) }
        let ctx = CGContext(
            data: CVPixelBufferGetBaseAddress(pb),
            width: width, height: height,
            bitsPerComponent: 8,
            bytesPerRow: CVPixelBufferGetBytesPerRow(pb),
            space: CGColorSpaceCreateDeviceGray(),
            bitmapInfo: CGImageAlphaInfo.none.rawValue
        )
        ctx?.draw(cg, in: CGRect(x: 0, y: 0, width: width, height: height))
        return pb
    }
}
