package com.delish.pinster

import android.graphics.*

object GroundingShadowEngine {

    data class ShadowConfig(
        val contactBlur: Float = 8f,
        val contactAlpha: Int = 45,
        val ambientBlur: Float = 25f,
        val ambientAlpha: Int = 20,
        val spread: Float = 1.15f,
        val offsetY: Float = 4f,
        val offsetX: Float = 2f,
        val layers: Int = 3,
        val color: Int = Color.rgb(20, 18, 15)
    )

    private val configs = mapOf(
        ArtDirection.LUXURY_EDITORIAL to ShadowConfig(
            contactBlur = 10f, contactAlpha = 50, ambientBlur = 30f, ambientAlpha = 22,
            spread = 1.18f, offsetY = 5f, offsetX = 3f, layers = 3
        ),
        ArtDirection.MODERN_MINIMAL to ShadowConfig(
            contactBlur = 7f, contactAlpha = 40, ambientBlur = 22f, ambientAlpha = 18,
            spread = 1.12f, offsetY = 3f, offsetX = 2f, layers = 2
        ),
        ArtDirection.ORGANIC_LIFESTYLE to ShadowConfig(
            contactBlur = 8f, contactAlpha = 38, ambientBlur = 25f, ambientAlpha = 16,
            spread = 1.10f, offsetY = 4f, offsetX = 2f, layers = 2
        ),
        ArtDirection.BOLD_CAMPAIGN to ShadowConfig(
            contactBlur = 12f, contactAlpha = 55, ambientBlur = 35f, ambientAlpha = 25,
            spread = 1.20f, offsetY = 6f, offsetX = 4f, layers = 3
        ),
        ArtDirection.SOFT_PINTEREST to ShadowConfig(
            contactBlur = 6f, contactAlpha = 35, ambientBlur = 20f, ambientAlpha = 15,
            spread = 1.08f, offsetY = 3f, offsetX = 1f, layers = 2
        )
    )

    fun drawGroundingShadow(
        c: Canvas,
        productRect: RectF,
        direction: ArtDirection,
        lighting: LightingDirection = LightingDirection.SOFT_DIFFUSE
    ) {
        val config = configs[direction] ?: configs[ArtDirection.MODERN_MINIMAL]!!
        val adjustedConfig = adjustForLighting(config, lighting)

        drawContactShadow(c, productRect, adjustedConfig)
        drawAmbientShadow(c, productRect, adjustedConfig)
    }

    private fun drawContactShadow(c: Canvas, rect: RectF, config: ShadowConfig) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        for (layer in 0 until config.layers) {
            val layerScale = 1f + (layer * 0.08f)
            val layerAlpha = (config.contactAlpha * (1f - layer * 0.25f)).toInt()

            val contactRect = RectF(
                rect.left + rect.width() * 0.08f * layerScale + config.offsetX * layerScale,
                rect.bottom - 2f + config.offsetY * layerScale,
                rect.right - rect.width() * 0.08f * layerScale + config.offsetX * layerScale,
                rect.bottom + config.contactBlur * 1.5f * layerScale + config.offsetY * layerScale
            )

            paint.shader = LinearGradient(
                contactRect.left, contactRect.top, contactRect.left, contactRect.bottom,
                Color.argb(layerAlpha, Color.red(config.color), Color.green(config.color), Color.blue(config.color)),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
            c.drawOval(contactRect, paint)
        }
    }

    private fun drawAmbientShadow(c: Canvas, rect: RectF, config: ShadowConfig) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val ambientRect = RectF(
            rect.left - rect.width() * 0.05f + config.offsetX,
            rect.top + rect.height() * 0.02f + config.offsetY * 0.5f,
            rect.right + rect.width() * 0.05f + config.offsetX,
            rect.bottom + config.ambientBlur * 2f + config.offsetY * 0.5f
        )

