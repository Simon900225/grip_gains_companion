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
     * Format: bytes 10-11 contain weight as big-endian 16-bit signed integer, byte 14 contains unit
     * Note: Android's getManufacturerSpecificData already strips the manufacturer ID prefix,
     * so offsets are 2 less than the raw advertisement offsets.
     */
    private fun parseManufacturerData(data: ByteArray): Double? {
        // Verify minimum data size (need byte 14 for unit)
        if (data.size < AppConstants.WHC06_MIN_DATA_SIZE) {
            Log.w(TAG, "Manufacturer data too small: ${data.size} bytes, need ${AppConstants.WHC06_MIN_DATA_SIZE}")
            return null
        }

        // Extract weight from bytes 10-11 (big-endian, signed Int16 for negative values after tare)
        val weightOffset = AppConstants.WHC06_WEIGHT_BYTE_OFFSET
        val buffer = ByteBuffer.wrap(data, weightOffset, 2)
        buffer.order(ByteOrder.BIG_ENDIAN)
        val rawWeight = buffer.short.toInt()

        // Get raw value in device's current unit
        val rawValue = rawWeight / AppConstants.WHC06_WEIGHT_DIVISOR

        // Read unit from byte 14 (low nibble)
        val unitByte = (data[AppConstants.WHC06_UNIT_BYTE_OFFSET].toInt() and 0x0f).toByte()

        // Convert to kg if device is set to lbs
        return if (unitByte == AppConstants.WHC06_UNIT_LBS) {
            rawValue * AppConstants.WHC06_LBS_TO_KG
        } else {
            rawValue
        }
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
