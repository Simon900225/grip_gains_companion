import ActivityKit
import Foundation

/// ActivityAttributes for the grip timer Live Activity
struct GripTimerAttributes: ActivityAttributes {
    public struct ContentState: Codable, Hashable {
        /// Seconds elapsed since grip started
        var elapsedSeconds: Int
        /// Seconds remaining until target (negative = overtime, display as bonus)
        var remainingSeconds: Int
    }

    // No static attributes needed - all data is in ContentState
}
