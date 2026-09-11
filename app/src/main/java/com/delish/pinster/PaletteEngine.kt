package com.delish.pinster

import android.graphics.Color

data class PosterPalette(
    val background: Int,
    val surface: Int,
    val primary: Int,
    val secondary: Int,
    val accent: Int,
    val text: Int,
    val textMuted: Int,
    val textOnDark: Int,
    val shadow: Int,
    val highlight: Int,
    val vignette: Int,
    val isDark: Boolean,
    val atmosphere: Int = Color.TRANSPARENT,
    val depthLayer: Int = Color.TRANSPARENT,
    val textBackground: Int = Color.argb(160, 0, 0, 0),
    val textBackgroundLight: Int = Color.argb(160, 255, 255, 255)
)

object PaletteEngine {

    fun generate(fp: ProductFingerprint, direction: ArtDirection, concept: CreativeConcept): PosterPalette {
        val dominant = fp.dominantColors.firstOrNull() ?: Color.rgb(180, 170, 160)
        val dr = Color.red(dominant); val dg = Color.green(dominant); val db = Color.blue(dominant)

        return when (direction) {
            ArtDirection.LUXURY_EDITORIAL -> editorialPalette(fp, dr, dg, db, concept)
            ArtDirection.MODERN_MINIMAL -> minimalPalette(fp, dr, dg, db, concept)
            ArtDirection.ORGANIC_LIFESTYLE -> lifestylePalette(fp, dr, dg, db, concept)
            ArtDirection.BOLD_CAMPAIGN -> boldPalette(fp, dr, dg, db, concept)
            ArtDirection.SOFT_PINTEREST -> softPalette(fp, dr, dg, db, concept)
        }
    }

    private fun editorialPalette(fp: ProductFingerprint, r: Int, g: Int, b: Int, concept: CreativeConcept): PosterPalette {
        val bg = if (fp.isDarkProduct) Color.rgb(24, 22, 20) else Color.rgb(18, 16, 14)
        val surface = blendColor(bg, Color.rgb(38, 35, 30), 0.3f)
        val primary = Color.rgb(245, 238, 225)
        val secondary = Color.rgb(195, 180, 155)
        val accent = warmAccent(r, g, b)
        val highlight = if (fp.isWarmTone) Color.argb(18, 230, 210, 170) else Color.argb(12, 200, 195, 210)
        val atmosphere = blendColor(bg, Color.rgb(35, 30, 22), 0.15f)
        return PosterPalette(bg, surface, primary, secondary, accent,
            Color.rgb(245, 238, 225), Color.rgb(165, 155, 140), Color.WHITE,
            Color.argb(45, 0, 0, 0), highlight,
            Color.argb(30, 0, 0, 0), isDark = true,
            atmosphere = atmosphere,
            textBackground = Color.argb(180, 18, 16, 14),
            textBackgroundLight = Color.argb(180, 245, 238, 225))
    }

    private fun minimalPalette(fp: ProductFingerprint, r: Int, g: Int, b: Int, concept: CreativeConcept): PosterPalette {
        val bg = if (fp.isDarkProduct) Color.rgb(245, 244, 240) else Color.rgb(252, 251, 248)
        val surface = Color.WHITE
        val primary = Color.rgb(28, 28, 28)
        val secondary = Color.rgb(100, 100, 100)
        val accent = desaturatedAccent(r, g, b)
        val highlight = Color.argb(8, 0, 0, 0)
        val atmosphere = blendColor(bg, Color.rgb(240, 238, 232), 0.2f)
        return PosterPalette(bg, surface, primary, secondary, accent,
            Color.rgb(28, 28, 28), Color.rgb(130, 130, 130), Color.WHITE,
            Color.argb(18, 0, 0, 0), highlight,
            Color.argb(12, 0, 0, 0), isDark = false,
            atmosphere = atmosphere,
            textBackground = Color.argb(180, 252, 251, 248),
            textBackgroundLight = Color.argb(180, 28, 28, 28))
    }

    private fun lifestylePalette(fp: ProductFingerprint, r: Int, g: Int, b: Int, concept: CreativeConcept): PosterPalette {
        val bg = Color.rgb(245, 238, 228)
        val surface = Color.WHITE
        val primary = Color.rgb(55, 48, 38)
        val secondary = Color.rgb(115, 100, 82)
        val accent = earthAccent(r, g, b)
        val warmHl = if (fp.isWarmTone) Color.argb(12, 225, 205, 170) else Color.argb(8, 200, 195, 180)
        val atmosphere = blendColor(bg, Color.rgb(238, 228, 212), 0.2f)
        return PosterPalette(bg, surface, primary, secondary, accent,
            Color.rgb(55, 48, 38), Color.rgb(140, 125, 105), Color.WHITE,
            Color.argb(15, 80, 60, 40), warmHl,
            Color.argb(18, 60, 40, 20), isDark = false,
            atmosphere = atmosphere,
            textBackground = Color.argb(180, 245, 238, 228),
            textBackgroundLight = Color.argb(180, 55, 48, 38))
    }

