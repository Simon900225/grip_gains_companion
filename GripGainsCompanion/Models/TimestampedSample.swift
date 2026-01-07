import Foundation

/// A force sample with device timestamp from Tindeq Progressor
struct TimestampedSample: Equatable {
    let weight: Double
    let timestamp: UInt32  // microseconds from device
}
