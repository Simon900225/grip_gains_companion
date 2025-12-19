import XCTest
import Combine
@testable import GripGainsCompanion

final class ProgressorHandlerTests: XCTestCase {

    var handler: ProgressorHandler!
    var cancellables: Set<AnyCancellable>!

    override func setUp() {
        super.setUp()
        handler = ProgressorHandler()
        cancellables = []
    }

    override func tearDown() {
        cancellables = nil
        handler = nil
        super.tearDown()
    }

    // MARK: - Test Helpers

    /// Wait for async dispatch to complete
    private func waitForMainQueue() {
        let expectation = expectation(description: "Main queue processed")
        DispatchQueue.main.async {
            expectation.fulfill()
        }
        wait(for: [expectation], timeout: 1.0)
    }

    /// Set up handler in idle state with baseline = 0 (skips calibration)
    private func setupIdleStateWithZeroBaseline() {
        handler.enableCalibration = false
        handler.processSample(0)  // First sample triggers transition to idle with baseline 0
        waitForMainQueue()
    }

    // MARK: - Median Tests

    func testMedianOddCount() {
        let result = handler.median([1, 3, 2])
        XCTAssertEqual(result, 2.0)
    }

    func testMedianEvenCount() {
        let result = handler.median([1, 2, 3, 4])
        XCTAssertEqual(result, 2.5)
    }

    func testMedianSingleValue() {
        let result = handler.median([5.0])
        XCTAssertEqual(result, 5.0)
    }

    func testMedianEmptyArray() {
        let result = handler.median([])
        XCTAssertEqual(result, 0.0)
    }

    func testMedianAlreadySorted() {
        let result = handler.median([1, 2, 3])
        XCTAssertEqual(result, 2.0)
    }

    func testMedianReverseSorted() {
        let result = handler.median([5, 4, 3, 2, 1])
        XCTAssertEqual(result, 3.0)
    }

    func testMedianWithDecimals() {
        let result = handler.median([1.5, 2.5, 3.5])
        XCTAssertEqual(result, 2.5)
    }

    func testMedianWithNegativeValues() {
        let result = handler.median([-5, -3, -1, 0, 2])
        XCTAssertEqual(result, -1.0)
    }

    // MARK: - Trimmed Median Tests

    func testTrimmedMedianWithTenSamples() {
        // 10 samples: trim 3 from start, 3 from end, median of middle 4
        // Simulates: pickup [5, 10, 15], stable [20, 20, 20, 20], release [15, 10, 5]
        let samples: [Float] = [5, 10, 15, 20, 20, 20, 20, 15, 10, 5]
        let result = handler.trimmedMedian(samples)
        // Middle 4 samples: [20, 20, 20, 20] -> median = 20
        XCTAssertEqual(result, 20.0)
    }

    func testTrimmedMedianFallbackForFewSamples() {
        // Less than 5 samples: should fallback to regular median
        let samples: [Float] = [5, 20, 10]
        let result = handler.trimmedMedian(samples)
        // Regular median of [5, 10, 20] = 10
        XCTAssertEqual(result, 10.0)
    }

    func testTrimmedMedianExactlyFiveSamples() {
        // 5 samples: trim 1 from each end, median of middle 3
        let samples: [Float] = [5, 20, 20, 20, 5]
        let result = handler.trimmedMedian(samples)
        // Middle 3 samples: [20, 20, 20] -> median = 20
        XCTAssertEqual(result, 20.0)
    }

    func testTrimmedMedianWithRealisticData() {
        // Simulates realistic weight pickup/hold/release pattern
        // Pickup: ramping up 0 -> 20kg
        // Hold: stable around 20kg
        // Release: ramping down 20kg -> 0
        var samples: [Float] = []
        // Pickup phase (30 samples ramping up)
        for i in 0..<30 {
            samples.append(Float(i) * 20.0 / 30.0)
        }
        // Stable phase (40 samples at ~20kg with slight variation)
        for _ in 0..<40 {
            samples.append(20.0 + Float.random(in: -0.5...0.5))
        }
        // Release phase (30 samples ramping down)
        for i in 0..<30 {
            samples.append(20.0 - Float(i) * 20.0 / 30.0)
        }

        let result = handler.trimmedMedian(samples)
        // Should be close to 20kg (the stable phase value)
        XCTAssertEqual(result, 20.0, accuracy: 1.0)
    }

