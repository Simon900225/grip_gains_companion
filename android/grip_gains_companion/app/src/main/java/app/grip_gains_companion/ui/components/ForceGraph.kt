package app.grip_gains_companion.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import app.grip_gains_companion.config.AppConstants
import app.grip_gains_companion.model.ForceHistoryEntry
import java.util.Date
import kotlin.math.max
import kotlin.math.min

/**
 * Real-time force graph showing recent force history
 */
@Composable
fun ForceGraph(
    forceHistory: List<ForceHistoryEntry>,
    useLbs: Boolean,
    windowSeconds: Int, // 0 = entire session
    targetWeight: Double?,
    tolerance: Double?,
    modifier: Modifier = Modifier
) {
    val backgroundColor = MaterialTheme.colorScheme.surface
    
    // Filter history to the selected time window
    val visibleHistory = remember(forceHistory, windowSeconds) {
        if (windowSeconds <= 0) {
            forceHistory
        } else {
            val cutoff = Date(System.currentTimeMillis() - windowSeconds * 1000L)
            forceHistory.filter { it.timestamp.time >= cutoff.time }
        }
    }
    
    // Convert to display units
    val displayMultiplier = if (useLbs) AppConstants.KG_TO_LBS else 1.0
    val displayHistory = remember(visibleHistory, useLbs) {
        visibleHistory.map { it.force * displayMultiplier }
    }
    
    val displayTarget = targetWeight?.times(displayMultiplier)
    val displayTolerance = tolerance?.times(displayMultiplier)
    
    // Calculate Y-axis domain
    val (yMin, yMax) = remember(displayHistory, displayTarget, displayTolerance) {
        val forces = displayHistory
        var lower = forces.minOrNull() ?: 0.0
        var upper = forces.maxOrNull() ?: 10.0
        
        // Include target and tolerance in range
        if (displayTarget != null) {
            val tolValue = displayTolerance ?: 0.0
            lower = min(lower, displayTarget - tolValue)
            upper = max(upper, displayTarget + tolValue)
        }
        
        // Add padding
        val range = upper - lower
        val padding = max(range * 0.15, 2.0)
        Pair(max(0.0, lower - padding), upper + padding)
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(backgroundColor)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        if (visibleHistory.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No data",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val yRange = yMax - yMin
                
                fun yToCanvas(y: Double): Float {
                    return (height - ((y - yMin) / yRange * height)).toFloat()
                }
                
                // Draw tolerance band
                if (displayTarget != null && displayTolerance != null) {
                    val upperY = yToCanvas(displayTarget + displayTolerance)
                    val lowerY = yToCanvas(displayTarget - displayTolerance)
                    drawRect(
                        color = Color.Gray.copy(alpha = 0.3f),
                        topLeft = Offset(0f, upperY),
                        size = androidx.compose.ui.geometry.Size(width, lowerY - upperY)
                    )
                }
                
                // Draw target line
                if (displayTarget != null) {
                    val targetY = yToCanvas(displayTarget)
                    drawLine(
                        color = Color(0xFF10B981).copy(alpha = 0.7f),
                        start = Offset(0f, targetY),
                        end = Offset(width, targetY),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f))
                    )
                }
                
                // Draw force line
                if (displayHistory.size >= 2) {
                    val path = Path()
                    val xStep = width / (displayHistory.size - 1).coerceAtLeast(1)
                    
                    displayHistory.forEachIndexed { index, force ->
                        val x = index * xStep
                        val y = yToCanvas(force)
                        
                        if (index == 0) {
                            path.moveTo(x, y)
                        } else {
                            path.lineTo(x, y)
                        }
                    }
                    
                    drawPath(
                        path = path,
                        color = Color(0xFF3B82F6),
                        style = Stroke(width = 4f)
                    )
                }
                
                // Draw Y-axis labels (left side)
                // Note: Text drawing in Canvas is complex in Compose, 
                // so we'll use a simple approach with positioned elements outside
            }
            
            // Y-axis labels overlay
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${yMax.toInt()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${yMin.toInt()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
