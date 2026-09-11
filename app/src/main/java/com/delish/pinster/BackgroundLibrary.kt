package com.delish.pinster

import android.content.Context
import android.graphics.*
import java.io.InputStream

object BackgroundLibrary {

    private var context: Context? = null
    private var cachedBackgrounds = mutableMapOf<String, Bitmap>()
    private var initialized = false

    data class BackgroundCandidate(
        val metadata: BackgroundMetadata,
        val bitmap: Bitmap,
        val score: Float
    )

    fun initialize(ctx: Context) {
        if (initialized) return
        context = ctx.applicationContext
        initialized = true
    }

    fun getBackgroundsForCategory(category: BackgroundCategory): List<BackgroundMetadata> {
        return allMetadata.filter { it.category == category }
    }

    fun getBackgroundById(id: String): BackgroundMetadata? {
        return allMetadata.find { it.id == id }
    }

    fun renderBackground(metadata: BackgroundMetadata, width: Int, height: Int): Bitmap {
        val cacheKey = "${metadata.id}_${width}_${height}"
        cachedBackgrounds[cacheKey]?.let { return it }

        val assetPath = "backgrounds/${metadata.category.name.lowercase()}/${metadata.id}.jpg"
        val bmp = loadFromAssets(assetPath, width, height)

        if (bmp != null) {
            cachedBackgrounds[cacheKey] = bmp
            return bmp
        }

        // Fallback to generated background if asset not found
        val fallback = generateFallback(metadata, width, height)
        cachedBackgrounds[cacheKey] = fallback
        return fallback
    }

    private fun loadFromAssets(path: String, targetW: Int, targetH: Int): Bitmap? {
        return try {
            val ctx = context ?: return null
            val inputStream: InputStream = ctx.assets.open(path)
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            val sampleSize = calculateSampleSize(options.outWidth, options.outHeight, targetW * 2, targetH * 2)
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val inputStream2 = ctx.assets.open(path)
            val bitmap = BitmapFactory.decodeStream(inputStream2, null, decodeOptions)
            inputStream2.close()

            bitmap?.let { cropToCenter(it, targetW, targetH) }
        } catch (e: Exception) {
            null
        }
    }

    private fun calculateSampleSize(srcW: Int, srcH: Int, reqW: Int, reqH: Int): Int {
        var sampleSize = 1
        while (srcW / sampleSize > reqW || srcH / sampleSize > reqH) {
            sampleSize *= 2
        }
        return sampleSize
    }

    private fun cropToCenter(src: Bitmap, targetW: Int, targetH: Int): Bitmap {
        val srcRatio = src.width.toFloat() / src.height
        val targetRatio = targetW.toFloat() / targetH

        val cropW: Int
        val cropH: Int
        val offsetX: Int
        val offsetY: Int

        if (srcRatio > targetRatio) {
            cropH = src.height
            cropW = (cropH * targetRatio).toInt()
            offsetX = (src.width - cropW) / 2
            offsetY = 0
        } else {
            cropW = src.width
            cropH = (cropW / targetRatio).toInt()
            offsetX = 0
            offsetY = (src.height - cropH) / 2
        }

        val cropped = Bitmap.createBitmap(src, offsetX, offsetY, cropW, cropH)
        if (cropped !== src) src.recycle()
        return Bitmap.createScaledBitmap(cropped, targetW, targetH, true)
    }

    private fun generateFallback(metadata: BackgroundMetadata, width: Int, height: Int): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val dominant = metadata.dominantColors.firstOrNull() ?: Color.rgb(240, 238, 235)

        c.drawColor(dominant)

