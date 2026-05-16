package com.nextjedi.sudokustreak.domain.haptics

/**
 * Web (wasmJs) actual for [PlatformHaptics].
 *
 * Browsers do not expose haptics consistently — `navigator.vibrate` is supported only
 * on mobile Chrome. Permanently no-op on web.
 */
actual class PlatformHaptics {
    actual fun tap() {}
    actual fun success() {}
    actual fun warning() {}
    actual fun error() {}
}
