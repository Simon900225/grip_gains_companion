package app.grip_gains_companion.model

import app.grip_gains_companion.util.StatisticsUtils

/**
 * Statistics for a complete set (multiple reps)
 */
data class SetStatistics(
    val reps: List<RepResult>
) {
    val repCount: Int
        get() = reps.size
    
    val totalDuration: Double
        get() = reps.sumOf { it.duration }
    
    val averageDuration: Double
        get() = if (reps.isEmpty()) 0.0 else totalDuration / reps.size
    
    val allSamples: List<Double>
        get() = reps.flatMap { it.samples }
    
    val overallMean: Double
        get() = StatisticsUtils.mean(allSamples)
    
    val overallStdDev: Double
        get() = StatisticsUtils.standardDeviation(allSamples)
    
    val maxForce: Double
        get() = reps.maxOfOrNull { it.maxForce } ?: 0.0
    
    val minForce: Double
        get() = reps.filter { it.samples.isNotEmpty() }.minOfOrNull { it.minForce } ?: 0.0
}
