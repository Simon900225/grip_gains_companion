import Foundation

/// Aggregated statistics for a complete set (multiple reps)
struct SetStatistics {
    let reps: [RepResult]

    var totalReps: Int { reps.count }

    // MARK: - Duration Statistics

    var totalDuration: TimeInterval {
        reps.reduce(0) { $0 + $1.duration }
    }

    // MARK: - Target Weight

    /// Target weight (from the first rep that has one)
    var targetWeight: Double? {
        reps.first(where: { $0.targetWeight != nil })?.targetWeight
    }

    // MARK: - Median Statistics

    /// Mean of rep medians (for calculating absolute std dev)
    var medianMean: Double? {
        let medians = reps.map(\.median)
        guard !medians.isEmpty else { return nil }
        return medians.reduce(0, +) / Double(medians.count)
    }

    /// Average of rep standard deviations (mean force stability across the set)
    var averageStdDev: Double? {
        let stdDevs = reps.map(\.stdDev)
        guard !stdDevs.isEmpty else { return nil }
        return stdDevs.reduce(0, +) / Double(stdDevs.count)
    }

    // MARK: - Deviation from Target Statistics

    /// Mean absolute deviation from target (in kg)
    var meanAbsoluteDeviation: Double? {
        let validDeviations = reps.compactMap(\.absoluteDeviation)
        guard !validDeviations.isEmpty else { return nil }
        return validDeviations.reduce(0, +) / Double(validDeviations.count)
    }

    /// Mean deviation from target across all reps (%)
    var meanDeviation: Double? {
        let validDeviations = reps.compactMap(\.deviationPercentage)
        guard !validDeviations.isEmpty else { return nil }
        return validDeviations.reduce(0, +) / Double(validDeviations.count)
    }

    /// Whether the summary section has any meaningful data to display
    var hasSummaryData: Bool {
        meanAbsoluteDeviation != nil || averageStdDev != nil || targetWeight != nil
    }
}
