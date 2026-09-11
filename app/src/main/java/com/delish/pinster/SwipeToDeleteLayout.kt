package com.delish.pinster

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout

class SwipeToDeleteLayout(
    context: Context,
    private val onDelete: () -> Unit
) : FrameLayout(context) {

    private var downX = 0f
    private var downY = 0f
    private var tracking = false
    private var revealed = false
    private val swipeThreshold = 0.25f
    private val revealOffset = 80f * resources.displayMetrics.density
    private var currentTranslation = 0f

    private val deleteBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(220, 50, 50)
    }

    private val xPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        strokeWidth = 3f * resources.displayMetrics.density
        strokeCap = Paint.Cap.ROUND
        style = Paint.Style.STROKE
    }

    private val xBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(180, 30, 30)
        style = Paint.Style.FILL
    }

    private val trashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 18f * resources.displayMetrics.density
        textAlign = Paint.Align.CENTER
    }

    private val xRect = RectF()

    override fun onDraw(c: Canvas) {
        if (currentTranslation < 0f) {
            val d = resources.displayMetrics.density
            // Red background
            c.drawRect(
                width + currentTranslation, 0f,
                width.toFloat(), height.toFloat(),
                deleteBgPaint
            )
            // Trash icon
            val btnSize = 36f * d
            val btnCx = width + currentTranslation + (width * 0.12f)
            val btnCy = height / 2f
            xRect.set(btnCx - btnSize / 2, btnCy - btnSize / 2, btnCx + btnSize / 2, btnCy + btnSize / 2)
            c.drawCircle(btnCx, btnCy, btnSize / 2, xBgPaint)
            c.drawText("\uD83D\uDDD1", btnCx, btnCy + 6f * d, trashPaint)
        }
        super.onDraw(c)
    }

    override fun onInterceptTouchEvent(e: MotionEvent): Boolean {
        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = e.x
                downY = e.y
                tracking = false

                if (revealed) {
                    if (xRect.contains(e.x, e.y)) {
                        performDelete()
                        return true
                    }
                    snapBack()
                    return true
                }
                return false
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = e.x - downX
                val dy = e.y - downY
                if (!tracking && !revealed) {
                    if (dx < -20f * resources.displayMetrics.density && Math.abs(dx) > Math.abs(dy) * 1.2f) {
                        tracking = true
                        return true
                    }
                }
            }
        }
        return false
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.action) {
            MotionEvent.ACTION_MOVE -> {
                if (tracking) {
                    val dx = e.x - downX
                    val translation = dx.coerceIn(-revealOffset * 1.5f, 0f)
                    currentTranslation = translation
                    applyTranslation(translation)
                    invalidate()
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (tracking) {
                    val dx = e.x - downX
                    if (dx < -width * swipeThreshold) {
                        snapToRevealed()
                    } else {
                        snapBack()
                    }
                    tracking = false
                    return true
                }
            }
        }
        return super.onTouchEvent(e)
    }

    fun collapseAndDelete() {
        performDelete()
    }

    private fun applyTranslation(translation: Float) {
        for (i in 0 until childCount) {
            getChildAt(i)?.translationX = translation
        }
    }

    private fun snapBack() {
        revealed = false
        val animator = ValueAnimator.ofFloat(currentTranslation, 0f)
        animator.duration = 180
        animator.interpolator = DecelerateInterpolator()
        animator.addUpdateListener { anim ->
            val value = anim.animatedValue as Float
            currentTranslation = value
            applyTranslation(value)
            invalidate()
        }
        animator.start()
    }

    private fun snapToRevealed() {
        revealed = true
        hapticFeedback()
        val animator = ValueAnimator.ofFloat(currentTranslation, -revealOffset)
        animator.duration = 180
        animator.interpolator = DecelerateInterpolator()
        animator.addUpdateListener { anim ->
            val value = anim.animatedValue as Float
            currentTranslation = value
            applyTranslation(value)
            invalidate()
        }
        animator.start()
    }

    private fun performDelete() {
        revealed = false
        hapticFeedback()

        // Slide out animation
        val slideAnimator = ValueAnimator.ofFloat(currentTranslation, -width.toFloat())
        slideAnimator.duration = 200
        slideAnimator.interpolator = DecelerateInterpolator()
        slideAnimator.addUpdateListener { anim ->
            val value = anim.animatedValue as Float
            currentTranslation = value
            applyTranslation(value)
            invalidate()
        }
        slideAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                // Collapse height animation
                val parent = parent
                if (parent is ViewGroup) {
                    val startHeight = height
                    val collapseAnimator = ValueAnimator.ofInt(startHeight, 0)
                    collapseAnimator.duration = 180
                    collapseAnimator.interpolator = DecelerateInterpolator()
                    collapseAnimator.addUpdateListener { anim ->
                        val h = anim.animatedValue as Int
                        layoutParams.height = h
                        requestLayout()
                    }
                    collapseAnimator.addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            onDelete()
                        }
                    })
                    collapseAnimator.start()
                } else {
                    onDelete()
                }
            }
        })
        slideAnimator.start()
    }

    private fun hapticFeedback() {
        try {
            performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
        } catch (_: Exception) {}
    }
}
