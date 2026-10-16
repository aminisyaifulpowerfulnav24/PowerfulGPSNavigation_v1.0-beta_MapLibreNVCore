package id.powerfulgpsnavigation.satnav

import android.os.Parcel
import android.os.Parcelable
import kotlinx.serialization.Serializable

data class GeocodeResponse(
    val lat: String,
    val lon: String,
    val display_name: String,
    val name: String
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: ""
    )

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(name)
        dest.writeString(display_name)
        dest.writeString(lat)
        dest.writeString(lon)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<GeocodeResponse> {
        override fun createFromParcel(parcel: Parcel): GeocodeResponse = GeocodeResponse(parcel)
        override fun newArray(size: Int): Array<GeocodeResponse?> = arrayOfNulls(size)
    }
}