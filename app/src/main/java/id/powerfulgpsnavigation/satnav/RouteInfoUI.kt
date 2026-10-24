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
        navigationViewModel.setRoute(route)
        navigationViewModel.setNavigationData(navigation)
        isNavigationTurnByTurnMode = true
        isNavigating = true
        toggleLocationSource(true)
        saveStatus(true)
        startSimulationRoute()
    }

    startNavigationBtn.setOnClickListener {
        navigationViewModel.setRoute(route)
        navigationViewModel.setNavigationData(navigation)
        isNavigationTurnByTurnMode = true
        isNavigating = true
        toggleLocationSource(false)
        saveStatus(true)
        startNavigationRoute()
    }
}