package app.grip_gains_companion.service.ble

import android.bluetooth.le.ScanResult
import android.os.Handler
import android.os.Looper
import android.util.Log
import app.grip_gains_companion.config.AppConstants
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Protocol handler for Weiheng WH-C06 hanging scale
 * Uses advertisement-based protocol (no GATT connection)
 */
class WHC06Service {

    companion object {
        private const val TAG = "WHC06Service"
        private const val DISCONNECT_TIMEOUT_MS = 5000L  // 5 seconds without data = disconnected
    }

    /** Callback for force samples (weight in kg, timestamp in microseconds) */
    var onForceSample: ((Double, Long) -> Unit)? = null

    /** Callback when device stops advertising (disconnect) */
    var onDisconnect: (() -> Unit)? = null

    private var baseTimestamp: Long = 0
    private var sampleCounter: Long = 0
    private var disconnectTimer: Runnable? = null
    private val handler = Handler(Looper.getMainLooper())

    /**
     * Start the service
     */
    fun start() {
        Log.i(TAG, "Starting WHC06 service...")
        baseTimestamp = System.currentTimeMillis() * 1000  // Convert to microseconds
        sampleCounter = 0
        resetDisconnectTimer()
    }

    /**
     * Stop the service
     */
    fun stop() {
        Log.i(TAG, "Stopping WHC06 service...")
        cancelDisconnectTimer()
    }

    /**
     * Process advertisement data from WHC06
     * Called each time we receive an advertisement from the device
     */
    fun processAdvertisement(scanResult: ScanResult) {
        val manufacturerData = scanResult.scanRecord?.getManufacturerSpecificData(AppConstants.WHC06_MANUFACTURER_ID)
        if (manufacturerData == null) {
            Log.w(TAG, "No manufacturer data in advertisement")
            return
        }

        val weight = parseManufacturerData(manufacturerData)
        if (weight == null) {
            Log.w(TAG, "Failed to parse weight from manufacturer data")
            return
        }

        // Reset disconnect timer since we received data
        resetDisconnectTimer()

        // Generate synthetic timestamp
        val timestamp = generateTimestamp()

        // Send every sample - Android advertisement rate is already slow for WHC06,
        // and we need continuous samples for calibration to complete
        onForceSample?.invoke(weight, timestamp)
    }

    /**
     * Parse manufacturer data from WHC06 advertisement
     * Format: bytes 10-11 (relative to manufacturer data start) contain weight as big-endian 16-bit signed integer, divided by 100 for kg
     * Note: Android's getManufacturerSpecificData already strips the manufacturer ID prefix,
     * so we use offset 10-11 directly (not 12-13 like in iOS where prefix is included)
     */
    private fun parseManufacturerData(data: ByteArray): Double? {
        // The offset in the spec is 10-11 within the manufacturer-specific payload
        // Android's getManufacturerSpecificData returns data AFTER the 2-byte manufacturer ID
        val weightOffset = 10  // bytes 10-11 in the payload (after manufacturer ID is stripped)
        val minSize = weightOffset + 2  // Need bytes at offset 10-11

        if (data.size < minSize) {
            Log.w(TAG, "Manufacturer data too small: ${data.size} bytes, need $minSize")
            return null
        }

        // Extract weight from bytes 10-11 (big-endian, signed Int16 for negative values after tare)
        val buffer = ByteBuffer.wrap(data, weightOffset, 2)
        buffer.order(ByteOrder.BIG_ENDIAN)
        val rawWeight = buffer.short.toInt()

        // Convert to kg (divide by 100)
        val weightKg = rawWeight / AppConstants.WHC06_WEIGHT_DIVISOR

        return weightKg
    }

    /**
     * Generate synthetic timestamp (microseconds since start)
     */
    private fun generateTimestamp(): Long {
        sampleCounter++
        // WHC06 advertises at ~1Hz
        return baseTimestamp + (sampleCounter * 1_000_000)  // 1 second per sample
    }

    /**
     * Reset the disconnect timer
     */
    private fun resetDisconnectTimer() {
        cancelDisconnectTimer()
        disconnectTimer = Runnable {
            Log.i(TAG, "WHC06 disconnect timeout - no advertisements received")
            onDisconnect?.invoke()
        }
        handler.postDelayed(disconnectTimer!!, DISCONNECT_TIMEOUT_MS)
    }

    /**
     * Cancel the disconnect timer
     */
    private fun cancelDisconnectTimer() {
        disconnectTimer?.let { handler.removeCallbacks(it) }
        disconnectTimer = null
    }
}
