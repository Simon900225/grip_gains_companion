package app.grip_gains_companion.service

import app.grip_gains_companion.config.AppConstants
import app.grip_gains_companion.model.ForceHistoryEntry
import app.grip_gains_companion.model.ProgressorState
import app.grip_gains_companion.model.TimestampedSample
import app.grip_gains_companion.util.StatisticsUtils
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Date

/**
 * State machine for processing Tindeq Progressor force samples
 */
class ProgressorHandler {
    
    // MARK: - Published State
    
    private val _state = MutableStateFlow<ProgressorState>(ProgressorState.WaitingForSamples)
    val state: StateFlow<ProgressorState> = _state.asStateFlow()
    
    private val _currentForce = MutableStateFlow(0.0)
    val currentForce: StateFlow<Double> = _currentForce.asStateFlow()
    
    private val _calibrationTimeRemaining = MutableStateFlow(AppConstants.CALIBRATION_DURATION_MS)
    val calibrationTimeRemaining: StateFlow<Long> = _calibrationTimeRemaining.asStateFlow()
    
    private val _weightMedian = MutableStateFlow<Double?>(null)
    val weightMedian: StateFlow<Double?> = _weightMedian.asStateFlow()
    
    private val _sessionMean = MutableStateFlow<Double?>(null)
    val sessionMean: StateFlow<Double?> = _sessionMean.asStateFlow()
    
    private val _sessionStdDev = MutableStateFlow<Double?>(null)
    val sessionStdDev: StateFlow<Double?> = _sessionStdDev.asStateFlow()
    
    private val _forceHistory = MutableStateFlow<List<ForceHistoryEntry>>(emptyList())
    val forceHistory: StateFlow<List<ForceHistoryEntry>> = _forceHistory.asStateFlow()
    
    private val _isOffTarget = MutableStateFlow(false)
    val isOffTarget: StateFlow<Boolean> = _isOffTarget.asStateFlow()
    
    private val _offTargetDirection = MutableStateFlow<Double?>(null)
    val offTargetDirection: StateFlow<Double?> = _offTargetDirection.asStateFlow()
    
    // MARK: - Events (SharedFlow for one-shot events)
    
    private val _calibrationCompleted = MutableSharedFlow<Unit>()
    val calibrationCompleted = _calibrationCompleted.asSharedFlow()
    
    private val _gripFailed = MutableSharedFlow<Unit>()
    val gripFailed = _gripFailed.asSharedFlow()
    
    private val _gripDisengaged = MutableSharedFlow<Pair<Double, List<Double>>>()
    val gripDisengaged = _gripDisengaged.asSharedFlow()
    
    private val _offTargetChanged = MutableSharedFlow<Pair<Boolean, Double?>>()
    val offTargetChanged = _offTargetChanged.asSharedFlow()
    
    // MARK: - External Input
    
    var canEngage: Boolean = false
    var enableCalibration: Boolean = true
    var engageThreshold: Double = AppConstants.DEFAULT_ENGAGE_THRESHOLD
    var failThreshold: Double = AppConstants.DEFAULT_FAIL_THRESHOLD
    var targetWeight: Double? = null
    var weightTolerance: Double = AppConstants.DEFAULT_WEIGHT_TOLERANCE
    
    // Percentage-based thresholds
    var enablePercentageThresholds: Boolean = AppConstants.DEFAULT_ENABLE_PERCENTAGE_THRESHOLDS
    var engagePercentage: Double = AppConstants.DEFAULT_ENGAGE_PERCENTAGE
    var disengagePercentage: Double = AppConstants.DEFAULT_DISENGAGE_PERCENTAGE
    var tolerancePercentage: Double = AppConstants.DEFAULT_TOLERANCE_PERCENTAGE
    
    // Percentage threshold bounds
    var engageFloor: Double = AppConstants.DEFAULT_ENGAGE_FLOOR
    var engageCeiling: Double = AppConstants.DEFAULT_ENGAGE_CEILING
    var disengageFloor: Double = AppConstants.DEFAULT_DISENGAGE_FLOOR
    var disengageCeiling: Double = AppConstants.DEFAULT_DISENGAGE_CEILING
    var toleranceFloor: Double = AppConstants.DEFAULT_TOLERANCE_FLOOR
    var toleranceCeiling: Double = AppConstants.DEFAULT_TOLERANCE_CEILING
    
    var weightCalibrationThreshold: Double = AppConstants.DEFAULT_WEIGHT_CALIBRATION_THRESHOLD
    
    // Internal state
    private var lastTimestamp: Long = 0
    private var firstDeviceTimestamp: Long? = null
    private var firstDisplayTimestamp: Date? = null
    private var offTargetTimerRunning = false
    
