package id.powerfulgpsnavigation.satnav

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ManeuverBannerManager(
    private val context: Context,
    private val iconManeuver: ImageView?,
    private val tvDistance: TextView?,
    private val tvStreetName: TextView?
) {

    @RequiresApi(Build.VERSION_CODES.O)
    private val formatterHHmm = DateTimeFormatter.ofPattern("HH:mm")

    /**
     * Update Tampilan UI Maneuver Turn-by-Turn berdasarkan RouteProgress
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun updateManeuver(routeProgress: RouteProgress) {
        val currentLegProgress = routeProgress.currentLegProgress() ?: return
        val currentStep = currentLegProgress.upComingStep() ?: return

        // 1. Update Jarak ke Maneuver Berikutnya (misal: "200 m" atau "1.2 km")
        val stepMeters = currentLegProgress.currentStepProgress()?.distanceRemaining()?.toInt() ?: 0
        tvDistance?.text = formatDistance(stepMeters)

        // 2. Update Teks Instruksi / Nama Jalan (maneuver_directions_streetname)
        val instructionText = currentStep.name()
        tvStreetName?.text = instructionText

        // 3. Update Ikon Maneuver (icon_maneuver_directions)
        val primaryInstruction = currentStep.bannerInstructions()?.firstOrNull()?.primary()
        val type = primaryInstruction?.type() ?: currentStep.maneuver().type() ?: ""
        val modifier = primaryInstruction?.modifier() ?: currentStep.maneuver().modifier()

        updateManeuverIcon(type, modifier)
    }

    /**
     * Format Jarak (Meter / Kilometer)
     */
    @SuppressLint("DefaultLocale")
    private fun formatDistance(meters: Int): String {
        return if (meters >= 1000) {
            val km = meters / 1000.0
            String.format("%.1f km", km)
        } else {
            "$meters m"
        }
    }

    /**
     * Format Durasi Sisa Waktu Rute
     */
    private fun formatDuration(durationSeconds: Double): String {
        val totalMinutes = (durationSeconds / 60).toInt()
        val days = totalMinutes / (24 * 60)
        val hours = (totalMinutes % (24 * 60)) / 60
        val minutes = totalMinutes % 60

        return when {
            days > 0 -> "${days}h ${hours}j ${minutes}m"
            hours > 0 -> "${hours}j ${minutes}m"
            else -> "${minutes}m"
        }
    }

    /**
     * Kalkulasi Waktu Tiba (ETA)
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun calculateEta(durationSeconds: Double): String {
        val minutesToTrip = (durationSeconds / 60).toLong()
        val arrivalTime = LocalDateTime.now().plusMinutes(minutesToTrip)
        return arrivalTime.format(formatterHHmm)
    }

    /**
     * Pemetaan Ikon Maneuver otomatis sesuai nama file drawable
     */
    private fun updateManeuverIcon(type: String, modifier: String?) {
        if (iconManeuver == null) return

        val iconName = buildString {
            append("direction_")
            append(type.replace(" ", "_"))
            if (!modifier.isNullOrEmpty()) {
                append("_")
                append(modifier.replace(" ", "_"))
            }
        }.lowercase()

        // Ambil resource ID dari nama file drawable (misal: direction_turn_right)
        val resourceId = context.resources.getIdentifier(iconName, "drawable", context.packageName)
        if (resourceId != 0) {
            iconManeuver.setImageResource(resourceId)
        } else {
            // Fallback jika drawable spesifik tidak ditemukan
            iconManeuver.setImageResource(com.mapbox.services.android.navigation.R.drawable.direction_continue_straight)
        }
    }
}

/* ========================================================================= */
/* EXTENSION FUNCTIONS (Helper untuk parsing Mapbox/OSRM Navigation v2.0.0+) */
/* ========================================================================= */

fun BannerInstructions.getFormattedText(): String {
    val primary = primary() ?: return ""

    if (!primary.text().isNullOrBlank()) {
        return primary.text()!!
    }

    val components = primary.components()
    if (!components.isNullOrEmpty()) {
        return components.joinToString(" ") { component ->
            component.text() ?: ""
        }.trim()
    }

    return ""
}

fun LegStep.getManeuverText(): String {
    val bannerText = bannerInstructions()
        ?.firstOrNull()
        ?.getFormattedText()

    if (!bannerText.isNullOrBlank()) {
        return bannerText
    }

    val voiceText = voiceInstructions()
        ?.firstOrNull()
        ?.announcement()

    if (!voiceText.isNullOrBlank()) {
        return voiceText
    }

    val type = maneuver().type() ?: ""
    val modifier = maneuver().modifier() ?: ""

    return when {
        modifier.isNotEmpty() -> "$type $modifier".replace("_", " ").capitalizeWords()
        type.isNotEmpty() -> type.replace("_", " ").capitalizeWords()
        else -> "Lanjutkan mengikuti rute"
    }
}

private fun String.capitalizeWords(): String =
    split(" ").joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }