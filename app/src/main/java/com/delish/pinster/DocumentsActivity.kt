package com.delish.pinster

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var charCountText: TextView
    private lateinit var openBtn: Button

    private lateinit var contentScroll: ScrollView
    private lateinit var contentText: TextView
    private lateinit var contentEdit: EditText
    private lateinit var contentCard: LinearLayout

    private lateinit var copyBtn: Button
    private lateinit var clearBtn: Button
    private lateinit var editBtn: Button
    private lateinit var saveBtn: Button
    private lateinit var exportPdfBtn: Button

    private lateinit var aiRow: LinearLayout
    private lateinit var rewriteBtn: Button
    private lateinit var shortenBtn: Button
    private lateinit var expandBtn: Button
    private lateinit var fixGrammarBtn: Button

    private var currentUri: Uri? = null
    private var currentFileName: String? = null
    private var extractedText: String? = null
    private var editableContent: String? = null
    private var documentProfile: DocumentProfile? = null
    private var documentFingerprint: String? = null
    private var isEditing = false

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

        fileNameText = TextView(this).apply {
            textSize = 13f
            setTextColor(muted())
            setPadding(0, dp(4), 0, dp(8))
            visibility = View.GONE
        }
        root.addView(fileNameText)

        statusText = TextView(this).apply {
            textSize = 13f
            setTextColor(muted())
            setPadding(0, dp(4), 0, dp(8))
            visibility = View.GONE
        }
        root.addView(statusText)

        progressBar = ProgressBar(this).apply {
            visibility = View.GONE
            indeterminateTintList = android.content.res.ColorStateList.valueOf(accentBlue)
        }
        root.addView(progressBar, LinearLayout.LayoutParams(dp(36), dp(36)).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            topMargin = dp(10); bottomMargin = dp(10)
        })

        charCountText = TextView(this).apply {
            textSize = 12f
            setTextColor(muted())
            setPadding(0, dp(4), 0, dp(8))
            visibility = View.GONE
        }
        root.addView(charCountText)

        contentCard = LinearLayout(this).apply {
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

        val buttonRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, 0)
        }

        copyBtn = Button(this).apply {
            text = "COPY"
            setTextColor(Color.WHITE)
            textSize = 11f
            typeface = Typeface.MONOSPACE
            setBackgroundColor(accentBlue)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            visibility = View.GONE
            setOnClickListener { copyTextToClipboard() }
        }
        buttonRow.addView(copyBtn, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(4) })

        editBtn = Button(this).apply {
            text = "EDIT"
            setTextColor(Color.WHITE)
            textSize = 11f
            typeface = Typeface.MONOSPACE
            setBackgroundColor(Color.rgb(60, 140, 80))
            setPadding(dp(12), dp(10), dp(12), dp(10))
            visibility = View.GONE
            setOnClickListener { toggleEditMode() }
        }
        buttonRow.addView(editBtn, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(4); marginEnd = dp(4) })

        exportPdfBtn = Button(this).apply {
            text = "PDF"
            setTextColor(Color.WHITE)
            textSize = 11f
            typeface = Typeface.MONOSPACE
            setBackgroundColor(Color.rgb(180, 60, 60))
            setPadding(dp(12), dp(10), dp(12), dp(10))
            visibility = View.GONE
            setOnClickListener { exportPdf() }
        }
        buttonRow.addView(exportPdfBtn, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(4) })

        clearBtn = Button(this).apply {
            text = "CLEAR"
            setTextColor(muted())
            textSize = 11f
            typeface = Typeface.MONOSPACE
            setBackgroundColor(cardBg())
            setPadding(dp(12), dp(10), dp(12), dp(10))
            visibility = View.GONE
            setOnClickListener { clearAll() }
        }
        buttonRow.addView(clearBtn, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(4) })

        root.addView(buttonRow)

        aiRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, 0)
            visibility = View.GONE
        }

        fun aiBtn(label: String, color: Int, action: () -> Unit): Button {
            return Button(this@DocumentsActivity).apply {
                text = label
                setTextColor(Color.WHITE)
                textSize = 10f
                typeface = Typeface.MONOSPACE
                setBackgroundColor(color)
                setPadding(dp(8), dp(8), dp(8), dp(8))
                setOnClickListener { action() }
            }
        }

        rewriteBtn = aiBtn("Rewrite", Color.rgb(70, 130, 200)) { aiEditSelected("Rewrite this text professionally while preserving meaning.") }
        aiRow.addView(rewriteBtn, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(3) })

        shortenBtn = aiBtn("Shorten", Color.rgb(160, 100, 50)) { aiEditSelected("Shorten this text while preserving key information.") }
        aiRow.addView(shortenBtn, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(3); marginEnd = dp(3) })

        expandBtn = aiBtn("Expand", Color.rgb(50, 140, 100)) { aiEditSelected("Expand this text with more detail and context.") }
        aiRow.addView(expandBtn, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(3); marginEnd = dp(3) })

        fixGrammarBtn = aiBtn("Fix", Color.rgb(150, 60, 150)) { aiEditSelected("Fix grammar and spelling errors in this text.") }
        aiRow.addView(fixGrammarBtn, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(3) })

        root.addView(aiRow)

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
            var name = uri.lastPathSegment?.substringAfterLast('/') ?: "document"
            if (!name.contains('.')) {
                val mime = contentResolver.getType(uri) ?: ""
                val ext = when {
                    mime.contains("pdf") -> ".pdf"
                    mime.contains("wordprocessingml") || mime.equals("application/msword", true) -> ".docx"
                    mime.contains("spreadsheet") || mime.contains("excel") -> ".xlsx"
                    mime.contains("presentation") || mime.equals("application/vnd.ms-powerpoint", true) -> ".pptx"
                    mime.equals("text/plain", true) -> ".txt"
                    else -> ""
                }
                name = name + ext
            }
            currentFileName = name
            fileNameText.text = "\uD83D\uDCC4 $name"
            fileNameText.visibility = View.VISIBLE
            extractDocument(uri, name)
        }
    }

    private fun extractDocument(uri: Uri, name: String) {
        statusText.text = "Reading document..."
        statusText.visibility = View.VISIBLE
        contentCard.visibility = View.GONE
        copyBtn.visibility = View.GONE
        editBtn.visibility = View.GONE
        exportPdfBtn.visibility = View.GONE
        clearBtn.visibility = View.GONE
        aiRow.visibility = View.GONE
        charCountText.visibility = View.GONE
        progressBar.visibility = View.VISIBLE
        extractedText = null
        editableContent = null
        documentProfile = null
        documentFingerprint = null
        isEditing = false

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
                contentCard.visibility = View.GONE
                copyBtn.visibility = View.GONE
                editBtn.visibility = View.GONE
                exportPdfBtn.visibility = View.GONE
                clearBtn.visibility = View.VISIBLE
            } else {
                extractedText = result
                editableContent = result
                statusText.visibility = View.GONE
                contentText.text = result
                contentCard.visibility = View.VISIBLE
                charCountText.text = "${result.length} characters"
                charCountText.visibility = View.VISIBLE
                copyBtn.visibility = View.VISIBLE
                editBtn.visibility = View.VISIBLE
                exportPdfBtn.visibility = View.VISIBLE
                clearBtn.visibility = View.VISIBLE

                statusText.text = "Understanding document..."
                statusText.setTextColor(muted())
                statusText.visibility = View.VISIBLE
                progressBar.visibility = View.VISIBLE

                scope.launch {
                    try {
                        val r = DocumentIntelligenceManager.understand(
                            this@DocumentsActivity, uri, name, result
                        )
                        documentProfile = r.profile
                        documentFingerprint = r.fingerprint
                        val cacheInfo = if (r.fromCache) " (cached)" else ""
                        statusText.text = "Profile ready${cacheInfo}: ${r.profile.mainTopic}"
                        statusText.setTextColor(Color.rgb(80, 180, 80))
                    } catch (e: Exception) {
                        statusText.text = "Profile unavailable: ${e.message}"
                        statusText.setTextColor(Color.rgb(200, 150, 50))
                    }
                    progressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun toggleEditMode() {
        isEditing = !isEditing
        if (isEditing) {
            contentScroll.removeView(contentText)
            contentEdit = EditText(this).apply {
                setText(editableContent ?: extractedText ?: "")
                textSize = 13f
                setTextColor(text())
                typeface = Typeface.MONOSPACE
                setLineSpacing(0f, 1.3f)
                setBackgroundColor(Color.TRANSPARENT)
                setPadding(0, 0, 0, dp(40))
                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable?) {
                        editableContent = s?.toString()
                        charCountText.text = "${s?.length ?: 0} characters"
                    }
                })
            }
            contentScroll.addView(contentEdit)
            editBtn.text = "DONE"
            editBtn.setBackgroundColor(Color.rgb(180, 120, 40))
            aiRow.visibility = View.VISIBLE
        } else {
            val editedText = contentEdit.text.toString()
            editableContent = editedText
            contentScroll.removeView(contentEdit)
            contentText.text = editedText
            contentScroll.addView(contentText)
            editBtn.text = "EDIT"
            editBtn.setBackgroundColor(Color.rgb(60, 140, 80))
            aiRow.visibility = View.GONE
        }
    }

    private fun aiEditSelected(instruction: String) {
        if (!isEditing) return
        val selectedText = try {
            val start = contentEdit.selectionStart
            val end = contentEdit.selectionEnd
            if (start >= 0 && end > start && end <= contentEdit.text.length) {
                contentEdit.text.substring(start, end)
            } else {
                contentEdit.text.toString()
            }
        } catch (e: Exception) {
            contentEdit.text.toString()
        }

        if (selectedText.isBlank()) {
            Toast.makeText(this, "Select text to edit", Toast.LENGTH_SHORT).show()
            return
        }

        statusText.text = "AI editing..."
        statusText.setTextColor(muted())
        statusText.visibility = View.VISIBLE
        progressBar.visibility = View.VISIBLE

        scope.launch {
            try {
                val result = DocumentIntelligenceManager.editText(
                    this@DocumentsActivity, selectedText, instruction, documentProfile
                )
                val start = contentEdit.selectionStart
                val end = contentEdit.selectionEnd
                if (start >= 0 && end > start && end <= contentEdit.text.length) {
                    contentEdit.text.replace(start, end, result)
                } else {
                    contentEdit.setText(result)
                    editableContent = result
                }
                statusText.text = "Edit applied"
                statusText.setTextColor(Color.rgb(80, 180, 80))
            } catch (e: Exception) {
                statusText.text = "Edit failed: ${e.message}"
                statusText.setTextColor(Color.rgb(220, 80, 60))
            }
            progressBar.visibility = View.GONE
        }
    }

    private fun exportPdf() {
        val content = if (isEditing) contentEdit.text.toString() else (editableContent ?: extractedText ?: return)
        if (content.isBlank()) {
            Toast.makeText(this, "No content to export", Toast.LENGTH_SHORT).show()
            return
        }

        statusText.text = "Exporting PDF..."
        statusText.setTextColor(muted())
        statusText.visibility = View.VISIBLE
        progressBar.visibility = View.VISIBLE

        scope.launch {
            val result = withContext(Dispatchers.IO) {
                PdfExporter.export(
                    this@DocumentsActivity,
                    content,
                    currentFileName?.substringBeforeLast('.') ?: "document",
                    currentFileName ?: "Document"
                )
            }
            progressBar.visibility = View.GONE
            if (result.success) {
                statusText.text = "PDF saved: ${result.filePath?.substringAfterLast('/')}"
                statusText.setTextColor(Color.rgb(80, 180, 80))
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, androidx.core.content.FileProvider.getUriForFile(
                        this@DocumentsActivity,
                        "${packageName}.fileprovider",
                        java.io.File(result.filePath!!)
                    ))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(shareIntent, "Share PDF"))
            } else {
                statusText.text = "PDF export failed: ${result.error}"
                statusText.setTextColor(Color.rgb(220, 80, 60))
            }
        }
    }

    private fun copyTextToClipboard() {
        val text = if (isEditing) contentEdit.text.toString() else (editableContent ?: extractedText ?: return)
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Document text", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Text copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun clearAll() {
        currentUri = null
        currentFileName = null
        extractedText = null
        editableContent = null
        documentProfile = null
        documentFingerprint = null
        isEditing = false
        fileNameText.visibility = View.GONE
        fileNameText.text = ""
        statusText.visibility = View.GONE
        statusText.text = ""
        statusText.setTextColor(muted())
        contentCard.visibility = View.GONE
        contentText.text = ""
        charCountText.visibility = View.GONE
        charCountText.text = ""
        copyBtn.visibility = View.GONE
        editBtn.visibility = View.GONE
        editBtn.text = "EDIT"
        editBtn.setBackgroundColor(Color.rgb(60, 140, 80))
        exportPdfBtn.visibility = View.GONE
        clearBtn.visibility = View.GONE
        aiRow.visibility = View.GONE
        progressBar.visibility = View.GONE
        if (::contentEdit.isInitialized) contentEdit.text.clear()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
