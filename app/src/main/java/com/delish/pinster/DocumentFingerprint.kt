package com.delish.pinster

import android.content.Context
import android.net.Uri
import java.io.InputStream
import java.security.MessageDigest

object DocumentFingerprint {

    fun calculate(context: Context, uri: Uri): String {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                calculateFromStream(stream)
            } ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    fun calculate(file: java.io.File): String {
        return try {
            file.inputStream().use { stream ->
                calculateFromStream(stream)
            }
        } catch (e: Exception) {
            ""
        }
    }

    fun calculate(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(bytes)
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun calculateFromStream(stream: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        var bytesRead: Int
        while (stream.read(buffer).also { bytesRead = it } != -1) {
            digest.update(buffer, 0, bytesRead)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
