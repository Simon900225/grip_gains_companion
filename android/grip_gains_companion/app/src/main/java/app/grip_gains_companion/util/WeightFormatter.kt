package app.grip_gains_companion.util

import app.grip_gains_companion.config.AppConstants
import java.util.Locale

/**
 * Formats weight values for display
 */
object WeightFormatter {
    
    /**
     * Format a weight value in kg to the user's preferred unit
     * @param weightKg Weight in kilograms
     * @param useLbs Whether to display in pounds
     * @param includeUnit Whether to include the unit suffix
     * @param decimals Number of decimal places
     */
    fun format(
        weightKg: Double,
        useLbs: Boolean,
        includeUnit: Boolean = true,
        decimals: Int = 1
    ): String {
        val value = if (useLbs) weightKg * AppConstants.KG_TO_LBS else weightKg
        val unit = if (useLbs) "lbs" else "kg"
        val formatString = "%.${decimals}f"
        val formatted = String.format(Locale.US, formatString, value)
        return if (includeUnit) "$formatted $unit" else formatted
    }
    
    /**
     * Convert a display value to kg
     * @param displayValue The value as displayed (in kg or lbs)
     * @param isLbs Whether the display value is in pounds
     */
    fun toKg(displayValue: Double, isLbs: Boolean): Double {
        return if (isLbs) displayValue / AppConstants.KG_TO_LBS else displayValue
    }
    
    /**
     * Convert kg to display value
     * @param weightKg Weight in kilograms
     * @param useLbs Whether to convert to pounds
     */
    fun toDisplayValue(weightKg: Double, useLbs: Boolean): Double {
        return if (useLbs) weightKg * AppConstants.KG_TO_LBS else weightKg
    }
}
