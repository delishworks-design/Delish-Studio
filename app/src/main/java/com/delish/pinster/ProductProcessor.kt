package com.delish.pinster

import android.graphics.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

object ProductProcessor {

    enum class SegmentationMode { STANDARD, FINE_DETAIL, TRANSLUCENT, COMPLEX, BLACK_PRODUCT }

    data class ProductAnalysis(
        val cutout: Bitmap,
        val silhouette: Bitmap,
        val dominantColors: List<Int>,
        val textColor: Int,
        val busyRegions: List<RectF>,
        val safeTextZones: List<RectF>,
        val mode: SegmentationMode,
        val contrastScore: Float,
        val textOnProductSafe: Boolean,
        val productBounds: RectF,
        val edgeSharpness: Float,
        val dominantHue: Float,
        val dominantSaturation: Float
    )

    fun analyzeAndProcess(product: Bitmap, canvasW: Int, canvasH: Int): ProductAnalysis {
        val mode = detectSegmentationMode(product)
        val cutout = removeBackground(product, mode)
        val refinedCutout = refineEdges(cutout)
        val silhouette = createSilhouette(refinedCutout, mode)
        val colors = extractDominantColors(product, 5)
        val textColor = chooseTextColorAdvanced(colors, mode)
        val busy = detectBusyRegions(product)
        val safe = findSafeTextZones(busy, canvasW, canvasH)
        val contrast = calculateContrastScore(product, textColor)
        val textSafe = checkTextOnProduct(product, textColor)
        val bounds = findProductBounds(refinedCutout)
        val sharpness = calculateEdgeSharpness(product)
        val (hue, sat) = extractDominantHueSat(product)
        return ProductAnalysis(refinedCutout, silhouette, colors, textColor, busy, safe, mode, contrast, textSafe, bounds, sharpness, hue, sat)
    }

    private fun detectSegmentationMode(src: Bitmap): SegmentationMode {
        val w = src.width; val h = src.height
        var edgeEntropy = 0
        var translucentCount = 0
        var darkPixelCount = 0
        val blockSize = 10

        for (by in 0 until h step blockSize) {
            for (bx in 0 until w step blockSize) {
                val endX = min(bx + blockSize, w)
                val endY = min(by + blockSize, h)
                var localEdges = 0
                for (y in by until endY - 1) {
                    for (x in bx until endX - 1) {
                        val p = src.getPixel(x, y)
                        val a1 = Color.alpha(p)
                        val brightness = (Color.red(p) + Color.green(p) + Color.blue(p)) / 3
                        val a2 = Color.alpha(src.getPixel(x + 1, y))
                        val a3 = Color.alpha(src.getPixel(x, y + 1))
                        if (abs(a1 - a2) > 30 || abs(a1 - a3) > 30) localEdges++
                        if (a1 in 50..200) translucentCount++
                        if (brightness < 50) darkPixelCount++
                    }
                }
                if (localEdges > blockSize * 2) edgeEntropy++
            }
        }

        val totalBlocks = (w / blockSize) * (h / blockSize)
        val translucentRatio = translucentCount.toFloat() / (totalBlocks * blockSize * blockSize)
        val darkRatio = darkPixelCount.toFloat() / (totalBlocks * blockSize * blockSize)

        return when {
            darkRatio > 0.6f -> SegmentationMode.BLACK_PRODUCT
            translucentRatio > 0.3f -> SegmentationMode.TRANSLUCENT
            edgeEntropy > totalBlocks * 0.4f -> SegmentationMode.COMPLEX
            edgeEntropy > totalBlocks * 0.2f -> SegmentationMode.FINE_DETAIL
            else -> SegmentationMode.STANDARD
        }
    }

    fun removeBackground(src: Bitmap, mode: SegmentationMode = SegmentationMode.STANDARD): Bitmap {
        val w = src.width; val h = src.height
        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)

        val bgColor = detectBackgroundColor(pixels, w, h)
        val bgR = Color.red(bgColor); val bgG = Color.green(bgColor); val bgB = Color.blue(bgColor)

        val (core, feather) = when (mode) {
            SegmentationMode.STANDARD -> Pair(55, 85)
            SegmentationMode.FINE_DETAIL -> Pair(35, 120)
            SegmentationMode.TRANSLUCENT -> Pair(45, 100)
            SegmentationMode.COMPLEX -> Pair(50, 90)
            SegmentationMode.BLACK_PRODUCT -> Pair(30, 140)
        }

