package app.grip_gains_companion.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.grip_gains_companion.ui.theme.StatusColors
import app.grip_gains_companion.util.WeightFormatter
import kotlin.math.abs

/**
 * Compact status bar showing force reading and connection state
 */
@Composable
fun StatusBar(
    force: Double,
    engaged: Boolean,
    calibrating: Boolean,
    waitingForSamples: Boolean,
    calibrationTimeRemaining: Long,
    weightMedian: Double?,
    targetWeight: Double?,
    isOffTarget: Boolean,
    offTargetDirection: Double?,
    sessionMean: Double?,
    sessionStdDev: Double?,
    useLbs: Boolean,
    expanded: Boolean,
    onUnitToggle: () -> Unit,
    onSettingsTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = MaterialTheme.colorScheme.surface
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(horizontal = 16.dp, vertical = if (expanded) 20.dp else 10.dp),
        verticalArrangement = Arrangement.spacedBy(if (expanded) 8.dp else 4.dp)
    ) {
        if (expanded) {
            ExpandedLayout(
                force = force,
                engaged = engaged,
                calibrating = calibrating,
                waitingForSamples = waitingForSamples,
                calibrationTimeRemaining = calibrationTimeRemaining,
                weightMedian = weightMedian,
                targetWeight = targetWeight,
                isOffTarget = isOffTarget,
                offTargetDirection = offTargetDirection,
                sessionMean = sessionMean,
                sessionStdDev = sessionStdDev,
                useLbs = useLbs,
                onUnitToggle = onUnitToggle,
                onSettingsTap = onSettingsTap
            )
        } else {
            CompactLayout(
                force = force,
                engaged = engaged,
                calibrating = calibrating,
                waitingForSamples = waitingForSamples,
                calibrationTimeRemaining = calibrationTimeRemaining,
                weightMedian = weightMedian,
                targetWeight = targetWeight,
                isOffTarget = isOffTarget,
                offTargetDirection = offTargetDirection,
                sessionMean = sessionMean,
                sessionStdDev = sessionStdDev,
                useLbs = useLbs,
                onUnitToggle = onUnitToggle,
                onSettingsTap = onSettingsTap
            )
        }
        
        // Calibration message
        if (calibrating) {
            Text(
                text = "Don't touch Tindeq",
                style = MaterialTheme.typography.labelMedium,
                color = StatusColors.calibrating,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun CompactLayout(
    force: Double,
    engaged: Boolean,
    calibrating: Boolean,
    waitingForSamples: Boolean,
    calibrationTimeRemaining: Long,
    weightMedian: Double?,
    targetWeight: Double?,
    isOffTarget: Boolean,
    offTargetDirection: Double?,
    sessionMean: Double?,
    sessionStdDev: Double?,
    useLbs: Boolean,
    onUnitToggle: () -> Unit,
    onSettingsTap: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Force display
        Text(
            text = WeightFormatter.format(force, useLbs),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = getForceColor(force, engaged, isOffTarget, weightMedian),
            modifier = Modifier.clickable { onUnitToggle() }
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Statistics
        if (sessionMean != null) {
            StatisticsDisplay(sessionMean, sessionStdDev, useLbs)
        }
        
        // Weight info
        WeightDisplay(
            weightMedian = weightMedian,
            targetWeight = targetWeight,
            isOffTarget = isOffTarget,
            offTargetDirection = offTargetDirection,
            engaged = engaged,
            useLbs = useLbs
        )
        
        // State badge
        StateBadge(
            engaged = engaged,
            calibrating = calibrating,
            waitingForSamples = waitingForSamples,
            calibrationTimeRemaining = calibrationTimeRemaining
        )
        
        // Settings button
        IconButton(onClick = onSettingsTap, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ExpandedLayout(
    force: Double,
    engaged: Boolean,
    calibrating: Boolean,
    waitingForSamples: Boolean,
    calibrationTimeRemaining: Long,
    weightMedian: Double?,
    targetWeight: Double?,
    isOffTarget: Boolean,
    offTargetDirection: Double?,
    sessionMean: Double?,
    sessionStdDev: Double?,
    useLbs: Boolean,
    onUnitToggle: () -> Unit,
    onSettingsTap: () -> Unit
) {
    // Top row: measured weight on left, badge and settings on right
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Measured weight
        if (weightMedian != null && !engaged) {
            Text(
                text = "⚖ ${WeightFormatter.format(weightMedian, useLbs)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        StateBadge(
            engaged = engaged,
            calibrating = calibrating,
            waitingForSamples = waitingForSamples,
            calibrationTimeRemaining = calibrationTimeRemaining
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        IconButton(onClick = onSettingsTap, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    
    // Center: giant force number
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = WeightFormatter.format(force, useLbs),
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = getForceColor(force, engaged, isOffTarget, weightMedian),
            modifier = Modifier.clickable { onUnitToggle() }
        )
    }
    
    // Bottom row: stats on left, target on right
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (sessionMean != null) {
            StatisticsDisplay(sessionMean, sessionStdDev, useLbs)
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        TargetWeightDisplay(
            targetWeight = targetWeight,
            isOffTarget = isOffTarget,
            offTargetDirection = offTargetDirection,
            useLbs = useLbs
        )
    }
}

@Composable
private fun StatisticsDisplay(
    mean: Double,
    stdDev: Double?,
    useLbs: Boolean
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Mean
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "x̄",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = WeightFormatter.format(mean, useLbs),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Std dev
        if (stdDev != null && stdDev > 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "σ",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = WeightFormatter.format(stdDev, useLbs, includeUnit = false),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun WeightDisplay(
    weightMedian: Double?,
    targetWeight: Double?,
    isOffTarget: Boolean,
    offTargetDirection: Double?,
    engaged: Boolean,
    useLbs: Boolean
) {
    if (weightMedian != null && !engaged) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "⚖ ${WeightFormatter.format(weightMedian, useLbs)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (targetWeight != null) {
                Text(
                    text = " → ${WeightFormatter.format(targetWeight, useLbs)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    } else if (engaged && targetWeight != null) {
        TargetWeightDisplay(targetWeight, isOffTarget, offTargetDirection, useLbs)
    }
}

@Composable
private fun TargetWeightDisplay(
    targetWeight: Double?,
    isOffTarget: Boolean,
    offTargetDirection: Double?,
    useLbs: Boolean
) {
    if (targetWeight != null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Target: ${WeightFormatter.format(targetWeight, useLbs)}",
                style = MaterialTheme.typography.labelMedium,
                color = if (isOffTarget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (isOffTarget && offTargetDirection != null) {
                val sign = if (offTargetDirection > 0) "+" else ""
                Text(
                    text = " ($sign${WeightFormatter.format(offTargetDirection, useLbs)})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun StateBadge(
    engaged: Boolean,
    calibrating: Boolean,
    waitingForSamples: Boolean,
    calibrationTimeRemaining: Long
) {
    val (text, color) = when {
        waitingForSamples -> "Waiting" to StatusColors.idle
        calibrating -> "${calibrationTimeRemaining / 1000}s" to StatusColors.calibrating
        engaged -> "Gripping" to StatusColors.gripping
        else -> "Idle" to StatusColors.idle
    }
    
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

private fun getForceColor(
    force: Double,
    engaged: Boolean,
    isOffTarget: Boolean,
    weightMedian: Double?
): Color {
    return when {
        engaged && isOffTarget -> Color(0xFFEF4444) // Red
        engaged -> Color(0xFF10B981) // Green
        force > (weightMedian ?: 3.0) -> Color(0xFF3B82F6) // Blue
        else -> Color(0xFF6B7280) // Gray
    }
}
