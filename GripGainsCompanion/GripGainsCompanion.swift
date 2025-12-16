import SwiftUI
import UIKit

@main
struct GripGainsCompanion: App {
    init() {
        // Keep screen always on while app is active
        UIApplication.shared.isIdleTimerDisabled = true
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
