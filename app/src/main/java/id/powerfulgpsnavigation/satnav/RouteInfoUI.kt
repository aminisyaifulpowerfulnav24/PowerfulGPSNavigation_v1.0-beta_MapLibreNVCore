package id.powerfulgpsnavigation.satnav

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
        isRouteSummaryUI = false
        isNavigationTurnByTurnMode = true
        isNavigating = true
        navigationViewModel.setDemoNavigationEngine(demoLocationEngine)
        saveStatus(true)
        toggleLocationSource(true)
        startSimulationRoute()
    }

    startNavigationBtn.setOnClickListener {
        isRouteSummaryUI = false
        isNavigationTurnByTurnMode = true
        isNavigating = true
        navigationViewModel.setNavigationEngine(navigationLocationEngine!!)
        saveStatus(true)
        toggleLocationSource(false)
        startNavigationRoute()
    }
}