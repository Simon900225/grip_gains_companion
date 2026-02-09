package app.grip_gains_companion.service.web

import app.grip_gains_companion.config.AppConstants

/**
 * JavaScript code snippets for interacting with the gripgains.ca web UI
 */
object JavaScriptBridge {
    
    /**
     * Close the weight picker if it's open on page load
     * This handles the case where Vue restores the picker state after a page refresh
     */
    val closePickerOnLoadScript = """
        (function() {
            function closePickerIfOpen() {
                const picker = document.querySelector('.weight-picker-modal');
                if (picker) {
                    const closeBtn = picker.querySelector('.close-button');
                    if (closeBtn) closeBtn.click();
                }
            }
            if (document.readyState === 'loading') {
                document.addEventListener('DOMContentLoaded', closePickerIfOpen);
            } else {
                closePickerIfOpen();
            }
        })();
    """.trimIndent()
    
    /**
     * Patch Date.now() and timer functions to account for background time
     */
    val backgroundTimeOffsetScript = """
        (function() {
            let offset = 0;
            const originalDateNow = Date.now;
            const originalSetInterval = window.setInterval;
            const originalDateGetTime = Date.prototype.getTime;
            const activeIntervals = new Map();
            let timerElapsedAtBackgroundStart = 0;

            function getElapsedTime() {
                const el = document.querySelector('.elapsed-time');
                return el ? (parseInt(el.textContent.trim()) || 0) : 0;
            }

            window._recordBackgroundStart = function() {
                try {
                    timerElapsedAtBackgroundStart = getElapsedTime();
                } catch (e) {}
            };

            window._addBackgroundTime = function(ms) {
                try {
                    offset += ms;
                    const timerNow = getElapsedTime();
                    const actualAdvance = timerNow - timerElapsedAtBackgroundStart;
                    const expectedAdvance = Math.floor(ms / 1000);
                    const missedTicks = Math.max(0, expectedAdvance - actualAdvance);

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

            Date.now = function() {
                return originalDateNow() + offset;
            };

            Date.prototype.getTime = function() {
                return originalDateGetTime.call(this) + offset;
            };

            window.setInterval = function(callback, delay, ...args) {
                const wrappedCallback = typeof callback === 'function'
                    ? () => callback(...args)
                    : () => eval(callback);
                const id = originalSetInterval(wrappedCallback, delay);
                activeIntervals.set(id, { callback: wrappedCallback, delay: delay });
                return id;
            };

            const originalClearInterval = window.clearInterval;
            window.clearInterval = function(id) {
                activeIntervals.delete(id);
                return originalClearInterval(id);
            };
        })();
    """.trimIndent()
    
    /**
     * Click the fail button
     */
    val clickFailButton = """
        (function() {
            const button = document.querySelector('button.btn-fail-prominent');
            if (button && !button.disabled) {
                button.click();
            }
        })();
    """.trimIndent()
    
    /**
     * Click the "End Session" button to abort the session
     */
    val clickEndSessionButton = """
        (function() {
            const button = document.querySelector('button.btn-danger.btn-lg.session-actions-end');
            if (button && !button.disabled) {
                button.click();
            }
        })();
    """.trimIndent()
    
    /**
     * Check if fail button is enabled and send result to Android
     */
    val checkFailButtonState = """
        (function() {
            const button = document.querySelector('button.btn-fail-prominent');
            const enabled = button && !button.disabled;
            Android.onButtonStateChanged(enabled);
        })();
    """.trimIndent()
    
    /**
     * MutationObserver script for real-time button state changes
     */
    val observerScript = """
        (function() {
            function setupObserver() {
                const button = document.querySelector('button.btn-fail-prominent');
                if (!button) {
                    setTimeout(setupObserver, 100);
                    return;
                }

                const observer = new MutationObserver(function() {
                    Android.onButtonStateChanged(!button.disabled);
                });

                observer.observe(button, {
                    attributes: true,
                    attributeFilter: ['disabled', 'class']
                });

                Android.onButtonStateChanged(!button.disabled);
            }

            if (document.readyState === 'loading') {
                document.addEventListener('DOMContentLoaded', setupObserver);
            } else {
                setupObserver();
            }
        })();
    """.trimIndent()
    
    /**
     * Scrape target weight from the session preview header
     */
    val scrapeTargetWeight = """
        (function() {
            const elements = document.querySelectorAll('.session-preview-header .text-white');
            for (const elem of elements) {
                const text = elem.textContent.trim();
                if (text.includes('kg') || text.includes('lbs') || text.includes('lb')) {
                    Android.onTargetWeightChanged(text);
                    return;
                }
            }
            Android.onTargetWeightChanged(null);
        })();
    """.trimIndent()
    
