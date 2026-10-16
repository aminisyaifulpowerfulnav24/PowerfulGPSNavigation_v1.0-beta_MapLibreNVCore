package id.powerfulgpsnavigation.satnav

import android.text.SpannableStringBuilder
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration

class RecenterPositionButtonUI(
    private var recenterPositionButton: FloatingActionButton,
) {
    fun recenterPosition() {
        recenterPositionButton.visibility = View.INVISIBLE
    }

    fun unrecenterPosition() {
        recenterPositionButton.visibility = View.VISIBLE
    }
}

class SearchLayoutUI(
    private var searchLocLayout: View,
    private var searchLocation: AppCompatEditText,
    private var mapView: MapView,
    private var mapButtonUI: ConstraintLayout
){
    fun clickSearch () {
        searchLocLayout.visibility = View.VISIBLE
        searchLocation.requestFocus()
        mapView.visibility = View.INVISIBLE
        mapButtonUI.visibility = View.INVISIBLE
    }

    fun backToMap () {
        searchLocLayout.visibility = View.GONE
        searchLocation.clearFocus()
        searchLocation.text!!.clear()
        mapView.visibility = View.VISIBLE
        mapButtonUI.visibility = View.VISIBLE
    }
}

class DataSearchUI(
    private var marker: Marker,
    private var searchLocLayout: View,
    private var locationInfoLayout: View,
    private var mapView: MapView,
    private var textLocationName: TextView,
    private var textLocationAddress: TextView,
    private var textLocationCoordinates: TextView,
    private var backToSearchMapBtn: FloatingActionButton
    ) {
    fun backToSearchLoc() {
        mapView.visibility = View.INVISIBLE
        locationInfoLayout.visibility = View.INVISIBLE
        backToSearchMapBtn.visibility = View.INVISIBLE
        searchLocLayout.visibility = View.VISIBLE

        marker.remove()

        textLocationName.text = ""
        textLocationAddress.text = ""
        textLocationCoordinates.text = ""
    }
}

class DataMarkerUI(
    private var searchLocLayout: View,
    private var mapView: MapView,
    private var locationInfoLayout: View,
    private var mapboxMap: MapboxMap,
    private var marker: Marker,
    private var markerOptions: MarkerOptions,
    private var markerIcon: com.mapbox.mapboxsdk.annotations.Icon,
    private var markerIconFactory: IconFactory,
    private var lat: Double,
    private var lon: Double
) {
    fun addMarker() {
        searchLocLayout.visibility = View.GONE
        mapView.visibility = View.VISIBLE
        locationInfoLayout.visibility = View.VISIBLE
    }
}

class RouteSummaryUI(
    private var locationInfoLayout: View,
    private var backToSearchMapBtn: FloatingActionButton,
    private var routeSummaryLayout: View,
    private var routeTransportationMode: CardView,
    private var startNavigationBtn: ExtendedFloatingActionButton,
    private var routeDistance: AppCompatTextView,
    private var routeDuration: AppCompatTextView,
    private var directionsStart: AppCompatEditText,
    private var directionsEnd: AppCompatEditText
) {
    fun getDirections() {
        locationInfoLayout.visibility = View.GONE
        backToSearchMapBtn.visibility = View.INVISIBLE
        routeSummaryLayout.visibility = View.VISIBLE
    }
    fun cancelDirections() {
        locationInfoLayout.visibility = View.VISIBLE
        backToSearchMapBtn.visibility = View.VISIBLE
        routeSummaryLayout.visibility = View.GONE
        routeTransportationMode.visibility = View.INVISIBLE
        startNavigationBtn.visibility = View.INVISIBLE
        directionsStart.text = SpannableStringBuilder("")
        directionsEnd.text = SpannableStringBuilder("")
        routeDistance.text = ""
        routeDuration.text = ""
    }
}

class NavigationTurnByTurnUI(
    private var navigationTurnByTurnUI: View,
    private var mapButtonUI: ConstraintLayout,
    private var directionsTime: TextView,
    private var directionsDistance: TextView,
    private var directionsEta: TextView,
    private var startNavigationBtn: ExtendedFloatingActionButton,
    private var routeDistance: AppCompatTextView,
    private var routeDuration: AppCompatTextView,
    private var directionsStart: AppCompatEditText,
    private var directionsEnd: AppCompatEditText,
    private var routeTransportationMode: CardView,
    private var routeTransportationCar: AppCompatImageButton,
    private var routeTransportationPedestrian: AppCompatImageButton
) {
    fun stopNavigationRoute(){
        navigationTurnByTurnUI.visibility = View.GONE
        mapButtonUI.visibility = View.VISIBLE
        directionsTime.text = ""
        directionsDistance.text = ""
        directionsEta.text = ""
        startNavigationBtn.visibility = View.INVISIBLE
        routeDistance.text = ""
        routeDuration.text = ""
        directionsStart.text = SpannableStringBuilder("")
        directionsEnd.text = SpannableStringBuilder("")
        routeTransportationMode.visibility = View.INVISIBLE
    }
}

