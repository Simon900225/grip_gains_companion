package app.grip_gains_companion.service.ble

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.os.Build
import app.grip_gains_companion.config.AppConstants
import app.grip_gains_companion.util.AppLogger
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
        AppLogger.i(TAG, "Starting PitchSix service...")
        baseTimestamp = System.currentTimeMillis() * 1000  // Convert to microseconds
        sampleCounter = 0
    }

    /**
     * Stop the service
     */
    fun stop() {
        AppLogger.i(TAG, "Stopping PitchSix service...")
    }

    /**
     * Send start streaming command to Device Mode characteristic
     * Write 0x04 to enter continuous streaming mode
     */
    fun startStreaming(gatt: BluetoothGatt, deviceModeCharacteristic: BluetoothGattCharacteristic): Boolean {
        AppLogger.i(TAG, "Sending start streaming command (0x04) to Device Mode characteristic...")
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // API 33+: new method signature
            gatt.writeCharacteristic(
                deviceModeCharacteristic,
                AppConstants.PITCH_SIX_MODE_STREAMING,
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            ) == BluetoothGatt.GATT_SUCCESS
        } else {
            // API < 33: old method signature
            @Suppress("DEPRECATION")
            deviceModeCharacteristic.value = AppConstants.PITCH_SIX_MODE_STREAMING
            @Suppress("DEPRECATION")
            deviceModeCharacteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            @Suppress("DEPRECATION")
            gatt.writeCharacteristic(deviceModeCharacteristic)
        }
    }

    /**
     * Send tare command via Device Mode characteristic
     * Write 0x05 to Device Mode to tare
     */
    fun sendTareViaDeviceMode(gatt: BluetoothGatt, deviceModeCharacteristic: BluetoothGattCharacteristic): Boolean {
        AppLogger.i(TAG, "Sending tare command (0x05) to Device Mode characteristic...")
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // API 33+: new method signature
            gatt.writeCharacteristic(
                deviceModeCharacteristic,
                AppConstants.PITCH_SIX_MODE_TARE,
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            ) == BluetoothGatt.GATT_SUCCESS
        } else {
            // API < 33: old method signature
            @Suppress("DEPRECATION")
            deviceModeCharacteristic.value = AppConstants.PITCH_SIX_MODE_TARE
            @Suppress("DEPRECATION")
            deviceModeCharacteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            @Suppress("DEPRECATION")
            gatt.writeCharacteristic(deviceModeCharacteristic)
        }
    }

    /**
     * Send tare command via Tare characteristic
     * Write 0x01 to Tare characteristic to tare
     */
    fun sendTare(gatt: BluetoothGatt, tareCharacteristic: BluetoothGattCharacteristic): Boolean {
        AppLogger.i(TAG, "Sending tare command (0x01) to Tare characteristic...")
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // API 33+: new method signature
            gatt.writeCharacteristic(
                tareCharacteristic,
                AppConstants.PITCH_SIX_TARE_COMMAND,
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            ) == BluetoothGatt.GATT_SUCCESS
        } else {
            // API < 33: old method signature
            @Suppress("DEPRECATION")
            tareCharacteristic.value = AppConstants.PITCH_SIX_TARE_COMMAND
            @Suppress("DEPRECATION")
            tareCharacteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            @Suppress("DEPRECATION")
            gatt.writeCharacteristic(tareCharacteristic)
        }
    }

    /**
     * Send stop command (enter Idle mode) to Device Mode characteristic
     * Write 0x07 to stop streaming
     */
    fun stopStreaming(gatt: BluetoothGatt, deviceModeCharacteristic: BluetoothGattCharacteristic): Boolean {
        AppLogger.i(TAG, "Sending stop/idle command (0x07) to Device Mode characteristic...")
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // API 33+: new method signature
            gatt.writeCharacteristic(
                deviceModeCharacteristic,
                AppConstants.PITCH_SIX_MODE_IDLE,
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            ) == BluetoothGatt.GATT_SUCCESS
        } else {
            // API < 33: old method signature
            @Suppress("DEPRECATION")
            deviceModeCharacteristic.value = AppConstants.PITCH_SIX_MODE_IDLE
            @Suppress("DEPRECATION")
            deviceModeCharacteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            @Suppress("DEPRECATION")
            gatt.writeCharacteristic(deviceModeCharacteristic)
        }
    }

    /**
     * Parse notification data from PitchSix
     * Format: [numSamples_MSB, numSamples_LSB, sample1_byte1, sample1_byte2, sample1_byte3, ...]
     * Each sample is 3 bytes representing pounds in the format: byte1*32768 + byte2*256 + byte3
     */
    fun parseNotification(data: ByteArray) {
        if (data.size < 2) {
            AppLogger.w(TAG, "Data too short: ${data.size} bytes")
            return
        }

        // First two bytes contain the number of samples in the packet
        val numSamples = ((data[0].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)
        
        AppLogger.d(TAG, "Received packet with $numSamples samples (${data.size} bytes)")

        // Process each sample (3 bytes per sample)
        for (i in 0 until numSamples) {
            val offset = 2 + i * 3  // Skip the first 2 bytes (sample count)
            
            if (offset + 2 >= data.size) {
                AppLogger.w(TAG, "Incomplete sample $i at offset $offset")
                break
            }

            // Parse 3-byte value as unsigned: byte1*32768 + byte2*256 + byte3
            val rawValueLbs = parseThreeByteUnsigned(data, offset)
            
            // Convert from pounds to kg
            val weightKg = rawValueLbs * AppConstants.PITCH_SIX_RAW_TO_KG_FACTOR

            // Generate synthetic timestamp
            val timestamp = generateTimestamp()

            AppLogger.v(TAG, "Sample $i: raw=$rawValueLbs lbs, weight=${"%.2f".format(weightKg)} kg")

            onForceSample?.invoke(weightKg, timestamp)
        }
    }

    /**
     * Parse a 3-byte unsigned value in the format: byte1*32768 + byte2*256 + byte3
     * This represents pounds (lbs) that need to be converted to kg
     */
    private fun parseThreeByteUnsigned(data: ByteArray, offset: Int): Double {
        val byte1 = data[offset].toInt() and 0xFF
        val byte2 = data[offset + 1].toInt() and 0xFF
        val byte3 = data[offset + 2].toInt() and 0xFF

        // Calculate as per PitchSix protocol: byte1*32768 + byte2*256 + byte3
        return (byte1 * 32768.0) + (byte2 * 256.0) + byte3.toDouble()
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
