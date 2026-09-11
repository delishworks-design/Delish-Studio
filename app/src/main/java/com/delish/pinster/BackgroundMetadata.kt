package com.delish.pinster

import android.graphics.Color

data class BackgroundMetadata(
    val id: String,
    val category: BackgroundCategory,
    val surfaces: List<SurfaceType>,
    val style: List<StyleTag>,
    val lighting: LightingDirection,
    val dominantColors: List<Int>,
    val productPlacements: List<ProductPlacement>,
    val usableTextRegions: List<TextRegion>,
    val brightness: Float,
    val contrast: Float,
    val warmth: Float,
    val priority: Int = 50
)

enum class BackgroundCategory {
    KITCHEN, LIVING_ROOM, BEDROOM, BATHROOM, OFFICE,
    CLOSET, LAUNDRY, ENTRYWAY, OUTDOOR, DINING,
    STORAGE, GENERAL, STUDIO, VANITY, PANTRY
}

enum class SurfaceType {
    MARBLE, WOOD, GRANITE, TILE, LINEN, CONCRETE,
    COUNTERTOP, DESK, TABLE, FLOOR, SHELF, FABRIC,
    METAL, GLASS, PAPER, STONE
}

enum class StyleTag {
    MODERN, MINIMAL, WARM, COZY, INDUSTRIAL,
    SCANDINAVIAN, BOHEMIAN, LUXURY, RUSTIC, CONTEMPORARY,
    BRIGHT, DARK, AIRY, INTIMATE, NATURAL, CLEAN
}

enum class LightingDirection {
    NATURAL_LEFT, NATURAL_RIGHT, OVERHEAD, FRONT,
    SOFT_DIFFUSE, WARM_AMBIENT, COOL_BRIGHT, DRAMATIC
}

enum class TextRegion {
    TOP_LEFT, TOP_CENTER, TOP_RIGHT,
    UPPER_LEFT, UPPER_CENTER, UPPER_RIGHT,
    MIDDLE_LEFT, MIDDLE_RIGHT,
    LOWER_LEFT, LOWER_CENTER, LOWER_RIGHT,
    FULL_TOP, FULL_BOTTOM
}
