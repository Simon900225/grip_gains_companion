package app.grip_gains_companion

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import app.grip_gains_companion.data.PreferencesRepository
import app.grip_gains_companion.model.ConnectionState
import app.grip_gains_companion.service.ProgressorHandler
import app.grip_gains_companion.service.ble.BluetoothManager
import app.grip_gains_companion.service.web.WebViewBridge
import app.grip_gains_companion.ui.screens.DeviceScannerScreen
import app.grip_gains_companion.ui.screens.MainScreen
import app.grip_gains_companion.ui.screens.SettingsScreen
import app.grip_gains_companion.ui.theme.GripGainsTheme
import app.grip_gains_companion.util.HapticManager
import app.grip_gains_companion.util.ToneGenerator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var progressorHandler: ProgressorHandler
    private lateinit var webViewBridge: WebViewBridge
    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var hapticManager: HapticManager
    
    private val requiredPermissions: Array<String>
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ uses new Bluetooth permissions
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            // Android 11 and below require location for BLE scanning
            // (BLUETOOTH and BLUETOOTH_ADMIN are normal permissions, granted at install)
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            bluetoothManager.startScanning()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Keep screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        // Initialize services
        bluetoothManager = BluetoothManager(this)
        progressorHandler = ProgressorHandler()
        webViewBridge = WebViewBridge()
        preferencesRepository = PreferencesRepository(this)
        hapticManager = HapticManager(this)
        
        // Connect BLE samples to handler
        bluetoothManager.onForceSample = { force, timestamp ->
            lifecycleScope.launch {
                progressorHandler.processSample(force, timestamp)
            }
        }
        
        // Set up event handlers
        setupEventHandlers()
        
        // Check permissions and start scanning
        if (hasAllPermissions()) {
            bluetoothManager.startScanning()
        } else {
            permissionLauncher.launch(requiredPermissions)
        }
        
        setContent {
            val connectionState by bluetoothManager.connectionState.collectAsState()
            val isConnected = connectionState == ConnectionState.Connected
            
            // Collect preferences
            val useLbs by preferencesRepository.useLbs.collectAsState(initial = false)
            val showStatusBar by preferencesRepository.showStatusBar.collectAsState(initial = true)
            val expandedForceBar by preferencesRepository.expandedForceBar.collectAsState(initial = true)
            val showForceGraph by preferencesRepository.showForceGraph.collectAsState(initial = true)
            val forceGraphWindow by preferencesRepository.forceGraphWindow.collectAsState(initial = 5)
            val enableTargetWeight by preferencesRepository.enableTargetWeight.collectAsState(initial = true)
            val useManualTarget by preferencesRepository.useManualTarget.collectAsState(initial = false)
            val manualTargetWeight by preferencesRepository.manualTargetWeight.collectAsState(initial = 20.0)
            val weightTolerance by preferencesRepository.weightTolerance.collectAsState(initial = 0.5)
            val enableHaptics by preferencesRepository.enableHaptics.collectAsState(initial = true)
            val enableTargetSound by preferencesRepository.enableTargetSound.collectAsState(initial = true)
            val enableCalibration by preferencesRepository.enableCalibration.collectAsState(initial = true)
            
            // Update handler settings
            LaunchedEffect(enableCalibration) {
                progressorHandler.enableCalibration = enableCalibration
            }
            
            // Screen state
            var skippedDevice by remember { mutableStateOf(false) }
            var showSettings by remember { mutableStateOf(false) }
            
            // Haptic feedback on connect
            LaunchedEffect(connectionState) {
                if (connectionState == ConnectionState.Connected && enableHaptics) {
                    hapticManager.success()
                }
                if (connectionState == ConnectionState.Disconnected) {
                    progressorHandler.reset()
                }
            }
            
            GripGainsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when {
                        showSettings -> {
                            SettingsScreen(
                                preferencesRepository = preferencesRepository,
                                bluetoothManager = bluetoothManager,
                                webViewBridge = webViewBridge,
                                onDismiss = { showSettings = false },
                                onDisconnect = {
                                    showSettings = false
                                    bluetoothManager.disconnect()
                                    skippedDevice = false
                                },
                                onConnectDevice = {
                                    showSettings = false
                                    skippedDevice = false
                                },
                                onRecalibrate = {
                                    showSettings = false
                                    progressorHandler.recalibrate()
                                    webViewBridge.refreshButtonState()
                                }
                            )
                        }
                        
                        isConnected || skippedDevice -> {
                            MainScreen(
                                bluetoothManager = bluetoothManager,
                                progressorHandler = progressorHandler,
                                webViewBridge = webViewBridge,
                                showStatusBar = showStatusBar,
                                expandedForceBar = expandedForceBar,
                                showForceGraph = showForceGraph,
                                forceGraphWindow = forceGraphWindow,
                                useLbs = useLbs,
                                enableTargetWeight = enableTargetWeight,
                                useManualTarget = useManualTarget,
                                manualTargetWeight = manualTargetWeight,
                                weightTolerance = weightTolerance,
                                onSettingsTap = { showSettings = true },
                                onUnitToggle = {
                                    lifecycleScope.launch {
                                        preferencesRepository.setUseLbs(!useLbs)
                                    }
                                }
                            )
                        }
                        
                        else -> {
                            DeviceScannerScreen(
                                bluetoothManager = bluetoothManager,
                                onDeviceSelected = { device ->
                                    bluetoothManager.connect(device)
                                },
                                onSkipDevice = {
                                    skippedDevice = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    
    private fun setupEventHandlers() {
        // Grip failed -> click fail button
        lifecycleScope.launch {
            progressorHandler.gripFailed.collect {
                webViewBridge.clickFailButton()
                
                val enableHaptics = preferencesRepository.enableHaptics.first()
                if (enableHaptics) {
                    hapticManager.warning()
                }
            }
        }
        
        // Calibration complete
        lifecycleScope.launch {
            progressorHandler.calibrationCompleted.collect {
                val enableHaptics = preferencesRepository.enableHaptics.first()
                if (enableHaptics) {
                    hapticManager.light()
                }
            }
        }
        
        // Off-target feedback
        lifecycleScope.launch {
            progressorHandler.offTargetChanged.collect { (isOffTarget, direction) ->
                if (!isOffTarget) return@collect
                
                val enableHaptics = preferencesRepository.enableHaptics.first()
                val enableSound = preferencesRepository.enableTargetSound.first()
                
                if (enableHaptics) {
                    hapticManager.warning()
                }
                
                if (enableSound) {
                    if (direction != null) {
                        if (direction > 0) {
                            ToneGenerator.playHighTone() // Too heavy
                        } else {
                            ToneGenerator.playLowTone() // Too light
                        }
                    } else {
                        ToneGenerator.playWarningTone()
                    }
                }
            }
        }
        
        // Button state changes
        lifecycleScope.launch {
            webViewBridge.buttonEnabled.collect { enabled ->
                progressorHandler.canEngage = enabled
            }
        }
        
        // Update handler with target weight from web
        lifecycleScope.launch {
            webViewBridge.targetWeight.collect { weight ->
                val enableTargetWeight = preferencesRepository.enableTargetWeight.first()
                val useManualTarget = preferencesRepository.useManualTarget.first()
                
                if (enableTargetWeight && !useManualTarget) {
                    progressorHandler.targetWeight = weight
                }
            }
        }
    }
    
    private fun hasAllPermissions(): Boolean {
        return requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        bluetoothManager.disconnect()
    }
}
