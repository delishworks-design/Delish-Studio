package com.delish.pinster

import android.graphics.*
import kotlin.math.max
import kotlin.math.min

object TextBackgroundRenderer {

    data class TextRegion(
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
        val lineCount: Int = 1,
        val lineSpacing: Float = 0f
    )

    fun drawTextWithBackground(
        c: Canvas,
        text: String,
        paint: Paint,
        region: TextRegion,
        bgColor: Int,
        bgAlpha: Int = 180,
        cornerRadius: Float = 8f,
        paddingH: Float = 12f,
        paddingV: Float = 6f,
        shadowEnabled: Boolean = true
    ) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        bgPaint.color = Color.argb(bgAlpha, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))

        val totalH = region.height * region.lineCount + (region.lineSpacing * (region.lineCount - 1).coerceAtLeast(0))
        val left = region.x - paddingH
        val top = region.y - region.height - paddingV
        val right = region.x + region.width + paddingH
        val bottom = region.y + totalH + paddingV

        if (shadowEnabled) {
            val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            shadowPaint.color = Color.argb(40, 0, 0, 0)
            c.drawRoundRect(
                RectF(left + 3f, top + 3f, right + 3f, bottom + 3f),
                cornerRadius, cornerRadius, shadowPaint
            )
        }

        c.drawRoundRect(RectF(left, top, right, bottom), cornerRadius, cornerRadius, bgPaint)
        c.drawText(text, region.x, region.y, paint)
    }

    fun drawTextWithGradientBackground(
        c: Canvas,
        text: String,
        paint: Paint,
        region: TextRegion,
        gradientStart: Int,
        gradientEnd: Int,
        bgAlpha: Int = 160,
        paddingH: Float = 12f,
        paddingV: Float = 6f
    ) {
        val totalH = region.height * region.lineCount + (region.lineSpacing * (region.lineCount - 1).coerceAtLeast(0))
        val left = region.x - paddingH
        val top = region.y - region.height - paddingV
        val right = region.x + region.width + paddingH
        val bottom = region.y + totalH + paddingV

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        bgPaint.shader = LinearGradient(
            left, top, right, bottom,
            Color.argb(bgAlpha, Color.red(gradientStart), Color.green(gradientStart), Color.blue(gradientStart)),
            Color.argb(bgAlpha, Color.red(gradientEnd), Color.green(gradientEnd), Color.blue(gradientEnd)),
            Shader.TileMode.CLAMP
        )
        c.drawRect(RectF(left, top, right, bottom), bgPaint)
        c.drawText(text, region.x, region.y, paint)
    }

    fun drawMultiLineTextWithBackground(
        c: Canvas,
        lines: List<String>,
        paint: Paint,
        x: Float,
        startY: Float,
        lineSpacing: Float,
        bgColor: Int,
        bgAlpha: Int = 180,
        cornerRadius: Float = 10f,
        paddingH: Float = 14f,
        paddingV: Float = 8f,
        textAlign: Paint.Align = Paint.Align.LEFT,
        shadowEnabled: Boolean = true
    ) {
        if (lines.isEmpty()) return

        val lineHeights = lines.map { line ->
            val bounds = Rect()
            paint.getTextBounds(line, 0, line.length, bounds)
            bounds.height().toFloat()
        }

        val maxWidth = lines.maxOf { line ->
            val bounds = Rect()
            paint.getTextBounds(line, 0, line.length, bounds)
            bounds.width().toFloat()
        }

        val totalTextHeight = lineHeights.sum() + (lineSpacing * (lines.size - 1).coerceAtLeast(0))
        val maxLineH = lineHeights.max()

        val left = when (textAlign) {
            Paint.Align.CENTER -> x - maxWidth / 2f - paddingH
            Paint.Align.RIGHT -> x - maxWidth - paddingH
            else -> x - paddingH
        }
        val right = when (textAlign) {
            Paint.Align.CENTER -> x + maxWidth / 2f + paddingH
            Paint.Align.RIGHT -> x + paddingH
            else -> x + maxWidth + paddingH
        }
        val top = startY - maxLineH - paddingV
        val bottom = startY + totalTextHeight + paddingV

        if (shadowEnabled) {
            val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            shadowPaint.color = Color.argb(35, 0, 0, 0)
            c.drawRoundRect(
                RectF(left + 4f, top + 4f, right + 4f, bottom + 4f),
                cornerRadius, cornerRadius, shadowPaint
            )
        }

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        bgPaint.color = Color.argb(bgAlpha, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
        c.drawRoundRect(RectF(left, top, right, bottom), cornerRadius, cornerRadius, bgPaint)

        var curY = startY
        for (line in lines) {
            c.drawText(line, x, curY, paint)
            curY += lineSpacing
        }
    }

    fun drawOutlinedText(
        c: Canvas,
        text: String,
        paint: Paint,
        x: Float,
        y: Float,
        outlineColor: Int,
        outlineWidth: Float = 3f,
        outlineAlpha: Int = 200
    ) {
        val outlinePaint = Paint(paint)
        outlinePaint.style = Paint.Style.STROKE
        outlinePaint.strokeWidth = outlineWidth
        outlinePaint.color = Color.argb(outlineAlpha, Color.red(outlineColor), Color.green(outlineColor), Color.blue(outlineColor))
        outlinePaint.strokeJoin = Paint.Join.ROUND
        c.drawText(text, x, y, outlinePaint)
        c.drawText(text, x, y, paint)
    }

    fun drawTextWithUnderline(
        c: Canvas,
        text: String,
        paint: Paint,
        x: Float,
        y: Float,
        underlineColor: Int,
        underlineThickness: Float = 2f,
        underlineAlpha: Int = 100,
        underlineOffset: Float = 4f
    ) {
        c.drawText(text, x, y, paint)
        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        val ulPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        ulPaint.strokeWidth = underlineThickness
        ulPaint.color = Color.argb(underlineAlpha, Color.red(underlineColor), Color.green(underlineColor), Color.blue(underlineColor))
        val ulY = y + underlineOffset
        c.drawLine(x, ulY, x + bounds.width().toFloat(), ulY, ulPaint)
    }

    fun measureTextWidth(paint: Paint, text: String): Float {
        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        return bounds.width().toFloat()
    }

    fun measureTextHeight(paint: Paint, text: String): Float {
        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        return bounds.height().toFloat()
    }
}
