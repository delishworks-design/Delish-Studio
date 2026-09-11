package com.delish.pinster

import android.graphics.*

data class QualityScore(
    val total: Float,
    val productProminence: Float,
    val typographyHierarchy: Float,
    val readability: Float,
    val negativeSpace: Float,
    val visualBalance: Float,
    val colorHarmony: Float,
    val contrast: Float,
    val composition: Float,
    val collision: Float,
    val premiumAesthetic: Float,
    val hookStrength: Float = 70f,
    val visualImpact: Float = 70f,
    val textDensity: Float = 70f,
    val productIsolation: Float = 70f,
    val commercialClarity: Float = 70f,
    val mobileLegibility: Float = 70f,
    val scrollStopPower: Float = 70f,
    val whitespaceQuality: Float = 70f
)

object QualityScorer {

    fun score(bmp: Bitmap, fp: ProductFingerprint, layout: CompositionLayout, palette: PosterPalette, concept: CreativeConcept): QualityScore {
        val w = bmp.width.toFloat()
        val h = bmp.height.toFloat()

        val prodArea = layout.productRect.width() * layout.productRect.height()
        val totalArea = w * h
        val prodRatio = prodArea / totalArea
        val productProminence = when {
            prodRatio in 0.18f..0.38f -> 95f
            prodRatio in 0.12f..0.48f -> 85f
            prodRatio in 0.08f..0.55f -> 70f
            else -> 50f
        }

        val headlineArea = layout.headlineRect.width() * layout.headlineRect.height()
        val headlineRatio = headlineArea / totalArea
        val typographyHierarchy = when {
            headlineRatio > 0.08f -> 92f
            headlineRatio > 0.05f -> 80f
            headlineRatio > 0.03f -> 68f
            else -> 50f
        }

        val margin = 70f
        val textInSafe = layout.headlineRect.left >= margin && layout.headlineRect.right <= w - margin &&
                layout.headlineRect.top >= margin && layout.headlineRect.bottom <= h - margin
        val readability = if (textInSafe) 92f else 60f

        val textArea = (layout.headlineRect.width() * layout.headlineRect.height()) +
                (layout.supportRect.width() * layout.supportRect.height())
        val labelArea = layout.labelRect.width() * layout.labelRect.height()
        val decorArea = layout.decorationRects.sumOf { (it.width() * it.height()).toDouble() }.toFloat()
        val usedRatio = (prodArea + textArea + labelArea + decorArea) / totalArea
        val negSpace = (1f - usedRatio).coerceIn(0f, 1f)
        val negativeSpace = when {
            negSpace in 0.28f..0.58f -> 92f
            negSpace in 0.18f..0.68f -> 78f
            negSpace in 0.10f..0.78f -> 65f
            else -> 48f
        }

        val prodCX = layout.productRect.centerX() / w
        val prodCY = layout.productRect.centerY() / h
        val balanceX = 1f - kotlin.math.abs(prodCX - 0.5f) * 2f
        val balanceY = 1f - kotlin.math.abs(prodCY - 0.48f) * 2f
        val visualBalance = ((balanceX + balanceY) / 2f * 90f).coerceIn(50f, 95f)

        val colorHarmony = if (palette.isDark == fp.isDarkProduct) 88f else 72f

        val textLum = luminance(palette.text)
        val bgLum = luminance(palette.background)
        val contrastRatio = if (textLum > bgLum) (textLum + 0.05f) / (bgLum + 0.05f)
        else (bgLum + 0.05f) / (textLum + 0.05f)
        val contrast = when {
            contrastRatio >= 7f -> 98f
            contrastRatio >= 5.5f -> 92f
            contrastRatio >= 4.5f -> 85f
            contrastRatio >= 3.5f -> 75f
            contrastRatio >= 2.5f -> 65f
            else -> 50f
        }

        val contrastFloorMet = contrastRatio >= 4.5f

        val prodOverlapsText = RectF.intersects(layout.productRect, layout.headlineRect)
        val collision = if (prodOverlapsText) 40f else 94f

        // Hook strength: how well the concept differentiates from raw title
        val hookWords = concept.hook.split(" ").filter { it.length > 2 }
        val titleWords = concept.productLabel.split(" ").filter { it.length > 2 }
        val overlap = hookWords.intersect(titleWords.toSet()).size
        val hookDistinctiveness = if (hookWords.isNotEmpty()) 1f - (overlap.toFloat() / hookWords.size) else 0.5f
        val hookStrength = (hookDistinctiveness * 80f + concept.hook.length.coerceAtMost(30).toFloat() / 30f * 20f).coerceIn(55f, 95f)

        // Visual impact: based on palette contrast and concept mood
        val visualImpact = when (concept.lightingMood) {
            "dramatic" -> 90f
            "directional" -> 85f
            "warm diffuse" -> 75f
            "soft" -> 70f
            "diffuse" -> 72f
            else -> 70f
        }

        // Text density: less text = better for Pinterest
        val supportLen = concept.supportCopy.length
        val textDensity = when {
            supportLen < 40 -> 90f
            supportLen < 70 -> 82f
            supportLen < 100 -> 72f
            else -> 60f
        }

        // Product isolation: product has clear space around it
        val productPadding = minOf(
            layout.productRect.left,
            layout.productRect.top,
            w - layout.productRect.right,
            h - layout.productRect.bottom
        )
        val productIsolation = (productPadding / 70f * 90f).coerceIn(50f, 95f)

        // Commercial clarity: distinct product, label, and hook
        val hasLabel = concept.productLabel.isNotBlank()
        val hasHook = concept.hook.isNotBlank()
        val hasSupport = concept.supportCopy.isNotBlank()
        val commercialClarity = when {
            hasLabel && hasHook && hasSupport -> 90f
            hasLabel && hasHook -> 82f
            hasLabel -> 70f
            else -> 55f
        }

        val premiumAesthetic = (productProminence * 0.20f + typographyHierarchy * 0.12f +
                readability * 0.12f + negativeSpace * 0.12f + visualBalance * 0.08f +
                colorHarmony * 0.06f + contrast * 0.08f + hookStrength * 0.10f +
                visualImpact * 0.06f + commercialClarity * 0.06f)

        val mobileLegibility = readability * 0.4f + contrast * 0.3f + typographyHierarchy * 0.3f
        val scrollStopPower = hookStrength * 0.35f + visualImpact * 0.30f + colorHarmony * 0.20f + contrast * 0.15f
        val whitespaceQuality = negativeSpace * 0.5f + visualBalance * 0.3f + (100f - textDensity) * 0.2f

        val total = (hookStrength * 0.20f + visualBalance * 0.18f + colorHarmony * 0.15f +
                readability * 0.12f + premiumAesthetic * 0.12f + visualImpact * 0.10f +
                productIsolation * 0.08f + whitespaceQuality * 0.05f)

        return QualityScore(total, productProminence, typographyHierarchy, readability,
            negativeSpace, visualBalance, colorHarmony, contrast, composition = 80f,
            collision, premiumAesthetic, hookStrength, visualImpact, textDensity, productIsolation, commercialClarity,
            mobileLegibility, scrollStopPower, whitespaceQuality)
    }

