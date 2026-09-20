package com.lanu.notdefteri.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min

/**
 * WCAG 2.1 standartlarına uygun, arka plan rengine göre bağıl parlaklık (relative luminance)
 * ve kontrast oranını (contrast ratio) otomatik hesaplayan ColorUtils yardımcı sınıfı.
 */
object ColorUtils {

    /**
     * W3C WCAG 2.1 formülüyle rengin bağıl parlaklığını (Luminance) hesaplar.
     * Değer 0.0 (tam siyah) ile 1.0 (tam beyaz) arasındadır.
     */
    fun calculateLuminance(color: Color): Float {
        return color.luminance()
    }

    /**
     * İki renk arasındaki WCAG kontrast oranını hesaplar: (L1 + 0.05) / (L2 + 0.05)
     * Oran 1.0 (kontrast yok) ile 21.0 (maksimum siyah/beyaz kontrastı) arasındadır.
     */
    fun calculateContrastRatio(foreground: Color, background: Color): Float {
        val l1 = calculateLuminance(foreground)
        val l2 = calculateLuminance(background)
        val lighter = max(l1, l2)
        val darker = min(l1, l2)
        return (lighter + 0.05f) / (darker + 0.05f)
    }

    /**
     * Arka plan rengine göre WCAG standartlarına uyarak en yüksek kontrastı veren
     * metin rengini (Siyah veya Beyaz) döndürür.
     */
    fun getContrastingTextColor(backgroundColor: Color): Color {
        val contrastWithWhite = calculateContrastRatio(Color.White, backgroundColor)
        val contrastWithBlack = calculateContrastRatio(Color.Black, backgroundColor)
        return if (contrastWithBlack >= contrastWithWhite) Color.Black else Color.White
    }

    /**
     * Arka planın açık (light) renk olup olmadığını kontrol eder.
     */
    fun isLightBackground(backgroundColor: Color): Boolean {
        return getContrastingTextColor(backgroundColor) == Color.Black
    }

    /**
     * Not kartlarındaki başlık metinleri için yüksek kontrastlı renk.
     */
    fun getPrimaryTitleColor(backgroundColor: Color): Color {
        return getContrastingTextColor(backgroundColor)
    }

    /**
     * Not kartlarındaki içerik gövdesi için yüksek kontrastlı metin rengi.
     */
    fun getBodyContentColor(backgroundColor: Color): Color {
        return if (isLightBackground(backgroundColor)) Color.Black else Color.White
    }

    /**
     * Tarih, kelime sayısı gibi ikincil bilgiler için kontrastlı metin rengi.
     */
    fun getSubtleMetadataColor(backgroundColor: Color): Color {
        return if (isLightBackground(backgroundColor)) Color.Black.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.7f)
    }

    /**
     * Aksiyon simgeleri için kontrastlı renk.
     */
    fun getActionIconTint(backgroundColor: Color): Color {
        return getContrastingTextColor(backgroundColor)
    }
}
