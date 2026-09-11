package com.delish.pinster

import android.graphics.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class ProductFingerprint(
    val width: Int,
    val height: Int,
    val aspectRatio: Float,
    val alphaBounds: Rect,
    val productArea: Float,
    val canvasFillRatio: Float,
    val centroidX: Float,
    val centroidY: Float,
    val opticalCenterX: Float,
    val opticalCenterY: Float,
    val dominantColors: List<Int>,
    val averageBrightness: Float,
    val averageSaturation: Float,
    val hasAlpha: Boolean,
    val edgeDensity: Float,
    val silhouetteComplexity: Float,
    val visualMass: Float,
    val negativeSpaceTop: Float,
    val negativeSpaceBottom: Float,
    val negativeSpaceLeft: Float,
    val negativeSpaceRight: Float,
    val isDarkProduct: Boolean,
    val isWarmTone: Boolean,
    val isCoolTone: Boolean,
    val productCategory: ProductCategory
)

enum class ProductCategory {
    UNKNOWN, KITCHEN, BEDROOM, BATHROOM, STORAGE, DECOR, OFFICE, ELECTRONICS, FASHION, FOOD, OUTDOOR, BEAUTY
}

object ProductAnalyzer {

    private const val SAMPLE_SIZE = 64

    fun analyze(bmp: Bitmap): ProductFingerprint {
        val w = bmp.width
        val h = bmp.height
        val aspect = w.toFloat() / h

        val scaled = Bitmap.createScaledBitmap(bmp, SAMPLE_SIZE, SAMPLE_SIZE, true)
        val pixels = IntArray(SAMPLE_SIZE * SAMPLE_SIZE)
        scaled.getPixels(pixels, 0, SAMPLE_SIZE, 0, 0, SAMPLE_SIZE, SAMPLE_SIZE)
        scaled.recycle()

        var minX = SAMPLE_SIZE; var maxX = 0; var minY = SAMPLE_SIZE; var maxY = 0
        var sumR = 0L; var sumG = 0L; var sumB = 0L; var sumA = 0L
        var totalBrightness = 0f; var totalSaturation = 0f
        var nonTransparent = 0
        var weightedCX = 0f; var weightedCY = 0f
        var totalWeight = 0f
        val colorBuckets = IntArray(64)

        for (y in 0 until SAMPLE_SIZE) {
            for (x in 0 until SAMPLE_SIZE) {
                val p = pixels[y * SAMPLE_SIZE + x]
                val a = Color.alpha(p)
                if (a < 30) continue

                nonTransparent++
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < minY) minY = y
                if (y > maxY) maxY = y

                val r = Color.red(p); val g = Color.green(p); val b = Color.blue(p)
                sumR += r; sumG += g; sumB += b; sumA += a

                val brightness = (r * 0.299f + g * 0.587f + b * 0.114f) / 255f
                totalBrightness += brightness

                val maxC = maxOf(r, g, b).toFloat()
                val minC = minOf(r, g, b).toFloat()
                val sat = if (maxC > 0) (maxC - minC) / maxC else 0f
                totalSaturation += sat

                val weight = a / 255f
                weightedCX += x * weight
                weightedCY += y * weight
                totalWeight += weight

                val bucket = ((r + g + b) / 3 * 63 / 255).coerceIn(0, 63)
                colorBuckets[bucket] += a
            }
        }

        val hasAlpha = nonTransparent < SAMPLE_SIZE * SAMPLE_SIZE * 0.95
        val areaRatio = nonTransparent.toFloat() / (SAMPLE_SIZE * SAMPLE_SIZE)

        val cx = if (totalWeight > 0) weightedCX / totalWeight else SAMPLE_SIZE / 2f
        val cy = if (totalWeight > 0) weightedCY / totalWeight else SAMPLE_SIZE / 2f

        val avgR = (sumR / max(nonTransparent, 1)).toInt()
        val avgG = (sumG / max(nonTransparent, 1)).toInt()
        val avgB = (sumB / max(nonTransparent, 1)).toInt()

        val dominant = extractDominantColors(pixels, SAMPLE_SIZE)

        val avgBrightness = if (nonTransparent > 0) totalBrightness / nonTransparent else 0.5f
        val avgSaturation = if (nonTransparent > 0) totalSaturation / nonTransparent else 0f

