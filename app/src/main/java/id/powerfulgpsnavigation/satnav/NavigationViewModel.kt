package id.powerfulgpsnavigation.satnav

import android.content.Context
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
import com.mapbox.services.android.navigation.v5.models.LegStep
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigationOptions
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener
import com.mapbox.services.android.navigation.v5.routeprogress.RouteLegProgress
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress

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

    private val _onNavigationRouteProgress = MutableLiveData<ProgressChangeListener>()
    var onNavigationRouteProgress: LiveData<ProgressChangeListener> = _onNavigationRouteProgress

    private val _lastLegIndex = MutableLiveData<Int>()
    var lastLegIndex: LiveData<Int> = _lastLegIndex

    private val _lastStepIndex = MutableLiveData<Int>()
    var lastStepIndex: LiveData<Int> = _lastStepIndex

    private val _lastCurrentStep = MutableLiveData<LegStep>()
    var lastCurrentStep: LiveData<LegStep> = _lastCurrentStep

    private val _lastNextStep = MutableLiveData<LegStep?>()
    var lastNextStep: LiveData<LegStep?> = _lastNextStep

    private val _lastLegProgress = MutableLiveData<RouteLegProgress>()
    var lastLegProgress: LiveData<RouteLegProgress> = _lastLegProgress

    private val _lastDistanceRemaining = MutableLiveData<Double>()
    var lastDistanceRemaining: LiveData<Double> = _lastDistanceRemaining

    private val _lastRouteProgress = MutableLiveData<RouteProgress>()
    var lastRouteProgress: LiveData<RouteProgress> = _lastRouteProgress

    // Simpan status apakah sedang navigasi turn-by-turn
    val isNavigating: LiveData<Boolean> = savedStateHandle.getLiveData("IS_NAVIGATING", false)

    var _navigationTurnByTurnData = MutableLiveData<MapboxNavigation>()
    var navigationTurnByTurnData: LiveData<MapboxNavigation> = _navigationTurnByTurnData

    private var mapboxNavigationInstance: MapboxNavigation? = null

    fun getOrCreateNavigation(context: Context): MapboxNavigation {
        if (mapboxNavigationInstance == null) {
            val options = MapboxNavigationOptions.builder()
                .snapToRoute(true)
                .enableOffRouteDetection(true)
                .minimumDistanceBeforeRerouting(20.0)
                .secondsBeforeReroute(3)
                .enableFasterRouteDetection(true)
                .userLocationSnapDistance(25.0)
                .defaultMilestonesEnabled(false)
                .build()
            mapboxNavigationInstance = MapboxNavigation(context.applicationContext, options)
        }
        return mapboxNavigationInstance!!
    }

    override fun onCleared() {
        super.onCleared()
        mapboxNavigationInstance?.onDestroy()
        mapboxNavigationInstance = null
    }

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

    fun setOnNavigationRouteProgress(listener: ProgressChangeListener) {
        _onNavigationRouteProgress.value = listener
    }

    fun clearOnNavigationRouteProgress() {
        _onNavigationRouteProgress.value = null
    }

    fun updateLastRouteProgress(progress: RouteProgress) {
        _lastRouteProgress.value = progress
    }

    fun clearLastRouteProgress() {
        _lastRouteProgress.value = null
    }

    fun setLastStepIndex(index: Int) {
        _lastStepIndex.value = index
    }

    fun clearLastStepIndex() {
        _lastStepIndex.value = null
    }

    fun setLastLegIndex(index: Int) {
        _lastLegIndex.value = index
    }

    fun clearLastLegIndex() {
        _lastLegIndex.value = null
    }

    fun setLastCurrentStep(step: LegStep) {
        _lastCurrentStep.value = step
    }

    fun clearLastCurrentStep() {
        _lastCurrentStep.value = null
    }

    fun setLastNextStep(step: LegStep?) {
        _lastNextStep.value = step
    }

    fun clearLastNextStep() {
        _lastNextStep.value = null
    }

    fun setLastLegProgress(progress: RouteLegProgress) {
        _lastLegProgress.value = progress
    }

    fun clearLastLegProgress() {
        _lastLegProgress.value = null
    }

    fun setLastDistanceRemaining(distance: Double) {
        _lastDistanceRemaining.value = distance
    }

    fun clearLastDistanceRemaining() {
        _lastDistanceRemaining.value = null
    }

    fun setNavigatingStatus(isNavigating: Boolean) {
        savedStateHandle["IS_NAVIGATING"] = isNavigating
    }

    fun clearNavigatingStatus() {
        savedStateHandle.remove<Boolean>("IS_NAVIGATING")
    }

    fun updateLocation(location: Location) {
        _currentLocation.value = location
    }

    fun clearLocationData() {
        _currentLocation.value = null
    }
}