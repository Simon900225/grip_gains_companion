import SwiftUI

enum ForceBarTheme: String, CaseIterable {
    case system
    case light
    case dark

    var displayName: String {
        switch self {
        case .system: return "System"
        case .light: return "Light"
        case .dark: return "Dark"
        }
    }
}

struct SettingsView: View {
    let deviceName: String?
    let isDeviceConnected: Bool
    @Binding var useLbs: Bool
    let webCoordinator: WebViewCoordinator
    let onDisconnect: () -> Void
    let onConnectDevice: () -> Void

    @Environment(\.dismiss) private var dismiss
    @AppStorage("enableHaptics") private var enableHaptics = true
    @AppStorage("showStatusBar") private var showStatusBar = true
    @AppStorage("fullScreen") private var fullScreen = true
    @AppStorage("forceBarTheme") private var forceBarTheme = ForceBarTheme.system.rawValue

    var body: some View {
        NavigationStack {
            List {
                // Device section
                Section("Device") {
                    if isDeviceConnected {
                        if let name = deviceName {
                            HStack {
                                Text("Connected to")
                                Spacer()
                                Text(name)
                                    .foregroundColor(.secondary)
                            }
                        }

                        Button(role: .destructive) {
                            onDisconnect()
                        } label: {
                            HStack {
                                Image(systemName: "wifi.slash")
                                Text("Disconnect")
                            }
                        }
                    } else {
                        Button {
                            onConnectDevice()
                        } label: {
                            HStack {
                                Image(systemName: "wave.3.right")
                                Text("Connect Device")
                            }
                        }
                    }
                }

                // Units section
                Section("Units") {
                    Picker("Weight", selection: $useLbs) {
                        Text("kg").tag(false)
                        Text("lbs").tag(true)
                    }
                    .pickerStyle(.segmented)
                }

                // Display section
                Section("Display") {
                    Toggle("Show Force Bar", isOn: $showStatusBar)
                    Toggle("Full Screen", isOn: $fullScreen)
                    Picker("Force Bar Theme", selection: $forceBarTheme) {
                        ForEach(ForceBarTheme.allCases, id: \.rawValue) { theme in
                            Text(theme.displayName).tag(theme.rawValue)
                        }
                    }
                    .pickerStyle(.segmented)
                }

                // Feedback section
                Section("Feedback") {
                    Toggle("Haptic Feedback", isOn: $enableHaptics)
                }

                // Website section
                Section("Website") {
                    Button {
                        webCoordinator.reloadPage()
                        dismiss()
                    } label: {
                        HStack {
                            Image(systemName: "arrow.clockwise")
                            Text("Refresh Page")
                        }
                    }

                    Button(role: .destructive) {
                        webCoordinator.clearWebsiteData()
                        dismiss()
                    } label: {
                        HStack {
                            Image(systemName: "trash")
                            Text("Clear Website Data")
                        }
                    }
                }
            }
            .navigationTitle("Settings")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Done") {
                        dismiss()
                    }
                }
            }
        }
    }
}

#Preview("Connected") {
    SettingsView(
        deviceName: "Progressor_123",
        isDeviceConnected: true,
        useLbs: .constant(false),
        webCoordinator: WebViewCoordinator(),
        onDisconnect: {},
        onConnectDevice: {}
    )
}

#Preview("No Device") {
    SettingsView(
        deviceName: nil,
        isDeviceConnected: false,
        useLbs: .constant(false),
        webCoordinator: WebViewCoordinator(),
        onDisconnect: {},
        onConnectDevice: {}
    )
}
