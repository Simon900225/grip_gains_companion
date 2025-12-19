import ActivityKit
import Foundation

/// Manages the grip timer Live Activity lifecycle
@MainActor
class ActivityManager: ObservableObject {
    private var currentActivity: Activity<GripTimerAttributes>?
    private var updateTimer: Timer?
    private var gripStartTime: Date?
    private var elapsedAtStart: Int = 0
    private var remainingAtStart: Int = 0

    /// Start a new Live Activity for the grip session
    func startActivity(elapsedSeconds: Int, remainingSeconds: Int) {
        // Don't start if already active
        guard currentActivity == nil else { return }

        Log.app.info("Starting Live Activity: elapsed=\(elapsedSeconds), remaining=\(remainingSeconds)")

        guard ActivityAuthorizationInfo().areActivitiesEnabled else {
            Log.app.warning("Live Activities are not enabled on this device")
            return
        }

        Log.app.info("Live Activities are enabled, proceeding...")

        self.elapsedAtStart = elapsedSeconds
        self.remainingAtStart = remainingSeconds
        self.gripStartTime = Date()

        let attributes = GripTimerAttributes()
        let state = GripTimerAttributes.ContentState(
            elapsedSeconds: elapsedSeconds,
            remainingSeconds: remainingSeconds
        )

        do {
            currentActivity = try Activity.request(
                attributes: attributes,
                content: .init(state: state, staleDate: nil),
                pushType: nil
            )
            Log.app.info("Started Live Activity")
            startUpdateTimer()
        } catch {
            Log.app.error("Failed to start Live Activity: \(error.localizedDescription)")
        }
    }

    /// Update the Live Activity with current elapsed time
    func updateActivity() {
        guard let activity = currentActivity,
              let startTime = gripStartTime else { return }

        let secondsSinceStart = Int(Date().timeIntervalSince(startTime))
        let elapsed = elapsedAtStart + secondsSinceStart
        let remaining = remainingAtStart - secondsSinceStart

        let state = GripTimerAttributes.ContentState(
            elapsedSeconds: elapsed,
            remainingSeconds: remaining
        )

        Log.app.debug("Updating content for activity \(activity.id)")

        Task {
            await activity.update(.init(state: state, staleDate: nil))
        }
    }

    /// End the current Live Activity
    func endActivity() {
        stopUpdateTimer()

        guard let activity = currentActivity else { return }

        let secondsSinceStart: Int
        if let startTime = gripStartTime {
            secondsSinceStart = Int(Date().timeIntervalSince(startTime))
        } else {
            secondsSinceStart = 0
        }

        let finalElapsed = elapsedAtStart + secondsSinceStart
        let finalRemaining = remainingAtStart - secondsSinceStart
        let finalState = GripTimerAttributes.ContentState(
            elapsedSeconds: finalElapsed,
            remainingSeconds: finalRemaining
        )

        Task {
            await activity.end(.init(state: finalState, staleDate: nil), dismissalPolicy: .immediate)
            Log.app.info("Ended Live Activity")
        }

        currentActivity = nil
        gripStartTime = nil
    }

    /// Check if there's an active Live Activity
    var isActivityActive: Bool {
        currentActivity != nil
    }

    // MARK: - Private

    private func startUpdateTimer() {
        stopUpdateTimer()
        updateTimer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { [weak self] _ in
            Task { @MainActor in
                self?.updateActivity()
            }
        }
    }

    private func stopUpdateTimer() {
        updateTimer?.invalidate()
        updateTimer = nil
    }
}
