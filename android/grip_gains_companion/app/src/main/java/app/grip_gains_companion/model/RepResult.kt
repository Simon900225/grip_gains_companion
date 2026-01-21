package app.grip_gains_companion.model

import app.grip_gains_companion.util.StatisticsUtils
import java.util.Date

/**
 * Result of a single rep (grip hold)
 */
data class RepResult(
    val timestamp: Date,
    val duration: Double,  // seconds
    val samples: List<Double>,
    val targetWeight: Double?
) {
    val meanForce: Double
        get() = StatisticsUtils.mean(samples)
    
    val stdDev: Double
        get() = StatisticsUtils.standardDeviation(samples)
    
    val maxForce: Double
        get() = samples.maxOrNull() ?: 0.0
    
    val minForce: Double
        get() = samples.minOrNull() ?: 0.0
}
