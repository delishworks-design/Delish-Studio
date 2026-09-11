package com.delish.pinster

import android.graphics.*

object BackgroundCropper {

    private const val TARGET_W = 1000
    private const val TARGET_H = 1500
    private const val TARGET_RATIO = TARGET_W.toFloat() / TARGET_H

    fun cropToPinterest(
        background: Bitmap,
        textRegion: TextRegion = TextRegion.UPPER_CENTER,
        productPlacement: ProductPlacement = ProductPlacement.CENTER
    ): Bitmap {
        val srcW = background.width
        val srcH = background.height
        val srcRatio = srcW.toFloat() / srcH

        val (cropW, cropH, offsetX, offsetY) = calculateCrop(srcW, srcH, srcRatio, textRegion, productPlacement)

        val cropped = Bitmap.createBitmap(background, offsetX, offsetY, cropW, cropH)
        return Bitmap.createScaledBitmap(cropped, TARGET_W, TARGET_H, true)
    }

    private fun calculateCrop(
        srcW: Int, srcH: Int, srcRatio: Float,
        textRegion: TextRegion,
        productPlacement: ProductPlacement
    ): CropParams {
        if (srcRatio > TARGET_RATIO) {
            val cropW = (srcH * TARGET_RATIO).toInt()
            val cropH = srcH
            val offsetX = calculateHorizontalOffset(cropW, srcW, textRegion)
            return CropParams(cropW, cropH, offsetX, 0)
        } else {
            val cropW = srcW
            val cropH = (srcW / TARGET_RATIO).toInt()
            val offsetY = calculateVerticalOffset(cropH, srcH, textRegion, productPlacement)
            return CropParams(cropW, cropH, 0, offsetY)
        }
    }

    private fun calculateHorizontalOffset(cropW: Int, srcW: Int, textRegion: TextRegion): Int {
        return when (textRegion) {
            TextRegion.TOP_LEFT, TextRegion.UPPER_LEFT, TextRegion.MIDDLE_LEFT, TextRegion.LOWER_LEFT -> 0
            TextRegion.TOP_RIGHT, TextRegion.UPPER_RIGHT, TextRegion.MIDDLE_RIGHT, TextRegion.LOWER_RIGHT -> (srcW - cropW)
            else -> ((srcW - cropW) / 2).coerceAtLeast(0)
        }
    }

    private fun calculateVerticalOffset(
        cropH: Int, srcH: Int,
        textRegion: TextRegion,
        productPlacement: ProductPlacement
    ): Int {
        val hasUpperText = textRegion in listOf(
            TextRegion.TOP_LEFT, TextRegion.TOP_CENTER, TextRegion.TOP_RIGHT,
            TextRegion.UPPER_LEFT, TextRegion.UPPER_CENTER, TextRegion.UPPER_RIGHT,
            TextRegion.FULL_TOP
        )

        val hasLowerProduct = productPlacement in listOf(
            ProductPlacement.LOWER_CENTER, ProductPlacement.LOWER_THIRD
        )

        return when {
            hasUpperText && hasLowerProduct -> {
                val maxOffset = (srcH - cropH).coerceAtLeast(0)
                (maxOffset * 0.3f).toInt()
            }
            hasUpperText -> {
                val maxOffset = (srcH - cropH).coerceAtLeast(0)
                (maxOffset * 0.4f).toInt()
            }
            hasLowerProduct -> {
                val maxOffset = (srcH - cropH).coerceAtLeast(0)
                (maxOffset * 0.2f).toInt()
            }
            else -> {
                ((srcH - cropH) / 2).coerceAtLeast(0)
            }
        }
    }

    private data class CropParams(
        val width: Int,
        val height: Int,
        val offsetX: Int,
        val offsetY: Int
    )
}