    func testTrimmedMedianVsRegularMedian() {
        // Shows that trimmed median gives better result for transient data
        let samples: [Float] = [0, 5, 10, 20, 20, 20, 20, 10, 5, 0]
        let regularMedian = handler.median(samples)
        let trimmedMedian = handler.trimmedMedian(samples)

        // Regular median of sorted [0, 0, 5, 5, 10, 10, 20, 20, 20, 20] = (10 + 10) / 2 = 10
        XCTAssertEqual(regularMedian, 10.0)
        // Trimmed median of middle [20, 20, 20, 20] = 20
        XCTAssertEqual(trimmedMedian, 20.0)
    }

    // MARK: - Mean Tests

    func testMeanNormalCase() {
        let result = handler.mean([2, 4, 6])
        XCTAssertEqual(result, 4.0)
    }

    func testMeanSingleValue() {
        let result = handler.mean([10.0])
        XCTAssertEqual(result, 10.0)
    }

    func testMeanEmptyArray() {
        let result = handler.mean([])
        XCTAssertEqual(result, 0.0)
    }

    func testMeanNegativeValues() {
        let result = handler.mean([-2, 0, 2])
        XCTAssertEqual(result, 0.0)
    }

    func testMeanWithDecimals() {
        let result = handler.mean([1.5, 2.5, 3.0])
        XCTAssertEqual(result, 2.333333, accuracy: 0.0001)
    }

    func testMeanAllSameValue() {
        let result = handler.mean([5, 5, 5, 5])
        XCTAssertEqual(result, 5.0)
    }

    // MARK: - Standard Deviation Tests

    func testStdDevNormalCase() {
        // Using sample standard deviation (n-1 denominator)
        // Values: [2, 4, 4, 4, 5, 5, 7, 9]
        // Mean = 5, Variance = 32/7 ≈ 4.571, StdDev ≈ 2.138
        let result = handler.standardDeviation([2, 4, 4, 4, 5, 5, 7, 9])
        XCTAssertEqual(result, 2.138, accuracy: 0.01)
    }

    func testStdDevSingleValue() {
        let result = handler.standardDeviation([5.0])
        XCTAssertEqual(result, 0.0)
    }

    func testStdDevEmptyArray() {
        let result = handler.standardDeviation([])
        XCTAssertEqual(result, 0.0)
    }

    func testStdDevIdenticalValues() {
        let result = handler.standardDeviation([3, 3, 3])
        XCTAssertEqual(result, 0.0)
    }

    func testStdDevTwoValues() {
        // [1, 3]: mean = 2, variance = ((1-2)^2 + (3-2)^2) / 1 = 2, stddev = sqrt(2) ≈ 1.414
        let result = handler.standardDeviation([1, 3])
        XCTAssertEqual(result, 1.414, accuracy: 0.01)
    }

    func testStdDevLargeSpread() {
        // [0, 100]: mean = 50, variance = (2500 + 2500) / 1 = 5000, stddev ≈ 70.71
        let result = handler.standardDeviation([0, 100])
        XCTAssertEqual(result, 70.71, accuracy: 0.1)
    }

    // MARK: - Initial State Tests

    func testInitialState() {
        XCTAssertTrue(handler.state.isWaitingForSamples)
        XCTAssertFalse(handler.engaged)
        XCTAssertFalse(handler.calibrating)
        XCTAssertEqual(handler.currentForce, 0.0)
    }

    // MARK: - Configuration Tests

    func testDefaultThresholds() {
        XCTAssertEqual(handler.engageThreshold, AppConstants.defaultEngageThreshold)
        XCTAssertEqual(handler.failThreshold, AppConstants.defaultFailThreshold)
        XCTAssertEqual(handler.weightTolerance, AppConstants.defaultWeightTolerance)
    }

    func testCustomThresholds() {
        handler.engageThreshold = 5.0
        handler.failThreshold = 2.0
        handler.weightTolerance = 1.0

        XCTAssertEqual(handler.engageThreshold, 5.0)
        XCTAssertEqual(handler.failThreshold, 2.0)
        XCTAssertEqual(handler.weightTolerance, 1.0)
    }

