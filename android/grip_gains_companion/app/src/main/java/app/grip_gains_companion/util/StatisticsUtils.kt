package app.grip_gains_companion.util

import kotlin.math.sqrt

/**
 * Statistical utility functions
 */
object StatisticsUtils {
    
    fun mean(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        return values.sum() / values.size
    }
    
    fun median(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 0) {
            (sorted[mid - 1] + sorted[mid]) / 2.0
        } else {
            sorted[mid]
        }
    }
    
    fun standardDeviation(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        val mean = mean(values)
        val sumSquaredDiff = values.sumOf { (it - mean) * (it - mean) }
        return sqrt(sumSquaredDiff / values.size)
    }
    
    /**
     * Trimmed median - removes extreme values before calculating median
     * @param trimFraction Fraction of values to remove from each end (0.0-0.5)
     */
    fun trimmedMedian(values: List<Double>, trimFraction: Double = 0.3): Double {
        if (values.isEmpty()) return 0.0
        if (values.size < 3) return median(values)
        
        val sorted = values.sorted()
        val trimCount = (sorted.size * trimFraction).toInt()
        val trimmed = if (trimCount > 0 && sorted.size > trimCount * 2) {
            sorted.subList(trimCount, sorted.size - trimCount)
        } else {
            sorted
        }
        
        return median(trimmed)
    }
}
