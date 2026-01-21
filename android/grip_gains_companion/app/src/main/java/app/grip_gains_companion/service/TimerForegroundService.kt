package app.grip_gains_companion.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import app.grip_gains_companion.MainActivity
import app.grip_gains_companion.R
import app.grip_gains_companion.config.AppConstants
import java.util.Timer
import java.util.TimerTask

/**
 * Foreground service to display timer status in notification
 * This is the Android equivalent of iOS Live Activity
 */
class TimerForegroundService : Service() {
    
    private var timer: Timer? = null
    private var elapsedAtStart: Int = 0
    private var remainingAtStart: Int = 0
    private var startTimeMs: Long = 0
    
    companion object {
        const val ACTION_START = "app.grip_gains_companion.START_TIMER"
        const val ACTION_STOP = "app.grip_gains_companion.STOP_TIMER"
        const val EXTRA_ELAPSED = "elapsed"
        const val EXTRA_REMAINING = "remaining"
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                elapsedAtStart = intent.getIntExtra(EXTRA_ELAPSED, 0)
                remainingAtStart = intent.getIntExtra(EXTRA_REMAINING, 0)
                startTimeMs = System.currentTimeMillis()
                
                startForeground(AppConstants.NOTIFICATION_ID, buildNotification(elapsedAtStart, remainingAtStart))
                startUpdateTimer()
            }
            ACTION_STOP -> {
                stopUpdateTimer()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        
        return START_NOT_STICKY
    }
    
    private fun startUpdateTimer() {
        stopUpdateTimer()
        timer = Timer().apply {
            scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    val secondsSinceStart = ((System.currentTimeMillis() - startTimeMs) / 1000).toInt()
                    val elapsed = elapsedAtStart + secondsSinceStart
                    val remaining = remainingAtStart - secondsSinceStart
                    
                    val notification = buildNotification(elapsed, remaining)
                    val notificationManager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
                    notificationManager.notify(AppConstants.NOTIFICATION_ID, notification)
                }
            }, 1000, 1000)
        }
    }
    
    private fun stopUpdateTimer() {
        timer?.cancel()
        timer = null
    }
    
    private fun buildNotification(elapsed: Int, remaining: Int): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val remainingText = if (remaining < 0) {
            "+${-remaining}s bonus"
        } else {
            "${remaining}s remaining"
        }
        
        return NotificationCompat.Builder(this, AppConstants.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Grip Timer")
            .setContentText("${elapsed}s elapsed • $remainingText")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopUpdateTimer()
    }
}
