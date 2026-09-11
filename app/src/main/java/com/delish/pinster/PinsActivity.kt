package com.delish.pinster

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.graphics.drawable.GradientDrawable
import org.json.JSONArray

class PinsActivity : Activity() {

    private val darkBg = Color.rgb(18, 18, 18)
    private val darkText = Color.rgb(245, 245, 243)
    private val darkMuted = Color.rgb(130, 127, 123)
    private var darkMode = false

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
    private fun bg(): Int = if (darkMode) darkBg else Color.WHITE
    private fun text(): Int = if (darkMode) darkText else Color.rgb(28, 28, 28)
    private fun muted(): Int = if (darkMode) darkMuted else Color.rgb(100, 97, 93)

    private lateinit var pinsList: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        darkMode = getSharedPreferences("pinster", MODE_PRIVATE).getBoolean("dark_mode", false)
        window.statusBarColor = bg()
        window.navigationBarColor = bg()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg())
            setPadding(dp(20), dp(24), dp(20), dp(16))
        }

        // Header
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val backBtn = TextView(this).apply {
            text = "\u2190"
            textSize = 20f
            setTextColor(text())
            setPadding(0, dp(4), dp(16), dp(4))
            setOnClickListener { finish() }
        }
        header.addView(backBtn)
        header.addView(TextView(this).apply {
            text = "PINS"
            textSize = 16f
            setTextColor(text())
            typeface = Typeface.MONOSPACE
            letterSpacing = 0.12f
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        root.addView(header, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        val separator = View(this).apply {
            setBackgroundColor(if (darkMode) Color.rgb(55, 55, 55) else Color.rgb(225, 223, 220))
        }
        root.addView(separator, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)).apply {
            topMargin = dp(12); bottomMargin = dp(12)
        })

        // Scrollable pins list
        val scroll = ScrollView(this).apply {
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
        }
        pinsList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        scroll.addView(pinsList, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        setContentView(root)
        loadPins()
    }

    private fun loadPins() {
        pinsList.removeAllViews()
        val prefs = getSharedPreferences("pinster_history", MODE_PRIVATE)
        val array = JSONArray(prefs.getString("items", "[]") ?: "[]")

        if (array.length() == 0) {
            pinsList.addView(TextView(this).apply {
                text = "No pins yet"
                textSize = 14f
                setTextColor(muted())
                setPadding(0, dp(40), 0, 0)
                gravity = Gravity.CENTER_HORIZONTAL
            })
            return
        }

        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val pinTitle = item.optString("pinTitle", "")
            val productTitle = item.optString("productTitle", "").ifBlank { item.optString("title", "Untitled") }
            val displayName = pinTitle.ifBlank { productTitle }

            val row = TextView(this).apply {
                text = displayName
                textSize = 14f
                setTextColor(text())
                typeface = Typeface.MONOSPACE
                maxLines = 2
                ellipsize = android.text.TextUtils.TruncateAt.END
                setPadding(dp(4), dp(14), dp(4), dp(14))
                setOnClickListener {
                    // TODO: open pin detail
                }
            }
            pinsList.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

            if (i < array.length() - 1) {
                pinsList.addView(View(this).apply {
                    setBackgroundColor(if (darkMode) Color.rgb(35, 35, 35) else Color.rgb(240, 239, 237))
                }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)))
            }
        }
    }
}
