package com.delish.pinster

import android.content.Context
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object TitleGenerator {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val greetings = setOf(
        "hi", "hello", "hey", "yo", "sup", "hola", "hiya",
        "good morning", "good afternoon", "good evening",
        "what's up", "whats up", "how are you", "how are u",
        "um", "hmm", "ok", "okay", "yes", "no", "thanks", "ty"
    )

    fun isGreeting(text: String): Boolean {
        val clean = text.lowercase().trim()
        return clean.length <= 12 && greetings.any { clean.startsWith(it) }
    }

    fun generateTitle(
        context: Context,
        messages: List<Pair<String, String>>
    ): Result<String> {
        val userMessages = messages.filter { it.first == "user" }
        if (userMessages.isEmpty()) return Result.success("New Chat")

        val firstMsg = userMessages.first().second.trim()
        if (isGreeting(firstMsg) && userMessages.size <= 1) {
            return Result.success("Quick chat")
        }

        val sec = SecureSettings(context)
        val ep = sec.getEndpoint()
        val md = sec.getModel()
        val ky = sec.getApiKey()

        if (ep.isBlank() || ky.isBlank()) {
            return Result.success(fallbackTitle(messages))
        }

        val conversationText = messages.takeLast(6).joinToString("\n") { (role, content) ->
            "$role: ${content.replace("\n", " ").trim().take(200)}"
        }

        val prompt = """Generate a short, descriptive title (max 40 characters) for this conversation.

Rules:
- Reply with ONLY the title text, no quotes, no explanation
- Max 40 characters
- Be specific and descriptive
- Examples: "Amazon tripod SEO pins", "Wireless earbuds product pins", "Kitchen gadget title ideas"

Conversation:
$conversationText"""

        return try {
            val url = ep.trimEnd('/') + "/chat/completions"

            val body = JSONObject()
                .put("model", md)
                .put("messages", JSONArray()
                    .put(JSONObject().put("role", "system").put("content", "You generate short conversation titles. Reply with ONLY the title."))
                    .put(JSONObject().put("role", "user").put("content", prompt))
                )
                .put("max_tokens", 30)
                .put("reasoning", JSONObject().put("effort", "none"))
                .toString()

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $ky")
                .addHeader("Content-Type", "application/json")
                .addHeader("HTTP-Referer", "https://pinster.app")
                .addHeader("X-Title", "Pinster")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return Result.success(fallbackTitle(messages))
                }

                val raw = response.body?.string().orEmpty()
                android.util.Log.d("TitleGen", "RAW: ${raw.take(500)}")
                val json = try { JSONObject(raw) } catch (_: Exception) { null }
                val title = json?.optJSONArray("choices")
                    ?.optJSONObject(0)
                    ?.optJSONObject("message")
                    ?.optString("content")
                    ?.trim()
                    .orEmpty()
                    .removeSurrounding("\"")
                    .let { if (it.equals("null", ignoreCase = true)) "" else it }
                    .take(40)
                android.util.Log.d("TitleGen", "TITLE: [$title]")

                if (title.isBlank()) {
                    Result.success(fallbackTitle(messages))
                } else {
                    Result.success(title)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("TitleGen", "ERROR: ${e.message}")
            Result.success(fallbackTitle(messages))
        }
    }

    fun generatePinTitle(
        context: Context,
        productTitle: String,
        seoTitle: String
    ): Result<String> {
        val sec = SecureSettings(context)
        val ep = sec.getEndpoint()
        val md = sec.getModel()
        val ky = sec.getApiKey()

        if (ep.isBlank() || ky.isBlank()) {
            return Result.success(fallbackPinTitle(productTitle))
        }

        val prompt = """Generate a short, descriptive title (max 40 characters) for this Pinterest pin.

Rules:
- Reply with ONLY the title text, no quotes, no explanation
- Max 40 characters
- Combine product type + purpose in a concise way
- Examples: "Bluetooth Speaker SEO Pin", "Yoga Mat Product Listing", "Coffee Maker Pin Ideas"

Product: ${productTitle.take(100)}
SEO Title: ${seoTitle.take(100)}"""

        return try {
            val url = ep.trimEnd('/') + "/chat/completions"

            val body = JSONObject()
                .put("model", md)
                .put("messages", JSONArray()
                    .put(JSONObject().put("role", "system").put("content", "You generate short pin titles. Reply with ONLY the title."))
                    .put(JSONObject().put("role", "user").put("content", prompt))
                )
                .put("max_tokens", 30)
                .put("reasoning", JSONObject().put("effort", "none"))
                .toString()

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $ky")
                .addHeader("Content-Type", "application/json")
                .addHeader("HTTP-Referer", "https://pinster.app")
                .addHeader("X-Title", "Pinster")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return Result.success(fallbackPinTitle(productTitle))
                }

                val raw = response.body?.string().orEmpty()
                android.util.Log.d("TitleGen", "PIN RAW: ${raw.take(500)}")
                val json = try { JSONObject(raw) } catch (_: Exception) { null }
                val title = json?.optJSONArray("choices")
                    ?.optJSONObject(0)
                    ?.optJSONObject("message")
                    ?.optString("content")
                    ?.trim()
                    .orEmpty()
                    .removeSurrounding("\"")
                    .let { if (it.equals("null", ignoreCase = true)) "" else it }
                    .take(40)
                android.util.Log.d("TitleGen", "PIN TITLE: [$title]")

                if (title.isBlank()) {
                    Result.success(fallbackPinTitle(productTitle))
                } else {
                    Result.success(title)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("TitleGen", "PIN ERROR: ${e.message}")
            Result.success(fallbackPinTitle(productTitle))
        }
    }

    private fun fallbackTitle(messages: List<Pair<String, String>>): String {
        val firstUser = messages.firstOrNull { it.first == "user" }?.second ?: return "New Chat"
        val clean = firstUser.replace("\n", " ").trim()
        return if (clean.length <= 40) clean else clean.substring(0, 40) + "..."
    }

    private fun fallbackPinTitle(productTitle: String): String {
        val clean = productTitle.replace("\n", " ").trim()
        return if (clean.length <= 40) clean else clean.substring(0, 40) + "..."
    }
}
