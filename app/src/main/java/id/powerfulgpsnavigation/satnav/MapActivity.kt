package id.powerfulgpsnavigation.satnav

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.os.Parcelable
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.location.LocationComponent
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.OnLocationCameraTransitionListener
import com.mapbox.mapboxsdk.location.engine.LocationEngine
import com.mapbox.mapboxsdk.location.engine.LocationEngineCallback
import com.mapbox.mapboxsdk.location.engine.LocationEngineRequest
import com.mapbox.mapboxsdk.location.engine.LocationEngineResult
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.maps.UiSettings
import com.mapbox.services.android.navigation.ui.v5.route.NavigationRoute
import com.mapbox.services.android.navigation.v5.models.DirectionsCriteria
import com.mapbox.services.android.navigation.v5.models.DirectionsRoute
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute
import com.mapbox.services.android.navigation.ui.v5.route.OnRouteSelectionChangeListener
import com.mapbox.services.android.navigation.v5.location.engine.LocationEngineProvider
import com.mapbox.services.android.navigation.v5.location.replay.ReplayRouteLocationEngine
import com.mapbox.services.android.navigation.v5.milestone.MilestoneEventListener
import com.mapbox.services.android.navigation.v5.milestone.VoiceInstructionMilestone
import com.mapbox.services.android.navigation.v5.models.RouteOptions
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigationOptions
import com.mapbox.services.android.navigation.v5.offroute.OffRouteListener
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.net.URL
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Objects

class MapActivity : AppCompatActivity() {

