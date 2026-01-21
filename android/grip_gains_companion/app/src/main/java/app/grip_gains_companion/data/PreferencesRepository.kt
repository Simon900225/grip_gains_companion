package app.grip_gains_companion.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.grip_gains_companion.config.AppConstants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Repository for app preferences using DataStore
 */
class PreferencesRepository(private val context: Context) {
    
    // Keys
    private object Keys {
        val USE_LBS = booleanPreferencesKey("use_lbs")
        val ENABLE_HAPTICS = booleanPreferencesKey("enable_haptics")
        val ENABLE_TARGET_SOUND = booleanPreferencesKey("enable_target_sound")
        val SHOW_STATUS_BAR = booleanPreferencesKey("show_status_bar")
        val EXPANDED_FORCE_BAR = booleanPreferencesKey("expanded_force_bar")
        val SHOW_FORCE_GRAPH = booleanPreferencesKey("show_force_graph")
        val FORCE_GRAPH_WINDOW = intPreferencesKey("force_graph_window")
        val FULL_SCREEN = booleanPreferencesKey("full_screen")
        val FORCE_BAR_THEME = stringPreferencesKey("force_bar_theme")
        
        val ENABLE_TARGET_WEIGHT = booleanPreferencesKey("enable_target_weight")
        val USE_MANUAL_TARGET = booleanPreferencesKey("use_manual_target")
        val MANUAL_TARGET_WEIGHT = doublePreferencesKey("manual_target_weight")
        val WEIGHT_TOLERANCE = doublePreferencesKey("weight_tolerance")
        
        val ENABLE_CALIBRATION = booleanPreferencesKey("enable_calibration")
        val ENGAGE_THRESHOLD = doublePreferencesKey("engage_threshold")
        val FAIL_THRESHOLD = doublePreferencesKey("fail_threshold")
        
        val ENABLE_PERCENTAGE_THRESHOLDS = booleanPreferencesKey("enable_percentage_thresholds")
        val ENGAGE_PERCENTAGE = doublePreferencesKey("engage_percentage")
        val DISENGAGE_PERCENTAGE = doublePreferencesKey("disengage_percentage")
        val TOLERANCE_PERCENTAGE = doublePreferencesKey("tolerance_percentage")
        
        val ENGAGE_FLOOR = doublePreferencesKey("engage_floor")
        val ENGAGE_CEILING = doublePreferencesKey("engage_ceiling")
        val DISENGAGE_FLOOR = doublePreferencesKey("disengage_floor")
        val DISENGAGE_CEILING = doublePreferencesKey("disengage_ceiling")
        val TOLERANCE_FLOOR = doublePreferencesKey("tolerance_floor")
        val TOLERANCE_CEILING = doublePreferencesKey("tolerance_ceiling")
        
        val BACKGROUND_TIME_SYNC = booleanPreferencesKey("background_time_sync")
        val ENABLE_LIVE_ACTIVITY = booleanPreferencesKey("enable_live_activity")
        val AUTO_SELECT_WEIGHT = booleanPreferencesKey("auto_select_weight")
        val AUTO_SELECT_FROM_MANUAL = booleanPreferencesKey("auto_select_from_manual")
        
        val SHOW_GRIP_STATS = booleanPreferencesKey("show_grip_stats")
        val SHOW_SET_REVIEW = booleanPreferencesKey("show_set_review")
        
        val LAST_CONNECTED_DEVICE_ADDRESS = stringPreferencesKey("last_connected_device_address")
    }
    
    // Unit preference
    val useLbs: Flow<Boolean> = context.dataStore.data.map { it[Keys.USE_LBS] ?: false }
    suspend fun setUseLbs(value: Boolean) = context.dataStore.edit { it[Keys.USE_LBS] = value }
    
    // Feedback preferences
    val enableHaptics: Flow<Boolean> = context.dataStore.data.map { 
        it[Keys.ENABLE_HAPTICS] ?: AppConstants.DEFAULT_ENABLE_HAPTICS 
    }
    suspend fun setEnableHaptics(value: Boolean) = context.dataStore.edit { it[Keys.ENABLE_HAPTICS] = value }
    
