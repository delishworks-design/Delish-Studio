package com.delish.pinster

import android.graphics.*

object ProductCompositor {

    data class PlacementResult(
        val productRect: RectF,
        val headlineRect: RectF,
        val supportRect: RectF,
        val labelRect: RectF,
        val textSafeRegion: RectF,
        val productScale: Float
    )

    private const val W = 1000f
    private const val H = 1500f
    private const val MARGIN = 70f

    fun calculatePlacement(
        product: Bitmap,
        backgroundMeta: BackgroundMetadata,
        concept: CreativeConcept,
        direction: ArtDirection
    ): PlacementResult {
        val productAspect = product.width.toFloat() / product.height
        val productRect = calculateProductRect(productAspect, concept, backgroundMeta)
        val textRects = calculateTextRects(productRect, concept, backgroundMeta)
        val textSafeRegion = calculateTextSafeRegion(backgroundMeta)

        return PlacementResult(
            productRect = productRect,
            headlineRect = textRects.headline,
            supportRect = textRects.support,
            labelRect = textRects.label,
            textSafeRegion = textSafeRegion,
            productScale = concept.productScale
        )
    }

    private fun calculateProductRect(
        aspectRatio: Float,
        concept: CreativeConcept,
        meta: BackgroundMetadata
    ): RectF {
        val baseWidth = W * 0.45f * concept.productScale
        val baseHeight = baseWidth / aspectRatio

        val maxH = H * 0.55f
        val finalH = baseHeight.coerceAtMost(maxH)
        val finalW = (finalH * aspectRatio).coerceAtMost(W * 0.85f)

        val (centerX, centerY) = when (concept.productPlacement) {
            ProductPlacement.CENTER -> Pair(W * 0.5f, H * 0.52f)
            ProductPlacement.LOWER_CENTER -> Pair(W * 0.5f, H * 0.60f)
            ProductPlacement.LOWER_THIRD -> Pair(W * 0.5f, H * 0.65f)
            ProductPlacement.LEFT_HERO -> Pair(W * 0.35f, H * 0.50f)
            ProductPlacement.RIGHT_HERO -> Pair(W * 0.65f, H * 0.50f)
            ProductPlacement.OFFSET -> Pair(W * 0.45f, H * 0.55f)
            ProductPlacement.CROPPED_EDITORIAL -> Pair(W * 0.55f, H * 0.48f)
            ProductPlacement.FRAMED -> Pair(W * 0.5f, H * 0.52f)
        }

        val left = (centerX - finalW / 2f).coerceIn(MARGIN, W - MARGIN - finalW)
        val top = (centerY - finalH / 2f).coerceIn(MARGIN, H - MARGIN - finalH)

        return RectF(left, top, left + finalW, top + finalH)
    }

    private data class TextRects(
        val headline: RectF,
        val support: RectF,
        val label: RectF
    )

