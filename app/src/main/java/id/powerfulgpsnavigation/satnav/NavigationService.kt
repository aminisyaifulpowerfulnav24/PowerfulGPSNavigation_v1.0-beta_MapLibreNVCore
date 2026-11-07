package id.powerfulgpsnavigation.satnav

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class NavigationService : Service() {

    private val CHANNEL_ID = "navigation_channel"
    private val NOTIFICATION_ID = 1001

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.navigation_service_active_title))
            .setContentText(getString(R.string.navigation_service_active_text))
            .setSmallIcon(com.mapbox.services.android.navigation.R.drawable.ic_navigation)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.navigation_service_active_title),
                NotificationManager.IMPORTANCE_HIGH // Wajib HIGH agar prioritas tidak diturunkan
            ).apply {
                description = getString(R.string.navigation_service_active_description)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onBind(p0: Intent?): IBinder? = null
}