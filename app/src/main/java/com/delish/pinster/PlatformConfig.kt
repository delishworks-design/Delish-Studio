package com.delish.pinster

import android.graphics.Color

object ColorHarmonyEngine {

    data class HarmonyPalette(
        val background: Int,
        val surface: Int,
        val accent: Int,
        val text: Int,
        val textMuted: Int,
        val secondary: Int,
        val highlight: Int
    )

    fun generate(productHue: Float, productSat: Float, direction: ArtDirection): HarmonyPalette {
        val base = productHue

        val hues = when (direction) {
            ArtDirection.LUXURY_EDITORIAL -> floatArrayOf(base, base + 30f, base + 60f)
            ArtDirection.MODERN_MINIMAL -> floatArrayOf(base, base + 180f)
            ArtDirection.ORGANIC_LIFESTYLE -> floatArrayOf(base, base + 30f, base - 30f)
            ArtDirection.BOLD_CAMPAIGN -> floatArrayOf(base, base + 120f, base + 240f)
            ArtDirection.SOFT_PINTEREST -> floatArrayOf(base, base + 15f, base + 45f)
        }

        val bgHue = hues[0]
        val accentHue = hues.getOrElse(1) { hues[0] }
        val secondaryHue = hues.getOrElse(2) { hues[0] }

        val bgLightness = when (direction) {
            ArtDirection.LUXURY_EDITORIAL -> 0.12f
            ArtDirection.MODERN_MINIMAL -> 0.92f
            ArtDirection.ORGANIC_LIFESTYLE -> 0.88f
            ArtDirection.BOLD_CAMPAIGN -> 0.15f
            ArtDirection.SOFT_PINTEREST -> 0.94f
        }

        val bgSat = when (direction) {
            ArtDirection.LUXURY_EDITORIAL -> 0.15f
            ArtDirection.MODERN_MINIMAL -> 0.05f
            ArtDirection.ORGANIC_LIFESTYLE -> 0.20f
            ArtDirection.BOLD_CAMPAIGN -> 0.30f
            ArtDirection.SOFT_PINTEREST -> 0.10f
        }

        val bg = hslToColor(bgHue, bgSat, bgLightness)
        val surface = hslToColor(bgHue, bgSat * 0.8f, (bgLightness + 0.05f).coerceIn(0f, 1f))
        val accent = hslToColor(accentHue, 0.65f, 0.55f)
        val secondary = hslToColor(secondaryHue, 0.40f, 0.45f)
        val text = if (bgLightness > 0.5f) Color.rgb(30, 30, 30) else Color.rgb(245, 245, 245)
        val textMuted = if (bgLightness > 0.5f) Color.rgb(110, 110, 110) else Color.rgb(170, 170, 170)
        val highlight = hslToColor(accentHue, 0.50f, 0.70f)

        return HarmonyPalette(bg, surface, accent, text, textMuted, secondary, highlight)
    }

    fun categoryPsychology(category: ProductCategory): Pair<Float, Float> {
        return when (category) {
            ProductCategory.FOOD -> Pair(15f, 0.70f)
            ProductCategory.KITCHEN -> Pair(30f, 0.40f)
            ProductCategory.BEDROOM -> Pair(220f, 0.25f)
            ProductCategory.BATHROOM -> Pair(195f, 0.30f)
            ProductCategory.DECOR -> Pair(45f, 0.50f)
            ProductCategory.FASHION -> Pair(340f, 0.55f)
            ProductCategory.ELECTRONICS -> Pair(210f, 0.35f)
            ProductCategory.BEAUTY -> Pair(330f, 0.45f)
            ProductCategory.OUTDOOR -> Pair(120f, 0.40f)
            ProductCategory.OFFICE -> Pair(215f, 0.30f)
            ProductCategory.STORAGE -> Pair(40f, 0.20f)
            ProductCategory.UNKNOWN -> Pair(0f, 0.30f)
        }
    }

    private fun hslToColor(h: Float, s: Float, l: Float): Int {
        val hue = ((h % 360) + 360) % 360
        val c = (1f - kotlin.math.abs(2f * l - 1f)) * s
        val x = c * (1f - kotlin.math.abs((hue / 60f) % 2 - 1f))
        val m = l - c / 2f
        val (r, g, b) = when {
            hue < 60 -> Triple(c, x, 0f)
            hue < 120 -> Triple(x, c, 0f)
            hue < 180 -> Triple(0f, c, x)
            hue < 240 -> Triple(0f, x, c)
            hue < 300 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        return Color.rgb(
            ((r + m) * 255).toInt().coerceIn(0, 255),
            ((g + m) * 255).toInt().coerceIn(0, 255),
            ((b + m) * 255).toInt().coerceIn(0, 255)
        )
    }
}

object WhitespaceBudget {

