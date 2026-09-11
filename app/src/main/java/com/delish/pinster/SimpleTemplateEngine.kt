package com.delish.pinster

import android.graphics.*

object SimpleTemplateEngine {

    private const val W = 1000f
    private const val H = 1500f
    private const val M = 80f

    enum class Template {
        HERO_CENTER,
        SPLIT,
        EDITORIAL,
        FEATURE,
        BOTTOM_HERO
    }

    data class TemplateLayout(
        val template: Template,
        val productRect: RectF,
        val headlineRect: RectF,
        val supportRect: RectF,
        val eyebrowRect: RectF,
        val featureRects: List<RectF>,
        val textColor: Int,
        val bgColor: Int,
        val accentColor: Int
    )

    fun calculateLayout(
        product: Bitmap,
        template: Template,
        concept: CreativeConcept,
        fp: ProductFingerprint
    ): TemplateLayout {
        val productAspect = product.width.toFloat() / product.height
        val isWide = productAspect > 1.2f
        val isTall = productAspect < 0.8f

        val bgColor = Color.rgb(250, 249, 247)
        val textColor = Color.rgb(28, 28, 28)
        val accentColor = Color.rgb(100, 100, 100)

        return when (template) {
            Template.HERO_CENTER -> heroCenterLayout(product, concept, productAspect, bgColor, textColor, accentColor)
            Template.SPLIT -> splitLayout(product, concept, productAspect, isWide, bgColor, textColor, accentColor)
            Template.EDITORIAL -> editorialLayout(product, concept, productAspect, bgColor, textColor, accentColor)
            Template.FEATURE -> featureLayout(product, concept, productAspect, bgColor, textColor, accentColor)
            Template.BOTTOM_HERO -> bottomHeroLayout(product, concept, productAspect, bgColor, textColor, accentColor)
        }
    }

    // ─── TEMPLATE 1: HERO CENTER ─────────────────

    private fun heroCenterLayout(
        product: Bitmap,
        concept: CreativeConcept,
        aspect: Float,
        bg: Int, text: Int, accent: Int
    ): TemplateLayout {
        val productW = W * 0.65f
        val productH = productW / aspect
        val maxProductH = H * 0.50f
        val finalH = productH.coerceAtMost(maxProductH)
        val finalW = (finalH * aspect).coerceAtMost(W * 0.85f)

        val productRect = RectF(
            (W - finalW) / 2f,
            (H - finalH) / 2f - 40f,
            (W + finalW) / 2f,
            (H + finalH) / 2f - 40f
        )

        val eyebrowRect = RectF(M, productRect.top - 90f, W - M, productRect.top - 55f)
        val headlineRect = RectF(M + 20f, productRect.bottom + 50f, W - M - 20f, productRect.bottom + 180f)
        val supportRect = RectF(M + 40f, headlineRect.bottom + 15f, W - M - 40f, headlineRect.bottom + 70f)

        return TemplateLayout(
            template = Template.HERO_CENTER,
            productRect = productRect,
            headlineRect = headlineRect,
            supportRect = supportRect,
            eyebrowRect = eyebrowRect,
            featureRects = emptyList(),
            textColor = text,
            bgColor = bg,
            accentColor = accent
        )
    }

    // ─── TEMPLATE 2: SPLIT ───────────────────────

    private fun splitLayout(
        product: Bitmap,
        concept: CreativeConcept,
        aspect: Float,
        isWide: Boolean,
        bg: Int, text: Int, accent: Int
    ): TemplateLayout {
        val productW = W * 0.42f
        val productH = productW / aspect
        val maxProductH = H * 0.65f
        val finalH = productH.coerceAtMost(maxProductH)
        val finalW = (finalH * aspect).coerceAtMost(W * 0.42f)

        val productRect = RectF(
            M,
            (H - finalH) / 2f,
            M + finalW,
            (H + finalH) / 2f
        )

        val textLeft = productRect.right + 50f
        val textRight = W - M

        val eyebrowRect = RectF(textLeft, H * 0.28f, textRight, H * 0.28f + 35f)
        val headlineRect = RectF(textLeft, H * 0.32f, textRight, H * 0.52f)
        val supportRect = RectF(textLeft, H * 0.54f, textRight, H * 0.64f)

        return TemplateLayout(
            template = Template.SPLIT,
            productRect = productRect,
            headlineRect = headlineRect,
            supportRect = supportRect,
            eyebrowRect = eyebrowRect,
            featureRects = emptyList(),
            textColor = text,
            bgColor = bg,
            accentColor = accent
        )
    }

    // ─── TEMPLATE 3: EDITORIAL ───────────────────

