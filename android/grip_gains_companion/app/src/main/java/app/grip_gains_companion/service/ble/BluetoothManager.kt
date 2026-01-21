package app.grip_gains_companion.service.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import app.grip_gains_companion.config.AppConstants
import app.grip_gains_companion.model.ConnectionState
import app.grip_gains_companion.model.ProgressorDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

/**
 * Manages Bluetooth Low Energy operations for discovering and connecting to Tindeq Progressor
 */
@SuppressLint("MissingPermission")
class BluetoothManager(private val context: Context) {
    
    companion object {
        private const val TAG = "BluetoothManager"
        private val CLIENT_CHARACTERISTIC_CONFIG: UUID = 
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }
    
    private val bluetoothManager: BluetoothManager = 
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    
    private var bluetoothGatt: BluetoothGatt? = null
    private var notifyCharacteristic: BluetoothGattCharacteristic? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null
    
    private val handler = Handler(Looper.getMainLooper())
    private var retryCount = 0
    private var pendingDevice: ProgressorDevice? = null
    private var shouldAutoReconnect = true
    
    // State flows
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Initializing)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    private val _discoveredDevices = MutableStateFlow<List<ProgressorDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<ProgressorDevice>> = _discoveredDevices.asStateFlow()
    
    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName.asStateFlow()
    
    // Callback for force samples
    var onForceSample: ((Double, Long) -> Unit)? = null
    
    // Stored device address for auto-reconnect
    private var lastConnectedDeviceAddress: String? = null
    
    init {
        if (bluetoothAdapter == null) {
            _connectionState.value = ConnectionState.Error("Bluetooth not available")
        } else if (!bluetoothAdapter.isEnabled) {
            _connectionState.value = ConnectionState.Error("Bluetooth is off")
        } else {
            _connectionState.value = ConnectionState.Disconnected
        }
    }
    
    // MARK: - Scanning
    
    fun startScanning() {
        if (bluetoothAdapter?.isEnabled != true) {
            _connectionState.value = ConnectionState.Error("Bluetooth is off")
            return
        }
        
        // On Android 11 and below, Location Services must be enabled for BLE scanning
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val isLocationEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            if (!isLocationEnabled) {
                Log.e(TAG, "Location services disabled - required for BLE scanning on Android 11 and below")
                _connectionState.value = ConnectionState.Error("Location services required")
                return
            }
        }
        
        Log.i(TAG, "Starting scan...")
        _discoveredDevices.value = emptyList()
        _connectionState.value = ConnectionState.Scanning
        
        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
        
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        
        // No filter - we'll filter by name in the callback
        bluetoothLeScanner?.startScan(null, settings, scanCallback)
    }
    
    fun stopScanning() {
        bluetoothLeScanner?.stopScan(scanCallback)
        if (_connectionState.value == ConnectionState.Scanning) {
            _connectionState.value = ConnectionState.Disconnected
        }
    }
    
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = ProgressorDevice.fromBluetoothDevice(result.device, result.rssi) ?: return
            
            val currentList = _discoveredDevices.value.toMutableList()
            val existingIndex = currentList.indexOfFirst { it.address == device.address }
            
            if (existingIndex >= 0) {
                currentList[existingIndex] = device
            } else {
                Log.i(TAG, "Discovered: ${device.name}")
                currentList.add(device)
                
                // Auto-connect if this is the last connected device
                if (device.address == lastConnectedDeviceAddress) {
                    Log.i(TAG, "Auto-reconnecting to last device...")
                    connect(device)
                }
            }
            
            _discoveredDevices.value = currentList
        }
        
        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "Scan failed with error: $errorCode")
            _connectionState.value = ConnectionState.Error("Scan failed: $errorCode")
        }
    }
    
    // MARK: - Connection
    
    fun connect(device: ProgressorDevice) {
        Log.i(TAG, "Connecting to ${device.name}...")
        stopScanning()
        cancelRetryTimer()
        
        pendingDevice = device
        shouldAutoReconnect = true
        _connectionState.value = ConnectionState.Connecting
        
        val bluetoothDevice = bluetoothAdapter?.getRemoteDevice(device.address)
        bluetoothGatt = bluetoothDevice?.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }
    
    fun disconnect(preserveAutoReconnect: Boolean = false) {
        Log.i(TAG, "Disconnecting${if (preserveAutoReconnect) " (preserving auto-reconnect)" else ""}...")
        
        shouldAutoReconnect = false
        cancelRetryTimer()
        pendingDevice = null
        
        stopScanning()
        
        bluetoothGatt?.close()
        bluetoothGatt = null
        notifyCharacteristic = null
        writeCharacteristic = null
        _connectedDeviceName.value = null
        
        if (!preserveAutoReconnect) {
            lastConnectedDeviceAddress = null
        }
        
        _discoveredDevices.value = emptyList()
        _connectionState.value = ConnectionState.Disconnected
        
        if (!preserveAutoReconnect) {
            startScanning()
        }
    }
    
    // MARK: - GATT Callback
    
    private val gattCallback = object : BluetoothGattCallback() {
        
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.i(TAG, "Connected to ${gatt.device.name}")
                    retryCount = 0
                    _connectionState.value = ConnectionState.Connected
                    _connectedDeviceName.value = gatt.device.name ?: "Unknown Progressor"
                    lastConnectedDeviceAddress = gatt.device.address
                    
                    // Discover services
                    handler.post {
                        gatt.discoverServices()
                    }
                }
                
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.i(TAG, "Disconnected")
                    _connectionState.value = ConnectionState.Disconnected
                    _connectedDeviceName.value = null
                    notifyCharacteristic = null
                    writeCharacteristic = null
                    
                    if (shouldAutoReconnect) {
                        scheduleRetry()
                    }
                }
            }
        }
        
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "Service discovery failed: $status")
                return
            }
            
            Log.i(TAG, "Services discovered: ${gatt.services.size}")
            
            val service = gatt.getService(AppConstants.PROGRESSOR_SERVICE_UUID)
            if (service == null) {
                Log.e(TAG, "Progressor service not found")
                return
            }
            
            Log.i(TAG, "Found Progressor service")
            
            // Get characteristics
            notifyCharacteristic = service.getCharacteristic(AppConstants.NOTIFY_CHARACTERISTIC_UUID)
            writeCharacteristic = service.getCharacteristic(AppConstants.WRITE_CHARACTERISTIC_UUID)
            
            if (notifyCharacteristic == null) {
                Log.e(TAG, "Notify characteristic not found")
                return
            }
            
            // Enable notifications
            Log.i(TAG, "Enabling notifications...")
            gatt.setCharacteristicNotification(notifyCharacteristic, true)
            
            val descriptor = notifyCharacteristic?.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG)
            descriptor?.let {
                gatt.writeDescriptor(it, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
            }
        }
        
        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Notifications enabled")
                // Start weight measurement
                startWeightMeasurement()
            } else {
                Log.e(TAG, "Failed to enable notifications: $status")
            }
        }
        
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            if (characteristic.uuid == AppConstants.NOTIFY_CHARACTERISTIC_UUID) {
                parseNotification(value)
            }
        }
        
        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Write successful")
            } else {
                Log.e(TAG, "Write failed: $status")
            }
        }
    }
    
    // MARK: - Commands
    
    private fun startWeightMeasurement() {
        val characteristic = writeCharacteristic ?: run {
            Log.e(TAG, "Write characteristic not available")
            return
        }
        
        Log.i(TAG, "Sending start weight command...")
        bluetoothGatt?.writeCharacteristic(
            characteristic,
            AppConstants.START_WEIGHT_COMMAND,
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        )
    }
    
    // MARK: - Data Parsing
    
    /**
     * Parse incoming BLE notification data
     * Tindeq batches ~16 samples per notification, each with weight + timestamp
     */
    private fun parseNotification(data: ByteArray) {
        // Verify packet type and minimum size
        if (data.size < AppConstants.PACKET_MIN_SIZE || 
            data[0] != AppConstants.WEIGHT_DATA_PACKET_TYPE) {
            return
        }
        
        // Parse ALL samples from notification
        // Each sample: 4-byte float (weight) + 4-byte uint32 (microseconds) = 8 bytes
        val payload = data.copyOfRange(2, data.size) // Skip packet type and count byte
        
        var offset = 0
        while (offset + AppConstants.SAMPLE_SIZE <= payload.size) {
            val weightBytes = payload.copyOfRange(offset, offset + 4)
            val timeBytes = payload.copyOfRange(offset + 4, offset + 8)
            
            // Parse little-endian float and uint32
            val weightFloat = ByteBuffer.wrap(weightBytes)
                .order(ByteOrder.LITTLE_ENDIAN)
                .float
            val timestamp = ByteBuffer.wrap(timeBytes)
                .order(ByteOrder.LITTLE_ENDIAN)
                .int
                .toLong() and 0xFFFFFFFFL  // Convert to unsigned
            
            onForceSample?.invoke(weightFloat.toDouble(), timestamp)
            
            offset += AppConstants.SAMPLE_SIZE
        }
    }
    
    // MARK: - Retry Logic
    
    private fun calculateRetryDelay(): Long {
        val baseDelay = 1000L
        val delay = baseDelay * (1L shl minOf(retryCount, 5))
        return minOf(delay, AppConstants.MAX_RETRY_DELAY_MS)
    }
    
    private fun scheduleRetry() {
        val device = pendingDevice ?: return
        
        retryCount++
        val delay = calculateRetryDelay()
        Log.i(TAG, "Scheduling retry #$retryCount in ${delay}ms...")
        
        handler.postDelayed({
            if (shouldAutoReconnect) {
                Log.i(TAG, "Retrying connection to ${device.name}...")
                _connectionState.value = ConnectionState.Connecting
                val bluetoothDevice = bluetoothAdapter?.getRemoteDevice(device.address)
                bluetoothGatt = bluetoothDevice?.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
            }
        }, delay)
    }
    
    private fun cancelRetryTimer() {
        handler.removeCallbacksAndMessages(null)
    }
}
