package app.grip_gains_companion.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.grip_gains_companion.data.PreferencesRepository
import app.grip_gains_companion.model.ConnectionState
import app.grip_gains_companion.service.ble.BluetoothManager
import app.grip_gains_companion.service.web.WebViewBridge
import app.grip_gains_companion.util.WeightFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    preferencesRepository: PreferencesRepository,
    bluetoothManager: BluetoothManager,
    webViewBridge: WebViewBridge,
    onDismiss: () -> Unit,
    onDisconnect: () -> Unit,
    onConnectDevice: () -> Unit,
    onRecalibrate: () -> Unit,
    onViewLogs: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val connectionState by bluetoothManager.connectionState.collectAsStateWithLifecycle()
    val connectedDeviceName by bluetoothManager.connectedDeviceName.collectAsStateWithLifecycle()
    val selectedDeviceType by bluetoothManager.selectedDeviceType.collectAsStateWithLifecycle()
    val isConnected = connectionState == ConnectionState.Connected
    
    // Collect all preferences
    val useLbs by preferencesRepository.useLbs.collectAsStateWithLifecycle(initialValue = false)
    val enableHaptics by preferencesRepository.enableHaptics.collectAsStateWithLifecycle(initialValue = true)
    val enableTargetSound by preferencesRepository.enableTargetSound.collectAsStateWithLifecycle(initialValue = true)
    val showStatusBar by preferencesRepository.showStatusBar.collectAsStateWithLifecycle(initialValue = true)
    val expandedForceBar by preferencesRepository.expandedForceBar.collectAsStateWithLifecycle(initialValue = true)
    val showForceGraph by preferencesRepository.showForceGraph.collectAsStateWithLifecycle(initialValue = true)
    val forceGraphWindow by preferencesRepository.forceGraphWindow.collectAsStateWithLifecycle(initialValue = 5)
    val enableTargetWeight by preferencesRepository.enableTargetWeight.collectAsStateWithLifecycle(initialValue = true)
    val useManualTarget by preferencesRepository.useManualTarget.collectAsStateWithLifecycle(initialValue = false)
    val manualTargetWeight by preferencesRepository.manualTargetWeight.collectAsStateWithLifecycle(initialValue = 20.0)
    val weightTolerance by preferencesRepository.weightTolerance.collectAsStateWithLifecycle(initialValue = 0.5)
    val enableCalibration by preferencesRepository.enableCalibration.collectAsStateWithLifecycle(initialValue = true)
    val showGripStats by preferencesRepository.showGripStats.collectAsStateWithLifecycle(initialValue = true)
    val showSetReview by preferencesRepository.showSetReview.collectAsStateWithLifecycle(initialValue = false)
    val backgroundTimeSync by preferencesRepository.backgroundTimeSync.collectAsStateWithLifecycle(initialValue = true)
    val enableLiveActivity by preferencesRepository.enableLiveActivity.collectAsStateWithLifecycle(initialValue = true)
    val autoSelectWeight by preferencesRepository.autoSelectWeight.collectAsStateWithLifecycle(initialValue = true)
    
    val scrapedTargetWeight by webViewBridge.targetWeight.collectAsStateWithLifecycle()
    
    var showResetConfirmation by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Target Weight section (only when connected)
            if (isConnected) {
                SettingsSection(title = "Target Weight") {
                    SwitchPreference(
                        title = "Enable Target Weight",
                        checked = enableTargetWeight,
                        onCheckedChange = { scope.launch { preferencesRepository.setEnableTargetWeight(it) } }
                    )
                    
                    if (enableTargetWeight) {
                        SegmentedButtonRow(
                            options = listOf("Auto", "Manual"),
                            selectedIndex = if (useManualTarget) 1 else 0,
                            onSelectionChanged = { 
                                scope.launch { preferencesRepository.setUseManualTarget(it == 1) }
                            }
                        )
                        
                        if (!useManualTarget) {
                            ListItem(
                                headlineContent = { Text("Target") },
                                trailingContent = {
                                    Text(
                                        text = scrapedTargetWeight?.let { WeightFormatter.format(it, useLbs) } ?: "Not detected",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            )
                            Text(
                                text = "Target weight is automatically read from the timer page",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        } else {
                            SliderPreference(
                                title = "Manual Target",
                                value = manualTargetWeight,
                                valueRange = 0.5..100.0,
                                steps = 199,
                                valueFormat = { WeightFormatter.format(it, useLbs) },
                                onValueChange = { scope.launch { preferencesRepository.setManualTargetWeight(it) } }
                            )
                        }
                    }
                }
            }
            
            // Website section
            SettingsSection(title = "Website") {
                ListItem(
                    headlineContent = { Text("Refresh Page") },
                    leadingContent = { Icon(Icons.Default.Refresh, contentDescription = null) },
                    modifier = Modifier.clickableRow { 
                        webViewBridge.reloadPage()
                        onDismiss()
                    }
                )
                ListItem(
                    headlineContent = { Text("Clear Website Data") },
                    leadingContent = { Icon(Icons.Default.Delete, contentDescription = null) },
                    colors = ListItemDefaults.colors(
                        headlineColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.clickableRow { 
                        webViewBridge.clearWebsiteData()
                        onDismiss()
                    }
                )
            }
            
            // Grip Detection section (only when connected)
            if (isConnected) {
                SettingsSection(title = "Grip Detection") {
                    ListItem(
                        headlineContent = { Text("Recalibrate Tare") },
                        leadingContent = { Icon(Icons.Default.Refresh, contentDescription = null) },
                        modifier = Modifier.clickableRow { onRecalibrate() }
                    )
                    
                    SwitchPreference(
                        title = "Tare on Startup",
                        checked = enableCalibration,
                        onCheckedChange = { scope.launch { preferencesRepository.setEnableCalibration(it) } }
                    )
                    
                    Text(
                        text = "Zeros the scale when ${selectedDeviceType.shortName} connects to detect grip and fail states.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    
                    SliderPreference(
                        title = "Tolerance",
                        value = weightTolerance,
                        valueRange = 0.1..5.0,
                        steps = 49,
                        valueFormat = { "±${WeightFormatter.format(it, useLbs)}" },
                        onValueChange = { scope.launch { preferencesRepository.setWeightTolerance(it) } }
                    )
                }
            }
            
            // Display section
            SettingsSection(title = "Display") {
                SwitchPreference(
                    title = "Show Force Bar",
                    checked = showStatusBar,
                    onCheckedChange = { scope.launch { preferencesRepository.setShowStatusBar(it) } }
                )
                
                SwitchPreference(
                    title = "Expanded Force Bar",
                    checked = expandedForceBar,
                    onCheckedChange = { scope.launch { preferencesRepository.setExpandedForceBar(it) } }
                )
                
                SwitchPreference(
                    title = "Force Graph",
                    checked = showForceGraph,
                    onCheckedChange = { scope.launch { preferencesRepository.setShowForceGraph(it) } }
                )
                
                if (showForceGraph) {
                    SegmentedButtonRow(
                        options = listOf("5s", "10s", "30s", "All"),
                        selectedIndex = when (forceGraphWindow) {
                            5 -> 0
                            10 -> 1
                            30 -> 2
                            else -> 3
                        },
                        onSelectionChanged = {
                            val value = when (it) {
                                0 -> 5
                                1 -> 10
                                2 -> 30
                                else -> 0
                            }
                            scope.launch { preferencesRepository.setForceGraphWindow(value) }
                        }
                    )
                }
            }
            
            // Feedback section
            SettingsSection(title = "Feedback") {
                SwitchPreference(
                    title = "Haptic Feedback",
                    checked = enableHaptics,
                    onCheckedChange = { scope.launch { preferencesRepository.setEnableHaptics(it) } }
                )
                
                SwitchPreference(
                    title = "Target Weight Sounds",
                    checked = enableTargetSound,
                    onCheckedChange = { scope.launch { preferencesRepository.setEnableTargetSound(it) } }
                )
                
                SwitchPreference(
                    title = "Grip Statistics",
                    checked = showGripStats,
                    onCheckedChange = { scope.launch { preferencesRepository.setShowGripStats(it) } }
                )
                
                SwitchPreference(
                    title = "End-of-Set Summary",
                    checked = showSetReview,
                    onCheckedChange = { scope.launch { preferencesRepository.setShowSetReview(it) } }
                )
            }
            
            // Device section
            SettingsSection(title = "Device") {
                if (isConnected) {
                    ListItem(
                        headlineContent = { Text("Connected to") },
                        trailingContent = {
                            Text(
                                text = connectedDeviceName ?: "Unknown",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                    
                    ListItem(
                        headlineContent = { Text("Disconnect") },
                        leadingContent = { 
                            Icon(
                                Icons.Default.BluetoothDisabled, 
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            ) 
                        },
                        colors = ListItemDefaults.colors(
                            headlineColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.clickableRow { onDisconnect() }
                    )
                } else {
                    ListItem(
                        headlineContent = { Text("Connect Device") },
                        leadingContent = { Icon(Icons.Default.Bluetooth, contentDescription = null) },
                        modifier = Modifier.clickableRow { onConnectDevice() }
                    )
                }
            }
            
            // Debug section
            SettingsSection(title = "Debug") {
                ListItem(
                    headlineContent = { Text("View Debug Logs") },
                    supportingContent = { Text("View and share diagnostic logs") },
                    leadingContent = { Icon(Icons.Default.BugReport, contentDescription = null) },
                    modifier = Modifier.clickableRow { onViewLogs() }
                )
            }
            
            // Experimental section
            SettingsSection(title = "Experimental") {
                SwitchPreference(
                    title = "Background Timer Sync",
                    checked = backgroundTimeSync,
                    onCheckedChange = { scope.launch { preferencesRepository.setBackgroundTimeSync(it) } }
                )
                
                Text(
                    text = "Keeps the timer accurate when the app is in background.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                
                if (backgroundTimeSync) {
                    SwitchPreference(
                        title = "Background Notification",
                        checked = enableLiveActivity,
                        onCheckedChange = { scope.launch { preferencesRepository.setEnableLiveActivity(it) } }
                    )
                    
                    Text(
                        text = "Shows elapsed and remaining time in notification when backgrounded.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                
                SwitchPreference(
                    title = "Auto-set Target Weight",
                    checked = autoSelectWeight,
                    onCheckedChange = { scope.launch { preferencesRepository.setAutoSelectWeight(it) } }
                )
            }
            
            // Units section
            SettingsSection(title = "Units") {
                SegmentedButtonRow(
                    options = listOf("kg", "lbs"),
                    selectedIndex = if (useLbs) 1 else 0,
                    onSelectionChanged = { 
                        scope.launch { preferencesRepository.setUseLbs(it == 1) }
                    }
                )
            }
            
            // Reset section
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { showResetConfirmation = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reset to Defaults")
            }
            
            Text(
                text = "Restores all settings to their recommended values.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
    
    // Reset confirmation dialog
    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            title = { Text("Reset to Defaults") },
            text = { Text("This will restore all settings to their recommended values.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            preferencesRepository.resetToDefaults()
                        }
                        showResetConfirmation = false
                    }
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        content()
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    }
}

@Composable
private fun SwitchPreference(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    )
}

@Composable
private fun SliderPreference(
    title: String,
    value: Double,
    valueRange: ClosedFloatingPointRange<Double>,
    steps: Int,
    valueFormat: (Double) -> String,
    onValueChange: (Double) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title)
            Text(
                text = valueFormat(value),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toDouble()) },
            valueRange = valueRange.start.toFloat()..valueRange.endInclusive.toFloat(),
            steps = steps
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SegmentedButtonRow(
    options: List<String>,
    selectedIndex: Int,
    onSelectionChanged: (Int) -> Unit
) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = index == selectedIndex,
                onClick = { onSelectionChanged(index) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size)
            ) {
                Text(option)
            }
        }
    }
}

private fun Modifier.clickableRow(onClick: () -> Unit): Modifier {
    return this.clickable(onClick = onClick)
}
