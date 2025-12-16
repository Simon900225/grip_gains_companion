import SwiftUI

/// Bundles all state needed for the status bar display
struct StatusBarState {
    let force: Float
    let engaged: Bool
    let calibrating: Bool
    let waitingForSamples: Bool
    let calibrationTimeRemaining: TimeInterval
    let weightMedian: Float?

    /// Show weight only when idle (not gripping, calibrating, or connecting)
    var showWeight: Bool {
        weightMedian != nil && !engaged && !calibrating && !waitingForSamples
    }

    var stateText: String {
        if waitingForSamples { return "CONNECTING" }
        if calibrating { return "CALIBRATING \(Int(ceil(calibrationTimeRemaining)))s" }
        if engaged { return "GRIPPING" }
        return "IDLE"
    }

    var stateColor: Color {
        if waitingForSamples { return .orange }
        if calibrating { return .gray }
        if engaged { return .green }
        return .blue
    }

    func forceColor(baseline: Float = 0, isDarkMode: Bool = true) -> Color {
        if waitingForSamples || calibrating { return .gray }
        if engaged { return .green }
        if force - baseline >= AppConstants.engageThreshold { return .orange }
        return isDarkMode ? .white : .primary
    }
}