        val contrastBoost = if (mode == SegmentationMode.BLACK_PRODUCT) 1.25f else 1.0f

        for (i in pixels.indices) {
            val p = pixels[i]
            val r = Color.red(p); val g = Color.green(p); val b = Color.blue(p)
            val dist = sqrt(abs(r - bgR).toDouble().pow(2) + abs(g - bgG).toDouble().pow(2) + abs(b - bgB).toDouble().pow(2)) * contrastBoost

            when {
                dist < core -> pixels[i] = Color.argb(0, 0, 0, 0)
                dist < core + feather -> {
                    val alpha = ((dist - core) / feather.toDouble() * 255).coerceIn(0.0, 255.0).toInt()
                    val preservedAlpha = if (mode == SegmentationMode.TRANSLUCENT) {
                        max(alpha, Color.alpha(p) / 2)
                    } else alpha
                    pixels[i] = Color.argb(preservedAlpha, r, g, b)
                }
            }
        }
        result.setPixels(pixels, 0, w, 0, 0, w, h)
        return result
    }

    private fun refineEdges(src: Bitmap): Bitmap {
        val w = src.width; val h = src.height
        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val srcPixels = IntArray(w * h)
        src.getPixels(srcPixels, 0, w, 0, 0, w, h)
        val dstPixels = IntArray(w * h)

        for (y in 0 until h) {
            for (x in 0 until w) {
                val idx = y * w + x
                val a = Color.alpha(srcPixels[idx])

                if (a in 10..245) {
                    var maxAlpha = 0
                    for (dy in -1..1) {
                        for (dx in -1..1) {
                            if (dx == 0 && dy == 0) continue
                            val nx = x + dx; val ny = y + dy
                            if (nx in 0 until w && ny in 0 until h) {
                                maxAlpha = max(maxAlpha, Color.alpha(srcPixels[ny * w + nx]))
                            }
                        }
                    }
                    val refined = (a * 0.6f + maxAlpha * 0.4f).toInt().coerceIn(0, 255)
                    dstPixels[idx] = Color.argb(refined, Color.red(srcPixels[idx]), Color.green(srcPixels[idx]), Color.blue(srcPixels[idx]))
                } else {
                    dstPixels[idx] = srcPixels[idx]
                }
            }
        }
        result.setPixels(dstPixels, 0, w, 0, 0, w, h)
        return result
    }

    private fun detectBackgroundColor(pixels: IntArray, w: Int, h: Int): Int {
        val edgePixels = mutableListOf<Int>()
        for (x in 0 until w) { edgePixels.add(pixels[x]); edgePixels.add(pixels[(h - 1) * w + x]) }
        for (y in 0 until h) { edgePixels.add(pixels[y * w]); edgePixels.add(pixels[y * w + w - 1]) }

        var rSum = 0L; var gSum = 0L; var bSum = 0L
        for (p in edgePixels) { rSum += Color.red(p); gSum += Color.green(p); bSum += Color.blue(p) }
        val n = edgePixels.size
        return Color.rgb((rSum / n).toInt(), (gSum / n).toInt(), (bSum / n).toInt())
    }

    fun createSilhouette(cutout: Bitmap, mode: SegmentationMode = SegmentationMode.STANDARD): Bitmap {
        val w = cutout.width; val h = cutout.height
        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(w * h)
        cutout.getPixels(pixels, 0, w, 0, 0, w, h)

        val edgeBlur = when (mode) {
            SegmentationMode.FINE_DETAIL -> 40
            SegmentationMode.TRANSLUCENT -> 50
            SegmentationMode.COMPLEX -> 45
            SegmentationMode.BLACK_PRODUCT -> 55
            SegmentationMode.STANDARD -> 30
        }

        for (i in pixels.indices) {
            val a = Color.alpha(pixels[i])
            pixels[i] = if (a > edgeBlur) Color.argb(180, 0, 0, 0) else Color.TRANSPARENT
        }
        result.setPixels(pixels, 0, w, 0, 0, w, h)
        return result
    }

    fun extractDominantColors(src: Bitmap, count: Int): List<Int> {
        val scaled = Bitmap.createScaledBitmap(src, 60, 60, true)
        val pixels = IntArray(60 * 60)
        scaled.getPixels(pixels, 0, 60, 0, 0, 60, 60)
        scaled.recycle()

        val colorBuckets = mutableMapOf<Int, Int>()
        for (p in pixels) {
            if (Color.alpha(p) < 50) continue
            val r = (Color.red(p) / 32) * 32
            val g = (Color.green(p) / 32) * 32
            val b = (Color.blue(p) / 32) * 32
            val bucket = Color.rgb(r, g, b)
            colorBuckets[bucket] = (colorBuckets[bucket] ?: 0) + 1
        }

        return colorBuckets.entries.sortedByDescending { it.value }
            .take(count).map { it.key }
    }

    fun chooseTextColor(colors: List<Int>): Int {
        return chooseTextColorAdvanced(colors, SegmentationMode.STANDARD)
    }

    private fun chooseTextColorAdvanced(colors: List<Int>, mode: SegmentationMode): Int {
        if (colors.isEmpty()) return Color.BLACK
        var totalLum = 0.0
        var totalSat = 0.0
        var totalWarm = 0.0
        for (c in colors) {
            totalLum += luminance(c)
            totalSat += saturation(c)
            totalWarm += warmth(c)
        }
        val avgLum = totalLum / colors.size
        val avgSat = totalSat / colors.size
        val avgWarm = totalWarm / colors.size
        val isWarm = avgWarm > 0.5f

        return when {
            avgLum > 0.5f && isWarm -> Color.rgb(30, 25, 20)
            avgLum > 0.5f -> Color.rgb(30, 30, 30)
            avgLum < 0.3f && isWarm -> Color.rgb(255, 250, 240)
            avgLum < 0.3f -> Color.rgb(245, 245, 245)
            avgSat > 0.5f -> Color.rgb(50, 50, 55)
            else -> Color.rgb(35, 35, 40)
        }
    }

    fun luminance(color: Int): Double {
        val r = Color.red(color) / 255.0
        val g = Color.green(color) / 255.0
        val b = Color.blue(color) / 255.0
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun saturation(color: Int): Double {
        val r = Color.red(color) / 255.0
        val g = Color.green(color) / 255.0
        val b = Color.blue(color) / 255.0
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        return if (max == 0.0) 0.0 else (max - min) / max
    }

    private fun warmth(color: Int): Float {
        val r = Color.red(color).toFloat()
        val g = Color.green(color).toFloat()
        val b = Color.blue(color).toFloat()
        return (r - b) / (r + g + b + 1f) * 0.5f + 0.5f
    }

    fun detectBusyRegions(src: Bitmap): List<RectF> {
        val w = src.width; val h = src.height
        val regions = mutableListOf<RectF>()
        val blockSize = 20

        for (by in 0 until h step blockSize) {
            for (bx in 0 until w step blockSize) {
                var edgeCount = 0
                val endX = min(bx + blockSize, w)
                val endY = min(by + blockSize, h)
                for (y in by until endY - 1) {
                    for (x in bx until endX - 1) {
                        val p1 = src.getPixel(x, y)
                        val p2 = src.getPixel(x + 1, y)
                        val p3 = src.getPixel(x, y + 1)
                        if (colorDiff(p1, p2) > 40 || colorDiff(p1, p3) > 40) edgeCount++
                    }
                }
                if (edgeCount > blockSize * 2) {
                    regions.add(RectF(bx.toFloat(), by.toFloat(), endX.toFloat(), endY.toFloat()))
                }
            }
        }
        return regions
    }

    private fun colorDiff(c1: Int, c2: Int): Int {
        return abs(Color.red(c1) - Color.red(c2)) +
                abs(Color.green(c1) - Color.green(c2)) +
                abs(Color.blue(c1) - Color.blue(c2))
    }

    fun findSafeTextZones(busyRegions: List<RectF>, canvasW: Int, canvasH: Int): List<RectF> {
        val zones = mutableListOf<RectF>()
        val candidates = listOf(
            RectF(canvasW * 0.08f, canvasH * 0.04f, canvasW * 0.92f, canvasH * 0.16f),
            RectF(canvasW * 0.08f, canvasH * 0.80f, canvasW * 0.92f, canvasH * 0.96f),
            RectF(canvasW * 0.05f, canvasH * 0.30f, canvasW * 0.30f, canvasH * 0.70f),
            RectF(canvasW * 0.70f, canvasH * 0.30f, canvasW * 0.95f, canvasH * 0.70f),
            RectF(canvasW * 0.35f, canvasH * 0.42f, canvasW * 0.65f, canvasH * 0.58f),
            RectF(canvasW * 0.05f, canvasH * 0.05f, canvasW * 0.45f, canvasH * 0.25f),
            RectF(canvasW * 0.55f, canvasH * 0.05f, canvasW * 0.95f, canvasH * 0.25f),
            RectF(canvasW * 0.05f, canvasH * 0.75f, canvasW * 0.45f, canvasH * 0.95f)
        )

        for (zone in candidates) {
            var busyOverlap = 0f
            val zoneArea = zone.width() * zone.height()
            for (busy in busyRegions) {
                val intersection = RectF()
                if (intersection.setIntersect(zone, busy)) {
                    busyOverlap += intersection.width() * intersection.height()
                }
            }
            if (busyOverlap / zoneArea < 0.08f) {
                zones.add(zone)
            }
        }
        return zones
    }

    private fun calculateContrastScore(src: Bitmap, textColor: Int): Float {
        val textLum = luminance(textColor)
        var totalContrast = 0f
        var count = 0
        val sampleSize = 20
        val scaled = Bitmap.createScaledBitmap(src, sampleSize, sampleSize, true)
        for (y in 0 until sampleSize) {
            for (x in 0 until sampleSize) {
                val bgLum = luminance(scaled.getPixel(x, y))
                val contrast = if (textLum > bgLum) {
                    (textLum + 0.05) / (bgLum + 0.05)
                } else {
                    (bgLum + 0.05) / (textLum + 0.05)
                }
                totalContrast += contrast.toFloat()
                count++
            }
        }
        scaled.recycle()
        return if (count > 0) totalContrast / count else 0f
    }

    private fun checkTextOnProduct(src: Bitmap, textColor: Int): Boolean {
        val w = src.width; val h = src.height
        val productCenter = luminance(src.getPixel(w / 2, h / 2))
        val textLum = luminance(textColor)
        val contrast = if (textLum > productCenter) {
            (textLum + 0.05) / (productCenter + 0.05)
        } else {
            (productCenter + 0.05) / (textLum + 0.05)
        }
        return contrast >= 4.5
    }

    private fun findProductBounds(cutout: Bitmap): RectF {
        val w = cutout.width; val h = cutout.height
        val pixels = IntArray(w * h)
        cutout.getPixels(pixels, 0, w, 0, 0, w, h)
        var minX = w; var minY = h; var maxX = 0; var maxY = 0
        var found = false
        for (y in 0 until h) {
            for (x in 0 until w) {
                if (Color.alpha(pixels[y * w + x]) > 30) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                    found = true
                }
            }
        }
        return if (found) RectF(minX.toFloat(), minY.toFloat(), maxX.toFloat(), maxY.toFloat()) else RectF(0f, 0f, w.toFloat(), h.toFloat())
    }

    private fun calculateEdgeSharpness(src: Bitmap): Float {
        val scaled = Bitmap.createScaledBitmap(src, 100, 100, true)
        val pixels = IntArray(100 * 100)
        scaled.getPixels(pixels, 0, 100, 0, 0, 100, 100)
        scaled.recycle()
        var edgeSum = 0f
        var count = 0
        for (y in 0 until 99) {
            for (x in 0 until 99) {
                val p1 = pixels[y * 100 + x]
                val p2 = pixels[y * 100 + x + 1]
                val p3 = pixels[(y + 1) * 100 + x]
                val diff = colorDiff(p1, p2) + colorDiff(p1, p3)
                edgeSum += diff.toFloat()
                count++
            }
        }
        return if (count > 0) (edgeSum / count / 255f * 10f).coerceIn(0f, 1f) else 0.5f
    }

    private fun extractDominantHueSat(src: Bitmap): Pair<Float, Float> {
        val scaled = Bitmap.createScaledBitmap(src, 30, 30, true)
        val pixels = IntArray(30 * 30)
        scaled.getPixels(pixels, 0, 30, 0, 0, 30, 30)
        scaled.recycle()
        var totalHue = 0f; var totalSat = 0f; var count = 0
        for (p in pixels) {
            if (Color.alpha(p) < 50) continue
            val r = Color.red(p) / 255f
            val g = Color.green(p) / 255f
            val b = Color.blue(p) / 255f
            val max = max(r, max(g, b))
            val min = min(r, min(g, b))
            val sat = if (max == 0f) 0f else (max - min) / max
            val hue = when {
                max == min -> 0f
                max == r -> 60f * ((g - b) / (max - min))
                max == g -> 60f * (2f + (b - r) / (max - min))
                else -> 60f * (4f + (r - g) / (max - min))
            }
            totalHue += (hue + 360f) % 360f
            totalSat += sat
            count++
        }
        return if (count > 0) Pair(totalHue / count, totalSat / count) else Pair(0f, 0.3f)
    }
}