    /**
     * MutationObserver script for real-time target weight and duration changes
     */
    val targetWeightObserverScript = """
        (function() {
            function scrapeAndSendValues() {
                const elements = document.querySelectorAll('.session-preview-header .text-white');
                let foundWeight = false;
                let foundDuration = false;

                for (const elem of elements) {
                    const text = elem.textContent.trim();

                    if (!foundWeight && (text.includes('kg') || text.includes('lbs') || text.includes('lb'))) {
                        Android.onTargetWeightChanged(text);
                        foundWeight = true;
                    }

                    if (!foundDuration && text.endsWith('s') && !text.includes('kg') && !text.includes('lb')) {
                        const seconds = parseInt(text);
                        if (!isNaN(seconds) && seconds > 0) {
                            Android.onTargetDurationChanged(seconds);
                            foundDuration = true;
                        }
                    }
                }

                if (!foundWeight) {
                    Android.onTargetWeightChanged(null);
                }
                if (!foundDuration) {
                    Android.onTargetDurationChanged(-1);
                }

                const purpleElements = document.querySelectorAll('.session-preview-header .text-purple-200');
                const gripper = purpleElements.length > 0 ? purpleElements[0].textContent.trim() : null;
                const side = purpleElements.length > 1 ? purpleElements[1].textContent.trim() : null;
                Android.onSessionInfoChanged(gripper, side);
            }

            function setupTargetObserver() {
                const previewHeader = document.querySelector('.session-preview-header');
                if (!previewHeader) {
                    setTimeout(setupTargetObserver, 500);
                    return;
                }

                const observer = new MutationObserver(function() {
                    scrapeAndSendValues();
                });

                observer.observe(previewHeader, {
                    childList: true,
                    subtree: true,
                    characterData: true
                });

                scrapeAndSendValues();
            }

            if (document.readyState === 'loading') {
                document.addEventListener('DOMContentLoaded', setupTargetObserver);
            } else {
                setupTargetObserver();
            }
        })();
    """.trimIndent()
    
    /**
     * MutationObserver script to detect settings visibility changes
     */
    val settingsVisibilityObserverScript = """
        (function() {
            let lastVisible = null;

            function checkAndSend() {
                const advancedHeader = document.querySelector('.advanced-settings-header');
                const isVisible = advancedHeader !== null && advancedHeader.offsetParent !== null;
                if (isVisible !== lastVisible) {
                    lastVisible = isVisible;
                    Android.onSettingsVisibleChanged(isVisible);
                }
            }

            const observer = new MutationObserver(checkAndSend);
            observer.observe(document.body, { childList: true, subtree: true });

            if (document.readyState === 'loading') {
                document.addEventListener('DOMContentLoaded', checkAndSend);
            } else {
                checkAndSend();
            }
        })();
    """.trimIndent()
    
    /**
     * Generate script to set target weight in the web UI picker
     */
    fun setTargetWeightScript(weightKg: Double): String = """
        (function() {
            const KG_TO_LBS = ${AppConstants.KG_TO_LBS};
            const targetKg = $weightKg;

            const button = document.querySelector('.weight-picker-button');
            if (!button) return;

            // Inject CSS to hide the picker while we interact with it
            const style = document.createElement('style');
            style.id = 'auto-select-hide';
            style.textContent = '.weight-picker-modal { visibility: hidden !important; opacity: 0 !important; position: fixed !important; }';
            document.head.appendChild(style);

            // Click to open the picker
            button.click();

            // Wait for picker to render, then find options
            setTimeout(() => {
                const options = document.querySelectorAll('.weight-option');
                if (!options.length) {
                    style.remove();
                    return;
                }

                const firstText = options[0].textContent.trim();
                const isLbs = firstText.toLowerCase().includes('lb');
                const targetValue = isLbs ? targetKg * KG_TO_LBS : targetKg;

                let closest = null;
                let closestDiff = Infinity;

                options.forEach(opt => {
                    const text = opt.textContent.trim();
                    const value = parseFloat(text);
                    const diff = Math.abs(value - targetValue);
                    if (diff < closestDiff) {
                        closestDiff = diff;
                        closest = opt;
                    }
                });

                // Temporarily switch to opacity-based hiding to allow clicking
                style.textContent = '.weight-picker-modal { opacity: 0 !important; pointer-events: auto !important; }';

                // Click the closest option (Vue handles the rest)
                if (closest) closest.click();

                // Remove the hiding style after picker closes
                setTimeout(() => style.remove(), 100);
            }, 50);
        })();
    """.trimIndent()
    
