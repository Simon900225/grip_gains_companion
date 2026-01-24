package app.grip_gains_companion.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.grip_gains_companion.model.ConnectionState
import app.grip_gains_companion.model.ForceHistoryEntry
import app.grip_gains_companion.service.ProgressorHandler
import app.grip_gains_companion.service.ble.BluetoothManager
import app.grip_gains_companion.service.web.WebViewBridge
import app.grip_gains_companion.ui.components.ForceGraph
import app.grip_gains_companion.ui.components.StatusBar
import app.grip_gains_companion.ui.components.TimerWebView

/**
 * Main screen with WebView and optional status bar / force graph
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    bluetoothManager: BluetoothManager,
    progressorHandler: ProgressorHandler,
    webViewBridge: WebViewBridge,
    showStatusBar: Boolean,
    expandedForceBar: Boolean,
    showForceGraph: Boolean,
    forceGraphWindow: Int,
    useLbs: Boolean,
    enableTargetWeight: Boolean,
    useManualTarget: Boolean,
    manualTargetWeight: Double,
    weightTolerance: Double,
    onSettingsTap: () -> Unit,
    onUnitToggle: () -> Unit
) {
    val connectionState by bluetoothManager.connectionState.collectAsState()
    val isConnected = connectionState == ConnectionState.Connected
    val selectedDeviceType by bluetoothManager.selectedDeviceType.collectAsState()
    
    // Progressor handler state
    val state by progressorHandler.state.collectAsState()
    val currentForce by progressorHandler.currentForce.collectAsState()
    val calibrationTimeRemaining by progressorHandler.calibrationTimeRemaining.collectAsState()
    val weightMedian by progressorHandler.weightMedian.collectAsState()
    val sessionMean by progressorHandler.sessionMean.collectAsState()
    val sessionStdDev by progressorHandler.sessionStdDev.collectAsState()
    val forceHistory by progressorHandler.forceHistory.collectAsState()
    val isOffTarget by progressorHandler.isOffTarget.collectAsState()
    val offTargetDirection by progressorHandler.offTargetDirection.collectAsState()
    
    // Web view state
    val scrapedTargetWeight by webViewBridge.targetWeight.collectAsState()
    
    // Effective target weight
    val effectiveTargetWeight = remember(enableTargetWeight, useManualTarget, manualTargetWeight, scrapedTargetWeight) {
        if (!enableTargetWeight) null
        else if (useManualTarget) manualTargetWeight
        else scrapedTargetWeight
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Status bar (only when connected)
            if (isConnected && showStatusBar) {
                StatusBar(
                    force = currentForce,
                    engaged = state.isEngaged,
                    calibrating = state.isCalibrating,
                    waitingForSamples = state.isWaitingForSamples,
                    calibrationTimeRemaining = calibrationTimeRemaining,
                    weightMedian = weightMedian,
                    targetWeight = effectiveTargetWeight,
                    isOffTarget = isOffTarget,
                    offTargetDirection = offTargetDirection,
                    sessionMean = sessionMean,
                    sessionStdDev = sessionStdDev,
                    useLbs = useLbs,
                    expanded = expandedForceBar,
                    deviceShortName = selectedDeviceType.shortName,
                    onUnitToggle = onUnitToggle,
                    onSettingsTap = onSettingsTap
                )
            }
            
            // Force graph (only when connected)
            if (isConnected && showForceGraph) {
                ForceGraph(
                    forceHistory = forceHistory,
                    useLbs = useLbs,
                    windowSeconds = forceGraphWindow,
                    targetWeight = effectiveTargetWeight,
                    tolerance = if (enableTargetWeight) weightTolerance else null
                )
            }
            
            // WebView
            TimerWebView(
                webViewBridge = webViewBridge,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
        
        // Floating settings button (when status bar is hidden)
        if (!isConnected || !showStatusBar) {
            FloatingActionButton(
                onClick = onSettingsTap,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(40.dp),
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
