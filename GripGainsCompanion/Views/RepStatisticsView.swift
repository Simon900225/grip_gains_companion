import SwiftUI

// MARK: - Shared Rep Statistics Components

/// Displays a labeled value row with optional bold styling
struct StatRowView: View {
    let label: String
    let value: String
    var bold: Bool = false

    var body: some View {
        HStack {
            Text(label)
                .foregroundColor(.secondary)
            Spacer()
            Text(value)
                .fontWeight(bold ? .medium : .regular)
        }
        .font(.subheadline)
    }
}

/// Compact horizontal grid showing percentile distribution
struct PercentilesGridView: View {
    let percentiles: [(String, Double)]
    let useLbs: Bool

    var body: some View {
        VStack(spacing: 2) {
            Text("Percentiles")
                .foregroundColor(.secondary)
                .frame(maxWidth: .infinity, alignment: .leading)

            HStack(spacing: 0) {
                ForEach(percentiles, id: \.0) { label, _ in
                    Text(label)
                        .frame(maxWidth: .infinity)
                }
            }
            .foregroundColor(.secondary)

            HStack(spacing: 0) {
                ForEach(percentiles, id: \.0) { _, value in
                    Text(WeightFormatter.format(value, useLbs: useLbs, includeUnit: false))
                        .frame(maxWidth: .infinity)
                }
            }
        }
        .font(.caption)
    }
}

/// Formats deviation values for display
enum DeviationFormatter {
    static func format(absolute: Double, percentage: Double, useLbs: Bool) -> String {
        let displayAbs = useLbs ? absolute * AppConstants.kgToLbs : absolute
        let unit = useLbs ? "lbs" : "kg"
        return String(format: "%+.2f %@ (%+.1f%%)", displayAbs, unit, percentage)
    }
}
