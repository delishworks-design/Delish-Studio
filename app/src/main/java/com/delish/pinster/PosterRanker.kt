package com.delish.pinster

object PosterRanker {

    data class RankedPoster(
        val bitmap: android.graphics.Bitmap,
        val score: QualityScore,
        val direction: ArtDirection,
        val diversityBonus: Float = 0f
    )

    fun rank(
        posters: List<android.graphics.Bitmap>,
        scores: List<QualityScore>,
        directions: List<ArtDirection>
    ): List<android.graphics.Bitmap> {
        if (posters.size != scores.size || posters.size != directions.size) {
            return posters
        }

        val ranked = posters.indices.map { idx ->
            RankedPoster(posters[idx], scores[idx], directions[idx])
        }.sortedByDescending { it.score.total }

        val diversityRanked = applyDiversityBonus(ranked)

        return diversityRanked.sortedByDescending { it.score.total + it.diversityBonus }.map { it.bitmap }
    }

    fun rank(
        posters: List<android.graphics.Bitmap>,
        scores: List<QualityScore>
    ): List<android.graphics.Bitmap> {
        if (posters.size != scores.size) {
            return posters
        }

        val directions = listOf(
            ArtDirection.LUXURY_EDITORIAL,
            ArtDirection.MODERN_MINIMAL,
            ArtDirection.ORGANIC_LIFESTYLE,
            ArtDirection.BOLD_CAMPAIGN,
            ArtDirection.SOFT_PINTEREST
        )

        return rank(posters, scores, directions)
    }

    private fun applyDiversityBonus(posters: List<RankedPoster>): List<RankedPoster> {
        if (posters.size <= 1) return posters

        val selected = mutableListOf(posters.first())

        for (poster in posters.drop(1)) {
            var bonus = 0f
            for (sel in selected) {
                if (poster.direction != sel.direction) bonus += 5f

                val hookDist = kotlin.math.abs(poster.score.hookStrength - sel.score.hookStrength)
                if (hookDist > 20f) bonus += 3f

                val colorDist = kotlin.math.abs(poster.score.colorHarmony - sel.score.colorHarmony)
                if (colorDist > 15f) bonus += 2f

                val balanceDist = kotlin.math.abs(poster.score.visualBalance - sel.score.visualBalance)
                if (balanceDist > 10f) bonus += 1f
            }

            val qualityBonus = when {
                poster.score.total > 85f -> 2f
                poster.score.total > 75f -> 1f
                else -> 0f
            }
            bonus += qualityBonus

            selected.add(RankedPoster(poster.bitmap, poster.score, poster.direction, bonus))
        }

        return selected
    }
}
