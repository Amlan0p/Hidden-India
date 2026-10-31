package `in`.hiddenindia.app.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import `in`.hiddenindia.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.Executor
import kotlin.coroutines.resume

/** A validated, recent device location. */
data class FreshFix(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float?,
    val ageMs: Long,
    val provider: String
)

/**
 * Requests the device's PRESENT location for the Emergency SOS screen.
 *
 * Unlike LocationManager.getLastKnownLocation() (which returns whatever fix any app last
 * stored, possibly hours or days old and from another city), this asks the platform for a
 * new fix via LocationManagerCompat.getCurrentLocation (already available through
 * androidx.core, no new dependency). Every returned fix is validated:
 *  - finite, in range, and not (0.0, 0.0)
 *  - no older than [MAX_FIX_AGE_MS]
 * If no valid fix can be obtained in time, null is returned. Nothing is ever substituted from
 * a cache, a destination, or a previous selection.
 */
object FreshLocationProvider {
    private const val TAG = "EmergencySOS"
    private const val MAX_FIX_AGE_MS = 60_000L      // a "fresh" fix must be at most 1 minute old
    private const val GOOD_ACCURACY_M = 100f        // stop waiting once we have this good a fix
    private const val GRACE_AFTER_FIRST_FIX_MS = 3_000L // wait briefly for a more accurate fix
    private const val DEFAULT_TIMEOUT_MS = 20_000L

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    /** Returns a validated fresh fix, or null if one could not be obtained. */
    @SuppressLint("MissingPermission") // permission is checked below before any request
    suspend fun getFreshLocation(context: Context, timeoutMs: Long = DEFAULT_TIMEOUT_MS): FreshFix? {
        val appContext = context.applicationContext
        val fineGranted = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (!hasLocationPermission(appContext)) {
            debug("fresh location: permission not granted")
            return null
        }

        val lm = appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val candidates = buildList {
            if (fineGranted) add(LocationManager.GPS_PROVIDER)
            add(LocationManager.NETWORK_PROVIDER)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(LocationManager.FUSED_PROVIDER)
        }
        val providers = candidates.filter { p ->
            try { lm.isProviderEnabled(p) } catch (e: Exception) { false }
        }
        if (providers.isEmpty()) {
            debug("fresh location: no enabled location provider")
            return null
        }

        val executor = ContextCompat.getMainExecutor(appContext)
        var best: Location? = null
        var bestProvider = ""

        withTimeoutOrNull(timeoutMs) {
            coroutineScope {
                val results = Channel<ProviderResult>(Channel.UNLIMITED)
                providers.forEach { provider ->
                    launch(Dispatchers.Main) {
                        results.send(ProviderResult(provider, requestFromProvider(lm, provider, executor)))
                    }
                }

                var received = 0
                var graceDeadline = 0L
                while (received < providers.size) {
                    val next: ProviderResult = if (best == null) {
                        results.receive()
                    } else {
                        val remaining = graceDeadline - SystemClock.elapsedRealtime()
                        if (remaining <= 0L) break
                        withTimeoutOrNull(remaining) { results.receive() } ?: break
                    }
                    received++

                    val loc = next.location
                    if (loc != null && isUsable(loc)) {
                        val current = best
                        val better = current == null ||
                            (loc.hasAccuracy() && (!current.hasAccuracy() || loc.accuracy < current.accuracy))
                        if (better) {
                            best = loc
                            bestProvider = next.provider
                        }
                        if (current == null) {
                            graceDeadline = SystemClock.elapsedRealtime() + GRACE_AFTER_FIRST_FIX_MS
                        }
                        val chosen = best
                        if (chosen != null && chosen.hasAccuracy() && chosen.accuracy <= GOOD_ACCURACY_M) break
                    }
                }
                // Stop any provider request still running.
                coroutineContext.cancelChildren()
            }
        }

        val fix = best ?: return null
        return FreshFix(
            latitude = fix.latitude,
            longitude = fix.longitude,
            accuracyMeters = if (fix.hasAccuracy()) fix.accuracy else null,
            ageMs = ageMs(fix),
            provider = bestProvider
        )
    }

    private class ProviderResult(val provider: String, val location: Location?)

    @SuppressLint("MissingPermission")
    private suspend fun requestFromProvider(
        lm: LocationManager,
        provider: String,
        executor: Executor
    ): Location? = suspendCancellableCoroutine { cont ->
        val signal = CancellationSignal()
        cont.invokeOnCancellation { signal.cancel() }
        try {
            LocationManagerCompat.getCurrentLocation(lm, provider, signal, executor) { location ->
                if (cont.isActive) cont.resume(location)
            }
        } catch (e: SecurityException) {
            if (cont.isActive) cont.resume(null)
        } catch (e: Exception) {
            if (cont.isActive) cont.resume(null)
        }
    }

    /** Valid = finite, in range, not (0,0), and recent. */
    fun isUsable(loc: Location): Boolean {
        val lat = loc.latitude
        val lng = loc.longitude
        if (lat.isNaN() || lng.isNaN() || lat.isInfinite() || lng.isInfinite()) return false
        if (lat < -90.0 || lat > 90.0 || lng < -180.0 || lng > 180.0) return false
        if (kotlin.math.abs(lat) < 1e-6 && kotlin.math.abs(lng) < 1e-6) return false
        val age = ageMs(loc)
        return age in -5_000L..MAX_FIX_AGE_MS
    }

    private fun ageMs(loc: Location): Long =
        if (loc.elapsedRealtimeNanos > 0L) {
            (SystemClock.elapsedRealtimeNanos() - loc.elapsedRealtimeNanos) / 1_000_000L
        } else {
            System.currentTimeMillis() - loc.time
        }

    private fun debug(msg: String) {
        if (BuildConfig.DEBUG) Log.d(TAG, "SOS LOCATION:\n$msg")
    }
}
