package id.powerfulgpsnavigation.satnav

fun MapActivity.GeocodeSearchUI() {
    isSearchUIVisible = true
    SearchLayoutUI(searchLocLayout, searchLocation, mapView, mapButtonUI).clickSearch()

    onBackPressedDispatcher.addCallback(backToMap)
    removeCloseThisNavMapsApp()
}