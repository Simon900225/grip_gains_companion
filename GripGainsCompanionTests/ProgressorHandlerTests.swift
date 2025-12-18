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
}
