package com.delish.pinster

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.ColorFilter
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.animation.ValueAnimator
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.MotionEvent
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.*
import android.widget.ImageView
import android.content.Context
import android.content.SharedPreferences
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.text.format.DateFormat
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import java.io.File

class MainActivity : Activity() {

    private lateinit var root: FrameLayout
    private lateinit var content: LinearLayout
    private lateinit var drawer: LinearLayout

    private lateinit var prefs: SharedPreferences

    private var darkMode = false
    private val history = mutableListOf<String>()

    private var currentAngles: List<PinterestAngle>? = null
    private var currentCarousel: CoverFlowCarousel? = null
    private var currentSeoResult: SeoResult? = null
    private var currentSelectedAngleIndex: Int = 0

    // Drawer view references for live theme switching
    private lateinit var drawerTitle: TextView
    private lateinit var drawerSeparator: View
    private val navItemTextViews = mutableListOf<TextView>()
    private lateinit var drawerFuturisticToggle: FuturisticToggleView
    private lateinit var drawerInspectorPanel: LinearLayout
    private lateinit var settingsIcon: SettingsIconView
    private lateinit var recentPinsList: LinearLayout
    private var searchPinsInput: EditText? = null
    private var searchConvosInput: EditText? = null
    private var searchConvoWrap: LinearLayout? = null

    // Home screen view references for live theme switching
    private var homeGridBg: GridBackgroundView? = null
    private var homeMenuIcon: MenuIconView? = null

    // Chat UI fields
    private lateinit var msgList: LinearLayout
    private lateinit var chatScroller: ScrollView
    private lateinit var chatRecycler: androidx.recyclerview.widget.RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private val chatMessages = mutableListOf<ChatMessage>()
    private lateinit var chatInput: EditText
    private lateinit var chatBtnSend: FrameLayout
    private lateinit var chatBtnStop: FrameLayout
    private lateinit var chatThinkingText: TextView
    private lateinit var chatPlaceholderText: TextView
    private lateinit var chatAttachPanel: LinearLayout
    private lateinit var chatPlusBtn: View
    private lateinit var chatImagePreviewContainer: FrameLayout
    private lateinit var chatImagePreviewImg: ImageView
    private lateinit var chatDocPreviewContainer: LinearLayout
    private lateinit var newChatBtn: TextView
    private lateinit var chatDocPreviewName: TextView
    private lateinit var chatResultOverlay: FrameLayout
    private lateinit var chatHeaderTitle: TextView
    private val chatHistory = ArrayList<Pair<String, String>>()
    private var chatBusy = false
    private var chatCurrentCall: okhttp3.Call? = null
    private var currentChatId: String = ""
    private var chatCameraPhotoPath: String? = null
    private var pendingImageFile: java.io.File? = null
    private var pendingDocumentContent: String? = null
    private var pendingDocumentName: String? = null
    private var drawerWidth = 0
    private val ui = Handler(Looper.getMainLooper())
    private val http = okhttp3.OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    private val chatScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main + kotlinx.coroutines.SupervisorJob())

    // Result screen view references for live theme switching
    private var seoTitleText: TextView? = null
    private var seoDescText: TextView? = null
    private var seoAltText: TextView? = null
    private var pinReadyTitle: TextView? = null
    private var generateAgainBtn: TextView? = null
    private var seoAffiliateLink: TextView? = null

    private val lightBg = Color.rgb(250, 249, 247)
    private val lightText = Color.rgb(28, 28, 28)
    private val lightMuted = Color.rgb(100, 97, 93)
    private val lightInput = Color.WHITE

    private val darkBg = Color.rgb(18, 18, 18)
    private val darkText = Color.rgb(245, 245, 243)
    private val darkMuted = Color.rgb(155, 155, 150)
    private val darkInput = Color.rgb(30, 30, 30)
