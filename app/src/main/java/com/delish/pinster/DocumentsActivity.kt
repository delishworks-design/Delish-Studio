package com.delish.pinster

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DocumentsActivity : Activity() {

    private val darkBg = Color.rgb(18, 18, 18)
    private val darkText = Color.rgb(245, 245, 243)
    private val darkMuted = Color.rgb(130, 127, 123)
    private val darkCard = Color.rgb(28, 28, 28)
    private val lightCard = Color.rgb(245, 245, 243)
    private val accentBlue = Color.rgb(100, 160, 255)

    private var darkMode = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private lateinit var fileNameText: TextView
    private lateinit var contentScroll: ScrollView
    private lateinit var contentText: TextView
    private lateinit var charCountText: TextView
    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var openBtn: Button
    private lateinit var copyBtn: Button
    private lateinit var clearBtn: Button

    private var currentUri: Uri? = null
    private var currentFileName: String? = null
    private var extractedText: String? = null

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
    private fun bg(): Int = if (darkMode) darkBg else Color.WHITE
    private fun text(): Int = if (darkMode) darkText else Color.rgb(28, 28, 28)
    private fun muted(): Int = if (darkMode) darkMuted else Color.rgb(100, 97, 93)
    private fun cardBg(): Int = if (darkMode) darkCard else lightCard
    private fun sepColor(): Int = if (darkMode) Color.rgb(55, 55, 55) else Color.rgb(225, 223, 220)

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
        header.addView(TextView(this).apply {
            text = "\u2190"
            textSize = 20f
            setTextColor(text())
            setPadding(0, dp(4), dp(16), dp(4))
            setOnClickListener { finish() }
        })
        header.addView(TextView(this).apply {
            text = "DOCUMENTS"
            textSize = 16f
            setTextColor(text())
            typeface = Typeface.MONOSPACE
            letterSpacing = 0.12f
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        root.addView(header, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        root.addView(View(this).apply {
            setBackgroundColor(sepColor())
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)).apply {
            topMargin = dp(12); bottomMargin = dp(12)
        })

        // Open Document button
        openBtn = Button(this).apply {
            text = "OPEN DOCUMENT"
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = Typeface.MONOSPACE
            letterSpacing = 0.08f
            setBackgroundColor(accentBlue)
            setPadding(dp(20), dp(14), dp(20), dp(14))
            val btnParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            btnParams.bottomMargin = dp(12)
            layoutParams = btnParams
            setOnClickListener { openDocumentPicker() }
        }
        root.addView(openBtn)

        // File name
        fileNameText = TextView(this).apply {
            textSize = 13f
            setTextColor(muted())
            setPadding(0, dp(4), 0, dp(12))
            visibility = View.GONE
        }
        root.addView(fileNameText)

        // Status / Error
        statusText = TextView(this).apply {
            textSize = 13f
            setTextColor(muted())
            setPadding(0, dp(4), 0, dp(8))
            visibility = View.GONE
        }
        root.addView(statusText)

        // Progress bar
        progressBar = ProgressBar(this).apply {
            visibility = View.GONE
            indeterminateTintList = android.content.res.ColorStateList.valueOf(accentBlue)
        }
        root.addView(progressBar, LinearLayout.LayoutParams(dp(36), dp(36)).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            topMargin = dp(20); bottomMargin = dp(20)
        })

        // Character count
        charCountText = TextView(this).apply {
            textSize = 12f
            setTextColor(muted())
            setPadding(0, dp(4), 0, dp(8))
            visibility = View.GONE
        }
        root.addView(charCountText)

        // Content card
        val contentCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(cardBg())
            setPadding(dp(16), dp(12), dp(16), dp(12))
            visibility = View.GONE
        }
        contentScroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        contentText = TextView(this).apply {
            textSize = 13f
            setTextColor(text())
            typeface = Typeface.MONOSPACE
            setLineSpacing(0f, 1.3f)
            setTextIsSelectable(true)
        }
        contentScroll.addView(contentText)
        contentCard.addView(contentScroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(contentCard, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        // Button row
        val buttonRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, 0)
        }

        copyBtn = Button(this).apply {
            text = "COPY TEXT"
            setTextColor(Color.WHITE)
            textSize = 12f
            typeface = Typeface.MONOSPACE
            letterSpacing = 0.06f
            setBackgroundColor(accentBlue)
            setPadding(dp(20), dp(12), dp(20), dp(12))
            visibility = View.GONE
            setOnClickListener { copyTextToClipboard() }
        }
        buttonRow.addView(copyBtn, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginEnd = dp(8)
        })

        clearBtn = Button(this).apply {
            text = "CLEAR"
            setTextColor(muted())
            textSize = 12f
            typeface = Typeface.MONOSPACE
            letterSpacing = 0.06f
            setBackgroundColor(cardBg())
            setPadding(dp(20), dp(12), dp(20), dp(12))
            visibility = View.GONE
            setOnClickListener { clearAll() }
        }
        buttonRow.addView(clearBtn, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginStart = dp(8)
        })

        root.addView(buttonRow)

        setContentView(root)
    }

    private fun openDocumentPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        @Suppress("DEPRECATION")
        startActivityForResult(intent, 9001)
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 9001 && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            currentUri = uri
            val name = uri.lastPathSegment?.substringAfterLast('/') ?: "document"
            currentFileName = name
            fileNameText.text = "\uD83D\uDCC4 $name"
            fileNameText.visibility = View.VISIBLE
            extractDocument(uri, name)
        }
    }

    private fun extractDocument(uri: Uri, name: String) {
        // Show loading
        statusText.text = "Reading document..."
        statusText.visibility = View.VISIBLE
        contentCard().visibility = View.GONE
        copyBtn.visibility = View.GONE
        clearBtn.visibility = View.GONE
        charCountText.visibility = View.GONE
        progressBar.visibility = View.VISIBLE
        extractedText = null

        scope.launch {
            val result = try {
                withContext(Dispatchers.IO) {
                    DocumentReader.read(this@DocumentsActivity, uri, name)
                }
            } catch (e: Exception) {
                "Error reading document: ${e.message}"
            }

            progressBar.visibility = View.GONE

            if (result.startsWith("[") && result.endsWith("]") || result.startsWith("Error") || result.contains("failed")) {
                statusText.text = result
                statusText.setTextColor(Color.rgb(220, 80, 60))
                statusText.visibility = View.VISIBLE
                contentCard().visibility = View.GONE
                copyBtn.visibility = View.GONE
                clearBtn.visibility = View.VISIBLE
            } else {
                extractedText = result
                statusText.visibility = View.GONE
                contentText.text = result
                contentCard().visibility = View.VISIBLE
                charCountText.text = "${result.length} characters"
                charCountText.visibility = View.VISIBLE
                copyBtn.visibility = View.VISIBLE
                clearBtn.visibility = View.VISIBLE
            }
        }
    }

    private fun contentCard(): LinearLayout {
        return (contentScroll.parent as? LinearLayout) ?: contentScroll.parent?.parent as? LinearLayout ?: contentScroll.parent as? LinearLayout ?: run {
            // Fallback: find by traversing
            var p: View = contentScroll
            while (p.parent is View) p = p.parent as View
            p as? LinearLayout ?: contentScroll.parent as LinearLayout
        }
    }

    private fun copyTextToClipboard() {
        val text = extractedText ?: return
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Document text", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Text copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun clearAll() {
        currentUri = null
        currentFileName = null
        extractedText = null
        fileNameText.visibility = View.GONE
        fileNameText.text = ""
        statusText.visibility = View.GONE
        statusText.text = ""
        statusText.setTextColor(muted())
        contentCard().visibility = View.GONE
        contentText.text = ""
        charCountText.visibility = View.GONE
        charCountText.text = ""
        copyBtn.visibility = View.GONE
        clearBtn.visibility = View.GONE
        progressBar.visibility = View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
