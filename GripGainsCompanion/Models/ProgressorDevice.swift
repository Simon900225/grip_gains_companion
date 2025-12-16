import Foundation
import CoreBluetooth

struct ProgressorDevice: Identifiable, Equatable {
    let id: UUID
    let name: String
    let peripheralIdentifier: UUID
    var rssi: Int

    init(peripheral: CBPeripheral, rssi: Int = 0) {
        self.id = peripheral.identifier
        self.name = peripheral.name ?? "Unknown Progressor"
        self.peripheralIdentifier = peripheral.identifier
        self.rssi = rssi
    }

    var signalStrength: String {
        if rssi > SignalThreshold.excellent { return "Excellent" }
        if rssi > SignalThreshold.good { return "Good" }
        if rssi > SignalThreshold.fair { return "Fair" }
        return "Weak"
    }

    var signalBars: Int {
        if rssi > SignalThreshold.excellent { return 4 }
        if rssi > SignalThreshold.good { return 3 }
        if rssi > SignalThreshold.fair { return 2 }
        return 1
    }
}