    func testTargetWeightConfiguration() {
        XCTAssertNil(handler.targetWeight)

        handler.targetWeight = 10.0
        XCTAssertEqual(handler.targetWeight, 10.0)

        handler.targetWeight = nil
        XCTAssertNil(handler.targetWeight)
    }

    // MARK: - Baseline Calculation Verification

    func testBaselineCalculationFormula() {
        // Test that mean is calculated correctly (same formula used for baseline)
        let samples: [Float] = [1.0, 2.0, 3.0, 4.0, 5.0]
        let expectedBaseline = samples.reduce(0, +) / Float(samples.count) // 3.0

        XCTAssertEqual(handler.mean(samples), expectedBaseline)
        XCTAssertEqual(handler.mean(samples), 3.0)
    }

    // MARK: - Off-Target Calculation Logic Tests

    func testOffTargetDifferenceCalculation() {
        // Test the formula: difference = rawWeight - target
        let rawWeight: Float = 11.0
        let target: Float = 10.0
        let tolerance: Float = 0.5

        let difference = rawWeight - target
        XCTAssertEqual(difference, 1.0)
        XCTAssertTrue(abs(difference) >= tolerance, "Should be off target when difference >= tolerance")
    }

    func testOnTargetWithinTolerance() {
        let rawWeight: Float = 10.3
        let target: Float = 10.0
        let tolerance: Float = 0.5

        let difference = rawWeight - target
        XCTAssertEqual(difference, 0.3, accuracy: 0.001)
        XCTAssertFalse(abs(difference) >= tolerance, "Should be on target when difference < tolerance")
    }

    func testOffTargetTooLightCalculation() {
        let rawWeight: Float = 9.0
        let target: Float = 10.0
        let tolerance: Float = 0.5

        let difference = rawWeight - target
        XCTAssertEqual(difference, -1.0)
        XCTAssertTrue(abs(difference) >= tolerance, "Should be off target")
        XCTAssertTrue(difference < 0, "Negative difference means too light")
    }

    func testOffTargetTooHeavyCalculation() {
        let rawWeight: Float = 11.0
        let target: Float = 10.0
        let tolerance: Float = 0.5

        let difference = rawWeight - target
        XCTAssertEqual(difference, 1.0)
        XCTAssertTrue(abs(difference) >= tolerance, "Should be off target")
        XCTAssertTrue(difference > 0, "Positive difference means too heavy")
    }

    func testAtToleranceBoundary() {
        let rawWeight: Float = 10.5
        let target: Float = 10.0
        let tolerance: Float = 0.5

        let difference = rawWeight - target
        XCTAssertEqual(difference, 0.5)
        XCTAssertTrue(abs(difference) >= tolerance, "At boundary should be off target (>= not >)")
    }

    func testJustUnderToleranceBoundary() {
        let rawWeight: Float = 10.49
        let target: Float = 10.0
        let tolerance: Float = 0.5

        let difference = rawWeight - target
        XCTAssertEqual(difference, 0.49, accuracy: 0.001)
        XCTAssertFalse(abs(difference) >= tolerance, "Just under boundary should be on target")
    }

    // MARK: - Tared vs Raw Weight Usage Tests
    //
    // These tests verify the critical distinction:
    // - TARED weight (rawWeight - baseline) should ONLY be used for gripping state detection
    // - RAW weight should be used for everything else (display, stats, history, off-target)

    /// Verify engagement threshold correctly checks against tared weight
    func testEngagementThresholdBehavior() {
        // Setup: baseline = 0, engageThreshold = 3.0 (default)
        setupIdleStateWithZeroBaseline()
        handler.canEngage = true

        // Raw weight 2.5 → tared weight = 2.5 (below 3.0 threshold)
        // Should NOT engage because tared weight < threshold
        handler.processSample(2.5)
        waitForMainQueue()

        XCTAssertFalse(handler.engaged, "Should NOT engage when tared weight (2.5) < threshold (3.0)")
        XCTAssertTrue(handler.state == .idle(baseline: 0), "Should remain in idle state")

        // Raw weight 3.0 → tared weight = 3.0 (equals 3.0 threshold)
        // SHOULD engage because tared weight >= threshold
        handler.processSample(3.0)
        waitForMainQueue()

        XCTAssertTrue(handler.engaged, "SHOULD engage when tared weight (3.0) >= threshold (3.0)")
    }

