package com.lanu.notdefteri.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min

/**
 * WCAG 2.1 Compliant Utility for calculating relative luminance and contrast ratios
 * to ensure optimal text readability (black vs white) on any theme or wallpaper background.
 */
object ColorContrastHelper {
    fun calculateLuminance(color: Color): Float = ColorUtils.calculateLuminance(color)
    fun calculateContrastRatio(foreground: Color, background: Color): Float = ColorUtils.calculateContrastRatio(foreground, background)
    fun getContrastTextColor(backgroundColor: Color): Color = ColorUtils.getContrastingTextColor(backgroundColor)
    fun isLightBackground(backgroundColor: Color): Boolean = ColorUtils.isLightBackground(backgroundColor)
    fun getPrimaryTextColor(backgroundColor: Color): Color = ColorUtils.getPrimaryTitleColor(backgroundColor)
    fun getBodyTextColor(backgroundColor: Color): Color = ColorUtils.getBodyContentColor(backgroundColor)
    fun getSubtleTextColor(backgroundColor: Color): Color = ColorUtils.getSubtleMetadataColor(backgroundColor)
    fun getIconTint(backgroundColor: Color): Color = ColorUtils.getActionIconTint(backgroundColor)
}