    // Convenience properties
    val engaged: Boolean get() = _state.value.isEngaged
    val calibrating: Boolean get() = _state.value.isCalibrating
    val waitingForSamples: Boolean get() = _state.value.isWaitingForSamples
    
    val gripElapsedSeconds: Int
        get() {
            val currentState = _state.value
            return if (currentState is ProgressorState.Gripping) {
                ((lastTimestamp - currentState.startTimestamp) / 1_000_000).toInt()
            } else 0
        }
    
    // MARK: - Effective Thresholds
    
    private fun applyBounds(value: Double, floor: Double, ceiling: Double): Double {
        val floored = if (floor > 0) maxOf(value, floor) else value
        return if (ceiling > 0) minOf(floored, ceiling) else floored
    }
    
    private val effectiveEngageThreshold: Double
        get() {
            if (enablePercentageThresholds) {
                targetWeight?.let { target ->
                    return applyBounds(target * engagePercentage, engageFloor, engageCeiling)
                }
            }
            return engageThreshold
        }
    
    private val effectiveFailThreshold: Double
        get() {
            if (enablePercentageThresholds) {
                targetWeight?.let { target ->
                    return applyBounds(target * disengagePercentage, disengageFloor, disengageCeiling)
                }
            }
            return failThreshold
        }
    
    private val effectiveTolerance: Double
        get() {
            if (enablePercentageThresholds) {
                targetWeight?.let { target ->
                    return applyBounds(target * tolerancePercentage, toleranceFloor, toleranceCeiling)
                }
            }
            return weightTolerance
        }
    
    // MARK: - Public Methods
    
    /**
     * Process a single force sample from the BLE device
     */
    suspend fun processSample(rawWeight: Double, timestamp: Long) {
        _currentForce.value = rawWeight
        lastTimestamp = timestamp
        
        // Calculate display timestamp
        val displayTimestamp: Date
        val firstDevice = firstDeviceTimestamp
        val firstDisplay = firstDisplayTimestamp
        
        if (firstDevice != null && firstDisplay != null) {
            val offsetMicros = timestamp - firstDevice
            displayTimestamp = Date(firstDisplay.time + offsetMicros / 1000)
        } else {
            firstDeviceTimestamp = timestamp
            firstDisplayTimestamp = Date()
            displayTimestamp = firstDisplayTimestamp!!
        }
        
        // Update force history
        val newHistory = _forceHistory.value.toMutableList()
        newHistory.add(ForceHistoryEntry(displayTimestamp, rawWeight))
        _forceHistory.value = newHistory
        
        processStateTransition(rawWeight, timestamp)
    }
    
    /**
     * Reset handler state for a new session
     */
    fun reset() {
        resetCommonState()
        _currentForce.value = 0.0
    }
    
    /**
     * Trigger recalibration
     */
    fun recalibrate() {
        resetCommonState()
    }
    
    private fun resetCommonState() {
        stopOffTargetTimer()
        _state.value = ProgressorState.WaitingForSamples
        _calibrationTimeRemaining.value = AppConstants.CALIBRATION_DURATION_MS
        _weightMedian.value = null
        _isOffTarget.value = false
        _offTargetDirection.value = null
        _sessionMean.value = null
        _sessionStdDev.value = null
        _forceHistory.value = emptyList()
        firstDeviceTimestamp = null
        firstDisplayTimestamp = null
    }
    
    // MARK: - State Machine Logic
    
