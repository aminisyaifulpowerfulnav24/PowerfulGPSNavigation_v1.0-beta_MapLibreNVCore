package id.powerfulgpsnavigation.satnav

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mapbox.geojson.Point
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

fun MapActivity.GeocodeSearchUI() {
    searchMapButton.setOnClickListener {
        isSearchUIVisible = true
        isGeosearchUI = true
        SearchLayoutUI(searchLocLayout, searchLocation, mapView, mapButtonUI).clickSearch()

        onBackPressedDispatcher.addCallback(backToMap)
        removeCloseThisNavMapsApp()
    }

    backToMapButton.setOnClickListener {
        backToMap.handleOnBackPressed()
    }

    searchLocButton.setOnClickListener {
        val searchText = searchLocation.text.toString()
        val urlString = "https://nominatim.openstreetmap.org/search?q=$searchText&format=jsonv2"

        GlobalScope.launch(Dispatchers.IO) {
            try {
                // Fetch JSON from Nominatim URL
                val url = URL(urlString)
                val connection = url.openConnection() as java.net.HttpURLConnection

                // Change "MyAndroidMapApp" to your own project or app name
                connection.setRequestProperty(
                    "User-Agent",
                    "PowerfulGPSNavigationBETAAppForAndroid/1.0"
                )

                // 3. Read response string from the connection stream
                val json = connection.inputStream.bufferedReader().use { it.readText() }

                // Parse JSON using Gson
                val listType = object : TypeToken<ArrayList<GeocodeResponse>>() {}.type
                val placeList: ArrayList<GeocodeResponse> = Gson().fromJson(json, listType)

                savedPlaceList = placeList

                // Update UI on the Main Thread
                withContext(Dispatchers.Main) {
                    adapter = PlaceAdapter(this@GeocodeSearchUI, placeList)
                    listView.adapter = adapter
                }

                listView.visibility = View.VISIBLE
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@GeocodeSearchUI, "Error fetching data", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    searchLocation.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            if (s.toString().isEmpty()) {
                // Solusi aman ganti adapter kosong untuk membersihkan ListView tanpa ClassCastException
                savedPlaceList = null
                adapter = PlaceAdapter(this@GeocodeSearchUI, arrayListOf())
                listView.adapter = adapter
                listView.visibility = View.INVISIBLE
            }
        }

        override fun beforeTextChanged(
            s: CharSequence?,
            start: Int,
            count: Int,
            after: Int
        ) {

        }

        override fun onTextChanged(
            s: CharSequence?,
            start: Int,
            before: Int,
            count: Int
        ) {

        }

    })

    listView.setOnItemClickListener { parent, view, position, id ->

        isGeosearchUI = false
        isLocationSelected = true
        isLocationSelectedUI = true
        locationSelectedOnMapUIStatus = true

        saveStatus(false)
        removeCenteringPosition()
        onMovingMap()

        selectedLocation = adapter.getItem(position)!!
        name = selectedLocation.name
        displayName = selectedLocation.display_name
        lat = selectedLocation.lat.toDouble()
        lon = selectedLocation.lon.toDouble()

        onLocationResultSelected()

        val inputMethodManager =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(searchLocation.windowToken, 0)
    }

    backToSearchMapBtn.setOnClickListener {
        backToSearchMap.handleOnBackPressed()
    }
}