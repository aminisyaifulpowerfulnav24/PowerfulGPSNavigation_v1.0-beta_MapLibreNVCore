package id.powerfulgpsnavigation.satnav

val LIGHT_STYLE = "asset://maps/style/maptiler_streets-v4_style.json"
val DARK_STYLE = "asset://maps/style/maptiler_streets-v4-dark_style.json"

// Fungsi untuk mengecek apakah saat ini malam hari berdasarkan jam
fun isNightTime(): Boolean {
    val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return currentHour >= 18 || currentHour < 6
}