package com.lanu.notdefteri.data.model

import com.lanu.notdefteri.R

enum class DarkModeOption(val title: String) {
    SYSTEM("Sistem"),
    LIGHT("Aydınlık"),
    DARK("Karanlık")
}

enum class ColorPalette(
    val id: String,
    val title: String,
    val primaryColorHex: Long,
    val surfaceColorHex: Long
) {
    WARM_EARTH(
        id = "warm_earth",
        title = "Sıcak Toprak",
        primaryColorHex = 0xFFC25D23,
        surfaceColorHex = 0xFFF6F2EA
    ),
    OCEAN_MIDNIGHT(
        id = "ocean_midnight",
        title = "Gece Okyanusu",
        primaryColorHex = 0xFF2563EB,
        surfaceColorHex = 0xFF1E293B
    ),
    FOREST_EMERALD(
        id = "forest_emerald",
        title = "Zümrüt Doğa",
        primaryColorHex = 0xFF059669,
        surfaceColorHex = 0xFF132A13
    ),
    SUNSET_ROSE(
        id = "sunset_rose",
        title = "Gün Batımı",
        primaryColorHex = 0xFFE11D48,
        surfaceColorHex = 0xFF2B0914
    ),
    AMOLED_OBSIDIAN(
        id = "amoled_obsidian",
        title = "AMOLED Siyah",
        primaryColorHex = 0xFF94A3B8,
        surfaceColorHex = 0xFF000000
    )
}

enum class WallpaperPreset(
    val id: String,
    val title: String,
    val drawableRes: Int?
) {
    NONE(
        id = "none",
        title = "Sade / Düz Renk",
        drawableRes = null
    ),
    ABSTRACT_SHAPES(
        id = "abstract_shapes",
        title = "Pastel Sanat",
        drawableRes = R.drawable.img_wallpaper_abstract
    ),
    DARK_COSMIC(
        id = "dark_cosmic",
        title = "Gece Samanyolu",
        drawableRes = R.drawable.img_wallpaper_cosmic
    ),
    BOTANICAL_EMERALD(
        id = "botanical_emerald",
        title = "Zümrüt Botanik",
        drawableRes = R.drawable.img_wallpaper_botanical
    ),
    CUSTOM_GALLERY(
        id = "custom_gallery",
        title = "Galeriden Seç",
        drawableRes = null
    )
}

enum class NoteLayoutMode(val id: String, val title: String) {
    STAGGERED_GRID("staggered_grid", "Staggered Grid (2 Sütun)"),
    SINGLE_COLUMN("single_column", "Geniş Kartlar (1 Sütun)")
}

data class ThemeConfig(
    val darkMode: DarkModeOption = DarkModeOption.SYSTEM,
    val colorPalette: ColorPalette = ColorPalette.WARM_EARTH,
    val wallpaper: WallpaperPreset = WallpaperPreset.NONE,
    val customWallpaperUri: String? = null,
    val wallpaperDim: Float = 0.35f,
    val layoutMode: NoteLayoutMode = NoteLayoutMode.STAGGERED_GRID
)
