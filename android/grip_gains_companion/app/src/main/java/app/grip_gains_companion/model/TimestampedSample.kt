package app.grip_gains_companion.model

/**
 * A force sample with device timestamp
 */
data class TimestampedSample(
    val weight: Double,
    val timestamp: Long  // microseconds from device
)
