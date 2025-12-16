import Foundation
import os

/// App-wide logging with subsystem categories
/// Usage: Log.ble.info("Connected"), Log.ble.debug("Data: \(hex)"), Log.ble.error("Failed: \(error)")
enum Log {
    private static let subsystem = Bundle.main.bundleIdentifier ?? "GripGainsCompanion"

    /// BLE operations logging
    static let ble = Logger(subsystem: subsystem, category: "BLE")

    /// General app logging
    static let app = Logger(subsystem: subsystem, category: "App")
}
