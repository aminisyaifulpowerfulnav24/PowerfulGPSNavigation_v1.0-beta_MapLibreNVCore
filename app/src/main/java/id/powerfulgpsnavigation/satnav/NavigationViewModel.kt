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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class NavigationViewModel : ViewModel() {
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

    private val _currentLocationLat = MutableStateFlow<Double>(0.0)
    var currentLocationLat: StateFlow<Double> = _currentLocationLat

    private val _currentLocationLng = MutableStateFlow(0.0)
    var currentLocationLng: StateFlow<Double> = _currentLocationLng

    private val _currentLocationBearing = MutableStateFlow(0f)
    var currentLocationBearing: StateFlow<Float> = _currentLocationBearing

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
                .apply {
                    this.snapEngine
                }
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

    fun clearNavigationEngine() {
        _navigationLocationEngine.value = null
    }

    fun getOrCreateDemoEngine(): ReplayRouteLocationEngine? {
        if (_replayRouteLocationEngine.value == null) {
            _replayRouteLocationEngine.value = ReplayRouteLocationEngine()
        }
        return _replayRouteLocationEngine.value
    }

    fun destroyDemoEngine(): ReplayRouteLocationEngine? {
        _replayRouteLocationEngine.value?.let { engine ->
            try {
                engine.onStop()
            } catch (e: Exception) {
                android.util.Log.e("NavVM", "Gagal menghentikan demo engine: ${e.message}")
            }
        }
        _replayRouteLocationEngine.value = null
        return _replayRouteLocationEngine.value
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

    fun clearNavigationData() {
        mapboxNavigationInstance?.stopNavigation()
        mapboxNavigationInstance?.onDestroy()
        mapboxNavigationInstance = null
        _navigationTurnByTurnData.value = null
    }

    fun startNavigation(route: DirectionsRoute) {
        if (route != null) {
            mapboxNavigationInstance?.startNavigation(route)
        }
    }

    fun stopNavigation() {
        mapboxNavigationInstance?.stopNavigation()
    }

    fun clearRouteAlt() {
        _currentRouteAlt.value = null
    }

    // Clear rute ketika navigasi selesai atau dibatalkan
    fun clearRoute() {
        _currentRoute.value = null
    }

    fun updateLocation(location: Location) {
        _currentLocation.value = location
        _currentLocationLat.value = location.latitude
        _currentLocationLng.value = location.longitude
        _currentLocationBearing.value = location.bearing
    }

    fun clearLocationData() {
        _currentLocation.value = null
        _currentLocationLat.value = 0.0
        _currentLocationLng.value = 0.0
        _currentLocationBearing.value = 0f
    }

    fun setOnNavigationRouteProgress(listener: ProgressChangeListener) {
        _onNavigationRouteProgress.value = listener
    }

    fun clearOnNavigationRouteProgress() {
        _onNavigationRouteProgress.value = null
    }

    fun updateLastRouteProgress(progress: RouteProgress, legIndex: Int, stepIndex: Int, distanceRemaining: Double, nextStep: LegStep?, currentStep: LegStep) {
        val oldLeg = lastLegIndex.value ?: 0
        val oldStep = lastStepIndex.value ?: 0

        if (legIndex > oldLeg || (legIndex == oldLeg && stepIndex >= oldStep)) {
            _lastRouteProgress.value = progress
            _lastLegIndex.value = legIndex
            _lastStepIndex.value = stepIndex
            _lastDistanceRemaining.value = distanceRemaining
            _lastNextStep.value = nextStep
            _lastCurrentStep.value = currentStep
        }
    }

    fun clearLastRouteProgress() {
        _lastRouteProgress.value = null
        _lastLegIndex.value = null
        _lastStepIndex.value = null
        _lastDistanceRemaining.value = null
        _lastNextStep.value = null
        _lastCurrentStep.value = null
    }

    fun updateRouteProgress(routeProgress: RouteProgress) {
        _lastRouteProgress.value = routeProgress
    }

    fun clearRouteProgress() {
        _lastRouteProgress.value = null
    }

    fun updateLastLocation(location: Location) {
        _currentLocation.value = location
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
}