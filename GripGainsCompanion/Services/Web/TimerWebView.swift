import SwiftUI
import WebKit

/// SwiftUI wrapper for WKWebView that displays the gripgains.ca timer page
struct TimerWebView: UIViewRepresentable {
    let coordinator: WebViewCoordinator

    func makeUIView(context: Context) -> WKWebView {
        let config = WKWebViewConfiguration()

        // Enable default caching
        config.websiteDataStore = WKWebsiteDataStore.default()

        // Suppress media content loading (not needed for this app)
        config.mediaTypesRequiringUserActionForPlayback = .all
        config.allowsInlineMediaPlayback = false

        // Disable JavaScript popup windows
        config.preferences.javaScriptCanOpenWindowsAutomatically = false

        let contentController = config.userContentController

        // Add message handler for JS -> Swift communication
        contentController.add(coordinator, name: "buttonState")

        // Inject observer script on document end
        let observerScript = WKUserScript(
            source: JavaScriptBridge.observerScript,
            injectionTime: .atDocumentEnd,
            forMainFrameOnly: true
        )
        contentController.addUserScript(observerScript)

        let webView = WKWebView(frame: .zero, configuration: config)
        webView.navigationDelegate = coordinator
        coordinator.setWebView(webView)

        // Load the gripgains timer page with caching
        var request = URLRequest(url: AppConstants.gripGainsURL)
        request.cachePolicy = .returnCacheDataElseLoad
        webView.load(request)

        return webView
    }

    func updateUIView(_ uiView: WKWebView, context: Context) {
        // No updates needed
    }

    static func dismantleUIView(_ uiView: WKWebView, coordinator: ()) {
        // Clean up message handler to avoid memory leaks
        uiView.configuration.userContentController.removeScriptMessageHandler(forName: "buttonState")
    }
}