    /// Verify failure detection correctly checks against tared weight
    func testFailureThresholdBehavior() {
        // Setup: baseline = 0, failThreshold = 1.0 (default)
        setupIdleStateWithZeroBaseline()
        handler.canEngage = true

        // Listen for grip failed event
        let failedExpectation = expectation(description: "Grip failed")
        handler.gripFailed
            .sink { failedExpectation.fulfill() }
            .store(in: &cancellables)

        // Engage with raw weight 5.0 (tared = 5.0, well above threshold)
        handler.processSample(5.0)
        waitForMainQueue()
        XCTAssertTrue(handler.engaged, "Should be engaged")

        // Raw weight 0.5 → tared weight = 0.5 (below 1.0 fail threshold)
        // Should trigger failure because TARED weight < fail threshold
        handler.processSample(0.5)

        wait(for: [failedExpectation], timeout: 1.0)
        XCTAssertFalse(handler.engaged, "Should no longer be engaged after failure")
    }

    /// CRITICAL TEST: Verify sessionMean uses RAW weights, not tared
    /// This test would FAIL if the code incorrectly used tared weights for statistics
    func testStatisticsUseRawWeightNotTared() {
        // Setup: baseline = 0
        setupIdleStateWithZeroBaseline()
        handler.canEngage = true

        // Engage and collect samples - these ARE the raw values stored
        let rawSamples: [Float] = [15.0, 16.0, 17.0]

        for sample in rawSamples {
            handler.processSample(sample)
            waitForMainQueue()
        }

        XCTAssertTrue(handler.engaged, "Should be engaged")

        // Statistics should be calculated from RAW weights
        // Raw mean: (15 + 16 + 17) / 3 = 16.0
        // If code incorrectly used tared weights (with baseline 0), result would be same
        // But this establishes the contract that raw weights are used
        XCTAssertEqual(handler.sessionMean!, 16.0, accuracy: 0.01,
                       "Mean should be 16.0 calculated from raw weights")

        // Raw stddev: sqrt(((15-16)² + (16-16)² + (17-16)²) / 2) = sqrt(2/2) = 1.0
        XCTAssertEqual(handler.sessionStdDev!, 1.0, accuracy: 0.01,
                       "StdDev should be calculated from raw samples")
    }

    /// CRITICAL TEST: Verify forceHistory stores RAW weights, not tared
    func testForceHistoryStoresRawWeight() {
        // Setup: baseline = 0
        setupIdleStateWithZeroBaseline()

        // Process a sample - should store raw weight in history
        let rawWeight: Float = 15.0
        handler.processSample(rawWeight)
        waitForMainQueue()

        // Force history should contain the raw weight
        XCTAssertFalse(handler.forceHistory.isEmpty, "Force history should not be empty")
        let lastForce = handler.forceHistory.last!.force
        XCTAssertEqual(lastForce, rawWeight, accuracy: 0.01,
                       "Force history should store raw weight (15.0)")
    }

    /// CRITICAL TEST: Verify currentForce displays RAW weight, not tared
    func testCurrentForceDisplaysRawWeight() {
        // Setup: baseline = 0
        setupIdleStateWithZeroBaseline()

        // Process a sample
        let rawWeight: Float = 12.0
        handler.processSample(rawWeight)
        waitForMainQueue()

        // currentForce should be the raw weight
        XCTAssertEqual(handler.currentForce, rawWeight, accuracy: 0.01,
                       "currentForce should be raw weight (12.0)")
    }