    private fun luminance(color: Int): Float {
        val r = Color.red(color) / 255f
        val g = Color.green(color) / 255f
        val b = Color.blue(color) / 255f
        return 0.2126f * r + 0.7152f * g + 0.0722f * b
    }

    fun score(bmp: Bitmap, fp: ProductFingerprint, layout: SimpleTemplateEngine.TemplateLayout, concept: CreativeConcept): QualityScore {
        val w = bmp.width.toFloat()
        val h = bmp.height.toFloat()

        val prodArea = layout.productRect.width() * layout.productRect.height()
        val totalArea = w * h
        val prodRatio = prodArea / totalArea
        val productProminence = when {
            prodRatio in 0.15f..0.45f -> 95f
            prodRatio in 0.10f..0.55f -> 85f
            prodRatio in 0.06f..0.65f -> 70f
            else -> 50f
        }

        val headlineArea = layout.headlineRect.width() * layout.headlineRect.height()
        val headlineRatio = headlineArea / totalArea
        val typographyHierarchy = when {
            headlineRatio > 0.06f -> 92f
            headlineRatio > 0.04f -> 80f
            headlineRatio > 0.02f -> 68f
            else -> 50f
        }

        val margin = 60f
        val textInSafe = layout.headlineRect.left >= margin && layout.headlineRect.right <= w - margin &&
                layout.headlineRect.top >= margin && layout.headlineRect.bottom <= h - margin
        val readability = if (textInSafe) 92f else 60f

        val textArea = (layout.headlineRect.width() * layout.headlineRect.height()) +
                (layout.supportRect.width() * layout.supportRect.height())
        val usedRatio = (prodArea + textArea) / totalArea
        val negSpace = (1f - usedRatio).coerceIn(0f, 1f)
        val negativeSpace = when {
            negSpace in 0.30f..0.60f -> 92f
            negSpace in 0.20f..0.70f -> 78f
            negSpace in 0.12f..0.78f -> 65f
            else -> 48f
        }

        val prodCX = layout.productRect.centerX() / w
        val prodCY = layout.productRect.centerY() / h
        val balanceX = 1f - kotlin.math.abs(prodCX - 0.5f) * 2f
        val balanceY = 1f - kotlin.math.abs(prodCY - 0.48f) * 2f
        val visualBalance = ((balanceX + balanceY) / 2f * 90f).coerceIn(50f, 95f)

        val colorHarmony = 88f

        val textLum = luminance(layout.textColor)
        val bgLum = luminance(layout.bgColor)
        val contrastRatio = if (textLum > bgLum) (textLum + 0.05f) / (bgLum + 0.05f)
        else (bgLum + 0.05f) / (textLum + 0.05f)
        val contrast = when {
            contrastRatio >= 7f -> 98f
            contrastRatio >= 5.5f -> 92f
            contrastRatio >= 4.5f -> 85f
            contrastRatio >= 3.5f -> 75f
            contrastRatio >= 2.5f -> 65f
            else -> 50f
        }

        val prodOverlapsText = RectF.intersects(layout.productRect, layout.headlineRect)
        val collision = if (prodOverlapsText) 40f else 94f

        val hookWords = concept.hook.split(" ").filter { it.length > 2 }
        val titleWords = concept.productLabel.split(" ").filter { it.length > 2 }
        val overlap = hookWords.intersect(titleWords.toSet()).size
        val hookDistinctiveness = if (hookWords.isNotEmpty()) 1f - (overlap.toFloat() / hookWords.size) else 0.5f
        val hookStrength = (hookDistinctiveness * 80f + concept.hook.length.coerceAtMost(30).toFloat() / 30f * 20f).coerceIn(55f, 95f)

        val visualImpact = 78f

        val supportLen = concept.supportCopy.length
        val textDensity = when {
            supportLen < 40 -> 90f
            supportLen < 70 -> 82f
            supportLen < 100 -> 72f
            else -> 60f
        }

        val productPadding = minOf(
            layout.productRect.left,
            layout.productRect.top,
            w - layout.productRect.right,
            h - layout.productRect.bottom
        )
        val productIsolation = (productPadding / 60f * 90f).coerceIn(50f, 95f)

        val hasLabel = concept.productLabel.isNotBlank()
        val hasHook = concept.hook.isNotBlank()
        val hasSupport = concept.supportCopy.isNotBlank()
        val commercialClarity = when {
            hasLabel && hasHook && hasSupport -> 90f
            hasLabel && hasHook -> 82f
            hasLabel -> 70f
            else -> 55f
        }

        val premiumAesthetic = (productProminence * 0.25f + typographyHierarchy * 0.15f +
                readability * 0.15f + negativeSpace * 0.10f + visualBalance * 0.10f +
                contrast * 0.10f + hookStrength * 0.10f + commercialClarity * 0.05f)

        val mobileLegibility = readability * 0.4f + contrast * 0.3f + typographyHierarchy * 0.3f
        val scrollStopPower = hookStrength * 0.35f + visualImpact * 0.30f + colorHarmony * 0.20f + contrast * 0.15f
        val whitespaceQuality = negativeSpace * 0.5f + visualBalance * 0.3f + (100f - textDensity) * 0.2f

        val total = (productProminence * 0.25f + readability * 0.20f + visualBalance * 0.15f +
                contrast * 0.15f + hookStrength * 0.10f + whitespaceQuality * 0.10f +
                commercialClarity * 0.05f)

        return QualityScore(total, productProminence, typographyHierarchy, readability,
            negativeSpace, visualBalance, colorHarmony, contrast, composition = 85f,
            collision, premiumAesthetic, hookStrength, visualImpact, textDensity, productIsolation, commercialClarity,
            mobileLegibility, scrollStopPower, whitespaceQuality)
    }
}
