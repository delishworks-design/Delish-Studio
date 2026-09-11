package com.delish.pinster

import android.graphics.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object ProductEnhancer {

    data class EnhancementConfig(
        val brightness: Float = 1.08f,
        val contrast: Float = 1.18f,
        val saturation: Float = 1.12f,
        val sharpness: Float = 1.3f,
        val warmth: Float = 1.03f,
        val vignetteStrength: Float = 0f,
        val edgeGlow: Boolean = true,
        val shadowDepth: Float = 0.15f
    )

    private val configs = mapOf(
        ArtDirection.LUXURY_EDITORIAL to EnhancementConfig(
            brightness = 1.06f, contrast = 1.22f, saturation = 1.08f,
            sharpness = 1.35f, warmth = 1.02f, edgeGlow = true, shadowDepth = 0.18f
        ),
        ArtDirection.MODERN_MINIMAL to EnhancementConfig(
            brightness = 1.10f, contrast = 1.15f, saturation = 1.10f,
            sharpness = 1.28f, warmth = 1.01f, edgeGlow = true, shadowDepth = 0.12f
        ),
        ArtDirection.ORGANIC_LIFESTYLE to EnhancementConfig(
            brightness = 1.08f, contrast = 1.12f, saturation = 1.18f,
            sharpness = 1.25f, warmth = 1.06f, edgeGlow = false, shadowDepth = 0.10f
        ),
        ArtDirection.BOLD_CAMPAIGN to EnhancementConfig(
            brightness = 1.12f, contrast = 1.25f, saturation = 1.15f,
            sharpness = 1.40f, warmth = 1.02f, edgeGlow = true, shadowDepth = 0.20f
        ),
        ArtDirection.SOFT_PINTEREST to EnhancementConfig(
            brightness = 1.10f, contrast = 1.10f, saturation = 1.12f,
            sharpness = 1.22f, warmth = 1.04f, edgeGlow = false, shadowDepth = 0.08f
        )
    )

    fun enhance(product: Bitmap, direction: ArtDirection): Bitmap {
        val config = configs[direction] ?: EnhancementConfig()
        var result = adjustBrightness(product, config.brightness)
        result = adjustContrast(result, config.contrast)
        result = adjustSaturation(result, config.saturation)
        if (config.warmth != 1.0f) {
            result = adjustWarmth(result, config.warmth)
        }
        result = sharpen(result, config.sharpness)
        if (config.edgeGlow) {
            result = addEdgeGlow(result)
        }
        return result
    }

    fun addEnhancedShadow(
        c: Canvas,
        product: Bitmap,
        productRect: RectF,
        direction: ArtDirection
    ) {
        val config = configs[direction] ?: EnhancementConfig()
        if (config.shadowDepth <= 0f) return

        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        shadowPaint.maskFilter = BlurMaskFilter(18f, BlurMaskFilter.Blur.NORMAL)
        val alpha = (config.shadowDepth * 255).toInt().coerceIn(30, 70)
        shadowPaint.color = Color.argb(alpha, 0, 0, 0)

        val offset = 6f
        c.drawBitmap(product, null, RectF(
            productRect.left + offset,
            productRect.top + offset,
            productRect.right + offset,
            productRect.bottom + offset
        ), shadowPaint)
    }

    private fun adjustBrightness(src: Bitmap, factor: Float): Bitmap {
        val w = src.width
        val h = src.height
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)

        for (i in pixels.indices) {
            val r = min(255, (Color.red(pixels[i]) * factor).toInt())
            val g = min(255, (Color.green(pixels[i]) * factor).toInt())
            val b = min(255, (Color.blue(pixels[i]) * factor).toInt())
            pixels[i] = Color.argb(Color.alpha(pixels[i]), r, g, b)
        }

        bmp.setPixels(pixels, 0, w, 0, 0, w, h)
        return bmp
    }

    private fun adjustContrast(src: Bitmap, factor: Float): Bitmap {
        val w = src.width
        val h = src.height
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)

        val intercept = 128 * (1 - factor)

        for (i in pixels.indices) {
            val r = min(255, max(0, (Color.red(pixels[i]) * factor + intercept).toInt()))
            val g = min(255, max(0, (Color.green(pixels[i]) * factor + intercept).toInt()))
            val b = min(255, max(0, (Color.blue(pixels[i]) * factor + intercept).toInt()))
            pixels[i] = Color.argb(Color.alpha(pixels[i]), r, g, b)
        }

        bmp.setPixels(pixels, 0, w, 0, 0, w, h)
        return bmp
    }

    private fun adjustSaturation(src: Bitmap, factor: Float): Bitmap {
        val w = src.width
        val h = src.height
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)

        for (i in pixels.indices) {
            val r = Color.red(pixels[i])
            val g = Color.green(pixels[i])
            val b = Color.blue(pixels[i])
            val gray = 0.299f * r + 0.587f * g + 0.114f * b
            val nr = min(255, max(0, (gray + factor * (r - gray)).toInt()))
            val ng = min(255, max(0, (gray + factor * (g - gray)).toInt()))
            val nb = min(255, max(0, (gray + factor * (b - gray)).toInt()))
            pixels[i] = Color.argb(Color.alpha(pixels[i]), nr, ng, nb)
        }

        bmp.setPixels(pixels, 0, w, 0, 0, w, h)
        return bmp
    }

    private fun adjustWarmth(src: Bitmap, factor: Float): Bitmap {
        val w = src.width
        val h = src.height
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)

        for (i in pixels.indices) {
            val r = min(255, (Color.red(pixels[i]) * factor).toInt())
            val g = Color.green(pixels[i])
            val b = min(255, (Color.blue(pixels[i]) / factor).toInt())
            pixels[i] = Color.argb(Color.alpha(pixels[i]), r, g, b)
        }

        bmp.setPixels(pixels, 0, w, 0, 0, w, h)
        return bmp
    }

    private fun sharpen(src: Bitmap, factor: Float): Bitmap {
        val w = src.width
        val h = src.height
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)

        val strength = (factor - 1.0f) * 0.5f
        val kernel = floatArrayOf(
            0f, -strength, 0f,
            -strength, 1f + 4f * strength, -strength,
            0f, -strength, 0f
        )

        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                var r = 0f; var g = 0f; var b = 0f
                var ki = 0
                for (ky in -1..1) {
                    for (kx in -1..1) {
                        val px = pixels[(y + ky) * w + (x + kx)]
                        r += Color.red(px) * kernel[ki]
                        g += Color.green(px) * kernel[ki]
                        b += Color.blue(px) * kernel[ki]
                        ki++
                    }
                }
                pixels[y * w + x] = Color.argb(
                    Color.alpha(pixels[y * w + x]),
                    min(255, max(0, r.toInt())),
                    min(255, max(0, g.toInt())),
                    min(255, max(0, b.toInt()))
                )
            }
        }

        bmp.setPixels(pixels, 0, w, 0, 0, w, h)
        return bmp
    }

    private fun addEdgeGlow(src: Bitmap): Bitmap {
        val w = src.width
        val h = src.height
        val bmp = src.copy(Bitmap.Config.ARGB_8888, true)

        val edgePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        edgePaint.maskFilter = BlurMaskFilter(4f, BlurMaskFilter.Blur.NORMAL)
        edgePaint.colorFilter = PorterDuffColorFilter(Color.argb(25, 255, 255, 255), PorterDuff.Mode.SRC_ATOP)

        val canvas = Canvas(bmp)
        canvas.drawBitmap(bmp, 0f, 0f, edgePaint)

        return bmp
    }

    fun createProductCutout(
        product: Bitmap,
        bgColor: Int
    ): Bitmap {
        val w = product.width
        val h = product.height
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(w * h)
        product.getPixels(pixels, 0, w, 0, 0, w, h)

        val bgR = Color.red(bgColor)
        val bgG = Color.green(bgColor)
        val bgB = Color.blue(bgColor)

        for (i in pixels.indices) {
            val r = Color.red(pixels[i])
            val g = Color.green(pixels[i])
            val b = Color.blue(pixels[i])
            val dist = sqrt(
                ((r - bgR) * (r - bgR) + (g - bgG) * (g - bgG) + (b - bgB) * (b - bgB)).toDouble()
            ).toFloat()
            val alpha = if (dist < 30f) 0 else if (dist < 60f) (dist * 4.25f).toInt().coerceIn(0, 255) else 255
            pixels[i] = Color.argb(alpha, r, g, b)
        }

        bmp.setPixels(pixels, 0, w, 0, 0, w, h)
        return bmp
    }
}
