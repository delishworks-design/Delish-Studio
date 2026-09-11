package com.delish.pinster

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

class CoverFlowCarousel @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val bmps = mutableListOf<Bitmap>()
    private var current = 0
    private var scrollPos = 0f    // integer = centered item, fractional = transition progress
    private var scrolling = false
    private var downRawX = 0f
    private var downRawY = 0f
    private var scrollStart = 0f

    private val RATIO = 2f / 3f
    private var cw = 0
    private var ch = 0
    private var gap = 0
    private val SIDE_SCL = 0.68f
    private val SIDE_A = 0.40f
    private val TILT_DEG = 18f

    private var snapAnim: ValueAnimator? = null

    var onSelectionChanged: ((Int) -> Unit)? = null
    var onImageClicked: ((Int) -> Unit)? = null
    var onImageLongPressed: ((Int) -> Unit)? = null

    private var tapX = 0f
    private var tapY = 0f
    private var tapDown = 0L
    private var dragLock = false
    private var isHoriz = false
    private var holdRunnable: Runnable? = null

    private val shadowP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(25, 0, 0, 0)
        maskFilter = BlurMaskFilter(20f, BlurMaskFilter.Blur.NORMAL)
    }

    fun setImages(list: List<Bitmap>) {
        bmps.clear()
        bmps.addAll(list)
        current = 0
        scrollPos = 0f
        requestLayout()
        invalidate()
    }

    fun getSelectedIndex() = current

    override fun onMeasure(w: Int, h: Int) {
        val W = MeasureSpec.getSize(w)
        val H = MeasureSpec.getSize(h)
        cw = (W * 0.78f).toInt()
        ch = (cw / RATIO).toInt()
        if (ch > H * 0.9f) {
            ch = (H * 0.9f).toInt()
            cw = (ch * RATIO).toInt()
        }
        gap = (cw * 0.55f).toInt()
        setMeasuredDimension(W, H)
    }

    // ── RENDER ────────────────────────────────────

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        if (bmps.isEmpty()) return
        val n = bmps.size
        val cx = width / 2f
        val cy = height / 2f

        // scrollPos: 0 = item[0] centered, 1 = item[1] centered, etc.
        // For each visible item, compute its visual position on screen
        data class Vis(val bmp: Bitmap, val x: Float, val scl: Float, val al: Float, val tilt: Float)

        val vis = mutableListOf<Vis>()
        val intPos = scrollPos.toInt()
        val frac = scrollPos - intPos

        // Show items from intPos-2 to intPos+3
        for (offset in -2..3) {
            val slot = offset - frac   // visual position: slot 0 = center
            if (abs(slot) > 2.5f) continue
            val idx = ((current + intPos + offset) % n + n) % n
            val absSlot = abs(slot)
            val scl = when {
                absSlot < 0.01f -> 1f
                absSlot <= 1f -> 1f - (1f - SIDE_SCL) * absSlot
                else -> SIDE_SCL * (1f - (absSlot - 1f) * 0.08f)
            }.coerceIn(0.42f, 1f)
            val al = when {
                absSlot < 0.01f -> 1f
                absSlot <= 1f -> 1f - (1f - SIDE_A) * absSlot
                else -> (SIDE_A * 0.7f).coerceIn(0.2f, 1f)
            }
            val tilt = when {
                slot < -0.01f -> -TILT_DEG * absSlot.coerceAtMost(1f)
                slot > 0.01f -> TILT_DEG * absSlot.coerceAtMost(1f)
                else -> 0f
            }
            vis.add(Vis(bmps[idx], cx + slot * gap, scl, al, tilt))
        }

        // Draw back to front
        vis.sortByDescending { abs(it.x - cx) }
        for (v in vis) drawOne(c, v.bmp, v.x, cy, v.scl, v.al, v.tilt)
    }

    private fun drawOne(c: Canvas, bmp: Bitmap, ix: Float, cy: Float, scl: Float, al: Float, tilt: Float) {
        val dw = cw * scl
        val dh = dw / RATIO
        val lx = ix - dw / 2f
        val ly = cy - dh / 2f

        val save = c.save()

        // Shadow
        val absPos = abs(ix - width / 2f) / gap
        if (absPos < 0.5f) {
            shadowP.alpha = ((1f - absPos / 0.5f) * 25).toInt().coerceIn(0, 25)
            c.drawRoundRect(
                RectF(lx + 5f, ly + dh - 3f, lx + dw - 5f, ly + dh + 12f),
                8f, 8f, shadowP
            )
        }

        // Tilt
        c.save()
        c.rotate(tilt, lx + dw / 2f, ly + dh / 2f)

        // Clip
        val rad = 12f * scl
        val dst = RectF(lx, ly, lx + dw, ly + dh)
        val clip = Path().apply { addRoundRect(dst, rad, rad, Path.Direction.CW) }
        c.clipPath(clip)

        // Bitmap
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.alpha = (al * 255).toInt().coerceIn(0, 255)
        c.drawBitmap(bmp, null, dst, p)
        c.restore()

        // Border on center
        if (absPos < 0.12f) {
            val bp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 2f
                color = Color.argb(30, 255, 255, 255)
            }
            bp.alpha = (al * 255).toInt()
            c.drawRoundRect(dst, rad, rad, bp)
        }

        c.restoreToCount(save)
    }

    // ── TOUCH ─────────────────────────────────────

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                snapAnim?.cancel()
                downRawX = e.rawX
                downRawY = e.rawY
                scrollStart = scrollPos
                scrolling = true
                dragLock = false
                isHoriz = false
                tapX = e.x
                tapY = e.y
                tapDown = System.currentTimeMillis()
                parent?.requestDisallowInterceptTouchEvent(true)
                holdRunnable?.let { removeCallbacks(it) }
                holdRunnable = Runnable {
                    if (scrolling && !dragLock) {
                        onImageLongPressed?.invoke(current)
                    }
                }
                postDelayed(holdRunnable, 500)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!scrolling) return true
                val dx = e.rawX - downRawX
                val dy = e.rawY - downRawY
                if (!dragLock && (abs(dx) > dp(6) || abs(dy) > dp(6))) {
                    dragLock = true
                    isHoriz = abs(dx) > abs(dy)
                    holdRunnable?.let { removeCallbacks(it) }
                }
                if (dragLock && !isHoriz) {
                    parent?.requestDisallowInterceptTouchEvent(false)
                    scrolling = false
                    return false
                }
                if (isHoriz) {
                    parent?.requestDisallowInterceptTouchEvent(true)
                    scrollPos = scrollStart + (-dx.toFloat() / gap)
                    scrollPos = scrollPos.coerceIn(-1f, (bmps.size - 1).toFloat())
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                holdRunnable?.let { removeCallbacks(it) }
                val was = scrolling
                scrolling = false
                parent?.requestDisallowInterceptTouchEvent(false)
                if (e.actionMasked == MotionEvent.ACTION_UP && was) {
                    val dx = e.x - tapX
                    val dy = e.y - tapY
                    val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                    if (dist < dp(10) && System.currentTimeMillis() - tapDown < 300) {
                        val idx = hitTest(e.x, e.y)
                        if (idx == current) onImageClicked?.invoke(current)
                        else if (idx >= 0) snapTo(idx)
                    } else {
                        snapNearest()
                    }
                } else {
                    snapNearest()
                }
                return true
            }
        }
        return true
    }

    private fun hitTest(x: Float, y: Float): Int {
        val cx = width / 2f
        val cy = height / 2f
        val n = bmps.size
        val intPos = scrollPos.toInt()
        val frac = scrollPos - intPos

        for (offset in -2..3) {
            val slot = offset - frac
            if (abs(slot) > 1.8f) continue
            val idx = ((current + intPos + offset) % n + n) % n
            val absSlot = abs(slot)
            val scl = when {
                absSlot < 0.01f -> 1f
                absSlot <= 1f -> 1f - (1f - SIDE_SCL) * absSlot
                else -> SIDE_SCL
            }
            val dw = cw * scl
            val dh = dw / RATIO
            val ix = cx + slot * gap
            if (x in (ix - dw / 2f)..(ix + dw / 2f) && y in (cy - dh / 2f)..(cy + dh / 2f)) {
                return idx
            }
        }
        return -1
    }

    // ── SNAP ──────────────────────────────────────

    private fun snapNearest() {
        animateTo(scrollPos.roundToInt().toFloat().coerceIn(-1f, (bmps.size - 1).toFloat()))
    }

    fun snapTo(index: Int) {
        var target = index.toFloat() - current
        while (target - scrollPos > bmps.size / 2f) target -= bmps.size
        while (target - scrollPos < -bmps.size / 2f) target += bmps.size
        animateTo(target.coerceIn(-1f, (bmps.size - 1).toFloat()))
    }

    private fun animateTo(target: Float) {
        snapAnim?.cancel()
        val start = scrollPos
        val dist = abs(target - start)
        snapAnim = ValueAnimator.ofFloat(start, target).apply {
            duration = (160 + dist * 55).toLong().coerceIn(160L, 280L)
            interpolator = DecelerateInterpolator(2f)
            addUpdateListener {
                scrollPos = it.animatedValue as Float
                invalidate()
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(a: android.animation.Animator) {
                    finishSnap()
                }
            })
            start()
        }
    }

    private fun finishSnap() {
        val shift = scrollPos.roundToInt()
        current = ((current + shift) % bmps.size + bmps.size) % bmps.size
        scrollPos = 0f
        onSelectionChanged?.invoke(current)
        invalidate()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
