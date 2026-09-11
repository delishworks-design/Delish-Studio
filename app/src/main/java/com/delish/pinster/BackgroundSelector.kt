package com.delish.pinster

import android.graphics.Color

object BackgroundSelector {

    data class ScoredBackground(
        val metadata: BackgroundMetadata,
        val totalScore: Float,
        val categoryScore: Float,
        val surfaceScore: Float,
        val styleScore: Float,
        val lightingScore: Float,
        val brightnessScore: Float,
        val textRegionScore: Float
    )

    fun select(
        fp: ProductFingerprint,
        concept: CreativeConcept,
        direction: ArtDirection,
        count: Int = 5
    ): List<ScoredBackground> {
        val candidates = BackgroundLibrary.allMetadata.map { meta ->
            scoreBackground(fp, concept, direction, meta)
        }

        return candidates
            .sortedByDescending { it.totalScore }
            .take(count)
            .distinctBy { it.metadata.category }
            .take(count)
    }

    fun selectBest(
        fp: ProductFingerprint,
        concept: CreativeConcept,
        direction: ArtDirection
    ): ScoredBackground {
        return select(fp, concept, direction, 1).first()
    }

    private fun scoreBackground(
        fp: ProductFingerprint,
        concept: CreativeConcept,
        direction: ArtDirection,
        meta: BackgroundMetadata
    ): ScoredBackground {
        val categoryScore = scoreCategory(fp, meta)
        val surfaceScore = scoreSurface(fp, meta)
        val styleScore = scoreStyle(concept, direction, meta)
        val lightingScore = scoreLighting(fp, meta)
        val brightnessScore = scoreBrightness(direction, meta)
        val textRegionScore = scoreTextRegion(meta)

        val totalScore = (categoryScore * 0.30f +
                surfaceScore * 0.15f +
                styleScore * 0.25f +
                lightingScore * 0.10f +
                brightnessScore * 0.12f +
                textRegionScore * 0.08f) * (meta.priority / 100f)

        return ScoredBackground(
            metadata = meta,
            totalScore = totalScore,
            categoryScore = categoryScore,
            surfaceScore = surfaceScore,
            styleScore = styleScore,
            lightingScore = lightingScore,
            brightnessScore = brightnessScore,
            textRegionScore = textRegionScore
        )
    }

    private fun scoreCategory(fp: ProductFingerprint, meta: BackgroundMetadata): Float {
        val targetCategories = when (fp.productCategory) {
            ProductCategory.KITCHEN -> listOf(BackgroundCategory.KITCHEN, BackgroundCategory.PANTRY, BackgroundCategory.DINING)
            ProductCategory.BEDROOM -> listOf(BackgroundCategory.BEDROOM, BackgroundCategory.CLOSET, BackgroundCategory.ENTRYWAY)
            ProductCategory.BATHROOM -> listOf(BackgroundCategory.BATHROOM, BackgroundCategory.VANITY, BackgroundCategory.LAUNDRY)
            ProductCategory.STORAGE -> listOf(BackgroundCategory.STORAGE, BackgroundCategory.CLOSET, BackgroundCategory.ENTRYWAY)
            ProductCategory.DECOR -> listOf(BackgroundCategory.LIVING_ROOM, BackgroundCategory.BEDROOM, BackgroundCategory.ENTRYWAY)
            ProductCategory.OFFICE -> listOf(BackgroundCategory.OFFICE, BackgroundCategory.GENERAL)
            ProductCategory.ELECTRONICS -> listOf(BackgroundCategory.OFFICE, BackgroundCategory.LIVING_ROOM, BackgroundCategory.GENERAL)
            ProductCategory.FASHION -> listOf(BackgroundCategory.BEDROOM, BackgroundCategory.CLOSET, BackgroundCategory.VANITY)
            ProductCategory.FOOD -> listOf(BackgroundCategory.KITCHEN, BackgroundCategory.PANTRY, BackgroundCategory.DINING)
            ProductCategory.OUTDOOR -> listOf(BackgroundCategory.OUTDOOR, BackgroundCategory.GENERAL)
            ProductCategory.BEAUTY -> listOf(BackgroundCategory.VANITY, BackgroundCategory.BATHROOM, BackgroundCategory.BEDROOM)
            ProductCategory.UNKNOWN -> listOf(BackgroundCategory.GENERAL, BackgroundCategory.LIVING_ROOM, BackgroundCategory.KITCHEN)
        }

        return when (meta.category) {
            in targetCategories -> 95f
            BackgroundCategory.GENERAL -> 70f
            else -> 40f
        }
    }

