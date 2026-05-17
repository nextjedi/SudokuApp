package com.nextjedi.sudokustreak.android.sensor

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.nextjedi.sudokustreak.domain.sensor.SensorService

/**
 * Process-wide singleton holder for [SensorService].
 *
 * ## Why a singleton?
 *
 * `SensorService` owns three [kotlinx.coroutines.flow.MutableSharedFlow]s that are
 * intended to fan out to multiple consumers (the GameViewModel reads proximity,
 * the theme controller reads light, the Game screen reads tilt). Constructing a
 * fresh instance per-screen would (a) leak listeners across config changes and
 * (b) cost a sensor-framework `registerListener` call per ViewModel — which on
 * `TYPE_ROTATION_VECTOR` wakes the fusion DSP for each one.
 *
 * Conceptually parallel to `dataStore` (`SudokuApplication.kt`): created once at
 * Application scope, lifecycle-bound to the foreground Activity.
 *
 * ## Wiring
 *
 * 1. `SudokuApplication.onCreate()` calls [get] so the singleton is built eagerly,
 *    *before* the first Activity hits `onCreate`. This guarantees that sensor
 *    listeners attach the instant the user reaches the Game screen instead of
 *    paying for cold-start the first time the screen needs a tilt value.
 *
 * 2. `MainActivity.onCreate()` calls [bind] with `this` so listeners auto-register
 *    on `ON_RESUME` and unregister on `ON_PAUSE`.
 *
 * @see com.nextjedi.sudokustreak.android.SudokuApplication
 */
object SensorServiceProvider {
    @Volatile
    private var instance: SensorService? = null

    /**
     * Get or lazily construct the singleton [SensorService]. Double-checked
     * locking pattern — concurrent `get()` calls return the same instance.
     *
     * Safe to call from any thread.
     */
    fun get(context: Context): SensorService {
        val cached = instance
        if (cached != null) return cached
        return synchronized(this) {
            val again = instance
            if (again != null) {
                again
            } else {
                val created = SensorService.create(context.applicationContext)
                instance = created
                created
            }
        }
    }

    /**
     * Bind the singleton to a [LifecycleOwner] so its listeners auto-register on
     * `ON_RESUME` and unregister on `ON_PAUSE`. Idempotent — observing the same
     * owner twice is a no-op.
     */
    fun bind(owner: LifecycleOwner, context: Context) {
        get(context).observe(owner)
    }

    /**
     * Drop the singleton — exposed for test use only. Production code never
     * needs to release the instance because it lives at Application scope.
     */
    internal fun reset() {
        synchronized(this) {
            instance?.stop()
            instance = null
        }
    }
}
