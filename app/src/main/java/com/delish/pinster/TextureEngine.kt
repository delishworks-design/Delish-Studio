package com.delish.pinster

import android.graphics.*
import kotlin.random.Random

object TextureEngine {

    fun addProductAwareGrain(c: Canvas, w: Int, h: Int, fp: ProductFingerprint) {
        val surface = TextureIntelligence.detectSurface(fp)
        val intensity = TextureIntelligence.grainIntensity(surface)
        addPaperGrain(c, w, h, intensity)
    }

    fun addPaperGrain(c: Canvas, w: Int, h: Int, intensity: Int = 8) {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(w * h)
        val rng = Random(42)
        for (i in pixels.indices) {
            val noise = rng.nextInt(-intensity, intensity + 1)
            val v = (128 + noise).coerceIn(0, 255)
            pixels[i] = Color.argb(12, v, v, v)
        }
        bmp.setPixels(pixels, 0, w, 0, 0, w, h)
        c.drawBitmap(bmp, 0f, 0f, null)
        bmp.recycle()
    }

    fun addFilmGrain(c: Canvas, w: Int, h: Int, intensity: Int = 5) {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(w * h)
        val rng = Random(System.nanoTime())
        for (i in pixels.indices) {
            val noise = rng.nextInt(-intensity, intensity + 1)
            val v = (128 + noise).coerceIn(0, 255)
            pixels[i] = Color.argb(8, v, v, v)
        }
        bmp.setPixels(pixels, 0, w, 0, 0, w, h)
        c.drawBitmap(bmp, 0f, 0f, null)
        bmp.recycle()
    }

