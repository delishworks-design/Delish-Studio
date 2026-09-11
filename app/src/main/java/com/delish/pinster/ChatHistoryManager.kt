package com.delish.pinster

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object ChatHistoryManager {

    private const val PREFS = "pinster_chat_history"
    private const val KEY = "conversations"
    private const val MAX_CONVERSATIONS = 40

    fun saveConversation(context: Context, id: String, title: String, messages: List<Pair<String, String>>) {
        synchronized(this) {
            try {
                val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                val raw = prefs.getString(KEY, "[]") ?: "[]"
                val array = try { JSONArray(raw) } catch (_: Exception) { JSONArray("[]") }

                val msgArray = JSONArray()
                for (m in messages) {
                    msgArray.put(JSONObject().apply {
                        put("role", m.first)
                        put("content", m.second)
                    })
                }

                // Find existing conversation or create new
                var found = false
                for (i in 0 until array.length()) {
                    val existing = array.optJSONObject(i) ?: continue
                    if (existing.optString("id") == id) {
                        existing.put("title", title)
                        existing.put("messages", msgArray)
                        existing.put("time", System.currentTimeMillis())
                        found = true
                        break
                    }
                }

                if (!found) {
                    array.put(JSONObject().apply {
                        put("id", id)
                        put("title", title)
                        put("messages", msgArray)
                        put("time", System.currentTimeMillis())
                    })
                }

                // Sort newest first
                val sorted = (0 until array.length())
                    .mapNotNull { array.optJSONObject(it) }
                    .sortedByDescending { it.optLong("time", 0) }

                val result = JSONArray()
                for (item in sorted) result.put(item)

                // Keep max
                while (result.length() > MAX_CONVERSATIONS) {
                    result.remove(result.length() - 1)
                }

                prefs.edit().putString(KEY, result.toString()).apply()
            } catch (_: Exception) { }
        }
    }

    fun getConversations(context: Context): List<JSONObject> {
        return try {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val raw = prefs.getString(KEY, "[]") ?: "[]"
            val array = try { JSONArray(raw) } catch (_: Exception) { JSONArray("[]") }
            (0 until array.length())
                .mapNotNull { array.optJSONObject(it) }
                .sortedByDescending { it.optLong("time", 0) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun getConversation(context: Context, id: String): JSONObject? {
        return getConversations(context).find { it.optString("id") == id }
    }

    fun deleteConversation(context: Context, id: String) {
        synchronized(this) {
            try {
                val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                val raw = prefs.getString(KEY, "[]") ?: "[]"
                val array = try { JSONArray(raw) } catch (_: Exception) { JSONArray("[]") }

                val result = JSONArray()
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    if (item.optString("id") != id) result.put(item)
                }

                prefs.edit().putString(KEY, result.toString()).apply()
            } catch (_: Exception) { }
        }
    }

    fun generateTitle(messages: List<Pair<String, String>>): String {
        val firstUser = messages.firstOrNull { it.first == "user" }?.second ?: return "New Chat"
        val clean = firstUser.replace("\n", " ").trim()
        return if (clean.length <= 40) clean else clean.substring(0, 40) + "..."
    }

    fun updateTitle(context: Context, id: String, title: String) {
        synchronized(this) {
            try {
                val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                val raw = prefs.getString(KEY, "[]") ?: "[]"
                val array = try { JSONArray(raw) } catch (_: Exception) { JSONArray("[]") }

                for (i in 0 until array.length()) {
                    val existing = array.optJSONObject(i) ?: continue
                    if (existing.optString("id") == id) {
                        existing.put("title", title)
                        break
                    }
                }

                prefs.edit().putString(KEY, array.toString()).apply()
            } catch (_: Exception) { }
        }
    }

    fun togglePin(context: Context, id: String) {
        synchronized(this) {
            try {
                val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                val raw = prefs.getString(KEY, "[]") ?: "[]"
                val array = try { JSONArray(raw) } catch (_: Exception) { JSONArray("[]") }

                for (i in 0 until array.length()) {
                    val existing = array.optJSONObject(i) ?: continue
                    if (existing.optString("id") == id) {
                        val isPinned = existing.optBoolean("isPinned", false)
                        existing.put("isPinned", !isPinned)
                        break
                    }
                }

                prefs.edit().putString(KEY, array.toString()).apply()
            } catch (_: Exception) { }
        }
    }

    fun isPinned(context: Context, id: String): Boolean {
        return try {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val raw = prefs.getString(KEY, "[]") ?: "[]"
            val array = try { JSONArray(raw) } catch (_: Exception) { JSONArray("[]") }
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                if (item.optString("id") == id) return item.optBoolean("isPinned", false)
            }
            false
        } catch (_: Exception) { false }
    }
}
