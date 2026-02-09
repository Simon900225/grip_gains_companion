package app.grip_gains_companion.service.web

import android.webkit.JavascriptInterface
import android.webkit.WebView
import app.grip_gains_companion.config.AppConstants
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

/**
 * Bridge between Android and JavaScript in the WebView
 * Handles JS -> Android callbacks via @JavascriptInterface
 */
class WebViewBridge {
    
    private var webView: WebView? = null
    
    // State flows for reactive updates
    private val _buttonEnabled = MutableStateFlow(false)
    val buttonEnabled: StateFlow<Boolean> = _buttonEnabled.asStateFlow()
    
    private val _targetWeight = MutableStateFlow<Double?>(null)
    val targetWeight: StateFlow<Double?> = _targetWeight.asStateFlow()
    
    private val _targetDuration = MutableStateFlow<Int?>(null)
    val targetDuration: StateFlow<Int?> = _targetDuration.asStateFlow()
    
    private val _remainingTime = MutableStateFlow<Int?>(null)
    val remainingTime: StateFlow<Int?> = _remainingTime.asStateFlow()
    
    private val _weightOptions = MutableStateFlow<List<Double>>(emptyList())
    val weightOptions: StateFlow<List<Double>> = _weightOptions.asStateFlow()
    
    private val _weightOptionsIsLbs = MutableStateFlow(false)
    val weightOptionsIsLbs: StateFlow<Boolean> = _weightOptionsIsLbs.asStateFlow()
    
    private val _gripper = MutableStateFlow<String?>(null)
    val gripper: StateFlow<String?> = _gripper.asStateFlow()
    
    private val _side = MutableStateFlow<String?>(null)
    val side: StateFlow<String?> = _side.asStateFlow()
    
    private val _settingsVisible = MutableStateFlow(true)
    val settingsVisible: StateFlow<Boolean> = _settingsVisible.asStateFlow()
    
    // Events
    private val _saveButtonAppeared = MutableSharedFlow<Unit>()
    val saveButtonAppeared = _saveButtonAppeared.asSharedFlow()
    
    fun setWebView(webView: WebView) {
        this.webView = webView
    }
    
    // MARK: - JavaScript Interface (JS -> Android)
    
    @JavascriptInterface
    fun onButtonStateChanged(enabled: Boolean) {
        _buttonEnabled.value = enabled
    }
    
    @JavascriptInterface
    fun onTargetWeightChanged(weightString: String?) {
        _targetWeight.value = weightString?.let { parseWeight(it) }
    }
    
    @JavascriptInterface
    fun onTargetDurationChanged(seconds: Int) {
        _targetDuration.value = if (seconds > 0) seconds else null
    }
    
    @JavascriptInterface
    fun onRemainingTimeChanged(seconds: Int) {
        _remainingTime.value = if (seconds != -9999) seconds else null
    }
    
    @JavascriptInterface
    fun onWeightOptionsChanged(weightsJson: String, isLbs: Boolean) {
        try {
            val jsonArray = JSONArray(weightsJson)
            val weights = mutableListOf<Double>()
            for (i in 0 until jsonArray.length()) {
                weights.add(jsonArray.getDouble(i))
            }
            _weightOptions.value = weights.sorted()
            _weightOptionsIsLbs.value = isLbs
        } catch (e: Exception) {
            _weightOptions.value = emptyList()
            _weightOptionsIsLbs.value = false
        }
    }
    
    @JavascriptInterface
    fun onSessionInfoChanged(gripper: String?, side: String?) {
        _gripper.value = gripper
        _side.value = side
    }
    
    @JavascriptInterface
    fun onSettingsVisibleChanged(visible: Boolean) {
        _settingsVisible.value = visible
    }
    
    @JavascriptInterface
    fun onSaveButtonAppeared() {
        _saveButtonAppeared.tryEmit(Unit)
    }
    
    // MARK: - Android -> JavaScript
    
    fun clickFailButton() {
        evaluateJavaScript(JavaScriptBridge.clickFailButton)
    }
    
    fun clickEndSessionButton() {
        evaluateJavaScript(JavaScriptBridge.clickEndSessionButton)
    }
    
    fun refreshButtonState() {
        evaluateJavaScript(JavaScriptBridge.checkFailButtonState)
    }
    
    fun reloadPage() {
        webView?.reload()
    }
    
    fun clearWebsiteData() {
        webView?.clearCache(true)
        webView?.clearHistory()
        android.webkit.CookieManager.getInstance().removeAllCookies(null)
        webView?.reload()
    }
    
    fun scrapeTargetWeight() {
        evaluateJavaScript(JavaScriptBridge.scrapeTargetWeight)
    }
    
    fun scrapeWeightOptions() {
        evaluateJavaScript(JavaScriptBridge.scrapeWeightOptions)
    }
    
    fun setTargetWeight(weightKg: Double) {
        evaluateJavaScript(JavaScriptBridge.setTargetWeightScript(weightKg))
    }
    
    fun recordBackgroundStart() {
        evaluateJavaScript("window._recordBackgroundStart()")
    }
    
    fun addBackgroundTime(milliseconds: Double) {
        evaluateJavaScript("window._addBackgroundTime($milliseconds)")
    }
    
    private fun evaluateJavaScript(script: String) {
        webView?.post {
            webView?.evaluateJavascript(script, null)
        }
    }
    
    // MARK: - Weight Parsing
    
    /**
     * Parse weight string like "20.0 kg" or "44 lbs" to Double (always returns kg)
     */
    private fun parseWeight(string: String): Double? {
        val lowercased = string.lowercase()
        val isLbs = lowercased.contains("lbs") || lowercased.contains("lb")
        
        val cleaned = lowercased
            .replace("lbs", "")
            .replace("lb", "")
            .replace("kg", "")
            .trim()
        
        val value = cleaned.toDoubleOrNull() ?: return null
        
        // Convert lbs to kg if needed (internal storage is always kg)
        return if (isLbs) value / AppConstants.KG_TO_LBS else value
    }
}