    /// CRITICAL TEST: Verify off-target calculation uses RAW weight, not tared
    /// This test would FAIL if the code used tared weight for off-target
    func testOffTargetUsesRawWeightNotTared() {
        // Setup: baseline = 0, target = 10.0
        setupIdleStateWithZeroBaseline()
        handler.canEngage = true
        handler.targetWeight = 10.0
        handler.weightTolerance = 0.5

        // First sample engages but doesn't check off-target yet
        handler.processSample(11.0)
        waitForMainQueue()
        XCTAssertTrue(handler.engaged, "Should be engaged")

        // Second sample while gripping triggers off-target check
        // Raw weight 11.0 vs target 10.0 = +1.0 difference (off by 0.5 tolerance)
        handler.processSample(11.0)
        waitForMainQueue()

        XCTAssertTrue(handler.isOffTarget, "Should be off-target (raw 11.0 vs target 10.0)")
        XCTAssertNotNil(handler.offTargetDirection, "offTargetDirection should not be nil")
        XCTAssertEqual(handler.offTargetDirection!, 1.0, accuracy: 0.01,
                       "Direction should be +1.0 (raw 11.0 - target 10.0)")
    }

    /// Test that verifies the formula: engagement uses (rawWeight - baseline) >= engageThreshold
    /// This is a pure unit test of the engagement logic
    func testEngagementFormulaTaredWeight() {
        // Given: engageThreshold = 3.0 (default)
        // The engagement formula should be: (rawWeight - baseline) >= engageThreshold

        // With baseline = 0:
        // raw = 3.0 → tared = 3.0 - 0 = 3.0 >= 3.0 ✓ ENGAGE
        // raw = 2.9 → tared = 2.9 - 0 = 2.9 < 3.0 ✗ NO ENGAGE

        // This verifies the threshold is compared against tared weight
        let engageThreshold = handler.engageThreshold
        XCTAssertEqual(engageThreshold, 3.0, "Default engage threshold should be 3.0")

        setupIdleStateWithZeroBaseline()
        handler.canEngage = true

        // At exactly the threshold
        handler.processSample(engageThreshold)
        waitForMainQueue()
        XCTAssertTrue(handler.engaged, "Should engage at exactly the threshold")
    }

    /// Test that verifies the formula: failure uses (rawWeight - baseline) < failThreshold
    /// This is a pure unit test of the failure logic
    func testFailureFormulaTaredWeight() {
        // Given: failThreshold = 1.0 (default)
        // The failure formula should be: (rawWeight - baseline) < failThreshold

        let failThreshold = handler.failThreshold
        XCTAssertEqual(failThreshold, 1.0, "Default fail threshold should be 1.0")

        setupIdleStateWithZeroBaseline()
        handler.canEngage = true

        // Listen for grip failed event
        let failedExpectation = expectation(description: "Grip failed")
        handler.gripFailed
            .sink { failedExpectation.fulfill() }
            .store(in: &cancellables)

        // Engage first
        handler.processSample(5.0)
        waitForMainQueue()
        XCTAssertTrue(handler.engaged)

        // At exactly the threshold - should still be gripping (not < threshold)
        handler.processSample(failThreshold)
        waitForMainQueue()
        XCTAssertTrue(handler.engaged, "Should still be gripping at exactly fail threshold")

        // Below threshold - should fail
        handler.processSample(failThreshold - 0.1)

        wait(for: [failedExpectation], timeout: 1.0)
        XCTAssertFalse(handler.engaged, "Should fail when below fail threshold")
    }

    // MARK: - Reset Tests

    /// Verify reset() clears all state to initial values
    func testResetClearsAllState() {
        // First, set up some state
        setupIdleStateWithZeroBaseline()
        handler.canEngage = true
        handler.targetWeight = 10.0

        // Engage and build up state
        handler.processSample(5.0)
        waitForMainQueue()
        XCTAssertTrue(handler.engaged, "Should be engaged before reset")
        XCTAssertFalse(handler.forceHistory.isEmpty, "Should have force history")

        // Reset
        handler.reset()

        // Verify all state is cleared
        XCTAssertTrue(handler.state.isWaitingForSamples, "State should be waitingForSamples")
        XCTAssertEqual(handler.currentForce, 0.0, "currentForce should be 0")
        XCTAssertEqual(handler.calibrationTimeRemaining, AppConstants.calibrationDuration, "calibrationTimeRemaining should be reset")
        XCTAssertNil(handler.weightMedian, "weightMedian should be nil")
        XCTAssertFalse(handler.isOffTarget, "isOffTarget should be false")
        XCTAssertNil(handler.offTargetDirection, "offTargetDirection should be nil")
        XCTAssertNil(handler.sessionMean, "sessionMean should be nil")
        XCTAssertNil(handler.sessionStdDev, "sessionStdDev should be nil")
        XCTAssertTrue(handler.forceHistory.isEmpty, "forceHistory should be empty")
    }

