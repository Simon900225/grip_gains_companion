import SwiftUI
import Charts

/// Export-optimized view for sharing a single rep as an image
struct ShareableRepView: View {
    let rep: RepResult
    let repNumber: Int
    let useLbs: Bool

    static let exportWidth: CGFloat = 1080
    private let graphHeight: CGFloat = 400
    private let padding: CGFloat = 32

    var body: some View {
        VStack(alignment: .leading, spacing: 24) {
            // Rep header
            Text("Rep \(repNumber)")
                .font(.system(size: 36, weight: .bold))
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

            // Stats
            VStack(spacing: 12) {
                statRow("Duration", value: String(format: "%.1fs", rep.duration))
                statRow("Median", value: WeightFormatter.format(rep.median, useLbs: useLbs))
                if let absDev = rep.absoluteDeviation, let pctDev = rep.deviationPercentage {
                    statRow("Difference from Target", value: formatDeviation(absolute: absDev, percentage: pctDev))
                }
            }
        }
        .padding(padding)
        .frame(width: Self.exportWidth)
        .background(Color.white)
    }

    private func statRow(_ label: String, value: String) -> some View {
        HStack {
            Text(label)
                .foregroundColor(.gray)
            Spacer()
            Text(value)
                .fontWeight(.medium)
        }
        .font(.system(size: 20))
        .foregroundColor(.black)
    }

    private func formatDeviation(absolute: Double, percentage: Double) -> String {
        let displayAbs = useLbs ? absolute * AppConstants.kgToLbs : absolute
        let unit = useLbs ? "lbs" : "kg"
        return String(format: "%+.2f %@ (%+.1f%%)", displayAbs, unit, percentage)
    }
}

/// Force graph optimized for image export with explicit colors (no environment dependencies)
struct ShareableForceGraphView: View {
    let samples: [Double]
    let duration: TimeInterval
    let targetWeight: Double?
    let useLbs: Bool
    let filterStartIndex: Int
    let filterEndIndex: Int

    private func displayForce(_ force: Double) -> Double {
        useLbs ? force * AppConstants.kgToLbs : force
    }

    private func elapsedTime(for index: Int) -> Double {
        guard samples.count > 1 else { return 0 }
        return duration * Double(index) / Double(samples.count - 1)
    }

    var body: some View {
        Chart {
            // Raw samples (gray, thin)
            ForEach(Array(samples.enumerated()), id: \.offset) { index, force in
                LineMark(
                    x: .value("Time", elapsedTime(for: index)),
                    y: .value("Force", displayForce(force)),
                    series: .value("Series", "raw")
                )
                .foregroundStyle(Color.gray.opacity(0.4))
                .lineStyle(StrokeStyle(lineWidth: 1.5))
            }

            // Filtered samples (blue, thick)
            ForEach(Array(samples.enumerated()), id: \.offset) { index, force in
                if index >= filterStartIndex && index <= filterEndIndex {
                    LineMark(
                        x: .value("Time", elapsedTime(for: index)),
                        y: .value("Force", displayForce(force)),
                        series: .value("Series", "filtered")
                    )
                    .foregroundStyle(Color.blue)
                    .lineStyle(StrokeStyle(lineWidth: 3))
                }
            }

            // Target weight line
            if let target = targetWeight {
                RuleMark(y: .value("Target", displayForce(target)))
                    .foregroundStyle(Color.green.opacity(0.8))
                    .lineStyle(StrokeStyle(lineWidth: 2, dash: [8, 4]))
            }
        }
        .chartXAxis {
            AxisMarks(values: .automatic(desiredCount: 5)) { value in
                AxisGridLine(stroke: StrokeStyle(lineWidth: 0.5))
                    .foregroundStyle(Color.gray.opacity(0.3))
                AxisValueLabel {
                    if let seconds = value.as(Double.self) {
                        Text(String(format: "%.0fs", seconds))
                            .font(.system(size: 14))
                            .foregroundColor(.gray)
                    }
                }
            }
        }
        .chartYAxis {
            AxisMarks(position: .leading) { value in
                AxisGridLine(stroke: StrokeStyle(lineWidth: 0.5))
                    .foregroundStyle(Color.gray.opacity(0.3))
                AxisValueLabel {
                    if let force = value.as(Double.self) {
                        Text("\(Int(force))")
                            .font(.system(size: 14))
                            .foregroundColor(.gray)
                    }
                }
            }
        }
        .chartYScale(domain: yAxisDomain)
        .chartPlotStyle { plotArea in
            plotArea.background(Color.white)
        }
    }

    private var yAxisDomain: ClosedRange<Double> {
        let forces = samples.map { displayForce($0) }
        var lower = forces.min() ?? 0
        var upper = forces.max() ?? 10

        if let target = targetWeight {
            let targetDisplay = displayForce(target)
            lower = min(lower, targetDisplay)
            upper = max(upper, targetDisplay)
        }

        let range = upper - lower
        let padding = max(range * 0.15, 2.0)
        return max(0, lower - padding)...(upper + padding)
    }
}