override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(applicationContext)

        prefs = getSharedPreferences("pinster", MODE_PRIVATE)

        darkMode = prefs.getBoolean("dark_mode", false)

        currentChatId = "chat_${System.currentTimeMillis()}"

        loadHistory()
        buildInterface()
    }

    override fun onResume() {
        super.onResume()
        if (::recentPinsList.isInitialized) populateRecentPins()
    }

    override fun onDestroy() {
        super.onDestroy()
        chatScope.cancel()
    }

    private fun bg(): Int =
        if (darkMode) darkBg else lightBg

    private fun text(): Int =
        if (darkMode) darkText else lightText

    private fun muted(): Int =
        if (darkMode) darkMuted else lightMuted

    private fun containerBg(): Int = if (darkMode) Color.rgb(30, 30, 30) else Color.WHITE

    private fun sendBtnIcon(): Int = if (darkMode) Color.WHITE else Color.rgb(28, 28, 28)

    private fun inputColor(): Int =
        if (darkMode) darkInput else lightInput

    private fun accentBlue(): Int = Color.rgb(100, 160, 255)

    private fun updateSystemBars() {

        window.statusBarColor = bg()
        window.navigationBarColor = bg()

        window.decorView.systemUiVisibility =
            if (darkMode) 0
            else View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
    }

    private fun updateDrawerColors() {
        // Drawer background
        drawer.setBackgroundColor(if (darkMode) Color.rgb(22, 22, 22) else Color.WHITE)
        // Title
        drawerTitle.setTextColor(text())
        // Separator
        drawerSeparator.setBackgroundColor(if (darkMode) Color.rgb(55, 55, 55) else Color.rgb(225, 223, 220))
        // Sync toggle
        drawerFuturisticToggle.setOn(darkMode)
        // Settings icon
        settingsIcon.invalidate()
        // Nav item text colors
        navItemTextViews.forEach { it.setTextColor(text()) }
        // New Chat button
        newChatBtn.setTextColor(if (darkMode) Color.BLACK else Color.WHITE)
        newChatBtn.background = GradientDrawable().apply {
            setColor(if (darkMode) Color.WHITE else Color.rgb(28, 28, 28))
            cornerRadius = dp(14).toFloat()
        }
        // Search input hints and text
        if (searchConvosInput != null) {
            searchConvosInput!!.setTextColor(text())
            searchConvosInput!!.setHintTextColor(muted())
        }
        // Rebuild conversation list with new colors
        populateInspectorPanel()
    }

    private fun updateHomeScreenColors() {
        // Root background
        root.setBackgroundColor(bg())
        // Grid background
        homeGridBg?.invalidate()
        // Menu icon
        homeMenuIcon?.setLineColor(muted())
        // Chat header
        if (::chatHeaderTitle.isInitialized) chatHeaderTitle.setTextColor(text())
        // Chat scroller background
        if (::chatScroller.isInitialized) chatScroller.setBackgroundColor(if (darkMode) Color.argb(200, 18, 18, 18) else Color.argb(200, 250, 249, 247))
        // Placeholder and thinking text
        if (::chatPlaceholderText.isInitialized) chatPlaceholderText.setTextColor(muted())
        if (::chatThinkingText.isInitialized) chatThinkingText.setTextColor(muted())
        // Chat input
        if (::chatInput.isInitialized) {
            chatInput.setTextColor(text())
            chatInput.setHintTextColor(muted())
        }
        // + and send icons
        if (::chatPlusBtn.isInitialized) chatPlusBtn.invalidate()
        if (::chatBtnSend.isInitialized) {
            chatBtnSend.invalidate()
            if (chatBtnSend.childCount > 0) chatBtnSend.getChildAt(0).invalidate()
        }
        if (::chatBtnStop.isInitialized) chatBtnStop.invalidate()
        // Collapsible panel text colors
        if (::chatAttachPanel.isInitialized) {
            for (i in 0 until chatAttachPanel.childCount) {
                val child = chatAttachPanel.getChildAt(i)
                if (child is LinearLayout) {
                    for (j in 0 until child.childCount) {
                        val inner = child.getChildAt(j)
                        if (inner is TextView) inner.setTextColor(text())
                    }
                }
            }
        }
        // Retroactively update all existing message text colors
        if (::msgList.isInitialized) {
            for (i in 0 until msgList.childCount) {
                val child = msgList.getChildAt(i)
                if (child is LinearLayout) {
                    for (j in 0 until child.childCount) {
                        val inner = child.getChildAt(j)
                        if (inner is TextView) inner.setTextColor(text())
                    }
                }
            }
        }
        // Result screen views
        pinReadyTitle?.setTextColor(text())
        seoTitleText?.setTextColor(text())
        seoDescText?.setTextColor(text())
        seoAltText?.setTextColor(text())
        seoAffiliateLink?.setTextColor(text())
        generateAgainBtn?.apply {
            setTextColor(if (darkMode) Color.BLACK else Color.WHITE)
            background = rounded(if (darkMode) Color.WHITE else Color.rgb(28, 28, 28), 18)
        }
        // Re-render result screen if active
        val seResult = currentSeoResult
        val sAngles = currentAngles
        if (seResult != null && sAngles != null) {
            showPinResult(seResult, sAngles, saveToHistory = false)
        }
    }

    private fun updateChatColors() {
        if (::chatRecycler.isInitialized) {
            chatRecycler.setBackgroundColor(if (darkMode) Color.argb(200, 18, 18, 18) else Color.argb(200, 250, 249, 247))
            chatRecycler.recycledViewPool.clear()
            chatAdapter.notifyDataSetChanged()
        }
    }

    private class GridBackgroundView(
        context: android.content.Context
    ) : View(context) {

        private val gridPaint = android.graphics.Paint(
            android.graphics.Paint.ANTI_ALIAS_FLAG
        ).apply {
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 1f
        }

        private val lightPaint = android.graphics.Paint(
            android.graphics.Paint.ANTI_ALIAS_FLAG
        ).apply {
            style = android.graphics.Paint.Style.STROKE
            strokeCap = android.graphics.Paint.Cap.ROUND
        }

        private data class Light(
            var x: Float,
            var y: Float,
            var horizontal: Boolean,
            var speed: Float,
            var length: Float,
            var color: Int,
            var alpha: Int
        )

        private val lights = mutableListOf<Light>()
        private var initialized = false
        private var lastTime = 0L

        private fun darkMode(): Boolean {
            return context.getSharedPreferences(
                "pinster",
                android.content.Context.MODE_PRIVATE
            ).getBoolean("dark_mode", false)
        }

        private fun randomColor(): Int {
            val colors = if (darkMode()) {
                intArrayOf(
                    android.graphics.Color.rgb(180, 220, 255),
                    android.graphics.Color.rgb(205, 190, 255),
                    android.graphics.Color.rgb(170, 245, 220),
                    android.graphics.Color.rgb(255, 220, 170)
                )
            } else {
                intArrayOf(
                    android.graphics.Color.rgb(70, 125, 190),
                    android.graphics.Color.rgb(105, 90, 165),
                    android.graphics.Color.rgb(55, 145, 125),
                    android.graphics.Color.rgb(185, 125, 65)
                )
            }

            return colors[
                kotlin.random.Random.nextInt(colors.size)
            ]
        }

        private fun createLight() {
            if (width <= 0 || height <= 0) return

            val density = resources.displayMetrics.density
            val size = 32f * density

            val horizontal =
                kotlin.random.Random.nextBoolean()

            val position =
                if (horizontal) {
                    (kotlin.random.Random.nextInt(
                        1,
                        (height / size).toInt().coerceAtLeast(2)
                    ) * size)
                } else {
                    (kotlin.random.Random.nextInt(
                        1,
                        (width / size).toInt().coerceAtLeast(2)
                    ) * size)
                }

            lights.add(
                Light(
                    x = if (horizontal) -120f else position,
                    y = if (horizontal) position else -120f,
                    horizontal = horizontal,
                    speed = (45f + kotlin.random.Random.nextFloat() * 50f) * density,
                    length = (22f + kotlin.random.Random.nextFloat() * 33f) * density,
                    color = randomColor(),
                    alpha = kotlin.random.Random.nextInt(15, 35)
                )
            )
        }

        override fun onSizeChanged(
            w: Int,
            h: Int,
            oldw: Int,
            oldh: Int
        ) {
            super.onSizeChanged(w, h, oldw, oldh)

            if (!initialized) {
                repeat(7) {
                    createLight()
                }
                initialized = true
            }
        }

        override fun onDraw(canvas: android.graphics.Canvas) {
            super.onDraw(canvas)

            val density = resources.displayMetrics.density
            val size = 32f * density
            val dark = darkMode()

            /*
             * FADED GRID
             * Strongest near the center, almost invisible at edges.
             */
            var x = 0f
            while (x <= width) {
                val edge =
                    kotlin.math.abs(
                        x - width / 2f
                    ) / (width / 2f).coerceAtLeast(1f)

                val fade =
                    (1f - edge).coerceIn(0f, 1f)

                gridPaint.color =
                    if (dark) {
                        android.graphics.Color.argb(
                            (20f * fade * fade).toInt(),
                            255,
                            255,
                            255
                        )
                    } else {
                        android.graphics.Color.argb(
                            (14f * fade * fade).toInt(),
                            35,
                            35,
                            35
                        )
                    }

                canvas.drawLine(
                    x,
                    0f,
                    x,
                    height.toFloat(),
                    gridPaint
                )

                x += size
            }

            var y = 0f
            while (y <= height) {
                val edge =
                    kotlin.math.abs(
                        y - height / 2f
                    ) / (height / 2f).coerceAtLeast(1f)

                val fade =
                    (1f - edge).coerceIn(0f, 1f)

                gridPaint.color =
                    if (dark) {
                        android.graphics.Color.argb(
                            (20f * fade * fade).toInt(),
                            255,
                            255,
                            255
                        )
                    } else {
                        android.graphics.Color.argb(
                            (14f * fade * fade).toInt(),
                            35,
                            35,
                            35
                        )
                    }

                canvas.drawLine(
                    0f,
                    y,
                    width.toFloat(),
                    y,
                    gridPaint
                )

                y += size
            }

            /*
             * MOVING LIGHT STREAKS
             * Small glowing lines travelling along grid paths.
             */
            val now = android.os.SystemClock.uptimeMillis()

            if (lastTime == 0L) {
                lastTime = now
            }

            val delta =
                ((now - lastTime).coerceAtMost(40L)) / 1000f

            lastTime = now

            val iterator = lights.iterator()

            while (iterator.hasNext()) {
                val light = iterator.next()

                if (light.horizontal) {
                    light.x += light.speed * delta

                    if (light.x - light.length > width) {
                        iterator.remove()
                        continue
                    }

                    val startX = light.x - light.length

                    // soft trail
                    lightPaint.color =
                        android.graphics.Color.argb(
                            (light.alpha * 0.20f).toInt(),
                            android.graphics.Color.red(light.color),
                            android.graphics.Color.green(light.color),
                            android.graphics.Color.blue(light.color)
                        )
                    lightPaint.strokeWidth = 5f * density

                    canvas.drawLine(
                        startX,
                        light.y,
                        light.x,
                        light.y,
                        lightPaint
                    )

                    // brighter core
                    lightPaint.color =
                        android.graphics.Color.argb(
                            (light.alpha * 0.65f).toInt(),
                            android.graphics.Color.red(light.color),
                            android.graphics.Color.green(light.color),
                            android.graphics.Color.blue(light.color)
                        )
                    lightPaint.strokeWidth = 1.4f * density

                    canvas.drawLine(
                        startX,
                        light.y,
                        light.x,
                        light.y,
                        lightPaint
                    )

                } else {
                    light.y += light.speed * delta

                    if (light.y - light.length > height) {
                        iterator.remove()
                        continue
                    }

                    val startY = light.y - light.length

                    lightPaint.color =
                        android.graphics.Color.argb(
                            (light.alpha * 0.20f).toInt(),
                            android.graphics.Color.red(light.color),
                            android.graphics.Color.green(light.color),
                            android.graphics.Color.blue(light.color)
                        )
                    lightPaint.strokeWidth = 5f * density

                    canvas.drawLine(
                        light.x,
                        startY,
                        light.x,
                        light.y,
                        lightPaint
                    )

                    lightPaint.color =
                        android.graphics.Color.argb(
                            (light.alpha * 0.65f).toInt(),
                            android.graphics.Color.red(light.color),
                            android.graphics.Color.green(light.color),
                            android.graphics.Color.blue(light.color)
                        )
                    lightPaint.strokeWidth = 1.4f * density

                    canvas.drawLine(
                        light.x,
                        startY,
                        light.x,
                        light.y,
                        lightPaint
                    )
                }
            }

            if (lights.size < 7 &&
                kotlin.random.Random.nextFloat() < 0.025f
            ) {
                createLight()
            }

            postInvalidateOnAnimation()
        }
    }

    private class MenuIconView(
        context: android.content.Context
    ) : View(context) {

        private val paint = android.graphics.Paint(
            android.graphics.Paint.ANTI_ALIAS_FLAG
        ).apply {
            strokeWidth = 2.6f
            strokeCap = android.graphics.Paint.Cap.SQUARE
            style = android.graphics.Paint.Style.STROKE
        }

        private var progress = 0f

        fun setLineColor(color: Int) {
            paint.color = color
            invalidate()
        }

        fun setProgress(value: Float) {
            progress = value.coerceIn(0f, 1f)
            invalidate()
        }

        fun animateToClose() {
            android.animation.ValueAnimator.ofFloat(progress, 1f).apply {
                duration = 240L
                addUpdateListener {
                    progress = it.animatedValue as Float
                    invalidate()
                }
                start()
            }
        }

        fun animateToMenu() {
            android.animation.ValueAnimator.ofFloat(progress, 0f).apply {
                duration = 200L
                addUpdateListener {
                    progress = it.animatedValue as Float
                    invalidate()
                }
                start()
            }
        }

        override fun onDraw(canvas: android.graphics.Canvas) {
            super.onDraw(canvas)

            val cx = width / 2f
            val cy = height / 2f

            val halfLength = dpLocal(11f)
            val separation = dpLocal(5f)

            val gap = separation * (1f - progress)
            val rotation = 45f * progress

            // TOP LINE
            canvas.save()

            canvas.rotate(
                rotation,
                cx,
                cy
            )

            canvas.drawLine(
                cx - halfLength,
                cy - gap,
                cx + halfLength,
                cy - gap,
                paint
            )

            canvas.restore()

            // BOTTOM LINE
            canvas.save()

            canvas.rotate(
                -rotation,
                cx,
                cy
            )

            canvas.drawLine(
                cx - halfLength,
                cy + gap,
                cx + halfLength,
                cy + gap,
                paint
            )

            canvas.restore()
        }

        private fun dpLocal(value: Float): Float {
            return value * resources.displayMetrics.density
        }
    }

    // FUTURISTIC DARK MODE TOGGLE
    private class FuturisticToggleView(context: android.content.Context) : View(context) {
        var isOn = false
        var onToggle: (() -> Unit)? = null

        private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val knobPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private var knobX = 0f
        private val trackW = dpLocal(48f)
        private val trackH = dpLocal(26f)
        private val knobR = dpLocal(9f)
        private val trackR = dpLocal(13f)

        init {
            setOnClickListener {
                isOn = !isOn
                animateKnob()
                onToggle?.invoke()
            }
        }

        fun setOn(isOn: Boolean, animate: Boolean = false) {
            this.isOn = isOn
            if (animate) animateKnob() else {
                knobX = if (isOn) trackW - knobR * 2 - dpLocal(4f) else dpLocal(4f)
                invalidate()
            }
        }

        private fun animateKnob() {
            val target = if (isOn) trackW - knobR * 2 - dpLocal(4f) else dpLocal(4f)
            val start = knobX
            val anim = ValueAnimator.ofFloat(start, target)
            anim.duration = 200L
            anim.interpolator = DecelerateInterpolator()
            anim.addUpdateListener {
                knobX = it.animatedValue as Float
                invalidate()
            }
            anim.start()
        }

        override fun onMeasure(w: Int, h: Int) {
            setMeasuredDimension(trackW.toInt(), trackH.toInt())
        }

        override fun onDraw(c: Canvas) {
            val cy = trackH / 2f
            val trackRect = android.graphics.RectF(0f, 0f, trackW, trackH)
            // Track
            trackPaint.color = if (isOn) Color.WHITE else Color.rgb(50, 50, 50)
            c.drawRoundRect(trackRect, trackR, trackR, trackPaint)
            // Knob
            knobPaint.color = if (isOn) Color.rgb(30, 30, 30) else Color.rgb(120, 120, 120)
            c.drawCircle(knobX + knobR, cy, knobR, knobPaint)
        }

        private fun dpLocal(v: Float): Float = v * resources.displayMetrics.density
    }

    // Minimal search icon (circle + line handle)
    private inner class SearchIconView(context: android.content.Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = 2.6f
            strokeCap = Paint.Cap.SQUARE
            style = Paint.Style.STROKE
            color = muted()
        }

        override fun onDraw(c: Canvas) {
            val d = resources.displayMetrics.density
            val cx = width / 2f - 2f * d
            val cy = height / 2f - 2f * d
            val r = 6f * d
            paint.color = muted()
            // Circle (lens)
            c.drawCircle(cx, cy, r, paint)
            // Handle line
            val hx = cx + r * 0.707f
            val hy = cy + r * 0.707f
            c.drawLine(hx, hy, hx + 4f * d, hy + 4f * d, paint)
        }

        override fun onMeasure(w: Int, h: Int) {
            val size = (24 * resources.displayMetrics.density).toInt()
            setMeasuredDimension(size, size)
        }
    }

    // Settings icon: 2 vertical dots
    private inner class SettingsIconView(context: android.content.Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = 2.4f
            strokeCap = Paint.Cap.ROUND
            style = Paint.Style.STROKE
        }

        override fun onDraw(c: Canvas) {
            val d = resources.displayMetrics.density
            paint.color = muted()
            val r = 4.2f * d
            val gap = 13f * d
            val cx = width / 2f
            val cy = height / 2f
            c.drawCircle(cx, cy - gap / 2, r, paint)
            c.drawCircle(cx, cy + gap / 2, r, paint)
        }

        override fun onMeasure(w: Int, h: Int) {
            val size = (28 * resources.displayMetrics.density).toInt()
            setMeasuredDimension(size, size)
        }
    }

    // Nav icon: Pin (circle + pointed line)
    private inner class NavPinIconView(context: android.content.Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = 2f
            strokeCap = Paint.Cap.ROUND
            style = Paint.Style.STROKE
        }

        override fun onDraw(c: Canvas) {
            val d = resources.displayMetrics.density
            paint.color = muted()
            val cx = width / 2f
            val cy = 7f * d
            val r = 4.5f * d
            c.drawCircle(cx, cy, r, paint)
            c.drawLine(cx, cy + r, cx, cy + r + 11f * d, paint)
        }

        override fun onMeasure(w: Int, h: Int) {
            val size = (24 * resources.displayMetrics.density).toInt()
            setMeasuredDimension(size, size)
        }
    }

    // Nav icon: Folder
    private inner class NavFolderIconView(context: android.content.Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = 2f
            strokeCap = Paint.Cap.ROUND
            style = Paint.Style.STROKE
        }

        override fun onDraw(c: Canvas) {
            val d = resources.displayMetrics.density
            paint.color = muted()
            val left = 2f * d
            val top = 5f * d
            val right = 22f * d
            val bottom = 19f * d
            val tabW = 7f * d
            val tabH = 3f * d
            // Tab
            c.drawLine(left, top + tabH, left + tabW, top + tabH, paint)
            c.drawLine(left + tabW, top + tabH, left + tabW + 2f * d, top, paint)
            c.drawLine(left + tabW + 2f * d, top, right - 3f * d, top, paint)
            // Body
            val path = android.graphics.Path()
            path.moveTo(left, top + tabH)
            path.lineTo(left, bottom)
            path.lineTo(right, bottom)
            path.lineTo(right, top)
            c.drawPath(path, paint)
        }

        override fun onMeasure(w: Int, h: Int) {
            val size = (24 * resources.displayMetrics.density).toInt()
            setMeasuredDimension(size, size)
        }
    }

    // Nav icon: Document (rectangle + folded corner)
    private inner class NavDocIconView(context: android.content.Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = 2f
            strokeCap = Paint.Cap.ROUND
            style = Paint.Style.STROKE
        }

        override fun onDraw(c: Canvas) {
            val d = resources.displayMetrics.density
            paint.color = muted()
            val left = 4f * d
            val top = 2f * d
            val right = 20f * d
            val bottom = 22f * d
            val fold = 5f * d
            val path = android.graphics.Path()
            path.moveTo(left, top)
            path.lineTo(right - fold, top)
            path.lineTo(right, top + fold)
            path.lineTo(right, bottom)
            path.lineTo(left, bottom)
            path.close()
            c.drawPath(path, paint)
            // Fold line
            c.drawLine(right - fold, top, right - fold, top + fold, paint)
            c.drawLine(right - fold, top + fold, right, top + fold, paint)
        }

        override fun onMeasure(w: Int, h: Int) {
            val size = (24 * resources.displayMetrics.density).toInt()
            setMeasuredDimension(size, size)
        }
    }

    // Popup icon: Pin (smaller)
    private inner class PopupPinIconView(context: android.content.Context, private val iconColor: Int) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = 1.5f
            strokeCap = Paint.Cap.ROUND
            style = Paint.Style.STROKE
        }

        override fun onDraw(c: Canvas) {
            val d = resources.displayMetrics.density
            paint.color = iconColor
            val cx = width / 2f
            val cy = 5f * d
            val r = 3.5f * d
            c.drawCircle(cx, cy, r, paint)
            c.drawLine(cx, cy + r, cx, cy + r + 8f * d, paint)
        }

        override fun onMeasure(w: Int, h: Int) {
            val size = (20 * resources.displayMetrics.density).toInt()
            setMeasuredDimension(size, size)
        }
    }

    // Popup icon: Pencil
    private inner class PopupPencilIconView(context: android.content.Context, private val iconColor: Int) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = 1.5f
            strokeCap = Paint.Cap.ROUND
            style = Paint.Style.STROKE
        }

        override fun onDraw(c: Canvas) {
            val d = resources.displayMetrics.density
            paint.color = iconColor
            val path = android.graphics.Path()
            // Pencil body (slanted rectangle)
            path.moveTo(4f * d, 17f * d)
            path.lineTo(14f * d, 3f * d)
            path.lineTo(17f * d, 5f * d)
            path.lineTo(7f * d, 19f * d)
            path.close()
            c.drawPath(path, paint)
            // Tip
            c.drawLine(4f * d, 17f * d, 2f * d, 19f * d, paint)
        }

        override fun onMeasure(w: Int, h: Int) {
            val size = (20 * resources.displayMetrics.density).toInt()
            setMeasuredDimension(size, size)
        }
    }

    // Popup icon: Trash
    private inner class PopupTrashIconView(context: android.content.Context, private val iconColor: Int) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = 1.5f
            strokeCap = Paint.Cap.ROUND
            style = Paint.Style.STROKE
        }

        override fun onDraw(c: Canvas) {
            val d = resources.displayMetrics.density
            paint.color = iconColor
            // Lid
            c.drawLine(4f * d, 5f * d, 16f * d, 5f * d, paint)
            c.drawLine(8f * d, 3f * d, 12f * d, 3f * d, paint)
            // Body
            val path = android.graphics.Path()
            path.moveTo(5f * d, 5f * d)
            path.lineTo(6f * d, 18f * d)
            path.lineTo(14f * d, 18f * d)
            path.lineTo(15f * d, 5f * d)
            c.drawPath(path, paint)
            // Lines inside
            c.drawLine(8f * d, 8f * d, 8f * d, 15f * d, paint)
            c.drawLine(12f * d, 8f * d, 12f * d, 15f * d, paint)
        }

        override fun onMeasure(w: Int, h: Int) {
            val size = (20 * resources.displayMetrics.density).toInt()
            setMeasuredDimension(size, size)
        }
    }

    // Animated loading lines for buttons
    private inner class LoadingLinesView(context: android.content.Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = 2.6f
            strokeCap = Paint.Cap.SQUARE
            style = Paint.Style.FILL
            color = muted()
        }
        private val handler = Handler(Looper.getMainLooper())
        private var animating = false

        fun start() { animating = true; animateFrame() }
        fun stop() { animating = false; handler.removeCallbacksAndMessages(null) }

        private fun animateFrame() {
            if (!animating) return
            invalidate()
            handler.postDelayed({ animateFrame() }, 50)
        }

        override fun onDraw(c: Canvas) {
            val d = resources.displayMetrics.density
            val cx = width / 2f
            val cy = height / 2f
            val lineW = 8f * d
            val gap = 4f * d
            val h = 2f * d
            val totalW = 3 * lineW + 2 * gap
            val startX = cx - totalW / 2f
            val t = System.currentTimeMillis()

            for (i in 0..2) {
                val phase = ((t / 180) + i * 100) % 600
                val alpha = if (phase < 300) (phase / 300f) else (1f - (phase - 300) / 300f)
                paint.alpha = (alpha * 255).toInt().coerceIn(60, 255)
                paint.color = muted()
                val x = startX + i * (lineW + gap)
                c.drawRect(x, cy - h / 2f, x + lineW, cy + h / 2f, paint)
            }
        }

        override fun onMeasure(w: Int, h: Int) {
            val size = (40 * resources.displayMetrics.density).toInt()
            setMeasuredDimension(size, (12 * resources.displayMetrics.density).toInt())
        }
    }

    // Circle with + icon for save button
    private inner class CirclePlusView(context: android.content.Context) : View(context) {
        private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = 2.6f
            strokeCap = Paint.Cap.SQUARE
            style = Paint.Style.STROKE
            color = muted()
        }
        private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = 2.6f
            strokeCap = Paint.Cap.SQUARE
            style = Paint.Style.STROKE
            color = muted()
        }

        override fun onDraw(c: Canvas) {
            val d = resources.displayMetrics.density
            val cx = width / 2f
            val cy = height / 2f
            val r = 8f * d
            strokePaint.color = muted()
            c.drawCircle(cx, cy, r, strokePaint)
            val hl = 4f * d
            linePaint.color = muted()
            c.drawLine(cx, cy - hl, cx, cy + hl, linePaint)
            c.drawLine(cx - hl, cy, cx + hl, cy, linePaint)
        }

        override fun onMeasure(w: Int, h: Int) {
            val size = (24 * resources.displayMetrics.density).toInt()
            setMeasuredDimension(size, size)
        }
    }

    private fun buildInterface() {

        updateSystemBars()

        root = FrameLayout(this)
        root.setBackgroundColor(bg())

        // SUBTLE ANIMATED GRID BACKGROUND
        val gridBackground = GridBackgroundView(this)
        homeGridBg = gridBackground

        root.addView(
            gridBackground,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, 0, 0, 0)
        }

        // ── CHAT HEADER ──
        val hdr = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(6))
        }

        chatHeaderTitle = TextView(this).apply {
            text = getString(R.string.chat_header_title)
            textSize = 14f
            setTextColor(text())
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        hdr.addView(chatHeaderTitle)

        content.addView(hdr, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        // ── MESSAGES ──
        chatScroller = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            setBackgroundColor(if (darkMode) Color.argb(200, 18, 18, 18) else Color.argb(200, 250, 249, 247))
            visibility = View.GONE
        }

        msgList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(6), dp(10), dp(6))
        }

        chatScroller.addView(msgList)

        chatRecycler = androidx.recyclerview.widget.RecyclerView(this).apply {
            setBackgroundColor(if (darkMode) Color.argb(200, 18, 18, 18) else Color.argb(200, 250, 249, 247))
            layoutManager = LinearLayoutManager(this@MainActivity).apply { stackFromEnd = true }
            setHasFixedSize(false)
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.hideSoftInputFromWindow(chatInput.windowToken, 0)
                    }
                }
            })
        }
        chatAdapter = ChatAdapter(chatMessages, { darkMode }) { position ->
            retryFailedMessage(position)
        }
        chatRecycler.adapter = chatAdapter

        content.addView(chatRecycler, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        // Placeholder - add as first message in adapter
        chatPlaceholderText = TextView(this).apply {
            text = getString(R.string.chat_placeholder)
            textSize = 15f
            setTextColor(muted())
            gravity = Gravity.CENTER
            setPadding(dp(32), dp(80), dp(32), dp(32))
        }
        // Add placeholder as initial message
        chatMessages.add(ChatMessage(ChatMessage.ROLE_ASSISTANT, getString(R.string.chat_placeholder)))
        chatAdapter.notifyItemInserted(0)

        // ── THINKING ──
        chatThinkingText = TextView(this).apply {
            text = getString(R.string.thinking)
            textSize = 12f
            setTextColor(muted())
            typeface = Typeface.MONOSPACE
            setPadding(dp(16), dp(4), dp(16), dp(4))
            visibility = View.GONE
        }
        content.addView(chatThinkingText, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        // ── INPUT AREA ──
        val inputContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(6), dp(14), dp(6))
            background = GradientDrawable().apply {
                setColor(Color.TRANSPARENT)
                cornerRadius = dp(20).toFloat()
                setStroke(1, if (darkMode) Color.rgb(55, 55, 55) else Color.rgb(210, 208, 205))
            }
        }

        // Attach panel
        chatAttachPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(dp(8), dp(4), dp(8), dp(4))
        }
        fun addAttachOption(label: String, onClick: () -> Unit) {
            val row = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(12), dp(10), dp(12), dp(10))
                setOnClickListener { onClick(); chatAttachPanel.visibility = View.GONE }
            }
            row.addView(TextView(this@MainActivity).apply { text = label; textSize = 13f; setTextColor(text()); typeface = Typeface.MONOSPACE })
            chatAttachPanel.addView(row)
        }
        addAttachOption("Upload Photo") { pickImage() }
        addAttachOption("Take a Picture") { takePicture() }
        addAttachOption("Attach File") { pickDocument() }
        inputContainer.addView(chatAttachPanel)

        // Image preview
        chatImagePreviewContainer = FrameLayout(this).apply { visibility = View.GONE }
        val imgPrevCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(6), dp(8), dp(6))
            background = GradientDrawable().apply { setColor(containerBg()); cornerRadius = dp(12).toFloat() }
        }
        chatImagePreviewImg = ImageView(this@MainActivity).apply {
            layoutParams = LinearLayout.LayoutParams(dp(60), dp(60)); scaleType = ImageView.ScaleType.CENTER_CROP
        }
        imgPrevCard.addView(chatImagePreviewImg)
        imgPrevCard.addView(TextView(this@MainActivity).apply {
            text = "✕"; textSize = 14f; setTextColor(muted()); setPadding(dp(12), 0, 0, 0)
            setOnClickListener { clearPendingImage() }
        })
        chatImagePreviewContainer.addView(imgPrevCard)
        inputContainer.addView(chatImagePreviewContainer)

        // Document preview
        chatDocPreviewContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            visibility = View.GONE; setPadding(dp(8), dp(6), dp(8), dp(6))
            background = GradientDrawable().apply { setColor(containerBg()); cornerRadius = dp(12).toFloat() }
        }
        chatDocPreviewName = TextView(this@MainActivity).apply {
            textSize = 12f; setTextColor(text()); typeface = Typeface.MONOSPACE; maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END; layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        chatDocPreviewContainer.addView(chatDocPreviewName)
        chatDocPreviewContainer.addView(TextView(this@MainActivity).apply {
            text = "✕"; textSize = 14f; setTextColor(muted()); setPadding(dp(12), 0, 0, 0)
            setOnClickListener { clearPendingDocument() }
        })
        inputContainer.addView(chatDocPreviewContainer)

        // Input row
        val irow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        chatPlusBtn = object : View(this@MainActivity) {
            private val p = Paint(Paint.ANTI_ALIAS_FLAG)
            override fun onDraw(c: Canvas) {
                p.color = text(); p.strokeWidth = 1.2f * resources.displayMetrics.density; p.strokeCap = Paint.Cap.SQUARE
                val cx = width / 2f; val cy = height / 2f; val s = 10f * resources.displayMetrics.density
                c.drawLine(cx - s, cy, cx + s, cy, p); c.drawLine(cx, cy - s, cx, cy + s, p)
            }
            override fun onMeasure(w: Int, h: Int) { val sz = dp(28); setMeasuredDimension(sz, sz) }
        }.apply { setOnClickListener { toggleChatAttachPanel() } }
        irow.addView(chatPlusBtn)

        chatInput = EditText(this).apply {
            hint = getString(R.string.chat_hint)
            textSize = 14f; setTextColor(text()); setHintTextColor(muted())
            typeface = Typeface.MONOSPACE; isSingleLine = false; maxLines = 4
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            setPadding(dp(10), dp(8), dp(10), dp(8))
            background = null
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) { sendChat(); true } else false
            }
        }
        irow.addView(chatInput, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        chatBtnSend = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(34), dp(34))
            setPadding(dp(4), dp(4), dp(4), dp(4))
            setOnClickListener { sendChat() }
        }
        chatBtnSend.addView(object : View(this@MainActivity) {
            private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.SQUARE; strokeWidth = 1.2f * resources.displayMetrics.density }
            override fun onDraw(c: Canvas) {
                p.color = sendBtnIcon()
                val cx = width / 2f; val cy = height / 2f
                val half = 11f * resources.displayMetrics.density
                c.drawLine(cx - half, cy, cx + half, cy, p)
                c.drawLine(cx + half, cy, cx + half - half * 0.5f, cy - half * 0.5f, p)
                c.drawLine(cx + half, cy, cx + half - half * 0.5f, cy + half * 0.5f, p)
            }
            override fun onMeasure(w: Int, h: Int) { setMeasuredDimension(dp(26), dp(26)) }
        })
        irow.addView(chatBtnSend)

        chatBtnStop = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(34), dp(34))
            setPadding(dp(4), dp(4), dp(4), dp(4))
            visibility = View.GONE
            setOnClickListener { stopGeneration() }
        }
        chatBtnStop.addView(object : View(this@MainActivity) {
            private val p = Paint(Paint.ANTI_ALIAS_FLAG)
            override fun onDraw(c: Canvas) {
                p.color = sendBtnIcon(); p.style = Paint.Style.STROKE; p.strokeWidth = 1.2f * resources.displayMetrics.density
                val r = 9f * resources.displayMetrics.density
                c.drawCircle(width / 2f, height / 2f, r, p)
            }
        })
        irow.addView(chatBtnStop)

        inputContainer.addView(irow)
        content.addView(inputContainer, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            leftMargin = dp(14)
            rightMargin = dp(14)
            topMargin = dp(10)
            bottomMargin = dp(10)
        })

        root.addView(
            content,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        buildDrawer()

        setContentView(root)

        setupDrawerGestures()
    }

    private fun buildDrawer() {
        drawerWidth = (resources.displayMetrics.widthPixels * 0.80).toInt()

        drawer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(
                if (darkMode) Color.rgb(22, 22, 22)
                else Color.WHITE
            )
            setPadding(
                dp(24),
                dp(28),
                dp(24),
                dp(18)
            )
            elevation = dp(12).toFloat()
        }

        // TOP ROW
        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        drawerTitle = TextView(this).apply {
            text = "DELISH STUDIO"
            textSize = 13f
            setTextColor(text())
            typeface = Typeface.MONOSPACE
            letterSpacing = 0.16f

            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                weight = 1f
            }
        }

        topRow.addView(drawerTitle)

        // FUTURISTIC DARK MODE TOGGLE
        drawerFuturisticToggle = FuturisticToggleView(this).apply {
            isOn = darkMode
            onToggle = {
                darkMode = isOn
                prefs.edit().putBoolean("dark_mode", darkMode).apply()
                updateHomeScreenColors()
                updateDrawerColors()
                updateSystemBars()
                updateChatColors()
            }
        }
        drawerFuturisticToggle.setOn(darkMode)

        topRow.addView(
            drawerFuturisticToggle,
            LinearLayout.LayoutParams(
                dp(48),
                dp(26)
            )
        )

        drawer.addView(topRow)

        // ONE CLEAN LINE
        drawerSeparator = View(this).apply {
            setBackgroundColor(
                if (darkMode)
                    Color.rgb(55, 55, 55)
                else
                    Color.rgb(225, 223, 220)
            )
        }

        drawer.addView(
            drawerSeparator,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(1)
            ).apply {
                topMargin = dp(18)
                bottomMargin = dp(18)
            }
        )

        addSpace(drawer, 14)

        // Search convos
        searchConvoWrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        val searchConvoRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(2), dp(8), dp(2))
            background = GradientDrawable().apply {
                setColor(if (darkMode) Color.rgb(35, 35, 35) else Color.rgb(240, 239, 237))
                cornerRadius = dp(10).toFloat()
            }
        }
        val searchConvoIcon = SearchIconView(this)
        searchConvoRow.addView(searchConvoIcon, LinearLayout.LayoutParams(dp(28), dp(28)))

        searchConvosInput = EditText(this).apply {
            hint = "Search conversations..."
            textSize = 13f
            typeface = Typeface.MONOSPACE
            setTextColor(text())
            setHintTextColor(muted())
            setPadding(dp(8), dp(4), 0, dp(4))
            background = null
            isSingleLine = true
            addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    populateInspectorPanel()
                }
            })
        }
        searchConvoRow.addView(searchConvosInput!!, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        searchConvoWrap!!.addView(searchConvoRow, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        drawer.addView(searchConvoWrap, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        // Chat list (scrollable)
        val chatScroll = android.widget.ScrollView(this).apply {
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
            isVerticalScrollBarEnabled = false
            isFillViewport = true
        }
        drawerInspectorPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        chatScroll.addView(drawerInspectorPanel, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        drawer.addView(chatScroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        populateInspectorPanel()

        // =====================================================
        // NAVIGATION ITEMS: PINS, PROJECTS, DOCUMENTS
        // =====================================================

        // Separator line
        drawer.addView(View(this).apply {
            setBackgroundColor(if (darkMode) Color.rgb(55, 55, 55) else Color.rgb(225, 223, 220))
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)))

        fun addNavItem(label: String, iconView: View, onClick: () -> Unit) {
            val navRow = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(4), dp(10), dp(4), dp(10))
                setOnClickListener { onClick() }
            }
            navRow.addView(iconView, LinearLayout.LayoutParams(dp(24), dp(24)))
            val navLabel = TextView(this@MainActivity).apply {
                text = label
                textSize = 13f
                setTextColor(text())
                typeface = Typeface.MONOSPACE
                setPadding(dp(10), 0, 0, 0)
            }
            navRow.addView(navLabel)
            navItemTextViews.add(navLabel)
            drawer.addView(navRow, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }

        addNavItem("PINS", NavPinIconView(this)) {
            closeDrawer()
            startActivity(android.content.Intent(this, PinsActivity::class.java))
        }
        addNavItem("PROJECTS", NavFolderIconView(this)) {
            closeDrawer()
            startActivity(android.content.Intent(this, ProjectsActivity::class.java))
        }
        addNavItem("DOCUMENTS", NavDocIconView(this)) {
            closeDrawer()
            startActivity(android.content.Intent(this, DocumentsActivity::class.java))
        }

        addSpace(drawer, 4)

        // Bottom row: New Chat left, Settings right
        val bottomRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, dp(4))
        }
        newChatBtn = TextView(this).apply {
            text = "+ New Chat"
            textSize = 13f
            setTextColor(if (darkMode) Color.BLACK else Color.WHITE)
            typeface = Typeface.MONOSPACE
            setPadding(dp(14), dp(10), dp(14), dp(10))
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(if (darkMode) Color.WHITE else Color.rgb(28, 28, 28))
                cornerRadius = dp(14).toFloat()
            }
            setOnClickListener {
                closeDrawer()
                startNewChat()
            }
        }
        bottomRow.addView(newChatBtn, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { gravity = Gravity.START })

        val bottomSpacer = Space(this)
        bottomRow.addView(bottomSpacer, LinearLayout.LayoutParams(0, 0, 1f))

        settingsIcon = SettingsIconView(this).apply {
            setOnClickListener {
                startActivity(android.content.Intent(this@MainActivity, SettingsActivity::class.java))
            }
        }
        bottomRow.addView(settingsIcon, LinearLayout.LayoutParams(dp(40), dp(40)).apply { gravity = Gravity.CENTER_VERTICAL or Gravity.END })
        drawer.addView(bottomRow, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        drawerOverlay = View(this).apply {
            setBackgroundColor(Color.argb(120, 0, 0, 0))
            visibility = View.GONE
            alpha = 0f
            setOnClickListener { closeDrawer() }
        }

        root.addView(drawerOverlay, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        root.addView(drawer, FrameLayout.LayoutParams(drawerWidth, ViewGroup.LayoutParams.MATCH_PARENT).apply {
            gravity = Gravity.START
            leftMargin = -drawerWidth
        })

        menuIcon = MenuIconView(this).apply {
            setLineColor(muted())
            homeMenuIcon = this
            setOnClickListener {
                val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(window.decorView.windowToken, 0)
                if (drawer.translationX > 0f) closeDrawer() else openDrawer()
            }
        }
        root.addView(menuIcon, FrameLayout.LayoutParams(dp(36), dp(36)).apply {
            gravity = Gravity.START or Gravity.TOP
            topMargin = dp(12)
            leftMargin = dp(6)
        })
        menuIcon.elevation = dp(16).toFloat()
        menuIcon.bringToFront()
    }

    private lateinit var menuIcon: MenuIconView
    private lateinit var drawerOverlay: View

    private var gestureDownX = 0f
    private var gestureDownY = 0f

    private var drawerGestureStartX = 0f
    private var drawerGestureStartTranslation = 0f
    private var drawerDragging = false

    // True only while the product image fullscreen viewer is active.
    private var isProductFullscreen = false

    private fun setupDrawerGestures() {

        // Edge swipe view: transparent strip on the left edge for opening drawer
        val edgeSwipeView = View(this).apply {
            setOnTouchListener { _, event ->
                // Don't intercept touches when drawer is open - let drawer content handle them
                if (drawer.translationX > 0f) return@setOnTouchListener false
                
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        gestureDownX = event.rawX
                        gestureDownY = event.rawY
                        drawerGestureStartX = event.rawX
                        drawerGestureStartTranslation = drawer.translationX
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - drawerGestureStartX
                        val start = drawerGestureStartTranslation
                        val newTranslation = (start + dx).coerceIn(0f, drawerWidth.toFloat())
                        val progress = newTranslation / drawerWidth.toFloat()
                        drawer.translationX = newTranslation
                        menuIcon.translationX = newTranslation
                        menuIcon.setProgress(progress)
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        val progress = drawer.translationX / drawerWidth.toFloat()
                        if (progress >= 0.5f) openDrawer() else closeDrawer()
                        true
                    }
                    else -> false
                }
            }
        }
        root.addView(edgeSwipeView, FrameLayout.LayoutParams(dp(30), ViewGroup.LayoutParams.MATCH_PARENT).apply {
            gravity = Gravity.START
        })

        // Edge swipe view on the right side for closing drawer
        val rightEdgeSwipeView = View(this).apply {
            setOnTouchListener { _, event ->
                // Only intercept when drawer is open
                if (drawer.translationX <= 0f) return@setOnTouchListener false
                
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        gestureDownX = event.rawX
                        drawerGestureStartX = event.rawX
                        drawerGestureStartTranslation = drawer.translationX
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - drawerGestureStartX
                        val start = drawerGestureStartTranslation
                        val newTranslation = (start + dx).coerceIn(0f, drawerWidth.toFloat())
                        val progress = newTranslation / drawerWidth.toFloat()
                        drawer.translationX = newTranslation
                        menuIcon.translationX = newTranslation
                        menuIcon.setProgress(progress)
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        val progress = drawer.translationX / drawerWidth.toFloat()
                        if (progress >= 0.5f) openDrawer() else closeDrawer()
                        true
                    }
                    else -> false
                }
            }
        }
        root.addView(rightEdgeSwipeView, FrameLayout.LayoutParams(dp(30), ViewGroup.LayoutParams.MATCH_PARENT).apply {
            gravity = Gravity.END
        })
    }

    private fun animateMenuToClose() {
        menuIcon.animateToClose()
    }

    private fun animateMenuToOpen() {
        menuIcon.animateToMenu()
    }

    private fun openDrawer() {
        if (isProductFullscreen) return

        populateInspectorPanel()

        menuIcon.bringToFront()
        drawerOverlay.visibility = View.VISIBLE

        val start = drawer.translationX
        val end = drawerWidth.toFloat()

        drawerOverlay.animate().alpha(1f).setDuration(260L).start()

        android.animation.ValueAnimator.ofFloat(start, end).apply {

            duration = 260L
            interpolator = DecelerateInterpolator()

            addUpdateListener { animator ->

                val value = animator.animatedValue as Float

                drawer.translationX = value
                menuIcon.translationX = value

                val progress =
                    (value / drawerWidth.toFloat())
                        .coerceIn(0f, 1f)

                // = -> X animation
                menuIcon.setProgress(progress)
            }

            start()
        }
    }

    private fun closeDrawer() {

        menuIcon.bringToFront()
        drawerOverlay.animate().alpha(0f).setDuration(220L).withEndAction { drawerOverlay.visibility = View.GONE }.start()

        val start = drawer.translationX
        val end = 0f

        android.animation.ValueAnimator.ofFloat(start, end).apply {

            duration = 220L
            interpolator = DecelerateInterpolator()

            addUpdateListener { animator ->

                val value = animator.animatedValue as Float

                drawer.translationX = value
                menuIcon.translationX = value

                val progress =
                    (value / drawerWidth.toFloat())
                        .coerceIn(0f, 1f)

                // X -> = animation
                menuIcon.setProgress(progress)
            }

            start()
        }
    }

    // ═══════════════════════════════════════════════
    // CHAT METHODS
    // ═══════════════════════════════════════════════

    private fun startNewChat() {
        currentChatId = "chat_${System.currentTimeMillis()}"
        chatHistory.clear()
        chatMessages.clear()
        if (::chatAdapter.isInitialized) chatAdapter.notifyDataSetChanged()
        msgList.removeAllViews()
        chatPlaceholderText = TextView(this).apply {
            text = getString(R.string.chat_placeholder)
            textSize = 15f; setTextColor(muted()); gravity = Gravity.CENTER
            setPadding(dp(32), dp(80), dp(32), dp(32))
        }
        msgList.addView(chatPlaceholderText)
        chatHeaderTitle.text = getString(R.string.chat_header_title)
        // Close pin ready screen if open
        if (::chatResultOverlay.isInitialized) chatResultOverlay.visibility = View.GONE
        // Reset chat state
        chatBusy = false
        chatCurrentCall?.cancel()
        chatCurrentCall = null
        chatInput.setText("")
        pendingImageFile = null
        pendingDocumentContent = null
        pendingDocumentName = null
        chatImagePreviewContainer.visibility = View.GONE
        chatDocPreviewContainer.visibility = View.GONE
        chatThinkingText.visibility = View.GONE
        chatBtnSend.visibility = View.VISIBLE
        chatBtnStop.visibility = View.GONE
    }

    private fun sendChat() {
        val t = chatInput.text.toString().trim()
        val imgFile = pendingImageFile
        val docContent = pendingDocumentContent
        if ((t.isEmpty() && imgFile == null && docContent == null) || chatBusy) return

        val urlPattern = Regex("""https?://\S+""")
        val detectedUrl = urlPattern.find(t)?.value
        if (detectedUrl != null && imgFile == null && docContent == null) {
            createPinFromChat(detectedUrl, t)
            return
        }

        chatBusy = true
        chatInput.setText("")
        chatInput.requestFocus()
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(chatInput, InputMethodManager.SHOW_FORCED)

        chatBtnSend.visibility = View.GONE
        chatBtnStop.visibility = View.VISIBLE
        chatThinkingText.visibility = View.VISIBLE

        hideChatPlaceholder()

        val userMsg = when {
            imgFile != null && t.isNotEmpty() -> t
            imgFile != null -> getString(R.string.sent_image)
            docContent != null && t.isNotEmpty() -> getString(R.string.attached_document, t, pendingDocumentName ?: "")
            docContent != null -> getString(R.string.sent_document, pendingDocumentName ?: "")
            else -> t
        }
        val indicator = when {
            docContent != null && t.isNotEmpty() -> "\uD83D\uDCCE $pendingDocumentName\n\n$t"
            docContent != null -> "\uD83D\uDCCE $pendingDocumentName"
            else -> t
        }
        // For API: include document content in user message so AI can actually read it
        val apiUserMsg = if (docContent != null) {
            "$indicator\n\n---DOCUMENT CONTENT---\n$docContent\n---END DOCUMENT---"
        } else indicator
        when {
            imgFile != null -> addChatUserWithImage(userMsg, imgFile)
            docContent != null -> addChatUser(apiUserMsg)
            else -> addChatUser(indicator)
        }

        clearPendingImage()
        clearPendingDocument()

        val full = org.json.JSONArray()
        val imageNote = if (imgFile != null) {
            "\n\nIMPORTANT: The user has sent an image with their message. If your model supports vision/image analysis, describe and analyze the image in detail. If your model does NOT support vision, respond honestly: 'I can see you sent an image but I cannot analyze images — my model does not have vision capability. Please describe the image to me and I will help.'"
        } else ""
        val docNote = if (docContent != null) {
            "\n\nDOCUMENT ATTACHMENT: The user's message contains an attached document between ---DOCUMENT CONTENT--- and ---END DOCUMENT--- markers. Read the ENTIRE document content carefully. Preserve formatting references (headings, bold, lists, tables). Summarize structure and key points. Quote specific sections when relevant. If the user asks a question, answer using the document content."
        } else ""
        val systemContent = AIInspector.buildSystemPrompt() + imageNote + docNote

        full.put(org.json.JSONObject().apply { put("role", "system"); put("content", systemContent) })
        for (m in chatHistory) full.put(org.json.JSONObject().apply { put("role", m.first); put("content", m.second) })

        val sec = SecureSettings(this)
        val ep = sec.getEndpoint(); val md = sec.getModel(); val ky = sec.getApiKey()
        if (ep.isEmpty() || md.isEmpty() || ky.isEmpty()) {
            ui.post { addChatBotFailed(getString(R.string.error_no_api)); resetChatUI() }
            return
        }

        val body = org.json.JSONObject()
        body.put("model", md); body.put("messages", full)
        body.put("temperature", 0.7); body.put("max_tokens", if (docContent != null) 4000 else 2000)

        val req = okhttp3.Request.Builder()
            .url(ep.trimEnd('/') + "/chat/completions")
            .addHeader("Authorization", "Bearer $ky")
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        chatScope.launch(Dispatchers.IO) {
            try {
                chatCurrentCall = http.newCall(req)
                val r = chatCurrentCall!!.execute()
                val rb = r.body?.string() ?: ""
                if (!r.isSuccessful) {
                    launch(Dispatchers.Main) { addChatBotFailed(getString(R.string.error_http, r.code, rb.take(200))); resetChatUI() }
                    return@launch
                }
                val j = org.json.JSONObject(rb)
                val ch = j.optJSONArray("choices")
                var c = ""
                if (ch != null && ch.length() > 0) {
                    val msg = ch.getJSONObject(0).optJSONObject("message")
                    if (msg != null) c = msg.optString("content", "")
                }
                launch(Dispatchers.Main) {
                    if (c.isEmpty()) addChatBotFailed(getString(R.string.error_empty_response)) else addChatBot(c)
                    resetChatUI()
                }
            } catch (e: Exception) {
                if (chatBusy) launch(Dispatchers.Main) { addChatBotFailed(getString(R.string.response_stopped)); resetChatUI() }
            }
        }
    }

    private fun stopGeneration() {
        chatBusy = false
        chatCurrentCall?.cancel()
        chatCurrentCall = null
        resetChatUI()
    }

    @Suppress("DEPRECATION")
    private fun formatBotText(text: String): CharSequence {
        if (text.isBlank()) return text
        var html = text
            .replace(Regex("\\[([^]]+)]\\(([^)]+)\\)")) { m ->
                "<a href=\"${m.groupValues[2]}\">${m.groupValues[1]}</a>"
            }
            .replace(Regex("(https?://[^\\s<>\"]+)")) { m ->
                val url = m.value.trimEnd('.', ',', ')', '>')
                "<a href=\"$url\">$url</a>"
            }
            .replace(Regex("\\*\\*(.+?)\\*\\*")) { "<b>${it.groupValues[1]}</b>" }
            .replace(Regex("(?<!\\*)\\*([^*]+?)\\*(?!\\*)")) { "<i>${it.groupValues[1]}</i>" }
            .replace(Regex("_(.+?)_")) { "<i>${it.groupValues[1]}</i>" }
            .replace(Regex("`(.+?)`")) { m ->
                val monoColor = "#${Integer.toHexString(muted()).removePrefix("ff")}"
                "<font face=\"monospace\" color=\"$monoColor\">${m.groupValues[1]}</font>"
            }
            .replace("\n", "<br>")
        return try {
            android.text.Html.fromHtml(html, android.text.Html.FROM_HTML_MODE_LEGACY)
        } catch (_: Exception) {
            text
        }
    }

    private fun resetChatUI() {
        chatBusy = false
        chatCurrentCall = null
        chatBtnSend.visibility = View.VISIBLE
        chatBtnStop.visibility = View.GONE
        chatThinkingText.visibility = View.GONE
        chatInput.requestFocus()
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(chatInput, InputMethodManager.SHOW_FORCED)
    }

    private fun hideChatPlaceholder() {
        if (::chatPlaceholderText.isInitialized && chatPlaceholderText.parent != null) {
            (chatPlaceholderText.parent as? ViewGroup)?.removeView(chatPlaceholderText)
        }
    }

    private fun showChatPlaceholder() {
        if (::chatPlaceholderText.isInitialized && chatPlaceholderText.parent == null) {
            msgList.addView(chatPlaceholderText)
        }
    }

    private fun addChatUser(t: String) {
        chatHistory.add(Pair("user", t))
        addChatUserToView(t)
        saveChatConversation()
    }


    private fun createChatBubble(text: CharSequence, isUser: Boolean = true): View {
        val d = resources.displayMetrics.density
        val card = android.widget.FrameLayout(this@MainActivity).apply {
            val bg = GradientDrawable().apply {
                setColor(if (isUser) Color.rgb(28, 28, 28) else containerBg())
                cornerRadius = 16f * d
            }
            background = bg
            setPadding((14 * d).toInt(), (10 * d).toInt(), (14 * d).toInt(), (10 * d).toInt())
        }
        val displayText = if (isUser) text else formatBotText(text.toString())
        val tv = TextView(this@MainActivity).apply {
            this.text = displayText
            textSize = 14f
            typeface = Typeface.MONOSPACE
            setTextColor(if (isUser) Color.WHITE else text())
            setLineSpacing(0f, 1.3f)
            setTextIsSelectable(true)
            linksClickable = true
            movementMethod = android.text.method.LinkMovementMethod.getInstance()
            setLinkTextColor(if (isUser) Color.rgb(130, 180, 255) else accentBlue())
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        card.addView(tv)
        return card
    }

    private fun addChatUserToView(t: String) {
        chatMessages.add(ChatMessage(ChatMessage.ROLE_USER, t))
        chatAdapter.notifyItemInserted(chatMessages.size - 1)
        scrollChat()
    }

    private fun addChatUserWithImage(t: String, imgFile: java.io.File) {
        chatHistory.add(Pair("user", t))
        val imgView = ImageView(this).apply {
            val bmp = android.graphics.BitmapFactory.decodeFile(imgFile.absolutePath)
            setImageBitmap(bmp)
            scaleType = ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = true
            maxWidth = dp(180)
            setPadding(0, 0, 0, dp(6))
            clipToOutline = true
            outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: View, outline: android.graphics.Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, dp(12).toFloat())
                }
            }
        }
        msgList.addView(imgView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { gravity = Gravity.END })
        if (t.isNotEmpty() && t != "(sent an image)") {
            val b = TextView(this).apply {
                text = t; textSize = 14f; typeface = Typeface.MONOSPACE
                setTextColor(text())
                setPadding(dp(4), dp(6), dp(4), dp(6))
                gravity = Gravity.END
            }
            msgList.addView(b, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        scrollChat()
        saveChatConversation()
    }

    private fun addChatBot(t: String) {
        chatHistory.add(Pair("assistant", t))
        addChatBotToView(t)
        saveChatConversation()
    }

    private fun addChatBotFailed(t: String) {
        chatMessages.add(ChatMessage(ChatMessage.ROLE_ASSISTANT, t, isFailed = true))
        chatAdapter.notifyItemInserted(chatMessages.size - 1)
        scrollChat()
    }

    private fun addChatBotToView(t: String) {
        if (t.startsWith("[pin_data]") && t.endsWith("[/pin_data]")) {
            val json = t.removePrefix("[pin_data]").removeSuffix("[/pin_data]")
            try {
                val obj = org.json.JSONObject(json)
                val seoObj = obj.getJSONObject("seo")
                val anglesArr = obj.getJSONArray("angles")
                val imageUrl = obj.optString("imageUrl", "")
                val productImagesArr = seoObj.optJSONArray("productImages")
                val angleSeoArr = seoObj.optJSONArray("angleSeo")

                val seo = SeoResult(
                    title = seoObj.optString("title", ""), description = seoObj.optString("description", ""),
                    altText = seoObj.optString("altText", ""), affiliateLink = seoObj.optString("affiliateLink", ""),
                    productImages = (0 until (productImagesArr?.length() ?: 0)).map { productImagesArr!!.optString(it) },
                    productTitle = seoObj.optString("productTitle", ""), productBrand = seoObj.optString("productBrand", ""),
                    angleSeo = (0 until (angleSeoArr?.length() ?: 0)).map {
                        val s = angleSeoArr!!.getJSONObject(it)
                        AngleSeoResult(s.optString("angleId", ""), s.optString("title", ""), s.optString("description", ""), s.optString("altText", ""))
                    }
                )
                val angles = (0 until anglesArr.length()).map { j ->
                    val a = anglesArr.getJSONObject(j)
                    PinterestAngle(a.optString("id", ""), a.optString("title", ""), a.optInt("number", j + 1),
                        a.optString("shortDescription", ""), "", "", a.optString("completePrompt", ""),
                        android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888), isSaved = a.optBoolean("isSaved", false))
                }

                if (imageUrl.isNotBlank()) {
                    addChatBotToView(getString(R.string.loading_pin))
                    chatScope.launch(Dispatchers.IO) {
                        var conn: java.net.HttpURLConnection? = null
                        try {
                            conn = java.net.URL(imageUrl).openConnection() as java.net.HttpURLConnection
                            conn.connectTimeout = 15000; conn.readTimeout = 15000; conn.instanceFollowRedirects = true
                            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36")
                            conn.setRequestProperty("Referer", "https://www.amazon.com/")
                            conn.connect()
                            if (conn.responseCode == 200) {
                                val bytes = conn.inputStream.use { it.readBytes() }
                                val bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                if (bmp != null) {
                                    angles.forEach { it.productBitmap = bmp }
                                    launch(Dispatchers.Main) {
                                        // Remove loading message and show summary card
                                        if (chatMessages.isNotEmpty()) {
                                            val lastIdx = chatMessages.size - 1
                                            chatMessages.removeAt(lastIdx)
                                            chatAdapter.notifyItemRemoved(lastIdx)
                                        }
                                        showChatSummaryCard(seo, angles, bmp)
                                    }
                                } else {
                                    launch(Dispatchers.Main) { showChatBotText(getString(R.string.pin_generated, seo.productTitle)) }
                                }
                            } else {
                                launch(Dispatchers.Main) { showChatBotText(getString(R.string.pin_generated, seo.productTitle)) }
                            }
                        } catch (_: Exception) {
                            launch(Dispatchers.Main) { showChatBotText(getString(R.string.pin_generated, seo.productTitle)) }
                        } finally { conn?.disconnect() }
                    }
                    return
                }
            } catch (_: Exception) {}
        }

        chatMessages.add(ChatMessage(ChatMessage.ROLE_ASSISTANT, t))
        chatAdapter.notifyItemInserted(chatMessages.size - 1)
        scrollChat()
    }

    private fun showChatBotText(t: String) {
        chatMessages.add(ChatMessage(ChatMessage.ROLE_ASSISTANT, t))
        chatAdapter.notifyItemInserted(chatMessages.size - 1)
        scrollChat()
    }

    private fun scrollChat() {
        ui.postDelayed({
            if (chatMessages.isNotEmpty()) {
                chatRecycler.scrollToPosition(chatMessages.size - 1)
            }
        }, 100)
    }

    private fun retryFailedMessage(position: Int) {
        if (position < 0 || position >= chatMessages.size) return
        val failedMsg = chatMessages[position]
        if (!failedMsg.isFailed) return

        // Remove the failed message
        chatMessages.removeAt(position)
        chatAdapter.notifyItemRemoved(position)

        // Find the last user message before this one and resend it
        var lastUserMsg = ""
        for (i in position - 1 downTo 0) {
            if (chatMessages[i].role == ChatMessage.ROLE_USER) {
                lastUserMsg = chatMessages[i].content
                break
            }
        }
        if (lastUserMsg.isNotEmpty()) {
            chatInput.setText(lastUserMsg)
            sendChat()
        }
    }

    private fun toggleChatAttachPanel() {
        chatAttachPanel.visibility = if (chatAttachPanel.visibility == View.VISIBLE) View.GONE else View.VISIBLE
    }

    private fun pickImage() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, 2001)
    }

    private fun takePicture() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val file = java.io.File(cacheDir, "cam_${System.currentTimeMillis()}.jpg")
        chatCameraPhotoPath = file.absolutePath
        val uri = Uri.fromFile(file)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
        startActivityForResult(intent, 2002)
    }

    private fun pickDocument() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        startActivityForResult(intent, 2003)
    }

    private fun clearPendingImage() {
        pendingImageFile = null
        chatImagePreviewContainer.visibility = View.GONE
        chatImagePreviewImg.setImageBitmap(null)
    }

    private fun clearPendingDocument() {
        pendingDocumentContent = null
        pendingDocumentName = null
        chatDocPreviewContainer.visibility = View.GONE
    }

    // ── DOCUMENT HANDLING ──

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        when (requestCode) {
            2001 -> {
                val uri = data?.data ?: return
                val file = java.io.File(cacheDir, "img_${System.currentTimeMillis()}.jpg")
                contentResolver.openInputStream(uri)?.use { input -> file.outputStream().use { output -> input.copyTo(output) } }
                pendingImageFile = file
                val bmp = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                chatImagePreviewImg.setImageBitmap(bmp)
                chatImagePreviewContainer.visibility = View.VISIBLE
            }
            2002 -> {
                val path = chatCameraPhotoPath ?: return
                val file = java.io.File(path)
                if (file.exists()) {
                    pendingImageFile = file
                    val bmp = android.graphics.BitmapFactory.decodeFile(path)
                    chatImagePreviewImg.setImageBitmap(bmp)
                    chatImagePreviewContainer.visibility = View.VISIBLE
                }
            }
            2003 -> {
                val uri = data?.data ?: return
                var name = uri.lastPathSegment?.substringAfterLast('/') ?: "document"
                if (!name.contains('.')) {
                    val mime = contentResolver.getType(uri) ?: ""
                    val ext = when {
                        mime.contains("pdf") -> ".pdf"
                        mime.contains("wordprocessingml") || mime.equals("application/msword", true) -> ".docx"
                        mime.contains("spreadsheet") || mime.contains("excel") || mime.equals("application/vnd.ms-excel", true) -> ".xlsx"
                        mime.contains("presentation") || mime.equals("application/vnd.ms-powerpoint", true) -> ".pptx"
                        mime.equals("text/plain", true) -> ".txt"
                        mime.equals("text/csv", true) -> ".csv"
                        mime.equals("text/html", true) -> ".html"
                        mime.equals("text/xml", true) -> ".xml"
                        mime.equals("application/json", true) -> ".json"
                        mime.equals("text/rtf", true) || mime.equals("application/rtf", true) -> ".rtf"
                        else -> ""
                    }
                    name = name + ext
                }
                val content = DocumentReader.read(this@MainActivity, uri, name)
                pendingDocumentContent = content
                pendingDocumentName = name
                chatDocPreviewName.text = "\uD83D\uDCC4 $name"
                chatDocPreviewContainer.visibility = View.VISIBLE
            }
        }
    }

    private fun saveChatConversation() {
        if (chatHistory.isEmpty()) return
        val existing = ChatHistoryManager.getConversation(this, currentChatId)
        val existingTitle = existing?.optString("title", "") ?: ""
        val title = if (existingTitle.isNotBlank() && existingTitle != "New Chat" && !existingTitle.equals("null", ignoreCase = true)) {
            existingTitle
        } else {
            ChatHistoryManager.generateTitle(chatHistory)
        }
        ChatHistoryManager.saveConversation(this, currentChatId, title, chatHistory)
    }

    private fun loadChatConversation(chatId: String) {
        val convo = ChatHistoryManager.getConversation(this, chatId) ?: return
        chatHistory.clear()
        msgList.removeAllViews()
        currentChatId = chatId

        val title = convo.optString("title", "")
        if (title.isNotBlank() && !title.equals("null", ignoreCase = true)) {
            chatHeaderTitle.text = title
        }

        val messages = convo.optJSONArray("messages") ?: return
        for (i in 0 until messages.length()) {
            val msg = messages.getJSONObject(i)
            val role = msg.getString("role")
            val messageContent = msg.getString("content")
            chatHistory.add(Pair(role, messageContent))
            if (role == "user") {
                addChatUserToView(messageContent)
            } else if (messageContent.startsWith("[pin_data]") && messageContent.endsWith("[/pin_data]")) {
                addChatBotToView(messageContent)
            } else {
                addChatBotToView(messageContent)
            }
        }
    }

    private fun createPinFromChat(url: String, userText: String) {
        chatBusy = true
        chatInput.setText("")
        hideChatPlaceholder()
        addChatUser(userText)

        chatBtnSend.visibility = View.GONE
        chatBtnStop.visibility = View.VISIBLE
        chatThinkingText.visibility = View.VISIBLE

        val sec = SecureSettings(this)
        val ep = sec.getEndpoint(); val md = sec.getModel(); val ky = sec.getApiKey()
        if (ep.isEmpty() || md.isEmpty() || ky.isEmpty()) {
            ui.post { addChatBotFailed(getString(R.string.error_no_api)); resetChatUI() }
            return
        }

        ui.post { addChatBot(getString(R.string.analyzing_product)) }

        chatScope.launch(Dispatchers.IO) {
            val extraction = ProductUrlEngine(sec.getBrowserlessApiKey()).extract(url)
            extraction.fold(
                onSuccess = { product ->
                    launch(Dispatchers.Main) { addChatBot(getString(R.string.generating_seo)) }
                    val result = AIClient().generateSeo(
                        endpoint = ep, model = md, apiKey = ky,
                        productEvidence = product.evidenceText, affiliateLink = product.sourceUrl,
                        productImages = product.imageUrls.take(3)
                    )
                    result.fold(
                        onSuccess = { rawSeo ->
                            val seo = rawSeo.copy(productTitle = product.title, productBrand = product.brand)
                            launch(Dispatchers.Main) { addChatBot(getString(R.string.creating_posters)) }

                            var productBmp: android.graphics.Bitmap? = null
                            var productImgUrl = ""
                            for (imgUrl in product.imageUrls.take(3)) {
                                var conn: java.net.HttpURLConnection? = null
                                try {
                                    conn = java.net.URL(imgUrl).openConnection() as java.net.HttpURLConnection
                                    conn.connectTimeout = 20000; conn.readTimeout = 20000; conn.instanceFollowRedirects = true
                                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36")
                                    conn.setRequestProperty("Accept", "image/*,*/*;q=0.8")
                                    conn.setRequestProperty("Referer", "https://www.amazon.com/")
                                    conn.connect()
                                    if (conn.responseCode != 200) continue
                                    val bytes = conn.inputStream.use { it.readBytes() }
                                    val bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                    if (bmp != null) { productBmp = bmp; productImgUrl = imgUrl; break }
                                } catch (_: Exception) {} finally { conn?.disconnect() }
                            }

                            if (productBmp == null) {
                                launch(Dispatchers.Main) { addChatBotFailed(getString(R.string.error_image_download)); resetChatUI() }
                                return@launch
                            }

                            val angles = PosterRenderer.renderAll(this@MainActivity, productBmp, seo.title, seo.description, seo, product, productImgUrl)
                            saveSeoHistory(seo, angles, productImgUrl)
                            generatePinTitleAsync(product.title, seo.title)

                            val pinData = org.json.JSONObject().apply {
                                put("seo", org.json.JSONObject().apply {
                                    put("title", seo.title); put("description", seo.description); put("altText", seo.altText)
                                    put("affiliateLink", seo.affiliateLink); put("productTitle", seo.productTitle); put("productBrand", seo.productBrand)
                                    put("productImages", org.json.JSONArray().apply { seo.productImages.take(3).forEach { put(it) } })
                                    put("angleSeo", org.json.JSONArray().apply {
                                        for (s in seo.angleSeo) put(org.json.JSONObject().apply {
                                            put("angleId", s.angleId); put("title", s.title); put("description", s.description); put("altText", s.altText)
                                        })
                                    })
                                })
                                put("angles", org.json.JSONArray().apply {
                                    for (a in angles) put(org.json.JSONObject().apply {
                                        put("id", a.id); put("title", a.title); put("number", a.number)
                                        put("shortDescription", a.shortDescription); put("completePrompt", a.completePrompt); put("isSaved", a.isSaved)
                                    })
                                })
                                put("imageUrl", productImgUrl)
                            }
                            val summaryText = "[pin_data]${pinData}[/pin_data]"
                            chatHistory.add(Pair("assistant", summaryText))
                            saveChatConversation()
                            generateChatTitle()

                            launch(Dispatchers.Main) {
                                resetChatUI()
                                showChatSummaryCard(seo, angles, productBmp)
                            }
                        },
                    onFailure = { error ->
                        launch(Dispatchers.Main) { addChatBotFailed(getString(R.string.error_seo_failed, error.message ?: "")); resetChatUI() }
                    }
                )
            },
            onFailure = { error ->
                launch(Dispatchers.Main) { addChatBotFailed(getString(R.string.error_extraction_failed, error.message ?: "")); resetChatUI() }
            }
            )
        }
    }

    private fun showChatSummaryCard(seo: SeoResult, angles: List<PinterestAngle>, thumbnail: android.graphics.Bitmap) {
        val w = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.START; setPadding(0, dp(6), dp(14), dp(6))
        }
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply { setColor(containerBg()); cornerRadius = dp(16).toFloat() }
            clipToOutline = true
            outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: View, outline: android.graphics.Outline) { outline.setRoundRect(0, 0, view.width, view.height, dp(16).toFloat()) }
            }
        }
        card.addView(ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(180))
            setImageBitmap(thumbnail)
        })
        card.addView(TextView(this).apply {
            text = seo.productTitle.ifEmpty { seo.title }; textSize = 14f; setTextColor(text())
            typeface = Typeface.MONOSPACE; setPadding(dp(14), dp(10), dp(14), dp(4)); maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
        })
        card.addView(TextView(this).apply {
            text = seo.title; textSize = 12f; setTextColor(muted())
            typeface = Typeface.MONOSPACE; setPadding(dp(14), 0, dp(14), dp(10)); maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
        })
        val btnRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(dp(14), dp(4), dp(14), dp(14)) }
        btnRow.addView(TextView(this).apply {
            text = getString(R.string.see_full_result); textSize = 13f
            setTextColor(if (darkMode) Color.BLACK else Color.WHITE)
            typeface = Typeface.MONOSPACE
            background = GradientDrawable().apply { setColor(if (darkMode) Color.WHITE else Color.rgb(28, 28, 28)); cornerRadius = dp(12).toFloat() }
            setPadding(dp(16), dp(10), dp(16), dp(10)); gravity = Gravity.CENTER
            setOnClickListener { showChatResultOverlay(seo, angles) }
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        card.addView(btnRow)
        w.addView(card, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        msgList.addView(w, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        scrollChat()
    }

    private fun showChatResultOverlay(seo: SeoResult, angles: List<PinterestAngle>) {
        chatResultOverlay.removeAllViews()
        chatResultOverlay.visibility = View.VISIBLE

        val bg = if (darkMode) Color.rgb(18, 18, 18) else Color.rgb(250, 249, 247)
        val outer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(bg) }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(14), dp(10), dp(14), dp(10))
        }
        header.addView(TextView(this).apply {
            text = "✕"; textSize = 20f; setTextColor(muted()); setPadding(dp(8), dp(8), dp(8), dp(8))
            setOnClickListener { chatResultOverlay.visibility = View.GONE }
        })
        header.addView(TextView(this).apply {
            text = getString(R.string.pin_ready); textSize = 16f; setTextColor(text()); typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER; letterSpacing = 0.08f
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        outer.addView(header, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        val pageScroll = ScrollView(this).apply { isVerticalScrollBarEnabled = false }
        val page = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(4), 0, dp(40)) }

        val dotViews = mutableListOf<TextView>()
        val dots = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        for (i in angles.indices) {
            val dot = TextView(this).apply { text = "●"; textSize = 10f; gravity = Gravity.CENTER; setPadding(dp(6), 0, dp(6), 0) }
            dotViews.add(dot); dots.addView(dot)
        }
        val presetNames = angles.map { it.title }
        val presetLabel = TextView(this).apply {
            text = presetNames[0]; textSize = 13f; setTextColor(muted()); typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER; letterSpacing = 0.06f
        }

        val carouselOuter = FrameLayout(this)
        val carousel = CoverFlowCarousel(this).apply {
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
        carousel.setImages(angles.map { it.productBitmap })
        carousel.onImageClicked = { idx -> openChatAngleFullscreen(angles, idx, seo) }
        carousel.onImageLongPressed = { idx ->
            if (idx < angles.size) {
                angles[idx].isSaved = !angles[idx].isSaved
                showChatPinnedOverlay(carouselOuter, angles[idx].isSaved)
                updateAnglePinState(idx)
            }
        }
        carousel.onSelectionChanged = { idx ->
            for (i in dotViews.indices) { dotViews[i].setTextColor(if (i == idx) text() else muted()); dotViews[i].textSize = if (i == idx) 13f else 10f }
            presetLabel.text = presetNames[idx]
            showChatPinnedOverlay(carouselOuter, idx < angles.size && angles[idx].isSaved)
        }

        page.addView(carouselOuter, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(540)))
        carouselOuter.addView(carousel)
        if (angles.isNotEmpty() && angles[0].isSaved) showChatPinnedOverlay(carouselOuter, true)

        addSpace(page, 8); page.addView(presetLabel); addSpace(page, 4); page.addView(dots); addSpace(page, 12)

        fun addTerminalSection(container: LinearLayout, label: String, value: String): TextView {
            container.addView(TextView(this).apply { text = "> $label"; textSize = 12f; setTextColor(muted()); typeface = Typeface.MONOSPACE; setPadding(0, dp(6), 0, dp(2)) })
            val tv = TextView(this).apply { text = value; textSize = 13f; setTextColor(text()); typeface = Typeface.MONOSPACE; setLineSpacing(0f, 1.0f); setPadding(0, dp(4), 0, dp(4)) }
            container.addView(tv)
            container.addView(object : View(this) {
                override fun onDraw(c: Canvas) {
                    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1.5f * resources.displayMetrics.density; color = muted() }
                    val r = 3f * resources.displayMetrics.density; val s = width * 0.36f; val ox = width * 0.14f; val oy = height * 0.18f
                    c.drawRoundRect(RectF(ox, oy, ox + s, oy + s), r, r, p)
                    c.drawRoundRect(RectF(ox + s * 0.3f, oy + s * 0.3f, ox + s * 1.3f, oy + s * 1.3f), r, r, p)
                }
            }.apply {
                setBackgroundColor(Color.TRANSPARENT)
                setOnClickListener {
                    val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText(label, tv.text.toString()))
                    Toast.makeText(this@MainActivity, getString(R.string.copied), Toast.LENGTH_SHORT).show()
                }
            }.also { it.layoutParams = LinearLayout.LayoutParams(dp(28), dp(28)) })
            val divider = View(this).apply {
                setBackgroundColor(if (darkMode) Color.rgb(40, 40, 40) else Color.rgb(220, 218, 215))
            }
            container.addView(divider, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1))
            return tv
        }

        seoTitleText = addTerminalSection(page, "TITLE", seo.title)
        addSpace(page, 8)
        seoDescText = addTerminalSection(page, "DESCRIPTION", seo.description)
        addSpace(page, 8)
        addTerminalSection(page, "AFFILIATE LINK", seo.affiliateLink)
        addSpace(page, 8)
        seoAltText = addTerminalSection(page, "ALT TEXT", seo.altText)

        pageScroll.addView(page, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        outer.addView(pageScroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        chatResultOverlay.addView(outer, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }

    private fun showChatPinnedOverlay(parent: ViewGroup, show: Boolean) {
        val existing = parent.findViewWithTag<View>("pinned_overlay")
        if (!show) { existing?.let { parent.removeView(it) }; return }
        if (existing != null) return
        parent.addView(TextView(this).apply {
            text = getString(R.string.pinned); textSize = 16f; setTextColor(Color.WHITE); typeface = Typeface.MONOSPACE
            letterSpacing = 0.2f; gravity = Gravity.CENTER; setPadding(dp(20), dp(10), dp(20), dp(10)); tag = "pinned_overlay"
            background = GradientDrawable().apply { setColor(Color.argb(160, 0, 0, 0)); setStroke(dp(2), Color.WHITE); cornerRadius = dp(4).toFloat() }
        }, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { gravity = Gravity.CENTER })
    }

    private fun openChatAngleFullscreen(angles: List<PinterestAngle>, startIndex: Int, result: SeoResult) {
        val viewer = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        val contentScroll = ScrollView(this).apply { isVerticalScrollBarEnabled = true }
        val contentLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(24), dp(60), dp(24), dp(80)) }

        val angleTitle = TextView(this).apply { textSize = 22f; setTextColor(Color.WHITE); typeface = Typeface.MONOSPACE; gravity = Gravity.CENTER; letterSpacing = 0.08f }
        val angleDesc = TextView(this).apply { textSize = 13f; setTextColor(Color.argb(160, 255, 255, 255)); typeface = Typeface.MONOSPACE; gravity = Gravity.CENTER; setPadding(0, dp(4), 0, dp(20)) }
        val promptText = TextView(this).apply { textSize = 13f; setTextColor(Color.argb(200, 255, 255, 255)); typeface = Typeface.MONOSPACE; setLineSpacing(dp(4).toFloat(), 1.15f); setTextIsSelectable(true) }

        fun updateContent(idx: Int) {
            val angle = angles[idx]
            angleTitle.text = "${String.format("%02d", angle.number)} \u2014 ${angle.title}"
            angleDesc.text = angle.shortDescription
            promptText.text = angle.completePrompt
        }
        updateContent(startIndex)

        contentLayout.addView(angleTitle, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        contentLayout.addView(angleDesc, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        contentLayout.addView(TextView(this).apply { text = getString(R.string.label_image_prompt); textSize = 11f; setTextColor(Color.argb(100, 255, 255, 255)); typeface = Typeface.MONOSPACE; letterSpacing = 0.12f; setPadding(0, dp(16), 0, dp(8)) })
        contentLayout.addView(promptText, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        contentScroll.addView(contentLayout, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        viewer.addView(contentScroll, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        var currentPage = startIndex
        viewer.addView(TextView(this).apply {
            text = "✕"; textSize = 22f; setTextColor(Color.WHITE); setPadding(dp(16), dp(16), dp(16), dp(16))
            setOnClickListener {
                contentScroll.animate().cancel()
                contentScroll.animate().alpha(0f).setDuration(220L).setInterpolator(DecelerateInterpolator())
                    .withEndAction { (viewer.parent as? ViewGroup)?.removeView(viewer) }.start()
            }
        }, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { gravity = Gravity.TOP or Gravity.START; setMargins(dp(16), dp(16), 0, 0) })

        var swipeStartX = 0f
        viewer.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> { swipeStartX = event.rawX; true }
                MotionEvent.ACTION_UP -> {
                    val dx = event.rawX - swipeStartX
                    if (kotlin.math.abs(dx) > dp(80)) {
                        if (dx < 0 && currentPage < angles.lastIndex) currentPage++ else if (dx > 0 && currentPage > 0) currentPage--
                        updateContent(currentPage); contentScroll.scrollTo(0, 0)
                    }; true
                }
                else -> false
            }
        }

        chatResultOverlay.addView(viewer, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        contentScroll.alpha = 0f; contentScroll.animate().alpha(1f).setDuration(280L).setInterpolator(DecelerateInterpolator()).start()
    }

    private fun generateChatTitle() {
        val msgs = ArrayList(chatHistory)
        Thread {
            try {
                TitleGenerator.generateTitle(this, msgs).onSuccess { title ->
                    runOnUiThread { try { chatHeaderTitle.text = title } catch (_: Exception) {} }
                    ChatHistoryManager.updateTitle(this, currentChatId, title)
                }
            } catch (_: Exception) {}
        }.start()
    }

    private fun loadExistingChatTitle() {
        val convo = ChatHistoryManager.getConversation(this, currentChatId) ?: return
        val title = convo.optString("title", "")
        if (title.isNotBlank() && !title.equals("null", ignoreCase = true)) {
            chatHeaderTitle.text = title
        }
    }

    private fun generatePinTitleAsync(productTitle: String, seoTitle: String) {
        Thread {
            try {
                TitleGenerator.generatePinTitle(this, productTitle, seoTitle).onSuccess { pinTitle ->
                    try {
                        val prefs = getSharedPreferences("pinster_history", MODE_PRIVATE)
                        val raw = prefs.getString("items", "[]") ?: "[]"
                        val arr = org.json.JSONArray(raw)
                        for (i in 0 until arr.length()) {
                            val obj = arr.optJSONObject(i) ?: continue
                            if (obj.optString("productTitle", "") == productTitle) { obj.put("pinTitle", pinTitle); break }
                        }
                        prefs.edit().putString("items", arr.toString()).apply()
                        runOnUiThread { populateRecentPins() }
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
        }.start()
    }

    // ═══════════════════════════════════════════════
    // END CHAT METHODS
    // ═══════════════════════════════════════════════

    private fun showLoading(productUrl: String) {
        content.removeAllViews()

        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(
                dp(24),
                dp(90),
                dp(24),
                dp(40)
            )
        }

        content.addView(
            page,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        // DELISH STUDIO LOADING

        val logo = ImageView(this).apply {
            setImageResource(R.drawable.home_logo)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            alpha = 0f
            scaleX = 0.86f
            scaleY = 0.86f
        }

        page.addView(
            logo,
            LinearLayout.LayoutParams(
                dp(88),
                dp(88)
            )
        )

        logo.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(650)
            .setInterpolator(DecelerateInterpolator(2f))
            .start()

        val logoPulse =
            android.animation.ObjectAnimator.ofPropertyValuesHolder(
                logo,
                android.animation.PropertyValuesHolder.ofFloat(
                    View.SCALE_X,
                    0.97f,
                    1.035f,
                    0.97f
                ),
                android.animation.PropertyValuesHolder.ofFloat(
                    View.SCALE_Y,
                    0.97f,
                    1.035f,
                    0.97f
                ),
                android.animation.PropertyValuesHolder.ofFloat(
                    View.ALPHA,
                    0.82f,
                    1f,
                    0.82f
                )
            ).apply {
                duration = 1900
                repeatCount =
                    android.animation.ValueAnimator.INFINITE
                interpolator =
                    DecelerateInterpolator(1.5f)
            }

        logoPulse.start()

        addSpace(page, 22)

        val studioTitle = TextView(this).apply {
            text = "DELISH STUDIO"
            textSize = 22f
            setTextColor(text())
            typeface = Typeface.MONOSPACE
            letterSpacing = 0.16f
            gravity = Gravity.CENTER
            alpha = 0f
        }

        page.addView(studioTitle)

        studioTitle.animate()
            .alpha(1f)
            .setDuration(700)
            .start()

        addSpace(page, 36)

        val dots = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        val dotViews = mutableListOf<TextView>()

        repeat(3) {
            val dot = TextView(this).apply {
                text = "•"
                textSize = 18f
                setTextColor(muted())
                gravity = Gravity.CENTER
                alpha = 0.25f
            }

            dotViews.add(dot)

            dots.addView(
                dot,
                LinearLayout.LayoutParams(
                    dp(18),
                    dp(30)
                )
            )
        }

        page.addView(dots)

        dotViews.forEachIndexed { index, dot ->

            android.animation.ObjectAnimator
                .ofPropertyValuesHolder(
                    dot,
                    android.animation.PropertyValuesHolder.ofFloat(
                        View.TRANSLATION_Y,
                        dp(4).toFloat(),
                        dp(-4).toFloat(),
                        dp(4).toFloat()
                    ),
                    android.animation.PropertyValuesHolder.ofFloat(
                        View.ALPHA,
                        0.25f,
                        1f,
                        0.25f
                    )
                )
                .apply {
                    duration = 1100
                    startDelay = index * 180L
                    repeatCount =
                        android.animation.ValueAnimator.INFINITE
                    interpolator =
                        DecelerateInterpolator(1.4f)
                }
                .start()
        }

        addSpace(page, 20)

        val status = TextView(this).apply {
            text = "ANALYZING PRODUCT"
            textSize = 11f
            setTextColor(muted())
            typeface = Typeface.MONOSPACE
            letterSpacing = 0.14f
            gravity = Gravity.CENTER
            alpha = 0.75f
        }

        page.addView(status)

        val statusMessages = arrayOf(
            "ANALYZING PRODUCT",
            "EXTRACTING DATA",
            "CRAFTING SEO",
            "REFINING RESULT",
            "CREATING POSTERS"
        )

        val statusHandler =
            android.os.Handler(
                android.os.Looper.getMainLooper()
            )

        var statusIndex = 0

        val statusRunnable = object : Runnable {
            override fun run() {

                status.animate()
                    .alpha(0f)
                    .setDuration(180)
                    .withEndAction {

                        statusIndex =
                            (statusIndex + 1) %
                                statusMessages.size

                        status.text =
                            statusMessages[statusIndex]

                        status.animate()
                            .alpha(0.75f)
                            .setDuration(260)
                            .start()
                    }
                    .start()

                statusHandler.postDelayed(
                    this,
                    1800
                )
            }
        }

        statusHandler.postDelayed(
            statusRunnable,
            1800
        )

        // EXISTING GENERATION PIPELINE

        val secureSettings = SecureSettings(this)

        val endpoint =
            secureSettings.getEndpoint()

        val model =
            secureSettings.getModel()

        val apiKey =
            secureSettings.getApiKey()

        if (
            endpoint.isBlank() ||
            model.isBlank() ||
            apiKey.isBlank()
        ) {
            statusHandler.removeCallbacks(
                statusRunnable
            )

            showGenerationError(
                "AI settings are incomplete.\n\n" +
                    "Open API CONNECTION and add your API key."
            )
            return
        }

        Thread {

            val extraction =
                ProductUrlEngine(
                    secureSettings.getBrowserlessApiKey()
                ).extract(productUrl)

            extraction.fold(

                onSuccess = { product ->

                    val existingItem = findHistoryByUrl(product.sourceUrl)
                    if (existingItem != null) {
                        runOnUiThread {
                            statusHandler.removeCallbacks(statusRunnable)
                            showHistoryResult(existingItem)
                        }
                        return@fold
                    }

                    val result =
                        AIClient().generateSeo(
                            endpoint = endpoint,
                            model = model,
                            apiKey = apiKey,
                            productEvidence =
                                product.evidenceText,
                            affiliateLink =
                                product.sourceUrl,
                            productImages =
                                product.imageUrls.take(3)
                        )

                    runOnUiThread {

                        statusHandler.removeCallbacks(
                            statusRunnable
                        )

                        result
                            .onSuccess { rawSeo ->

                                // Copy product data into SeoResult
                                val seo = rawSeo.copy(
                                    productTitle = product.title,
                                    productBrand = product.brand
                                )

                                // Load product image for poster generation
                                val imageUrls = product.imageUrls

                                if (imageUrls.isEmpty()) {
                                    showReady(seo)
                                    return@onSuccess
                                }

                                Thread {
                                    try {
                                    var productBmp: android.graphics.Bitmap? = null
                                    var productImgUrl = ""
                                    for (url in imageUrls.take(3)) {
                                        try {
                                            val conn = java.net.URL(url)
                                                .openConnection() as java.net.HttpURLConnection
                                            conn.connectTimeout = 20000
                                            conn.readTimeout = 20000
                                            conn.instanceFollowRedirects = true
                                            conn.setRequestProperty("User-Agent",
                                                "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36")
                                            conn.setRequestProperty("Accept", "image/*,*/*;q=0.8")
                                            conn.setRequestProperty("Referer", "https://www.amazon.com/")
                                            val bytes = conn.inputStream.use { it.readBytes() }
                                            conn.disconnect()
                                            val bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                            if (bmp != null) {
                                                productBmp = bmp
                                                productImgUrl = url
                                                break
                                            }
                                        } catch (e: Exception) {
                                            android.util.Log.e("Pinster", "Image download failed: $url - ${e.message}")
                                        }
                                    }

                                    if (productBmp == null) {
                                        android.util.Log.e("Pinster", "All image downloads failed for: ${imageUrls.take(3)}")
                                        runOnUiThread { showReady(seo) }
                                        return@Thread
                                    }

                                    val angles = PosterRenderer.renderAll(
                                        this@MainActivity,
                                        productBmp,
                                        seo.title,
                                        seo.description,
                                        seo,
                                        product,
                                        productImgUrl
                                    )

                                    runOnUiThread {
                                        showPinResult(seo, angles)
                                    }
                                    } catch (e: Exception) {
                                        runOnUiThread {
                                            showGenerationError(
                                                e.message ?: "Generation failed."
                                            )
                                        }
                                    }
                                }.start()
                            }
                            .onFailure { error ->
                                showGenerationError(
                                    error.message
                                        ?: "Unable to generate SEO."
                                )
                            }
                    }
                },

                onFailure = { error ->

                    runOnUiThread {

                        statusHandler.removeCallbacks(
                            statusRunnable
                        )

                        showGenerationError(
                            error.message
                                ?: "Unable to extract product."
                        )
                    }
                }
            )
        }.start()
    }

    private fun showGenerationError(message: String) {

        content.removeAllViews()

        addSpace(content, 80)

        val title = TextView(this).apply {
            text = "GENERATION FAILED"
            textSize = 20f
            setTextColor(text())
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
        }

        content.addView(
            title,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        addSpace(content, 20)

        val error = TextView(this).apply {
            text = message
            textSize = 14f
            setTextColor(muted())
            gravity = Gravity.CENTER
            setPadding(
                dp(20),
                dp(10),
                dp(20),
                dp(10)
            )
        }

        content.addView(
            error,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        addSpace(content, 28)

        val retry = TextView(this).apply {
            text = "TRY AGAIN"
            textSize = 14f
            setTextColor(
                if (darkMode) Color.BLACK else Color.WHITE
            )
            gravity = Gravity.CENTER
            typeface = Typeface.MONOSPACE

            background = rounded(
                if (darkMode) Color.WHITE else Color.rgb(28, 28, 28),
                18
            )

            setOnClickListener {
                buildInterface()
            }
        }

        content.addView(
            retry,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(54)
            )
        )
    }

    private fun showPinResult(
        result: SeoResult,
        angles: List<PinterestAngle>,
        saveToHistory: Boolean = true
    ) {
        if (angles.isEmpty()) {
            showReady(result)
            return
        }

        val posters = angles.map { it.productBitmap }

        currentAngles = angles
        currentSeoResult = result
        currentSelectedAngleIndex = 0

        if (saveToHistory) {
            saveSeoHistory(result, angles)
            populateRecentPins()
        }

        content.removeAllViews()

        val pageScroll = android.widget.ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
        }

        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(16), 0, dp(40))
        }

        pageScroll.addView(
            page,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        content.addView(
            pageScroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        // Title
        pinReadyTitle = TextView(this).apply {
            text = getString(R.string.pin_ready)
            textSize = 22f
            setTextColor(text())
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
            letterSpacing = 0.08f
        }

        page.addView(
            pinReadyTitle,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        addSpace(page, 4)

        // Preset names & dots (declared here, added to page after carousel)
        val presetNames = angles.map { it.title }
        val dotViews = mutableListOf<TextView>()

        val dots = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        for (i in posters.indices) {
            val dot = TextView(this).apply {
                text = "●"
                textSize = 10f
                gravity = Gravity.CENTER
                setPadding(dp(6), 0, dp(6), 0)
            }
            dotViews.add(dot)
            dots.addView(dot)
        }

        val presetLabel = TextView(this).apply {
            text = presetNames[0]
            textSize = 13f
            setTextColor(muted())
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
            letterSpacing = 0.06f
        }

        // 3D Cover Flow carousel
        val carouselOuter = object : FrameLayout(this) {
            private var intercepting = false
            private var downX = 0f
            private var downY = 0f

            override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
                when (ev.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = ev.rawX
                        downY = ev.rawY
                        intercepting = false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (ev.rawX - downX).let { if (it < 0) -it else it }
                        val dy = (ev.rawY - downY).let { if (it < 0) -it else it }
                        if (dx > dp(8) && dx > dy * 1.5f) {
                            intercepting = true
                            parent?.requestDisallowInterceptTouchEvent(true)
                        }
                    }
                }
                return intercepting
            }
        }
        val carousel = CoverFlowCarousel(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        currentCarousel = carousel

        carousel.setImages(posters)

        carousel.onImageClicked = { idx ->
            openAngleFullscreen(angles, idx, result)
        }

        // Helper to get angle-specific SEO for an index
        fun getAngleSeoForIndex(idx: Int): Triple<String, String, String> {
            if (idx < angles.size && result.angleSeo.isNotEmpty()) {
                val angleSeo = result.angleSeo.find { it.angleId == angles[idx].id }
                if (angleSeo != null) {
                    return Triple(angleSeo.title, angleSeo.description, angleSeo.altText)
                }
            }
            return Triple(result.title, result.description, result.altText)
        }

        // SEO text views - terminal style, monospace
        seoTitleText = TextView(this).apply {
            text = result.title
            textSize = 13f
            setTextColor(text())
            typeface = Typeface.MONOSPACE
            setLineSpacing(0f, 1.0f)
            setPadding(0, dp(4), 0, dp(4))
        }

        seoDescText = TextView(this).apply {
            text = result.description
            textSize = 13f
            setTextColor(text())
            typeface = Typeface.MONOSPACE
            setLineSpacing(0f, 1.0f)
            setPadding(0, dp(4), 0, dp(4))
        }

        seoAltText = TextView(this).apply {
            text = result.altText
            textSize = 13f
            setTextColor(text())
            typeface = Typeface.MONOSPACE
            setLineSpacing(0f, 1.0f)
            setPadding(0, dp(4), 0, dp(4))
        }

        fun updateSeoForAngle(idx: Int) {
            val (t, d, a) = getAngleSeoForIndex(idx)
            seoTitleText?.text = t
            seoDescText?.text = d
            seoAltText?.text = a
            currentSelectedAngleIndex = idx
        }

        carousel.onImageLongPressed = { idx ->
            if (idx < (currentAngles?.size ?: 0)) {
                val angle = currentAngles!![idx]
                angle.isSaved = !angle.isSaved
                showPinnedOverlay(carouselOuter, angle.isSaved)
                updateAnglePinState(idx)
                populateRecentPins()
            }
        }

        carousel.onSelectionChanged = { idx ->
            for (i in dotViews.indices) {
                dotViews[i].setTextColor(
                    if (i == idx) text() else muted()
                )
                dotViews[i].textSize = if (i == idx) 13f else 10f
            }
            presetLabel.text = presetNames[idx]
            updateSeoForAngle(idx)
            val pinned = idx < (currentAngles?.size ?: 0) && currentAngles!![idx].isSaved
            showPinnedOverlay(carouselOuter, pinned)
        }

        // Add carousel first
        page.addView(carouselOuter, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(540)
        ))
        carouselOuter.addView(carousel)

        if (angles.isNotEmpty() && angles[0].isSaved) {
            showPinnedOverlay(carouselOuter, true)
        }

        addSpace(page, 8)

        // Then preset label below carousel
        page.addView(presetLabel)

        addSpace(page, 4)

        // Then dots
        page.addView(dots)

        addSpace(page, 12)

        // Terminal-style SEO sections
        fun addTerminalSection(container: LinearLayout, label: String, textView: TextView) {
            val labelView = TextView(this).apply {
                text = "> $label"
                textSize = 12f
                setTextColor(muted())
                typeface = Typeface.MONOSPACE
                setPadding(0, dp(6), 0, dp(2))
            }
            container.addView(labelView)

            container.addView(textView)

            // Copy icon — two stacked rounded squares, no background
            val clipBtn = object : View(this@MainActivity) {
                override fun onDraw(c: Canvas) {
                    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.STROKE
                        strokeWidth = 1.5f * resources.displayMetrics.density
                        color = muted()
                    }
                    val r = 3f * resources.displayMetrics.density
                    val s = width * 0.36f
                    val ox = width * 0.14f
                    val oy = height * 0.18f
                    c.drawRoundRect(RectF(ox, oy, ox + s, oy + s), r, r, p)
                    c.drawRoundRect(RectF(ox + s * 0.3f, oy + s * 0.3f, ox + s * 1.3f, oy + s * 1.3f), r, r, p)
                }
            }.apply {
                setBackgroundColor(Color.TRANSPARENT)
                setOnClickListener {
                    val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText(label, textView.text.toString()))
                }
            }
            clipBtn.layoutParams = LinearLayout.LayoutParams(dp(28), dp(28))
            container.addView(clipBtn)

            // Divider
            val divider = View(this).apply {
                setBackgroundColor(if (darkMode) Color.rgb(40, 40, 40) else Color.rgb(220, 218, 215))
            }
            container.addView(divider, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1))
        }

        addTerminalSection(page, "TITLE", seoTitleText!!)
        addSpace(page, 8)
        addTerminalSection(page, "DESCRIPTION", seoDescText!!)
        addSpace(page, 8)

        seoAffiliateLink = TextView(this).apply {
            text = result.affiliateLink
            textSize = 13f
            setTextColor(text())
            typeface = Typeface.MONOSPACE
            setLineSpacing(0f, 1.0f)
            setPadding(0, dp(4), 0, dp(4))
        }
        addTerminalSection(page, "AFFILIATE LINK", seoAffiliateLink!!)
        addSpace(page, 8)
        addTerminalSection(page, "ALT TEXT", seoAltText!!)

        addSpace(page, 28)

        // CREATE ANOTHER button
        generateAgainBtn = TextView(this).apply {
            text = "CREATE ANOTHER  \u2726"
            textSize = 15f
            setTextColor(
                if (darkMode) Color.BLACK else Color.WHITE
            )
            gravity = Gravity.CENTER
            typeface = Typeface.MONOSPACE
            background = rounded(
                if (darkMode) Color.WHITE else Color.rgb(28, 28, 28),
                18
            )

            setOnClickListener {
                buildInterface()
            }
        }

        page.addView(
            generateAgainBtn,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(56)
            ).apply {
                setMargins(dp(20), 0, dp(20), 0)
            }
        )

        addSpace(page, 20)
    }

    private fun showSeoDialog(result: SeoResult) {
        val message = """
TITLE:
${result.title}

DESCRIPTION:
${result.description}

ALT TEXT:
${result.altText}

AFFILIATE LINK:
${result.affiliateLink}
        """.trimIndent()

        android.app.AlertDialog.Builder(this)
            .setTitle("SEO Data")
            .setMessage(message)
            .setPositiveButton("CLOSE", null)
            .setNeutralButton("COPY ALL") { _, _ ->
                val clipboard = getSystemService(
                    android.content.Context.CLIPBOARD_SERVICE
                ) as android.content.ClipboardManager
                clipboard.setPrimaryClip(
                    android.content.ClipData.newPlainText("SEO", message)
                )
                Toast.makeText(this, "COPIED ✓", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun saveBitmapToGallery(bitmap: android.graphics.Bitmap) {
        Thread {
            try {
                val values = android.content.ContentValues().apply {
                    put(
                        android.provider.MediaStore.Images.Media.DISPLAY_NAME,
                        "Pinster_Pin_${System.currentTimeMillis()}.jpg"
                    )
                    put(
                        android.provider.MediaStore.Images.Media.MIME_TYPE,
                        "image/jpeg"
                    )
                    put(
                        android.provider.MediaStore.Images.Media.RELATIVE_PATH,
                        android.os.Environment.DIRECTORY_PICTURES + "/Pinster"
                    )
                }

                val uri = contentResolver.insert(
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    values
                )

                if (uri != null) {
                    contentResolver.openOutputStream(uri)?.use { output ->
                        bitmap.compress(
                            android.graphics.Bitmap.CompressFormat.JPEG,
                            100,
                            output
                        )
                    }

                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            "Pin saved to Pictures/Pinster",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (_: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "Save failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }.start()
    }

    private fun shareBitmap(bitmap: android.graphics.Bitmap) {
        try {
            val file = java.io.File(cacheDir, "pinster_pin.jpg")
            java.io.FileOutputStream(file).use { output ->
                bitmap.compress(
                    android.graphics.Bitmap.CompressFormat.JPEG,
                    95,
                    output
                )
            }

            val uri = androidx.core.content.FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )

            val intent = android.content.Intent(
                android.content.Intent.ACTION_SEND
            ).apply {
                type = "image/jpeg"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(
                android.content.Intent.createChooser(intent, "Share pin")
            )
        } catch (_: Exception) {
            Toast.makeText(this, "Share failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openAngleFullscreen(
        angles: List<PinterestAngle>,
        startIndex: Int,
        result: SeoResult
    ) {
        val previousSystemUiVisibility =
            window.decorView.systemUiVisibility

        window.decorView.systemUiVisibility =
            android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
            android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE

        val viewer = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
        }

        val contentScroll = android.widget.ScrollView(this).apply {
            isVerticalScrollBarEnabled = true
        }

        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(60), dp(24), dp(80))
        }

        val angleTitle = TextView(this).apply {
            textSize = 22f
            setTextColor(Color.WHITE)
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
            letterSpacing = 0.08f
        }

        val angleDesc = TextView(this).apply {
            textSize = 13f
            setTextColor(Color.argb(160, 255, 255, 255))
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, dp(20))
        }

        val promptLabel = TextView(this).apply {
            text = getString(R.string.label_image_prompt)
            textSize = 11f
            setTextColor(Color.argb(100, 255, 255, 255))
            typeface = Typeface.MONOSPACE
            letterSpacing = 0.12f
            setPadding(0, dp(16), 0, dp(8))
        }

        val promptText = TextView(this).apply {
            textSize = 13f
            setTextColor(Color.argb(200, 255, 255, 255))
            typeface = Typeface.MONOSPACE
            setLineSpacing(dp(4).toFloat(), 1.15f)
            setTextIsSelectable(true)
        }

        fun updateContent(idx: Int) {
            val angle = angles[idx]
            angleTitle.text = "${String.format("%02d", angle.number)} \u2014 ${angle.title}"
            angleDesc.text = angle.shortDescription
            promptText.text = angle.completePrompt
        }

        updateContent(startIndex)

        contentLayout.addView(angleTitle, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        contentLayout.addView(angleDesc, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        contentLayout.addView(promptLabel, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        contentLayout.addView(promptText, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))

        contentScroll.addView(contentLayout, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))

        viewer.addView(contentScroll, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ))

        val controls = FrameLayout(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
        }

        val back = MenuIconView(this@MainActivity).apply {
            setLineColor(Color.WHITE)
            setProgress(1f)
            setOnClickListener {
                contentScroll.animate().cancel()
                contentScroll.animate()
                    .alpha(0f)
                    .setDuration(220L)
                    .setInterpolator(DecelerateInterpolator())
                    .withEndAction {
                        window.decorView.systemUiVisibility = previousSystemUiVisibility
                        isProductFullscreen = false
                        drawer.visibility = View.VISIBLE
                        menuIcon.visibility = View.VISIBLE
                        menuIcon.isClickable = true
                        menuIcon.isFocusable = true
                        root.removeView(viewer)
                    }
                    .start()
            }
        }

        controls.addView(
            back,
            FrameLayout.LayoutParams(dp(44), dp(44)).apply {
                gravity = Gravity.TOP or Gravity.START
                setMargins(dp(16), dp(16), 0, 0)
            }
        )

        var swipeStartX = 0f
        var currentPage = startIndex

        val more = TextView(this@MainActivity).apply {
            text = "\u22EF"
            textSize = 30f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.TRANSPARENT)
            isClickable = true
            isFocusable = true

            setOnClickListener {
                val menu = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(8), dp(8), dp(8), dp(8))
                    background = android.graphics.drawable.GradientDrawable().apply {
                        setColor(Color.argb(235, 28, 28, 30))
                        cornerRadius = dp(18).toFloat()
                    }
                }

                val popup = android.widget.PopupWindow(
                    menu,
                    dp(240),
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    true
                ).apply {
                    elevation = dp(16).toFloat()
                }

                fun menuOption(label: String, onClick: () -> Unit): TextView {
                    return TextView(this@MainActivity).apply {
                        text = label
                        textSize = 14f
                        setTextColor(Color.WHITE)
                        typeface = Typeface.MONOSPACE
                        setPadding(dp(16), dp(14), dp(16), dp(14))
                        setOnClickListener {
                            popup.dismiss()
                            onClick()
                        }
                    }
                }

                menu.addView(menuOption("Copy Prompt") {
                    val clipboard = getSystemService(
                        android.content.Context.CLIPBOARD_SERVICE
                    ) as android.content.ClipboardManager
                    clipboard.setPrimaryClip(
                        android.content.ClipData.newPlainText(
                            "Image Prompt",
                            angles[currentPage].completePrompt
                        )
                    )
                    Toast.makeText(
                        this@MainActivity,
                        "Prompt copied",
                        Toast.LENGTH_SHORT
                    ).show()
                })

                menu.addView(menuOption("Share Prompt + Image") {
                    shareAngleWithImage(angles[currentPage])
                })

                menu.addView(menuOption("Save Product Image") {
                    saveBitmapToGallery(angles[currentPage].productBitmap)
                })

                popup.showAsDropDown(this, 0, dp(8))
            }
        }

        controls.addView(
            more,
            FrameLayout.LayoutParams(dp(44), dp(44)).apply {
                gravity = Gravity.TOP or Gravity.END
                setMargins(0, dp(16), dp(16), 0)
            }
        )

        viewer.addView(
            controls,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        viewer.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    swipeStartX = event.rawX
                    true
                }
                android.view.MotionEvent.ACTION_UP -> {
                    val dx = event.rawX - swipeStartX
                    if (kotlin.math.abs(dx) > dp(80)) {
                        if (dx < 0 && currentPage < angles.lastIndex) {
                            currentPage++
                        } else if (dx > 0 && currentPage > 0) {
                            currentPage--
                        }
                        updateContent(currentPage)
                        contentScroll.scrollTo(0, 0)
                    }
                    true
                }
                else -> false
            }
        }

        isProductFullscreen = true
        drawer.visibility = View.GONE
        menuIcon.visibility = View.GONE
        menuIcon.isClickable = false
        menuIcon.isFocusable = false

        root.addView(
            viewer,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        contentScroll.alpha = 0f
        contentScroll.animate()
            .alpha(1f)
            .setDuration(280L)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun shareAngleWithImage(angle: PinterestAngle) {
        try {
            val file = java.io.File(cacheDir, "pinster_share_${System.currentTimeMillis()}.jpg")

            val bmp = angle.productBitmap
            if (bmp == null || bmp.isRecycled) {
                Toast.makeText(this, "Image not available", Toast.LENGTH_SHORT).show()
                return
            }

            java.io.FileOutputStream(file).use { output ->
                bmp.compress(
                    android.graphics.Bitmap.CompressFormat.JPEG,
                    95,
                    output
                )
            }

            if (!file.exists() || file.length() == 0L) {
                Toast.makeText(this, "Image save failed", Toast.LENGTH_SHORT).show()
                return
            }

            val uri = androidx.core.content.FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )

            val shareIntent = android.content.Intent(
                android.content.Intent.ACTION_SEND
            ).apply {
                type = "image/jpeg"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                putExtra(android.content.Intent.EXTRA_TEXT, angle.completePrompt)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(
                android.content.Intent.createChooser(
                    shareIntent,
                    "Share prompt + image"
                )
            )
        } catch (e: Exception) {
            android.util.Log.e("Pinster", "Share failed", e)
            Toast.makeText(this, "Share failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showReady(
        result: SeoResult,
        saveToHistory: Boolean = true
    ) {
        if (saveToHistory) saveSeoHistory(result)

        content.removeAllViews()

        val pageScroll = android.widget.ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
        }

        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                dp(2),
                dp(28),
                dp(2),
                dp(40)
            )
        }

        pageScroll.addView(
            page,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )


        content.addView(
            pageScroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        val readyTitle = TextView(this).apply {
            text = "SEO READY"
            textSize = 22f
            setTextColor(text())
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
            letterSpacing = 0.08f
        }

        page.addView(
            readyTitle,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        addSpace(page, 24)


          // PRODUCT IMAGE
          val productImageUrl = result.productImages.firstOrNull()

          if (!productImageUrl.isNullOrBlank()) {

              val imageContainer = LinearLayout(this).apply {
                  orientation = LinearLayout.VERTICAL
                  gravity = Gravity.CENTER
                  background = rounded(inputColor(), 18)
              }

              val imageView = ImageView(this).apply {
                  scaleType = ImageView.ScaleType.CENTER_CROP
                  setBackgroundColor(inputColor())
                  contentDescription = "Product image"
                  isClickable = true
                  isFocusable = true
              }

              imageContainer.addView(
                  imageView,
                  LinearLayout.LayoutParams(
                      ViewGroup.LayoutParams.MATCH_PARENT,
                      dp(320)
                  )
              )

              fun loadProductBitmap(
                  url: String,
                  callback: (android.graphics.Bitmap?) -> Unit
              ) {
                  Thread {
                      var bitmap: android.graphics.Bitmap? = null

                      try {
                          val connection =
                              java.net.URL(url)
                                  .openConnection() as java.net.HttpURLConnection

                          connection.connectTimeout = 20000
                          connection.readTimeout = 20000
                          connection.instanceFollowRedirects = true

                          connection.setRequestProperty(
                              "User-Agent",
                              "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36"
                          )

                          connection.setRequestProperty(
                              "Accept",
                              "image/*,*/*;q=0.8"
                          )

                          connection.setRequestProperty(
                              "Referer",
                              "https://www.amazon.com/"
                          )

                          val bytes =
                              connection.inputStream.use { it.readBytes() }

                          connection.disconnect()

                          bitmap =
                              android.graphics.BitmapFactory.decodeByteArray(
                                  bytes,
                                  0,
                                  bytes.size
                              )

                      } catch (_: Exception) {
                      }

                      runOnUiThread {
                          callback(bitmap)
                      }
                  }.start()
              }

              fun saveProductBitmap(bitmap: android.graphics.Bitmap) {
                  Thread {
                      try {
                          val values =
                              android.content.ContentValues().apply {
                                  put(
                                      android.provider.MediaStore.Images.Media.DISPLAY_NAME,
                                      "DelishStudio_Product_${System.currentTimeMillis()}.jpg"
                                  )
                                  put(
                                      android.provider.MediaStore.Images.Media.MIME_TYPE,
                                      "image/jpeg"
                                  )
                                  put(
                                      android.provider.MediaStore.Images.Media.RELATIVE_PATH,
                                      android.os.Environment.DIRECTORY_PICTURES +
                                          "/DelishStudio"
                                  )
                              }

                          val uri =
                              contentResolver.insert(
                                  android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                  values
                              )

                          if (uri != null) {
                              contentResolver
                                  .openOutputStream(uri)
                                  ?.use { output ->
                                      bitmap.compress(
                                          android.graphics.Bitmap.CompressFormat.JPEG,
                                          100,
                                          output
                                      )
                                  }

                              runOnUiThread {
                                  Toast.makeText(
                                      this@MainActivity,
                                      "Image saved to Pictures/DelishStudio",
                                      Toast.LENGTH_SHORT
                                  ).show()
                              }
                          }

                      } catch (_: Exception) {
                          runOnUiThread {
                              Toast.makeText(
                                  this@MainActivity,
                                  "Save failed",
                                  Toast.LENGTH_SHORT
                              ).show()
                          }
                      }
                  }.start()
              }

              imageView.setOnClickListener {

                  loadProductBitmap(productImageUrl) { bitmap ->

                      if (bitmap == null) {
                          Toast.makeText(
                              this@MainActivity,
                              "Unable to open image",
                              Toast.LENGTH_SHORT
                          ).show()
                          return@loadProductBitmap
                      }

                        // Immersive fullscreen for the image viewer only.
                        // Preserve the existing system UI state so the result screen
                        // returns exactly to its previous appearance.
                        val previousSystemUiVisibility =
                            window.decorView.systemUiVisibility

                        window.decorView.systemUiVisibility =
                            android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                            android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
                            android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                            android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                            android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                            android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE

                        val viewer =
                            android.widget.FrameLayout(this@MainActivity).apply {
                                setBackgroundColor(Color.BLACK)
                            }

                        // Blurred atmospheric background using the same image.
                        val blurredImage =
                            ImageView(this@MainActivity).apply {
                                setImageBitmap(bitmap)
                                scaleType = ImageView.ScaleType.CENTER_CROP
                                alpha = 0.34f
                            }

                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                            blurredImage.setRenderEffect(
                                android.graphics.RenderEffect.createBlurEffect(
                                    dp(30).toFloat(),
                                    dp(30).toFloat(),
                                    android.graphics.Shader.TileMode.CLAMP
                                )
                            )
                        }

                        viewer.addView(
                            blurredImage,
                            android.widget.FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        )

                        // Original product image remains sharp and untouched.
                        val fullImage =
                            ImageView(this@MainActivity).apply {
                                setImageBitmap(bitmap)
                                scaleType = ImageView.ScaleType.FIT_CENTER
                                setBackgroundColor(Color.TRANSPARENT)
                                alpha = 0f
                                scaleX = 0.94f
                                scaleY = 0.94f
                            }

                        viewer.addView(
                            fullImage,
                            android.widget.FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        )

                        // Strong fullscreen radial vignette.
                        val vignette =
                            object : View(this@MainActivity) {

                                private val vignettePaint =
                                    android.graphics.Paint(
                                        android.graphics.Paint.ANTI_ALIAS_FLAG
                                    )

                                override fun onDraw(
                                    canvas: android.graphics.Canvas
                                ) {
                                    super.onDraw(canvas)

                                    val w = width.toFloat()
                                    val h = height.toFloat()

                                    vignettePaint.shader =
                                        android.graphics.RadialGradient(
                                            w / 2f,
                                            h / 2f,
                                            maxOf(w, h) * 0.72f,
                                            intArrayOf(
                                                Color.TRANSPARENT,
                                                Color.argb(38, 0, 0, 0),
                                                Color.argb(125, 0, 0, 0)
                                            ),
                                            floatArrayOf(
                                                0f,
                                                0.52f,
                                                1f
                                            ),
                                            android.graphics.Shader.TileMode.CLAMP
                                        )

                                    canvas.drawRect(
                                        0f,
                                        0f,
                                        w,
                                        h,
                                        vignettePaint
                                    )

                                    vignettePaint.shader = null
                                }
                            }

                        viewer.addView(
                            vignette,
                            android.widget.FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        )

                        // Cinematic black fade at the bottom.
                        val bottomGradient =
                            object : View(this@MainActivity) {

                                private val gradientPaint =
                                    android.graphics.Paint(
                                        android.graphics.Paint.ANTI_ALIAS_FLAG
                                    )

                                override fun onDraw(
                                    canvas: android.graphics.Canvas
                                ) {
                                    super.onDraw(canvas)

                                    val w = width.toFloat()
                                    val h = height.toFloat()

                                    gradientPaint.shader =
                                        android.graphics.LinearGradient(
                                            0f,
                                            h * 0.56f,
                                            0f,
                                            h,
                                            Color.TRANSPARENT,
                                            Color.argb(205, 0, 0, 0),
                                            android.graphics.Shader.TileMode.CLAMP
                                        )

                                    canvas.drawRect(
                                        0f,
                                        h * 0.56f,
                                        w,
                                        h,
                                        gradientPaint
                                    )

                                    gradientPaint.shader = null
                                }
                            }

                        viewer.addView(
                            bottomGradient,
                            android.widget.FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        )

                        val controls =
                            FrameLayout(this@MainActivity).apply {
                                setBackgroundColor(Color.TRANSPARENT)
                            }

                        fun plainIcon(@androidx.annotation.DrawableRes resId: Int): ImageView {
                            return ImageView(this@MainActivity).apply {
                                setImageResource(resId)
                                scaleType = ImageView.ScaleType.CENTER_INSIDE
                                setBackgroundColor(Color.TRANSPARENT)
                                setPadding(0, 0, 0, 0)
                                isClickable = true
                                isFocusable = true
                            }
                        }

                          // Fullscreen exit / interactive any-angle swipe.
                          var fullscreenExiting = false
                          var swipeDownX = 0f
                          var swipeDownY = 0f

                          // Capture the original result-image position BEFORE fullscreen
                          // covers/removes the result screen.
                          val originalResultLocation = IntArray(2)
                          imageView.getLocationOnScreen(originalResultLocation)

                          val originalResultX = originalResultLocation[0].toFloat()
                          val originalResultY = originalResultLocation[1].toFloat()
                          val originalResultWidth = imageView.width.toFloat()
                          val originalResultHeight = imageView.height.toFloat()

                          fun finishFullscreenExit() {
                              window.decorView.systemUiVisibility =
                                  previousSystemUiVisibility

                              // Restore drawer/menu after fullscreen exits.
                              isProductFullscreen = false
                              drawer.visibility = View.VISIBLE
                              menuIcon.visibility = View.VISIBLE
                              menuIcon.isClickable = true
                              menuIcon.isFocusable = true

                              // Remove fullscreen viewer and rebuild the existing
                              // SEO result exactly as before.
                              root.removeView(viewer)
                              showReady(result, false)
                          }

                            fun animateImageBackToResult() {
    if (fullscreenExiting) return
    fullscreenExiting = true
    finishFullscreenExit()
}

fun exitFullscreenToResult() {
                                animateImageBackToResult()
                            }

                            fun dismissFullscreen(
                                directionX: Float,
                                directionY: Float
                            ) {
                                // Swipe direction is only a trigger.
                                // The image NEVER follows the finger.
                                animateImageBackToResult()
                            }

                            fun springBackFullscreen() {
                                fullImage.animate().cancel()

                                fullImage.animate()
                                    .translationX(0f)
                                    .translationY(0f)
                                    .scaleX(1f)
                                    .scaleY(1f)
                                    .alpha(1f)
                                    .setDuration(220L)
                                    .setInterpolator(
                                        android.view.animation.OvershootInterpolator(1.05f)
                                    )
                                    .start()

                                vignette.animate()
                                    .alpha(1f)
                                    .setDuration(180L)
                                    .start()
                            }

                            // Any-angle swipe = TRIGGER ONLY.
                            // Nothing moves while dragging.
                            viewer.setOnTouchListener { _, event ->
                                when (event.actionMasked) {

                                    android.view.MotionEvent.ACTION_DOWN -> {
                                        if (fullscreenExiting) {
                                            false
                                        } else {
                                            swipeDownX = event.rawX
                                            swipeDownY = event.rawY

                                            fullImage.animate().cancel()
                                            vignette.animate().cancel()

                                            true
                                        }
                                    }

                                    android.view.MotionEvent.ACTION_MOVE -> {
                                        // IMPORTANT:
                                        // Do NOT translate viewer or fullImage here.
                                        true
                                    }

                                    android.view.MotionEvent.ACTION_UP -> {
                                        if (fullscreenExiting) {
                                            true
                                        } else {
                                            val dx =
                                                event.rawX - swipeDownX

                                            val dy =
                                                event.rawY - swipeDownY

                                            val movement =
                                                kotlin.math.sqrt(
                                                    dx * dx + dy * dy
                                                )

                                            if (movement >= dp(110).toFloat()) {
                                                dismissFullscreen(dx, dy)
                                            } else {
                                                springBackFullscreen()
                                            }

                                            true
                                        }
                                    }

                                    android.view.MotionEvent.ACTION_CANCEL -> {
                                        if (!fullscreenExiting) {
                                            springBackFullscreen()
                                        }
                                        true
                                    }

                                    else -> true
                                }
                            }

// Plain Pinterest-style pin — no circle.
                        val back =
                            MenuIconView(this@MainActivity).apply {
                                setLineColor(Color.WHITE)
                                setProgress(1f)

                                setOnClickListener {
                                    exitFullscreenToResult()
                                }
                            }

                        val save =
                            plainIcon(com.delish.pinster.R.drawable.ic_viewer_pin).apply {
                                setOnClickListener {
                                    saveProductBitmap(bitmap)
                                }
                            }

                        // Standard 3-node share icon — no circle.
                        val share =
                            plainIcon(com.delish.pinster.R.drawable.ic_viewer_share).apply {
                                setOnClickListener {
                                    try {
                                        val file =
                                            java.io.File(
                                                cacheDir,
                                                "delishstudio_product.jpg"
                                            )

                                        java.io.FileOutputStream(file).use { output ->
                                            bitmap.compress(
                                                android.graphics.Bitmap.CompressFormat.JPEG,
                                                95,
                                                output
                                            )
                                        }

                                        val uri =
                                            androidx.core.content.FileProvider.getUriForFile(
                                                this@MainActivity,
                                                "${packageName}.fileprovider",
                                                file
                                            )

                                        val intent =
                                            android.content.Intent(
                                                android.content.Intent.ACTION_SEND
                                            ).apply {
                                                type = "image/jpeg"
                                                putExtra(
                                                    android.content.Intent.EXTRA_STREAM,
                                                    uri
                                                )
                                                addFlags(
                                                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                )
                                            }

                                        startActivity(
                                            android.content.Intent.createChooser(
                                                intent,
                                                "Share product image"
                                            )
                                        )
                                    } catch (_: Exception) {
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Share failed",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }

                        val backParams =
                            FrameLayout.LayoutParams(
                                dp(44),
                                dp(44)
                            ).apply {
                                gravity = Gravity.TOP or Gravity.START
                                setMargins(
                                    dp(16),
                                    dp(24),
                                    0,
                                    0
                                )
                            }

                          val more = TextView(this@MainActivity).apply {
    text = "⋯"
    textSize = 30f
    gravity = Gravity.CENTER
    setTextColor(Color.WHITE)
    setBackgroundColor(Color.TRANSPARENT)
    isClickable = true
    isFocusable = true

    setOnClickListener {
        val menu = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(8), dp(8), dp(8))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.argb(235, 28, 28, 30))
                cornerRadius = dp(18).toFloat()
            }
        }

        val popup = PopupWindow(
            menu,
            dp(240),
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            setBackgroundDrawable(
                android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)
            )
            isOutsideTouchable = true
            elevation = dp(12).toFloat()
        }

        fun addOption(icon: String, label: String, action: () -> Unit) {
            val row = TextView(this@MainActivity).apply {
                text = "$icon   $label"
                textSize = 16f
                gravity = Gravity.CENTER_VERTICAL
                setTextColor(Color.WHITE)
                setPadding(dp(18), 0, dp(18), 0)
                isClickable = true

                setOnClickListener {
                    popup.dismiss()
                    action()
                }
            }

            menu.addView(
                row,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(56)
                )
            )
        }

          fun shareImage(errorText: String) {
              try {
                  val file = java.io.File(cacheDir, "pinster_share.jpg")
                  java.io.FileOutputStream(file).use { output ->
                      bitmap.compress(
                          android.graphics.Bitmap.CompressFormat.JPEG,
                          95,
                          output
                      )
                  }

                  val uri = androidx.core.content.FileProvider.getUriForFile(
                      this@MainActivity,
                      "${this@MainActivity.packageName}.fileprovider",
                      file
                  )

                  val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                      type = "image/*"
                      putExtra(android.content.Intent.EXTRA_STREAM, uri)
                      addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                  }

                  startActivity(android.content.Intent.createChooser(intent, "Share product image"))
              } catch (_: Exception) {
                  Toast.makeText(
                      this@MainActivity,
                      errorText,
                      Toast.LENGTH_SHORT
                  ).show()
              }
          }

        addOption("↗", "Share") {
              shareImage("Unable to share")
          }

        addOption("＋", "Save") {
            saveProductBitmap(bitmap)
        }

        popup.showAtLocation(
              viewer,
              Gravity.TOP or Gravity.END,
              dp(16),
              dp(80)
          )
    }
}

val moreParams =
    FrameLayout.LayoutParams(
        dp(52),
        dp(52)
    ).apply {
        gravity = Gravity.TOP or Gravity.END
        setMargins(
            0,
            dp(18),
            dp(10),
            0
        )
    }

controls.addView(more, moreParams)

val controlsParams =
                            FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            ).apply {
                                gravity = Gravity.TOP
                            }

                        viewer.addView(
                            controls,
                            controlsParams
                        )

                        // Hide the app drawer/menu while fullscreen viewer is open.
                        // Android status bar/system UI is intentionally untouched.
                        isProductFullscreen = true
                        drawer.visibility = View.GONE
                        menuIcon.visibility = View.GONE
                        menuIcon.isClickable = false
                        menuIcon.isFocusable = false


                        content.removeAllViews()

                          root.addView(
                              viewer,
                              FrameLayout.LayoutParams(
                                  ViewGroup.LayoutParams.MATCH_PARENT,
                                  ViewGroup.LayoutParams.MATCH_PARENT
                              ).apply {
                                  gravity = Gravity.TOP or Gravity.START
                              }
                          )

                        // Seamless result -> fullscreen transition.
                        fullImage.animate()
                            .alpha(1f)
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(220L)
                            .setInterpolator(
                                android.view.animation.DecelerateInterpolator()
                            )
                            .start()

            }
        }

page.addView(
                  imageContainer,
                  LinearLayout.LayoutParams(
                      ViewGroup.LayoutParams.MATCH_PARENT,
                      dp(320)
                  )
              )

              loadProductBitmap(productImageUrl) { bitmap ->
                  if (bitmap != null) {
                      imageView.setImageBitmap(bitmap)
                      imageView.invalidate()
                  }
              }
          }

        addSpace(page, 24)
              addCopySection(page, "TITLE", result.title, dp(90))
          addSpace(page, 18)

          addCopySection(page, "DESCRIPTION", result.description, dp(230))
          addSpace(page, 18)

          addCopySection(page, "ALT TEXT", result.altText, dp(150))
          addSpace(page, 18)

          addCopySection(
              page,
              "AFFILIATE LINK",
              result.affiliateLink,
              dp(40)
          )

          addSpace(page, 28)

          val generateAgain = TextView(this).apply {
              text = "CREATE PIN  ✦"
              textSize = 15f
              setTextColor(
                  if (darkMode) Color.BLACK else Color.WHITE
              )
              gravity = Gravity.CENTER
              typeface = Typeface.MONOSPACE
              background = rounded(
                  if (darkMode) Color.WHITE else Color.rgb(28, 28, 28),
                  18
              )

              setOnClickListener {
                  buildInterface()
              }
          }

          page.addView(
              generateAgain,
              LinearLayout.LayoutParams(
                  ViewGroup.LayoutParams.MATCH_PARENT,
                  dp(56)
              )
          )

          addSpace(page, 20)

      }
