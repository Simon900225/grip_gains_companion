package app.grip_gains_companion.model

/**
 * Explicit state for the Progressor force handler
 */
sealed class ProgressorState {
    data object WaitingForSamples : ProgressorState()
    
    data class Calibrating(
        val startTimeMs: Long,
        val samples: List<TimestampedSample>
    ) : ProgressorState()
    
    data class Idle(
        val baselineValue: Double
    ) : ProgressorState()
    
    data class Gripping(
        val baselineValue: Double,
        val startTimestamp: Long,
        val samples: List<TimestampedSample>
    ) : ProgressorState()
    
    data class WeightCalibration(
        val baselineValue: Double,
        val samples: List<TimestampedSample>,
        val isHolding: Boolean
    ) : ProgressorState()
    
    // Convenience computed properties for UI
    val isCalibrating: Boolean
        get() = this is Calibrating
    
    val isWaitingForSamples: Boolean
        get() = this is WaitingForSamples
    
    val isEngaged: Boolean
        get() = this is Gripping
    
    val baseline: Double
        get() = when (this) {
            is Idle -> baselineValue
            is Gripping -> baselineValue
            is WeightCalibration -> baselineValue
            else -> 0.0
        }
}
