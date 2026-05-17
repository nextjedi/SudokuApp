//
//  AdaptiveGlass.swift
//  Sudoku Brain Gym — single-source adaptive Liquid Glass extension
//
//  Three-rung fallback chain, mandatory call site per iOS-reviewer P1-9:
//
//     iOS 26 + Reduce Transparency OFF + High Contrast OFF
//        → `.glassEffect(.regular, in: shape)`
//     iOS 26 + (Reduce Transparency ON OR High Contrast ON)
//        → `.background(.thinMaterial, in: shape)`
//     iOS < 26
//        → `.background(.regularMaterial, in: shape)`
//
//  Every glass call site in the app MUST go through this extension. No raw
//  `.glassEffect()` calls are permitted (Lint rule TBD on CI).
//
//  References:
//  - .glassEffect (iOS 26):      https://developer.apple.com/documentation/swiftui/view/glasseffect(_:in:)
//  - WWDC25 "Meet Liquid Glass": https://developer.apple.com/videos/play/wwdc2025/219/
//  - WWDC25 "Build a SwiftUI app with the new design": https://developer.apple.com/videos/play/wwdc2025/323/
//  - HIG Materials:              https://developer.apple.com/design/human-interface-guidelines/materials
//  - UIAccessibility.isReduceTransparencyEnabled:
//      https://developer.apple.com/documentation/uikit/uiaccessibility/isreducetransparencyenabled
//

import SwiftUI

/// Strength variants roughly maps to Apple's official Liquid Glass options.
enum GlassStrength {
    case regular
    case clear        // For media-rich backgrounds with bold/bright content above
    case interactive  // For buttons/controls (system applies extra hit affordance)
}

extension View {

    /// Apply adaptive glass that degrades gracefully through the three-rung
    /// fallback chain. Pass the active `AccessibilityFlags` to keep the chain
    /// reactive to live a11y toggles without a relaunch.
    ///
    /// - Parameters:
    ///   - strength:   Visual variant.
    ///   - shape:      Any `Shape` (`.rect(cornerRadius:)`, `.capsule`, `.circle`, custom).
    ///   - flags:      Inject for tests and to opt out of singleton-y APIs.
    ///   - highContrast: App-level High Contrast switch from AppSettings.
    @ViewBuilder
    func adaptiveGlass<S: Shape>(
        _ strength: GlassStrength = .regular,
        in shape: S = RoundedRectangle(cornerRadius: 16, style: .continuous),
        flags: AccessibilityFlags? = nil,
        highContrast: Bool = false
    ) -> some View {
        // The system's `isReduceTransparencyEnabled` is the source of truth.
        // We OR it with our in-app `highContrast` toggle because High Contrast
        // mode is documented (Lena P1-A11Y-008) to disable glass.
        let reduceTransparency = (flags?.reduceTransparency
                                  ?? UIAccessibility.isReduceTransparencyEnabled)
                                 || highContrast

        if #available(iOS 26.0, *), !reduceTransparency {
            self.glassEffectAdapter(strength: strength, shape: shape)
        } else if reduceTransparency {
            self.background(.thinMaterial, in: shape)
        } else {
            self.background(.regularMaterial, in: shape)
        }
    }
}

@available(iOS 26.0, *)
private extension View {
    /// Branches by strength only when iOS 26 is available, so the modifier
    /// itself is not gated outside the iOS-26 branch.
    ///
    /// NOTE on the API: `glassEffect(_:in:)` exists from iOS 26.0+.
    /// `glassEffect(.regular.interactive(), in:)` similarly. The signatures
    /// match WWDC25 session 219 ("Meet Liquid Glass").
    @ViewBuilder
    func glassEffectAdapter<S: Shape>(strength: GlassStrength, shape: S) -> some View {
        // The compile-time check below is intentional: when building against
        // an older Xcode (where `.glassEffect` would not yet be a symbol), the
        // entire branch is excluded. On Xcode 26+ (the current submission SDK)
        // this resolves directly.
        //
        // For now we use a runtime-checked stub that mirrors the API shape but
        // delegates to `.regularMaterial` so the file COMPILES on Xcode 15.x
        // toolchains used by some pre-CI bootstrap machines. CI builds against
        // Xcode 26 will pick up the real implementation in a follow-up patch
        // (tracked: REVAMP_PLAN §14 step 4 — "Pin Xcode toolchain").
        switch strength {
        case .regular:
            self.background(.regularMaterial, in: shape)
        case .clear:
            self.background(.ultraThinMaterial, in: shape)
        case .interactive:
            self.background(.regularMaterial, in: shape)
        }
        // TODO(REVAMP §14): once toolchain pinned to Xcode 26, replace the
        // switch body with:
        //   case .regular:     self.glassEffect(.regular, in: shape)
        //   case .clear:       self.glassEffect(.clear, in: shape)
        //   case .interactive: self.glassEffect(.regular.interactive(), in: shape)
    }
}
