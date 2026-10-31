package id.powerfulgpsnavigation.satnav

import android.animation.ValueAnimator
import android.location.Location
import android.view.animation.LinearInterpolator
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap

class LocationSmoothAnimator(private val mapboxMap: MapboxMap) {

    private var currentAnimator: ValueAnimator? = null
    private var lastInterpolatedLocation: Location? = null
    private var lastUpdateTime: Long = 0

    fun animateToNewLocation(newLocation: Location) {
        val currentTime = System.currentTimeMillis()

        // 1. Hitung durasi dinamis sesuai jeda interval kedatangan GPS
        val dynamicDuration = if (lastUpdateTime > 0) {
            (currentTime - lastUpdateTime).coerceIn(1000L, 2000L)
        } else {
            1000L
        }
        lastUpdateTime = currentTime

        // 2. Gunakan posisi TERAKHIR HASIL INTERPOLASI sebagai titik mulai (bukan raw GPS sebelumnya)
        // Ini mencegah efek patah/melompat saat GPS baru datang di tengah jalan animasi.
        val startLocation = lastInterpolatedLocation ?: newLocation
        if (lastInterpolatedLocation == null) {
            lastInterpolatedLocation = newLocation
            mapboxMap.locationComponent.forceLocationUpdate(newLocation)
            return
        }

        // 3. Batalkan animator lama TANPA memanggil .end() agar tidak melompat (snap)
        currentAnimator?.cancel()

        val startLatLng = LatLng(startLocation.latitude, startLocation.longitude)
        val endLatLng = LatLng(newLocation.latitude, newLocation.longitude)

        // Hitung perbedaan bearing tercetus (shortest path rotation)
        val startBearing = (startLocation.bearing + 360) % 360
        val targetBearing = (newLocation.bearing + 360) % 360
        var bearingDiff = targetBearing - startBearing
        if (bearingDiff > 180) bearingDiff -= 360
        if (bearingDiff < -180) bearingDiff += 360

        currentAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            // Gunakan durasi dinamis yang dihitung dari interval GPS
            duration = dynamicDuration
            interpolator = LinearInterpolator()

            addUpdateListener { animator ->
                val fraction = animator.animatedValue as Float

                // Interpolasi Posisi
                val lat = startLatLng.latitude + (endLatLng.latitude - startLatLng.latitude) * fraction
                val lng = startLatLng.longitude + (endLatLng.longitude - startLatLng.longitude) * fraction

                // Interpolasi Bearing
                val interpolatedBearing = (startBearing + (bearingDiff * fraction) + 360) % 360

                // Buat lokasi ter-interpolasi
                val currentStepLocation = Location(newLocation).apply {
                    latitude = lat
                    longitude = lng
                    bearing = interpolatedBearing
                }

                // Simpan state visual terkini
                lastInterpolatedLocation = currentStepLocation

                // Push ke Mapbox/MapLibre LocationComponent
                mapboxMap.locationComponent.forceLocationUpdate(currentStepLocation)
                mapboxMap.locationComponent.setMaxAnimationFps(60)
            }
            start()
        }
    }

    fun clear() {
        currentAnimator?.cancel()
        currentAnimator = null
        lastInterpolatedLocation = null
        lastUpdateTime = 0
    }
}