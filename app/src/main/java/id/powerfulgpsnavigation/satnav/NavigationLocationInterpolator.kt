package id.powerfulgpsnavigation.satnav

import android.content.Context
import android.hardware.display.DisplayManager
import android.location.Location
import android.os.Build
import android.os.SystemClock
import android.view.Choreographer
import android.view.Display
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap

class LocationSmoothAnimator(
    private val context: Context,
    private val mapboxMap: MapboxMap
) {

    private var lastInterpolatedLocation: Location? = null
    private var lastUpdateTime: Long = 0L
    private var isMaxFpsConfigured = false

    // State untuk animasi berbasis Choreographer (kebal Animator Duration Scale)
    private var isAnimating = false
    private var animStartTimeMs: Long = 0L
    private var animDurationMs: Long = 1000L
    private var startLatLng: LatLng? = null
    private var endLatLng: LatLng? = null
    private var startBearing: Float = 0f
    private var bearingDiff: Float = 0f
    private var targetLocationTemplate: Location? = null

    // Reuse objek untuk mencegah alokasi memori per frame
    private val reusableStepLocation = Location("interpolated")

    private val choreographer = Choreographer.getInstance()

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!isAnimating) return

            // Hentikan animasi secara aman jika LocationComponent belum siap atau dinonaktifkan
            if (!isLocationComponentReady()) {
                isAnimating = false
                return
            }

            val start = startLatLng ?: return
            val end = endLatLng ?: return
            val template = targetLocationTemplate ?: return

            val elapsedMs = SystemClock.elapsedRealtime() - animStartTimeMs
            val rawFraction = if (animDurationMs > 0L) {
                (elapsedMs.toFloat() / animDurationMs.toFloat()).coerceIn(0f, 1f)
            } else {
                1f
            }

            val lat = start.latitude + (end.latitude - start.latitude) * rawFraction
            val lng = start.longitude + (end.longitude - start.longitude) * rawFraction
            val interpolatedBearing = (startBearing + (bearingDiff * rawFraction) + 360f) % 360f

            reusableStepLocation.set(template)
            reusableStepLocation.latitude = lat
            reusableStepLocation.longitude = lng
            reusableStepLocation.bearing = interpolatedBearing

            lastInterpolatedLocation = reusableStepLocation
            mapboxMap.locationComponent.forceLocationUpdate(reusableStepLocation)

            if (rawFraction < 1f) {
                choreographer.postFrameCallback(this)
            } else {
                isAnimating = false
            }
        }
    }

    /**
     * Memastikan LocationComponent sudah diaktivasi dan di-enable sebelum method lain dipanggil
     */
    private fun isLocationComponentReady(): Boolean {
        val component = mapboxMap.locationComponent
        return component.isLocationComponentActivated && component.isLocationComponentEnabled
    }

    /**
     * Mengatur FPS maksimum hanya setelah LocationComponent dipastikan aktif
     */
    private fun ensureMaxFpsConfigured() {
        if (isMaxFpsConfigured || !isLocationComponentReady()) return

        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
        val display = displayManager?.getDisplay(Display.DEFAULT_DISPLAY)
        val refreshRate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display?.refreshRate ?: display?.refreshRate ?: 60f
        } else {
            @Suppress("DEPRECATION")
            display?.refreshRate ?: 60f
        }

        mapboxMap.locationComponent.setMaxAnimationFps(refreshRate.toInt().coerceAtLeast(60))
        isMaxFpsConfigured = true
    }

    fun animateToNewLocation(newLocation: Location) {
        // Cegah LocationComponentNotInitializedException jika GPS masuk lebih dulu sebelum peta siap
        if (!isLocationComponentReady()) {
            return
        }

        // Terapkan setting FPS saat pertama kali LocationComponent sudah siap
        ensureMaxFpsConfigured()

        // 1. Filter akurasi rendah saat di antara gedung bertingkat (urban canyon)
        if (newLocation.hasAccuracy() && newLocation.accuracy > 35f) {
            return
        }

        val currentTime = SystemClock.elapsedRealtime()
        val dynamicDuration = if (lastUpdateTime > 0L) {
            (currentTime - lastUpdateTime).coerceIn(800L, 1800L)
        } else {
            1000L
        }
        lastUpdateTime = currentTime

        val currentStart = lastInterpolatedLocation
        if (currentStart == null) {
            lastInterpolatedLocation = Location(newLocation)
            mapboxMap.locationComponent.forceLocationUpdate(newLocation)
            return
        }

        // 2. Filter getaran kecil saat kendaraan berhenti/macet
        val distanceMoved = currentStart.distanceTo(newLocation)
        if (distanceMoved < 1.2f && (!newLocation.hasSpeed() || newLocation.speed < 0.8f)) {
            return
        }

        // 3. Hentikan loop frame lama tanpa melompat
        choreographer.removeFrameCallback(frameCallback)

        startLatLng = LatLng(currentStart.latitude, currentStart.longitude)
        endLatLng = LatLng(newLocation.latitude, newLocation.longitude)
        targetLocationTemplate = newLocation

        // Kunci bearing saat kecepatan rendah agar panah navigasi tidak berputar liar
        startBearing = (currentStart.bearing + 360f) % 360f
        val targetBearing = if (newLocation.hasSpeed() && newLocation.speed < 0.5f) {
            startBearing
        } else {
            (newLocation.bearing + 360f) % 360f
        }

        var diff = targetBearing - startBearing
        if (diff > 180f) diff -= 360f
        if (diff < -180f) diff += 360f
        bearingDiff = diff

        animStartTimeMs = currentTime
        animDurationMs = dynamicDuration
        isAnimating = true

        choreographer.postFrameCallback(frameCallback)
    }

    fun clear() {
        isAnimating = false
        isMaxFpsConfigured = false
        choreographer.removeFrameCallback(frameCallback)
        lastInterpolatedLocation = null
        lastUpdateTime = 0L
        startLatLng = null
        endLatLng = null
        targetLocationTemplate = null
    }
}
