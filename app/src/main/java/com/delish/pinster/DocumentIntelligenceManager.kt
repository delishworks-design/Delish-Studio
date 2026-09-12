package com.delish.pinster

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

object DocumentIntelligenceManager {

    data class UnderstandingResult(
        val profile: DocumentProfile,
        val fingerprint: String,
        val fromCache: Boolean
    )

    suspend fun understand(
        context: Context,
        uri: Uri,
        fileName: String,
        extractedText: String
    ): UnderstandingResult = withContext(Dispatchers.IO) {

        val fingerprint = DocumentFingerprint.calculate(context, uri)
        if (fingerprint.isBlank()) {
            throw Exception("Could not compute document fingerprint.")
        }

        val cached = DocumentCache.getProfile(context, fingerprint)
        if (cached != null) {
            return@withContext UnderstandingResult(
                profile = cached,
                fingerprint = fingerprint,
                fromCache = true
            )
        }

        val sec = SecureSettings(context)
        val endpoint = sec.getEndpoint()
        val model = sec.getDocumentPrimaryModel()
        val apiKey = sec.getApiKey()

        val ext = fileName.substringAfterLast('.', "").lowercase()
        val charCount = extractedText.length
        val metadata = buildString {
            append("fileName: $fileName\n")
            append("fileType: $ext\n")
            append("characterCount: $charCount\n")
            append("fingerprint: ${fingerprint.take(16)}...")
        }

        val textToSend = if (extractedText.length > 80000) {
            extractedText.take(40000) + "\n\n[...middle section omitted...]\n\n" +
                extractedText.takeLast(40000)
        } else {
            extractedText
        }

        val client = AIClient()
        val result = client.generateDocumentUnderstanding(
            endpoint = endpoint,
            model = model,
            apiKey = apiKey,
            documentText = textToSend,
            metadata = metadata
        )

        val jsonStr = result.getOrThrow()
        val json = JSONObject(jsonStr)
        val profile = DocumentProfile.fromJson(json)

        val meta = DocumentCache.CacheMetadata(
            fingerprint = fingerprint,
            fileName = fileName,
            fileType = ext,
            characterCount = charCount,
            createdAt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).format(java.util.Date()),
            primaryModel = model,
            profileVersion = 1
        )

        DocumentCache.saveProfile(context, fingerprint, profile, meta)
        DocumentCache.saveFullText(context, fingerprint, extractedText)

        for (section in profile.sections) {
            val sectionText = extractSectionText(extractedText, section)
            if (sectionText.isNotBlank()) {
                DocumentCache.saveExcerpt(context, fingerprint, section.id, sectionText)
            }
        }

        UnderstandingResult(
            profile = profile,
            fingerprint = fingerprint,
            fromCache = false
        )
    }

    suspend fun analyze(
        context: Context,
        fingerprint: String,
        question: String,
        profile: DocumentProfile
    ): String = withContext(Dispatchers.IO) {

        val sec = SecureSettings(context)
        val endpoint = sec.getEndpoint()
        val model = sec.getDocumentSecondaryModel()
        val apiKey = sec.getApiKey()

        val taskType = classifyTask(question)
        val maxTokens = when (taskType) {
            TaskType.SIMPLE_QA -> 1200
            TaskType.NORMAL_ANALYSIS -> 2000
            TaskType.DEEP_ANALYSIS -> 3000
            TaskType.TEXT_REWRITE -> 1500
            TaskType.TEXT_SUMMARY -> 2000
            TaskType.TEXT_EXTRACTION -> 1500
        }

        val fullText = DocumentCache.getFullText(context, fingerprint)
        val contextResult = DocumentContextBuilder.build(profile, question, fullText)

        val client = AIClient()
        val result = client.generateDocumentAnalysis(
            endpoint = endpoint,
            model = model,
            apiKey = apiKey,
            context = contextResult.fullContext,
            question = question,
            maxTokens = maxTokens
        )

        result.getOrThrow()
    }

    suspend fun editText(
        context: Context,
        selectedText: String,
        instruction: String,
        profile: DocumentProfile? = null
    ): String = withContext(Dispatchers.IO) {

        val sec = SecureSettings(context)
        val endpoint = sec.getEndpoint()
        val model = sec.getDocumentSecondaryModel()
        val apiKey = sec.getApiKey()

        val contextHint = if (profile != null) {
            DocumentContextBuilder.buildForEdit(profile, selectedText, instruction)
        } else {
            instruction
        }

        val client = AIClient()
        val result = client.generateTextEdit(
            endpoint = endpoint,
            model = model,
            apiKey = apiKey,
            selectedText = selectedText,
            instruction = instruction,
            contextHint = if (profile != null) "" else "",
            maxTokens = 1500
        )

        result.getOrThrow()
    }

    fun classifyTask(question: String): TaskType {
        val q = question.lowercase().trim()

        if (q.length < 20 && (q.startsWith("what") || q.startsWith("who") || q.startsWith("when") || q.startsWith("where") || q.startsWith("is ") || q.startsWith("are ") || q.startsWith("does "))) {
            return TaskType.SIMPLE_QA
        }

        val rewriteKeywords = listOf("rewrite", "shorten", "expand", "fix grammar", "professionalize", "paraphrased", "rephrase", "simplify")
        if (rewriteKeywords.any { q.contains(it) }) {
            return TaskType.TEXT_REWRITE
        }

        val summaryKeywords = listOf("summarize", "summary", "brief overview", "tldr", "tl;dr")
        if (summaryKeywords.any { q.contains(it) }) {
            return TaskType.TEXT_SUMMARY
        }

        val extractKeywords = listOf("extract", "list all", "find all", "identify all", "pull out")
        if (extractKeywords.any { q.contains(it) }) {
            return TaskType.TEXT_EXTRACTION
        }

        val deepKeywords = listOf("compare", "contrast", "analyze", "analyse", "evaluate", "critique", "argument", "contradiction", "strength", "weakness", "implication", "in-depth", "comprehensive", "thorough")
        if (deepKeywords.any { q.contains(it) }) {
            return TaskType.DEEP_ANALYSIS
        }

        return TaskType.NORMAL_ANALYSIS
    }

    enum class TaskType {
        SIMPLE_QA,
        NORMAL_ANALYSIS,
        DEEP_ANALYSIS,
        TEXT_REWRITE,
        TEXT_SUMMARY,
        TEXT_EXTRACTION
    }

    private fun extractSectionText(fullText: String, section: DocumentProfile.Section): String {
        if (section.startLocation.isNotBlank()) {
            val idx = fullText.indexOf(section.startLocation, ignoreCase = true)
            if (idx != -1) {
                val endIdx = if (section.endLocation.isNotBlank()) {
                    val e = fullText.indexOf(section.endLocation, ignoreCase = true, startIndex = idx + section.startLocation.length)
                    if (e != -1) e + section.endLocation.length else idx + 2000
                } else {
                    idx + 2000
                }
                return fullText.substring(idx, minOf(endIdx, fullText.length))
            }
        }

        if (section.title.isNotBlank()) {
            val idx = fullText.indexOf(section.title, ignoreCase = true)
            if (idx != -1) {
                val endIdx = minOf(fullText.length, idx + 2000)
                return fullText.substring(idx, endIdx)
            }
        }

        return ""
    }
}
