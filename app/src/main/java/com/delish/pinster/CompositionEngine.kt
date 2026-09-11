package com.delish.pinster

import android.graphics.RectF
import kotlin.math.abs

enum class ArtDirection {
    LUXURY_EDITORIAL, MODERN_MINIMAL, ORGANIC_LIFESTYLE, BOLD_CAMPAIGN, SOFT_PINTEREST
}

enum class CompositionMode {
    HERO, OFFSET, FLOAT, FRAME, ASYMMETRIC, SPLIT, CARD,
    GOLDEN_SECTION, DIAGONAL, INTIMATE, ENVIRONMENTAL
}

data class CompositionLayout(
    val mode: CompositionMode,
    val direction: ArtDirection,
    val productRect: RectF,
    val headlineRect: RectF,
    val supportRect: RectF,
    val labelRect: RectF,
    val decorationRects: List<RectF>,
    val splitRatio: Float,
    val hasCard: Boolean,
    val cardRect: RectF?,
    val eyebrowText: String,
    val headlineAlign: TextAlign,
    val supportAlign: TextAlign,
    val productScale: Float,
    val productPlacement: ProductPlacement,
    val leadingLines: List<LeadingLine>,
    val goldenPoints: List<Pair<Float, Float>> = emptyList(),
    val visualWeightBalance: Float = 0.5f
)

data class LeadingLine(
    val x1: Float, val y1: Float,
    val x2: Float, val y2: Float,
    val type: LineType
)

enum class LineType { RULER, ACCENT, GUIDE, SEPARATOR, DIAGONAL, CURVE }

enum class TextAlign { LEFT, CENTER, RIGHT }

object CompositionEngine {

    private const val W = 1000f
    private const val H = 1500f
    private const val M = 70f
    private const val GOLDEN_RATIO = 1.618f

    private val goldenThirdX = W / GOLDEN_RATIO
    private val goldenThirdY = H / GOLDEN_RATIO
    private val goldenTwoThirdsX = W - goldenThirdX
    private val goldenTwoThirdsY = H - goldenThirdY

    val goldenPoints = listOf(
        Pair(goldenThirdX, goldenThirdY),
        Pair(goldenTwoThirdsX, goldenThirdY),
        Pair(goldenThirdX, goldenTwoThirdsY),
        Pair(goldenTwoThirdsX, goldenTwoThirdsY),
        Pair(W * 0.5f, goldenThirdY),
        Pair(W * 0.5f, goldenTwoThirdsY),
        Pair(goldenThirdX, H * 0.5f),
        Pair(goldenTwoThirdsX, H * 0.5f)
    )

    fun resolve(fp: ProductFingerprint, direction: ArtDirection, variation: Int, concept: CreativeConcept): CompositionLayout {
        return when (direction) {
            ArtDirection.LUXURY_EDITORIAL -> editorialLayout(fp, variation, concept)
            ArtDirection.MODERN_MINIMAL -> minimalLayout(fp, variation, concept)
            ArtDirection.ORGANIC_LIFESTYLE -> lifestyleLayout(fp, variation, concept)
            ArtDirection.BOLD_CAMPAIGN -> boldLayout(fp, variation, concept)
            ArtDirection.SOFT_PINTEREST -> softLayout(fp, variation, concept)
        }
    }

    private fun computeProductRect(
        fp: ProductFingerprint,
        concept: CreativeConcept,
        widthFraction: Float,
        minH: Float,
        maxH: Float,
        placementY: Float,
        offsetX: Float = 0f
    ): RectF {
        val productW = W * widthFraction * concept.productScale
        val productH = (productW / fp.aspectRatio.coerceIn(0.4f, 2.5f)).coerceIn(minH, maxH)
        val left = (W - productW) / 2f + offsetX
        val top = placementY
        return RectF(left, top, left + productW, top + productH)
    }

    private fun editorialLayout(fp: ProductFingerprint, v: Int, concept: CreativeConcept): CompositionLayout {
        val productRect = when (concept.productPlacement) {
            ProductPlacement.LOWER_CENTER -> computeProductRect(fp, concept, 0.72f, 400f, 780f, goldenTwoThirdsY - 120f)
            ProductPlacement.CENTER -> computeProductRect(fp, concept, 0.72f, 400f, 780f, goldenThirdY - 30f)
            else -> computeProductRect(fp, concept, 0.72f, 400f, 780f, goldenThirdY)
        }

        val headlineRect = RectF(M + 60, goldenThirdY * 0.4f, W - M - 60, goldenThirdY)
        val supportRect = RectF(M + 80, productRect.bottom + 50f, W - M - 80, productRect.bottom + 190f)
        val labelRect = RectF(M + 80, supportRect.bottom + 15f, W - M - 80, supportRect.bottom + 60f)

        val leadingLines = listOf(
            LeadingLine(M + 60f, goldenThirdY * 0.35f, W - M - 60f, goldenThirdY * 0.35f, LineType.RULER),
            LeadingLine(M + 60f, H - 90f, W - M - 60f, H - 90f, LineType.RULER),
            LeadingLine(goldenThirdX, M, goldenThirdX, H - M, LineType.GUIDE),
            LeadingLine(goldenTwoThirdsX, M, goldenTwoThirdsX, H - M, LineType.GUIDE)
        )

        return CompositionLayout(
            mode = CompositionMode.GOLDEN_SECTION, direction = ArtDirection.LUXURY_EDITORIAL,
            productRect = productRect, headlineRect = headlineRect,
            supportRect = supportRect, labelRect = labelRect,
            decorationRects = emptyList(),
            splitRatio = 0f, hasCard = false, cardRect = null,
            eyebrowText = concept.eyebrowText, headlineAlign = concept.headlineAlignment,
            supportAlign = concept.supportAlignment, productScale = concept.productScale,
            productPlacement = concept.productPlacement, leadingLines = leadingLines,
            goldenPoints = goldenPoints, visualWeightBalance = 0.5f
        )
    }

