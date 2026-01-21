package app.grip_gains_companion.config

import java.util.UUID

/**
 * Configuration constants ported from iOS AppConstants.swift
 */
object AppConstants {
    // MARK: - Thresholds (kg)
    const val ENGAGE_THRESHOLD = 3.0
    const val FAIL_THRESHOLD = 1.0
    const val CALIBRATION_DURATION_MS = 5000L

    // MARK: - Grip Detection Defaults (kg)
    const val DEFAULT_ENGAGE_THRESHOLD = 3.0
    const val DEFAULT_FAIL_THRESHOLD = 1.0
    const val MIN_GRIP_THRESHOLD = 0.5
    const val MAX_ENGAGE_THRESHOLD = 10.0
    const val MAX_FAIL_THRESHOLD = 5.0

    // MARK: - Target Weight
    const val DEFAULT_WEIGHT_TOLERANCE = 0.5  // kg
    const val MIN_WEIGHT_TOLERANCE = 0.1      // kg
    const val MAX_WEIGHT_TOLERANCE = 1.0      // kg
    const val OFF_TARGET_FEEDBACK_INTERVAL_MS = 1000L  // milliseconds (throttle)

    // MARK: - Percentage-Based Thresholds
    const val DEFAULT_ENABLE_PERCENTAGE_THRESHOLDS = true
    const val DEFAULT_ENGAGE_PERCENTAGE = 0.50      // 50% of target weight
    const val DEFAULT_DISENGAGE_PERCENTAGE = 0.20   // 20% of target weight
    const val DEFAULT_TOLERANCE_PERCENTAGE = 0.05   // 5% of target weight
    const val MIN_PERCENTAGE = 0.05                 // 5% minimum
    const val MAX_ENGAGE_PERCENTAGE = 0.90          // 90% maximum for engage
    const val MAX_DISENGAGE_PERCENTAGE = 0.50       // 50% maximum for disengage

    // MARK: - Percentage Threshold Bounds (kg)
    const val DEFAULT_ENGAGE_FLOOR = 3.0
    const val DEFAULT_ENGAGE_CEILING = 0.0
    const val DEFAULT_DISENGAGE_FLOOR = 2.0
    const val DEFAULT_DISENGAGE_CEILING = 0.0
    const val DEFAULT_TOLERANCE_FLOOR = 0.3
    const val DEFAULT_TOLERANCE_CEILING = 1.5

    // MARK: - Weight Calibration
    const val DEFAULT_WEIGHT_CALIBRATION_THRESHOLD = 3.0  // kg

    // MARK: - UI Defaults
    const val DEFAULT_ENABLE_HAPTICS = true
    const val DEFAULT_ENABLE_TARGET_SOUND = true
    const val DEFAULT_SHOW_GRIP_STATS = true
    const val DEFAULT_SHOW_SET_REVIEW = false
    const val DEFAULT_SHOW_STATUS_BAR = true
    const val DEFAULT_EXPANDED_FORCE_BAR = true
    const val DEFAULT_SHOW_FORCE_GRAPH = true
    const val DEFAULT_FORCE_GRAPH_WINDOW = 5
    const val DEFAULT_FULL_SCREEN = true
    const val DEFAULT_ENABLE_TARGET_WEIGHT = true
    const val DEFAULT_USE_MANUAL_TARGET = false
    const val DEFAULT_MANUAL_TARGET_WEIGHT = 20.0
    const val DEFAULT_ENABLE_CALIBRATION = true
    const val DEFAULT_BACKGROUND_TIME_SYNC = true
    const val DEFAULT_ENABLE_LIVE_ACTIVITY = true
    const val DEFAULT_AUTO_SELECT_WEIGHT = true
    const val DEFAULT_AUTO_SELECT_FROM_MANUAL = false
    const val DEFAULT_USE_KEYBOARD_INPUT = false

    // MARK: - Web
    const val GRIP_GAINS_URL = "https://gripgains.ca/timer"

    // MARK: - Tindeq Progressor BLE UUIDs
    val PROGRESSOR_SERVICE_UUID: UUID = UUID.fromString("7E4E1701-1EA6-40C9-9DCC-13D34FFEAD57")
    val NOTIFY_CHARACTERISTIC_UUID: UUID = UUID.fromString("7E4E1702-1EA6-40C9-9DCC-13D34FFEAD57")
    val WRITE_CHARACTERISTIC_UUID: UUID = UUID.fromString("7E4E1703-1EA6-40C9-9DCC-13D34FFEAD57")

    // MARK: - BLE Commands
    val START_WEIGHT_COMMAND = byteArrayOf(101)

    // MARK: - Data Format
    /** Each sample: 4-byte float (weight) + 4-byte uint32 (microseconds) */
    const val SAMPLE_SIZE = 8

    // MARK: - BLE Protocol
    const val WEIGHT_DATA_PACKET_TYPE: Byte = 1
    const val PACKET_MIN_SIZE = 6
    const val FLOAT_DATA_START = 2
    const val FLOAT_DATA_END = 6

    // MARK: - Timing
    const val SESSION_REFRESH_INTERVAL_MS = 2000L
    const val BLE_RECONNECT_DELAY_MS = 3000L
    const val DISCOVERY_TIMEOUT_MS = 30000L
    const val MAX_RETRY_DELAY_MS = 30000L
    const val BACKGROUND_INACTIVITY_TIMEOUT_MS = 300000L  // 5 minutes

    // MARK: - Unit Conversion
    const val KG_TO_LBS = 2.20462

    // MARK: - RSSI Signal Thresholds
    const val RSSI_EXCELLENT = -50
    const val RSSI_GOOD = -60
    const val RSSI_FAIR = -70
    const val RSSI_WEAK = -90

    // MARK: - Notification
    const val NOTIFICATION_CHANNEL_ID = "grip_timer_channel"
    const val NOTIFICATION_ID = 1
}