private fun addCopySection(
        container: LinearLayout,
        label: String,
        value: String,
        minHeight: Int
    ) {

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                dp(16),
                dp(14),
                dp(16),
                dp(14)
            )
            background = rounded(inputColor(), 18)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val labelView = TextView(this).apply {
            text = label
            textSize = 11f
            setTextColor(muted())
            typeface = Typeface.MONOSPACE
            letterSpacing = 0.12f
        }

        header.addView(
            labelView,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        val copyButton = TextView(this).apply {
            text = "COPY"
            textSize = 11f
            setTextColor(if (darkMode) Color.rgb(28, 28, 28) else Color.WHITE)
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
            setPadding(
                dp(14),
                dp(9),
                dp(14),
                dp(9)
            )
            background = rounded(if (darkMode) Color.WHITE else Color.rgb(28, 28, 28), 12)

            setOnClickListener {

                val clipboard =
                    getSystemService(
                        android.content.Context.CLIPBOARD_SERVICE
                    ) as android.content.ClipboardManager

                clipboard.setPrimaryClip(
                    android.content.ClipData.newPlainText(
                        label,
                        value
                    )
                )

                text = "COPIED ✓"

                postDelayed({
                    text = "COPY"
                }, 1600)
            }
        }

        header.addView(
            copyButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        card.addView(
            header,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        addSpace(card, 10)

        val valueScroll = android.widget.ScrollView(this)

        val valueView = TextView(this).apply {
            text = value
            textSize = 15f
            setTextColor(text())
            setLineSpacing(dp(5).toFloat(), 1f)
            setPadding(0, dp(4), 0, dp(4))
            setTextIsSelectable(true)
        }

        valueScroll.addView(
            valueView,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )


        card.addView(
            valueScroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                minHeight
            )
        )

        container.addView(
            card,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
    }

    private fun populateRecentPins() {
        if (!::recentPinsList.isInitialized) return
        recentPinsList.removeAllViews()
        val historyPrefs = getSharedPreferences("pinster_history", MODE_PRIVATE)
        val historyArray = org.json.JSONArray(historyPrefs.getString("items", "[]") ?: "[]")

        if (historyArray.length() == 0) {
            recentPinsList.addView(TextView(this).apply {
                text = "No pins yet"
                textSize = 13f
                setTextColor(muted())
                setPadding(0, dp(8), 0, dp(8))
            })
        } else {
            val query = currentSearchQuery?.lowercase()
            val maxShow = if (query.isNullOrBlank()) historyArray.length() else minOf(historyArray.length(), 4)
            var visibleCount = 0
            for (index in 0 until maxShow) {
                val item = historyArray.optJSONObject(index) ?: continue
                val pinTitle = item.optString("pinTitle", "")
                val productTitle = if (pinTitle.isNotBlank()) pinTitle else item.optString("productTitle", "").ifBlank { item.optString("title", "Untitled") }
                val productBrand = item.optString("productBrand", "")
                val displayName = if (pinTitle.isNotBlank()) {
                    pinTitle
                } else if (productBrand.isNotBlank()) {
                    "$productBrand - $productTitle"
                } else {
                    productTitle
                }

                // Filter by search query
                if (query != null) {
                    val matches = displayName.lowercase().contains(query) ||
                            productTitle.lowercase().contains(query) ||
                            productBrand.lowercase().contains(query)
                    if (!matches) continue
                }

                visibleCount++

                val anglesArray = item.optJSONArray("angles")
                var savedCount = 0
                if (anglesArray != null) {
                    for (i in 0 until anglesArray.length()) {
                        val angleObj = anglesArray.optJSONObject(i)
                        if (angleObj != null && angleObj.optBoolean("isSaved", false)) savedCount++
                    }
                }

                val itemView = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(0, dp(10), 0, dp(10))
                    setOnClickListener {
                        showHistoryResult(item)
                        closeDrawer()
                    }
                }

                itemView.addView(TextView(this).apply {
                    text = displayName
                    textSize = 13f
                    setTextColor(text())
                    maxLines = 2
                    ellipsize = android.text.TextUtils.TruncateAt.END
                })

                val dotsRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(0, dp(4), 0, 0)
                }
                for (d in 0 until 5) {
                    val dot = TextView(this).apply {
                        text = if (d < savedCount) "\u25CF" else "\u25CB"
                        textSize = 10f
                        setTextColor(if (d < savedCount) text() else muted())
                        setPadding(0, 0, dp(6), 0)
                    }
                    dotsRow.addView(dot)
                }
                itemView.addView(dotsRow)

                val swipeWrapper = SwipeToDeleteLayout(this) {
                    removeFromHistory(index)
                }
                swipeWrapper.addView(itemView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                swipeWrapper.setOnClickListener {
                    showHistoryResult(item)
                    closeDrawer()
                }

                recentPinsList.addView(swipeWrapper, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            }

            // Show "View All" if there are more pins than displayed
            if (historyArray.length() > 4 && query.isNullOrBlank()) {
                recentPinsList.addView(TextView(this).apply {
                    text = "View All (${historyArray.length()})"
                    textSize = 12f
                    setTextColor(Color.rgb(100, 160, 255))
                    typeface = Typeface.MONOSPACE
                    setPadding(0, dp(8), 0, dp(8))
                    setOnClickListener {
                        // Expand to show all pins
                        currentSearchQuery = null
                        populateRecentPins()
                    }
                })
            }

            if (query != null && visibleCount == 0) {
                recentPinsList.addView(TextView(this).apply {
                    text = "No matching pins"
                    textSize = 13f
                    setTextColor(muted())
                    setPadding(0, dp(8), 0, dp(8))
                })
            }
        }
    }

    private fun removeFromHistory(index: Int) {
        val prefs = getSharedPreferences("pinster_history", MODE_PRIVATE)
        val raw = prefs.getString("items", "[]") ?: "[]"
        val array = try {
            org.json.JSONArray(raw)
        } catch (_: Exception) {
            org.json.JSONArray("[]")
        }

        if (index in 0 until array.length()) {
            array.remove(index)
            prefs.edit().putString("items", array.toString()).apply()
        }

        // Haptic feedback
        try {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                vibrator.vibrate(android.os.VibrationEffect.createOneShot(40, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(40)
            }
        } catch (_: Exception) {}

        populateRecentPins()
    }

    private var currentSearchQuery: String? = null

    private fun findHistoryByUrl(url: String): org.json.JSONObject? {
        val prefs = getSharedPreferences("pinster_history", MODE_PRIVATE)
        val array = try {
            org.json.JSONArray(prefs.getString("items", "[]") ?: "[]")
        } catch (_: Exception) {
            return null
        }
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            if (item.optString("affiliateLink", "") == url) {
                return item
            }
        }
        return null
    }

    private fun updateAnglePinState(angleIdx: Int) {
        val result = currentSeoResult ?: return
        val angles = currentAngles ?: return
        if (angleIdx >= angles.size) return

        val prefs = getSharedPreferences("pinster_history", MODE_PRIVATE)
        val raw = prefs.getString("items", "[]") ?: "[]"
        val array = try {
            org.json.JSONArray(raw)
        } catch (_: Exception) { return }

        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            if (item.optString("affiliateLink", "") == result.affiliateLink) {
                val anglesArr = item.optJSONArray("angles") ?: continue
                if (angleIdx < anglesArr.length()) {
                    val angleObj = anglesArr.optJSONObject(angleIdx) ?: continue
                    angleObj.put("isSaved", angles[angleIdx].isSaved)
                    prefs.edit().putString("items", array.toString()).commit()
                }
                break
            }
        }
    }

    private fun saveSeoHistory(result: SeoResult, angles: List<PinterestAngle> = emptyList(), productImageUrl: String = "") {
        synchronized(this) {
            try {
                val prefs = getSharedPreferences(
                    "pinster_history",
                    MODE_PRIVATE
                )

                val raw = prefs.getString("items", "[]") ?: "[]"
                val array = try {
                    org.json.JSONArray(raw)
                } catch (_: Exception) {
                    org.json.JSONArray("[]")
                }

                val imageArray = org.json.JSONArray()
                result.productImages.take(3).forEach { imageArray.put(it) }

                val anglesArray = org.json.JSONArray()
                for (angle in angles) {
                    val angleObj = org.json.JSONObject()
                        .put("id", angle.id)
                        .put("title", angle.title)
                        .put("number", angle.number)
                        .put("shortDescription", angle.shortDescription)
                        .put("prompt", angle.completePrompt)
                        .put("isSaved", angle.isSaved)
                    anglesArray.put(angleObj)
                }

                // Build angle-specific SEO JSON
                val angleSeoArray = org.json.JSONArray()
                for (seo in result.angleSeo) {
                    angleSeoArray.put(
                        org.json.JSONObject()
                            .put("angleId", seo.angleId)
                            .put("title", seo.title)
                            .put("description", seo.description)
                            .put("altText", seo.altText)
                    )
                }

                val item = org.json.JSONObject()
                    .put("title", result.title)
                    .put("description", result.description)
                    .put("altText", result.altText)
                    .put("affiliateLink", result.affiliateLink)
                    .put("productImages", imageArray)
                    .put("angles", anglesArray)
                    .put("time", System.currentTimeMillis())
                    .put("productTitle", result.productTitle)
                    .put("productBrand", result.productBrand)
                    .put("angleSeo", angleSeoArray)
                    .put("imageUrl", productImageUrl)

                // Append every generation instead of replacing index 0.
                array.put(item)

                // Newest item goes first.
                val lastIndex = array.length() - 1
                if (lastIndex > 0) {
                    val newest = array.remove(lastIndex)
                    val reordered = org.json.JSONArray()
                    reordered.put(newest)

                    for (i in 0 until array.length()) {
                        val elem = array.opt(i) ?: continue
                        reordered.put(elem)
                    }

                    while (array.length() > 0) {
                        array.remove(array.length() - 1)
                    }

                    for (i in 0 until reordered.length()) {
                        val elem = reordered.opt(i) ?: continue
                        array.put(elem)
                    }
                }

                // Keep up to 30 generations.
                while (array.length() > 30) {
                    array.remove(array.length() - 1)
                }

                // COMMIT instead of APPLY:
                // guarantees the updated history is written before
                // another generation can read/save it.
                prefs.edit()
                    .putString("items", array.toString())
                    .commit()

                // Generate AI pin title async
                generatePinTitleAsync(result.productTitle, result.title)

            } catch (_: Exception) {
                // History must never break generation.
            }
        }
    }

    private fun showHistory() {

        val prefs = getSharedPreferences(
            "pinster_history",
            MODE_PRIVATE
        )

        val array = org.json.JSONArray(
            prefs.getString("items", "[]") ?: "[]"
        )

        if (array.length() == 0) {

            android.app.AlertDialog.Builder(this)
                .setTitle("History")
                .setMessage("No generations yet.")
                .setPositiveButton("Close", null)
                .show()

            return
        }

        val titles = Array(array.length()) { index ->
            val item = array.optJSONObject(index)

            item?.optString(
                "title",
                "Untitled"
            ) ?: "Untitled"
        }

        android.app.AlertDialog.Builder(this)
            .setTitle("History")
            .setItems(titles) { _, which ->

                val item = array.optJSONObject(which)

                if (item != null) {
                    showHistoryResult(item)
                }
            }
            .setNegativeButton("Close", null)
            .setNeutralButton("Clear") { _, _ ->

                android.app.AlertDialog.Builder(this)
                    .setTitle("Clear History?")
                    .setMessage(
                        "All saved SEO generations will be removed."
                    )
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Clear") { _, _ ->

                        prefs.edit()
                            .remove("items")
                            .apply()
                    }
                    .show()
            }
            .show()
    }

    private fun showHistoryResult(
        item: org.json.JSONObject
    ) {

        val savedImages = mutableListOf<String>()
        val imageArray = item.optJSONArray("productImages")

        if (imageArray != null) {
            for (i in 0 until minOf(imageArray.length(), 3)) {
                val imageUrl = imageArray.optString(i).trim()
                if (imageUrl.isNotBlank()) {
                    savedImages.add(imageUrl)
                }
            }
        }

        // Load angle-specific SEO
        val angleSeoList = mutableListOf<AngleSeoResult>()
        val angleSeoArray = item.optJSONArray("angleSeo")
        if (angleSeoArray != null) {
            for (i in 0 until angleSeoArray.length()) {
                val obj = angleSeoArray.optJSONObject(i) ?: continue
                angleSeoList.add(AngleSeoResult(
                    angleId = obj.optString("angleId", ""),
                    title = obj.optString("title", ""),
                    description = obj.optString("description", ""),
                    altText = obj.optString("altText", "")
                ))
            }
        }

        val result = SeoResult(
            title = item.optString("title"),
            description = item.optString("description"),
            altText = item.optString("altText"),
            affiliateLink = item.optString("affiliateLink"),
            productImages = savedImages,
            productTitle = item.optString("productTitle", ""),
            productBrand = item.optString("productBrand", ""),
            angleSeo = angleSeoList
        )

        val anglesArray = item.optJSONArray("angles")
        if (anglesArray != null && anglesArray.length() > 0) {
            val savedAngles = mutableListOf<org.json.JSONObject>()
            for (i in 0 until anglesArray.length()) {
                val angleObj = anglesArray.optJSONObject(i)
                if (angleObj != null) savedAngles.add(angleObj)
            }
            val angles = PinterestAngleBuilder.buildAllFromHistory(savedAngles, null, result)
            showPinResult(result, angles, saveToHistory = false)

            val imageUrl = savedImages.firstOrNull()
            if (!imageUrl.isNullOrBlank()) {
                Thread {
                    try {
                        val conn = java.net.URL(imageUrl)
                            .openConnection() as java.net.HttpURLConnection
                        conn.connectTimeout = 20000
                        conn.readTimeout = 20000
                        conn.instanceFollowRedirects = true
                        conn.setRequestProperty("User-Agent",
                            "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36")
                        conn.setRequestProperty("Accept", "image/*,*/*;q=0.8")
                        conn.setRequestProperty("Referer", "https://www.amazon.com/")
                        val bytes = conn.inputStream.use { it.readBytes() }
                        conn.disconnect()
                        val bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        if (bmp != null) {
                            runOnUiThread {
                                currentAngles?.forEach { it.productBitmap = bmp }
                                currentCarousel?.setImages(currentAngles?.map { it.productBitmap } ?: emptyList())
                            }
                        }
                    } catch (_: Exception) {
                    }
                }.start()
            }
        } else {
            showReady(result, saveToHistory = false)
        }
    }

    private fun loadHistory() {
        // History is loaded on demand through showHistory().
    }

    private var activePopup: android.widget.PopupWindow? = null

    private fun populateInspectorPanel() {
        drawerInspectorPanel.removeAllViews()
        val conversations = ChatHistoryManager.getConversations(this)

        if (conversations.isNotEmpty()) {
            val query = searchConvosInput?.text?.toString()?.trim()?.lowercase()

            // Sort: pinned first, then by time
            val sorted = conversations.sortedByDescending { it.optBoolean("isPinned", false) }

            var visibleCount = 0
            for (convo in sorted) {
                val chatTitle = convo.optString("title", "New Chat")
                val chatId = convo.optString("id")
                val isPinned = convo.optBoolean("isPinned", false)

                if (!query.isNullOrBlank() && !chatTitle.lowercase().contains(query)) continue

                visibleCount++
                val isActive = chatId == currentChatId
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(8), dp(10), dp(8), dp(10))
                    background = GradientDrawable().apply {
                        setColor(if (isActive) Color.argb(30, 100, 160, 255) else Color.TRANSPARENT)
                        cornerRadius = dp(8).toFloat()
                    }
                    setOnClickListener {
                        closeDrawer()
                        loadChatConversation(chatId)
                    }
                    setOnLongClickListener { v ->
                        showConvoPopup(v, chatId, chatTitle)
                        true
                    }
                }

                if (isPinned) {
                    row.addView(NavPinIconView(this).apply {
                        layoutParams = android.widget.LinearLayout.LayoutParams(dp(16), dp(16))
                    })
                }

                row.addView(TextView(this).apply {
                    text = chatTitle
                    textSize = 13f
                    setTextColor(if (isActive) text() else muted())
                    typeface = if (isActive) Typeface.DEFAULT else Typeface.MONOSPACE
                    maxLines = 1
                    ellipsize = android.text.TextUtils.TruncateAt.END
                    layoutParams = android.widget.LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    setPadding(if (isPinned) dp(6) else 0, 0, 0, 0)
                })

                drawerInspectorPanel.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            }

            if (!query.isNullOrBlank() && visibleCount == 0) {
                drawerInspectorPanel.addView(TextView(this).apply {
                    text = "No matching conversations"
                    textSize = 13f
                    setTextColor(muted())
                    setPadding(0, dp(10), 0, dp(10))
                }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            }
        }
    }

    private fun showConvoPopup(anchor: View, chatId: String, chatTitle: String) {
        activePopup?.dismiss()

        val d = resources.displayMetrics.density
        val popupView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(6), dp(12), dp(6))
            background = GradientDrawable().apply {
                setColor(if (darkMode) Color.rgb(35, 35, 35) else Color.WHITE)
                cornerRadius = dp(10).toFloat()
                setStroke(1, if (darkMode) Color.rgb(55, 55, 55) else Color.rgb(220, 220, 220))
            }
            elevation = 8f * d
        }

        fun addPopupItem(label: String, iconView: View, textColor: Int, onClick: () -> Unit) {
            val row = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(8), dp(8), dp(8), dp(8))
                setOnClickListener {
                    activePopup?.dismiss()
                    onClick()
                }
            }
            row.addView(iconView, LinearLayout.LayoutParams(dp(20), dp(20)))
            row.addView(TextView(this@MainActivity).apply {
                text = label
                textSize = 13f
                setTextColor(textColor)
                typeface = Typeface.MONOSPACE
                setPadding(dp(10), 0, 0, 0)
            })
            popupView.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }

        addPopupItem("Pin", PopupPinIconView(this, muted()), muted()) {
            ChatHistoryManager.togglePin(this, chatId)
            populateInspectorPanel()
        }
        addPopupItem("Rename", PopupPencilIconView(this, muted()), muted()) {
            showRenameDialog(chatId, chatTitle)
        }
        addPopupItem("Delete", PopupTrashIconView(this, Color.rgb(220, 50, 50)), Color.rgb(220, 50, 50)) {
            ChatHistoryManager.deleteConversation(this, chatId)
            populateInspectorPanel()
        }

        val popup = android.widget.PopupWindow(popupView, (200 * d).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT, true)
        popup.elevation = 10f * d
        popup.showAsDropDown(anchor, 0, 4)
        activePopup = popup
    }

    private fun showRenameDialog(chatId: String, currentTitle: String) {
        val d = resources.displayMetrics.density

        val overlay = View(this).apply {
            setBackgroundColor(Color.argb(120, 0, 0, 0))
        }

        val dialogBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((20 * d).toInt(), (16 * d).toInt(), (20 * d).toInt(), (16 * d).toInt())
            background = GradientDrawable().apply {
                setColor(if (darkMode) Color.rgb(35, 35, 35) else Color.WHITE)
                cornerRadius = 16f * d
            }
            elevation = 16f * d
        }

        dialogBox.addView(TextView(this).apply {
            text = "Rename Chat"
            textSize = 15f
            setTextColor(text())
            typeface = Typeface.MONOSPACE
            setPadding(0, 0, 0, (12 * d).toInt())
        })

        val input = EditText(this).apply {
            setText(currentTitle)
            setTextColor(text())
            setHintTextColor(muted())
            typeface = Typeface.MONOSPACE
            textSize = 14f
            setPadding((12 * d).toInt(), (10 * d).toInt(), (12 * d).toInt(), (10 * d).toInt())
            background = GradientDrawable().apply {
                setColor(if (darkMode) Color.rgb(28, 28, 28) else Color.rgb(245, 244, 242))
                cornerRadius = 10f * d
                setStroke(1, if (darkMode) Color.rgb(55, 55, 55) else Color.rgb(220, 220, 220))
            }
            isSingleLine = true
            setSelection(text.length)
        }
        dialogBox.addView(input, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            setPadding(0, (14 * d).toInt(), 0, 0)
        }

        btnRow.addView(TextView(this).apply {
            text = "Cancel"
            textSize = 13f
            setTextColor(muted())
            typeface = Typeface.MONOSPACE
            setPadding((14 * d).toInt(), (10 * d).toInt(), (14 * d).toInt(), (10 * d).toInt())
            setOnClickListener { dialogPopup?.dismiss() }
        })

        btnRow.addView(TextView(this).apply {
            text = "Save"
            textSize = 13f
            setTextColor(if (darkMode) Color.BLACK else Color.WHITE)
            typeface = Typeface.MONOSPACE
            setPadding((14 * d).toInt(), (10 * d).toInt(), (14 * d).toInt(), (10 * d).toInt())
            background = GradientDrawable().apply {
                setColor(if (darkMode) Color.WHITE else Color.rgb(28, 28, 28))
                cornerRadius = 10f * d
            }
            setOnClickListener {
                val newTitle = input.text.toString().trim()
                if (newTitle.isNotBlank()) {
                    ChatHistoryManager.updateTitle(this@MainActivity, chatId, newTitle)
                    populateInspectorPanel()
                }
                dialogPopup?.dismiss()
            }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            marginStart = (8 * d).toInt()
        })

        dialogBox.addView(btnRow)

        val container = FrameLayout(this)
        container.addView(overlay, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        container.addView(dialogBox, FrameLayout.LayoutParams((280 * d).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.CENTER
        })

        dialogPopup = android.widget.PopupWindow(container, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, true)
        dialogPopup!!.elevation = 20f * d
        dialogPopup!!.showAtLocation(root, Gravity.CENTER, 0, 0)

        overlay.setOnClickListener { dialogPopup?.dismiss() }
        input.post { input.requestFocus() }
    }

    private var dialogPopup: android.widget.PopupWindow? = null

    private fun showDeleteChatDialog(chatId: String, title: String) {
        android.app.AlertDialog.Builder(this)
            .setTitle("Delete Chat")
            .setMessage("Delete \"$title\"?")
            .setPositiveButton("Delete") { _, _ ->
                ChatHistoryManager.deleteConversation(this, chatId)
                populateInspectorPanel()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /*
     * Returns a small grayscale perceptual fingerprint.
     * Images that are visually the same but have different
     * URLs/resolutions will produce very similar fingerprints.
     */
    private fun imageFingerprint(bitmap: android.graphics.Bitmap): IntArray {
        val size = 16

        val scaled = android.graphics.Bitmap.createScaledBitmap(
            bitmap,
            size,
            size,
            true
        )

        val pixels = IntArray(size * size)

        scaled.getPixels(
            pixels,
            0,
            size,
            0,
            0,
            size,
            size
        )

        val gray = IntArray(pixels.size)

        for (i in pixels.indices) {
            val color = pixels[i]

            val r = android.graphics.Color.red(color)
            val g = android.graphics.Color.green(color)
            val b = android.graphics.Color.blue(color)

            gray[i] =
                (r * 299 + g * 587 + b * 114) / 1000
        }

        scaled.recycle()

        return gray
    }

    /*
     * Compare two image fingerprints using mean absolute difference.
     */
    private fun imagesVisuallySame(
        first: IntArray,
        second: IntArray
    ): Boolean {
        if (first.size != second.size) return false

        var difference = 0L

        for (i in first.indices) {
            difference += kotlin.math.abs(
                first[i] - second[i]
            )
        }

        val averageDifference =
            difference.toDouble() / first.size

        // Low value = essentially the same visual image.
        return averageDifference < 8.0
    }

    private fun showPinnedOverlay(parent: ViewGroup, show: Boolean) {
        val existing = parent.findViewWithTag<View>("pinned_overlay")
        if (!show) {
            existing?.let { parent.removeView(it) }
            return
        }
        if (existing != null) return

        val pinnedText = TextView(this).apply {
            text = getString(R.string.pinned)
            textSize = 16f
            setTextColor(Color.WHITE)
            typeface = Typeface.MONOSPACE
            letterSpacing = 0.2f
            gravity = Gravity.CENTER
            setPadding(dp(20), dp(10), dp(20), dp(10))
            background = GradientDrawable().apply {
                setColor(Color.argb(160, 0, 0, 0))
                setStroke(dp(2), Color.WHITE)
                cornerRadius = dp(4).toFloat()
            }
            tag = "pinned_overlay"
        }

        val lp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER
        }

        parent.addView(pinnedText, lp)
    }

    private fun rounded(
        color: Int,
        radius: Int
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }
    }

    private fun addSpace(
        parent: LinearLayout,
        amount: Int
    ) {
        parent.addView(
            Space(this),
            LinearLayout.LayoutParams(
                1,
                dp(amount)
            )
        )
    }

    private fun dp(value: Int): Int {
        return (
            value * resources.displayMetrics.density
        ).toInt()
    }
}

