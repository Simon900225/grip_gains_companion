package app.grip_gains_companion.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Manages haptic feedback
 */
class HapticManager(context: Context) {
    
    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    
    /**
     * Success feedback - used when device connects
     */
    fun success() {
        vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
    }
    
    /**
     * Warning feedback - used for grip fail and off-target alerts
     */
    fun warning() {
        // Double pulse pattern
        val timings = longArrayOf(0, 100, 50, 100)
        val amplitudes = intArrayOf(0, 200, 0, 200)
        vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
    }
    
    /**
     * Error feedback
     */
    fun error() {
        // Triple pulse pattern
        val timings = longArrayOf(0, 50, 50, 50, 50, 50)
        val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
        vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
    }
    
    /**
     * Light feedback - used for calibration complete
     */
    fun light() {
        vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
    }
    
    /**
     * Medium feedback
     */
    fun medium() {
        vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
    }
    
    private fun vibrate(effect: VibrationEffect) {
        if (vibrator.hasVibrator()) {
            vibrator.vibrate(effect)
        }
    }
}
