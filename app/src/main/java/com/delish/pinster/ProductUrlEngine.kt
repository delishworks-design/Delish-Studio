package com.delish.pinster

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit

data class ProductEvidence(
    val sourceUrl: String,
    val canonicalUrl: String,
    val title: String,
    val brand: String,
    val description: String,
    val category: String,
    val evidenceText: String,
    val imageUrls: List<String>
)

class ProductUrlEngine(private val browserlessApiKey: String) {

    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    fun extract(url: String): Result<ProductEvidence> {

        return try {

            val cleanUrl = url.trim()

            if (
                !cleanUrl.startsWith("http://") &&
                !cleanUrl.startsWith("https://")
            ) {
                return Result.failure(
                    Exception("Please enter a valid product URL.")
                )
            }

            if (browserlessApiKey.isBlank()) {
            return Result.failure(
                Exception("Browserless API key is not configured.")
            )
        }

        val browserlessUrl =
            "https://production-sfo.browserless.io/content?token=$browserlessApiKey"

        val browserlessBody = JSONObject()
            .put("url", cleanUrl)
            .put("waitForTimeout", 8000)
            .toString()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(browserlessUrl)
            .post(browserlessBody)
            .header("Content-Type", "application/json")
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .build()

        client.newCall(request).execute().use { response ->
            val html = response.body?.string().orEmpty()

            if (!response.isSuccessful || html.isBlank()) {
                return Result.failure(
                    Exception(
                        "Browserless failed. HTTP ${response.code}: ${html.take(300)}"
                    )
                )
            }

        val document = Jsoup.parse(
                    html,
                    response.request.url.toString()
                )

                // Extract DISTINCT Amazon gallery photos.
                //
                // IMPORTANT:
                // A thumbnail URL and its high-resolution URL can represent
                // the same photo. We identify the photo by Amazon's image ID,
                // then keep the highest-resolution URL for that ID.

                val imageUrls = linkedMapOf<String, Pair<String, Int>>()

                fun addAmazonImage(rawUrl: String) {
                    var url = rawUrl.trim()

                    if (
                        !url.startsWith("http://") &&
                        !url.startsWith("https://")
                    ) return

                    if (
                        !url.contains(
                            "m.media-amazon.com/images/"
                        )
                    ) return

                    // Remove whitespace accidentally embedded in copied URLs.
                    url = url.replace(
                        Regex("\\s+"),
                        ""
                    )

                    val uri = android.net.Uri.parse(url)
                    val path = uri.path.orEmpty()

                    if (path.isBlank()) return

                    val filename =
                        path.substringAfterLast("/")

                    // Amazon image IDs look like:
                    // 81V3HmeAWgL._AC_SL1500_.jpg
                    //
                    // The ID is:
                    // 81V3HmeAWgL
                    val imageId =
                        filename
                            .substringBefore("._")
                            .substringBeforeLast(".")

                    if (imageId.isBlank()) return

                    // Amazon may expose the same photo as a tiny
                    // thumbnail such as _AC_US100_, _AC_UL100_,
                    // _SL100_, etc.
                    //
                    // Do NOT request an arbitrary _AC_SL1500_ variant.
                    // Instead, remove Amazon's transformation suffix
                    // completely and request the canonical image asset.
                    //
                    // Example:
                    // 81V3HmeAWgL._AC_US100_.jpg
                    // becomes:
                    // 81V3HmeAWgL.jpg

                    val extension =
                        filename.substringAfterLast(
                            ".",
                            "jpg"
                        )

                    val canonicalUrl =
                        uri.buildUpon()
                            .path(
                                "/images/I/$imageId.$extension"
                            )
                            .clearQuery()
                            .fragment(null)
                            .build()
                            .toString()

                    // Canonical Amazon image URLs are preferred.
                    // The image ID remains the identity of the
                    // actual gallery photo.
                    val existing =
                        imageUrls[imageId]

                    if (existing == null) {
                        imageUrls[imageId] =
                            Pair(
                                canonicalUrl,
                                9999
                            )
                    }
                }

                // ------------------------------------------------------------
                // 1. Amazon gallery thumbnail elements.
                // These are the strongest signal for DISTINCT product photos.
                // ------------------------------------------------------------
                document.select(
                    "#altImages li img, " +
                    "#altImages img"
                ).forEach { img ->

                    addAmazonImage(
                        img.attr("data-old-hires")
                    )

                    addAmazonImage(
                        img.attr("src")
                    )

                    val dynamic =
                        img
                            .attr("data-a-dynamic-image")
                            .trim()

                    if (dynamic.isNotBlank()) {
                        try {
                            val json =
                                JSONObject(dynamic)

                            json.keys().forEach { key ->
                                addAmazonImage(key)
                            }
                        } catch (_: Exception) {
                        }
                    }
                }

                // ------------------------------------------------------------
                // 2. Page-level dynamic metadata.
                // This supplies high-resolution variants for gallery IDs.
                // ------------------------------------------------------------
                document
                    .select("[data-a-dynamic-image]")
                    .forEach { element ->

                        val raw =
                            element
                                .attr("data-a-dynamic-image")
                                .trim()

                        if (raw.isBlank()) {
                            return@forEach
                        }

                        try {
                            val json =
                                JSONObject(raw)

                            json.keys().forEach { key ->
                                addAmazonImage(key)
                            }
                        } catch (_: Exception) {
                        }
                    }

                // ------------------------------------------------------------
                // 3. Final fallback if Amazon's gallery markup is limited.
                // ------------------------------------------------------------
                if (imageUrls.size < 3) {
                    document.select("img").forEach { img ->

                        addAmazonImage(
                            img.attr("data-old-hires")
                        )

                        addAmazonImage(
                            img.attr("src")
                        )
                    }
                }

                // Keep up to 12 DISTINCT Amazon photo IDs.
                val extractedImageUrls =
                    imageUrls.values
                        .map { it.first }
                        .distinct()
                        .take(12)

                android.util.Log.d(
                    "PINSTER_IMAGES",
                    "DISTINCT PHOTO IDS=${imageUrls.size}"
                )

                extractedImageUrls.forEachIndexed { index, url ->
                    android.util.Log.d(
                        "PINSTER_IMAGES",
                        "PHOTO_${index + 1}=$url"
                    )
                }

                var title =
                    firstMeta(
                        document,
                        "meta[property=og:title]",
                        "meta[name=title]"
                    ).ifBlank {
                        document.title().trim()
                    }

                // Clean Amazon-specific prefixes/suffixes from title
                title = title
                    .replace(Regex("^Amazon\\.com:\\s*", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("\\s*-\\s*Amazon\\.com$", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("^Buy\\s+", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("\\s*:\\s*Buy.*$", RegexOption.IGNORE_CASE), "")
                    .trim()

                val description =
                    firstMeta(
                        document,
                        "meta[name=description]",
                        "meta[property=og:description]"
                    )

                var brand = ""
                var category = ""
                var jsonLdText = ""

                val jsonLd = JSONArray()

                document.select(
                    "script[type=application/ld+json]"
                ).forEach { script ->

                    val raw = script.data().trim()

                    if (raw.isBlank()) return@forEach

                    jsonLdText += "\n$raw"

                    try {
                        val parsed = JSONObject(raw)

                        if (
                            parsed.optString("@type")
                                .equals("Product", true)
                        ) {
                            if (title.isBlank()) {
                                title = parsed.optString("name").orEmpty()
                            }

                            brand =
                                parsed.optJSONObject("brand")
                                    ?.optString("name")
                                    .orEmpty()

                            category =
                                parsed.optString("category")
                        }

                    } catch (_: Exception) {
                        // Some pages contain invalid JSON-LD.
                    }
                }

                val productSelectors = listOf(
                    "#feature-bullets",
                    "#productDescription",
                    "#aplus",
                    "#detailBullets_feature_div",
                    "#productDetails_techSpec_section_1",
                    "#productTitle",
                    "#bylineInfo",
                    "#productOverview",
                    "#productDetails_detailBullets_sections1",
                    "#poExpander",
                    "#aplusProductDescription"
                )
                val productText = productSelectors
                    .flatMap { document.select(it) }
                    .joinToString(" ") { it.text() }
                    .replace(Regex("\\s+"), " ")
                    .trim()

                // Use product selectors if enough content, otherwise
                // fall back to a SMALLER targeted set — NOT the full body
                val bodyText = if (productText.length > 200) {
                    productText.take(8000)
                } else {
                    // Targeted fallback: only product-relevant sections
                    val fallbackSelectors = listOf(
                        "#productTitle",
                        "#bylineInfo",
                        "#feature-bullets",
                        "#productDescription",
                        "#aplus",
                        "#detailBullets_feature_div",
                        "#productOverview",
                        "#priceblock_ourprice",
                        "#priceblock_dealprice",
                        ".a-price .a-offscreen"
                    )
                    val fallbackText = fallbackSelectors
                        .flatMap { document.select(it) }
                        .joinToString(" ") { it.text() }
                        .replace(Regex("\\s+"), " ")
                        .trim()
                    fallbackText.take(8000)
                }

                if (title.isBlank() && bodyText.isBlank()) {
                    return Result.failure(
                        Exception(
                            "No usable product information was found on this page."
                        )
                    )
                }

                val evidence = buildString {

                    appendLine("SOURCE URL: $cleanUrl")
                    appendLine(
                        "FINAL URL: ${response.request.url}"
                    )
                    appendLine()

                    if (title.isNotBlank()) {
                        appendLine("PRODUCT TITLE: $title")
                    }

                    if (brand.isNotBlank()) {
                        appendLine("BRAND: $brand")
                    }

                    if (category.isNotBlank()) {
                        appendLine("CATEGORY: $category")
                    }

                    if (description.isNotBlank()) {
                        appendLine(
                            "META DESCRIPTION: $description"
                        )
                    }

                    if (jsonLdText.isNotBlank()) {
                        appendLine()
                        appendLine("STRUCTURED PRODUCT EVIDENCE:")
                        appendLine(
                            jsonLdText.take(12000)
                        )
                    }

                    appendLine()
                    appendLine("VISIBLE PAGE TEXT:")
                    appendLine(bodyText)
                }

                Result.success(
                    ProductEvidence(
                        sourceUrl = cleanUrl,
                        canonicalUrl =
                            document.select(
                                "link[rel=canonical]"
                            )
                                .attr("href")
                                .ifBlank {
                                    response.request.url.toString()
                                },
                        title = title,
                        brand = brand,
                        description = description,
                        category = category,
                        evidenceText = evidence,
                        imageUrls = extractedImageUrls
                    )
                )
            }

        } catch (e: Exception) {
            Result.failure(
                Exception(
                    "Product extraction failed: ${e.message}"
                )
            )
        }
    }

    private fun firstMeta(
        document: org.jsoup.nodes.Document,
        vararg selectors: String
    ): String {

        for (selector in selectors) {

            val value =
                document
                    .select(selector)
                    .firstOrNull()
                    ?.attr("content")
                    ?.trim()
                    .orEmpty()

            if (value.isNotBlank()) {
                return value
            }
        }

        return ""
    }
}
