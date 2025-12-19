//
//  GripGainsTimerWidgetLiveActivity.swift
//  GripGainsTimerWidget
//
//  Created by Diego Tertuliano on 12/19/25.
//

import ActivityKit
import WidgetKit
import SwiftUI

struct GripGainsTimerWidgetLiveActivity: Widget {
    var body: some WidgetConfiguration {
        ActivityConfiguration(for: GripTimerAttributes.self) { context in
            // Lock screen/banner UI
            HStack {
                VStack(alignment: .leading) {
                    Text("Elapsed")
                        .font(.caption2)
                        .foregroundColor(.secondary)
                    Text("\(context.state.elapsedSeconds)s")
                        .font(.title2)
                        .fontWeight(.bold)
                }
                Spacer()
                VStack(alignment: .trailing) {
                    Text(context.state.remainingSeconds < 0 ? "Bonus" : "Remaining")
                        .font(.caption2)
                        .foregroundColor(.secondary)
                    if context.state.remainingSeconds < 0 {
                        Text("+\(abs(context.state.remainingSeconds))s")
                            .font(.title2)
                            .fontWeight(.bold)
                            .foregroundColor(.green)
                    } else {
                        Text("\(context.state.remainingSeconds)s")
                            .font(.title2)
                            .fontWeight(.bold)
                    }
                }
            }
            .padding()
            .activityBackgroundTint(Color.black.opacity(0.8))
            .activitySystemActionForegroundColor(Color.white)

        } dynamicIsland: { context in
            DynamicIsland {
                // Expanded UI
                DynamicIslandExpandedRegion(.center) {
                    HStack(spacing: 24) {
                        VStack {
                            Text("Elapsed")
                                .font(.caption)
                                .foregroundColor(.secondary)
                            Text("\(context.state.elapsedSeconds)s")
                                .font(.title)
                                .fontWeight(.bold)
                        }
                        VStack {
                            Text(context.state.remainingSeconds < 0 ? "Bonus" : "Remaining")
                                .font(.caption)
                                .foregroundColor(.secondary)
                            if context.state.remainingSeconds < 0 {
                                Text("+\(abs(context.state.remainingSeconds))s")
                                    .font(.title)
                                    .fontWeight(.bold)
                                    .foregroundColor(.green)
                            } else {
                                Text("\(context.state.remainingSeconds)s")
                                    .font(.title)
                                    .fontWeight(.bold)
                            }
                        }
                    }
                }
            } compactLeading: {
                Text("\(context.state.elapsedSeconds)s")
                    .font(.caption2)
                    .fontWeight(.bold)
            } compactTrailing: {
                if context.state.remainingSeconds < 0 {
                    Text("+\(abs(context.state.remainingSeconds))s")
                        .font(.caption2)
                        .fontWeight(.bold)
                        .foregroundColor(.green)
                } else {
                    Text("\(context.state.remainingSeconds)s")
                        .font(.caption2)
                        .fontWeight(.bold)
                }
            } minimal: {
                if context.state.remainingSeconds < 0 {
                    Text("+\(abs(context.state.remainingSeconds))s")
                        .font(.caption2)
                        .foregroundColor(.green)
                } else {
                    Text("\(context.state.remainingSeconds)s")
                        .font(.caption2)
                }
            }
        }
    }
}

#Preview("Notification", as: .content, using: GripTimerAttributes()) {
    GripGainsTimerWidgetLiveActivity()
} contentStates: {
    GripTimerAttributes.ContentState(elapsedSeconds: 15, remainingSeconds: 15)
    GripTimerAttributes.ContentState(elapsedSeconds: 35, remainingSeconds: -5)
}