    private fun editorialLayout(
        product: Bitmap,
        concept: CreativeConcept,
        aspect: Float,
        bg: Int, text: Int, accent: Int
    ): TemplateLayout {
        val productW = W * 0.70f
        val productH = productW / aspect
        val maxProductH = H * 0.48f
        val finalH = productH.coerceAtMost(maxProductH)
        val finalW = (finalH * aspect).coerceAtMost(W * 0.85f)

        val productRect = RectF(
            (W - finalW) / 2f,
            H * 0.42f,
            (W + finalW) / 2f,
            H * 0.42f + finalH
        )

        val eyebrowRect = RectF(M + 40f, M + 20f, W - M - 40f, M + 55f)
        val headlineRect = RectF(M + 40f, M + 65f, W - M - 40f, M + 200f)
        val supportRect = RectF(M + 60f, productRect.bottom + 50f, W - M - 60f, productRect.bottom + 120f)

        return TemplateLayout(
            template = Template.EDITORIAL,
            productRect = productRect,
            headlineRect = headlineRect,
            supportRect = supportRect,
            eyebrowRect = eyebrowRect,
            featureRects = emptyList(),
            textColor = text,
            bgColor = bg,
            accentColor = accent
        )
    }

    // ─── TEMPLATE 4: FEATURE ─────────────────────

    private fun featureLayout(
        product: Bitmap,
        concept: CreativeConcept,
        aspect: Float,
        bg: Int, text: Int, accent: Int
    ): TemplateLayout {
        val productW = W * 0.60f
        val productH = productW / aspect
        val maxProductH = H * 0.42f
        val finalH = productH.coerceAtMost(maxProductH)
        val finalW = (finalH * aspect).coerceAtMost(W * 0.80f)

        val productRect = RectF(
            (W - finalW) / 2f,
            H * 0.35f,
            (W + finalW) / 2f,
            H * 0.35f + finalH
        )

        val eyebrowRect = RectF(M + 40f, M + 15f, W - M - 40f, M + 50f)
        val headlineRect = RectF(M + 40f, M + 60f, W - M - 40f, M + 180f)

        val featureY = productRect.bottom + 50f
        val featureRects = listOf(
            RectF(M + 60f, featureY, W - M - 60f, featureY + 30f),
            RectF(M + 60f, featureY + 40f, W - M - 60f, featureY + 70f),
            RectF(M + 60f, featureY + 80f, W - M - 60f, featureY + 110f)
        )

        val supportRect = RectF(M + 60f, featureY + 120f, W - M - 60f, featureY + 160f)

        return TemplateLayout(
            template = Template.FEATURE,
            productRect = productRect,
            headlineRect = headlineRect,
            supportRect = supportRect,
            eyebrowRect = eyebrowRect,
            featureRects = featureRects,
            textColor = text,
            bgColor = bg,
            accentColor = accent
        )
    }

    // ─── TEMPLATE 5: BOTTOM HERO ─────────────────

    private fun bottomHeroLayout(
        product: Bitmap,
        concept: CreativeConcept,
        aspect: Float,
        bg: Int, text: Int, accent: Int
    ): TemplateLayout {
        val productW = W * 0.72f
        val productH = productW / aspect
        val maxProductH = H * 0.52f
        val finalH = productH.coerceAtMost(maxProductH)
        val finalW = (finalH * aspect).coerceAtMost(W * 0.90f)

        val productRect = RectF(
            (W - finalW) / 2f,
            H - finalH - M,
            (W + finalW) / 2f,
            H - M
        )

        val eyebrowRect = RectF(M + 40f, M + 20f, W - M - 40f, M + 55f)
        val headlineRect = RectF(M + 40f, M + 65f, W - M - 40f, M + 200f)
        val supportRect = RectF(M + 60f, M + 215f, W - M - 60f, M + 275f)

        return TemplateLayout(
            template = Template.BOTTOM_HERO,
            productRect = productRect,
            headlineRect = headlineRect,
            supportRect = supportRect,
            eyebrowRect = eyebrowRect,
            featureRects = emptyList(),
            textColor = text,
            bgColor = bg,
            accentColor = accent
        )
    }

    // ─── RENDERING ───────────────────────────────

    fun render(
        c: Canvas,
        product: Bitmap,
        layout: TemplateLayout,
        concept: CreativeConcept,
        typo: TypographySystem
    ) {
        drawBackground(c, layout)
        drawProduct(c, product, layout)
        drawSubtleShadow(c, layout)
        drawTypography(c, layout, concept, typo)
    }

    private fun drawBackground(c: Canvas, layout: TemplateLayout) {
        c.drawColor(layout.bgColor)
    }

    private fun drawProduct(c: Canvas, product: Bitmap, layout: TemplateLayout) {
        val r = layout.productRect
        val scale = minOf(r.width() / product.width, r.height() / product.height)
        val dW = product.width * scale
        val dH = product.height * scale
        val dX = r.left + (r.width() - dW) / 2f
        val dY = r.top + (r.height() - dH) / 2f

        c.drawBitmap(product, null, RectF(dX, dY, dX + dW, dY + dH), null)
    }