    enum class Tier(val minRatio: Float, val maxRatio: Float, val target: Float) {
        PREMIUM(0.35f, 0.42f, 0.38f),
        BALANCED(0.28f, 0.34f, 0.31f),
        DENSE(0.20f, 0.27f, 0.24f)
    }

    fun tierFor(direction: ArtDirection): Tier {
        return when (direction) {
            ArtDirection.LUXURY_EDITORIAL -> Tier.PREMIUM
            ArtDirection.MODERN_MINIMAL -> Tier.PREMIUM
            ArtDirection.ORGANIC_LIFESTYLE -> Tier.BALANCED
            ArtDirection.BOLD_CAMPAIGN -> Tier.DENSE
            ArtDirection.SOFT_PINTEREST -> Tier.BALANCED
        }
    }

    fun textureMaxFor(direction: ArtDirection): Float {
        return when (direction) {
            ArtDirection.LUXURY_EDITORIAL -> 0.15f
            ArtDirection.MODERN_MINIMAL -> 0.10f
            ArtDirection.ORGANIC_LIFESTYLE -> 0.20f
            ArtDirection.BOLD_CAMPAIGN -> 0.25f
            ArtDirection.SOFT_PINTEREST -> 0.12f
        }
    }

    fun grainMaxFor(direction: ArtDirection): Float {
        return when (direction) {
            ArtDirection.LUXURY_EDITORIAL -> 0.05f
            ArtDirection.MODERN_MINIMAL -> 0.08f
            ArtDirection.ORGANIC_LIFESTYLE -> 0.10f
            ArtDirection.BOLD_CAMPAIGN -> 0.12f
            ArtDirection.SOFT_PINTEREST -> 0.06f
        }
    }

    fun gradientStopsMax(direction: ArtDirection): Int {
        return when (direction) {
            ArtDirection.LUXURY_EDITORIAL -> 3
            ArtDirection.MODERN_MINIMAL -> 2
            ArtDirection.ORGANIC_LIFESTYLE -> 3
            ArtDirection.BOLD_CAMPAIGN -> 4
            ArtDirection.SOFT_PINTEREST -> 2
        }
    }

    fun validate(canvasW: Int, canvasH: Int, contentPixels: Int, tier: Tier): Float {
        val total = canvasW * canvasH
        val whitespace = 1f - (contentPixels.toFloat() / total)
        val target = tier.target
        val deviation = kotlin.math.abs(whitespace - target)
        val maxDev = (tier.maxRatio - tier.minRatio) / 2f
        return (1f - deviation / maxDev).coerceIn(0f, 1f)
    }
}

object ShadowPersonalities {

    data class ShadowConfig(
        val ambientBlur: Float,
        val ambientAlpha: Int,
        val contactOffset: Float,
        val contactBlur: Float,
        val contactAlpha: Int,
        val color: Int,
        val offsetX: Float = 6f,
        val offsetY: Float = 10f,
        val layers: Int = 2,
        val opacity: Float = 0.30f
    )

    val configs = mapOf(
        ArtDirection.LUXURY_EDITORIAL to ShadowConfig(28f, 30, 6f, 3f, 50, Color.rgb(26, 26, 26), 6f, 10f, 2, 0.30f),
        ArtDirection.MODERN_MINIMAL to ShadowConfig(16f, 20, 4f, 2f, 40, Color.rgb(51, 51, 51), 0f, 4f, 1, 0.18f),
        ArtDirection.ORGANIC_LIFESTYLE to ShadowConfig(36f, 25, 5f, 3f, 45, Color.rgb(42, 31, 26), 8f, 14f, 3, 0.22f),
        ArtDirection.BOLD_CAMPAIGN to ShadowConfig(12f, 40, 10f, 6f, 60, Color.rgb(31, 15, 10), 12f, 16f, 2, 0.45f),
        ArtDirection.SOFT_PINTEREST to ShadowConfig(44f, 25, 8f, 8f, 35, Color.rgb(61, 43, 31), 4f, 6f, 2, 0.12f)
    )
}

object TextureIntelligence {

    enum class SurfaceType { MATTE, GLOSSY, ROUGH, METALLIC }

    fun detectSurface(fp: ProductFingerprint): SurfaceType {
        val edgeDensity = fp.edgeDensity
        val brightness = fp.averageBrightness
        val saturation = fp.averageSaturation

        return when {
            brightness > 0.7f && saturation < 0.2f -> SurfaceType.GLOSSY
            edgeDensity > 0.15f && saturation > 0.3f -> SurfaceType.ROUGH
            brightness > 0.6f && saturation < 0.15f -> SurfaceType.METALLIC
            else -> SurfaceType.MATTE
        }
    }

    fun grainIntensity(surface: SurfaceType): Int {
        return when (surface) {
            SurfaceType.MATTE -> 3
            SurfaceType.GLOSSY -> 1
            SurfaceType.ROUGH -> 5
            SurfaceType.METALLIC -> 1
        }
    }
}
