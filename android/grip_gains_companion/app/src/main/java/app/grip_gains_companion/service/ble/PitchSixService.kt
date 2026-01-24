package app.grip_gains_companion.service.ble

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.util.Log
import app.grip_gains_companion.config.AppConstants
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Protocol handler for PitchSix Force Board
 * Uses GATT connection with 3-byte samples
 */
class PitchSixService {

    companion object {
        private const val TAG = "PitchSixService"
    }

    /** Callback for force samples (weight in kg, timestamp in microseconds) */
    var onForceSample: ((Double, Long) -> Unit)? = null

    private var baseTimestamp: Long = 0
    private var sampleCounter: Long = 0

    /**
     * Start the service - called when connected
     */
    fun start() {
        Log.i(TAG, "Starting PitchSix service...")
        baseTimestamp = System.currentTimeMillis() * 1000  // Convert to microseconds
        sampleCounter = 0
    }

    /**
     * Stop the service
     */
    fun stop() {
        Log.i(TAG, "Stopping PitchSix service...")
    }

    /**
     * Send start streaming command
     */
    fun startStreaming(gatt: BluetoothGatt, writeCharacteristic: BluetoothGattCharacteristic): Boolean {
        Log.i(TAG, "Sending start streaming command...")
        return gatt.writeCharacteristic(
            writeCharacteristic,
            AppConstants.PITCH_SIX_START_STREAMING_COMMAND,
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        ) == BluetoothGatt.GATT_SUCCESS
    }

    /**
     * Send tare command
     */
    fun sendTare(gatt: BluetoothGatt, writeCharacteristic: BluetoothGattCharacteristic): Boolean {
        Log.i(TAG, "Sending tare command...")
        return gatt.writeCharacteristic(
            writeCharacteristic,
            AppConstants.PITCH_SIX_TARE_COMMAND,
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        ) == BluetoothGatt.GATT_SUCCESS
    }

    /**
     * Send stop command
     */
    fun stopStreaming(gatt: BluetoothGatt, writeCharacteristic: BluetoothGattCharacteristic): Boolean {
        Log.i(TAG, "Sending stop command...")
        return gatt.writeCharacteristic(
            writeCharacteristic,
            AppConstants.PITCH_SIX_STOP_COMMAND,
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        ) == BluetoothGatt.GATT_SUCCESS
    }

    /**
     * Parse notification data from PitchSix
     * Each sample is 3 bytes, big-endian 24-bit signed integer
     */
    fun parseNotification(data: ByteArray) {
        if (data.isEmpty()) return

        var offset = 0
        while (offset + AppConstants.PITCH_SIX_SAMPLE_SIZE <= data.size) {
            // Parse 3-byte big-endian value
            val rawValue = parseThreeByteInt(data, offset)
            
            // Convert to kg: raw value × 0.453592
            val weightKg = rawValue * AppConstants.PITCH_SIX_RAW_TO_KG_FACTOR

            // Generate synthetic timestamp
            val timestamp = generateTimestamp()

            onForceSample?.invoke(weightKg, timestamp)

            offset += AppConstants.PITCH_SIX_SAMPLE_SIZE
        }
    }

    /**
     * Parse a 3-byte big-endian signed integer
     */
    private fun parseThreeByteInt(data: ByteArray, offset: Int): Int {
        val b0 = data[offset].toInt() and 0xFF
        val b1 = data[offset + 1].toInt() and 0xFF
        val b2 = data[offset + 2].toInt() and 0xFF

        // Big-endian: first byte is MSB
        var value = (b0 shl 16) or (b1 shl 8) or b2

        // Sign-extend if negative (bit 23 is set)
        if ((value and 0x800000) != 0) {
            value = value or 0xFF000000.toInt()
        }

        return value
    }

    /**
     * Generate synthetic timestamp (microseconds since start)
     */
    private fun generateTimestamp(): Long {
        sampleCounter++
        // Assume ~10Hz sample rate for PitchSix
        return baseTimestamp + (sampleCounter * 100_000)  // 100ms per sample
    }
}