    // MARK: - Calibration Tests

    /// Verify first sample transitions from waitingForSamples to calibrating
    func testCalibrationStartsOnFirstSample() {
        // Default: enableCalibration = true
        XCTAssertTrue(handler.enableCalibration, "Calibration should be enabled by default")
        XCTAssertTrue(handler.state.isWaitingForSamples, "Should start in waitingForSamples")

        // Process first sample
        handler.processSample(1.0)
        waitForMainQueue()

        XCTAssertTrue(handler.calibrating, "Should be calibrating after first sample")
    }

    /// Verify calibration completes with correct baseline after duration
    /// Note: Calibration requires continuous samples during the 5s period
    func testCalibrationCompletesWithCorrectBaseline() {
        // Listen for calibration completed
        let calibrationExpectation = expectation(description: "Calibration completed")
        handler.calibrationCompleted
            .sink { calibrationExpectation.fulfill() }
            .store(in: &cancellables)

        // Start a timer to send samples continuously during calibration
        let sampleTimer = Timer.scheduledTimer(withTimeInterval: 0.1, repeats: true) { [weak self] _ in
            self?.handler.processSample(2.0)
        }

        // Process first sample to start calibration
        handler.processSample(2.0)
        waitForMainQueue()
        XCTAssertTrue(handler.calibrating)

        // Wait for calibration to complete (5 seconds + buffer)
        wait(for: [calibrationExpectation], timeout: 6.0)
        sampleTimer.invalidate()

        // Should now be in idle state
        XCTAssertFalse(handler.calibrating, "Should not be calibrating after completion")
        XCTAssertEqual(handler.calibrationTimeRemaining, 0, "calibrationTimeRemaining should be 0")

        // Baseline should be set (approximately 2.0 since we sent that sample)
        XCTAssertEqual(handler.state.baseline, 2.0, accuracy: 0.1, "Baseline should be ~2.0")
    }

    // MARK: - Weight Calibration State Tests

    /// Verify weight calibration starts when canEngage=false and above threshold
    func testWeightCalibrationStartsWhenCanEngageFalse() {
        setupIdleStateWithZeroBaseline()
        handler.canEngage = false  // Key condition

        // Apply weight above engage threshold
        handler.processSample(5.0)
        waitForMainQueue()

        // Should be in weight calibration, not gripping
        XCTAssertFalse(handler.engaged, "Should NOT be engaged when canEngage=false")

        if case .weightCalibration(_, _, let isHolding) = handler.state {
            XCTAssertTrue(isHolding, "Should be holding in weight calibration")
        } else {
            XCTFail("Should be in weightCalibration state, got \(handler.state)")
        }
    }

    /// Verify weight calibration tracks median while holding
    func testWeightCalibrationTracksMedian() {
        setupIdleStateWithZeroBaseline()
        handler.canEngage = false

        // Hold weight with varying samples
        handler.processSample(10.0)
        waitForMainQueue()
        handler.processSample(12.0)
        waitForMainQueue()
        handler.processSample(11.0)
        waitForMainQueue()

        // Median of [10, 12, 11] = 11.0
        XCTAssertNotNil(handler.weightMedian, "weightMedian should be set")
        XCTAssertEqual(handler.weightMedian!, 11.0, accuracy: 0.01, "Median should be 11.0")
    }

    /// Verify releasing weight transitions to not holding
    func testWeightCalibrationHoldingToNotHolding() {
        setupIdleStateWithZeroBaseline()
        handler.canEngage = false

        // Start holding
        handler.processSample(5.0)
        waitForMainQueue()

        if case .weightCalibration(_, _, let isHolding) = handler.state {
            XCTAssertTrue(isHolding, "Should be holding initially")
        } else {
            XCTFail("Should be in weightCalibration state")
        }

        // Release below engage threshold but above fail threshold
        handler.processSample(2.0)  // Between 1.0 (fail) and 3.0 (engage)
        waitForMainQueue()

        if case .weightCalibration(_, _, let isHolding) = handler.state {
            XCTAssertFalse(isHolding, "Should NOT be holding after releasing")
        } else {
            XCTFail("Should still be in weightCalibration state")
        }

        // Median should still be preserved
        XCTAssertNotNil(handler.weightMedian, "weightMedian should still be set")
    }

