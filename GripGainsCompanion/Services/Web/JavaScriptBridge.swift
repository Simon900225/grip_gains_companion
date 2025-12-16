import Foundation

/// JavaScript code snippets for interacting with the gripgains.ca web UI
enum JavaScriptBridge {
    /// Click the fail button
    static let clickFailButton = """
        (function() {
            const button = document.querySelector('button.btn-fail-prominent');
            if (button && !button.disabled) {
                button.click();
            }
        })();
    """

    /// Check if fail button is enabled
    static let checkFailButtonState = """
        (function() {
            const button = document.querySelector('button.btn-fail-prominent');
            const enabled = button && !button.disabled;
            window.webkit.messageHandlers.buttonState.postMessage(enabled);
        })();
    """

    /// MutationObserver script for real-time button state changes
    static let observerScript = """
        (function() {
            function setupObserver() {
                const button = document.querySelector('button.btn-fail-prominent');
                if (!button) {
                    // Button not ready, retry in 100ms
                    setTimeout(setupObserver, 100);
                    return;
                }

                const observer = new MutationObserver(function() {
                    window.webkit.messageHandlers.buttonState.postMessage(!button.disabled);
                });

                // Watch ONLY the button, not entire body
                observer.observe(button, {
                    attributes: true,
                    attributeFilter: ['disabled', 'class']
                });

                // Initial state
                window.webkit.messageHandlers.buttonState.postMessage(!button.disabled);
            }

            if (document.readyState === 'loading') {
                document.addEventListener('DOMContentLoaded', setupObserver);
            } else {
                setupObserver();
            }
        })();
    """
}
