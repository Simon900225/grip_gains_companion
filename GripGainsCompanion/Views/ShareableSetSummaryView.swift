import SwiftUI
import Charts

/// Export-optimized view for sharing full set summary as an image
struct ShareableSetSummaryView: View {
    let stats: SetStatistics
    let useLbs: Bool

    static let exportWidth: CGFloat = 1080
    private let graphHeight: CGFloat = 280
    private let padding: CGFloat = 32

    var body: some View {
        VStack(alignment: .leading, spacing: 24) {
            // Title
            Text("Set Summary")
                .font(.system(size: 40, weight: .bold))
                .foregroundColor(.black)

            // Aggregate stats section
            if stats.hasSummaryData {
                VStack(spacing: 12) {
                    if let absDev = stats.meanAbsoluteDeviation, let pctDev = stats.meanDeviation {
                        statRow("Avg. Deviation from Target", value: formatDeviation(absolute: absDev, percentage: pctDev), bold: true)
                    }
                    if let stdDev = stats.medianStdDev {
                        statRow("Standard Deviation", value: String(format: "%.2f %@", useLbs ? stdDev * AppConstants.kgToLbs : stdDev, useLbs ? "lbs" : "kg"))
                    }
                    if let target = stats.targetWeight {
                        statRow("Target", value: WeightFormatter.format(target, useLbs: useLbs))
                    }
                }

                divider
            }

            // All reps
            ForEach(Array(stats.reps.enumerated()), id: \.offset) { index, rep in
                repSection(index: index, rep: rep)

                if index < stats.reps.count - 1 {
                    divider
                }
            }
        }
        .padding(padding)
        .frame(width: Self.exportWidth)
        .background(Color.white)
    }

    private var divider: some View {
        Rectangle()
            .fill(Color.gray.opacity(0.3))
            .frame(height: 1)
            .padding(.vertical, 8)
    }

    private func repSection(index: Int, rep: RepResult) -> some View {
        VStack(alignment: .leading, spacing: 16) {
            // Rep header
            Text("Rep \(index + 1)")
                .font(.system(size: 24, weight: .semibold))
                .foregroundColor(.black)

            // Force graph
            let filter = rep.filterResult
            ShareableForceGraphView(
                samples: rep.samples,
                duration: rep.duration,
                targetWeight: rep.targetWeight,
                useLbs: useLbs,
                filterStartIndex: filter.startIndex,
                filterEndIndex: filter.endIndex
            )
            .frame(width: Self.exportWidth - padding * 2, height: graphHeight)

            // Rep stats
            VStack(spacing: 8) {
                statRow("Duration", value: String(format: "%.1fs", rep.duration))
                statRow("Median", value: WeightFormatter.format(rep.median, useLbs: useLbs))
                if let absDev = rep.absoluteDeviation, let pctDev = rep.deviationPercentage {
                    statRow("Difference from Target", value: formatDeviation(absolute: absDev, percentage: pctDev))
                }
            }
        }
    }

    private func statRow(_ label: String, value: String, bold: Bool = false) -> some View {
        HStack {
            Text(label)
                .foregroundColor(.gray)
            Spacer()
            Text(value)
                .fontWeight(bold ? .semibold : .medium)
        }
        .font(.system(size: 18))
        .foregroundColor(.black)
    }

    private func formatDeviation(absolute: Double, percentage: Double) -> String {
        let displayAbs = useLbs ? absolute * AppConstants.kgToLbs : absolute
        let unit = useLbs ? "lbs" : "kg"
        return String(format: "%+.2f %@ (%+.1f%%)", displayAbs, unit, percentage)
    }
}