    @SuppressLint("MissingPermission")
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted || coarseLocationGranted) {
            // Izin diberikan, aman untuk akses lokasi
            if (realTimeLocationEnabled) {
                startGPSUpdates()
            }
        } else {
            // Izin ditolak, beri tahu pengguna tanpa menutup aplikasi
            showPermissionDeniedDialog()
        }
    }

    private var isDataVisible = false
    internal var isSearchUIVisible = false
    private lateinit var sharedPreferences: SharedPreferences

    private var PREF_NAME = "UpdateDataUI"

    private var KEY_STATUS = "status"
    private var KEY_SEARCH_UI_STATUS = "searchUIStatus"
    private var lastPosition: Int = 0

    internal lateinit var mapView: MapView
    internal lateinit var mapboxMap: MapboxMap
    private lateinit var mapUiSettings: UiSettings
    private var fineLocationGranted: Boolean = false
    private var coarseLocationGranted: Boolean = false
    private var backgroundLocationGranted: Boolean = false
    private var locationManager: LocationManager? = null
    private var locationComponent: LocationComponent? = null
    private lateinit var locationAnimationComponentOptions: LocationComponentOptions
    private lateinit var locationComponentActivationOptions: LocationComponentActivationOptions
    private lateinit var freeDriveMode: MapboxNavigation
    private lateinit var speedNavigationInKph: TextView
    internal var navigationlocation: Location? = null
    internal var lastNavigationLocation: Location? = null
    internal var navigationLocationEngine: LocationEngine? = null
    private lateinit var navigationCamera: CameraPosition
    private lateinit var locationOptions: LocationComponentOptions

    private var density: Float = 0.0f

    private var pLeft: Int = 0
    private var pTop: Int = 0
    private var pRight: Int = 0
    private var pBottom: Int = 0

    internal var layoutLeft: Int = 0
    internal var layoutRight: Int = 0
    internal var layoutTop: Int = 0
    internal var layoutBottom: Int = 0

    private lateinit var locationRequest: LocationEngineRequest

    private var lastPositionLat = 0.0
    private var lastPositionLng = 0.0

    private var mapViewLat = 0.0
    private var mapViewLng = 0.0
    private var mapViewZoom = 1.0

    private var defaultMapViewLat = 0.0
    private var defaultMapViewLng = 0.0
    private var defaultMapViewZoom = 1.0

    private var currentMapStyle = ""

    private lateinit var mapViewCameraPosition: CameraPosition

    // Preferences configuration keys
    private val PREFS_NAME = "MapStatePrefs"
    private val KEY_LAT_VIEW = "saved_latitude"
    private val KEY_LNG_VIEW = "saved_longitude"
    private val KEY_ZOOM = "saved_zoom"

    private lateinit var setMapViewPosition: SharedPreferences
    private lateinit var cameraPosition: CameraPosition
    private lateinit var target: LatLng
    private lateinit var markerCameraPosition: CameraPosition

    private lateinit var style: Style

    internal lateinit var mapButtonUI: ConstraintLayout
    private lateinit var recenterPosition: FloatingActionButton

    internal lateinit var searchMapButton: FloatingActionButton
    internal lateinit var searchLocLayout: View
    internal lateinit var searchLocation: AppCompatEditText
    internal lateinit var searchLocButton: Button
    internal lateinit var backToMapButton: ImageButton
    internal lateinit var adapter: PlaceAdapter
    internal lateinit var listView: ListView
    internal var savedPlaceList: ArrayList<GeocodeResponse>? = null


    private var searchLayoutUIStatus: Boolean = false

    private lateinit var locationInfoLayout: View
    private lateinit var textLocationName: TextView
    private lateinit var textLocationAddress: TextView
    private lateinit var textLocationCoordinates: TextView
    private lateinit var bounds: MutableList<LatLng>
    internal lateinit var backToSearchMapBtn: FloatingActionButton
    private var restoredList: ArrayList<GeocodeResponse>? = null

    internal lateinit var selectedLocation: GeocodeResponse
    internal var name: String = ""
    internal var displayName: String = ""
    internal var lat: Double = 0.0
    internal var lon: Double = 0.0
    private lateinit var markerOptions: MarkerOptions
    private lateinit var markerIcon: com.mapbox.mapboxsdk.annotations.Icon
    private lateinit var markerIconFactory: IconFactory
    private lateinit var marker: Marker
    internal lateinit var getDirectionsButton: ExtendedFloatingActionButton
    private lateinit var getPlaceInfoButton: ExtendedFloatingActionButton

    internal var isLocationSelected: Boolean = false

    internal var isDirectionsRoute: Boolean = false
    private var isRouteSummary: Boolean = false

    companion object {
        private const val KEY_PLACE_LAT = "saved_location_lat"
        private const val KEY_PLACE_LON = "saved_location_lon"
        private const val KEY_PLACE_NAME = "saved_location_name"
        private const val KEY_PLACE_DISPLAY_NAME = "saved_location_display_name"
        private const val KEY_HAS_SELECTION = "saved_location_has_selection"

        private const val KEY_ROUTE_SUMMARY = "route_summary"
        private const val KEY_ROUTE_START_LAT = "route_start_lat"
        private const val KEY_ROUTE_START_LON = "route_start_lon"
        private const val KEY_ROUTE_END_LAT = "route_end_lat"
        private const val KEY_ROUTE_END_LON = "route_end_lon"
        private const val KEY_ROUTE_DISTANCE = "route_distance"
        private const val KEY_ROUTE_DURATION = "route_duration"
        private const val KEY_ROUTE_BASE_URL = "route_base_url"
        private const val KEY_ROUTE_TRANSPORTATION = "route_transportation"
        private const val KEY_ROUTE_URL = "route_url"

        private const val KEY_ROUTE_NAVIGATION = "key_route_navigation"
        private const val KEY_ROUTE_NAVIGATION_UI = "key_route_navigation_ui"
    }

    private lateinit var directionsRouteLayout: ConstraintLayout
    private lateinit var directionsRouteInfoLayout: ConstraintLayout
    private lateinit var routeSummaryLayout: View
    private lateinit var directionsStart: AppCompatEditText
    private lateinit var directionsEnd: AppCompatEditText
    internal lateinit var cancelRouteBtn: AppCompatImageButton
    private lateinit var routeTransportationMode: CardView
    private lateinit var routeTransportationCar: AppCompatImageButton
    private lateinit var routeTransportationPedestrian: AppCompatImageButton
    internal lateinit var routeSimulationBtn: FloatingActionButton
    internal lateinit var startNavigationBtn: ExtendedFloatingActionButton

    private lateinit var routeOptions: RouteOptions

    internal lateinit var padding: IntArray

    internal lateinit var originLocation: Location
    internal lateinit var origin: Point
    private lateinit var waypoint: Point
    internal lateinit var destination: Point

    internal var originLat: Double = 0.0
    internal var originLng: Double = 0.0

    private var waypointLat: Double = 0.0
    private var waypointLng: Double = 0.0

    internal var destinationLat: Double = 0.0
    internal var destinationLng: Double = 0.0

    private lateinit var routeDistance: AppCompatTextView
    private lateinit var routeDuration: AppCompatTextView
    private lateinit var routeEta: AppCompatTextView

    private lateinit var navigationRoute: DirectionsRoute
    private lateinit var navigationRouteBuilder: NavigationRoute.Builder

    internal lateinit var navigation: MapboxNavigation
    private var navigationMapRoute: NavigationMapRoute? = null

    private lateinit var directionsResponse: com.mapbox.services.android.navigation.v5.models.DirectionsResponse
    internal lateinit var route: DirectionsRoute
    private lateinit var allRoutes: MutableList<DirectionsRoute>

    private lateinit var routeList: List<DirectionsRoute> // Untuk menyimpan rute global
    private var currentRouteIndex = 0 // Untuk melacak rute ke berapa yang sedang aktif
    private var routeRestoredList: MutableList<DirectionsRoute> = mutableListOf()
    private lateinit var savedRouteListJson: String

    private var routesJson: String? = null

    private var routeNavigationJson: String? = null

    internal var isNavigating: Boolean = false

    private var osrmBaseUrl = ""
    private var user = ""
    private var vehicle = ""
    private var url = ""

    private lateinit var navigationTurnByTurnUI: View
    internal lateinit var navigationTurnByTurnCardView: CardView
    private lateinit var iconTurnByTurn: ImageView
    private lateinit var turnByTurnDistance: TextView
    private lateinit var turnByTurnStreetName: TextView
    internal lateinit var directionsSummaryCardView: CardView
    private lateinit var directionsTime: TextView
    private lateinit var directionsDistance: TextView
    private lateinit var directionsEta: TextView
    internal lateinit var stopNavigationButton: FloatingActionButton
    internal lateinit var routeOverview: ExtendedFloatingActionButton
    internal lateinit var routeNavigationCurrentSpeed: AppCompatTextView
    internal lateinit var btnRecenterPosition: ExtendedFloatingActionButton
    internal lateinit var gentleNavigationSpeaker: GentleNavigationSpeaker
    internal lateinit var btnMuteUnmute: ExtendedFloatingActionButton

    private var realTimeLocationEnabled = false
    private var isSimulationMode = false
    internal var isNavigationTurnByTurnMode = false
    private var isNavigationTurnByTurnEnabled = false
    internal var demoLocationEngine: ReplayRouteLocationEngine = ReplayRouteLocationEngine()
    private var demoLocationSpeed: Int = 65

    private var locationSmoothAnimator: LocationSmoothAnimator? = null

    internal val navigationViewModel: NavigationViewModel by viewModels()

    private var currentDistanceToManeuver: Double = Double.MAX_VALUE
    private var OSRM_VOICE_MILESTONE_ID = 999

    // State untuk mencatat indeks langkah terakhir yang suaranya sudah dibacakan
    private var lastSpokenStepIndex = -1
    // Ganti pengunci lama dengan pengunci berbasis String Trigger Key
    private var lastSpokenTriggerKey = ""


    @SuppressLint("SetTextI18n", "MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            clearLocalData()
        }

        Mapbox.getInstance(this)

        sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        val inflater = LayoutInflater.from(this)
        val rootView = inflater.inflate(R.layout.activity_map, null)
        setContentView(rootView)

        // 1. Tangkap kondisi berhenti mendadak / crash
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // Jalankan pembersihan data SharedPreferences secara instan
            val sharedPreferences = getSharedPreferences("UpdateDataUI", Context.MODE_PRIVATE)

            // WAJIB menggunakan commit() agar langsung disimpan saat itu juga saat crash
            sharedPreferences.edit().remove("searchUIStatus").commit()

            // Kembalikan ke handler bawaan Android agar aplikasi tetap menampilkan dialog crash
            defaultHandler?.uncaughtException(thread, throwable)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        mapView = rootView.findViewById(R.id.naviMapView)

        navigation = MapboxNavigation(
            this, MapboxNavigationOptions.builder()
                .snapToRoute(true)
                .enableOffRouteDetection(true)
                .minimumDistanceBeforeRerouting(20.0)
                .secondsBeforeReroute(3)
                .enableFasterRouteDetection(true)
                .userLocationSnapDistance(25.0)
                .defaultMilestonesEnabled(false)
                .build()
        ).apply {
            this.snapEngine
        }

        mapView.getMapAsync { map ->

            mapboxMap = map

            mapUiSettings = mapboxMap.uiSettings
            mapUiSettings.setCompassEnabled(true)
            mapUiSettings.setCompassMargins(0, 24, 24, 0)

            restoreMapViewPosition()

            currentMapStyle = if (isNightTime()) DARK_STYLE else LIGHT_STYLE

            mapboxMap.setStyle(currentMapStyle) { nvMapStyle ->

                navigationMapRoute = NavigationMapRoute(navigation, mapView, mapboxMap)

                locationSmoothAnimator = LocationSmoothAnimator(mapboxMap)

                style = nvMapStyle
                if (fineLocationGranted || coarseLocationGranted) {
                    locationComponent = mapboxMap.locationComponent
                    navigationLocationEngine = navigation.locationEngine
                    locationAnimationComponentOptions =
                        LocationComponentOptions.builder(this)
                            .accuracyAnimationEnabled(false)
                            .compassAnimationEnabled(false)
                            .trackingAnimationDurationMultiplier(0.0f)
                            .trackingInitialMoveThreshold(0.0f)
                            .build()
                    locationRequest = LocationEngineRequest.Builder(1000L)
                        .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                        .setFastestInterval(1000L)
                        .build()
                    locationComponentActivationOptions =
                        LocationComponentActivationOptions.builder(this, style)
                            .locationEngine(navigationLocationEngine)
                            .useDefaultLocationEngine(false)
                            .locationComponentOptions(locationAnimationComponentOptions)
                            .locationEngineRequest(locationRequest)
                            .build()

                    if (::navigation.isInitialized) {
                        locationComponent?.activateLocationComponent(
                            locationComponentActivationOptions
                        )
                        locationComponent?.isLocationComponentEnabled = true
                        locationComponent?.locationEngine = null
                        locationComponent?.renderMode = RenderMode.GPS
                        locationComponent?.setMaxAnimationFps(60)
                    }

                    if (isDataVisible) {
                        locationComponent?.setCameraMode(
                            CameraMode.TRACKING_GPS,
                            object : OnLocationCameraTransitionListener {
                                override fun onLocationCameraTransitionFinished(cameraMode: Int) {
                                    locationComponent?.zoomWhileTracking(16.24, 1000)
                                    locationComponent?.tiltWhileTracking(55.0)

                                    if (isNavigationTurnByTurnMode) {
                                        mapboxMap.setPadding(
                                            layoutLeft,
                                            layoutTop,
                                            layoutRight,
                                            layoutBottom
                                        )
                                    } else if (!isNavigationTurnByTurnMode) {
                                        mapboxMap.setPadding(
                                            pLeft,
                                            (pTop + 120).toInt(),
                                            pRight,
                                            pBottom
                                        )
                                    }

                                    mapUiSettings.setCompassEnabled(false)
                                }

                                override fun onLocationCameraTransitionCanceled(cameraMode: Int) {
                                    mapUiSettings.setCompassEnabled(true)
                                }
                            })
                        speedNavigationInKph.visibility = View.VISIBLE
                    } else {
                        locationComponent?.cameraMode = CameraMode.NONE
                        mapUiSettings.setCompassEnabled(true)
                        speedNavigationInKph.visibility = View.INVISIBLE
                    }
                }
            }
            mapboxMap.cameraPosition = CameraPosition.Builder()
                .target(LatLng(mapViewLat, mapViewLng))
                .zoom(mapViewZoom)
                .build()

            mapboxMap.addOnMoveListener(object : MapboxMap.OnMoveListener {

                override fun onMoveBegin(p0: MoveGestureDetector) {
                    saveStatus(false)
                    onMovingMap()
                    removeCenteringPosition()

                    if (isNavigationTurnByTurnMode) {
                        btnRecenterPosition.visibility = View.VISIBLE
                        routeOverview.visibility = View.INVISIBLE
                        routeNavigationCurrentSpeed.visibility = View.INVISIBLE
                    }
                }

                override fun onMove(p0: MoveGestureDetector) {
                    removeCenteringPosition()
                }

                override fun onMoveEnd(p0: MoveGestureDetector) {
                    removeCenteringPosition()

                    cameraPosition = mapboxMap.cameraPosition
                    target = cameraPosition.target!!

                    saveMapViewData()

                    removeCenteringPosition()
                }
            })

            if (savedInstanceState != null) {
                isSearchUIVisible = savedInstanceState.getBoolean(KEY_SEARCH_UI_STATUS, false)

                if (isSearchUIVisible) {
                    SearchLayoutUI(
                        searchLocLayout,
                        searchLocation,
                        mapView,
                        mapButtonUI
                    ).clickSearch()

                    onBackPressedDispatcher.addCallback(backToMap)
                    removeCloseThisNavMapsApp()
                }

                // Ambil kembali statusnya
                isLocationSelected = savedInstanceState.getBoolean(KEY_HAS_SELECTION, false)

                // Jika true (artinya rotasi terjadi SAAT lokasi terpilih, BUKAN setelah klik back)
                if (isLocationSelected) {
                    lat = savedInstanceState.getDouble(KEY_PLACE_LAT)
                    lon = savedInstanceState.getDouble(KEY_PLACE_LON)
                    name = savedInstanceState.getString(KEY_PLACE_NAME) ?: ""
                    displayName = savedInstanceState.getString(KEY_PLACE_DISPLAY_NAME) ?: ""

                    onLocationResultSelected()
                }

                isDirectionsRoute = savedInstanceState.getBoolean(KEY_ROUTE_SUMMARY, false)

                savedRouteListJson = savedInstanceState.getString("SAVED_ROUTES") ?: ""

                if (isDirectionsRoute) {
                    originLat = savedInstanceState.getDouble(KEY_ROUTE_START_LAT)
                    originLng = savedInstanceState.getDouble(KEY_ROUTE_START_LON)
                    destinationLat = savedInstanceState.getDouble(KEY_ROUTE_END_LAT)
                    destinationLng = savedInstanceState.getDouble(KEY_ROUTE_END_LON)

                    routeDistance.text = savedInstanceState.getString(KEY_ROUTE_DISTANCE)
                    routeDuration.text = savedInstanceState.getString(KEY_ROUTE_DURATION)

                    osrmBaseUrl = savedInstanceState.getString(KEY_ROUTE_BASE_URL) ?: ""
                    vehicle = savedInstanceState.getString(KEY_ROUTE_TRANSPORTATION) ?: ""
                    url = savedInstanceState.getString(KEY_ROUTE_URL) ?: ""

                    currentRouteIndex = savedInstanceState.getInt("KEY_CURRENT_ROUTE_INDEX", 0)

                    routingResponse()

                    if (savedRouteListJson.isNotEmpty()) {
                        try {
                            runOnUiThread {
                                val jsonArray = org.json.JSONArray(savedRouteListJson)
                                routeRestoredList = mutableListOf()

                                // Rekonstruksi kembali setiap rute dari string JSON
                                for (i in 0 until jsonArray.length()) {
                                    val routeJson = jsonArray.getString(i)
                                    val route = DirectionsRoute.fromJson(routeJson)
                                    routeRestoredList.add(route)
                                }

                                // Masukkan kembali ke variabel global rute Anda
                                routeList = routeRestoredList
                            }

                            Log.d("ROTATION_CHECK", "Semua rute alternatif berhasil dipulihkan.")
                        } catch (e: Exception) {
                            Log.e(
                                "ROTATION_ERROR",
                                "Gagal memulihkan rute alternatif: ${e.message}"
                            )
                        }
                    }
                }

                // Restore boolean & int
                isNavigating = savedInstanceState.getBoolean("KEY_NAV_STARTED", false)
                isNavigationTurnByTurnMode = savedInstanceState.getBoolean("KEY_IS_TBT_MODE", false)
                realTimeLocationEnabled = savedInstanceState.getBoolean("KEY_REALTIME_LOC", false)
                isSimulationMode = savedInstanceState.getBoolean("KEY_SIMULATION_MODE", false)

                navigationlocation?.longitude = savedInstanceState.getDouble("NAVIGATION_LOC_LNG")
                navigationlocation?.latitude = savedInstanceState.getDouble("NAVIGATION_LOC_LAT")
                navigationlocation?.speed = savedInstanceState.getFloat("NAVIGATION_LOC_SPEED")
                navigationlocation?.bearing = savedInstanceState.getFloat("NAVIGATION_LOC_BEARING")

                if (!routeList.isNullOrEmpty()) {
                    routeList = navigationViewModel.currentRouteAlt.value!!

                    navigationMapRoute?.addRoutes(routeList)

                    if (isNavigationTurnByTurnMode) {

                        if (!isSimulationMode) {
                            route = navigationViewModel.currentRoute.value!!
                            navigationlocation = navigationViewModel.currentLocation.value
                            navigation = navigationViewModel.navigationTurnByTurnData.value!!
                            navigationLocationEngine = navigationViewModel.navigationLocationEngine.value!!

                            startNavigationRoute()

                            navigationLocationEngine.let { navigationEngine ->
                                navigation.let {
                                    it.locationEngine = navigationEngine!!
                                    it.startNavigation(route)
                                    it.addProgressChangeListener(navigationProgressChangeListener)
                                    locationSmoothAnimator?.animateToNewLocation(navigationlocation!!)
                                }
                            }

                            if (isDataVisible) {
                                saveStatus(true)
                            } else {
                                saveStatus(false)
                            }
                        } else {
                            route = navigationViewModel.currentRoute.value!!
                            navigationlocation = navigationViewModel.currentLocation.value
                            navigation = navigationViewModel.navigationTurnByTurnData.value!!
                            demoLocationEngine = navigationViewModel.replayRouteLocationEngine.value!!

                            startSimulationRoute()

                            demoLocationEngine.let {
                                navigation.let { navigation ->
                                    it.updateSpeed(demoLocationSpeed)
                                    it.assign(route)
                                    navigation.locationEngine = it
                                    navigation.startNavigation(route)
                                    navigation.addProgressChangeListener(navigationProgressChangeListener)
                                    locationSmoothAnimator?.animateToNewLocation(navigationlocation!!)
                                }
                            }

                            if (isDataVisible) {
                                saveStatus(true)
                            } else {
                                saveStatus(false)
                            }
                        }
                    }
                }
            }

            realTimeLocationEnabled = true

            checkAndRequestLocationPermissions()
        }

        mapView.setMaximumFps(60)

        recenterPosition = rootView.findViewById(R.id.recenterNavigation)

        speedNavigationInKph = rootView.findViewById(R.id.navigationSpeedKph)

        searchMapButton = rootView.findViewById(R.id.searchplace_maps)

        mapButtonUI = rootView.findViewById(R.id.mapUIButton)

        val orientation = resources.configuration.orientation
        density = resources.displayMetrics.density

        when (orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                if (isNavigationTurnByTurnMode) {
                    layoutLeft =
                        (directionsSummaryCardView.width + pLeft + 195)
                    layoutBottom = pBottom
                    layoutRight = pRight
                    layoutTop = (directionsSummaryCardView.height + pTop)
                }
            }
            Configuration.ORIENTATION_PORTRAIT -> {
                if (isNavigationTurnByTurnMode) {
                    layoutLeft = pLeft
                    layoutBottom = pBottom
                    layoutRight = pRight
                    layoutTop = (pTop + 120)
                }
            }
            else -> {

            }
        }

        // Mengambil nilai pixel dari dimens yang otomatis menyesuaikan orientasi
        pLeft = resources.getDimensionPixelSize(R.dimen.mapbox_padding_left)
        pTop = resources.getDimensionPixelSize(R.dimen.mapbox_padding_top)
        pRight = resources.getDimensionPixelSize(R.dimen.mapbox_padding_right)
        pBottom = resources.getDimensionPixelSize(R.dimen.mapbox_padding_bottom)

        searchLocLayout = rootView.findViewById(R.id.searchLocLayout)
        searchLocation = rootView.findViewById(R.id.searchPlaceOnMap)
        backToMapButton = rootView.findViewById(R.id.backToMap)
        searchLocButton = rootView.findViewById(R.id.searchOnMapButton)
        listView = rootView.findViewById(R.id.searchOnMapResults)

        locationInfoLayout = rootView.findViewById(R.id.locationInfoLayout)
        textLocationName = rootView.findViewById(R.id.text_location_name_bar)
        textLocationAddress = rootView.findViewById(R.id.text_location_address_bar)
        textLocationCoordinates = rootView.findViewById(R.id.text_location_coordinates_bar)
        backToSearchMapBtn = rootView.findViewById(R.id.backToSearchMapButton)
        getDirectionsButton = rootView.findViewById(R.id.getDirectionsButton)
        getPlaceInfoButton = rootView.findViewById(R.id.getPlaceInfoButton)

        directionsRouteLayout = rootView.findViewById(R.id.directions_route_layout)
        directionsRouteInfoLayout = rootView.findViewById(R.id.directions_route_info_layout)
        routeSummaryLayout = rootView.findViewById(R.id.routeSummaryLayout)
        directionsStart = rootView.findViewById(R.id.directions_start)
        directionsEnd = rootView.findViewById(R.id.directions_end)
        cancelRouteBtn = rootView.findViewById(R.id.cancelRouteBtn)

        routeTransportationMode = rootView.findViewById(R.id.routeTransportationMode)
        routeTransportationCar = rootView.findViewById(R.id.trasportationCarMode)
        routeTransportationPedestrian = rootView.findViewById(R.id.trasportationWalkingMode)
        routeSimulationBtn = rootView.findViewById(R.id.startSimulationRoute)
        startNavigationBtn = rootView.findViewById(R.id.startNavigationButton)

        routeDistance = rootView.findViewById(R.id.directions_route_distance)
        routeDuration = rootView.findViewById(R.id.directions_route_duration)

        navigationTurnByTurnUI = rootView.findViewById(R.id.navigationTurnByTurnUi)
        navigationTurnByTurnCardView = rootView.findViewById(R.id.maneuver_directions_card)
        iconTurnByTurn = rootView.findViewById(R.id.icon_maneuver_directions)
        turnByTurnDistance = rootView.findViewById(R.id.maneuver_directions_distance)
        turnByTurnStreetName = rootView.findViewById(R.id.maneuver_directions_streetname)
        directionsSummaryCardView = rootView.findViewById(R.id.maneuver_directions_summary_card)
        directionsTime = rootView.findViewById(R.id.directions_time_remaining)
        directionsDistance = rootView.findViewById(R.id.directions_distance_remaining)
        directionsEta = rootView.findViewById(R.id.directions_eta)
        stopNavigationButton = rootView.findViewById(R.id.btnStopNavigation)
        routeOverview = rootView.findViewById(R.id.btnOverviewRoute)
        routeNavigationCurrentSpeed = rootView.findViewById(R.id.navigationRouteSpeedKph)
        btnRecenterPosition = rootView.findViewById(R.id.btnRecenterNavigation)
        btnMuteUnmute = rootView.findViewById(R.id.btn_navigation_voice_mute_unmute)

        recenterPosition.setOnClickListener {
            if (fineLocationGranted || coarseLocationGranted) {
                saveStatus(true)
                recenterCarLoc()
            }
        }

        GeocodeSearchUI()

        RouteDirectionsSummary()

        TurnByTurnNavigationUI()

        if (isDataVisible) {
            updateVisibilityUI()
        } else {
            updateVisibilityUI()
        }

        mapView.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(closeThisNavMapsApp)
    }

    private fun checkAndRequestLocationPermissions() {
        fineLocationGranted = ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        coarseLocationGranted = ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        when {
            fineLocationGranted || coarseLocationGranted -> {
                startGPSUpdates()
            }

            else -> {
                // Launch the system permission dialog
                locationPermissionRequest.launch(
                    arrayOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    // 1. Dialog saat izin ditolak (Memberi tahu akibat & opsi ke Pengaturan)
    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.app_permission_dialog_title))
            .setMessage(getString(R.string.app_permission_dialog_message))
            .setPositiveButton(getString(R.string.open_settings)) { _, _ ->
                // Mengarahkan pengguna langsung ke halaman info aplikasi di Pengaturan HP
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
                // Opsional: Tutup fitur spesifik yang butuh lokasi, BUKAN aplikasinya
            }
            .setCancelable(false) // Mencegah dialog tertutup jika pengguna mengetuk luar dialog
            .show()
    }

    @SuppressLint("MissingPermission")
    fun toggleLocationSource(enableDemoTBTNav: Boolean) {
        this.isSimulationMode = enableDemoTBTNav
    }


    @SuppressLint("SetTextI18n", "MissingPermission")
    fun startGPSUpdates() {
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        locationManager?.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            0L,
            0f,
            gpsLocationListenerUpdated
        )
    }

    private val gpsLocationListenerUpdated = object : LocationListener {
        override fun onLocationChanged(location: Location) {

            if(!isNavigationTurnByTurnMode) {
                navigationlocation = location

                runOnUiThread {
                    locationSmoothAnimator?.animateToNewLocation(navigationlocation!!)

                    speedNavigationInKph.text =
                        "${(navigationlocation!!.speed * 3.6).toInt()}      km/h"
                }
            } else if (isNavigationTurnByTurnMode) {
                lastNavigationLocation = navigationViewModel.currentLocation.value
                navigationlocation = lastNavigationLocation ?: location

                if (::navigation.isInitialized) {
                    runOnUiThread {
                        locationSmoothAnimator?.animateToNewLocation(navigationlocation!!)

                        routeNavigationCurrentSpeed.text = "${(navigationlocation!!.speed * 3.6).toInt()}      km/h"
                    }
                }
            }
        }
    }

    private fun saveMapViewData() {
        if (target != null) {
            setMapViewPosition = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            setMapViewPosition.edit().apply() {
                putFloat(KEY_LAT_VIEW, target.latitude.toFloat())
                putFloat(KEY_LNG_VIEW, target.longitude.toFloat())
                putFloat(KEY_ZOOM, cameraPosition.zoom.toFloat())
                apply()
            }
        }
    }

    // Fungsi bantuan untuk menyimpan status ke SharedPreferences
    internal fun saveStatus(isVisible: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_STATUS, isVisible).apply()
        recenterCarLoc()
        updateVisibilityUI()
    }

    fun updateVisibilityUI() {
        isDataVisible = sharedPreferences.getBoolean(KEY_STATUS, false)

        if (isDataVisible) {
            RecenterPositionButtonUI(recenterPosition).recenterPosition()
            speedNavigationInKph.visibility = View.VISIBLE
            recenterCarLoc()
        } else {
            onMovingMap()
        }
    }

    fun recenterCarLoc() {
        if (isDataVisible) {
            locationComponent?.setCameraMode(
                CameraMode.TRACKING_GPS,
                object : OnLocationCameraTransitionListener {
                    override fun onLocationCameraTransitionFinished(cameraMode: Int) {
                        locationComponent?.zoomWhileTracking(16.24)
                        locationComponent?.tiltWhileTracking(55.0)

                        val orientation = resources.configuration.orientation
                        density = resources.displayMetrics.density

                        when (orientation) {
                            Configuration.ORIENTATION_LANDSCAPE -> {
                                if (isNavigationTurnByTurnMode) {
                                    layoutLeft =
                                        (navigationTurnByTurnCardView.width + pLeft + 260)
                                    layoutBottom = pBottom
                                    layoutRight = pRight
                                    layoutTop = (navigationTurnByTurnCardView.height + pTop)
                                }
                            }
                            Configuration.ORIENTATION_PORTRAIT -> {
                                if (isNavigationTurnByTurnMode) {
                                    layoutLeft = pLeft
                                    layoutBottom = pBottom
                                    layoutRight = pRight
                                    layoutTop = (pTop + 120)
                                }
                            }
                            else -> {

                            }
                        }

                        if (isNavigationTurnByTurnMode) {
                            mapboxMap.setPadding(
                                layoutLeft,
                                layoutTop,
                                layoutRight,
                                layoutBottom
                            )
                        } else if (!isNavigationTurnByTurnMode) {
                            mapboxMap.setPadding(
                                pLeft,
                                (pTop + 120).toInt(),
                                pRight,
                                pBottom
                            )
                        }

                        mapUiSettings.setCompassEnabled(false)
                    }

                    override fun onLocationCameraTransitionCanceled(cameraMode: Int) {
                        mapUiSettings.setCompassEnabled(true)
                    }
                }
            )
        }
    }

    fun onMovingMap() {
        RecenterPositionButtonUI(recenterPosition).unrecenterPosition()
        speedNavigationInKph.visibility = View.INVISIBLE
    }

    fun removeCenteringPosition() {
        val cameraPosition = CameraPosition.Builder()
            .tilt(0.0)
            .build()

        mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 1000)
        mapboxMap.setPadding(0, 0, 0, 0)
        mapUiSettings.setCompassEnabled(true)
    }

    private fun restoreMapViewPosition() {
        setMapViewPosition = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        mapViewLat =
            setMapViewPosition.getFloat(KEY_LAT_VIEW, defaultMapViewLat.toFloat()).toDouble()
        mapViewLng =
            setMapViewPosition.getFloat(KEY_LNG_VIEW, defaultMapViewLng.toFloat()).toDouble()
        mapViewZoom = setMapViewPosition.getFloat(KEY_ZOOM, defaultMapViewZoom.toFloat()).toDouble()
    }

    private fun backToSearchMapUI() {
        onBackPressedDispatcher.addCallback(backToSearchMap)
    }

    internal fun onLocationResultSelected() {
        directionsStart.text = SpannableStringBuilder("Lokasi saya")
        directionsEnd.text = SpannableStringBuilder("${this.displayName}")

        textLocationName.text = name
        textLocationAddress.text = displayName
        textLocationCoordinates.text = "$lat, $lon"

        bounds = mutableListOf<LatLng>()

        val infoIconDrawable = ResourcesCompat.getDrawable(
            this.resources,
            com.mapbox.mapboxsdk.R.drawable.maplibre_marker_icon_default,
            null
        )!!

        val bitmapMarker = infoIconDrawable.toBitmap()
        markerIcon = IconFactory.getInstance(this@MapActivity).fromBitmap(bitmapMarker)

        val addressLatLng = LatLng(lat, lon)
        bounds.add(addressLatLng)

        markerOptions = MarkerOptions()
            .position(addressLatLng)
            .icon(markerIcon)
        marker = mapboxMap.addMarker(markerOptions)

        mapboxMap.getCameraForLatLngBounds(LatLngBounds.fromLatLngs(bounds))?.let {
            markerCameraPosition = CameraPosition.Builder()
                .target(it.target)
                .zoom(14.0)
                .bearing(0.0)
                .tilt(0.0)
                .build()
            mapboxMap.cameraPosition = markerCameraPosition
        }

        mapView.visibility = View.VISIBLE
        locationInfoLayout.visibility = View.VISIBLE
        backToSearchMapBtn.visibility = View.VISIBLE
        searchLocLayout.visibility = View.GONE

        removeCenteringPosition()
        backToSearchMapUI()
    }

    fun fetchOsrmRoute(
        url: String,
        callback: (com.mapbox.services.android.navigation.v5.models.DirectionsResponse?) -> Unit
    ) {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                e.printStackTrace()
                runOnUiThread { callback(null) }
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val jsonString = response.body?.string()
                if (response.isSuccessful && jsonString != null) {
                    try {
                        // MapLibre v2.0.0 masih menggunakan struktur model dari Mapbox v5 SDK secara internal
                        // OSRM JSON standar kompatibel dengan skema ini.
                        val directionsResponse =
                            com.mapbox.services.android.navigation.v5.models.DirectionsResponse.fromJson(
                                jsonString
                            )
                        callback(directionsResponse)
                    } catch (e: Exception) {
                        Log.e("OSRM_PARSE_ERROR", "Gagal memparsing JSON OSRM: ${e.message}")
                        runOnUiThread { callback(null) }
                    }
                } else {
                    runOnUiThread { callback(null) }
                }
            }
        })
    }

    fun routingResponse() {
        if (routeRestoredList.isNullOrEmpty()) {
            fetchRouting()
        } else if (!routeRestoredList.isNullOrEmpty()) {
            loadRoutes()
        }

        RouteSummaryUI(
            locationInfoLayout,
            backToSearchMapBtn,
            routeSummaryLayout,
            routeTransportationMode,
            startNavigationBtn,
            routeDistance,
            routeDuration,
            directionsStart,
            directionsEnd
        ).getDirections()

        onBackPressedDispatcher.addCallback(backToLocationOnMapInfo)
    }

    fun fetchRouting() {
        if (routeRestoredList.isNullOrEmpty()) {
            // GANTI BLOK NAVIGATIONROUTE.BUILDER LAMA ANDA DENGAN INI:
            osrmBaseUrl = "http://router.project-osrm.org/" // Ganti dengan URL server OSRM Anda
            user = "route/v1/"
            vehicle = "driving/"
            url =
                "${osrmBaseUrl}${user}${vehicle}${originLng},${originLat};${destinationLng},${destinationLat}?steps=true&alternatives=true&geometries=polyline6&overview=full&annotations=duration,distance,speed"

            fetchOsrmRoute(url) { responseRoutes ->

                if (responseRoutes != null && responseRoutes.routes().isNotEmpty()) {
                    val selectedRoute: DirectionsRoute = responseRoutes.routes()[currentRouteIndex]
                    directionsResponse = responseRoutes

                    routeList = directionsResponse.routes()
                    route = routeList[currentRouteIndex]

                    runOnUiThread {
                        loadRoutes()

                        navigationViewModel.setRouteAlt(routeList)
                        navigationViewModel.setRoute(route)
                    }
                }
            }
        }
    }

    fun loadRoutes() {
        if (routeList != null) {

            currentRouteIndex = 0

            navigationMapRoute?.addRoutes(routeList)

            if (currentRouteIndex < 1) {
                currentRouteIndex = 0
            } else {
                currentRouteIndex = 1
            }

            navigationMapRoute?.setOnRouteSelectionChangeListener {
                if (currentRouteIndex > 0) {
                    currentRouteIndex = 1
                    updateRouteUi(it)
                }
                if (currentRouteIndex < 1) {
                    currentRouteIndex = 0
                    updateRouteUi(it)
                }
            }

            if (!isNavigationTurnByTurnMode) {
                updateRouteUi(routeList[currentRouteIndex])// Set teks rute utama (Indeks ke-0) secara default
            }

            updateRouteSummaryUI()

        }
    }

    fun updateRouteSummaryUI() {
        if (isDirectionsRoute) {
            routeTransportationMode.visibility = View.VISIBLE
            routeTransportationCar.backgroundTintList =
                ContextCompat.getColorStateList(this@MapActivity, R.color.white)
            routeTransportationPedestrian.backgroundTintList =
                ContextCompat.getColorStateList(
                    this@MapActivity,
                    com.mapbox.services.android.navigation.R.color.md_grey_700
                )
            routeSimulationBtn.visibility = View.VISIBLE
            startNavigationBtn.visibility = View.VISIBLE
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateRouteUi(selectedRoute: DirectionsRoute) {
        this@MapActivity.route = selectedRoute

        // Format jarak (meter dan kilometer)
        val km = (selectedRoute.distance() / 1000).toInt()
        val m = (selectedRoute.distance()).toInt()

        val kilometersFormat = getString(R.string.route_kilometers)
        val metersFormat = getString(R.string.route_meters)

        // Format waktu
        val minutes = (selectedRoute.duration() / 60 % 60).toInt()
        val hours = (selectedRoute.duration() / 60 / 60).toInt()
        val days = (selectedRoute.duration() / 60 / 60 / 24).toInt()

        val minutesFormat = getString(R.string.route_durations_minutes)
        val hoursFormat = getString(R.string.route_durations_hours)
        val daysFormat = getString(R.string.route_durations_days)

        // Set teks durasi
        if (hours > 24) {
            routeDuration.text =
                "${days}${daysFormat}${(hours % 24)}${hoursFormat} ${minutes}${minutesFormat}"
        } else {
            routeDuration.text = "${hours}${hoursFormat}${minutes}${minutesFormat}"
        }

        // Set teks jarak
        if (m > 1000) {
            routeDistance.text = "${km} ${kilometersFormat}"
        } else {
            routeDistance.text =
                "${(selectedRoute.distance()).toInt()} ${metersFormat}" // Lebih akurat untuk meter asli
        }

        val orientation = resources.configuration.orientation
        when (orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                if (isDirectionsRoute) {
                    padding = intArrayOf((directionsRouteLayout.height+78), 78, 78, 78)
                }
            }
            Configuration.ORIENTATION_PORTRAIT -> {
                if (isDirectionsRoute) {
                    padding = intArrayOf(78, (directionsRouteLayout.height+67), 78, (directionsRouteInfoLayout.height+67))
                }
            } else -> {

            }
        }

        // Gerakkan kamera fokus ke rute terpilih
        getRouteBounds(selectedRoute)?.let { routeBounds ->
            mapboxMap.getCameraForLatLngBounds(routeBounds, padding)?.let { cameraPosition ->
                mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 1500)
            }
        }
    }

    fun getRouteBounds(route: DirectionsRoute): LatLngBounds? {
        // Decode the polyline geometry string from the route
        val geometry = route.geometry() ?: return null
        val lineString =
            LineString.fromPolyline(geometry, 6) // Ensure matching precision (typically 5 or 6)
        val coordinates = lineString.coordinates()

        if (coordinates.isEmpty()) return null

        val boundsBuilder = LatLngBounds.Builder()

        // Add all route coordinates to the bounding box builder
        for (point in coordinates) {
            boundsBuilder.include(LatLng(point.latitude(), point.longitude()))
        }

        return boundsBuilder.build()
    }

    fun startSimulationRoute() {
        routeSummaryLayout.visibility = View.GONE
        navigationTurnByTurnUI.visibility = View.VISIBLE

        val currentDemoLocationEngine = demoLocationEngine

        if (currentDemoLocationEngine != null) {
            route = navigationViewModel.currentRoute.value!!
            navigation.locationEngine = demoLocationEngine

            currentDemoLocationEngine.let {
                navigation.let { navigation ->
                    it.updateSpeed(demoLocationSpeed)
                    it.assign(route)
                    navigation.locationEngine = it
                    navigation.startNavigation(route)
                    navigation.addProgressChangeListener(navigationProgressChangeListener)
                }
            }

            Log.d("Navigation", "rute simulasi navigasi berhasil dipulihkan")
        }

        onBackPressedDispatcher.addCallback(backToStartMapView)
    }

    fun startNavigationRoute() {
        routeSummaryLayout.visibility = View.GONE
        navigationTurnByTurnUI.visibility = View.VISIBLE

        val currentLocationEngine = navigationLocationEngine

        if (currentLocationEngine != null) {
            route = navigationViewModel.currentRoute.value!!
            navigation.locationEngine = currentLocationEngine

            currentLocationEngine.let { locationEngine ->
                navigation.let {
                    it.locationEngine = locationEngine
                    it.startNavigation(route)
                    it.addProgressChangeListener(navigationProgressChangeListener)
                }
            }

            Log.d("Navigation", "rute navigasi berhasil dipulihkan")
        }

        onBackPressedDispatcher.addCallback(backToStartMapView)
    }

    @SuppressLint("NewApi", "SetTextI18n")
    var navigationProgressChangeListener = ProgressChangeListener { location, routeProgress ->
        lastNavigationLocation = location ?: return@ProgressChangeListener
        navigationViewModel.updateLocation(lastNavigationLocation!!)

        // Inisialisasi sekali di onCreate()
        val maneuverBannerManager = ManeuverBannerManager(
            context = this, // ID LinearLayout utama
            iconManeuver = findViewById(R.id.icon_maneuver_directions),
            tvDistance = findViewById(R.id.maneuver_directions_distance),
            tvStreetName = findViewById(R.id.maneuver_directions_streetname)             // ID opsional dari bottom bar
        )

        // Panggil di dalam listener onProgressChange
        runOnUiThread {
            if (isNavigationTurnByTurnMode && routeProgress != null) {
                navigationTurnByTurnCardView.visibility = View.VISIBLE
                maneuverBannerManager.updateManeuver(routeProgress)
            } else {
                navigationTurnByTurnCardView.visibility = View.GONE
            }
        }

        if (::navigation.isInitialized) {

            if (isNavigationTurnByTurnMode) {
                val m = getString(R.string.route_meters)
                val km = getString(R.string.route_kilometers)

                val meters = (routeProgress!!.distanceRemaining()).toInt()
                val kilometers = (routeProgress.distanceRemaining() / 1000).toInt()

                // Format waktu
                val minutes = (routeProgress.durationRemaining() / 60 % 60).toInt()
                val minutesToHours = (routeProgress.durationRemaining() / 60).toInt()
                val hours = (routeProgress.durationRemaining() / 60 / 60).toInt()
                val days = (routeProgress.durationRemaining() / 60 / 60 / 24).toInt()

                val minutesFormat = getString(R.string.directions_durations_minutes)
                val hoursFormat = getString(R.string.directions_durations_hours)
                val daysFormat = getString(R.string.directions_durations_days)

                if (meters > 1000) {
                    directionsDistance.text = "${kilometers} ${km}"
                } else {
                    directionsDistance.text = "${meters} ${m}"
                }

                if (hours > 24) {
                    directionsTime.text =
                        "${days}${daysFormat}${(hours % 24)}${hoursFormat}${minutes}${minutesFormat}"
                } else {
                    directionsTime.text = "${hours}${hoursFormat}${minutes}${minutesFormat}"
                }

                if (minutesToHours > 60) {
                    directionsTime.text = "${hours}${hoursFormat}${minutes}${minutesFormat}"
                } else {
                    directionsTime.text = "${minutes}${minutesFormat}"
                }

                val formatter = DateTimeFormatter.ofPattern("HH:mm")

                val arrivalTime = LocalDateTime.now().plusMinutes(minutesToHours.toLong())
                val timeNow = arrivalTime.format(formatter)
                directionsEta.text = "${timeNow}"

                runOnUiThread {
                    navigationlocation = navigationViewModel.currentLocation.value
                    locationSmoothAnimator?.animateToNewLocation(navigationlocation!!)
                    routeNavigationCurrentSpeed.text =
                        "${(navigationlocation!!.speed * 3.6).toInt()}      km/h"
                }

                val legProgress = routeProgress.currentLegProgress() ?: return@ProgressChangeListener
                val stepProgress = legProgress.currentStepProgress() ?: return@ProgressChangeListener

                val distanceRemaining = stepProgress.distanceRemaining()
                val stepIndex = legProgress.stepIndex()

                // 1. Ambil data tipe belokan (modifier) langsung dari rute OSRM
                val nextStep = routeProgress.currentLegProgress().upComingStep()
                val currentStep = routeProgress.currentLegProgress().currentStep()

                val targetStep = nextStep

                val modifier = targetStep?.maneuver()?.modifier() // contoh: "right", "left"
                val type = targetStep?.maneuver()?.type()         // contoh: "arrive", "depart"

                // 2. Terjemahkan arah belokan ke Bahasa Indonesia sederhana
                val arahBelokan = when (modifier) {
                    "uturn" -> "putar balik"
                    "right" -> "belok kanan"
                    "left" -> "belok kiri"
                    "slight right" -> "belokan sedikit ke kanan"
                    "slight left" -> "belokan sedikit ke kiri"
                    "sharp right" -> "belok tajam ke kanan"
                    "sharp left" -> "belok tajam ke kiri"
                    "straight" -> "lurus terus"
                    else -> ""
                }

                val jenisBelokan = when (type) {
                    "arrive" -> "anda telah sampai di tujuan"
                    "turn" -> arahBelokan
                    "continue" -> "terus berkendara sejauh"
                    "fork" -> "ambil $arahBelokan"
                    "merge" -> "bergabung ke jalur $arahBelokan"
                    "rotary" -> "di bundaran besar"
                    "exit rotary" -> "keluar dari bundaran besar"
                    "roundabout" -> "di bundaran"
                    "roundabout turn" -> "di bundaran, ambil belokan $arahBelokan"
                    "exit roundabout" -> "keluar dari bundaran"
                    "on ramp" -> "masuk ke jalan tol $arahBelokan"
                    "off ramp" -> "keluar dari jalan tol $arahBelokan"
                    else -> arahBelokan.ifEmpty { "ikuti petunjuk arah" }
                }

                // 3. Ambil nama jalan yang tertera di papan belokan XML saat ini
                val namaJalanDiPapan = targetStep?.name() ?: ""

                // 4. Rangkai kalimat petunjuk suara berdasarkan isi papan belokan visual + JARAK ADAPTIF
                var announcement = ""

                // Cetak Logcat untuk melihat apakah teks OSRM Anda benar-benar ada atau null
                Log.d("NAV_DEBUG", "Step Ke: $stepIndex | Jarak Tersisa: $distanceRemaining")

                val roundedDistance = if (distanceRemaining >= 1000) {
                        // Jika di atas 1 KM, bulatkan ke ratusan meter terdekat (misal 1250m jadi 1.2 km / 1200m)
                    (Math.round(distanceRemaining / 100.0) * 100).toInt()
                } else {
                    // Jika di bawah 1 KM, bulatkan ke puluhan meter terdekat
                    (Math.round(distanceRemaining / 10.0) * 10).toInt()
                }

                // Format penyebutan teks jarak agar natural di telinga (Mata / Kilometer)
                val teksJarak = if (roundedDistance >= 1000) {
                    val kmValue = roundedDistance / 1000.0
                    "Dalam $kmValue kilometer, "
                } else if (roundedDistance > 10) {
                    "Dalam $roundedDistance meter, "
                } else {
                    ""
                }

                if (type == "arrive") {
                    announcement = "$teksJarak anda akan sampai di tujuan"
                } else if (type == "depart") {
                    announcement = "Mulai perjalanan, ikuti petunjuk arah"
                } else {
                    announcement = if (namaJalanDiPapan.isNotEmpty() && namaJalanDiPapan != "null") {
                        "$teksJarak$jenisBelokan di $namaJalanDiPapan"
                    } else {
                        "$teksJarak$jenisBelokan"
                    }
                }

                // 5. PEMICU SUARA ADAPTIF & DINAMIS (Berdasarkan Kecepatan Nyata Kendaraan)
                if (announcement.isNotEmpty()) {

                    // Ambil kecepatan saat ini dalam m/s (meter per detik) dari objek location Anda
                    // Jika location null, berikan default kecepatan kota standar 30 km/jam (~8.3 m/s)
                    val speedMetersPerSecond = navigationlocation?.speed ?: 8.3f

                    /*
                        * Logika Waktu Reaksi (Waktu Mengemudi Menuju Belokan):
                        * Kita ingin suara berbunyi sekitar 15-20 detik sebelum belokan (Pengingat Jauh)
                        * Dan berbunyi kembali sekitar 4-6 detik sebelum belokan (Pengingat Dekat)
                        */
                    val triggerDistanceFar = (speedMetersPerSecond * 18).coerceAtLeast(100.0f) // Minimal di jarak 100 meter
                    val triggerDistanceNear = (speedMetersPerSecond * 5).coerceAtLeast(20.0f)   // Minimal di jarak 20 meter

                    // Buat pengunci bitwise atau string agar kita bisa membunyikan 2 kali dalam 1 segmen jalan (stepIndex)
                    val farTriggerKey = "${stepIndex}_FAR"
                    val nearTriggerKey = "${stepIndex}_NEAR"

                    runOnUiThread {
                        // --- PEMICU 1: PENGINGAT JAUH (Adaptif) ---
                        // Contoh: Melaju 60km/jam (~16m/s), suara akan memicu otomatis di jarak ~300 meter sebelum belokan!
                        if (distanceRemaining <= triggerDistanceFar && distanceRemaining > triggerDistanceNear + 20) {
                            // Cek apakah pengingat jauh untuk belokan ini belum pernah diucapkan
                            if (lastSpokenTriggerKey != farTriggerKey) {
                                lastSpokenTriggerKey = farTriggerKey // Kunci agar tidak berulang
                                gentleNavigationSpeaker.playGentle(announcement)
                            }
                        }

                        // --- PEMICU 2: PENGINGAT DEKAT / FINAL ---
                        // Contoh: Berbunyi tajam saat sisa waktu sisa 5 detik menjelang eksekusi belokan
                        else if (distanceRemaining <= triggerDistanceNear && distanceRemaining > 0.0) {
                            if (lastSpokenTriggerKey != nearTriggerKey) {
                                lastSpokenTriggerKey = nearTriggerKey // Kunci agar tidak berulang
                                gentleNavigationSpeaker.stopAll() // Potong paksa instruksi jauh yang barangkali masih terbaca

                                // Kalimat final tanpa meteran agar pengemudi fokus mengeksekusi setir
                                val finalAnnouncement = if (namaJalanDiPapan.isNotEmpty() && namaJalanDiPapan != "null") {
                                    "$jenisBelokan menuju $namaJalanDiPapan"
                                } else {
                                    "$jenisBelokan"
                                }
                                gentleNavigationSpeaker.playGentle(finalAnnouncement)
                            }
                        }
                    }
                }

                navigation.addOffRouteListener(offRouteListener)
            }
        }
    }

    private val offRouteListener = object: OffRouteListener {
        override fun userOffRoute(location: Location?) {
            navigationlocation = location ?: return

            if (::navigation.isInitialized) {
                
                if (isNavigationTurnByTurnMode) {
                    // Fungsi ini OTOMATIS dipicu oleh Core Engine saat GPS melenceng dari garis rute
                    originLat = navigationlocation!!.latitude
                    originLng = navigationlocation!!.longitude

                    osrmBaseUrl =
                        "http://router.project-osrm.org/" // Ganti dengan URL server OSRM Anda
                    user = "route/v1/"
                    vehicle = "driving/"
                    url =
                        "${osrmBaseUrl}${user}${vehicle}${originLng},${originLat};${destinationLng},${destinationLat}?steps=true&alternatives=true&geometries=polyline6&overview=full&annotations=duration,distance"

                    fetchOsrmRoute(url) { responseRoutes ->
                        if (responseRoutes != null && responseRoutes.routes().isNotEmpty()) {

                            routeList = responseRoutes.routes()
                            route = routeList[currentRouteIndex]

                            runOnUiThread {

                                navigation.stopNavigation() // Hentikan pelacakan rute lama
                                navigationViewModel.clearRoute()
                                navigationViewModel.clearRouteAlt()
                                navigationViewModel.clearNavigationData()
                                navigationViewModel.setRouteAlt(routeList)
                                navigationViewModel.setRoute(route)
                                routeRestoredList.clear()
                                navigation.startNavigation(route) // Mulai ulang pelacakan dengan rute baru (Kunci Snap Berfungsi Lagi)
                                navigationViewModel.setNavigationData(navigation)

                                // Gambar rute baru di peta buatan Anda
                                navigationMapRoute?.addRoute(route)

                            }
                        }
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        // Simpan data pencarian saat layar mulai berotasi
        if (savedPlaceList != null) {
            outState.putParcelableArrayList("SAVED_PLACES", savedPlaceList)
        }

        if (::routeList.isInitialized && routeList != null) {
            val jsonArray = org.json.JSONArray()

            for (route in routeList) {
                jsonArray.put(route.toJson())
            }

            outState.putString("SAVED_ROUTES", jsonArray.toString())
        }

        outState.putDouble(KEY_PLACE_LAT, lat)
        outState.putDouble(KEY_PLACE_LON, lon)
        outState.putString(KEY_PLACE_NAME, name)
        outState.putString(KEY_PLACE_DISPLAY_NAME, displayName)

        outState.putDouble(KEY_ROUTE_START_LAT, originLat)
        outState.putDouble(KEY_ROUTE_START_LON, originLng)
        outState.putDouble(KEY_ROUTE_END_LAT, destinationLat)
        outState.putDouble(KEY_ROUTE_END_LON, destinationLng)

        outState.putString(KEY_ROUTE_DISTANCE, routeDistance.text.toString())
        outState.putString(KEY_ROUTE_DURATION, routeDuration.text.toString())

        outState.putString(KEY_ROUTE_BASE_URL, osrmBaseUrl)
        outState.putString(KEY_ROUTE_TRANSPORTATION, vehicle)
        outState.putString(KEY_ROUTE_URL, url)

        // Simpan nilai isLocationSelected yang sudah disesuaikan (bisa true atau false)
        outState.putBoolean(KEY_SEARCH_UI_STATUS, isSearchUIVisible)
        outState.putBoolean(KEY_HAS_SELECTION, isLocationSelected)
        outState.putBoolean(KEY_ROUTE_SUMMARY, isDirectionsRoute)

        // 1. Simpan status UI & Variabel menggunakan putBoolean
        outState.putBoolean("KEY_NAV_STARTED", isNavigating)
        outState.putBoolean("KEY_IS_TBT_MODE", isNavigationTurnByTurnMode)
        outState.putBoolean("KEY_REALTIME_LOC", realTimeLocationEnabled)
        outState.putBoolean("KEY_SIMULATION_MODE", isSimulationMode)

        // 2. Simpan indeks rute jika ada
        outState.putInt("KEY_CURRENT_ROUTE_INDEX", currentRouteIndex)

        if (isNavigationTurnByTurnMode) {

            val orientation = resources.configuration.orientation
            when (orientation) {
                Configuration.ORIENTATION_LANDSCAPE -> {
                    navigationlocation.let {
                        outState.putDouble("NAVIGATION_LOC_LNG", it!!.longitude)
                        outState.putDouble("NAVIGATION_LOC_LAT", it.latitude)
                        outState.putFloat("NAVIGATION_LOC_BEARING", it.bearing)
                        outState.putFloat("NAVIGATION_LOC_SPEED", it.speed)
                    }
                }
                Configuration.ORIENTATION_PORTRAIT -> {
                    navigationlocation.let {
                        outState.putDouble("NAVIGATION_LOC_LNG", it!!.longitude)
                        outState.putDouble("NAVIGATION_LOC_LAT", it.latitude)
                        outState.putFloat("NAVIGATION_LOC_BEARING", it.bearing)
                        outState.putFloat("NAVIGATION_LOC_SPEED", it.speed)
                    }
                }
            }
        }

        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        // Ambil kembali data setelah rotasi selesai tanpa harus request ke internet lagi
        restoredList =
            savedInstanceState.getParcelableArrayList("SAVED_PLACES")

        if (restoredList != null) {
            savedPlaceList = restoredList
            adapter = PlaceAdapter(this, restoredList!!)
            listView.adapter = adapter
            listView.visibility = View.VISIBLE
        }

        if (isSearchUIVisible) {
            SearchLayoutUI(searchLocLayout, searchLocation, mapView, mapButtonUI).clickSearch()

            onBackPressedDispatcher.addCallback(backToMap)
            removeCloseThisNavMapsApp()
        }

        super.onRestoreInstanceState(savedInstanceState)
    }

    private var closeThisNavMapsApp = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            MaterialAlertDialogBuilder(this@MapActivity)
                .setTitle(R.string.exit_navigation_app_title)
                .setPositiveButton(R.string.exit_navigation_app_yes) { _, _ ->
                    finish()
                }
                .setNegativeButton(R.string.exit_navigation_app_no, null)
                .show()
        }
    }

    fun removeCloseThisNavMapsApp() {
        closeThisNavMapsApp.remove()
    }

    internal val backToMap = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            SearchLayoutUI(searchLocLayout, searchLocation, mapView, mapButtonUI).backToMap()
            isSearchUIVisible = false
            val inputMethodManager =
                getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(searchLocation.windowToken, 0)

            removeBackToMap()
            onBackPressedDispatcher.addCallback(closeThisNavMapsApp)
        }
    }

    internal val backToSearchMap = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            DataSearchUI(
                marker,
                searchLocLayout,
                locationInfoLayout,
                mapView,
                textLocationName,
                textLocationAddress,
                textLocationCoordinates,
                backToSearchMapBtn
            ).backToSearchLoc()
            isLocationSelected = false

            if (::marker.isInitialized && marker != null) {
                mapboxMap.removeMarker(marker)
            }

            removeBackToSearchMap()
        }
    }

    internal val backToLocationOnMapInfo = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            mapboxMap.setPadding(0, 0, 0, 0)

            navigationMapRoute?.removeRoute()
            mapboxMap.removeMarker(marker)
            routeRestoredList.clear()

            routeTransportationCar.backgroundTintList = ContextCompat.getColorStateList(
                this@MapActivity,
                com.mapbox.services.android.navigation.ui.v5.R.color.md_grey_700
            )
            routeTransportationPedestrian.backgroundTintList = ContextCompat.getColorStateList(
                this@MapActivity,
                com.mapbox.services.android.navigation.ui.v5.R.color.md_grey_700
            )

            RouteSummaryUI(
                locationInfoLayout,
                backToSearchMapBtn,
                routeSummaryLayout,
                routeTransportationMode,
                startNavigationBtn,
                routeDistance,
                routeDuration,
                directionsStart,
                directionsEnd
            ).cancelDirections()

            onLocationResultSelected()

            removeBackToLocationOnMapInfo()

            isDirectionsRoute = false
        }
    }

    internal val backToStartMapView = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            MaterialAlertDialogBuilder(this@MapActivity)
                .setTitle(R.string.exit_navigation_route_title)
                .setPositiveButton(R.string.exit_navigation_app_yes) { _, _ ->
                    cancelNavigationRoute()
                }
                .setNegativeButton(R.string.exit_navigation_app_no, null)
                .show()
        }
    }

    fun cancelNavigationRoute() {
        mapboxMap.setPadding(0, 0, 0, 0)
        toggleLocationSource(false)
        isSimulationMode = false
        navigation.removeOffRouteListener(offRouteListener)
        navigationMapRoute?.removeRoute()
        navigationViewModel.clearRoute()
        navigationViewModel.clearRouteAlt()
        routeRestoredList.clear()
        mapboxMap.removeMarker(marker)
        navigation.stopNavigation()
        navigationViewModel.clearNavigationData()
        navigationViewModel.clearLocationData()

        isNavigationTurnByTurnMode = false

        realTimeLocationEnabled = true
        isDirectionsRoute = false
        isLocationSelected = false
        isSearchUIVisible = false
        adapter.clear()
        searchLocation.text?.clear()
        NavigationTurnByTurnUI(
            navigationTurnByTurnUI,
            mapButtonUI,
            directionsTime,
            directionsDistance,
            directionsEta,
            startNavigationBtn,
            routeDistance,
            routeDuration,
            directionsStart,
            directionsEnd,
            routeTransportationMode,
            routeTransportationCar,
            routeTransportationPedestrian
        ).stopNavigationRoute()

        routeTransportationCar.backgroundTintList =
            ContextCompat.getColorStateList(this@MapActivity, R.color.white)
        routeTransportationPedestrian.backgroundTintList =
            ContextCompat.getColorStateList(
                this@MapActivity,
                com.mapbox.services.android.navigation.R.color.md_grey_700
            )

        removeBackToRouteSummary()
        removeBackToLocationOnMapInfo()
        removeBackToSearchMap()
        removeBackToMap()

        if (isDataVisible) {
            saveStatus(true)
        } else {
            saveStatus(false)
        }

        onBackPressedDispatcher.addCallback(closeThisNavMapsApp)
    }

    fun removeBackToSearchMap() {
        backToSearchMap.remove()
    }

    private fun removeBackToMap() {
        backToMap.remove()
    }

    private fun removeBackToLocationOnMapInfo() {
        backToLocationOnMapInfo.remove()
    }

    private fun removeBackToRouteSummary() {
        backToStartMapView.remove()
    }

    fun clearLocalData() {
        val sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.remove(KEY_SEARCH_UI_STATUS)
        editor.apply()
    }

    override fun onStart() {
        super.onStart();
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume();
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause();
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop();
        mapView.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy();
        mapView.onDestroy()
        navigation.onDestroy()
    }
}