    private fun scoreSurface(fp: ProductFingerprint, meta: BackgroundMetadata): Float {
        val isSmallProduct = fp.aspectRatio > 0.8f && fp.aspectRatio < 1.2f
        val isFlatProduct = fp.aspectRatio > 1.5f

        return when {
            isSmallProduct && SurfaceType.COUNTERTOP in meta.surfaces -> 90f
            isSmallProduct && SurfaceType.DESK in meta.surfaces -> 88f
            isSmallProduct && SurfaceType.TABLE in meta.surfaces -> 85f
            isFlatProduct && SurfaceType.SHELF in meta.surfaces -> 90f
            isFlatProduct && SurfaceType.FLOOR in meta.surfaces -> 85f
            SurfaceType.WOOD in meta.surfaces -> 75f
            SurfaceType.MARBLE in meta.surfaces -> 80f
            else -> 65f
        }
    }

    private fun scoreStyle(concept: CreativeConcept, direction: ArtDirection, meta: BackgroundMetadata): Float {
        val targetStyles = when (direction) {
            ArtDirection.LUXURY_EDITORIAL -> listOf(StyleTag.LUXURY, StyleTag.MODERN, StyleTag.CONTEMPORARY)
            ArtDirection.MODERN_MINIMAL -> listOf(StyleTag.MINIMAL, StyleTag.MODERN, StyleTag.BRIGHT, StyleTag.AIRY)
            ArtDirection.ORGANIC_LIFESTYLE -> listOf(StyleTag.NATURAL, StyleTag.WARM, StyleTag.BOHEMIAN)
            ArtDirection.BOLD_CAMPAIGN -> listOf(StyleTag.INDUSTRIAL, StyleTag.CONTEMPORARY, StyleTag.MODERN)
            ArtDirection.SOFT_PINTEREST -> listOf(StyleTag.COZY, StyleTag.WARM, StyleTag.SCANDINAVIAN)
        }

        val matchingStyles = meta.style.count { it in targetStyles }
        return when (matchingStyles) {
            3 -> 95f
            2 -> 85f
            1 -> 70f
            else -> 50f
        }
    }

    private fun scoreLighting(fp: ProductFingerprint, meta: BackgroundMetadata): Float {
        return when (meta.lighting) {
            LightingDirection.NATURAL_LEFT, LightingDirection.NATURAL_RIGHT -> 90f
            LightingDirection.SOFT_DIFFUSE -> 85f
            LightingDirection.COOL_BRIGHT -> if (fp.isDarkProduct) 80f else 70f
            LightingDirection.WARM_AMBIENT -> if (fp.isWarmTone) 85f else 70f
            LightingDirection.FRONT -> 75f
            LightingDirection.OVERHEAD -> 70f
            LightingDirection.DRAMATIC -> 60f
        }
    }

    private fun scoreBrightness(direction: ArtDirection, meta: BackgroundMetadata): Float {
        val targetBrightness = when (direction) {
            ArtDirection.LUXURY_EDITORIAL -> 0.75f
            ArtDirection.MODERN_MINIMAL -> 0.90f
            ArtDirection.ORGANIC_LIFESTYLE -> 0.82f
            ArtDirection.BOLD_CAMPAIGN -> 0.80f
            ArtDirection.SOFT_PINTEREST -> 0.85f
        }

        val diff = kotlin.math.abs(meta.brightness - targetBrightness)
        return (100f - diff * 200f).coerceIn(50f, 100f)
    }

    private fun scoreTextRegion(meta: BackgroundMetadata): Float {
        val hasGoodRegions = meta.usableTextRegions.any {
            it in listOf(TextRegion.UPPER_CENTER, TextRegion.TOP_LEFT, TextRegion.FULL_TOP)
        }
        return if (hasGoodRegions) 90f else 65f
    }
}
