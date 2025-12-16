import CoreBluetooth

/// Handles Tindeq Progressor BLE protocol: service discovery, notifications, and data parsing
class ProgressorService: NSObject, CBPeripheralDelegate {
    private let peripheral: CBPeripheral
    private var notifyCharacteristic: CBCharacteristic?
    private var writeCharacteristic: CBCharacteristic?
    private var discoveryTimer: Timer?

    /// Callback when a force sample is received
    var onForceSample: ((Float) -> Void)?

    /// Callback when discovery times out
    var onDiscoveryTimeout: (() -> Void)?

    init(peripheral: CBPeripheral) {
        self.peripheral = peripheral
        super.init()
        peripheral.delegate = self
    }

    deinit {
        cancelDiscoveryTimeout()
    }

    // MARK: - Public Methods

    func discoverServices() {
        Log.ble.info("Starting service discovery...")
        startDiscoveryTimeout()
        peripheral.discoverServices([AppConstants.progressorServiceUUID])
    }

    // MARK: - Discovery Timeout

    private func startDiscoveryTimeout() {
        cancelDiscoveryTimeout()
        discoveryTimer = Timer.scheduledTimer(withTimeInterval: AppConstants.discoveryTimeout, repeats: false) { [weak self] _ in
            guard let self = self else { return }
            Log.ble.error("Service discovery timed out")
            self.onDiscoveryTimeout?()
        }
    }

    private func cancelDiscoveryTimeout() {
        discoveryTimer?.invalidate()
        discoveryTimer = nil
    }

    /// Send the start weight command to begin measurements
    func startWeightMeasurement() {
        guard let writeChar = writeCharacteristic else {
            Log.ble.error("Write characteristic not available")
            return
        }
        Log.ble.info("Sending start weight command...")
        peripheral.writeValue(AppConstants.startWeightCommand, for: writeChar, type: .withResponse)
    }

    // MARK: - CBPeripheralDelegate

    func peripheral(_ peripheral: CBPeripheral, didDiscoverServices error: Error?) {
        if let error = error {
            Log.ble.error("Discovering services: \(error.localizedDescription)")
            return
        }

        Log.ble.info("Services discovered: \(peripheral.services?.count ?? 0)")

        guard let services = peripheral.services else {
            Log.ble.error("No services found")
            return
        }

        for service in services {
            if service.uuid == AppConstants.progressorServiceUUID {
                Log.ble.info("Found Progressor service, discovering characteristics...")
                peripheral.discoverCharacteristics(
                    [AppConstants.notifyCharacteristicUUID, AppConstants.writeCharacteristicUUID],
                    for: service
                )
            }
        }
    }

    func peripheral(_ peripheral: CBPeripheral,
                    didDiscoverCharacteristicsFor service: CBService,
                    error: Error?) {
        if let error = error {
            Log.ble.error("Discovering characteristics: \(error.localizedDescription)")
            return
        }

        Log.ble.info("Characteristics found: \(service.characteristics?.count ?? 0)")

        guard let characteristics = service.characteristics else {
            Log.ble.error("No characteristics found")
            return
        }

        for characteristic in characteristics {
            switch characteristic.uuid {
            case AppConstants.notifyCharacteristicUUID:
                Log.ble.info("Found notify characteristic, enabling notifications...")
                notifyCharacteristic = characteristic
                peripheral.setNotifyValue(true, for: characteristic)

            case AppConstants.writeCharacteristicUUID:
                Log.ble.info("Found write characteristic")
                writeCharacteristic = characteristic
                // Start weight measurement once we have the write characteristic
                startWeightMeasurement()

            default:
                break
            }
        }
    }

    func peripheral(_ peripheral: CBPeripheral,
                    didUpdateValueFor characteristic: CBCharacteristic,
                    error: Error?) {
        if let error = error {
            Log.ble.error("Receiving notification: \(error.localizedDescription)")
            return
        }

        guard characteristic.uuid == AppConstants.notifyCharacteristicUUID,
              let data = characteristic.value else {
            return
        }

        parseNotification(data)
    }

    func peripheral(_ peripheral: CBPeripheral,
                    didWriteValueFor characteristic: CBCharacteristic,
                    error: Error?) {
        if let error = error {
            Log.ble.error("Writing to characteristic: \(error.localizedDescription)")
            return
        }
        Log.ble.info("Write successful")
    }

    func peripheral(_ peripheral: CBPeripheral,
                    didUpdateNotificationStateFor characteristic: CBCharacteristic,
                    error: Error?) {
        if let error = error {
            Log.ble.error("Enabling notifications: \(error.localizedDescription)")
            return
        }
        Log.ble.info("Notifications \(characteristic.isNotifying ? "enabled" : "disabled")")

        // Discovery complete - cancel timeout
        if characteristic.isNotifying {
            cancelDiscoveryTimeout()
        }
    }

    // MARK: - Data Parsing

    /// Parse incoming BLE notification data
    /// From working watchOS app: data[0] == 1 and bytes 2-5 as little-endian float
    private func parseNotification(_ data: Data) {
        do {
            let weight = try parseWeightData(data)
            onForceSample?(weight)
        } catch {
            // Only log actual errors, not expected packet type filtering
            if case ParseError.invalidPacketType = error {
                // Silently ignore non-weight packets
            } else {
                Log.ble.debug("Parse error: \(error.localizedDescription)")
            }
        }
    }

    /// Parse weight data from BLE packet
    /// - Throws: ParseError if data is invalid
    private func parseWeightData(_ data: Data) throws -> Float {
        guard data.count >= ProgressorProtocol.packetMinSize else {
            throw ParseError.insufficientData(data.count)
        }

        let packetType = data[0]
        guard packetType == ProgressorProtocol.weightDataPacketType else {
            throw ParseError.invalidPacketType(packetType)
        }

        let floatData = data.subdata(in: ProgressorProtocol.floatDataStart..<ProgressorProtocol.floatDataEnd)
        let weight = floatData.withUnsafeBytes { $0.load(as: Float.self) }

        return weight
    }
}
