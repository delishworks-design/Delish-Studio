package com.delish.pinster

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Canvas
import android.graphics.Paint
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.animation.ValueAnimator
import android.widget.*
import android.graphics.drawable.GradientDrawable
import java.util.concurrent.Executors

class SettingsActivity : Activity() {

    private lateinit var settings: SecureSettings

    private lateinit var providerInput: EditText
    private lateinit var endpointInput: EditText
    private lateinit var modelInput: EditText
    private lateinit var keyInput: EditText
    private lateinit var browserlessKeyInput: EditText

    private val executor = Executors.newSingleThreadExecutor()

    private val darkBg = Color.rgb(18, 18, 18)
    private val darkSurface = Color.rgb(28, 28, 28)
    private val darkText = Color.rgb(245, 245, 243)
    private val darkMuted = Color.rgb(130, 127, 123)
    private val darkInput = Color.rgb(35, 35, 35)
    private val accentWhite = Color.WHITE

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun rounded(color: Int, radius: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }
    }

    // Bouncing dots animation view
    private inner class BouncingDotsView(context: android.content.Context) : View(context) {
        private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
        }
        private val dotRadius = 4f * resources.displayMetrics.density
        private val dotSpacing = 8f * resources.displayMetrics.density
        private var offset = 0f
        private var running = false
        private var anim: ValueAnimator? = null

        fun start() {
            running = true
            anim = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 600
                repeatCount = ValueAnimator.INFINITE
                interpolator = DecelerateInterpolator()
                addUpdateListener {
                    offset = it.animatedValue as Float
                    invalidate()
                }
                start()
            }
        }

        fun stop() {
            running = false
            anim?.cancel()
            anim = null
            offset = 0f
            invalidate()
        }

        override fun onDraw(c: Canvas) {
            if (!running) return
            val cy = height / 2f
            val totalWidth = dotRadius * 2 * 3 + dotSpacing * 2
            val startX = (width - totalWidth) / 2f

            for (i in 0..2) {
                val bounce = Math.sin((offset * Math.PI * 2) + (i * Math.PI * 0.7)).toFloat()
                val y = cy + bounce * dotRadius * 1.5f
                val x = startX + i * (dotRadius * 2 + dotSpacing) + dotRadius
                c.drawCircle(x, y, dotRadius, dotPaint)
            }
        }

        override fun onMeasure(w: Int, h: Int) {
            val wSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
            val hSpec = MeasureSpec.makeMeasureSpec(dp(24), MeasureSpec.EXACTLY)
            super.onMeasure(wSpec, hSpec)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        settings = SecureSettings(this)

        val root = ScrollView(this).apply {
            setBackgroundColor(darkBg)
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(40), dp(24), dp(32))
        }

        // Title
        val title = TextView(this).apply {
            text = "SETTINGS"
            textSize = 13f
            setTextColor(darkMuted)
            typeface = android.graphics.Typeface.MONOSPACE
            letterSpacing = 0.16f
            setPadding(0, 0, 0, dp(28))
        }
        content.addView(title)

        // --- API CONNECTION ---
        val apiLabel = TextView(this).apply {
            text = "API CONNECTION"
            textSize = 11f
            setTextColor(darkMuted)
            typeface = android.graphics.Typeface.MONOSPACE
            letterSpacing = 0.12f
            setPadding(0, 0, 0, dp(10))
        }
        content.addView(apiLabel)

        providerInput = createInput("Provider", settings.getProvider())
        content.addView(providerInput)

        modelInput = createInput("Model", settings.getModel())
        content.addView(modelInput)

        endpointInput = createInput("API Endpoint", settings.getEndpoint())
        content.addView(endpointInput)

        keyInput = createInput("API Key", settings.getApiKey())
        keyInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        content.addView(keyInput)

        // Test API button
        val testApiRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val testApiBg = rounded(accentWhite, 14)
        val testApiContainer = FrameLayout(this).apply {
            background = testApiBg
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        val testApiText = TextView(this).apply {
            text = "Test Connection"
            textSize = 12f
            typeface = android.graphics.Typeface.MONOSPACE
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
        }
        val testApiDots = BouncingDotsView(this)
        testApiDots.visibility = View.GONE
        testApiContainer.addView(testApiText, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        testApiContainer.addView(testApiDots, FrameLayout.LayoutParams(dp(60), dp(24)).apply { gravity = Gravity.CENTER })
        testApiContainer.setOnClickListener {
            val endpoint = endpointInput.text.toString().trim()
            val model = modelInput.text.toString().trim()
            val apiKey = keyInput.text.toString().trim()
            if (endpoint.isBlank() || model.isBlank() || apiKey.isBlank()) {
                Toast.makeText(this, "Complete all API fields first.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            testApiText.text = ""
            testApiDots.visibility = View.VISIBLE
            testApiDots.start()
            testApiContainer.isClickable = false
            Thread {
                val result = AIClient().testConnection(endpoint, model, apiKey)
                runOnUiThread {
                    testApiDots.stop()
                    testApiDots.visibility = View.GONE
                    result.onSuccess {
                        testApiText.text = "Connected"
                        testApiText.setTextColor(Color.rgb(76, 175, 80))
                    }.onFailure {
                        testApiText.text = "Failed"
                        testApiText.setTextColor(Color.rgb(244, 67, 54))
                    }
                    testApiContainer.postDelayed({
                        testApiText.text = "Test Connection"
                        testApiText.setTextColor(Color.BLACK)
                        testApiContainer.isClickable = true
                    }, 1500)
                }
            }.start()
        }
        testApiRow.addView(testApiContainer, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        content.addView(testApiRow, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(4) })

        // --- BROWSERLESS ---
        val browserlessLabel = TextView(this).apply {
            text = "BROWSERLESS"
            textSize = 11f
            setTextColor(darkMuted)
            typeface = android.graphics.Typeface.MONOSPACE
            letterSpacing = 0.12f
            setPadding(0, dp(28), 0, dp(10))
        }
        content.addView(browserlessLabel)

        browserlessKeyInput = createInput("Browserless API Key", settings.getBrowserlessApiKey())
        browserlessKeyInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        content.addView(browserlessKeyInput)

        // Test Browserless button
        val testBrowserlessRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val testBrowserlessContainer = FrameLayout(this).apply {
            background = rounded(accentWhite, 14)
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        val testBrowserlessText = TextView(this).apply {
            text = "Test Connection"
            textSize = 12f
            typeface = android.graphics.Typeface.MONOSPACE
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
        }
        val testBrowserlessDots = BouncingDotsView(this)
        testBrowserlessDots.visibility = View.GONE
        testBrowserlessContainer.addView(testBrowserlessText, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        testBrowserlessContainer.addView(testBrowserlessDots, FrameLayout.LayoutParams(dp(60), dp(24)).apply { gravity = Gravity.CENTER })
        testBrowserlessContainer.setOnClickListener {
            val token = browserlessKeyInput.text.toString().trim()
            if (token.isBlank()) {
                Toast.makeText(this, "Enter the Browserless API key.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            testBrowserlessText.text = ""
            testBrowserlessDots.visibility = View.VISIBLE
            testBrowserlessDots.start()
            testBrowserlessContainer.isClickable = false
            Thread {
                try {
                    val url = java.net.URL("https://production-sfo.browserless.io/content?token=" + java.net.URLEncoder.encode(token, "UTF-8"))
                    val connection = url.openConnection() as java.net.HttpURLConnection
                    connection.requestMethod = "POST"
                    connection.connectTimeout = 15000
                    connection.readTimeout = 15000
                    connection.doOutput = true
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.outputStream.use { it.write("""{"url":"https://example.com","waitForTimeout":1000}""".toByteArray()) }
                    val code = connection.responseCode
                    connection.disconnect()
                    runOnUiThread {
                        testBrowserlessDots.stop()
                        testBrowserlessDots.visibility = View.GONE
                        if (code in 200..299) {
                            testBrowserlessText.text = "Connected"
                            testBrowserlessText.setTextColor(Color.rgb(76, 175, 80))
                        } else {
                            testBrowserlessText.text = "Failed"
                            testBrowserlessText.setTextColor(Color.rgb(244, 67, 54))
                        }
                        testBrowserlessContainer.postDelayed({
                            testBrowserlessText.text = "Test Connection"
                            testBrowserlessText.setTextColor(Color.BLACK)
                            testBrowserlessContainer.isClickable = true
                        }, 1500)
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        testBrowserlessDots.stop()
                        testBrowserlessDots.visibility = View.GONE
                        testBrowserlessText.text = "Failed"
                        testBrowserlessText.setTextColor(Color.rgb(244, 67, 54))
                        testBrowserlessContainer.postDelayed({
                            testBrowserlessText.text = "Test Connection"
                            testBrowserlessText.setTextColor(Color.BLACK)
                            testBrowserlessContainer.isClickable = true
                        }, 1500)
                    }
                }
            }.start()
        }
        testBrowserlessRow.addView(testBrowserlessContainer, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        content.addView(testBrowserlessRow, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(4) })

        // --- BOTTOM ROW: Save (left) + Back (right) ---
        val bottomRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(32), 0, 0)
        }

        val saveBtnBg = rounded(darkSurface, 14)
        var saveBtn: TextView? = null
        saveBtn = TextView(this).apply {
            text = "Save"
            textSize = 13f
            typeface = android.graphics.Typeface.MONOSPACE
            setTextColor(darkText)
            background = saveBtnBg
            setPadding(dp(20), dp(14), dp(20), dp(14))
            gravity = Gravity.CENTER
            setOnClickListener {
                settings.saveProvider(providerInput.text.toString().trim())
                settings.saveModel(modelInput.text.toString().trim())
                settings.saveEndpoint(endpointInput.text.toString().trim())
                settings.saveApiKey(keyInput.text.toString().trim())
                settings.saveBrowserlessApiKey(browserlessKeyInput.text.toString().trim())
                Toast.makeText(this@SettingsActivity, "Settings saved", Toast.LENGTH_SHORT).show()
                saveBtn?.text = "Saved"
                saveBtn?.setTextColor(Color.rgb(76, 175, 80))
                saveBtn?.postDelayed({
                    saveBtn?.text = "Save"
                    saveBtn?.setTextColor(darkText)
                }, 1500)
            }
        }
        bottomRow.addView(saveBtn)

        // Spacer
        bottomRow.addView(Space(this), LinearLayout.LayoutParams(0, 1, 1f))

        val backBtnBg = rounded(Color.TRANSPARENT, 14)
        val backBtn = TextView(this).apply {
            text = "Back"
            textSize = 13f
            typeface = android.graphics.Typeface.MONOSPACE
            setTextColor(darkMuted)
            background = backBtnBg
            setPadding(dp(20), dp(14), dp(20), dp(14))
            gravity = Gravity.CENTER
            setOnClickListener { finish() }
        }
        bottomRow.addView(backBtn)

        content.addView(bottomRow)

        root.addView(content)
        setContentView(root)
    }

    private fun createInput(hint: String, value: String): EditText {
        return EditText(this).apply {
            this.hint = hint
            setText(value)
            textSize = 14f
            setTextColor(darkText)
            setHintTextColor(darkMuted)
            setSingleLine(true)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = rounded(darkInput, 14)
            typeface = android.graphics.Typeface.MONOSPACE
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(10)
            }
        }
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }
}
