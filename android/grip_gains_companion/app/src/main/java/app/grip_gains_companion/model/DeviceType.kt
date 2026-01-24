package app.grip_gains_companion.model

import android.bluetooth.le.ScanResult
import app.grip_gains_companion.config.AppConstants

/**
 * Supported force measurement device types
 */
enum class DeviceType {
    TINDEQ_PROGRESSOR,
    PITCH_SIX_FORCE_BOARD,
    WEIHENG_WHC06;

    val displayName: String
        get() = when (this) {
            TINDEQ_PROGRESSOR -> "Tindeq Progressor"
            PITCH_SIX_FORCE_BOARD -> "PitchSix Force Board"
            WEIHENG_WHC06 -> "Weiheng WH-C06"
        }

    val shortName: String
        get() = when (this) {
            TINDEQ_PROGRESSOR -> "Tindeq"
            PITCH_SIX_FORCE_BOARD -> "PitchSix"
            WEIHENG_WHC06 -> "WH-C06"
        }

    /**
     * Whether this device uses GATT connection (vs advertisement-only)
     */
    val usesGATTConnection: Boolean
        get() = when (this) {
            TINDEQ_PROGRESSOR, PITCH_SIX_FORCE_BOARD -> true
            WEIHENG_WHC06 -> false
        }

    companion object {
        /**
         * Detect device type from scan result
         */
        fun detect(scanResult: ScanResult): DeviceType? {
            val deviceName = try {
                scanResult.device.name
            } catch (e: SecurityException) {
                null
            }

            // Tindeq: name starts with "Progressor"
            if (deviceName?.startsWith("Progressor") == true) {
                return TINDEQ_PROGRESSOR
            }

            // PitchSix: name contains "Force Board" or "PitchSix" or "Forceboard"
            if (deviceName != null &&
                (deviceName.contains("Force Board") || 
                 deviceName.contains("PitchSix") || 
                 deviceName.contains("Forceboard"))) {
                return PITCH_SIX_FORCE_BOARD
            }

            // WHC06: manufacturer ID 0x0100
            val manufacturerData = scanResult.scanRecord?.getManufacturerSpecificData(AppConstants.WHC06_MANUFACTURER_ID)
            if (manufacturerData != null) {
                return WEIHENG_WHC06
            }

            return null
        }

        /**
         * Get DeviceType from string value (for persistence)
         */
        fun fromString(value: String?): DeviceType? {
            return try {
                value?.let { valueOf(it) }
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }
}
