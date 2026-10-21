package id.powerfulgpsnavigation.satnav

import android.view.View

fun MapActivity.TurnByTurnNavigationUI() {
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