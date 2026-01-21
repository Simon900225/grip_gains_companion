package app.grip_gains_companion.ui.components

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import app.grip_gains_companion.config.AppConstants
import app.grip_gains_companion.service.web.JavaScriptBridge
import app.grip_gains_companion.service.web.WebViewBridge

/**
 * Composable wrapper for WebView that displays the gripgains.ca timer page
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TimerWebView(
    webViewBridge: WebViewBridge,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    val webView = remember {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            
            // Configure WebView settings
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                cacheMode = WebSettings.LOAD_DEFAULT
                setSupportZoom(false)
                builtInZoomControls = false
                displayZoomControls = false
                loadWithOverviewMode = true
                useWideViewPort = true
                
                // Security settings
                allowFileAccess = false
                allowContentAccess = false
            }
            
            // Add JavaScript interface
            addJavascriptInterface(webViewBridge, "Android")
            webViewBridge.setWebView(this)
            
            // Set up WebViewClient
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    
                    // Inject scripts after page loads
                    view?.evaluateJavascript(JavaScriptBridge.backgroundTimeOffsetScript, null)
                    view?.evaluateJavascript(JavaScriptBridge.closePickerOnLoadScript, null)
                    view?.evaluateJavascript(JavaScriptBridge.observerScript, null)
                    view?.evaluateJavascript(JavaScriptBridge.targetWeightObserverScript, null)
                    view?.evaluateJavascript(JavaScriptBridge.remainingTimeObserverScript, null)
                    view?.evaluateJavascript(JavaScriptBridge.settingsVisibilityObserverScript, null)
                    view?.evaluateJavascript(JavaScriptBridge.saveButtonObserverScript, null)
                }
            }
            
            // Load the gripgains timer page
            loadUrl(AppConstants.GRIP_GAINS_URL)
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            webView.destroy()
        }
    }
    
    AndroidView(
        factory = { webView },
        modifier = modifier
    )
}
