package id.powerfulgpsnavigation.satnav

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.mapbox.mapboxsdk.location.engine.LocationEngine
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.services.android.navigation.v5.location.replay.ReplayRouteLocationEngine
import com.mapbox.services.android.navigation.v5.models.DirectionsRoute
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigationOptions

class NavigationViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {
    private val _navigationLocationEngine = MutableLiveData<LocationEngine>()
    val navigationLocationEngine: LiveData<LocationEngine> = _navigationLocationEngine

    private val _replayRouteLocationEngine = MutableLiveData<ReplayRouteLocationEngine>()
    val replayRouteLocationEngine: LiveData<ReplayRouteLocationEngine> = _replayRouteLocationEngine

    private val _currentRouteAlt = MutableLiveData<List<DirectionsRoute>>()
    val currentRouteAlt: LiveData<List<DirectionsRoute>> = _currentRouteAlt

    private val _currentRoute = MutableLiveData<DirectionsRoute>()
    val currentRoute: LiveData<DirectionsRoute> = _currentRoute

    // Data lokasi real-time menggunakan LiveData biasa (non-persisted)
    private val _currentLocation = MutableLiveData<Location>()

    var currentLocation: LiveData<Location> = _currentLocation

    // Simpan status apakah sedang navigasi turn-by-turn
    val isNavigating: LiveData<Boolean> = savedStateHandle.getLiveData("IS_NAVIGATING", false)

    var _navigationTurnByTurnData = MutableLiveData<MapboxNavigation>()
    var navigationTurnByTurnData: LiveData<MapboxNavigation> = _navigationTurnByTurnData

    fun setNavigationEngine(navigationLocationEngine: LocationEngine) {
        _navigationLocationEngine.value = navigationLocationEngine
    }

    fun setDemoNavigationEngine(replayRouteLocationEngine: ReplayRouteLocationEngine) {
        _replayRouteLocationEngine.value = replayRouteLocationEngine
    }

    fun setRouteAlt(routeAlt: List<DirectionsRoute>) {
        _currentRouteAlt.value = routeAlt
    }

    // Panggil fungsi ini saat pertama kali mendapatkan route
    fun setRoute(route: DirectionsRoute) {
        _currentRoute.value = route
    }

    fun setNavigationData(navigation: MapboxNavigation) {
        _navigationTurnByTurnData.value = navigation
    }

    fun clearRouteAlt() {
        _currentRouteAlt.value = null
    }

    // Clear rute ketika navigasi selesai atau dibatalkan
    fun clearRoute() {
        _currentRoute.value = null
    }

    fun clearNavigationData() {
        _navigationTurnByTurnData.value = null
    }

    fun clearLocationData() {
        _currentLocation.value = null
    }

    fun setNavigatingStatus(isNavigating: Boolean) {
        savedStateHandle["IS_NAVIGATING"] = isNavigating
    }

    fun updateLocation(location: Location) {
        _currentLocation.value = location
    }
}