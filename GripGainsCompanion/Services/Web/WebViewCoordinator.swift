import WebKit

/// Coordinator for WKWebView that handles JavaScript message callbacks
/// This bridges JavaScript calls back to Swift
class WebViewCoordinator: NSObject, WKScriptMessageHandler, WKNavigationDelegate {
    private weak var webView: WKWebView?

    /// Callback when button state changes
    var onButtonStateChanged: ((Bool) -> Void)?

    override init() {
        super.init()
    }

    func setWebView(_ webView: WKWebView) {
        self.webView = webView
    }

    // MARK: - WKNavigationDelegate

    func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
        // MutationObserver in JavaScript handles button state changes
    }

    // MARK: - WKScriptMessageHandler

    func userContentController(_ userContentController: WKUserContentController,
                               didReceive message: WKScriptMessage) {
        switch message.name {
        case "buttonState":
            if let enabled = message.body as? Bool {
                DispatchQueue.main.async { [weak self] in
                    self?.onButtonStateChanged?(enabled)
                }
            }

        default:
            break
        }
    }

    // MARK: - Public Methods

    /// Click the fail button via JavaScript injection
    func clickFailButton() {
        Task { @MainActor in
            await clickFailButtonAsync()
        }
    }

    /// Click the fail button via JavaScript injection (async version)
    @MainActor
    func clickFailButtonAsync() async {
        do {
            _ = try await webView?.evaluateJavaScript(JavaScriptBridge.clickFailButton)
        } catch {
            Log.app.error("Error clicking fail button: \(error.localizedDescription)")
        }
    }

    /// Request current button state from the page (for manual refresh if needed)
    func refreshButtonState() {
        Task { @MainActor in
            await refreshButtonStateAsync()
        }
    }

    /// Request current button state from the page (async version)
    @MainActor
    func refreshButtonStateAsync() async {
        do {
            _ = try await webView?.evaluateJavaScript(JavaScriptBridge.checkFailButtonState)
        } catch {
            Log.app.error("Error refreshing button state: \(error.localizedDescription)")
        }
    }

    /// Reload the current page
    func reloadPage() {
        Task { @MainActor in
            webView?.reload()
        }
    }

    /// Clear all website data (cache, cookies, storage) and reload
    func clearWebsiteData() {
        Task { @MainActor in
            let dataStore = WKWebsiteDataStore.default()
            let dataTypes = WKWebsiteDataStore.allWebsiteDataTypes()
            let date = Date(timeIntervalSince1970: 0)
            await dataStore.removeData(ofTypes: dataTypes, modifiedSince: date)
            webView?.reload()
        }
    }
}
