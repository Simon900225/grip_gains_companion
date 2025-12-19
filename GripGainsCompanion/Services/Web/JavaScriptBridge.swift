import Foundation

/// JavaScript code snippets for interacting with the gripgains.ca web UI
enum JavaScriptBridge {
    /// Patch Date.now() and timer functions to account for background time
    /// Must be injected at document start before any other scripts run
    static let backgroundTimeOffsetScript = """
        (function() {
            let offset = 0;
            const originalDateNow = Date.now;
            const originalSetInterval = window.setInterval;
            const originalDateGetTime = Date.prototype.getTime;

            // Track active intervals for catch-up ticks
            const activeIntervals = new Map();

            // Track timer state at background start
            let timerElapsedAtBackgroundStart = 0;

            // Get elapsed time from DOM timer
            function getElapsedTime() {
                const el = document.querySelector('.elapsed-time');
                return el ? (parseInt(el.textContent.trim()) || 0) : 0;
            }

            // Called when app enters background
            window._recordBackgroundStart = function() {
                try {
                    timerElapsedAtBackgroundStart = getElapsedTime();
                } catch (e) {}
            };

            // Called when app resumes from background
            window._addBackgroundTime = function(ms) {
                try {
                    offset += ms;

                    // Calculate missed display ticks (JS is throttled in background)
                    const timerNow = getElapsedTime();
                    const actualAdvance = timerNow - timerElapsedAtBackgroundStart;
                    const expectedAdvance = Math.floor(ms / 1000);
                    const missedTicks = Math.max(0, expectedAdvance - actualAdvance);

                    // Fire missed ticks to update display
                    if (missedTicks > 0) {
                        activeIntervals.forEach((info) => {
                            if (info.callback) {
                                for (let i = 0; i < missedTicks; i++) {
                                    try { info.callback(); } catch (e) {}
                                }
                            }
                        });
                    }
                } catch (e) {}
            };

            // Patch Date.now to include offset
            Date.now = function() {
                return originalDateNow() + offset;
            };

            // Patch Date.prototype.getTime to include offset
            Date.prototype.getTime = function() {
                return originalDateGetTime.call(this) + offset;
            };

            // Track setInterval calls
            window.setInterval = function(callback, delay, ...args) {
                const wrappedCallback = typeof callback === 'function'
                    ? () => callback(...args)
                    : () => eval(callback);
                const id = originalSetInterval(wrappedCallback, delay);
                activeIntervals.set(id, { callback: wrappedCallback, delay: delay });
                return id;
            };

            // Clean up interval tracking
            const originalClearInterval = window.clearInterval;
            window.clearInterval = function(id) {
                activeIntervals.delete(id);
                return originalClearInterval(id);
            };
        })();
    """

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

    /// Scrape target weight from the session preview header
    static let scrapeTargetWeight = """
        (function() {
            const elements = document.querySelectorAll('.session-preview-header .text-white');
            for (const elem of elements) {
                const text = elem.textContent.trim();
                if (text.includes('kg') || text.includes('lbs') || text.includes('lb')) {
                    window.webkit.messageHandlers.targetWeight.postMessage(text);
                    return;
                }
            }
            window.webkit.messageHandlers.targetWeight.postMessage(null);
        })();
    """

    /// MutationObserver script for real-time target weight changes
    static let targetWeightObserverScript = """
        (function() {
            function scrapeAndSendWeight() {
                const elements = document.querySelectorAll('.session-preview-header .text-white');
                for (const elem of elements) {
                    const text = elem.textContent.trim();
                    if (text.includes('kg') || text.includes('lbs') || text.includes('lb')) {
                        window.webkit.messageHandlers.targetWeight.postMessage(text);
                        return;
                    }
                }
                window.webkit.messageHandlers.targetWeight.postMessage(null);
            }

            function setupTargetWeightObserver() {
                const previewHeader = document.querySelector('.session-preview-header');
                if (!previewHeader) {
                    // Preview not ready, retry in 500ms
                    setTimeout(setupTargetWeightObserver, 500);
                    return;
                }

                const observer = new MutationObserver(function() {
                    scrapeAndSendWeight();
                });

                // Watch for changes in the preview header
                observer.observe(previewHeader, {
                    childList: true,
                    subtree: true,
                    characterData: true
                });

                // Send initial value
                scrapeAndSendWeight();
            }

            if (document.readyState === 'loading') {
                document.addEventListener('DOMContentLoaded', setupTargetWeightObserver);
            } else {
                setupTargetWeightObserver();
            }
        })();
    """
}
