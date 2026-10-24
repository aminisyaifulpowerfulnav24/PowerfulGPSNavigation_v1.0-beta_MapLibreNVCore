package id.powerfulgpsnavigation.satnav

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.services.android.navigation.v5.models.DirectionsRoute
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigationOptions

class NavigationViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {
    private val _currentMapView = MutableLiveData<MapView>()
    val currentMapView: LiveData<MapView> = _currentMapView

    private val _currentRoute = MutableLiveData<DirectionsRoute>()
    val currentRoute: LiveData<DirectionsRoute> = _currentRoute

    // Data lokasi real-time menggunakan LiveData biasa (non-persisted)
    private val _currentLocation = MutableLiveData<Location>()

    var currentLocation: LiveData<Location> = _currentLocation

    // Simpan status apakah sedang navigasi turn-by-turn
    val isNavigating: LiveData<Boolean> = savedStateHandle.getLiveData("IS_NAVIGATING", false)

    var _navigationTurnByTurnData = MutableLiveData<MapboxNavigation>()

    var navigationTurnByTurnData: LiveData<MapboxNavigation> = _navigationTurnByTurnData

    fun setMapView(mapView: MapView) {
        _currentMapView.value = mapView
    }

    // Panggil fungsi ini saat pertama kali mendapatkan route
    fun setRoute(route: DirectionsRoute) {
        _currentRoute.value = route
    }

    fun setNavigationData(navigation: MapboxNavigation) {
        _navigationTurnByTurnData.value = navigation
    }

    // Clear rute ketika navigasi selesai atau dibatalkan
    fun clearRoute() {
        _currentRoute.value = null
    }

    fun clearNavigationData() {
        _navigationTurnByTurnData.value = null
    }

    fun setNavigatingStatus(isNavigating: Boolean) {
        savedStateHandle["IS_NAVIGATING"] = isNavigating
    }

    fun updateLocation(location: Location) {
        _currentLocation.value = location
    }
}