    private fun minimalLayout(fp: ProductFingerprint, v: Int, concept: CreativeConcept): CompositionLayout {
        val productRect = when (concept.productPlacement) {
            ProductPlacement.LOWER_CENTER -> computeProductRect(fp, concept, 0.78f, 420f, 820f, goldenTwoThirdsY + 30f)
            ProductPlacement.LOWER_THIRD -> computeProductRect(fp, concept, 0.78f, 420f, 820f, goldenTwoThirdsY + 80f)
            else -> computeProductRect(fp, concept, 0.78f, 420f, 820f, goldenTwoThirdsY - 20f)
        }

        val headlineRect = RectF(M + 20, goldenThirdY * 0.5f, W * 0.72f, goldenThirdY + 50f)
        val supportRect = RectF(M + 20, productRect.bottom + 45f, W * 0.65f, productRect.bottom + 165f)
        val labelRect = RectF(M + 20, supportRect.bottom + 12f, W * 0.65f, supportRect.bottom + 52f)

        val accentX = M + 20f
        val leadingLines = listOf(
            LeadingLine(accentX, goldenThirdY * 0.45f, accentX + 180f, goldenThirdY * 0.45f, LineType.ACCENT),
            LeadingLine(accentX, H - 80f, accentX + 130f, H - 80f, LineType.ACCENT),
            LeadingLine(accentX, goldenThirdY * 0.45f, accentX, goldenThirdY + 50f, LineType.SEPARATOR)
        )

        return CompositionLayout(
            mode = CompositionMode.FLOAT, direction = ArtDirection.MODERN_MINIMAL,
            productRect = productRect, headlineRect = headlineRect,
            supportRect = supportRect, labelRect = labelRect,
            decorationRects = emptyList(),
            splitRatio = 0f, hasCard = false, cardRect = null,
            eyebrowText = concept.eyebrowText, headlineAlign = concept.headlineAlignment,
            supportAlign = concept.supportAlignment, productScale = concept.productScale,
            productPlacement = concept.productPlacement, leadingLines = leadingLines,
            goldenPoints = goldenPoints.subList(0, 4), visualWeightBalance = 0.6f
        )
    }

    private fun lifestyleLayout(fp: ProductFingerprint, v: Int, concept: CreativeConcept): CompositionLayout {
        val productRect = when (concept.productPlacement) {
            ProductPlacement.RIGHT_HERO -> computeProductRect(fp, concept, 0.56f, 300f, 620f, goldenThirdY, W * 0.12f)
            ProductPlacement.LEFT_HERO -> computeProductRect(fp, concept, 0.66f, 380f, 740f, goldenThirdY, -W * 0.12f)
            else -> computeProductRect(fp, concept, 0.66f, 380f, 740f, goldenThirdY + 20f, W * 0.08f)
        }

        val headlineRect = RectF(M + 20, goldenThirdY * 0.6f, W * 0.58f, goldenThirdY + 80f)
        val supportRect = RectF(M + 20, H * 0.76f, W * 0.52f, H * 0.86f)
        val labelRect = RectF(M + 20, H * 0.87f, W * 0.52f, H * 0.92f)

        val leadingLines = listOf(
            LeadingLine(M + 20f, H * 0.74f, W * 0.42f, H * 0.74f, LineType.SEPARATOR),
            LeadingLine(M + 20f, goldenThirdY * 0.55f, M + 20f, H * 0.74f, LineType.SEPARATOR)
        )

        return CompositionLayout(
            mode = CompositionMode.DIAGONAL, direction = ArtDirection.ORGANIC_LIFESTYLE,
            productRect = productRect, headlineRect = headlineRect,
            supportRect = supportRect, labelRect = labelRect,
            decorationRects = emptyList(),
            splitRatio = 0f, hasCard = false, cardRect = null,
            eyebrowText = concept.eyebrowText, headlineAlign = concept.headlineAlignment,
            supportAlign = concept.supportAlignment, productScale = concept.productScale,
            productPlacement = concept.productPlacement, leadingLines = leadingLines,
            goldenPoints = goldenPoints.subList(0, 2), visualWeightBalance = 0.45f
        )
    }

