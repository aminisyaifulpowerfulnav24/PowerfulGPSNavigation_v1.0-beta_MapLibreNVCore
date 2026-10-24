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
        navigationViewModel.setNavigationData(navigation)
        navigationViewModel.setDemoNavigationEngine(demoLocationEngine)
        isNavigationTurnByTurnMode = true
        isNavigating = true
        toggleLocationSource(true)
        saveStatus(true)
        startSimulationRoute()
    }

    startNavigationBtn.setOnClickListener {
        navigationViewModel.setNavigationData(navigation)
        navigationViewModel.setNavigationEngine(navigationLocationEngine!!)
        isNavigationTurnByTurnMode = true
        isNavigating = true
        toggleLocationSource(false)
        saveStatus(true)
        startNavigationRoute()
    }
}