    val enableTargetSound: Flow<Boolean> = context.dataStore.data.map { 
        it[Keys.ENABLE_TARGET_SOUND] ?: AppConstants.DEFAULT_ENABLE_TARGET_SOUND 
    }
    suspend fun setEnableTargetSound(value: Boolean) = context.dataStore.edit { it[Keys.ENABLE_TARGET_SOUND] = value }
    
    // Display preferences
    val showStatusBar: Flow<Boolean> = context.dataStore.data.map { 
        it[Keys.SHOW_STATUS_BAR] ?: AppConstants.DEFAULT_SHOW_STATUS_BAR 
    }
    suspend fun setShowStatusBar(value: Boolean) = context.dataStore.edit { it[Keys.SHOW_STATUS_BAR] = value }
    
    val expandedForceBar: Flow<Boolean> = context.dataStore.data.map { 
        it[Keys.EXPANDED_FORCE_BAR] ?: AppConstants.DEFAULT_EXPANDED_FORCE_BAR 
    }
    suspend fun setExpandedForceBar(value: Boolean) = context.dataStore.edit { it[Keys.EXPANDED_FORCE_BAR] = value }
    
    val showForceGraph: Flow<Boolean> = context.dataStore.data.map { 
        it[Keys.SHOW_FORCE_GRAPH] ?: AppConstants.DEFAULT_SHOW_FORCE_GRAPH 
    }
    suspend fun setShowForceGraph(value: Boolean) = context.dataStore.edit { it[Keys.SHOW_FORCE_GRAPH] = value }
    
    val forceGraphWindow: Flow<Int> = context.dataStore.data.map { 
        it[Keys.FORCE_GRAPH_WINDOW] ?: AppConstants.DEFAULT_FORCE_GRAPH_WINDOW 
    }
    suspend fun setForceGraphWindow(value: Int) = context.dataStore.edit { it[Keys.FORCE_GRAPH_WINDOW] = value }
    
    val fullScreen: Flow<Boolean> = context.dataStore.data.map { 
        it[Keys.FULL_SCREEN] ?: AppConstants.DEFAULT_FULL_SCREEN 
    }
    suspend fun setFullScreen(value: Boolean) = context.dataStore.edit { it[Keys.FULL_SCREEN] = value }
    
    val forceBarTheme: Flow<String> = context.dataStore.data.map { 
        it[Keys.FORCE_BAR_THEME] ?: "system" 
    }
    suspend fun setForceBarTheme(value: String) = context.dataStore.edit { it[Keys.FORCE_BAR_THEME] = value }
    
    // Target weight preferences
    val enableTargetWeight: Flow<Boolean> = context.dataStore.data.map { 
        it[Keys.ENABLE_TARGET_WEIGHT] ?: AppConstants.DEFAULT_ENABLE_TARGET_WEIGHT 
    }
    suspend fun setEnableTargetWeight(value: Boolean) = context.dataStore.edit { it[Keys.ENABLE_TARGET_WEIGHT] = value }
    
    val useManualTarget: Flow<Boolean> = context.dataStore.data.map { 
        it[Keys.USE_MANUAL_TARGET] ?: AppConstants.DEFAULT_USE_MANUAL_TARGET 
    }
    suspend fun setUseManualTarget(value: Boolean) = context.dataStore.edit { it[Keys.USE_MANUAL_TARGET] = value }
    
    val manualTargetWeight: Flow<Double> = context.dataStore.data.map { 
        it[Keys.MANUAL_TARGET_WEIGHT] ?: AppConstants.DEFAULT_MANUAL_TARGET_WEIGHT 
    }
    suspend fun setManualTargetWeight(value: Double) = context.dataStore.edit { it[Keys.MANUAL_TARGET_WEIGHT] = value }
    
