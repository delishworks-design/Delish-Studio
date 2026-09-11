package com.delish.pinster

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.DecelerateInterpolator
import android.view.animation.ScaleAnimation
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

class SplashActivity : Activity() {

    private val handler = Handler(Looper.getMainLooper())
    private val fullText = "DELISH STUDIO"
    private var charIndex = 0
    private var typingTextView: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.rgb(18, 18, 18))
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }

        // Logo
        val logo = ImageView(this).apply {
            setImageResource(R.drawable.splash_logo)
            scaleType = ImageView.ScaleType.FIT_CENTER
            val logoSize = (resources.displayMetrics.widthPixels * 0.42f).toInt()
            layoutParams = LinearLayout.LayoutParams(logoSize, logoSize).apply {
                gravity = Gravity.CENTER
            }
        }

        container.addView(logo)

        // Space between logo and text
        container.addView(android.widget.Space(this), LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, (resources.displayMetrics.density * 28).toInt()
        ))

        // Typewriter text
        typingTextView = TextView(this).apply {
            text = ""
            textSize = 16f
            setTextColor(Color.rgb(155, 155, 150))
            typeface = Typeface.MONOSPACE
            letterSpacing = 0.18f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
            }
        }

        container.addView(typingTextView)

        root.addView(container, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ).apply {
            gravity = Gravity.CENTER
        })

        setContentView(root)

        // Start fade in animation for logo
        val fadeIn = AlphaAnimation(0f, 1f).apply {
            duration = 600
            fillAfter = true
        }
        val scaleUp = ScaleAnimation(
            0.85f, 1f, 0.85f, 1f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 600
            fillAfter = true
        }
        val animSet = AnimationSet(true).apply {
            addAnimation(fadeIn)
            addAnimation(scaleUp)
            interpolator = DecelerateInterpolator()
        }
        logo.startAnimation(animSet)

        // Start typing after delay
        handler.postDelayed({ startTyping() }, 700)

        // Navigate to MainActivity after delay
        handler.postDelayed({
            startActivity(android.content.Intent(this, MainActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, 3200)
    }

    private fun startTyping() {
        if (charIndex <= fullText.length) {
            typingTextView?.text = fullText.substring(0, charIndex) + "_"
            charIndex++
            handler.postDelayed({ startTyping() }, 90)
        } else {
            // Blinking cursor after typing done
            blinkingCursor()
        }
    }

    private fun blinkingCursor() {
        val blink = object : Runnable {
            var showCursor = true
            override fun run() {
                if (showCursor) {
                    typingTextView?.text = fullText + "_"
                } else {
                    typingTextView?.text = fullText
                }
                showCursor = !showCursor
                handler.postDelayed(this, 530)
            }
        }
        handler.postDelayed(blink, 200)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