    /**
     * Scrape available weight options from the picker
     */
    val scrapeWeightOptions = """
        (function() {
            const button = document.querySelector('.weight-picker-button');
            if (!button) {
                Android.onWeightOptionsChanged('[]', false);
                return;
            }

            // Inject CSS to hide the modal while we interact
            const style = document.createElement('style');
            style.id = 'scrape-options-hide';
            style.textContent = '.weight-picker-modal { visibility: hidden !important; opacity: 0 !important; position: fixed !important; }';
            document.head.appendChild(style);

            // Click to open the picker
            button.click();

            // Wait for picker to render, then scrape options
            setTimeout(() => {
                const options = document.querySelectorAll('.weight-option');
                const weights = [];
                let isLbs = false;

                options.forEach(opt => {
                    const text = opt.textContent.trim().toLowerCase();
                    const value = parseFloat(text);
                    if (!isNaN(value)) {
                        weights.push(value);
                        if (text.includes('lb')) isLbs = true;
                    }
                });

                // Temporarily switch to opacity-based hiding to allow clicking
                style.textContent = '.weight-picker-modal { opacity: 0 !important; pointer-events: auto !important; }';

                // Close picker by clicking the close button
                const picker = document.querySelector('.weight-picker-modal');
                if (picker) {
                    const closeBtn = picker.querySelector('.close-button');
                    if (closeBtn) {
                        closeBtn.click();
                    } else {
                        button.click();
                    }
                }

                // Remove the hiding style after picker closes
                setTimeout(() => style.remove(), 150);

                Android.onWeightOptionsChanged(JSON.stringify(weights), isLbs);
            }, 100);
        })();
    """.trimIndent()
    
    /**
     * MutationObserver script for remaining time from timer display
     */
    val remainingTimeObserverScript = """
        (function() {
            function scrapeAndSendRemainingTime() {
                const timerValue = document.querySelector('.timer-value');
                if (!timerValue) {
                    Android.onRemainingTimeChanged(-9999);
                    return;
                }

                const text = timerValue.textContent.trim();
                let seconds;

                if (text.startsWith('+')) {
                    seconds = -parseInt(text.substring(1));
                } else {
                    seconds = parseInt(text);
                }

                if (!isNaN(seconds)) {
                    Android.onRemainingTimeChanged(seconds);
                } else {
                    Android.onRemainingTimeChanged(-9999);
                }
            }

            function setupRemainingTimeObserver() {
                const timerValue = document.querySelector('.timer-value');
                if (!timerValue) {
                    setTimeout(setupRemainingTimeObserver, 200);
                    return;
                }

                const observer = new MutationObserver(function() {
                    scrapeAndSendRemainingTime();
                });

                observer.observe(timerValue, {
                    childList: true,
                    subtree: true,
                    characterData: true
                });

                if (timerValue.parentElement) {
                    observer.observe(timerValue.parentElement, {
                        childList: true,
                        subtree: true
                    });
                }

                scrapeAndSendRemainingTime();
            }

            if (document.readyState === 'loading') {
                document.addEventListener('DOMContentLoaded', setupRemainingTimeObserver);
            } else {
                setupRemainingTimeObserver();
            }
        })();
    """.trimIndent()
    
    /**
     * MutationObserver script to detect "Save to Database" button appearance
     */
    val saveButtonObserverScript = """
        (function() {
            let lastSaveButtonVisible = false;

            function checkSaveButton() {
                const buttons = document.querySelectorAll('button.btn.btn-primary');
                let saveButtonFound = false;

                for (const button of buttons) {
                    if (button.textContent.trim() === 'Save to Database') {
                        saveButtonFound = true;
                        break;
                    }
                }

                if (saveButtonFound && !lastSaveButtonVisible) {
                    Android.onSaveButtonAppeared();
                }
                lastSaveButtonVisible = saveButtonFound;
            }

            function setupSaveButtonObserver() {
                const observer = new MutationObserver(function() {
                    checkSaveButton();
                });

                observer.observe(document.body, {
                    childList: true,
                    subtree: true
                });

                checkSaveButton();
            }

            if (document.readyState === 'loading') {
                document.addEventListener('DOMContentLoaded', setupSaveButtonObserver);
            } else {
                setupSaveButtonObserver();
            }
        })();
    """.trimIndent()
}
