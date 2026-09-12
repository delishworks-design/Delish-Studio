package com.delish.pinster

import android.content.Context
import org.json.JSONObject
import java.io.File

object DocumentCache {

    private const val CACHE_DIR = "document_cache"
    private const val PROFILE_FILE = "profile.json"
    private const val EXCERPTS_DIR = "excerpts"
    private const val META_FILE = "metadata.json"

    private fun cacheRoot(context: Context): File {
        return File(context.filesDir, CACHE_DIR)
    }

    private fun cacheDir(context: Context, fingerprint: String): File {
        return File(cacheRoot(context), fingerprint)
    }

    fun getProfile(context: Context, fingerprint: String): DocumentProfile? {
        val file = File(cacheDir(context, fingerprint), PROFILE_FILE)
        if (!file.exists()) return null
        return try {
            val json = JSONObject(file.readText())
            DocumentProfile.fromJson(json)
        } catch (e: Exception) {
            null
        }
    }

    fun saveProfile(context: Context, fingerprint: String, profile: DocumentProfile, meta: CacheMetadata) {
        val dir = cacheDir(context, fingerprint)
        dir.mkdirs()

        File(dir, PROFILE_FILE).writeText(profile.toJson().toString(2))
        File(dir, META_FILE).writeText(meta.toJson().toString(2))
    }

    fun getMetadata(context: Context, fingerprint: String): CacheMetadata? {
        val file = File(cacheDir(context, fingerprint), META_FILE)
        if (!file.exists()) return null
        return try {
            val json = JSONObject(file.readText())
            CacheMetadata.fromJson(json)
        } catch (e: Exception) {
            null
        }
    }

    fun hasProfile(context: Context, fingerprint: String): Boolean {
        return File(cacheDir(context, fingerprint), PROFILE_FILE).exists()
    }

    fun deleteProfile(context: Context, fingerprint: String) {
        val dir = cacheDir(context, fingerprint)
        if (dir.exists()) dir.deleteRecursively()
    }

    fun saveExcerpt(context: Context, fingerprint: String, sectionId: String, text: String) {
        val excerptsDir = File(cacheDir(context, fingerprint), EXCERPTS_DIR)
        excerptsDir.mkdirs()
        File(excerptsDir, "$sectionId.txt").writeText(text)
    }

    fun getExcerpt(context: Context, fingerprint: String, sectionId: String): String? {
        val file = File(File(cacheDir(context, fingerprint), EXCERPTS_DIR), "$sectionId.txt")
        if (!file.exists()) return null
        return try {
            file.readText()
        } catch (e: Exception) {
            null
        }
    }

    fun saveFullText(context: Context, fingerprint: String, text: String) {
        val dir = cacheDir(context, fingerprint)
        dir.mkdirs()
        File(dir, "full_text.txt").writeText(text)
    }

    fun getFullText(context: Context, fingerprint: String): String? {
        val file = File(cacheDir(context, fingerprint), "full_text.txt")
        if (!file.exists()) return null
        return try {
            file.readText()
        } catch (e: Exception) {
            null
        }
    }

    data class CacheMetadata(
        val fingerprint: String = "",
        val fileName: String = "",
        val fileType: String = "",
        val characterCount: Int = 0,
        val createdAt: String = "",
        val primaryModel: String = "",
        val profileVersion: Int = 1
    ) {
        fun toJson(): JSONObject {
            return JSONObject().apply {
                put("fingerprint", fingerprint)
                put("fileName", fileName)
                put("fileType", fileType)
                put("characterCount", characterCount)
                put("createdAt", createdAt)
                put("primaryModel", primaryModel)
                put("profileVersion", profileVersion)
            }
        }

        companion object {
            fun fromJson(json: JSONObject): CacheMetadata {
                return CacheMetadata(
                    fingerprint = json.optString("fingerprint", ""),
                    fileName = json.optString("fileName", ""),
                    fileType = json.optString("fileType", ""),
                    characterCount = json.optInt("characterCount", 0),
                    createdAt = json.optString("createdAt", ""),
                    primaryModel = json.optString("primaryModel", ""),
                    profileVersion = json.optInt("profileVersion", 1)
                )
            }
        }
    }
}
