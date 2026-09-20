package com.lanu.notdefteri.util

import android.content.Context
import android.content.SharedPreferences
import com.lanu.notdefteri.data.model.ColorPalette
import com.lanu.notdefteri.data.model.DarkModeOption
import com.lanu.notdefteri.data.model.NoteLayoutMode
import com.lanu.notdefteri.data.model.ThemeConfig
import com.lanu.notdefteri.data.model.WallpaperPreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ThemePreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("lanu_theme_prefs", Context.MODE_PRIVATE)

    private val _themeConfig = MutableStateFlow(loadConfig())
    val themeConfig: StateFlow<ThemeConfig> = _themeConfig.asStateFlow()

    private fun loadConfig(): ThemeConfig {
        val darkModeStr = prefs.getString(KEY_DARK_MODE, DarkModeOption.SYSTEM.name)
        val darkMode = try {
            DarkModeOption.valueOf(darkModeStr ?: DarkModeOption.SYSTEM.name)
        } catch (e: Exception) {
            DarkModeOption.SYSTEM
        }

        val paletteStr = prefs.getString(KEY_PALETTE, ColorPalette.WARM_EARTH.name)
        val palette = try {
            ColorPalette.valueOf(paletteStr ?: ColorPalette.WARM_EARTH.name)
        } catch (e: Exception) {
            ColorPalette.WARM_EARTH
        }

        val wallpaperStr = prefs.getString(KEY_WALLPAPER, WallpaperPreset.NONE.name)
        val wallpaper = try {
            WallpaperPreset.valueOf(wallpaperStr ?: WallpaperPreset.NONE.name)
        } catch (e: Exception) {
            WallpaperPreset.NONE
        }

        val customUri = prefs.getString(KEY_CUSTOM_WALLPAPER_URI, null)
        val dim = prefs.getFloat(KEY_WALLPAPER_DIM, 0.35f)

        val layoutStr = prefs.getString(KEY_LAYOUT_MODE, NoteLayoutMode.STAGGERED_GRID.name)
        val layoutMode = try {
            NoteLayoutMode.valueOf(layoutStr ?: NoteLayoutMode.STAGGERED_GRID.name)
        } catch (e: Exception) {
            NoteLayoutMode.STAGGERED_GRID
        }

        return ThemeConfig(
            darkMode = darkMode,
            colorPalette = palette,
            wallpaper = wallpaper,
            customWallpaperUri = customUri,
            wallpaperDim = dim,
            layoutMode = layoutMode
        )
    }

    fun setDarkMode(mode: DarkModeOption) {
        prefs.edit().putString(KEY_DARK_MODE, mode.name).apply()
        _themeConfig.update { it.copy(darkMode = mode) }
    }

    fun setColorPalette(palette: ColorPalette) {
        prefs.edit().putString(KEY_PALETTE, palette.name).apply()
        _themeConfig.update { it.copy(colorPalette = palette) }
    }

    fun setWallpaper(preset: WallpaperPreset) {
        prefs.edit().putString(KEY_WALLPAPER, preset.name).apply()
        _themeConfig.update { it.copy(wallpaper = preset) }
    }

    fun setCustomWallpaperUri(uri: String?) {
        prefs.edit()
            .putString(KEY_WALLPAPER, WallpaperPreset.CUSTOM_GALLERY.name)
            .putString(KEY_CUSTOM_WALLPAPER_URI, uri)
            .apply()
        _themeConfig.update {
            it.copy(
                wallpaper = WallpaperPreset.CUSTOM_GALLERY,
                customWallpaperUri = uri
            )
        }
    }

    fun setWallpaperDim(dim: Float) {
        val clamped = dim.coerceIn(0.05f, 0.85f)
        prefs.edit().putFloat(KEY_WALLPAPER_DIM, clamped).apply()
        _themeConfig.update { it.copy(wallpaperDim = clamped) }
    }

    fun setLayoutMode(layoutMode: NoteLayoutMode) {
        prefs.edit().putString(KEY_LAYOUT_MODE, layoutMode.name).apply()
        _themeConfig.update { it.copy(layoutMode = layoutMode) }
    }

    companion object {
        private const val KEY_DARK_MODE = "pref_dark_mode"
        private const val KEY_PALETTE = "pref_color_palette"
        private const val KEY_WALLPAPER = "pref_wallpaper_preset"
        private const val KEY_CUSTOM_WALLPAPER_URI = "pref_custom_wallpaper_uri"
        private const val KEY_WALLPAPER_DIM = "pref_wallpaper_dim"
        private const val KEY_LAYOUT_MODE = "pref_layout_mode"
    }
}