    private fun boldLayout(fp: ProductFingerprint, v: Int, concept: CreativeConcept): CompositionLayout {
        val productRect = when (concept.productPlacement) {
            ProductPlacement.RIGHT_HERO -> computeProductRect(fp, concept, 0.68f, 420f, 820f, goldenThirdY * 0.8f, W * 0.14f)
            ProductPlacement.CROPPED_EDITORIAL -> computeProductRect(fp, concept, 0.82f, 500f, 880f, goldenThirdY * 0.7f, W * 0.08f)
            else -> computeProductRect(fp, concept, 0.68f, 420f, 820f, goldenThirdY * 0.85f, W * 0.10f)
        }

        val headlineRect = RectF(M + 20, goldenThirdY * 0.3f, W * 0.58f, goldenThirdY * 0.9f)
        val supportRect = RectF(M + 20, goldenTwoThirdsY * 0.95f, W - M - 20, goldenTwoThirdsY * 1.15f)
        val labelRect = RectF(M + 20, supportRect.bottom + 10f, W - M - 20, supportRect.bottom + 50f)

        val leadingLines = listOf(
            LeadingLine(M + 20f, goldenThirdY * 0.25f, W * 0.45f, goldenThirdY * 0.25f, LineType.RULER),
            LeadingLine(M + 20f, goldenTwoThirdsY * 0.93f, W - M - 20f, goldenTwoThirdsY * 0.93f, LineType.SEPARATOR),
            LeadingLine(M + 20f, goldenThirdY * 0.3f, M + 20f, goldenThirdY * 0.9f, LineType.DIAGONAL)
        )

        return CompositionLayout(
            mode = CompositionMode.DIAGONAL, direction = ArtDirection.BOLD_CAMPAIGN,
            productRect = productRect, headlineRect = headlineRect,
            supportRect = supportRect, labelRect = labelRect,
            decorationRects = emptyList(),
            splitRatio = 0.48f, hasCard = false, cardRect = null,
            eyebrowText = concept.eyebrowText, headlineAlign = concept.headlineAlignment,
            supportAlign = concept.supportAlignment, productScale = concept.productScale,
            productPlacement = concept.productPlacement, leadingLines = leadingLines,
            goldenPoints = goldenPoints, visualWeightBalance = 0.55f
        )
    }

    private fun softLayout(fp: ProductFingerprint, v: Int, concept: CreativeConcept): CompositionLayout {
        val cardPad = 55f
        val cardRect = RectF(cardPad, cardPad, W - cardPad, H - cardPad)

        val productRect = when (concept.productPlacement) {
            ProductPlacement.LOWER_CENTER -> computeProductRect(fp, concept, 0.65f, 350f, 700f, goldenTwoThirdsY - 30f)
            ProductPlacement.CENTER -> computeProductRect(fp, concept, 0.65f, 350f, 700f, goldenThirdY + 80f)
            else -> computeProductRect(fp, concept, 0.65f, 350f, 700f, goldenThirdY + 100f)
        }

        val headlineRect = RectF(cardPad + 55, goldenThirdY * 0.4f, W - cardPad - 55, goldenThirdY)
        val supportRect = RectF(cardPad + 55, productRect.bottom + 40f, W - cardPad - 55, productRect.bottom + 165f)
        val labelRect = RectF(cardPad + 55, supportRect.bottom + 12f, W - cardPad - 55, supportRect.bottom + 52f)

        val sep = productRect.bottom + 28f
        val leadingLines = listOf(
            LeadingLine(W * 0.22f, sep, W * 0.78f, sep, LineType.SEPARATOR),
            LeadingLine(goldenThirdX, cardPad + 30f, goldenThirdX, H - cardPad - 30f, LineType.GUIDE),
            LeadingLine(goldenTwoThirdsX, cardPad + 30f, goldenTwoThirdsX, H - cardPad - 30f, LineType.GUIDE)
        )

        val innerDecors = listOf(
            RectF(cardPad + 30f, cardPad + 30f, W - cardPad - 30f, H - cardPad - 30f)
        )

        return CompositionLayout(
            mode = CompositionMode.CARD, direction = ArtDirection.SOFT_PINTEREST,
            productRect = productRect, headlineRect = headlineRect,
            supportRect = supportRect, labelRect = labelRect,
            decorationRects = innerDecors,
            splitRatio = 0f, hasCard = true, cardRect = cardRect,
            eyebrowText = concept.eyebrowText, headlineAlign = concept.headlineAlignment,
            supportAlign = concept.supportAlignment, productScale = concept.productScale,
            productPlacement = concept.productPlacement, leadingLines = leadingLines,
            goldenPoints = goldenPoints.subList(2, 4), visualWeightBalance = 0.5f
        )
    }
}
