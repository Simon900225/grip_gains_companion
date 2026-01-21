package app.grip_gains_companion.model

import java.util.Date

/**
 * A timestamped force entry for the force graph
 */
data class ForceHistoryEntry(
    val timestamp: Date,
    val force: Double
)