    val weightTolerance: Flow<Double> = context.dataStore.data.map { 
        it[Keys.WEIGHT_TOLERANCE] ?: AppConstants.DEFAULT_WEIGHT_TOLERANCE 
    }
    suspend fun setWeightTolerance(value: Double) = context.dataStore.edit { it[Keys.WEIGHT_TOLERANCE] = value }
    
    // Calibration preferences
    val enableCalibration: Flow<Boolean> = context.dataStore.data.map { 
        it[Keys.ENABLE_CALIBRATION] ?: AppConstants.DEFAULT_ENABLE_CALIBRATION 
    }
    suspend fun setEnableCalibration(value: Boolean) = context.dataStore.edit { it[Keys.ENABLE_CALIBRATION] = value }
    
    val engageThreshold: Flow<Double> = context.dataStore.data.map { 
        it[Keys.ENGAGE_THRESHOLD] ?: AppConstants.DEFAULT_ENGAGE_THRESHOLD 
    }
    suspend fun setEngageThreshold(value: Double) = context.dataStore.edit { it[Keys.ENGAGE_THRESHOLD] = value }
    
    val failThreshold: Flow<Double> = context.dataStore.data.map { 
        it[Keys.FAIL_THRESHOLD] ?: AppConstants.DEFAULT_FAIL_THRESHOLD 
    }
    suspend fun setFailThreshold(value: Double) = context.dataStore.edit { it[Keys.FAIL_THRESHOLD] = value }
    
    // Percentage threshold preferences
    val enablePercentageThresholds: Flow<Boolean> = context.dataStore.data.map { 
        it[Keys.ENABLE_PERCENTAGE_THRESHOLDS] ?: AppConstants.DEFAULT_ENABLE_PERCENTAGE_THRESHOLDS 
    }
    suspend fun setEnablePercentageThresholds(value: Boolean) = context.dataStore.edit { it[Keys.ENABLE_PERCENTAGE_THRESHOLDS] = value }
    
    val engagePercentage: Flow<Double> = context.dataStore.data.map { 
        it[Keys.ENGAGE_PERCENTAGE] ?: AppConstants.DEFAULT_ENGAGE_PERCENTAGE 
    }
    suspend fun setEngagePercentage(value: Double) = context.dataStore.edit { it[Keys.ENGAGE_PERCENTAGE] = value }
    
    val disengagePercentage: Flow<Double> = context.dataStore.data.map { 
        it[Keys.DISENGAGE_PERCENTAGE] ?: AppConstants.DEFAULT_DISENGAGE_PERCENTAGE 
    }
    suspend fun setDisengagePercentage(value: Double) = context.dataStore.edit { it[Keys.DISENGAGE_PERCENTAGE] = value }
    
    val tolerancePercentage: Flow<Double> = context.dataStore.data.map { 
        it[Keys.TOLERANCE_PERCENTAGE] ?: AppConstants.DEFAULT_TOLERANCE_PERCENTAGE 
    }
    suspend fun setTolerancePercentage(value: Double) = context.dataStore.edit { it[Keys.TOLERANCE_PERCENTAGE] = value }
    
    // Threshold bounds
    val engageFloor: Flow<Double> = context.dataStore.data.map { it[Keys.ENGAGE_FLOOR] ?: AppConstants.DEFAULT_ENGAGE_FLOOR }
    suspend fun setEngageFloor(value: Double) = context.dataStore.edit { it[Keys.ENGAGE_FLOOR] = value }
    
    val engageCeiling: Flow<Double> = context.dataStore.data.map { it[Keys.ENGAGE_CEILING] ?: AppConstants.DEFAULT_ENGAGE_CEILING }
    suspend fun setEngageCeiling(value: Double) = context.dataStore.edit { it[Keys.ENGAGE_CEILING] = value }
    
    val disengageFloor: Flow<Double> = context.dataStore.data.map { it[Keys.DISENGAGE_FLOOR] ?: AppConstants.DEFAULT_DISENGAGE_FLOOR }
    suspend fun setDisengageFloor(value: Double) = context.dataStore.edit { it[Keys.DISENGAGE_FLOOR] = value }
    