    private fun boldPalette(fp: ProductFingerprint, r: Int, g: Int, b: Int, concept: CreativeConcept): PosterPalette {
        val bg = Color.rgb(14, 14, 14)
        val surface = Color.rgb(246, 245, 243)
        val primary = Color.WHITE
        val secondary = Color.rgb(50, 50, 50)
        val accent = controlledAccent(r, g, b)
        val highlight = Color.argb(15, Color.red(accent), Color.green(accent), Color.blue(accent))
        val atmosphere = Color.argb(6, 0, 0, 0)
        return PosterPalette(bg, surface, primary, secondary, accent,
            Color.WHITE, Color.rgb(200, 200, 200), Color.WHITE,
            Color.argb(35, 0, 0, 0), highlight,
            Color.argb(35, 0, 0, 0), isDark = true,
            atmosphere = atmosphere,
            textBackground = Color.argb(180, 14, 14, 14),
            textBackgroundLight = Color.argb(180, 255, 255, 255))
    }

    private fun softPalette(fp: ProductFingerprint, r: Int, g: Int, b: Int, concept: CreativeConcept): PosterPalette {
        val bg1 = Color.rgb(235, 230, 242)
        val bg2 = Color.rgb(248, 243, 234)
        val bg = blendColor(bg1, bg2, 0.5f)
        val surface = Color.WHITE
        val primary = Color.rgb(50, 42, 62)
        val secondary = Color.rgb(105, 95, 118)
        val accent = softAccent(r, g, b)
        val highlight = Color.argb(10, 200, 180, 220)
        val atmosphere = blendColor(bg, Color.rgb(240, 235, 248), 0.2f)
        return PosterPalette(bg, surface, primary, secondary, accent,
            Color.rgb(50, 42, 62), Color.rgb(150, 140, 162), Color.WHITE,
            Color.argb(15, 0, 0, 0), highlight,
            Color.argb(12, 0, 0, 0), isDark = false,
            atmosphere = atmosphere,
            textBackground = Color.argb(180, 240, 235, 245),
            textBackgroundLight = Color.argb(180, 50, 42, 62))
    }

    private fun warmAccent(r: Int, g: Int, b: Int): Int {
        val h = FloatArray(3)
        Color.colorToHSV(Color.rgb(r, g, b), h)
        h[0] = 30f; h[1] = 0.3f; h[2] = 0.7f
        return Color.HSVToColor(180, h)
    }

    private fun desaturatedAccent(r: Int, g: Int, b: Int): Int {
        val gray = (r * 0.3 + g * 0.5 + b * 0.2).toInt()
        return Color.rgb((gray * 0.8 + r * 0.2).toInt().coerceIn(0, 255),
            (gray * 0.8 + g * 0.2).toInt().coerceIn(0, 255),
            (gray * 0.8 + b * 0.2).toInt().coerceIn(0, 255))
    }

    private fun earthAccent(r: Int, g: Int, b: Int): Int {
        val h = FloatArray(3)
        Color.colorToHSV(Color.rgb(r, g, b), h)
        h[0] = 35f; h[1] = (h[1] * 0.4f).coerceIn(0f, 0.5f); h[2] = 0.65f
        return Color.HSVToColor(160, h)
    }

    private fun controlledAccent(r: Int, g: Int, b: Int): Int {
        val h = FloatArray(3)
        Color.colorToHSV(Color.rgb(r, g, b), h)
        if (h[0] in 0f..40f || h[0] >= 320f) {
            h[0] = 2f; h[1] = 0.7f; h[2] = 0.78f
        } else {
            h[0] = 210f; h[1] = 0.5f; h[2] = 0.65f
        }
        return Color.HSVToColor(220, h)
    }

    private fun softAccent(r: Int, g: Int, b: Int): Int {
        val h = FloatArray(3)
        Color.colorToHSV(Color.rgb(r, g, b), h)
        h[0] = 270f; h[1] = 0.12f; h[2] = 0.82f
        return Color.HSVToColor(100, h)
    }

    private fun blendColor(c1: Int, c2: Int, ratio: Float): Int {
        val r = (Color.red(c1) * (1 - ratio) + Color.red(c2) * ratio).toInt()
        val g = (Color.green(c1) * (1 - ratio) + Color.green(c2) * ratio).toInt()
        val b = (Color.blue(c1) * (1 - ratio) + Color.blue(c2) * ratio).toInt()
        return Color.rgb(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
    }

    fun contrastRatio(fg: Int, bg: Int): Double {
        val l1 = relativeLuminance(fg)
        val l2 = relativeLuminance(bg)
        val lighter = maxOf(l1, l2)
        val darker = minOf(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun relativeLuminance(color: Int): Double {
        val r = Color.red(color) / 255.0
        val g = Color.green(color) / 255.0
        val b = Color.blue(color) / 255.0
        val rL = if (r <= 0.03928) r / 12.92 else Math.pow((r + 0.055) / 1.055, 2.4)
        val gL = if (g <= 0.03928) g / 12.92 else Math.pow((g + 0.055) / 1.055, 2.4)
        val bL = if (b <= 0.03928) b / 12.92 else Math.pow((b + 0.055) / 1.055, 2.4)
        return 0.2126 * rL + 0.7152 * gL + 0.0722 * bL
    }

    fun bestTextColor(fg: Int, bg: Int): Int {
        val whiteRatio = contrastRatio(Color.WHITE, bg)
        val blackRatio = contrastRatio(Color.BLACK, bg)
        return if (whiteRatio > blackRatio) fg else fg
    }
}
