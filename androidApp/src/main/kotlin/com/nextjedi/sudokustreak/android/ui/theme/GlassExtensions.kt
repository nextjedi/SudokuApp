package com.nextjedi.sudokustreak.android.ui.theme

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Marker for snapshot / preview / test environments where BlurEffect cannot render
 * (Robolectric, Paparazzi, Compose UI Test on a headless emulator pre-API-31).
 *
 * Inject `false` from those environments to force the solid-alpha fallback and get
 * deterministic snapshot output. Defaults to `true` in production.
 */
val LocalGlassEnabled = staticCompositionLocalOf { true }

/**
 * Internal hook for [glassyTint] — exposed so unit tests can pretend the runtime is
 * a different SDK without spinning up Robolectric for every test case.
 *
 * Default reads [Build.VERSION.SDK_INT]; tests can override via
 * [GlassSdkProvider.testOverride].
 */
internal object GlassSdkProvider {
    @Volatile
    private var override: Int? = null

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S, parameter = 0)
    fun isAtLeast(api: Int): Boolean {
        val effective = override ?: Build.VERSION.SDK_INT
        return effective >= api
    }

    /**
     * Set a fake SDK_INT for the duration of a test. Always reset to `null` in an
     * `@After` block. Production code MUST NOT call this.
     */
    @androidx.annotation.VisibleForTesting
    fun testOverride(sdk: Int?) {
        override = sdk
    }
}

/**
 * Glass-tinted background à la M3E `surfaceContainer` w/ optional blur.
 *
 *  - **API 31+ AND [LocalGlassEnabled] AND NOT [LocalReduceTransparency]**: apply a
 *    20px Gaussian blur via `RenderEffect.createBlurEffect()` plus an 8% primary
 *    tint over `surfaceContainer` at 70% alpha. Note: BlurEffect blurs the *content
 *    of this composable*, not what's behind it — for a "blurred backdrop" effect
 *    you must position your container on top of the already-rendered layout, e.g.
 *    via `Modifier.windowInsetsPadding(WindowInsets.statusBars)` over a scrollable
 *    list.
 *  - **Otherwise**: fall back to a solid `surfaceContainerHigh` at 92% alpha plus
 *    the same 8% primary tint. Visually similar but cheaper and deterministic.
 *
 *  Reduce-transparency override is consumed via [LocalReduceTransparency] — when
 *  true, the blur is skipped regardless of SDK. This honours system + per-app
 *  "reduce transparency" accessibility settings.
 *
 *  @param cornerRadius corner radius for the clip + background.
 *  @param tintAlpha alpha used for the primary tint overlay. Default 0.08.
 *  @param blurRadiusPx blur radius in pixels. Default 20px per spec §12.
 */
fun Modifier.glassyTint(
    cornerRadius: Dp = 16.dp,
    tintAlpha: Float = 0.08f,
    blurRadiusPx: Float = 20f,
): Modifier = composed {
    val tint = MaterialTheme.colorScheme.primary.copy(alpha = tintAlpha)
    val fallbackBase = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f)
    val baseGlass = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.70f)

    val canBlur = GlassSdkProvider.isAtLeast(Build.VERSION_CODES.S)
    val glassEnabled = LocalGlassEnabled.current
    val reduceTransparency = LocalReduceTransparency.current
    val useBlur = canBlur && glassEnabled && !reduceTransparency

    val shape = RoundedCornerShape(cornerRadius)

    if (useBlur) {
        this
            .clip(shape)
            .graphicsLayer {
                renderEffect = BlurEffect(
                    radiusX = blurRadiusPx,
                    radiusY = blurRadiusPx,
                    edgeTreatment = TileMode.Clamp,
                )
            }
            .background(baseGlass, shape)
            .background(tint, shape)
    } else {
        this
            .clip(shape)
            .background(fallbackBase, shape)
            .background(tint, shape)
    }
}

/**
 * Internal probe — exposes the modifier decision (blur on/off) to unit tests
 * without forcing them to walk the modifier chain. Pure function — no Compose
 * state, safe to call from non-`@Composable` test code.
 */
@androidx.annotation.VisibleForTesting
internal fun glassyTintWouldBlur(
    sdkInt: Int,
    glassEnabled: Boolean,
    reduceTransparency: Boolean,
): Boolean = sdkInt >= Build.VERSION_CODES.S && glassEnabled && !reduceTransparency

/**
 * Helper composable for tests — wraps content with overridden [LocalReduceTransparency]
 * and [LocalGlassEnabled] values so a test can verify both code paths.
 */
@Composable
@androidx.annotation.VisibleForTesting
internal fun TestGlassEnv(
    reduceTransparency: Boolean,
    glassEnabled: Boolean,
    content: @Composable () -> Unit,
) {
    androidx.compose.runtime.CompositionLocalProvider(
        LocalReduceTransparency provides reduceTransparency,
        LocalGlassEnabled provides glassEnabled,
        content = content,
    )
}