    private suspend fun processStateTransition(rawWeight: Double, timestamp: Long) {
        val sample = TimestampedSample(rawWeight, timestamp)
        
        when (val currentState = _state.value) {
            is ProgressorState.WaitingForSamples -> {
                _forceHistory.value = emptyList()
                if (enableCalibration) {
                    _state.value = ProgressorState.Calibrating(
                        startTimeMs = System.currentTimeMillis(),
                        samples = listOf(sample)
                    )
                    _calibrationTimeRemaining.value = AppConstants.CALIBRATION_DURATION_MS
                } else {
                    _state.value = ProgressorState.Idle(baselineValue = 0.0)
                    _calibrationTimeRemaining.value = 0
                    _calibrationCompleted.emit(Unit)
                }
            }
            
            is ProgressorState.Calibrating -> {
                val newSamples = currentState.samples + sample
                val elapsed = System.currentTimeMillis() - currentState.startTimeMs
                _calibrationTimeRemaining.value = maxOf(0, AppConstants.CALIBRATION_DURATION_MS - elapsed)
                
                if (elapsed >= AppConstants.CALIBRATION_DURATION_MS) {
                    val baselineAvg = newSamples.map { it.weight }.average()
                    _state.value = ProgressorState.Idle(baselineAvg)
                    _calibrationTimeRemaining.value = 0
                    _calibrationCompleted.emit(Unit)
                } else {
                    _state.value = ProgressorState.Calibrating(currentState.startTimeMs, newSamples)
                }
            }
            
            is ProgressorState.Idle -> {
                val taredWeight = rawWeight - currentState.baselineValue
                handleIdleState(rawWeight, taredWeight, currentState.baselineValue, timestamp)
            }
            
            is ProgressorState.Gripping -> {
                val newSamples = currentState.samples + sample
                val taredWeight = rawWeight - currentState.baselineValue
                
                // Calculate live statistics
                val weights = newSamples.map { it.weight }
                _sessionMean.value = StatisticsUtils.mean(weights)
                _sessionStdDev.value = StatisticsUtils.standardDeviation(weights)
                
                if (taredWeight < effectiveFailThreshold) {
                    // Grip failed
                    stopOffTargetTimer()
                    val duration = (timestamp - currentState.startTimestamp) / 1_000_000.0
                    _state.value = ProgressorState.Idle(currentState.baselineValue)
                    canEngage = false
                    _isOffTarget.value = false
                    _offTargetDirection.value = null
                    _gripFailed.emit(Unit)
                    _gripDisengaged.emit(Pair(duration, weights))
                } else {
                    _state.value = ProgressorState.Gripping(
                        currentState.baselineValue,
                        currentState.startTimestamp,
                        newSamples
                    )
                    checkOffTarget(rawWeight)
                }
            }
            
            is ProgressorState.WeightCalibration -> {
                val taredWeight = rawWeight - currentState.baselineValue
                handleWeightCalibrationState(
                    rawWeight, taredWeight, currentState.baselineValue,
                    currentState.samples, currentState.isHolding, timestamp
                )
            }
        }
    }
    
    private suspend fun handleIdleState(
        rawWeight: Double,
        taredWeight: Double,
        baseline: Double,
        timestamp: Long
    ) {
        val sample = TimestampedSample(rawWeight, timestamp)
        
        if (canEngage && taredWeight >= effectiveEngageThreshold) {
            // Start real grip session
            _weightMedian.value = null
            _state.value = ProgressorState.Gripping(baseline, timestamp, listOf(sample))
        } else if (!canEngage && taredWeight >= weightCalibrationThreshold) {
            // Start weight calibration
            _state.value = ProgressorState.WeightCalibration(baseline, listOf(sample), true)
            _weightMedian.value = rawWeight
        }
    }
    
    private fun handleWeightCalibrationState(
        rawWeight: Double,
        taredWeight: Double,
        baseline: Double,
        samples: List<TimestampedSample>,
        isHolding: Boolean,
        timestamp: Long
    ) {
        val sample = TimestampedSample(rawWeight, timestamp)
        
        if (canEngage && taredWeight >= effectiveEngageThreshold) {
            // Switch to real grip session
            _weightMedian.value = null
            _state.value = ProgressorState.Gripping(baseline, timestamp, listOf(sample))
        } else if (taredWeight >= weightCalibrationThreshold) {
            // Continue measuring
            if (isHolding) {
                val newSamples = samples + sample
                _weightMedian.value = StatisticsUtils.median(newSamples.map { it.weight })
                _state.value = ProgressorState.WeightCalibration(baseline, newSamples, true)
            } else {
                // Re-engaging weight
                _state.value = ProgressorState.WeightCalibration(baseline, listOf(sample), true)
                _weightMedian.value = rawWeight
            }
        } else if (taredWeight < weightCalibrationThreshold && isHolding) {
            // Put down weight - calculate final trimmed median
            _weightMedian.value = StatisticsUtils.trimmedMedian(samples.map { it.weight })
            _state.value = ProgressorState.WeightCalibration(baseline, samples, false)
        } else if (taredWeight < effectiveFailThreshold) {
            // Completely released - back to idle
            _state.value = ProgressorState.Idle(baseline)
        }
    }
    
    // MARK: - Target Weight Checking
    
    private suspend fun checkOffTarget(rawWeight: Double) {
        val target = targetWeight ?: run {
            stopOffTargetTimer()
            if (_isOffTarget.value) {
                _isOffTarget.value = false
                _offTargetDirection.value = null
                _offTargetChanged.emit(Pair(false, null))
            }
            return
        }
        
        val difference = rawWeight - target
        val wasOffTarget = _isOffTarget.value
        
        if (kotlin.math.abs(difference) >= effectiveTolerance) {
            _isOffTarget.value = true
            _offTargetDirection.value = difference
            if (!wasOffTarget) {
                // Just went off target - emit event
                _offTargetChanged.emit(Pair(true, difference))
            }
        } else {
            stopOffTargetTimer()
            _isOffTarget.value = false
            _offTargetDirection.value = null
            if (wasOffTarget) {
                _offTargetChanged.emit(Pair(false, null))
            }
        }
    }
    
    private fun stopOffTargetTimer() {
        offTargetTimerRunning = false
    }
}
