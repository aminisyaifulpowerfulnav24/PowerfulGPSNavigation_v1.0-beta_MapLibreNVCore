package id.powerfulgpsnavigation.satnav

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import com.mapbox.geojson.Point

fun MapActivity.RouteDirectionsSummary() {
    getDirectionsButton.setOnClickListener {
        isDirectionsRoute = true
        isRouteSummaryUI = true
        isLocationSelectedUI = false
        locationSelectedOnMapUIStatus = false

        originLocation = mapboxMap.locationComponent.lastKnownLocation!!

        originLat = originLocation.latitude
        originLng = originLocation.longitude

        destinationLat = lat
        destinationLng = lon

        origin = Point.fromLngLat(originLocation.longitude, originLocation.latitude)
        destination = Point.fromLngLat(destinationLng, destinationLat)

        routingResponse()
    }

    cancelRouteBtn.setOnClickListener {
        backToLocationOnMapInfo.handleOnBackPressed()
    }

    routeSimulationBtn.setOnClickListener {
        val demoNavigation = navigation ?: return@setOnClickListener

        isRouteSummaryUI = false
        isNavigationTurnByTurnMode = true
        isNavigating = true
        navigationViewModel.setNavigationData(demoNavigation)
        toggleLocationSource(true)
        startSimulationRoute()

        // 1. Cek orientasi layar saat ini
        val currentOrientation = resources.configuration.orientation

        // 2. Gunakan percabangan IF untuk mengunci posisi tersebut
        if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            // Jika sedang mendatar, kunci di posisi landscape
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
            // Jika sedang tegak, kunci di posisi portrait
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    startNavigationBtn.setOnClickListener {
        val realTimeNavigation = navigation ?: return@setOnClickListener

        isRouteSummaryUI = false
        isNavigationTurnByTurnMode = true
        isNavigating = true
        navigationViewModel.setNavigationData(realTimeNavigation)
        toggleLocationSource(false)
        startNavigationRoute()
    }
}