        paint.shader = RadialGradient(
            ambientRect.centerX() + config.offsetX * 0.5f,
            ambientRect.centerY(),
            maxOf(ambientRect.width(), ambientRect.height()) * 0.5f * config.spread,
            intArrayOf(
                Color.argb(config.ambientAlpha, Color.red(config.color), Color.green(config.color), Color.blue(config.color)),
                Color.TRANSPARENT
            ),
            floatArrayOf(0.4f, 1f),
            Shader.TileMode.CLAMP
        )
        c.drawOval(ambientRect, paint)
    }

    private fun adjustForLighting(config: ShadowConfig, lighting: LightingDirection): ShadowConfig {
        return when (lighting) {
            LightingDirection.NATURAL_LEFT -> config.copy(
                offsetX = config.offsetX + 4f,
                offsetY = config.offsetY + 1f
            )
            LightingDirection.NATURAL_RIGHT -> config.copy(
                offsetX = config.offsetX - 3f,
                offsetY = config.offsetY + 1f
            )
            LightingDirection.OVERHEAD -> config.copy(
                offsetX = config.offsetX,
                offsetY = config.offsetY + 3f,
                contactAlpha = (config.contactAlpha * 1.2f).toInt()
            )
            LightingDirection.FRONT -> config.copy(
                offsetX = config.offsetX,
                offsetY = config.offsetY + 2f,
                contactAlpha = (config.contactAlpha * 0.8f).toInt()
            )
            LightingDirection.DRAMATIC -> config.copy(
                offsetX = config.offsetX + 5f,
                offsetY = config.offsetY + 2f,
                contactAlpha = (config.contactAlpha * 1.3f).toInt(),
                ambientAlpha = (config.ambientAlpha * 1.2f).toInt()
            )
            else -> config
        }
    }

    fun drawSubtleReflection(
        c: Canvas,
        product: Bitmap,
        productRect: RectF,
        direction: ArtDirection
    ) {
        when (direction) {
            ArtDirection.LUXURY_EDITORIAL, ArtDirection.BOLD_CAMPAIGN -> {
                val reflectedAlpha = when (direction) {
                    ArtDirection.LUXURY_EDITORIAL -> 12
                    ArtDirection.BOLD_CAMPAIGN -> 8
                    else -> 5
                }

                val reflectionHeight = productRect.height() * 0.25f
                val reflectionRect = RectF(
                    productRect.left,
                    productRect.bottom,
                    productRect.right,
                    productRect.bottom + reflectionHeight
                )

                val save = c.save()
                val path = Path()
                path.addRect(reflectionRect, Path.Direction.CW)
                c.clipPath(path)

                val reflectedPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                reflectedPaint.alpha = reflectedAlpha

                val scale = minOf(
                    reflectionRect.width() / product.width,
                    reflectionRect.height() / product.height
                )
                val dW = product.width * scale
                val dH = product.height * scale
                val dX = reflectionRect.left + (reflectionRect.width() - dW) / 2f
                val dY = reflectionRect.bottom - dH

                c.save()
                c.scale(1f, -1f, reflectionRect.centerX(), reflectionRect.centerY() + reflectionRect.height() / 2f)
                c.drawBitmap(product, null, RectF(dX, dY, dX + dW, dY + dH), reflectedPaint)
                c.restoreToCount(save + 1)

                val fadePaint = Paint(Paint.ANTI_ALIAS_FLAG)
                fadePaint.shader = LinearGradient(
                    0f, reflectionRect.top, 0f, reflectionRect.bottom,
                    Color.TRANSPARENT,
                    Color.argb(255, Color.red(paletteColor(c)), Color.green(paletteColor(c)), Color.blue(paletteColor(c))),
                    Shader.TileMode.CLAMP
                )
                c.drawRect(reflectionRect, fadePaint)
            }
            else -> { }
        }
    }

    private fun paletteColor(c: Canvas): Int {
        return Color.rgb(240, 238, 235)
    }
}
