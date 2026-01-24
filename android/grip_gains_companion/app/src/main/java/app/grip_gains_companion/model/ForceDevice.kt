package app.grip_gains_companion.model

import android.bluetooth.le.ScanResult
import app.grip_gains_companion.config.AppConstants

/**
 * Represents a discovered force measurement device
 */
data class ForceDevice(
    val address: String,
    val name: String,
    val type: DeviceType,
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
        /**
         * Create a ForceDevice from a scan result if it matches any supported device type
         */
        fun fromScanResult(scanResult: ScanResult, filterType: DeviceType? = null): ForceDevice? {
            val deviceType = DeviceType.detect(scanResult) ?: return null
            
            // If a filter type is specified, only return devices of that type
            if (filterType != null && deviceType != filterType) {
                return null
            }

            val deviceName = try {
                scanResult.device.name
            } catch (e: SecurityException) {
                null
            }

            return ForceDevice(
                address = scanResult.device.address,
                name = deviceName ?: deviceType.displayName,
                type = deviceType,
                rssi = scanResult.rssi
            )
        }
    }
}
