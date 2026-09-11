package com.delish.pinster

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Creative hook strategy categories.
 * Each poster needs a creative reason to exist beyond "here is a product."
 */
enum class HookStrategy {
    BENEFIT,
    PROBLEM_SOLUTION,
    LIFESTYLE,
    USE_CASE,
    CURIOSITY,
    TRANSFORMATION,
    FEATURE,
    EMOTION,
    EDITORIAL,
    IDENTITY
}

/**
 * How the product occupies the canvas.
 */
enum class ProductPlacement {
    CENTER,
    LOWER_CENTER,
    LOWER_THIRD,
    LEFT_HERO,
    RIGHT_HERO,
    OFFSET,
    CROPPED_EDITORIAL,
    FRAMED
}

/**
 * Visual weight distribution.
 */
enum class VisualWeight {
    SYMMETRIC,
    TOP_HEAVY,
    BOTTOM_HEAVY,
    LEFT_HEAVY,
    RIGHT_HEAVY,
    DIAGONAL
}

/**
 * A lightweight creative blueprint for one poster.
 * Derived from ProductFingerprint + SeoResult — no additional API call.
 */
data class CreativeConcept(
    val hook: String,
    val supportCopy: String,
    val productLabel: String,
    val hookStrategy: HookStrategy,
    val artDirection: ArtDirection,
    val compositionMode: CompositionMode,
    val visualMood: String,
    val emphasis: VisualWeight,
    val productPlacement: ProductPlacement,
    val productScale: Float,
    val headlineAlignment: TextAlign,
    val supportAlignment: TextAlign,
    val negativeSpaceBias: Float,
    val lightingMood: String,
    val decorationDensity: Int,
    val eyebrowText: String
)

object ConceptEngine {

    /**
     * Generate 5 distinct creative concepts, one per art direction.
     * Each gets a different hook strategy, composition, and visual mood.
     */
    fun generate(
        fp: ProductFingerprint,
        seo: SeoResult,
        endpoint: String = "",
        model: String = "",
        apiKey: String = ""
    ): List<CreativeConcept> {

        val brand = extractBrand(seo)
        val coreFeature = extractCoreFeature(seo)
        val benefit = extractBenefit(seo)
        val useCase = extractUseCase(seo)
        val productLabel = buildProductLabel(brand, seo)

        val aiHooks = if (endpoint.isNotBlank() && apiKey.isNotBlank()) {
            fetchAiHooks(fp, seo, endpoint, model, apiKey, brand, coreFeature)
        } else null

        val directions = listOf(
            ArtDirection.LUXURY_EDITORIAL,
            ArtDirection.MODERN_MINIMAL,
            ArtDirection.ORGANIC_LIFESTYLE,
            ArtDirection.BOLD_CAMPAIGN,
            ArtDirection.SOFT_PINTEREST
        )

        return directions.mapIndexed { idx, dir ->
            val hookFromAi = aiHooks?.getOrNull(idx)
            buildConcept(dir, fp, seo, brand, coreFeature, benefit, useCase, productLabel, hookFromAi)
        }
    }

