import SwiftUI

/// Compact status bar showing force reading and connection state
struct StatusBarView: View {
    let state: StatusBarState
    let useLbs: Bool
    let theme: ForceBarTheme
    let onUnitToggle: () -> Void
    let onSettingsTap: () -> Void

    @Environment(\.colorScheme) private var colorScheme

    // Legacy initializer for backward compatibility
    init(
        force: Float,
        engaged: Bool,
        calibrating: Bool,
        waitingForSamples: Bool,
        calibrationTimeRemaining: TimeInterval,
        weightMedian: Float?,
        useLbs: Bool,
        theme: ForceBarTheme = .system,
        onUnitToggle: @escaping () -> Void,
        onSettingsTap: @escaping () -> Void
    ) {
        self.state = StatusBarState(
            force: force,
            engaged: engaged,
            calibrating: calibrating,
            waitingForSamples: waitingForSamples,
            calibrationTimeRemaining: calibrationTimeRemaining,
            weightMedian: weightMedian
        )
        self.useLbs = useLbs
        self.theme = theme
        self.onUnitToggle = onUnitToggle
        self.onSettingsTap = onSettingsTap
    }

    private var isDarkMode: Bool {
        switch theme {
        case .dark: return true
        case .light: return false
        case .system: return colorScheme == .dark
        }
    }

    private var backgroundColor: Color {
        isDarkMode ? .black : Color(.systemBackground)
    }

    private var secondaryTextColor: Color {
        isDarkMode ? .gray : .secondary
    }

    var body: some View {
        VStack(spacing: 4) {
            mainRow
            calibrationMessage
        }
        .padding(.horizontal)
        .padding(.vertical, 10)
        .background(backgroundColor)
    }

    // MARK: - View Components

    @ViewBuilder
    private var mainRow: some View {
        HStack(spacing: 8) {
            forceDisplay
            Spacer()
            weightDisplay
            stateBadge
            settingsButton
        }
    }

    private var forceDisplay: some View {
        Text(WeightFormatter.format(state.force, useLbs: useLbs))
            .font(.subheadline)
            .fontWeight(.semibold)
            .foregroundColor(state.forceColor(baseline: state.weightMedian ?? 0, isDarkMode: isDarkMode))
            .onTapGesture { onUnitToggle() }
    }

    @ViewBuilder
    private var weightDisplay: some View {
        if state.showWeight, let median = state.weightMedian {
            Text("⚖ \(WeightFormatter.format(median, useLbs: useLbs))")
                .font(.caption)
                .foregroundColor(secondaryTextColor)
        }
    }

    private var stateBadge: some View {
        Text(state.stateText)
            .font(.caption)
            .fontWeight(.medium)
            .padding(.horizontal, 8)
            .padding(.vertical, 3)
            .background(state.stateColor)
            .foregroundColor(.white)
            .cornerRadius(4)
    }

    private var settingsButton: some View {
        Button(action: onSettingsTap) {
            Image(systemName: "gearshape")
                .font(.subheadline)
                .foregroundColor(secondaryTextColor)
        }
    }

    @ViewBuilder
    private var calibrationMessage: some View {
        if state.calibrating {
            Text("Don't touch Tindeq")
                .font(.caption)
                .foregroundColor(.orange)
                .fontWeight(.medium)
        }
    }
}

#Preview("Idle with weight") {
    StatusBarView(
        force: 2.1,
        engaged: false,
        calibrating: false,
        waitingForSamples: false,
        calibrationTimeRemaining: 0,
        weightMedian: 2.1,
        useLbs: false,
        onUnitToggle: {},
        onSettingsTap: {}
    )
}

#Preview("Gripping") {
    StatusBarView(
        force: 25.3,
        engaged: true,
        calibrating: false,
        waitingForSamples: false,
        calibrationTimeRemaining: 0,
        weightMedian: 2.1,
        useLbs: false,
        onUnitToggle: {},
        onSettingsTap: {}
    )
}

#Preview("Calibrating") {
    StatusBarView(
        force: 0.0,
        engaged: false,
        calibrating: true,
        waitingForSamples: false,
        calibrationTimeRemaining: 3.5,
        weightMedian: nil,
        useLbs: false,
        onUnitToggle: {},
        onSettingsTap: {}
    )
}