    private fun drawSubtleShadow(c: Canvas, layout: TemplateLayout) {
        val r = layout.productRect
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        shadowPaint.shader = LinearGradient(
            r.left, r.bottom - 5f, r.left, r.bottom + 20f,
            Color.argb(12, 0, 0, 0),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        c.drawRect(r.left - 10f, r.bottom - 5f, r.right + 10f, r.bottom + 20f, shadowPaint)
    }

    private fun drawTypography(c: Canvas, layout: TemplateLayout, concept: CreativeConcept, typo: TypographySystem) {
        // ─── EYEBROW ───────────────────────────────
        val eyePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        eyePaint.color = layout.accentColor
        eyePaint.typeface = Typeface.create(typo.eyebrowTypeface, Typeface.NORMAL)
        eyePaint.textSize = 14f
        eyePaint.letterSpacing = 0.15f
        eyePaint.textAlign = Paint.Align.LEFT
        c.drawText(concept.eyebrowText.uppercase(), layout.eyebrowRect.left, layout.eyebrowRect.bottom, eyePaint)

        // ─── HEADLINE ──────────────────────────────
        val lines = TypographyEngine.headlineLines(concept.hook)
        val hPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        hPaint.color = layout.textColor
        hPaint.typeface = Typeface.create(typo.headlineTypeface, typo.headlineWeight)
        hPaint.letterSpacing = typo.tracking

        val maxHeadW = layout.headlineRect.width() * 0.95f
        val maxHeadH = layout.headlineRect.height() * 0.90f
        val hSize = TypographyEngine.fitHeadline(hPaint, lines, maxHeadW, 72f, 36f, maxHeadH, typo.headlineLineSpacing)
        hPaint.textSize = hSize

        // Simple shadow
        val shadowPaint = Paint(hPaint)
        shadowPaint.color = Color.argb(15, 0, 0, 0)
        shadowPaint.textAlign = Paint.Align.LEFT
        TypographyEngine.drawLines(c, lines, shadowPaint, layout.headlineRect.left + 1.5f, layout.headlineRect.top + 30f + 1.5f, 3, typo.headlineLineSpacing)

        hPaint.textAlign = Paint.Align.LEFT
        TypographyEngine.drawLines(c, lines, hPaint, layout.headlineRect.left, layout.headlineRect.top + 30f, 3, typo.headlineLineSpacing)

        // ─── SUPPORT ───────────────────────────────
        if (concept.supportCopy.isNotBlank()) {
            val sPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            sPaint.color = layout.textColor
            sPaint.alpha = 180
            sPaint.typeface = Typeface.create(typo.supportTypeface, typo.supportWeight)
            sPaint.textSize = 18f
            sPaint.textAlign = Paint.Align.LEFT

            val maxSupW = layout.supportRect.width() * 0.95f
            val supLines = TypographyEngine.wrapText(concept.supportCopy, sPaint, maxSupW)
            val maxSupH = layout.supportRect.height() * 0.90f
            val sSize = TypographyEngine.fitSupport(sPaint, supLines, maxSupW, 18f, 12f, maxSupH, typo.supportLineSpacing)
            sPaint.textSize = sSize
            TypographyEngine.drawLines(c, supLines, sPaint, layout.supportRect.left, layout.supportRect.top, 3, typo.supportLineSpacing)
        }

        // ─── FEATURES ──────────────────────────────
        if (layout.template == Template.FEATURE && layout.featureRects.isNotEmpty()) {
            val features = extractFeatures(concept)
            val fPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            fPaint.color = layout.textColor
            fPaint.alpha = 200
            fPaint.typeface = Typeface.create(typo.supportTypeface, Typeface.NORMAL)
            fPaint.textSize = 16f
            fPaint.textAlign = Paint.Align.LEFT

            for (i in layout.featureRects.indices.take(features.size)) {
                val feature = features[i]
                val bulletPaint = Paint(fPaint)
                bulletPaint.color = layout.accentColor
                c.drawText("\u2022", layout.featureRects[i].left, layout.featureRects[i].bottom, bulletPaint)
                c.drawText("  $feature", layout.featureRects[i].left + 16f, layout.featureRects[i].bottom, fPaint)
            }
        }
    }

    private fun extractFeatures(concept: CreativeConcept): List<String> {
        val features = mutableListOf<String>()
        val support = concept.supportCopy
        if (support.isNotBlank()) {
            val sentences = support.split(Regex("[.!?]+")).filter { it.trim().length > 5 }
            for (sentence in sentences.take(3)) {
                val feature = sentence.trim().take(40)
                if (feature.isNotBlank()) {
                    features.add(feature)
                }
            }
        }
        if (features.isEmpty()) {
            features.add("Premium quality")
            features.add("Smart design")
            features.add("Built to last")
        }
        return features
    }
}