    private fun fetchAiHooks(
        fp: ProductFingerprint,
        seo: SeoResult,
        endpoint: String,
        model: String,
        apiKey: String,
        brand: String,
        coreFeature: String
    ): List<String>? {
        return try {
            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(40, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            val systemPrompt = """
You are a Pinterest creative director. Generate 5 distinct creative hooks for product pin posters.

Each hook must be a short headline (2-4 lines, max 35 chars per line) that makes someone STOP SCROLLING.

The hooks must be DIFFERENT from each other — not color variants, but conceptually distinct ideas.

PRODUCT: ${seo.title}
${if (seo.description.isNotBlank()) "DESCRIPTION: ${seo.description.take(300)}" else ""}
BRAND: $brand
CATEGORY: ${fp.productCategory}
${if (coreFeature.isNotBlank()) "KEY FEATURE: $coreFeature" else ""}

Return ONLY valid JSON:
{
  "hook1": "EDITORIAL HOOK\nMULTI-LINE",
  "hook2": "BENEFIT HOOK\nMULTI-LINE",
  "hook3": "LIFESTYLE HOOK\nMULTI-LINE",
  "hook4": "CAMPAIGN HOOK\nMULTI-LINE",
  "hook5": "SOFT/EMOTIONAL HOOK\nMULTI-LINE"
}

Rules:
- Use \\n for line breaks within each hook
- NEVER use the raw product title as the hook
- Think like a magazine headline writer
- Each hook must communicate WHY someone should care
- Keep hooks under 35 characters per line
- Max 3 lines per hook
- No quotes inside hook values
""".trimIndent()

            val messages = org.json.JSONArray()
                .put(org.json.JSONObject().put("role", "system").put("content", systemPrompt))
                .put(org.json.JSONObject().put("role", "user").put("content", "Generate 5 creative hooks for this product."))

            val body = org.json.JSONObject()
                .put("model", model.ifBlank { "kimi-latest" })
                .put("messages", messages)
                .put("response_format", org.json.JSONObject().put("type", "json_object"))
                .put("max_completion_tokens", 500)
                .toString()

            val url = endpoint.trimEnd('/') + "/chat/completions"
            val request = okhttp3.Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val responseBody = response.body?.string().orEmpty()
                val json = org.json.JSONObject(responseBody)
                val content = json.optJSONArray("choices")
                    ?.optJSONObject(0)
                    ?.optJSONObject("message")
                    ?.optString("content")
                    ?.trim()
                    .orEmpty()

                if (content.isBlank()) return null

                val cleanJson = content
                    .removePrefix("```json")
                    .removePrefix("```")
                    .removeSuffix("```")
                    .trim()

                val result = org.json.JSONObject(cleanJson)
                listOf(
                    result.optString("hook1", ""),
                    result.optString("hook2", ""),
                    result.optString("hook3", ""),
                    result.optString("hook4", ""),
                    result.optString("hook5", "")
                ).filter { it.isNotBlank() }.ifEmpty { null }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun buildConcept(
        dir: ArtDirection,
        fp: ProductFingerprint,
        seo: SeoResult,
        brand: String,
        coreFeature: String,
        benefit: String,
        useCase: String,
        productLabel: String,
        aiHook: String? = null
    ): CreativeConcept {

        return when (dir) {
            ArtDirection.LUXURY_EDITORIAL -> CreativeConcept(
                hook = aiHook ?: generateEditorialHook(fp, seo, brand),
                supportCopy = generateEditorialSupport(fp, seo),
                productLabel = productLabel,
                hookStrategy = HookStrategy.EDITORIAL,
                artDirection = ArtDirection.LUXURY_EDITORIAL,
                compositionMode = CompositionMode.HERO,
                visualMood = "cinematic editorial",
                emphasis = VisualWeight.SYMMETRIC,
                productPlacement = choosePlacement(fp, ProductPlacement.CENTER),
                productScale = chooseScale(fp, 0.70f, 0.78f),
                headlineAlignment = TextAlign.CENTER,
                supportAlignment = TextAlign.CENTER,
                negativeSpaceBias = 0.42f,
                lightingMood = "dramatic",
                decorationDensity = 1,
                eyebrowText = chooseEyebrow(dir, 0)
            )
            ArtDirection.MODERN_MINIMAL -> CreativeConcept(
                hook = aiHook ?: generateMinimalHook(fp, seo, benefit),
                supportCopy = generateMinimalSupport(fp, seo),
                productLabel = productLabel,
                hookStrategy = HookStrategy.BENEFIT,
                artDirection = ArtDirection.MODERN_MINIMAL,
                compositionMode = CompositionMode.FLOAT,
                visualMood = "clean precision",
                emphasis = VisualWeight.TOP_HEAVY,
                productPlacement = choosePlacement(fp, ProductPlacement.LOWER_CENTER),
                productScale = chooseScale(fp, 0.68f, 0.74f),
                headlineAlignment = TextAlign.LEFT,
                supportAlignment = TextAlign.LEFT,
                negativeSpaceBias = 0.48f,
                lightingMood = "soft",
                decorationDensity = 0,
                eyebrowText = chooseEyebrow(dir, 1)
            )
            ArtDirection.ORGANIC_LIFESTYLE -> CreativeConcept(
                hook = aiHook ?: generateLifestyleHook(fp, seo, useCase),
                supportCopy = generateLifestyleSupport(fp, seo),
                productLabel = productLabel,
                hookStrategy = HookStrategy.LIFESTYLE,
                artDirection = ArtDirection.ORGANIC_LIFESTYLE,
                compositionMode = CompositionMode.ASYMMETRIC,
                visualMood = "warm editorial",
                emphasis = VisualWeight.DIAGONAL,
                productPlacement = choosePlacement(fp, ProductPlacement.RIGHT_HERO),
                productScale = chooseScale(fp, 0.70f, 0.76f),
                headlineAlignment = TextAlign.LEFT,
                supportAlignment = TextAlign.LEFT,
                negativeSpaceBias = 0.38f,
                lightingMood = "warm diffuse",
                decorationDensity = 2,
                eyebrowText = chooseEyebrow(dir, 2)
            )
            ArtDirection.BOLD_CAMPAIGN -> CreativeConcept(
                hook = aiHook ?: generateCampaignHook(fp, seo, coreFeature),
                supportCopy = generateCampaignSupport(fp, seo),
                productLabel = productLabel,
                hookStrategy = HookStrategy.TRANSFORMATION,
                artDirection = ArtDirection.BOLD_CAMPAIGN,
                compositionMode = CompositionMode.SPLIT,
                visualMood = "energetic commercial",
                emphasis = VisualWeight.LEFT_HEAVY,
                productPlacement = choosePlacement(fp, ProductPlacement.RIGHT_HERO),
                productScale = chooseScale(fp, 0.72f, 0.78f),
                headlineAlignment = TextAlign.LEFT,
                supportAlignment = TextAlign.LEFT,
                negativeSpaceBias = 0.35f,
                lightingMood = "directional",
                decorationDensity = 2,
                eyebrowText = chooseEyebrow(dir, 3)
            )
            ArtDirection.SOFT_PINTEREST -> CreativeConcept(
                hook = aiHook ?: generateSoftHook(fp, seo, benefit),
                supportCopy = generateSoftSupport(fp, seo),
                productLabel = productLabel,
                hookStrategy = HookStrategy.EMOTION,
                artDirection = ArtDirection.SOFT_PINTEREST,
                compositionMode = CompositionMode.CARD,
                visualMood = "gentle premium",
                emphasis = VisualWeight.BOTTOM_HEAVY,
                productPlacement = choosePlacement(fp, ProductPlacement.CENTER),
                productScale = chooseScale(fp, 0.72f, 0.78f),
                headlineAlignment = TextAlign.CENTER,
                supportAlignment = TextAlign.CENTER,
                negativeSpaceBias = 0.40f,
                lightingMood = "diffuse",
                decorationDensity = 2,
                eyebrowText = chooseEyebrow(dir, 4)
            )
        }
    }

    // ─── HOOK GENERATORS ─────────────────────────
    // These derive creative hooks locally from product evidence.
    // No API call. Pure string analysis + product fingerprint.

    private fun generateEditorialHook(fp: ProductFingerprint, seo: SeoResult, brand: String): String {
        val title = seo.title
        val words = title.split(" ").filter { it.length > 2 }
        val key = words.take(4).joinToString(" ").uppercase()
        val hooks = listOf(
            "THE ART OF\n${key}",
            "EXPERIENCE\n${key}",
            "ELEVATE YOUR\n${key.take(12)}",
            "${brand.uppercase()}\nDEFINED",
            "CURATED FOR\nTHE EXTRAORDINARY"
        )
        return hooks[fp.dominantColors.size % hooks.size]
    }

    private fun generateMinimalHook(fp: ProductFingerprint, seo: SeoResult, benefit: String): String {
        if (benefit.isNotBlank() && benefit.length < 40) {
            return benefit.uppercase().take(40)
        }
        val title = seo.title
        val words = title.split(" ").filter { it.length > 2 }.take(3)
        val clean = words.joinToString(" ").uppercase()
        val hooks = listOf(
            "SIMPLY\n${clean}",
            "ESSENTIAL\n${clean}",
            "REFINED\n${clean}",
            "CLEARLY\n${clean}"
        )
        return hooks[fp.visualMass.toInt() % hooks.size]
    }

    private fun generateLifestyleHook(fp: ProductFingerprint, seo: SeoResult, useCase: String): String {
        if (useCase.isNotBlank() && useCase.length < 45) {
            return useCase.split(" ").take(5).joinToString(" ").uppercase()
        }
        val title = seo.title
        val words = title.split(" ").filter { it.length > 2 }.take(3)
        val clean = words.joinToString(" ").uppercase()
        val hooks = listOf(
            "LIVE\n${clean}",
            "EVERYDAY\n${clean}",
            "YOUR SPACE\nYOUR WAY",
            "NATURALLY\n${clean}"
        )
        return hooks[fp.averageBrightness.toInt() % hooks.size]
    }

    private fun generateCampaignHook(fp: ProductFingerprint, seo: SeoResult, feature: String): String {
        if (feature.isNotBlank() && feature.length < 35) {
            return feature.uppercase().take(35)
        }
        val title = seo.title
        val words = title.split(" ").filter { it.length > 2 }.take(3)
        val clean = words.joinToString(" ").uppercase()
        val hooks = listOf(
            "GO BOLD\nWITH ${clean}",
            "POWER\n${clean}",
            "MAKE IT\n${clean}",
            "BEYOND\n${clean}"
        )
        return hooks[fp.silhouetteComplexity.toInt() % hooks.size]
    }

    private fun generateSoftHook(fp: ProductFingerprint, seo: SeoResult, benefit: String): String {
        if (benefit.isNotBlank() && benefit.length < 40) {
            return benefit.split(" ").take(4).joinToString(" ").uppercase()
        }
        val title = seo.title
        val words = title.split(" ").filter { it.length > 2 }.take(3)
        val clean = words.joinToString(" ").uppercase()
        val hooks = listOf(
            "BEAUTIFULLY\n${clean}",
            "GENTLY\n${clean}",
            "LOVELY\n${clean}",
            "WARMTH\nOF ${clean}"
        )
        return hooks[fp.averageSaturation.toInt() % hooks.size]
    }

    // ─── SUPPORT COPY GENERATORS ──────────────────

    private fun generateEditorialSupport(fp: ProductFingerprint, seo: SeoResult): String {
        val sentences = seo.description.split(Regex("[.!?]+")).map { it.trim() }.filter { it.length > 15 }
        val feature = sentences.firstOrNull { it.contains(Regex("feature|include|offer|provide|come", RegexOption.IGNORE_CASE)) }
        return feature?.take(80) ?: sentences.firstOrNull()?.take(80) ?: ""
    }

    private fun generateMinimalSupport(fp: ProductFingerprint, seo: SeoResult): String {
        val sentences = seo.description.split(Regex("[.!?]+")).map { it.trim() }.filter { it.length in 10..60 }
        return sentences.firstOrNull()?.take(55) ?: ""
    }

    private fun generateLifestyleSupport(fp: ProductFingerprint, seo: SeoResult): String {
        val sentences = seo.description.split(Regex("[.!?]+")).map { it.trim() }.filter { it.length > 15 }
        val lifestyle = sentences.firstOrNull { it.contains(Regex("home|space|room|life|daily|everyday|comfort", RegexOption.IGNORE_CASE)) }
        return lifestyle?.take(75) ?: sentences.firstOrNull()?.take(75) ?: ""
    }

    private fun generateCampaignSupport(fp: ProductFingerprint, seo: SeoResult): String {
        val sentences = seo.description.split(Regex("[.!?]+")).map { it.trim() }.filter { it.length > 15 }
        val feature = sentences.firstOrNull { it.contains(Regex("power|fast|strong|performance|high|top|best", RegexOption.IGNORE_CASE)) }
        return feature?.take(70) ?: sentences.firstOrNull()?.take(70) ?: ""
    }

    private fun generateSoftSupport(fp: ProductFingerprint, seo: SeoResult): String {
        val sentences = seo.description.split(Regex("[.!?]+")).map { it.trim() }.filter { it.length > 15 }
        val soft = sentences.firstOrNull { it.contains(Regex("design|style|beautiful|elegant|soft|gentle|comfort", RegexOption.IGNORE_CASE)) }
        return soft?.take(70) ?: sentences.firstOrNull()?.take(70) ?: ""
    }

    // ─── PRODUCT INTELLIGENCE ─────────────────────

    private fun extractBrand(seo: SeoResult): String {
        val title = seo.title
        val words = title.split(" ").filter { it.length > 1 }
        if (words.isNotEmpty()) {
            val first = words[0]
            if (first.all { it.isUpperCase() || it == '-' } && first.length in 2..20) {
                return first
            }
        }
        return words.firstOrNull() ?: "PRODUCT"
    }

    private fun extractCoreFeature(seo: SeoResult): String {
        val text = "${seo.title} ${seo.description}"
        val featurePatterns = listOf(
            Regex("(\\d+[Kk]\\s*(?:resolution|display|screen|ansi|lumens)?)", RegexOption.IGNORE_CASE),
            Regex("((?:wireless|bluetooth|wifi|smart|portable|waterproof|rechargeable|battery)[\\s\\w]{0,20})", RegexOption.IGNORE_CASE),
            Regex("((?:\\d+\\s*(?:GB|MB|TB|mAh|W|Hz|fps|lm|ANSI)))", RegexOption.IGNORE_CASE),
            Regex("((?:4K|HD|FHD|UHD|HDR|LED|LCD|OLED|AMOLED))", RegexOption.IGNORE_CASE),
            Regex("((?:fast|quick|rapid|turbo|boost|pro|ultra|max|plus)[\\s\\w]{0,15})", RegexOption.IGNORE_CASE)
        )
        for (p in featurePatterns) {
            val m = p.find(text)
            if (m != null) return m.value.trim().take(30)
        }
        return ""
    }

    private fun extractBenefit(seo: SeoResult): String {
        val text = "${seo.title} ${seo.description}".lowercase()
        val benefitPatterns = listOf(
            Regex("(?:enjoy|experience|discover|transform|upgrade|improve|enhance|bring|make|create|turn)[\\s\\w]{5,35}", RegexOption.IGNORE_CASE),
            Regex("(?:perfect for|ideal for|designed for|great for)[\\s\\w]{5,35}", RegexOption.IGNORE_CASE),
            Regex("(?:save|reduce|eliminate|prevent|protect|keep|maintain)[\\s\\w]{5,35}", RegexOption.IGNORE_CASE)
        )
        for (p in benefitPatterns) {
            val m = p.find(text)
            if (m != null) {
                val words = m.value.trim().split(" ").take(6)
                return words.joinToString(" ").uppercase()
            }
        }
        return ""
    }

    private fun extractUseCase(seo: SeoResult): String {
        val text = "${seo.title} ${seo.description}".lowercase()
        val useCasePatterns = listOf(
            Regex("(?:for|in|at|during|when|while|where)[\\s\\w]{5,35}", RegexOption.IGNORE_CASE),
            Regex("(?:home|office|kitchen|bedroom|bathroom|living room|outdoor|travel|gym|car)[\\s\\w]{0,25}", RegexOption.IGNORE_CASE),
            Regex("(?:morning|night|daily|weekend|holiday|party|gathering)[\\s\\w]{0,25}", RegexOption.IGNORE_CASE)
        )
        for (p in useCasePatterns) {
            val m = p.find(text)
            if (m != null) {
                val words = m.value.trim().split(" ").take(5)
                return words.joinToString(" ").uppercase()
            }
        }
        return ""
    }

    private fun buildProductLabel(brand: String, seo: SeoResult): String {
        val words = seo.title.split(" ").filter { it.length > 2 }
        val key = words.take(3).joinToString(" ").uppercase()
        return if (brand.isNotBlank() && key.contains(brand.uppercase())) {
            key
        } else {
            "${brand.uppercase()} ${key}".trim().take(35)
        }
    }

    private fun choosePlacement(fp: ProductFingerprint, fallback: ProductPlacement): ProductPlacement {
        val aspect = fp.aspectRatio
        return when {
            aspect > 1.3f -> ProductPlacement.CROPPED_EDITORIAL
            aspect < 0.5f -> ProductPlacement.LOWER_THIRD
            fp.visualMass > 0.6f -> ProductPlacement.CENTER
            fp.negativeSpaceLeft > 0.25f -> ProductPlacement.RIGHT_HERO
            fp.negativeSpaceRight > 0.25f -> ProductPlacement.LEFT_HERO
            else -> fallback
        }
    }

    private fun chooseScale(fp: ProductFingerprint, minScale: Float, maxScale: Float): Float {
        val base = minScale + (maxScale - minScale) * (1f - fp.visualMass)
        val aspectBoost = when {
            fp.aspectRatio > 1.2f -> 0.05f
            fp.aspectRatio < 0.6f -> -0.03f
            else -> 0f
        }
        return (base + aspectBoost).coerceIn(minScale, maxScale)
    }

    private fun chooseEyebrow(dir: ArtDirection, seed: Int): String {
        val eyebrows = mapOf(
            ArtDirection.LUXURY_EDITORIAL to listOf("FEATURED", "EDITOR'S PICK", "CURATED", "SELECTED", "REVIEWED"),
            ArtDirection.MODERN_MINIMAL to listOf("INTRODUCING", "NEW", "ESSENTIAL", "REFINED", "DEFINED"),
            ArtDirection.ORGANIC_LIFESTYLE to listOf("LIVE WELL", "EVERYDAY", "HOME", "NATURAL", "WELLNESS"),
            ArtDirection.BOLD_CAMPAIGN to listOf("CAMPAIGN", "COLLECTION", "LIMITED", "BOLD", "statement"),
            ArtDirection.SOFT_PINTEREST to listOf("LOVELY", "REFINED", "ELEGANT", "TIMELESS", "GENTLE")
        )
        val options = eyebrows[dir] ?: listOf("FEATURED")
        return options[seed % options.size]
    }
}
