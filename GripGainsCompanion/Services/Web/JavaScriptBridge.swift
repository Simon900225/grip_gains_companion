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

    /// MutationObserver script for real-time target weight and duration changes
    static let targetWeightObserverScript = """
        (function() {
            function scrapeAndSendValues() {
                const elements = document.querySelectorAll('.session-preview-header .text-white');
                let foundWeight = false;
                let foundDuration = false;

                for (const elem of elements) {
                    const text = elem.textContent.trim();

                    // Check for weight (contains kg or lbs)
                    if (!foundWeight && (text.includes('kg') || text.includes('lbs') || text.includes('lb'))) {
                        window.webkit.messageHandlers.targetWeight.postMessage(text);
                        foundWeight = true;
                    }

                    // Check for duration (ends with 's' but not weight units)
                    if (!foundDuration && text.endsWith('s') && !text.includes('kg') && !text.includes('lb')) {
                        const seconds = parseInt(text);
                        if (!isNaN(seconds) && seconds > 0) {
                            window.webkit.messageHandlers.targetDuration.postMessage(seconds);
                            foundDuration = true;
                        }
                    }
                }

                if (!foundWeight) {
                    window.webkit.messageHandlers.targetWeight.postMessage(null);
                }
                if (!foundDuration) {
                    window.webkit.messageHandlers.targetDuration.postMessage(null);
                }
            }

            function setupTargetObserver() {
                const previewHeader = document.querySelector('.session-preview-header');
                if (!previewHeader) {
                    // Preview not ready, retry in 500ms
                    setTimeout(setupTargetObserver, 500);
                    return;
                }

                const observer = new MutationObserver(function() {
                    scrapeAndSendValues();
                });

                // Watch for changes in the preview header
                observer.observe(previewHeader, {
                    childList: true,
                    subtree: true,
                    characterData: true
                });

                // Send initial values
                scrapeAndSendValues();
            }

            if (document.readyState === 'loading') {
                document.addEventListener('DOMContentLoaded', setupTargetObserver);
            } else {
                setupTargetObserver();
            }
        })();
    """

    /// MutationObserver script for real-time remaining time from timer display
    static let remainingTimeObserverScript = """
        (function() {
            function scrapeAndSendRemainingTime() {
                const timerValue = document.querySelector('.timer-value');
                if (!timerValue) {
                    window.webkit.messageHandlers.remainingTime.postMessage(null);
                    return;
                }

                const text = timerValue.textContent.trim();
                let seconds;

                if (text.startsWith('+')) {
                    // Past target: "+3" means 3 seconds overtime, store as -3
                    seconds = -parseInt(text.substring(1));
                } else {
                    // Normal: "30" means 30 seconds remaining
                    seconds = parseInt(text);
                }

                if (!isNaN(seconds)) {
                    window.webkit.messageHandlers.remainingTime.postMessage(seconds);
                } else {
                    window.webkit.messageHandlers.remainingTime.postMessage(null);
                }
            }

            function setupRemainingTimeObserver() {
                // Watch for timer-value element to appear
                const timerValue = document.querySelector('.timer-value');
                if (!timerValue) {
                    // Timer not ready, retry in 200ms
                    setTimeout(setupRemainingTimeObserver, 200);
                    return;
                }

                const observer = new MutationObserver(function() {
                    scrapeAndSendRemainingTime();
                });

                // Watch the timer value for text changes
                observer.observe(timerValue, {
                    childList: true,
                    subtree: true,
                    characterData: true
                });

                // Also watch parent for class changes (timer state changes)
                if (timerValue.parentElement) {
                    observer.observe(timerValue.parentElement, {
                        childList: true,
                        subtree: true
                    });
                }

                // Send initial value
                scrapeAndSendRemainingTime();
            }

            if (document.readyState === 'loading') {
                document.addEventListener('DOMContentLoaded', setupRemainingTimeObserver);
            } else {
                setupRemainingTimeObserver();
            }
        })();
    """
}
