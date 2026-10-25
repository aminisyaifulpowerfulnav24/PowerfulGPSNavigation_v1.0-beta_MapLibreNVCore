package id.powerfulgpsnavigation.satnav

import com.mapbox.geojson.Point

fun MapActivity.RouteDirectionsSummary() {
    getDirectionsButton.setOnClickListener {
        originLocation = mapboxMap.locationComponent.lastKnownLocation!!

        originLat = originLocation.latitude
        originLng = originLocation.longitude

        destinationLat = lat
        destinationLng = lon

        origin = Point.fromLngLat(originLocation.longitude, originLocation.latitude)
        destination = Point.fromLngLat(destinationLng, destinationLat)

        routingResponse()

        isDirectionsRoute = true
    }

    cancelRouteBtn.setOnClickListener {
        backToLocationOnMapInfo.handleOnBackPressed()
    }

    routeSimulationBtn.setOnClickListener {
        saveStatus(true)
        navigationViewModel.setNavigationData(navigation)
        navigationViewModel.setDemoNavigationEngine(demoLocationEngine)
        isNavigationTurnByTurnMode = true
        isNavigating = true
        toggleLocationSource(true)
        startSimulationRoute()
    }

    startNavigationBtn.setOnClickListener {
        saveStatus(true)
        navigationViewModel.setNavigationData(navigation)
        navigationViewModel.setNavigationEngine(navigationLocationEngine!!)
        isNavigationTurnByTurnMode = true
        isNavigating = true
        toggleLocationSource(false)
        startNavigationRoute()
    }
}