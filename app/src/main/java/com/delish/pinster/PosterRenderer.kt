package com.delish.pinster

import android.content.Context
import android.graphics.*

object PosterRenderer {

    fun renderAll(
        context: Context,
        productBitmap: Bitmap,
        title: String,
        description: String,
        seo: SeoResult? = null,
        productEvidence: ProductEvidence? = null,
        productImageUrl: String? = null
    ): List<PinterestAngle> {
        val effectiveSeo = seo ?: SeoResult(
            title = title,
            description = description,
            altText = "",
            affiliateLink = "",
            productImages = emptyList()
        )

        return PinterestAngleBuilder.buildAll(
            product = productEvidence,
            seo = effectiveSeo,
            productImageUrl = productImageUrl,
            productBitmap = productBitmap
        )
    }
}
