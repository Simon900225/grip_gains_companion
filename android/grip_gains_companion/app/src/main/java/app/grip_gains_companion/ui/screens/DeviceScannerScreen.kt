package app.grip_gains_companion.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SignalCellular4Bar
import androidx.compose.material3.*
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.grip_gains_companion.model.ConnectionState
import app.grip_gains_companion.model.DeviceType
import app.grip_gains_companion.model.ForceDevice
import app.grip_gains_companion.service.ble.BluetoothManager

@Composable
fun DeviceScannerScreen(
    bluetoothManager: BluetoothManager,
    onDeviceSelected: (ForceDevice) -> Unit,
    onSkipDevice: () -> Unit
) {
    val connectionState by bluetoothManager.connectionState.collectAsState()
    val discoveredDevices by bluetoothManager.discoveredDevices.collectAsState()
    val selectedDeviceType by bluetoothManager.selectedDeviceType.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        // Device type picker
        DeviceTypePicker(
            selectedType = selectedDeviceType,
            onTypeSelected = { bluetoothManager.setSelectedDeviceType(it) }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Header
        HeaderSection(connectionState, selectedDeviceType) {
            bluetoothManager.startScanning()
        }
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
        
        // Device list or empty state
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (discoveredDevices.isEmpty()) {
                EmptyStateView(connectionState) {
                    bluetoothManager.startScanning()
                }
            } else {
                DeviceList(
                    devices = discoveredDevices,
                    isConnecting = connectionState == ConnectionState.Connecting,
                    onDeviceSelected = onDeviceSelected
                )
            }
        }
        
        // Skip button
        TextButton(
            onClick = onSkipDevice,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 16.dp)
        ) {
            Text("Continue without device")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceTypePicker(
    selectedType: DeviceType,
    onTypeSelected: (DeviceType) -> Unit
) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier.fillMaxWidth()
    ) {
        DeviceType.entries.forEachIndexed { index, type ->
            SegmentedButton(
                selected = type == selectedType,
                onClick = { onTypeSelected(type) },
                shape = SegmentedButtonDefaults.itemShape(index, DeviceType.entries.size)
            ) {
                Text(
                    text = type.shortName,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun HeaderSection(
    connectionState: ConnectionState,
    selectedDeviceType: DeviceType,
    onRetry: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Bluetooth,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Select ${selectedDeviceType.shortName}",
            style = MaterialTheme.typography.headlineSmall
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        StatusIndicator(connectionState, selectedDeviceType, onRetry)
    }
}

@Composable
private fun StatusIndicator(
    connectionState: ConnectionState,
    selectedDeviceType: DeviceType,
    onRetry: () -> Unit
) {
    when (connectionState) {
        is ConnectionState.Initializing -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Initializing...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        is ConnectionState.Scanning -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Scanning...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        is ConnectionState.Connecting -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Connecting...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        is ConnectionState.Error -> {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = getUserFriendlyError(connectionState.message),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedButton(
                    onClick = onRetry,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Retry")
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = getTroubleshootingTip(connectionState.message, selectedDeviceType.shortName),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        
        else -> {
            Text(
                text = "Tap a device to connect",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyStateView(
    connectionState: ConnectionState,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (connectionState) {
            is ConnectionState.Initializing,
            is ConnectionState.Scanning -> {
                // Show nothing - status indicator shows progress
            }
            else -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No devices found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(onClick = onRetry) {
                        Text("Scan Again")
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceList(
    devices: List<ForceDevice>,
    isConnecting: Boolean,
    onDeviceSelected: (ForceDevice) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(devices) { device ->
            DeviceRow(
                device = device,
                enabled = !isConnecting,
                onClick = { onDeviceSelected(device) }
            )
        }
    }
}

@Composable
private fun DeviceRow(
    device: ForceDevice,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) 
                MaterialTheme.colorScheme.surface 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = device.signalStrength,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Signal strength indicator
            Icon(
                imageVector = Icons.Default.SignalCellular4Bar,
                contentDescription = "Signal: ${device.signalStrength}",
                modifier = Modifier.size(24.dp),
                tint = when (device.signalBars) {
                    4 -> MaterialTheme.colorScheme.primary
                    3 -> MaterialTheme.colorScheme.tertiary
                    2 -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.error
                }
            )
        }
    }
}

private fun getUserFriendlyError(message: String): String {
    val lower = message.lowercase()
    return when {
        lower.contains("off") -> "Bluetooth is turned off"
        lower.contains("location") -> "Location services required"
        lower.contains("unauthorized") || lower.contains("permission") -> "Bluetooth permission required"
        lower.contains("timeout") -> "Connection timed out"
        else -> message
    }
}

private fun getTroubleshootingTip(message: String, deviceName: String): String {
    val lower = message.lowercase()
    return when {
        lower.contains("off") -> "Enable Bluetooth in Settings to continue"
        lower.contains("location") -> "Enable Location in Settings for Bluetooth scanning"
        lower.contains("unauthorized") || lower.contains("permission") -> "Grant Bluetooth permission in Settings"
        lower.contains("timeout") -> "Make sure your $deviceName is powered on and nearby"
        else -> "Make sure your $deviceName is powered on and nearby"
    }
}
