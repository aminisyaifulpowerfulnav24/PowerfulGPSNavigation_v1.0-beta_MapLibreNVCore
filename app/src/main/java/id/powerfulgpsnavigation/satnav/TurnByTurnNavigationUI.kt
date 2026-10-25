package id.powerfulgpsnavigation.satnav

import android.content.res.Configuration
import android.view.View
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory

fun MapActivity.TurnByTurnNavigationUI() {
    routeOverview.setOnClickListener {
        if (isNavigationTurnByTurnMode) {
            saveStatus(false)

            val orientation = resources.configuration.orientation
            when (orientation) {
                Configuration.ORIENTATION_LANDSCAPE -> {
                    if (isNavigationTurnByTurnMode) {
                        padding = intArrayOf((directionsSummaryCardView.width + 67), 67, 67, 67)
                    }
                }
                Configuration.ORIENTATION_PORTRAIT -> {
                    if (isNavigationTurnByTurnMode) {
                        padding = intArrayOf(60, (navigationTurnByTurnCardView.height + 60), 60, (directionsSummaryCardView.height + 60))
                    }
                } else -> {

                }
            }

            getRouteBounds(route)?.let { routeBounds ->

                mapboxMap.setPadding(0, 0, 0, 0)

                mapboxMap.cameraPosition = CameraPosition.Builder()
                    .bearing(0.0)
                    .tilt(0.0)
                    .build()

                mapboxMap.getCameraForLatLngBounds(routeBounds, padding)?.let { cameraPosition ->
                    mapboxMap.animateCamera(
                        CameraUpdateFactory.newCameraPosition(cameraPosition),
                        1500
                    )
                }
            }

            routeOverview.visibility = View.INVISIBLE
            routeNavigationCurrentSpeed.visibility = View.INVISIBLE
            btnRecenterPosition.visibility = View.VISIBLE
        }
    }

    stopNavigationButton.setOnClickListener {
        backToStartMapView.handleOnBackPressed()
    }

    btnRecenterPosition.setOnClickListener {
        if (isNavigationTurnByTurnMode) {
            saveStatus(true)
            recenterCarLoc()

            btnRecenterPosition.visibility = View.INVISIBLE
            routeOverview.visibility = View.VISIBLE
            routeNavigationCurrentSpeed.visibility = View.VISIBLE
        }
    }

    gentleNavigationSpeaker = GentleNavigationSpeaker(this)

    // Set status awal tombol dari memori SharedPreferences
    if (gentleNavigationSpeaker.isMuted()) {
        btnMuteUnmute.setIconResource(com.mapbox.services.android.navigation.ui.v5.R.drawable.ic_sound_off)
    } else {
        btnMuteUnmute.setIconResource(com.mapbox.services.android.navigation.ui.v5.R.drawable.ic_sound_on)
    }

    btnMuteUnmute.setOnClickListener {
        val nowMuted = gentleNavigationSpeaker.toggleMute()
        btnMuteUnmute.setIconResource(if (nowMuted) com.mapbox.services.android.navigation.ui.v5.R.drawable.ic_sound_off else com.mapbox.services.android.navigation.ui.v5.R.drawable.ic_sound_on)
    }
}