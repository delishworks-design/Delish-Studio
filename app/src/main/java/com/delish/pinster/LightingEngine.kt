package com.delish.pinster

import android.graphics.*
import kotlin.math.cos
import kotlin.math.sin

object LightingEngine {

    fun drawAmbientShadow(c: Canvas, rect: RectF, palette: PosterPalette, direction: ArtDirection = ArtDirection.LUXURY_EDITORIAL) {
        val config = ShadowPersonalities.configs[direction] ?: ShadowPersonalities.configs[ArtDirection.LUXURY_EDITORIAL]!!
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        for (layer in 0 until config.layers) {
            val layerScale = 1f + (layer * 0.2f)
            val layerAlpha = (config.ambientAlpha * (1f - layer * 0.3f)).toInt()

            paint.shader = RadialGradient(
                rect.centerX() + config.offsetX * layerScale,
                rect.centerY() + config.offsetY * layerScale + rect.height() * 0.1f,
                maxOf(rect.width(), rect.height()) * 0.7f * layerScale,
                intArrayOf(Color.TRANSPARENT, Color.argb(layerAlpha, Color.red(config.color), Color.green(config.color), Color.blue(config.color))),
                floatArrayOf(0.5f, 1f),
                Shader.TileMode.CLAMP
            )
            c.drawOval(
                RectF(
                    rect.left - rect.width() * 0.15f + config.offsetX * layerScale,
                    rect.top - rect.height() * 0.05f + config.offsetY * layerScale,
                    rect.right + rect.width() * 0.15f + config.offsetX * layerScale,
                    rect.bottom + rect.height() * 0.2f + config.offsetY * layerScale
                ),
                paint
            )
        }
    }