    fun addAtmosphericHaze(c: Canvas, w: Int, h: Int, color: Int, intensity: Int = 6) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            Color.argb(intensity, Color.red(color), Color.green(color), Color.blue(color)),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
    }

    fun addSubtleVignette(c: Canvas, w: Int, h: Int, intensity: Int = 20) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = RadialGradient(
            w / 2f, h / 2f, maxOf(w, h) * 0.68f,
            intArrayOf(Color.TRANSPARENT, Color.argb(intensity, 0, 0, 0)),
            floatArrayOf(0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
    }

    fun addEdgeFade(c: Canvas, w: Int, h: Int, intensity: Int = 30) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        // Top fade
        paint.shader = LinearGradient(0f, 0f, 0f, h * 0.08f,
            Color.argb(intensity, 0, 0, 0), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w.toFloat(), h * 0.08f, paint)
        // Bottom fade
        paint.shader = LinearGradient(0f, h * 0.92f, 0f, h.toFloat(),
            Color.TRANSPARENT, Color.argb(intensity, 0, 0, 0), Shader.TileMode.CLAMP)
        c.drawRect(0f, h * 0.92f, w.toFloat(), h.toFloat(), paint)
        // Left fade
        paint.shader = LinearGradient(0f, 0f, w * 0.05f, 0f,
            Color.argb(intensity / 2, 0, 0, 0), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w * 0.05f, h.toFloat(), paint)
        // Right fade
        paint.shader = LinearGradient(w * 0.95f, 0f, w.toFloat(), 0f,
            Color.TRANSPARENT, Color.argb(intensity / 2, 0, 0, 0), Shader.TileMode.CLAMP)
        c.drawRect(w * 0.95f, 0f, w.toFloat(), h.toFloat(), paint)
    }

    fun addLuxuryPattern(c: Canvas, w: Int, h: Int, color: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.strokeWidth = 0.5f
        paint.color = Color.argb(6, Color.red(color), Color.green(color), Color.blue(color))
        val spacing = 32f
        var y = 0f
        while (y < h) {
            c.drawLine(0f, y, w.toFloat(), y, paint)
            y += spacing
        }
    }

    fun addMinimalDots(c: Canvas, w: Int, h: Int, color: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.argb(5, Color.red(color), Color.green(color), Color.blue(color))
        val spacing = 40f
        var y = 0f
        while (y < h) {
            var x = 0f
            while (x < w) {
                c.drawCircle(x, y, 1.2f, paint)
                x += spacing
            }
            y += spacing
        }
    }

    fun addOrganicWaves(c: Canvas, w: Int, h: Int, color: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.color = Color.argb(6, Color.red(color), Color.green(color), Color.blue(color))
        val path = Path()
        var y = h * 0.1f
        while (y < h) {
            path.reset()
            path.moveTo(0f, y)
            var x = 0f
            while (x < w) {
                val waveY = y + kotlin.math.sin((x / w * 4 * Math.PI).toFloat()) * 15f
                path.lineTo(x, waveY)
                x += 20f
            }
            c.drawPath(path, paint)
            y += 60f
        }
    }

    fun addBoldDiagonals(c: Canvas, w: Int, h: Int, color: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.strokeWidth = 1.5f
        paint.color = Color.argb(5, Color.red(color), Color.green(color), Color.blue(color))
        val spacing = 50f
        var offset = -h.toFloat()
        while (offset < w + h) {
            c.drawLine(offset, 0f, offset + h.toFloat(), h.toFloat(), paint)
            offset += spacing
        }
    }

    fun addSoftRadials(c: Canvas, w: Int, h: Int, color: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rng = Random(w.toLong())
        for (i in 0..4) {
            val x = rng.nextFloat() * w
            val y = rng.nextFloat() * h
            val r = 60f + rng.nextFloat() * 100f
            paint.shader = RadialGradient(x, y, r,
                intArrayOf(Color.argb(4, Color.red(color), Color.green(color), Color.blue(color)), Color.TRANSPARENT),
                floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
            c.drawCircle(x, y, r, paint)
        }
    }

    fun addGradientMesh(c: Canvas, w: Int, h: Int, palette: PosterPalette) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        // Top-left warm
        paint.shader = RadialGradient(w * 0.2f, h * 0.15f, w * 0.5f,
            intArrayOf(Color.argb(8, Color.red(palette.highlight), Color.green(palette.highlight), Color.blue(palette.highlight)), Color.TRANSPARENT),
            floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        // Bottom-right cool
        paint.shader = RadialGradient(w * 0.8f, h * 0.85f, w * 0.45f,
            intArrayOf(Color.argb(6, Color.red(palette.surface), Color.green(palette.surface), Color.blue(palette.surface)), Color.TRANSPARENT),
            floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
    }

    fun addColorAccentBorder(c: Canvas, w: Int, h: Int, color: Int, thickness: Int = 3) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = thickness.toFloat()
        paint.color = Color.argb(40, Color.red(color), Color.green(color), Color.blue(color))
        val inset = thickness.toFloat() + 4f
        c.drawRect(inset, inset, w - inset, h - inset, paint)
    }

    fun addHerringbonePattern(c: Canvas, w: Int, h: Int, color: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.strokeWidth = 0.8f
        paint.color = Color.argb(8, Color.red(color), Color.green(color), Color.blue(color))
        val size = 24f
        var y = 0f
        while (y < h) {
            var x = 0f
            while (x < w) {
                c.drawLine(x, y, x + size, y + size, paint)
                c.drawLine(x + size, y, x, y + size, paint)
                x += size * 2
            }
            y += size * 2
        }
    }

    fun addChevronPattern(c: Canvas, w: Int, h: Int, color: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.strokeWidth = 1f
        paint.color = Color.argb(7, Color.red(color), Color.green(color), Color.blue(color))
        val spacing = 40f
        var y = 0f
        while (y < h) {
            val path = Path()
            path.moveTo(0f, y)
            var x = 0f
            while (x < w) {
                path.lineTo(x + spacing / 2, y - spacing / 3)
                path.lineTo(x + spacing, y)
                x += spacing
            }
            c.drawPath(path, paint)
            y += spacing
        }
    }

    fun addCrossHatchPattern(c: Canvas, w: Int, h: Int, color: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.strokeWidth = 0.5f
        paint.color = Color.argb(6, Color.red(color), Color.green(color), Color.blue(color))
        val spacing = 20f
        var y = 0f
        while (y < h) {
            c.drawLine(0f, y, w.toFloat(), y, paint)
            y += spacing
        }
        var x = 0f
        while (x < w) {
            c.drawLine(x, 0f, x, h.toFloat(), paint)
            x += spacing
        }
    }

    fun addLinenTexture(c: Canvas, w: Int, h: Int, color: Int) {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(w * h)
        val rng = Random(123)
        for (y in 0 until h) {
            for (x in 0 until w) {
                val noise = rng.nextInt(-4, 5)
                val pattern = if (y % 3 == 0) 2 else 0
                val v = (128 + noise + pattern).coerceIn(0, 255)
                pixels[y * w + x] = Color.argb(6, v, v, v)
            }
        }
        bmp.setPixels(pixels, 0, w, 0, 0, w, h)
        c.drawBitmap(bmp, 0f, 0f, null)
        bmp.recycle()
    }

    fun addNoiseTexture(c: Canvas, w: Int, h: Int, intensity: Int = 3) {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(w * h)
        val rng = Random(System.nanoTime())
        for (i in pixels.indices) {
            val noise = rng.nextInt(-intensity, intensity + 1)
            val v = (128 + noise).coerceIn(0, 255)
            pixels[i] = Color.argb(5, v, v, v)
        }
        bmp.setPixels(pixels, 0, w, 0, 0, w, h)
        c.drawBitmap(bmp, 0f, 0f, null)
        bmp.recycle()
    }

    fun addSubtleGrid(c: Canvas, w: Int, h: Int, color: Int, spacing: Int = 50) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.strokeWidth = 0.3f
        paint.color = Color.argb(4, Color.red(color), Color.green(color), Color.blue(color))
        var y = 0f
        while (y < h) {
            c.drawLine(0f, y, w.toFloat(), y, paint)
            y += spacing.toFloat()
        }
        var x = 0f
        while (x < w) {
            c.drawLine(x, 0f, x, h.toFloat(), paint)
            x += spacing.toFloat()
        }
    }

    fun addConcentricCircles(c: Canvas, w: Int, h: Int, color: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.5f
        paint.color = Color.argb(5, Color.red(color), Color.green(color), Color.blue(color))
        val cx = w * 0.5f
        val cy = h * 0.45f
        var r = 50f
        while (r < maxOf(w, h) * 0.8f) {
            c.drawCircle(cx, cy, r, paint)
            r += 35f
        }
    }

    fun addRadialBurst(c: Canvas, w: Int, h: Int, color: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.strokeWidth = 0.5f
        paint.color = Color.argb(5, Color.red(color), Color.green(color), Color.blue(color))
        val cx = w * 0.5f
        val cy = h * 0.45f
        val rays = 24
        val maxLen = maxOf(w, h) * 0.7f
        for (i in 0 until rays) {
            val angle = (i.toFloat() / rays) * 360f
            val rad = Math.toRadians(angle.toDouble())
            val endX = cx + (maxLen * Math.cos(rad)).toFloat()
            val endY = cy + (maxLen * Math.sin(rad)).toFloat()
            c.drawLine(cx, cy, endX, endY, paint)
        }
    }

    fun addDiamondPattern(c: Canvas, w: Int, h: Int, color: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.6f
        paint.color = Color.argb(6, Color.red(color), Color.green(color), Color.blue(color))
        val size = 40f
        var y = 0f
        while (y < h + size) {
            var x = 0f
            while (x < w + size) {
                val path = Path()
                path.moveTo(x, y - size / 2)
                path.lineTo(x + size / 2, y)
                path.lineTo(x, y + size / 2)
                path.lineTo(x - size / 2, y)
                path.close()
                c.drawPath(path, paint)
                x += size
            }
            y += size
        }
    }
}