    private fun calculateTextRects(
        productRect: RectF,
        concept: CreativeConcept,
        meta: BackgroundMetadata
    ): TextRects {
        val textRegion = meta.usableTextRegions.firstOrNull() ?: TextRegion.UPPER_CENTER

        return when (textRegion) {
            TextRegion.UPPER_CENTER, TextRegion.TOP_CENTER, TextRegion.FULL_TOP -> {
                val headlineRect = RectF(
                    MARGIN + 40f, MARGIN + 20f,
                    W - MARGIN - 40f, productRect.top - 60f
                )
                val supportRect = RectF(
                    MARGIN + 60f, productRect.bottom + 50f,
                    W - MARGIN - 60f, productRect.bottom + 180f
                )
                val labelRect = RectF(
                    MARGIN + 60f, supportRect.bottom + 15f,
                    W - MARGIN - 60f, supportRect.bottom + 55f
                )
                TextRects(headlineRect, supportRect, labelRect)
            }
            TextRegion.TOP_LEFT, TextRegion.UPPER_LEFT -> {
                val headlineRect = RectF(
                    MARGIN + 20f, MARGIN + 20f,
                    W * 0.65f, productRect.top - 60f
                )
                val supportRect = RectF(
                    MARGIN + 20f, productRect.bottom + 50f,
                    W * 0.60f, productRect.bottom + 170f
                )
                val labelRect = RectF(
                    MARGIN + 20f, supportRect.bottom + 12f,
                    W * 0.60f, supportRect.bottom + 52f
                )
                TextRects(headlineRect, supportRect, labelRect)
            }
            TextRegion.TOP_RIGHT, TextRegion.UPPER_RIGHT -> {
                val headlineRect = RectF(
                    W * 0.35f, MARGIN + 20f,
                    W - MARGIN - 20f, productRect.top - 60f
                )
                val supportRect = RectF(
                    W * 0.40f, productRect.bottom + 50f,
                    W - MARGIN - 20f, productRect.bottom + 170f
                )
                val labelRect = RectF(
                    W * 0.40f, supportRect.bottom + 12f,
                    W - MARGIN - 20f, supportRect.bottom + 52f
                )
                TextRects(headlineRect, supportRect, labelRect)
            }
            TextRegion.FULL_BOTTOM, TextRegion.LOWER_CENTER, TextRegion.LOWER_LEFT, TextRegion.LOWER_RIGHT -> {
                val headlineRect = RectF(
                    MARGIN + 40f, MARGIN + 20f,
                    W - MARGIN - 40f, productRect.top - 60f
                )
                val supportRect = RectF(
                    MARGIN + 60f, productRect.bottom + 50f,
                    W - MARGIN - 60f, productRect.bottom + 180f
                )
                val labelRect = RectF(
                    MARGIN + 60f, supportRect.bottom + 15f,
                    W - MARGIN - 60f, supportRect.bottom + 55f
                )
                TextRects(headlineRect, supportRect, labelRect)
            }
            else -> {
                val headlineRect = RectF(
                    MARGIN + 40f, MARGIN + 20f,
                    W - MARGIN - 40f, productRect.top - 60f
                )
                val supportRect = RectF(
                    MARGIN + 60f, productRect.bottom + 50f,
                    W - MARGIN - 60f, productRect.bottom + 180f
                )
                val labelRect = RectF(
                    MARGIN + 60f, supportRect.bottom + 15f,
                    W - MARGIN - 60f, supportRect.bottom + 55f
                )
                TextRects(headlineRect, supportRect, labelRect)
            }
        }
    }

    private fun calculateTextSafeRegion(meta: BackgroundMetadata): RectF {
        val textRegion = meta.usableTextRegions.firstOrNull() ?: TextRegion.UPPER_CENTER

        return when (textRegion) {
            TextRegion.UPPER_CENTER, TextRegion.TOP_CENTER, TextRegion.FULL_TOP -> {
                RectF(MARGIN, MARGIN, W - MARGIN, H * 0.35f)
            }
            TextRegion.TOP_LEFT, TextRegion.UPPER_LEFT -> {
                RectF(MARGIN, MARGIN, W * 0.65f, H * 0.35f)
            }
            TextRegion.TOP_RIGHT, TextRegion.UPPER_RIGHT -> {
                RectF(W * 0.35f, MARGIN, W - MARGIN, H * 0.35f)
            }
            TextRegion.FULL_BOTTOM -> {
                RectF(MARGIN, H * 0.75f, W - MARGIN, H - MARGIN)
            }
            else -> {
                RectF(MARGIN, MARGIN, W - MARGIN, H * 0.35f)
            }
        }
    }

    fun drawProduct(
        c: Canvas,
        product: Bitmap,
        productRect: RectF,
        direction: ArtDirection
    ) {
        val scale = minOf(
            productRect.width() / product.width,
            productRect.height() / product.height
        )
        val dW = product.width * scale
        val dH = product.height * scale
        val dX = productRect.left + (productRect.width() - dW) / 2f
        val dY = productRect.top + (productRect.height() - dH) / 2f

        when (direction) {
            ArtDirection.SOFT_PINTEREST -> {
                val save = c.save()
                val path = Path()
                path.addRoundRect(RectF(dX, dY, dX + dW, dY + dH), 12f, 12f, Path.Direction.CW)
                c.clipPath(path)
                c.drawBitmap(product, null, RectF(dX, dY, dX + dW, dY + dH), null)
                c.restoreToCount(save)
            }
            else -> {
                c.drawBitmap(product, null, RectF(dX, dY, dX + dW, dY + dH), null)
            }
        }
    }
}
