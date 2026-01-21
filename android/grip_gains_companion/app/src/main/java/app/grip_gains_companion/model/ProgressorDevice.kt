package app.grip_gains_companion.model

import android.bluetooth.BluetoothDevice
import app.grip_gains_companion.config.AppConstants

/**
 * Represents a discovered Tindeq Progressor device
 */
data class ProgressorDevice(
    val address: String,
    val name: String,
    var rssi: Int = 0
) {
    val signalStrength: String
        get() = when {
            rssi > AppConstants.RSSI_EXCELLENT -> "Excellent"
            rssi > AppConstants.RSSI_GOOD -> "Good"
            rssi > AppConstants.RSSI_FAIR -> "Fair"
            else -> "Weak"
        }
    
    val signalBars: Int
        get() = when {
            rssi > AppConstants.RSSI_EXCELLENT -> 4
            rssi > AppConstants.RSSI_GOOD -> 3
            rssi > AppConstants.RSSI_FAIR -> 2
            else -> 1
        }
    
    companion object {
        fun fromBluetoothDevice(device: BluetoothDevice, rssi: Int): ProgressorDevice? {
            // Need BLUETOOTH_CONNECT permission to access name
            val deviceName = try {
                device.name
            } catch (e: SecurityException) {
                null
            }
            
            // Only create if it's a Progressor device
            if (deviceName?.startsWith("Progressor") != true) {
                return null
            }
            
            return ProgressorDevice(
                address = device.address,
                name = deviceName,
                rssi = rssi
            )
        }
    }
}
