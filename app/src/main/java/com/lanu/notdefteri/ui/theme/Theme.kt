package com.lanu.notdefteri.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.lanu.notdefteri.data.model.ColorPalette
import com.lanu.notdefteri.data.model.DarkModeOption
import com.lanu.notdefteri.data.model.ThemeConfig

fun getAppColorScheme(palette: ColorPalette, isDark: Boolean): ColorScheme {
    return when (palette) {
        ColorPalette.WARM_EARTH -> if (isDark) {
            darkColorScheme(
                primary = WarmEarthPrimaryDark,
                onPrimary = WarmEarthOnPrimaryDark,
                primaryContainer = WarmEarthPrimaryContainerDark,
                onPrimaryContainer = WarmEarthOnPrimaryContainerDark,
                secondary = WarmEarthSecondaryDark,
                onSecondary = WarmEarthOnSecondaryDark,
                secondaryContainer = WarmEarthSecondaryContainerDark,
                onSecondaryContainer = WarmEarthOnSecondaryContainerDark,
                background = WarmEarthBackgroundDark,
                onBackground = WarmEarthOnBackgroundDark,
                surface = WarmEarthSurfaceDark,
                onSurface = WarmEarthOnSurfaceDark,
                surfaceVariant = WarmEarthSurfaceVariantDark,
                onSurfaceVariant = WarmEarthOnSurfaceVariantDark,
                outline = WarmEarthOutlineDark,
                outlineVariant = WarmEarthOutlineVariantDark
            )
        } else {
            lightColorScheme(
                primary = WarmEarthPrimaryLight,
                onPrimary = WarmEarthOnPrimaryLight,
                primaryContainer = WarmEarthPrimaryContainerLight,
                onPrimaryContainer = WarmEarthOnPrimaryContainerLight,
                secondary = WarmEarthSecondaryLight,
                onSecondary = WarmEarthOnSecondaryLight,
                secondaryContainer = WarmEarthSecondaryContainerLight,
                onSecondaryContainer = WarmEarthOnSecondaryContainerLight,
                background = WarmEarthBackgroundLight,
                onBackground = WarmEarthOnBackgroundLight,
                surface = WarmEarthSurfaceLight,
                onSurface = WarmEarthOnSurfaceLight,
                surfaceVariant = WarmEarthSurfaceVariantLight,
                onSurfaceVariant = WarmEarthOnSurfaceVariantLight,
                outline = WarmEarthOutlineLight,
                outlineVariant = WarmEarthOutlineVariantLight
            )
        }

        ColorPalette.OCEAN_MIDNIGHT -> if (isDark) {
            darkColorScheme(
                primary = OceanPrimaryDark,
                onPrimary = OceanOnPrimaryDark,
                primaryContainer = OceanPrimaryContainerDark,
                onPrimaryContainer = OceanOnPrimaryContainerDark,
                secondary = OceanSecondaryDark,
                onSecondary = OceanOnSecondaryDark,
                secondaryContainer = OceanSecondaryContainerDark,
                onSecondaryContainer = OceanOnSecondaryContainerDark,
                background = OceanBackgroundDark,
                onBackground = OceanOnBackgroundDark,
                surface = OceanSurfaceDark,
                onSurface = OceanOnSurfaceDark,
                surfaceVariant = OceanSurfaceVariantDark,
                onSurfaceVariant = OceanOnSurfaceVariantDark,
                outline = OceanOutlineDark,
                outlineVariant = OceanOutlineVariantDark
            )
        } else {
            lightColorScheme(
                primary = OceanPrimaryLight,
                onPrimary = OceanOnPrimaryLight,
                primaryContainer = OceanPrimaryContainerLight,
                onPrimaryContainer = OceanOnPrimaryContainerLight,
                secondary = OceanSecondaryLight,
                onSecondary = OceanOnSecondaryLight,
                secondaryContainer = OceanSecondaryContainerLight,
                onSecondaryContainer = OceanOnSecondaryContainerLight,
                background = OceanBackgroundLight,
                onBackground = OceanOnBackgroundLight,
                surface = OceanSurfaceLight,
                onSurface = OceanOnSurfaceLight,
                surfaceVariant = OceanSurfaceVariantLight,
                onSurfaceVariant = OceanOnSurfaceVariantLight,
                outline = OceanOutlineLight,
                outlineVariant = OceanOutlineVariantLight
            )
        }

        ColorPalette.FOREST_EMERALD -> if (isDark) {
            darkColorScheme(
                primary = ForestPrimaryDark,
                onPrimary = ForestOnPrimaryDark,
                primaryContainer = ForestPrimaryContainerDark,
                onPrimaryContainer = ForestOnPrimaryContainerDark,
                secondary = ForestSecondaryDark,
                onSecondary = ForestOnSecondaryDark,
                secondaryContainer = ForestSecondaryContainerDark,
                onSecondaryContainer = ForestOnSecondaryContainerDark,
                background = ForestBackgroundDark,
                onBackground = ForestOnBackgroundDark,
                surface = ForestSurfaceDark,
                onSurface = ForestOnSurfaceDark,
                surfaceVariant = ForestSurfaceVariantDark,
                onSurfaceVariant = ForestOnSurfaceVariantDark,
                outline = ForestOutlineDark,
                outlineVariant = ForestOutlineVariantDark
            )
        } else {
            lightColorScheme(
                primary = ForestPrimaryLight,
                onPrimary = ForestOnPrimaryLight,
                primaryContainer = ForestPrimaryContainerLight,
                onPrimaryContainer = ForestOnPrimaryContainerLight,
                secondary = ForestSecondaryLight,
                onSecondary = ForestOnSecondaryLight,
                secondaryContainer = ForestSecondaryContainerLight,
                onSecondaryContainer = ForestOnSecondaryContainerLight,
                background = ForestBackgroundLight,
                onBackground = ForestOnBackgroundLight,
                surface = ForestSurfaceLight,
                onSurface = ForestOnSurfaceLight,
                surfaceVariant = ForestSurfaceVariantLight,
                onSurfaceVariant = ForestOnSurfaceVariantLight,
                outline = ForestOutlineLight,
                outlineVariant = ForestOutlineVariantLight
            )
        }

        ColorPalette.SUNSET_ROSE -> if (isDark) {
            darkColorScheme(
                primary = SunsetPrimaryDark,
                onPrimary = SunsetOnPrimaryDark,
                primaryContainer = SunsetPrimaryContainerDark,
                onPrimaryContainer = SunsetOnPrimaryContainerDark,
                secondary = SunsetSecondaryDark,
                onSecondary = SunsetOnSecondaryDark,
                secondaryContainer = SunsetSecondaryContainerDark,
                onSecondaryContainer = SunsetOnSecondaryContainerDark,
                background = SunsetBackgroundDark,
                onBackground = SunsetOnBackgroundDark,
                surface = SunsetSurfaceDark,
                onSurface = SunsetOnSurfaceDark,
                surfaceVariant = SunsetSurfaceVariantDark,
                onSurfaceVariant = SunsetOnSurfaceVariantDark,
                outline = SunsetOutlineDark,
                outlineVariant = SunsetOutlineVariantDark
            )
        } else {
            lightColorScheme(
                primary = SunsetPrimaryLight,
                onPrimary = SunsetOnPrimaryLight,
                primaryContainer = SunsetPrimaryContainerLight,
                onPrimaryContainer = SunsetOnPrimaryContainerLight,
                secondary = SunsetSecondaryLight,
                onSecondary = SunsetOnSecondaryLight,
                secondaryContainer = SunsetSecondaryContainerLight,
                onSecondaryContainer = SunsetOnSecondaryContainerLight,
                background = SunsetBackgroundLight,
                onBackground = SunsetOnBackgroundLight,
                surface = SunsetSurfaceLight,
                onSurface = SunsetOnSurfaceLight,
                surfaceVariant = SunsetSurfaceVariantLight,
                onSurfaceVariant = SunsetOnSurfaceVariantLight,
                outline = SunsetOutlineLight,
                outlineVariant = SunsetOutlineVariantLight
            )
        }

        ColorPalette.AMOLED_OBSIDIAN -> if (isDark) {
            darkColorScheme(
                primary = AmoledPrimaryDark,
                onPrimary = AmoledOnPrimaryDark,
                primaryContainer = AmoledPrimaryContainerDark,
                onPrimaryContainer = AmoledOnPrimaryContainerDark,
                secondary = AmoledSecondaryDark,
                onSecondary = AmoledOnSecondaryDark,
                secondaryContainer = AmoledSecondaryContainerDark,
                onSecondaryContainer = AmoledOnSecondaryContainerDark,
                background = AmoledBackgroundDark,
                onBackground = AmoledOnBackgroundDark,
                surface = AmoledSurfaceDark,
                onSurface = AmoledOnSurfaceDark,
                surfaceVariant = AmoledSurfaceVariantDark,
                onSurfaceVariant = AmoledOnSurfaceVariantDark,
                outline = AmoledOutlineDark,
                outlineVariant = AmoledOutlineVariantDark
            )
        } else {
            lightColorScheme(
                primary = AmoledPrimaryLight,
                onPrimary = AmoledOnPrimaryLight,
                primaryContainer = AmoledPrimaryContainerLight,
                onPrimaryContainer = AmoledOnPrimaryContainerLight,
                secondary = AmoledSecondaryLight,
                onSecondary = AmoledOnSecondaryLight,
                secondaryContainer = AmoledSecondaryContainerLight,
                onSecondaryContainer = AmoledOnSecondaryContainerLight,
                background = AmoledBackgroundLight,
                onBackground = AmoledOnBackgroundLight,
                surface = AmoledSurfaceLight,
                onSurface = AmoledOnSurfaceLight,
                surfaceVariant = AmoledSurfaceVariantLight,
                onSurfaceVariant = AmoledOnSurfaceVariantLight,
                outline = AmoledOutlineLight,
                outlineVariant = AmoledOutlineVariantLight
            )
        }
    }
}

@Composable
fun LanuTheme(
    themeConfig: ThemeConfig = ThemeConfig(),
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeConfig.darkMode) {
        DarkModeOption.SYSTEM -> systemDark
        DarkModeOption.DARK -> true
        DarkModeOption.LIGHT -> false
    }

    val colorScheme = getAppColorScheme(themeConfig.colorPalette, isDark)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