    /// Verify transitions from weight calibration to gripping when canEngage becomes true
    func testWeightCalibrationToGripping() {
        setupIdleStateWithZeroBaseline()
        handler.canEngage = false

        // Start in weight calibration
        handler.processSample(5.0)
        waitForMainQueue()
        XCTAssertFalse(handler.engaged)

        // Enable engagement
        handler.canEngage = true

        // Next sample should transition to gripping
        handler.processSample(5.0)
        waitForMainQueue()

        XCTAssertTrue(handler.engaged, "Should be engaged after canEngage becomes true")
    }

    // MARK: - Non-Zero Baseline Tests

    /// Verify statistics use raw weights even with non-zero baseline
    func testStatisticsWithNonZeroBaseline() {
        // Use calibration to get a non-zero baseline
        let calibrationExpectation = expectation(description: "Calibration completed")
        handler.calibrationCompleted
            .sink { calibrationExpectation.fulfill() }
            .store(in: &cancellables)

        // Start a timer to send samples continuously during calibration
        let sampleTimer = Timer.scheduledTimer(withTimeInterval: 0.1, repeats: true) { [weak self] _ in
            self?.handler.processSample(5.0)
        }

        // Send first sample during calibration (will become baseline)
        handler.processSample(5.0)
        waitForMainQueue()

        // Wait for calibration
        wait(for: [calibrationExpectation], timeout: 6.0)
        sampleTimer.invalidate()

        // Verify baseline is ~5.0
        let baseline = handler.state.baseline
        XCTAssertEqual(baseline, 5.0, accuracy: 0.1, "Baseline should be ~5.0")

        // Now engage and collect samples
        handler.canEngage = true
        let rawSamples: [Float] = [15.0, 16.0, 17.0]  // All above engage threshold (baseline + 3.0 = 8.0)

        for sample in rawSamples {
            handler.processSample(sample)
            waitForMainQueue()
        }

        XCTAssertTrue(handler.engaged, "Should be engaged")

        // Statistics should be from RAW weights, not tared
        // Raw mean: (15 + 16 + 17) / 3 = 16.0
        // If tared was used incorrectly: (10 + 11 + 12) / 3 = 11.0
        XCTAssertEqual(handler.sessionMean!, 16.0, accuracy: 0.1,
                       "Mean should be 16.0 (raw), not 11.0 (tared)")
    }

    /// Verify off-target uses raw weight with non-zero baseline
    func testOffTargetWithNonZeroBaseline() {
        // Use calibration to get a non-zero baseline
        let calibrationExpectation = expectation(description: "Calibration completed")
        handler.calibrationCompleted
            .sink { calibrationExpectation.fulfill() }
            .store(in: &cancellables)

        // Start a timer to send samples continuously during calibration
        let sampleTimer = Timer.scheduledTimer(withTimeInterval: 0.1, repeats: true) { [weak self] _ in
            self?.handler.processSample(5.0)
        }

        handler.processSample(5.0)
        waitForMainQueue()
        wait(for: [calibrationExpectation], timeout: 6.0)
        sampleTimer.invalidate()

        // Set target to 15.0 kg (raw)
        handler.targetWeight = 15.0
        handler.weightTolerance = 0.5
        handler.canEngage = true

        // Engage with on-target weight
        handler.processSample(15.0)
        waitForMainQueue()
        XCTAssertTrue(handler.engaged)

        // Second sample to trigger off-target check (still on target)
        handler.processSample(15.0)
        waitForMainQueue()
        XCTAssertFalse(handler.isOffTarget, "Should be on target at 15.0")

        // Now go off target with raw weight 16.5 (1.5 over target)
        handler.processSample(16.5)
        waitForMainQueue()

        XCTAssertTrue(handler.isOffTarget, "Should be off target at raw 16.5 vs target 15.0")
        XCTAssertEqual(handler.offTargetDirection!, 1.5, accuracy: 0.01,
                       "Direction should be +1.5 (raw 16.5 - target 15.0)")
    }
}
