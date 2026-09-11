package com.delish.pinster

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*

class ProjectsActivity : Activity() {

    private val darkBg = Color.rgb(18, 18, 18)
    private val darkText = Color.rgb(245, 245, 243)
    private val darkMuted = Color.rgb(130, 127, 123)
    private var darkMode = false

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
    private fun bg(): Int = if (darkMode) darkBg else Color.WHITE
    private fun text(): Int = if (darkMode) darkText else Color.rgb(28, 28, 28)
    private fun muted(): Int = if (darkMode) darkMuted else Color.rgb(100, 97, 93)

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
            text = "PROJECTS"
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

        // Empty state
        root.addView(TextView(this).apply {
            text = "No projects yet"
            textSize = 14f
            setTextColor(muted())
            setPadding(0, dp(40), 0, 0)
            gravity = Gravity.CENTER_HORIZONTAL
        })

        setContentView(root)
    }
}
