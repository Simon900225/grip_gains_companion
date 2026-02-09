import SwiftUI

/// Displays a single rep from history with force graph and statistics
struct HistoryRepSection: View {
    let rep: RepLog
    let index: Int
    let useLbs: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            // Rep Header
            HStack {
                Text("Rep \(index)")
                    .font(.headline)
                Spacer()
                Text(rep.timestamp, style: .time)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            .padding(.horizontal)

            // Force Graph (reuse RepForceGraphView from SetReviewSheet)
            RepForceGraphView(
                samples: rep.samples,
                duration: rep.duration,
                targetWeight: rep.targetWeight,
                useLbs: useLbs,
                filterStartIndex: rep.filterStartIndex,
                filterEndIndex: rep.filterEndIndex
            )
            .frame(height: 180)
            .padding(.horizontal)

            // Rep Stats
            VStack(spacing: 6) {
                StatRowView(label: "Duration", value: String(format: "%.1fs", rep.duration))
                StatRowView(label: "Median", value: WeightFormatter.format(rep.median, useLbs: useLbs))
                StatRowView(label: "Average", value: WeightFormatter.format(rep.mean, useLbs: useLbs))
                StatRowView(label: "Standard Deviation", value: String(format: "%.2f", useLbs ? rep.stdDev * AppConstants.kgToLbs : rep.stdDev))

                if let absDev = rep.absoluteDeviation, let pctDev = rep.deviationPercentage {
                    StatRowView(label: "Difference from Target", value: DeviationFormatter.format(absolute: absDev, percentage: pctDev, useLbs: useLbs))
                }

                // Percentile grid
                PercentilesGridView(percentiles: [
                    ("P1", rep.p1), ("P5", rep.p5), ("P10", rep.p10), ("P25", rep.q1),
                    ("P75", rep.q3), ("P90", rep.p90), ("P95", rep.p95), ("P99", rep.p99)
                ], useLbs: useLbs)
            }
            .padding(.horizontal)
        }
    }
}