    fun drawContactShadow(c: Canvas, rect: RectF, palette: PosterPalette, direction: ArtDirection = ArtDirection.LUXURY_EDITORIAL) {
        val config = ShadowPersonalities.configs[direction] ?: ShadowPersonalities.configs[ArtDirection.LUXURY_EDITORIAL]!!
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val contactRect = RectF(
            rect.left + rect.width() * 0.1f + config.offsetX,
            rect.bottom - config.contactOffset + config.offsetY,
            rect.right - rect.width() * 0.1f + config.offsetX,
            rect.bottom + config.contactBlur * 2f + config.offsetY
        )
        paint.shader = LinearGradient(
            contactRect.left, contactRect.top, contactRect.left, contactRect.bottom,
            Color.argb((config.opacity * 255).toInt(), Color.red(config.color), Color.green(config.color), Color.blue(config.color)),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        c.drawOval(contactRect, paint)
    }

    fun drawStudioLighting(c: Canvas, w: Int, h: Int, palette: PosterPalette, direction: ArtDirection) {
        when (direction) {
            ArtDirection.LUXURY_EDITORIAL -> drawDramaticLighting(c, w, h, palette)
            ArtDirection.MODERN_MINIMAL -> drawSoftFillLighting(c, w, h, palette)
            ArtDirection.ORGANIC_LIFESTYLE -> drawWarmDiffuseLighting(c, w, h, palette)
            ArtDirection.BOLD_CAMPAIGN -> drawDirectionalLighting(c, w, h, palette)
            ArtDirection.SOFT_PINTEREST -> drawAmbientLighting(c, w, h, palette)
        }
    }

    private fun drawDramaticLighting(c: Canvas, w: Int, h: Int, palette: PosterPalette) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = RadialGradient(
            w * 0.3f, h * 0.2f, w * 0.8f,
            intArrayOf(Color.argb(25, 255, 240, 220), Color.TRANSPARENT),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)

        val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        rimPaint.shader = RadialGradient(
            w * 0.7f, h * 0.15f, w * 0.5f,
            intArrayOf(Color.argb(15, 255, 255, 255), Color.TRANSPARENT),
            floatArrayOf(0f, 0.8f),
            Shader.TileMode.CLAMP
        )
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), rimPaint)
    }

    private fun drawSoftFillLighting(c: Canvas, w: Int, h: Int, palette: PosterPalette) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = RadialGradient(
            w * 0.5f, h * 0.35f, w * 0.6f,
            intArrayOf(Color.argb(12, 240, 245, 255), Color.TRANSPARENT),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
    }

    private fun drawWarmDiffuseLighting(c: Canvas, w: Int, h: Int, palette: PosterPalette) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = RadialGradient(
            w * 0.6f, h * 0.3f, w * 0.7f,
            intArrayOf(Color.argb(18, 255, 230, 200), Color.TRANSPARENT),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
    }

    private fun drawDirectionalLighting(c: Canvas, w: Int, h: Int, palette: PosterPalette) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val matrix = Matrix()
        matrix.setRotate(35f, w * 0.5f, h * 0.5f)
        paint.shader = LinearGradient(
            0f, 0f, w.toFloat(), 0f,
            intArrayOf(Color.TRANSPARENT, Color.argb(20, 255, 255, 255), Color.TRANSPARENT),
            floatArrayOf(0.2f, 0.5f, 0.8f),
            Shader.TileMode.CLAMP
        )
        paint.shader?.setLocalMatrix(matrix)
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
    }

    private fun drawAmbientLighting(c: Canvas, w: Int, h: Int, palette: PosterPalette) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = RadialGradient(
            w * 0.5f, h * 0.45f, w * 0.55f,
            intArrayOf(Color.argb(10, 255, 245, 255), Color.TRANSPARENT),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
    }

    fun drawRimLighting(c: Canvas, productRect: RectF, palette: PosterPalette, direction: ArtDirection) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rimAlpha = when (direction) {
            ArtDirection.LUXURY_EDITORIAL -> 20
            ArtDirection.BOLD_CAMPAIGN -> 25
            else -> 12
        }
        val rimColor = Color.argb(rimAlpha, 255, 255, 255)
        paint.shader = LinearGradient(
            productRect.left, productRect.top,
            productRect.right, productRect.top,
            rimColor, Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        c.drawRect(productRect, paint)
    }

    fun drawProductGlow(c: Canvas, productRect: RectF, palette: PosterPalette, radius: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = RadialGradient(
            productRect.centerX(), productRect.centerY(),
            maxOf(productRect.width(), productRect.height()) * 0.5f + radius,
            intArrayOf(Color.argb(15, Color.red(palette.highlight), Color.green(palette.highlight), Color.blue(palette.highlight)), Color.TRANSPARENT),
            floatArrayOf(0.6f, 1f),
            Shader.TileMode.CLAMP
        )
        c.drawOval(
            RectF(
                productRect.left - radius, productRect.top - radius,
                productRect.right + radius, productRect.bottom + radius
            ),
            paint
        )
    }

    fun drawLightLeak(c: Canvas, w: Int, h: Int, direction: ArtDirection, palette: PosterPalette) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        when (direction) {
            ArtDirection.LUXURY_EDITORIAL -> {
                paint.shader = RadialGradient(
                    w * 0.8f, h * 0.1f, w * 0.4f,
                    intArrayOf(Color.argb(12, 255, 220, 180), Color.TRANSPARENT),
                    floatArrayOf(0f, 0.8f), Shader.TileMode.CLAMP
                )
            }
            ArtDirection.ORGANIC_LIFESTYLE -> {
                paint.shader = RadialGradient(
                    w * 0.2f, h * 0.15f, w * 0.35f,
                    intArrayOf(Color.argb(10, 255, 240, 200), Color.TRANSPARENT),
                    floatArrayOf(0f, 0.85f), Shader.TileMode.CLAMP
                )
            }
            ArtDirection.SOFT_PINTEREST -> {
                paint.shader = RadialGradient(
                    w * 0.5f, h * 0.2f, w * 0.5f,
                    intArrayOf(Color.argb(8, 255, 240, 255), Color.TRANSPARENT),
                    floatArrayOf(0f, 0.9f), Shader.TileMode.CLAMP
                )
            }
            else -> return
        }
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
    }

    fun drawBokeh(c: Canvas, w: Int, h: Int, direction: ArtDirection, palette: PosterPalette) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.style = Paint.Style.FILL

        val count = when (direction) {
            ArtDirection.LUXURY_EDITORIAL -> 5
            ArtDirection.ORGANIC_LIFESTYLE -> 4
            ArtDirection.SOFT_PINTEREST -> 6
            ArtDirection.BOLD_CAMPAIGN -> 3
            else -> 2
        }

        val rng = kotlin.random.Random(42)
        for (i in 0 until count) {
            val x = w * (0.1f + rng.nextFloat() * 0.8f)
            val y = h * (0.1f + rng.nextFloat() * 0.8f)
            val r = 8f + rng.nextFloat() * 25f
            val alpha = 5 + rng.nextInt(10)
            paint.shader = RadialGradient(
                x, y, r,
                intArrayOf(Color.argb(alpha, 255, 255, 255), Color.TRANSPARENT),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
            c.drawCircle(x, y, r, paint)
        }
    }

    fun drawVolumetricRays(c: Canvas, productRect: RectF, palette: PosterPalette) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val cx = productRect.centerX()
        val cy = productRect.centerY()

        for (i in 0..2) {
            val angle = -30f + i * 30f
            val rad = Math.toRadians(angle.toDouble())
            val rayLen = 200f + i * 50f
            val endX = cx + (rayLen * cos(rad)).toFloat()
            val endY = cy - (rayLen * sin(rad)).toFloat()

            paint.shader = LinearGradient(
                cx, cy, endX, endY,
                Color.argb(6, 255, 255, 255), Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
            paint.strokeWidth = 15f + i * 5f
            paint.style = Paint.Style.STROKE
            c.drawLine(cx, cy, endX, endY, paint)
        }
        paint.style = Paint.Style.FILL
    }

    fun drawProductReflection(c: Canvas, product: Bitmap, productRect: RectF, palette: PosterPalette) {
        val reflectionHeight = productRect.height() * 0.25f
        val reflectionRect = RectF(
            productRect.left,
            productRect.bottom,
            productRect.right,
            productRect.bottom + reflectionHeight
        )

        val save = c.save()
        val matrix = Matrix()
        matrix.setScale(1f, -1f, product.width / 2f, product.height / 2f)
        c.concat(matrix)

        val reflectionPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        reflectionPaint.alpha = 25
        c.drawBitmap(product, null, reflectionRect, reflectionPaint)

        val fadePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        fadePaint.shader = LinearGradient(
            reflectionRect.left, reflectionRect.top,
            reflectionRect.left, reflectionRect.bottom,
            Color.argb(60, Color.red(palette.background), Color.green(palette.background), Color.blue(palette.background)),
            palette.background,
            Shader.TileMode.CLAMP
        )
        c.drawRect(reflectionRect, fadePaint)
        c.restoreToCount(save)
    }

    fun drawVignette(c: Canvas, w: Int, h: Int, strength: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = RadialGradient(
            w * 0.5f, h * 0.5f, maxOf(w, h) * 0.7f,
            intArrayOf(Color.TRANSPARENT, Color.argb(strength, 0, 0, 0)),
            floatArrayOf(0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
    }
}
