import SwiftUI

struct DeviceScannerView: View {
    @ObservedObject var bluetoothManager: BluetoothManager
    let onDeviceSelected: (ProgressorDevice) -> Void
    let onSkipDevice: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            headerSection
            Divider()
            deviceListSection
            Spacer()
            skipButton
        }
    }

    private var skipButton: some View {
        Button("Continue without device") {
            onSkipDevice()
        }
        .font(.footnote)
        .foregroundColor(.secondary)
        .padding(.bottom, 24)
    }

    // MARK: - View Components

    private var headerSection: some View {
        VStack(spacing: 8) {
            Image(systemName: "wave.3.right")
                .font(.system(size: 40))
                .foregroundColor(.blue)

            Text("Select Tindeq")
                .font(.title2)
                .fontWeight(.semibold)

            statusView
        }
        .padding(.vertical, 24)
    }

    @ViewBuilder
    private var deviceListSection: some View {
        if bluetoothManager.discoveredDevices.isEmpty {
            emptyStateView
        } else {
            deviceListView
        }
    }

    @ViewBuilder
    private var emptyStateView: some View {
        Spacer()
        VStack(spacing: 12) {
            switch bluetoothManager.connectionState {
            case .initializing:
                // Bluetooth not ready yet - show nothing (statusView shows "Initializing...")
                EmptyView()
            case .scanning:
                // Actively scanning - show nothing (statusView shows spinner)
                EmptyView()
            default:
                // Finished scanning with no results
                Text("No devices found")
                    .foregroundColor(.secondary)
                Button("Scan Again") {
                    bluetoothManager.startScanning()
                }
                .buttonStyle(.bordered)
            }
        }
        Spacer()
    }

    private var deviceListView: some View {
        List(bluetoothManager.discoveredDevices) { device in
            Button {
                onDeviceSelected(device)
            } label: {
                DeviceRowView(device: device)
            }
            .disabled(bluetoothManager.connectionState == .connecting)
        }
        .listStyle(.plain)
    }

    @ViewBuilder
    private var statusView: some View {
        switch bluetoothManager.connectionState {
        case .initializing:
            StatusIndicator(text: "Initializing...", showProgress: true)
        case .scanning:
            StatusIndicator(text: "Scanning...", showProgress: true)
        case .connecting:
            StatusIndicator(text: "Connecting...", showProgress: true)
        case .error(let msg):
            ErrorStatusView(message: msg) {
                bluetoothManager.startScanning()
            }
        default:
            Text("Tap a device to connect")
                .font(.caption)
                .foregroundColor(.secondary)
        }
    }
}

// MARK: - Error Status View

private struct ErrorStatusView: View {
    let message: String
    let onRetry: () -> Void

    var body: some View {
        VStack(spacing: 12) {
            HStack(spacing: 6) {
                Image(systemName: "exclamationmark.triangle.fill")
                    .foregroundColor(.red)
                Text(userFriendlyMessage)
                    .font(.caption)
                    .foregroundColor(.red)
            }

            Button(action: onRetry) {
                HStack(spacing: 4) {
                    Image(systemName: "arrow.clockwise")
                    Text("Retry")
                }
            }
            .buttonStyle(.bordered)
            .controlSize(.small)

            Text(troubleshootingTip)
                .font(.caption2)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal)
        }
    }

    private var userFriendlyMessage: String {
        if message.lowercased().contains("off") {
            return "Bluetooth is turned off"
        } else if message.lowercased().contains("unauthorized") {
            return "Bluetooth permission required"
        } else if message.lowercased().contains("timeout") {
            return "Connection timed out"
        }
        return message
    }

    private var troubleshootingTip: String {
        if message.lowercased().contains("off") {
            return "Enable Bluetooth in Settings to continue"
        } else if message.lowercased().contains("unauthorized") {
            return "Go to Settings > Grip Gains Companion > Bluetooth to grant permission"
        } else if message.lowercased().contains("timeout") {
            return "Make sure your Tindeq is powered on and nearby"
        }
        return "Make sure your Tindeq is powered on and nearby"
    }
}

// MARK: - Supporting Views

private struct StatusIndicator: View {
    let text: String
    let showProgress: Bool

    var body: some View {
        HStack(spacing: 6) {
            if showProgress {
                ProgressView()
                    .scaleEffect(0.8)
            }
            Text(text)
                .font(.caption)
                .foregroundColor(.secondary)
        }
    }
}

struct DeviceRowView: View {
    let device: ProgressorDevice

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text(device.name)
                    .font(.body)
                    .foregroundColor(.primary)
                Text(device.signalStrength)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }

            Spacer()

            Image(systemName: "wifi", variableValue: Double(device.signalBars) / 4.0)
                .font(.title3)
                .foregroundColor(.secondary)
        }
        .padding(.vertical, 4)
    }
}