    val disengageCeiling: Flow<Double> = context.dataStore.data.map { it[Keys.DISENGAGE_CEILING] ?: AppConstants.DEFAULT_DISENGAGE_CEILING }
    suspend fun setDisengageCeiling(value: Double) = context.dataStore.edit { it[Keys.DISENGAGE_CEILING] = value }
    
    val toleranceFloor: Flow<Double> = context.dataStore.data.map { it[Keys.TOLERANCE_FLOOR] ?: AppConstants.DEFAULT_TOLERANCE_FLOOR }
    suspend fun setToleranceFloor(value: Double) = context.dataStore.edit { it[Keys.TOLERANCE_FLOOR] = value }
    
    val toleranceCeiling: Flow<Double> = context.dataStore.data.map { it[Keys.TOLERANCE_CEILING] ?: AppConstants.DEFAULT_TOLERANCE_CEILING }
    suspend fun setToleranceCeiling(value: Double) = context.dataStore.edit { it[Keys.TOLERANCE_CEILING] = value }
    
    // Experimental features
    val backgroundTimeSync: Flow<Boolean> = context.dataStore.data.map { 
        it[Keys.BACKGROUND_TIME_SYNC] ?: AppConstants.DEFAULT_BACKGROUND_TIME_SYNC 
    }
    suspend fun setBackgroundTimeSync(value: Boolean) = context.dataStore.edit { it[Keys.BACKGROUND_TIME_SYNC] = value }
    
    val enableLiveActivity: Flow<Boolean> = context.dataStore.data.map { 
        it[Keys.ENABLE_LIVE_ACTIVITY] ?: AppConstants.DEFAULT_ENABLE_LIVE_ACTIVITY 
    }
    suspend fun setEnableLiveActivity(value: Boolean) = context.dataStore.edit { it[Keys.ENABLE_LIVE_ACTIVITY] = value }
    
    val autoSelectWeight: Flow<Boolean> = context.dataStore.data.map { 
        it[Keys.AUTO_SELECT_WEIGHT] ?: AppConstants.DEFAULT_AUTO_SELECT_WEIGHT 
    }
    suspend fun setAutoSelectWeight(value: Boolean) = context.dataStore.edit { it[Keys.AUTO_SELECT_WEIGHT] = value }
    
    val autoSelectFromManual: Flow<Boolean> = context.dataStore.data.map { 
        it[Keys.AUTO_SELECT_FROM_MANUAL] ?: AppConstants.DEFAULT_AUTO_SELECT_FROM_MANUAL 
    }
    suspend fun setAutoSelectFromManual(value: Boolean) = context.dataStore.edit { it[Keys.AUTO_SELECT_FROM_MANUAL] = value }
    
    // Statistics preferences
    val showGripStats: Flow<Boolean> = context.dataStore.data.map { 
        it[Keys.SHOW_GRIP_STATS] ?: AppConstants.DEFAULT_SHOW_GRIP_STATS 
    }
    suspend fun setShowGripStats(value: Boolean) = context.dataStore.edit { it[Keys.SHOW_GRIP_STATS] = value }
    
    val showSetReview: Flow<Boolean> = context.dataStore.data.map { 
        it[Keys.SHOW_SET_REVIEW] ?: AppConstants.DEFAULT_SHOW_SET_REVIEW 
    }
    suspend fun setShowSetReview(value: Boolean) = context.dataStore.edit { it[Keys.SHOW_SET_REVIEW] = value }
    
    // Device preferences
    val lastConnectedDeviceAddress: Flow<String?> = context.dataStore.data.map { 
        it[Keys.LAST_CONNECTED_DEVICE_ADDRESS] 
    }
    suspend fun setLastConnectedDeviceAddress(value: String?) = context.dataStore.edit { 
        if (value != null) {
            it[Keys.LAST_CONNECTED_DEVICE_ADDRESS] = value
        } else {
            it.remove(Keys.LAST_CONNECTED_DEVICE_ADDRESS)
        }
    }
    
    /**
     * Reset all settings to defaults
     */
    suspend fun resetToDefaults() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
