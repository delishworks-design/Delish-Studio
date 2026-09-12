package com.delish.pinster

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AngleSeoResult(
    val angleId: String,
    val title: String,
    val description: String,
    val altText: String
)

data class SeoResult(
    val title: String,
    val description: String,
    val altText: String,
    val affiliateLink: String,
    val productImages: List<String>,
    val productTitle: String = "",
    val productBrand: String = "",
    val angleSeo: List<AngleSeoResult> = emptyList()
)

class AIClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun generateSeo(
        endpoint: String,
        model: String,
        apiKey: String,
        productEvidence: String,
        affiliateLink: String,
        productImages: List<String>
    ): Result<SeoResult> {

        return try {

            val url = endpoint.trimEnd('/') + "/chat/completions"

            val systemPrompt = """
You are Pinster, a Pinterest SEO specialist.

The supplied PRODUCT EVIDENCE is your ONLY source of truth.

CRITICAL: The evidence is extracted from a product page. IGNORE any Amazon page chrome that may appear:
- Navigation menus, headers, footers
- "Customers also bought" / "Frequently bought together" / "Compare with similar items"
- Sponsored products or ads
- Sidebar widgets, ratings sections, Q&A sections
- Any text that is NOT about the specific product

Focus ONLY on these labeled sections in the evidence:
- "PRODUCT TITLE:" — the actual product name
- "BRAND:" — the manufacturer/brand
- "META DESCRIPTION:" — product description from metadata
- "STRUCTURED PRODUCT EVIDENCE:" — JSON-LD structured data (most reliable)
- "VISIBLE PAGE TEXT:" — extract ONLY product-specific facts from this section

NEVER invent:
- product specifications
- dimensions
- weight
- capacity
- materials
- certifications
- prices
- ratings
- reviews
- features
- accessories
- performance claims
- health or safety claims

If a fact is uncertain, omit it.

Preserve the exact product identity and brand.

Return ONLY valid JSON:

{
  "title": "...",
  "description": "...",
  "alt_text": "...",
  "angles": {
    "HERO_PRODUCT": { "title": "...", "description": "...", "alt_text": "..." },
    "LIFESTYLE": { "title": "...", "description": "...", "alt_text": "..." },
    "PROBLEM_SOLUTION": { "title": "...", "description": "...", "alt_text": "..." },
    "FEATURE_BENEFIT": { "title": "...", "description": "...", "alt_text": "..." },
    "PINTEREST_INSPIRATION": { "title": "...", "description": "...", "alt_text": "..." }
  }
}

The top-level title/description/alt_text are generic fallbacks.
The "angles" object contains 5 angle-specific versions, each with UNIQUE title, description, and alt_text:
- HERO_PRODUCT: Premium product showcase. Title emphasizes product quality/materials/design. Description focuses on visual appeal and premium positioning.
- LIFESTYLE: Real-life usage context. Title emphasizes everyday benefit. Description shows how product fits into daily life.
- PROBLEM_SOLUTION: Problem/solution story. Title emphasizes the problem solved. Description tells the story of before/after.
- FEATURE_BENEFIT: Feature highlight. Title emphasizes specific feature/benefit. Description deep-dives into that one standout feature.
- PINTEREST_INSPIRATION: Aspirational aesthetic. Title emphasizes lifestyle/aspiration. Description paints an aspirational picture.

Each angle must have a UNIQUE title (under 100 chars), description (700 chars max, 3 paragraphs with affiliate disclosure), and alt_text. No two angles should have the same title.

CRITICAL ANTI-REPETITION RULES:

You MUST write uniquely for EACH product. Two different products must NEVER receive similar SEO copy.

AVOID these overused phrases (they become repetitive across products):
- "Upgrade your..."
- "Transform your..."
- "Perfect for..."
- "Discover..."
- "Whether you're looking..."
- "Make your space..."
- "A must-have..."
- "Elevate your..."
- "Shop now..."
- "Check it out..."
- "Upgrade your space"
- "Transform your home"
- "Elevate your everyday"
- "A must-have addition"

Instead, write specifically about THIS product's actual features, materials, dimensions, use cases, and benefits as found in the evidence.

USE VARIED OPENING STRATEGIES:
- Start with the product name or brand
- Start with a specific material or feature
- Start with a specific use case
- Start with a dimension or specification
- Start with what makes it different from alternatives
- Start with who it is for and why

USE VARIED CTA STRATEGIES:
- Ask a question
- State a specific benefit
- Mention a specific场景/scenario
- Reference a specific feature
- Suggest a specific use
- Keep it short and factual

Rules:
- title: compelling Pinterest SEO title, under 100 characters. Must be unique to THIS product.
- description: maximum 700 characters total.
- description must contain exactly 3 paragraphs.
- Paragraph 1: factual product description based ONLY on evidence. Include specific details like materials, dimensions, features, use cases.
- Paragraph 2: natural CTA that is specific to this product type (not generic).
- Paragraph 3: short affiliate disclosure (1-2 sentences max).
- Separate paragraphs with blank lines.
- alt_text: concise factual description of the product, mentioning key visual attributes.
- Natural Pinterest language.
- No keyword stuffing.
- No markdown.
- No unsupported claims.
- Each product's SEO must feel individually written for that specific product.
""".trimIndent()

            val userPrompt = """
PRODUCT EVIDENCE:

$productEvidence

Generate accurate Pinterest SEO from this evidence only.
""".trimIndent()

            val messages = JSONArray()
                .put(
                    JSONObject()
                        .put("role", "system")
                        .put("content", systemPrompt)
                )
                .put(
                    JSONObject()
                        .put("role", "user")
                        .put("content", userPrompt)
                )

            val body = JSONObject()
                .put("model", model)
                .put("messages", messages)
                .put(
                    "response_format",
                    JSONObject().put("type", "json_object")
                )
                .put("max_tokens", 4000)
                .put(
                    "reasoning",
                    JSONObject().put("effort", "none")
                )
                .toString()

            android.util.Log.d("PINSTER_AI", "REQUEST: $url")
            android.util.Log.d("PINSTER_AI", "BODY: $body")

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .addHeader("HTTP-Referer", "https://pinster.app")
                .addHeader("X-Title", "Pinster")
                .post(
                    body.toRequestBody(
                        "application/json".toMediaType()
                    )
                )
                .build()

            client.newCall(request).execute().use { response ->

                val responseBody =
                    response.body?.string().orEmpty()

                android.util.Log.d("PINSTER_AI", "HTTP ${response.code}")
                android.util.Log.d("PINSTER_AI", "RESPONSE: ${responseBody.take(2000)}")

                if (!response.isSuccessful) {
                    return Result.failure(
                        Exception(
                            "HTTP ${response.code}: $responseBody"
                        )
                    )
                }

                val json = JSONObject(responseBody)

                val content =
                    json.optJSONArray("choices")
                        ?.optJSONObject(0)
                        ?.optJSONObject("message")
                        ?.optString("content")
                        ?.trim()
                        .orEmpty()

                android.util.Log.d("PINSTER_AI", "CONTENT: ${content.take(500)}")

                if (content.isBlank() || content == "null") {
                    return Result.failure(
                        Exception("AI returned an empty response.")
                    )
                }

                val cleanJson =
                    content
                        .removePrefix("```json")
                        .removePrefix("```")
                        .removeSuffix("```")
                        .trim()

                if (cleanJson.isBlank() || cleanJson == "null" || !cleanJson.startsWith("{")) {
                    return Result.failure(
                        Exception("AI returned an invalid response format.")
                    )
                }

                val resultJson = JSONObject(cleanJson)

                // Truncate description to 700 chars at last sentence boundary
                fun truncateDesc(raw: String): String {
                    val t = raw.trim()
                    if (t.length <= 700) return t
                    val cut = t.substring(0, 700)
                    val lastPeriod = cut.lastIndexOf(". ")
                    val lastNewline = cut.lastIndexOf("\n")
                    val boundary = maxOf(lastPeriod, lastNewline)
                    return if (boundary > 200) cut.substring(0, boundary + 1).trim() else cut.trim() + "..."
                }

                val title =
                    resultJson.optString("title").trim()

                val description =
                    truncateDesc(resultJson.optString("description"))

                val altText =
                    resultJson.optString("alt_text").trim()

                if (
                    title.isBlank() ||
                    description.isBlank() ||
                    altText.isBlank()
                ) {
                    return Result.failure(
                        Exception("AI returned incomplete SEO data.")
                    )
                }

                // Parse angle-specific SEO
                val angleSeoList = mutableListOf<AngleSeoResult>()
                val anglesJson = resultJson.optJSONObject("angles")
                if (anglesJson != null) {
                    for (angleId in listOf("HERO_PRODUCT", "LIFESTYLE", "PROBLEM_SOLUTION", "FEATURE_BENEFIT", "PINTEREST_INSPIRATION")) {
                        val angleData = anglesJson.optJSONObject(angleId)
                        if (angleData != null) {
                            angleSeoList.add(AngleSeoResult(
                                angleId = angleId,
                                title = angleData.optString("title", title).trim(),
                                description = truncateDesc(angleData.optString("description", description)),
                                altText = angleData.optString("alt_text", altText).trim()
                            ))
                        }
                    }
                }

                // Fallback: if angles missing/incomplete, use generic SEO for all 5 angles
                if (angleSeoList.size < 5) {
                    val angleIds = listOf("HERO_PRODUCT", "LIFESTYLE", "PROBLEM_SOLUTION", "FEATURE_BENEFIT", "PINTEREST_INSPIRATION")
                    val existing = angleSeoList.map { it.angleId }.toSet()
                    for (angleId in angleIds) {
                        if (angleId !in existing) {
                            angleSeoList.add(AngleSeoResult(
                                angleId = angleId,
                                title = title,
                                description = description,
                                altText = altText
                            ))
                        }
                    }
                }

                Result.success(
                    SeoResult(
                        title = title,
                        description = description,
                        altText = altText,
                        affiliateLink = affiliateLink,
                        productImages = productImages,
                        angleSeo = angleSeoList
                    )
                )
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Document Understanding (Primary Model) ──────────────────────

    fun generateDocumentUnderstanding(
        endpoint: String,
        model: String,
        apiKey: String,
        documentText: String,
        metadata: String
    ): Result<String> {

        return try {

            val url = endpoint.trimEnd('/') + "/chat/completions"

            val systemPrompt = """
You are a document intelligence system. Your ONLY job is to UNDERSTAND a document and produce a structured profile.

RULES:
- The document text is the ONLY source of truth.
- Do NOT invent facts, numbers, dates, citations, names, or claims.
- Do NOT fabricate information not present in the document.
- Identify uncertainty where it exists.
- Keep summaries compact but preserve important factual details.
- Identify sections where possible.
- Return ONLY valid JSON matching the schema below.

Return exactly this JSON structure:

{
  "documentSummary": "2-4 sentence summary of the entire document",
  "documentType": "type (e.g. report, article, contract, manual, presentation, spreadsheet, other)",
  "mainTopic": "the primary subject/topic",
  "sections": [
    {
      "id": "section_1",
      "title": "Section Title",
      "summary": "Compact summary of this section",
      "startLocation": "approximate location or heading reference",
      "endLocation": "approximate location or heading reference"
    }
  ],
  "keyPoints": ["important point 1", "important point 2"],
  "importantEntities": ["person/org/place 1", "entity 2"],
  "importantNumbers": ["$10,000 budget", "12 months", "3 phases"],
  "dates": ["January 2024", "Q3 2025"],
  "definitions": [{"term": "KPI", "definition": "Key Performance Indicator"}],
  "conclusions": ["conclusion 1", "conclusion 2"],
  "keywords": ["keyword1", "keyword2", "keyword3"],
  "warningsOrLimitations": ["limitation 1", "warning 1"]
}

Keep all fields compact. Limit sections to the most important ones (max 15).
Keep keyPoints, importantEntities, importantNumbers, dates, definitions, conclusions, keywords, warningsOrLimitations to the most significant items (max 15 each).
""".trimIndent()

            val userPrompt = """
DOCUMENT METADATA:
$metadata

DOCUMENT TEXT:
$documentText

Analyze this document and return the structured profile as JSON.
""".trimIndent()

            val messages = JSONArray()
                .put(JSONObject().put("role", "system").put("content", systemPrompt))
                .put(JSONObject().put("role", "user").put("content", userPrompt))

            val body = JSONObject()
                .put("model", model)
                .put("messages", messages)
                .put("response_format", JSONObject().put("type", "json_object"))
                .put("max_tokens", 2500)
                .put("temperature", 0.15)
                .put("reasoning", JSONObject().put("effort", "none"))
                .toString()

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .addHeader("HTTP-Referer", "https://pinster.app")
                .addHeader("X-Title", "Pinster")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()

                if (response.code == 402) {
                    return Result.failure(Exception("Not enough OpenRouter credits for document understanding. Try a shorter document, lower-cost model, or add credits."))
                }
                if (!response.isSuccessful) {
                    return Result.failure(Exception("HTTP ${response.code}: $responseBody"))
                }

                val json = JSONObject(responseBody)
                val content = json.optJSONArray("choices")
                    ?.optJSONObject(0)
                    ?.optJSONObject("message")
                    ?.optString("content")
                    ?.trim()
                    .orEmpty()

                if (content.isBlank() || content == "null") {
                    return Result.failure(Exception("AI returned an empty response."))
                }

                val cleanJson = content
                    .removePrefix("```json")
                    .removePrefix("```")
                    .removeSuffix("```")
                    .trim()

                if (cleanJson.isBlank() || !cleanJson.startsWith("{")) {
                    return Result.failure(Exception("AI returned invalid JSON."))
                }

                Result.success(cleanJson)
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Document Analysis (Secondary Model) ─────────────────────────

    fun generateDocumentAnalysis(
        endpoint: String,
        model: String,
        apiKey: String,
        context: String,
        question: String,
        maxTokens: Int = 2000
    ): Result<String> {

        return try {

            val url = endpoint.trimEnd('/') + "/chat/completions"

            val systemPrompt = """
You are a document analysis assistant. You answer questions based ONLY on the supplied document context.

RULES:
- Answer ONLY from the supplied document context.
- If the information cannot be verified from the supplied context, state that it could not be verified from the available document sections.
- Do NOT invent facts, numbers, dates, citations, names, or claims.
- Do NOT fabricate page numbers, citations, or references.
- Do NOT use knowledge outside the provided context.
- Be concise and direct.
- When quoting or referencing, note which section or area of the document it comes from.
""".trimIndent()

            val userPrompt = """
DOCUMENT CONTEXT:
$context

QUESTION:
$question

Answer based on the document context above.
""".trimIndent()

            val messages = JSONArray()
                .put(JSONObject().put("role", "system").put("content", systemPrompt))
                .put(JSONObject().put("role", "user").put("content", userPrompt))

            val body = JSONObject()
                .put("model", model)
                .put("messages", messages)
                .put("max_tokens", maxTokens)
                .put("temperature", 0.3)
                .put("reasoning", JSONObject().put("effort", "none"))
                .toString()

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .addHeader("HTTP-Referer", "https://pinster.app")
                .addHeader("X-Title", "Pinster")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()

                if (response.code == 402) {
                    return Result.failure(Exception("Not enough OpenRouter credits for this request. Try a shorter question or lower-cost model."))
                }
                if (!response.isSuccessful) {
                    return Result.failure(Exception("HTTP ${response.code}: $responseBody"))
                }

                val json = JSONObject(responseBody)
                val content = json.optJSONArray("choices")
                    ?.optJSONObject(0)
                    ?.optJSONObject("message")
                    ?.optString("content")
                    ?.trim()
                    .orEmpty()

                if (content.isBlank() || content == "null") {
                    return Result.failure(Exception("AI returned an empty response."))
                }

                Result.success(content)
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── AI Text Edit (Secondary Model) ─────────────────────────────

    fun generateTextEdit(
        endpoint: String,
        model: String,
        apiKey: String,
        selectedText: String,
        instruction: String,
        contextHint: String = "",
        maxTokens: Int = 1500
    ): Result<String> {

        return try {

            val url = endpoint.trimEnd('/') + "/chat/completions"

            val systemPrompt = """
You are a precise text editor. You follow the user's editing instruction exactly.

RULES:
- Apply ONLY the requested transformation.
- Preserve the original meaning and intent.
- Do NOT add information not present in the original text.
- Do NOT remove important factual content unless the instruction requires it.
- Return ONLY the edited text, no explanations or meta-commentary.
- If context is provided, use it to maintain consistency but do not include it in output.
""".trimIndent()

            val contextBlock = if (contextHint.isNotBlank()) "\n\nCONTEXT:\n$contextHint" else ""

            val userPrompt = """
TEXT TO EDIT:
$selectedText$contextBlock

INSTRUCTION: $instruction

Return only the edited text.
""".trimIndent()

            val messages = JSONArray()
                .put(JSONObject().put("role", "system").put("content", systemPrompt))
                .put(JSONObject().put("role", "user").put("content", userPrompt))

            val body = JSONObject()
                .put("model", model)
                .put("messages", messages)
                .put("max_tokens", maxTokens)
                .put("temperature", 0.4)
                .put("reasoning", JSONObject().put("effort", "none"))
                .toString()

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .addHeader("HTTP-Referer", "https://pinster.app")
                .addHeader("X-Title", "Pinster")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()

                if (response.code == 402) {
                    return Result.failure(Exception("Not enough OpenRouter credits. Try shorter text or lower-cost model."))
                }
                if (!response.isSuccessful) {
                    return Result.failure(Exception("HTTP ${response.code}: $responseBody"))
                }

                val json = JSONObject(responseBody)
                val content = json.optJSONArray("choices")
                    ?.optJSONObject(0)
                    ?.optJSONObject("message")
                    ?.optString("content")
                    ?.trim()
                    .orEmpty()

                if (content.isBlank() || content == "null") {
                    return Result.failure(Exception("AI returned an empty response."))
                }

                Result.success(content)
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun testConnection(
        endpoint: String,
        model: String,
        apiKey: String
    ): Result<String> {

        return try {

            val url =
                endpoint.trimEnd('/') + "/chat/completions"

            val body = JSONObject()
                .put("model", model)
                .put(
                    "messages",
                    JSONArray()
                        .put(
                            JSONObject()
                                .put("role", "user")
                                .put(
                                    "content",
                                    "Reply with exactly: PINSTER API WORKS"
                                )
                        )
                )
                .put("max_tokens", 20)
                .toString()

            val request = Request.Builder()
                .url(url)
                .addHeader(
                    "Authorization",
                    "Bearer $apiKey"
                )
                .addHeader(
                    "Content-Type",
                    "application/json"
                )
                .post(
                    body.toRequestBody(
                        "application/json".toMediaType()
                    )
                )
                .build()

            client.newCall(request).execute().use { response ->

                val responseBody =
                    response.body?.string().orEmpty()

                if (!response.isSuccessful) {
                    return Result.failure(
                        Exception(
                            "HTTP ${response.code}: $responseBody"
                        )
                    )
                }

                val json = JSONObject(responseBody)

                val content =
                    json.optJSONArray("choices")
                        ?.optJSONObject(0)
                        ?.optJSONObject("message")
                        ?.optString("content")
                        .orEmpty()

                if (content.isBlank()) {
                    return Result.failure(
                        Exception("API returned no response.")
                    )
                }

                Result.success(content)
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun testBrowserlessConnection(apiKey: String): Result<String> {
        return try {
            if (apiKey.isBlank()) {
                return Result.failure(Exception("Browserless API key is empty."))
            }

            val fingerprint = java.security.MessageDigest
                .getInstance("SHA-256")
                .digest(apiKey.trim().toByteArray())
                .joinToString("") { "%02x".format(it) }

            val url = "https://production-sfo.browserless.io/content?token=${apiKey.trim()}"

            val body = JSONObject()
                .put("url", "https://example.com")
                .put("waitForTimeout", 1000)
                .toString()
                .toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(url)
                .post(body)
                .header("Content-Type", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return Result.failure(
                        Exception("Browserless HTTP ${response.code}: TOKEN_FINGERPRINT=$fingerprint BODY=${responseBody.take(300)}")
                    )
                }
                Result.success("BROWSERLESS HTTP ${response.code}: ${responseBody.take(300)}")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}