        val lightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        lightPaint.shader = LinearGradient(
            0f, 0f, width * 0.5f, height * 0.3f,
            Color.argb((15 * metadata.brightness).toInt(), 255, 255, 255),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), lightPaint)

        return bmp
    }

    fun recycleCache() {
        cachedBackgrounds.values.forEach { if (!it.isRecycled) it.recycle() }
        cachedBackgrounds.clear()
    }

    // ─── METADATA DATABASE ───────────────────────

    val allMetadata = listOf(
        // KITCHEN
        BackgroundMetadata(
            id = "modern_marble_kitchen_01",
            category = BackgroundCategory.KITCHEN,
            surfaces = listOf(SurfaceType.MARBLE, SurfaceType.COUNTERTOP),
            style = listOf(StyleTag.MODERN, StyleTag.MINIMAL, StyleTag.BRIGHT),
            lighting = LightingDirection.NATURAL_LEFT,
            dominantColors = listOf(Color.rgb(245, 240, 235), Color.rgb(235, 230, 222)),
            productPlacements = listOf(ProductPlacement.CENTER, ProductPlacement.LOWER_CENTER, ProductPlacement.LEFT_HERO),
            usableTextRegions = listOf(TextRegion.UPPER_CENTER, TextRegion.TOP_LEFT, TextRegion.TOP_RIGHT),
            brightness = 0.85f, contrast = 0.40f, warmth = 1.02f, priority = 90
        ),
        BackgroundMetadata(
            id = "warm_wood_kitchen_01",
            category = BackgroundCategory.KITCHEN,
            surfaces = listOf(SurfaceType.WOOD, SurfaceType.COUNTERTOP),
            style = listOf(StyleTag.WARM, StyleTag.NATURAL, StyleTag.COZY),
            lighting = LightingDirection.WARM_AMBIENT,
            dominantColors = listOf(Color.rgb(210, 185, 155), Color.rgb(225, 205, 178)),
            productPlacements = listOf(ProductPlacement.CENTER, ProductPlacement.LOWER_CENTER),
            usableTextRegions = listOf(TextRegion.UPPER_CENTER, TextRegion.FULL_TOP),
            brightness = 0.78f, contrast = 0.38f, warmth = 1.06f, priority = 85
        ),
        BackgroundMetadata(
            id = "bright_white_kitchen_01",
            category = BackgroundCategory.KITCHEN,
            surfaces = listOf(SurfaceType.TILE, SurfaceType.COUNTERTOP),
            style = listOf(StyleTag.BRIGHT, StyleTag.MODERN, StyleTag.AIRY),
            lighting = LightingDirection.COOL_BRIGHT,
            dominantColors = listOf(Color.rgb(248, 250, 252), Color.rgb(240, 242, 245)),
            productPlacements = listOf(ProductPlacement.CENTER, ProductPlacement.LOWER_CENTER, ProductPlacement.RIGHT_HERO),
            usableTextRegions = listOf(TextRegion.UPPER_CENTER, TextRegion.TOP_LEFT, TextRegion.FULL_TOP),
            brightness = 0.92f, contrast = 0.35f, warmth = 0.98f, priority = 88
        ),

        // LIVING ROOM
        BackgroundMetadata(
            id = "modern_living_room_01",
            category = BackgroundCategory.LIVING_ROOM,
            surfaces = listOf(SurfaceType.WOOD, SurfaceType.TABLE),
            style = listOf(StyleTag.MODERN, StyleTag.SCANDINAVIAN, StyleTag.BRIGHT),
            lighting = LightingDirection.NATURAL_LEFT,
            dominantColors = listOf(Color.rgb(235, 228, 218), Color.rgb(225, 218, 205)),
            productPlacements = listOf(ProductPlacement.CENTER, ProductPlacement.LOWER_CENTER),
            usableTextRegions = listOf(TextRegion.UPPER_CENTER, TextRegion.TOP_LEFT, TextRegion.TOP_RIGHT),
            brightness = 0.82f, contrast = 0.42f, warmth = 1.03f, priority = 90
        ),
        BackgroundMetadata(
            id = "cozy_living_room_01",
            category = BackgroundCategory.LIVING_ROOM,
            surfaces = listOf(SurfaceType.WOOD, SurfaceType.FABRIC),
            style = listOf(StyleTag.COZY, StyleTag.WARM, StyleTag.BOHEMIAN),
            lighting = LightingDirection.WARM_AMBIENT,
            dominantColors = listOf(Color.rgb(215, 198, 175), Color.rgb(225, 210, 188)),
            productPlacements = listOf(ProductPlacement.CENTER, ProductPlacement.LOWER_CENTER),
            usableTextRegions = listOf(TextRegion.UPPER_CENTER, TextRegion.FULL_TOP),
            brightness = 0.75f, contrast = 0.40f, warmth = 1.08f, priority = 85
        ),
        BackgroundMetadata(
            id = "minimal_living_room_01",
            category = BackgroundCategory.LIVING_ROOM,
            surfaces = listOf(SurfaceType.CONCRETE, SurfaceType.TABLE),
            style = listOf(StyleTag.MINIMAL, StyleTag.CONTEMPORARY, StyleTag.AIRY),
            lighting = LightingDirection.SOFT_DIFFUSE,
            dominantColors = listOf(Color.rgb(242, 240, 238), Color.rgb(238, 235, 230)),
            productPlacements = listOf(ProductPlacement.CENTER, ProductPlacement.LOWER_CENTER, ProductPlacement.LEFT_HERO),
            usableTextRegions = listOf(TextRegion.UPPER_CENTER, TextRegion.TOP_LEFT, TextRegion.FULL_TOP),
            brightness = 0.88f, contrast = 0.38f, warmth = 1.00f, priority = 87
        ),

        // BEDROOM
        BackgroundMetadata(
            id = "bright_bedroom_01",
            category = BackgroundCategory.BEDROOM,
            surfaces = listOf(SurfaceType.LINEN, SurfaceType.SHELF),
            style = listOf(StyleTag.BRIGHT, StyleTag.MINIMAL, StyleTag.AIRY),
            lighting = LightingDirection.NATURAL_RIGHT,
            dominantColors = listOf(Color.rgb(242, 238, 232), Color.rgb(238, 234, 228)),
            productPlacements = listOf(ProductPlacement.CENTER, ProductPlacement.LOWER_CENTER),
            usableTextRegions = listOf(TextRegion.UPPER_CENTER, TextRegion.TOP_LEFT, TextRegion.FULL_TOP),
            brightness = 0.86f, contrast = 0.36f, warmth = 1.02f, priority = 88
        ),
        BackgroundMetadata(
            id = "warm_bedroom_01",
            category = BackgroundCategory.BEDROOM,
            surfaces = listOf(SurfaceType.WOOD, SurfaceType.FABRIC),
            style = listOf(StyleTag.WARM, StyleTag.COZY, StyleTag.NATURAL),
            lighting = LightingDirection.WARM_AMBIENT,
            dominantColors = listOf(Color.rgb(225, 215, 200), Color.rgb(218, 208, 192)),
            productPlacements = listOf(ProductPlacement.CENTER, ProductPlacement.LOWER_CENTER),
            usableTextRegions = listOf(TextRegion.UPPER_CENTER, TextRegion.FULL_TOP),
            brightness = 0.78f, contrast = 0.40f, warmth = 1.06f, priority = 84
        ),

        // BATHROOM
        BackgroundMetadata(
            id = "modern_bathroom_01",
            category = BackgroundCategory.BATHROOM,
            surfaces = listOf(SurfaceType.TILE, SurfaceType.MARBLE),
            style = listOf(StyleTag.MODERN, StyleTag.BRIGHT, StyleTag.MINIMAL),
            lighting = LightingDirection.COOL_BRIGHT,
            dominantColors = listOf(Color.rgb(245, 248, 250), Color.rgb(240, 244, 248)),
            productPlacements = listOf(ProductPlacement.CENTER, ProductPlacement.LOWER_CENTER),
            usableTextRegions = listOf(TextRegion.UPPER_CENTER, TextRegion.TOP_LEFT, TextRegion.FULL_TOP),
            brightness = 0.90f, contrast = 0.38f, warmth = 0.97f, priority = 88
        ),
        BackgroundMetadata(
            id = "spa_bathroom_01",
            category = BackgroundCategory.BATHROOM,
            surfaces = listOf(SurfaceType.STONE, SurfaceType.WOOD),
            style = listOf(StyleTag.WARM, StyleTag.NATURAL, StyleTag.LUXURY),
            lighting = LightingDirection.SOFT_DIFFUSE,
            dominantColors = listOf(Color.rgb(230, 225, 218), Color.rgb(220, 215, 205)),
            productPlacements = listOf(ProductPlacement.CENTER, ProductPlacement.LOWER_CENTER),
            usableTextRegions = listOf(TextRegion.UPPER_CENTER, TextRegion.FULL_TOP),
            brightness = 0.80f, contrast = 0.42f, warmth = 1.04f, priority = 85
        ),

        // OFFICE
        BackgroundMetadata(
            id = "modern_office_01",
            category = BackgroundCategory.OFFICE,
            surfaces = listOf(SurfaceType.WOOD, SurfaceType.DESK),
            style = listOf(StyleTag.MODERN, StyleTag.MINIMAL, StyleTag.CONTEMPORARY),
            lighting = LightingDirection.NATURAL_LEFT,
            dominantColors = listOf(Color.rgb(228, 222, 212), Color.rgb(220, 214, 202)),
            productPlacements = listOf(ProductPlacement.CENTER, ProductPlacement.LOWER_CENTER),
            usableTextRegions = listOf(TextRegion.UPPER_CENTER, TextRegion.TOP_LEFT, TextRegion.FULL_TOP),
            brightness = 0.82f, contrast = 0.42f, warmth = 1.01f, priority = 87
        ),
        BackgroundMetadata(
            id = "scandi_office_01",
            category = BackgroundCategory.OFFICE,
            surfaces = listOf(SurfaceType.WOOD, SurfaceType.DESK),
            style = listOf(StyleTag.SCANDINAVIAN, StyleTag.BRIGHT, StyleTag.AIRY),
            lighting = LightingDirection.COOL_BRIGHT,
            dominantColors = listOf(Color.rgb(240, 238, 232), Color.rgb(235, 232, 225)),
            productPlacements = listOf(ProductPlacement.CENTER, ProductPlacement.LOWER_CENTER, ProductPlacement.RIGHT_HERO),
            usableTextRegions = listOf(TextRegion.UPPER_CENTER, TextRegion.TOP_LEFT, TextRegion.FULL_TOP),
            brightness = 0.88f, contrast = 0.38f, warmth = 0.99f, priority = 86
        ),

        // DINING
        BackgroundMetadata(
            id = "modern_dining_01",
            category = BackgroundCategory.DINING,
            surfaces = listOf(SurfaceType.WOOD, SurfaceType.TABLE),
            style = listOf(StyleTag.MODERN, StyleTag.WARM, StyleTag.CONTEMPORARY),
            lighting = LightingDirection.WARM_AMBIENT,
            dominantColors = listOf(Color.rgb(218, 205, 185), Color.rgb(210, 198, 178)),
            productPlacements = listOf(ProductPlacement.CENTER, ProductPlacement.LOWER_CENTER),
            usableTextRegions = listOf(TextRegion.UPPER_CENTER, TextRegion.FULL_TOP),
            brightness = 0.80f, contrast = 0.42f, warmth = 1.05f, priority = 84
        ),

        // OUTDOOR
        BackgroundMetadata(
            id = "garden_outdoor_01",
            category = BackgroundCategory.OUTDOOR,
            surfaces = listOf(SurfaceType.WOOD, SurfaceType.STONE),
            style = listOf(StyleTag.NATURAL, StyleTag.BRIGHT, StyleTag.AIRY),
            lighting = LightingDirection.NATURAL_LEFT,
            dominantColors = listOf(Color.rgb(200, 215, 190), Color.rgb(190, 205, 180)),
            productPlacements = listOf(ProductPlacement.CENTER, ProductPlacement.LOWER_CENTER),
            usableTextRegions = listOf(TextRegion.UPPER_CENTER, TextRegion.FULL_TOP),
            brightness = 0.84f, contrast = 0.40f, warmth = 1.02f, priority = 82
        ),

        // GENERAL
        BackgroundMetadata(
            id = "clean_studio_01",
            category = BackgroundCategory.GENERAL,
            surfaces = listOf(SurfaceType.CONCRETE),
            style = listOf(StyleTag.MINIMAL, StyleTag.MODERN, StyleTag.BRIGHT),
            lighting = LightingDirection.SOFT_DIFFUSE,
            dominantColors = listOf(Color.rgb(242, 240, 238), Color.rgb(238, 236, 232)),
            productPlacements = listOf(ProductPlacement.CENTER, ProductPlacement.LOWER_CENTER, ProductPlacement.LEFT_HERO, ProductPlacement.RIGHT_HERO),
            usableTextRegions = listOf(TextRegion.UPPER_CENTER, TextRegion.TOP_LEFT, TextRegion.TOP_RIGHT, TextRegion.FULL_TOP),
            brightness = 0.88f, contrast = 0.38f, warmth = 1.00f, priority = 80
        ),
        BackgroundMetadata(
            id = "warm_studio_01",
            category = BackgroundCategory.GENERAL,
            surfaces = listOf(SurfaceType.WOOD),
            style = listOf(StyleTag.WARM, StyleTag.NATURAL, StyleTag.MINIMAL),
            lighting = LightingDirection.WARM_AMBIENT,
            dominantColors = listOf(Color.rgb(225, 215, 200), Color.rgb(218, 208, 192)),
            productPlacements = listOf(ProductPlacement.CENTER, ProductPlacement.LOWER_CENTER),
            usableTextRegions = listOf(TextRegion.UPPER_CENTER, TextRegion.FULL_TOP),
            brightness = 0.82f, contrast = 0.40f, warmth = 1.05f, priority = 79
        ),

        // STORAGE
        BackgroundMetadata(
            id = "modern_storage_01",
            category = BackgroundCategory.STORAGE,
            surfaces = listOf(SurfaceType.WOOD, SurfaceType.SHELF),
            style = listOf(StyleTag.MODERN, StyleTag.MINIMAL),
            lighting = LightingDirection.SOFT_DIFFUSE,
            dominantColors = listOf(Color.rgb(238, 235, 228), Color.rgb(232, 228, 220)),
            productPlacements = listOf(ProductPlacement.CENTER, ProductPlacement.LOWER_CENTER),
            usableTextRegions = listOf(TextRegion.UPPER_CENTER, TextRegion.FULL_TOP),
            brightness = 0.82f, contrast = 0.40f, warmth = 1.02f, priority = 83
        ),

        // CLOSET
        BackgroundMetadata(
            id = "organized_closet_01",
            category = BackgroundCategory.CLOSET,
            surfaces = listOf(SurfaceType.WOOD, SurfaceType.SHELF, SurfaceType.FABRIC),
            style = listOf(StyleTag.MODERN, StyleTag.MINIMAL, StyleTag.BRIGHT),
            lighting = LightingDirection.SOFT_DIFFUSE,
            dominantColors = listOf(Color.rgb(235, 232, 225), Color.rgb(228, 225, 218)),
            productPlacements = listOf(ProductPlacement.CENTER, ProductPlacement.LOWER_CENTER),
            usableTextRegions = listOf(TextRegion.UPPER_CENTER, TextRegion.FULL_TOP),
            brightness = 0.84f, contrast = 0.38f, warmth = 1.01f, priority = 82
        ),

        // LAUNDRY
        BackgroundMetadata(
            id = "bright_laundry_01",
            category = BackgroundCategory.LAUNDRY,
            surfaces = listOf(SurfaceType.TILE, SurfaceType.COUNTERTOP),
            style = listOf(StyleTag.BRIGHT, StyleTag.MODERN, StyleTag.CLEAN),
            lighting = LightingDirection.COOL_BRIGHT,
            dominantColors = listOf(Color.rgb(242, 245, 248), Color.rgb(238, 242, 245)),
            productPlacements = listOf(ProductPlacement.CENTER, ProductPlacement.LOWER_CENTER),
            usableTextRegions = listOf(TextRegion.UPPER_CENTER, TextRegion.FULL_TOP),
            brightness = 0.90f, contrast = 0.36f, warmth = 0.98f, priority = 83
        ),

        // ENTRYWAY
        BackgroundMetadata(
            id = "modern_entryway_01",
            category = BackgroundCategory.ENTRYWAY,
            surfaces = listOf(SurfaceType.WOOD, SurfaceType.FLOOR),
            style = listOf(StyleTag.MODERN, StyleTag.WARM, StyleTag.CONTEMPORARY),
            lighting = LightingDirection.NATURAL_LEFT,
            dominantColors = listOf(Color.rgb(220, 215, 205), Color.rgb(215, 210, 198)),
            productPlacements = listOf(ProductPlacement.CENTER, ProductPlacement.LOWER_CENTER),
            usableTextRegions = listOf(TextRegion.UPPER_CENTER, TextRegion.FULL_TOP),
            brightness = 0.80f, contrast = 0.42f, warmth = 1.03f, priority = 82
        ),

        // PANTRY
        BackgroundMetadata(
            id = "organized_pantry_01",
            category = BackgroundCategory.PANTRY,
            surfaces = listOf(SurfaceType.WOOD, SurfaceType.SHELF),
            style = listOf(StyleTag.MODERN, StyleTag.MINIMAL, StyleTag.WARM),
            lighting = LightingDirection.SOFT_DIFFUSE,
            dominantColors = listOf(Color.rgb(238, 232, 222), Color.rgb(232, 226, 215)),
            productPlacements = listOf(ProductPlacement.CENTER, ProductPlacement.LOWER_CENTER),
            usableTextRegions = listOf(TextRegion.UPPER_CENTER, TextRegion.FULL_TOP),
            brightness = 0.82f, contrast = 0.40f, warmth = 1.03f, priority = 82
        ),

        // VANITY
        BackgroundMetadata(
            id = "modern_vanity_01",
            category = BackgroundCategory.VANITY,
            surfaces = listOf(SurfaceType.MARBLE, SurfaceType.GLASS),
            style = listOf(StyleTag.MODERN, StyleTag.LUXURY, StyleTag.BRIGHT),
            lighting = LightingDirection.FRONT,
            dominantColors = listOf(Color.rgb(248, 245, 242), Color.rgb(242, 238, 235)),
            productPlacements = listOf(ProductPlacement.CENTER, ProductPlacement.LOWER_CENTER),
            usableTextRegions = listOf(TextRegion.UPPER_CENTER, TextRegion.FULL_TOP),
            brightness = 0.88f, contrast = 0.38f, warmth = 1.01f, priority = 85
        )
    )
}
