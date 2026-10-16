package id.powerfulgpsnavigation.satnav

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class PlaceAdapter(context: Context, places: ArrayList<GeocodeResponse>) :
    ArrayAdapter<GeocodeResponse>(context, 0, places) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_searchloc_results, parent, false)

        val place = getItem(position)

            val tvAddress = view.findViewById<TextView>(R.id.text_location_address)
            val tvName = view.findViewById<TextView>(R.id.text_location_name)
            val tvCoords = view.findViewById<TextView>(R.id.text_location_coordinates)

            tvAddress.text = place?.display_name
            tvName.text = place?.name
            tvCoords.text = "${place?.lat}, ${place?.lon}"

        return view
    }
}
