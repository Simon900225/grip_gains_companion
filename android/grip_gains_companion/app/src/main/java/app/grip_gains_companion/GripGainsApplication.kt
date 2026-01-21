package app.grip_gains_companion

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import app.grip_gains_companion.config.AppConstants

class GripGainsApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            AppConstants.NOTIFICATION_CHANNEL_ID,
            "Grip Timer",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows timer status during grip training"
            setShowBadge(false)
        }
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}