        val edgeCount = countEdges(pixels, SAMPLE_SIZE)
        val edgeDensity = edgeCount.toFloat() / (nonTransparent.coerceAtLeast(1))

        val avgColor = Color.rgb(avgR, avgG, avgB)
        val hue = FloatArray(3)
        Color.colorToHSV(avgColor, hue)

        val negTop = minY.toFloat() / SAMPLE_SIZE
        val negBot = (SAMPLE_SIZE - 1 - maxY).toFloat() / SAMPLE_SIZE
        val negLeft = minX.toFloat() / SAMPLE_SIZE
        val negRight = (SAMPLE_SIZE - 1 - maxX).toFloat() / SAMPLE_SIZE

        val visualMass = areaRatio * avgBrightness

        val category = guessCategory(dominant, avgBrightness, avgSaturation, aspect)

        return ProductFingerprint(
            width = w, height = h, aspectRatio = aspect,
            alphaBounds = Rect(minX * w / SAMPLE_SIZE, minY * h / SAMPLE_SIZE, maxX * w / SAMPLE_SIZE, maxY * h / SAMPLE_SIZE),
            productArea = areaRatio, canvasFillRatio = areaRatio,
            centroidX = cx / SAMPLE_SIZE, centroidY = cy / SAMPLE_SIZE,
            opticalCenterX = cx / SAMPLE_SIZE, opticalCenterY = (cy - SAMPLE_SIZE * 0.05f) / SAMPLE_SIZE,
            dominantColors = dominant,
            averageBrightness = avgBrightness, averageSaturation = avgSaturation,
            hasAlpha = hasAlpha, edgeDensity = edgeDensity,
            silhouetteComplexity = edgeDensity * areaRatio,
            visualMass = visualMass,
            negativeSpaceTop = negTop, negativeSpaceBottom = negBot,
            negativeSpaceLeft = negLeft, negativeSpaceRight = negRight,
            isDarkProduct = avgBrightness < 0.4f,
            isWarmTone = hue[0] in 0f..60f || hue[0] >= 300f,
            isCoolTone = hue[0] in 150f..270f,
            productCategory = category
        )
    }

    private fun extractDominantColors(pixels: IntArray, size: Int): List<Int> {
        val colorMap = mutableMapOf<Int, Int>()
        for (p in pixels) {
            if (Color.alpha(p) < 30) continue
            val r = Color.red(p) / 32 * 32
            val g = Color.green(p) / 32 * 32
            val b = Color.blue(p) / 32 * 32
            val quantized = Color.rgb(r, g, b)
            colorMap[quantized] = (colorMap[quantized] ?: 0) + 1
        }
        return colorMap.entries.sortedByDescending { it.value }.take(5).map { it.key }
    }

    private fun countEdges(pixels: IntArray, size: Int): Int {
        var count = 0
        for (y in 1 until size - 1) {
            for (x in 1 until size - 1) {
                val a = Color.alpha(pixels[y * size + x])
                val an = Color.alpha(pixels[(y - 1) * size + x])
                val as2 = Color.alpha(pixels[(y + 1) * size + x])
                val aw = Color.alpha(pixels[y * size + x - 1])
                val ae = Color.alpha(pixels[y * size + x + 1])
                if (abs(a - an) > 50 || abs(a - as2) > 50 || abs(a - aw) > 50 || abs(a - ae) > 50) {
                    count++
                }
            }
        }
        return count
    }

    private fun guessCategory(colors: List<Int>, brightness: Float, saturation: Float, aspect: Float): ProductCategory {
        if (colors.isEmpty()) return ProductCategory.UNKNOWN
        val c = colors[0]
        val r = Color.red(c); val g = Color.green(c); val b = Color.blue(c)
        val hue = FloatArray(3)
        Color.colorToHSV(c, hue)

        return when {
            hue[0] in 80f..160f && saturation > 0.2f -> ProductCategory.OUTDOOR
            hue[0] in 0f..40f && saturation > 0.3f -> ProductCategory.FOOD
            brightness > 0.85f && saturation < 0.1f -> ProductCategory.BATHROOM
            brightness < 0.3f -> ProductCategory.ELECTRONICS
            saturation < 0.15f && brightness in 0.5f..0.8f -> ProductCategory.STORAGE
            hue[0] in 20f..50f && saturation in 0.1f..0.4f -> ProductCategory.KITCHEN
            else -> ProductCategory.DECOR
        }
    }
}
