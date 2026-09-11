package com.delish.pinster

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class SecureSettings(private val context: Context) {

    companion object {
        private const val KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "PinsterSettingsKey"

        private const val PREFS = "pinster_settings"

        private const val PROVIDER = "provider"
        private const val ENDPOINT = "endpoint"
        private const val MODEL = "model"
        private const val API_KEY = "api_key"
        private const val BROWSERLESS_API_KEY = "browserless_api_key"

        private const val DEFAULT_PROVIDER = "OpenRouter"
        private const val DEFAULT_ENDPOINT = "https://openrouter.ai/api/v1"
        private const val DEFAULT_MODEL = "xiaomi/mimo-v2.5"
        private const val DEFAULT_API_KEY = "YOUR_OPENROUTER_API_KEY"
        private const val DEFAULT_BROWSERLESS_TOKEN = "YOUR_BROWSERLESS_TOKEN"
    }

    private val prefs =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun getKey(): SecretKey {

        val keyStore = KeyStore.getInstance(KEYSTORE)
        keyStore.load(null)

        if (keyStore.containsAlias(KEY_ALIAS)) {
            return keyStore.getKey(KEY_ALIAS, null) as SecretKey
        }

        val generator =
            KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                KEYSTORE
            )

        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or
                        KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(
                    KeyProperties.ENCRYPTION_PADDING_NONE
                )
                .build()
        )

        return generator.generateKey()
    }

    private fun encrypt(value: String): String {

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")

        cipher.init(
            Cipher.ENCRYPT_MODE,
            getKey()
        )

        val encrypted = cipher.doFinal(
            value.toByteArray(StandardCharsets.UTF_8)
        )

        val combined = cipher.iv + encrypted

        return Base64.encodeToString(
            combined,
            Base64.NO_WRAP
        )
    }

    private fun decrypt(value: String): String {

        val combined = Base64.decode(
            value,
            Base64.NO_WRAP
        )

        val iv = combined.copyOfRange(
            0,
            12
        )

        val encrypted = combined.copyOfRange(
            12,
            combined.size
        )

        val cipher = Cipher.getInstance(
            "AES/GCM/NoPadding"
        )

        val spec =
            javax.crypto.spec.GCMParameterSpec(
                128,
                iv
            )

        cipher.init(
            Cipher.DECRYPT_MODE,
            getKey(),
            spec
        )

        return String(
            cipher.doFinal(encrypted),
            StandardCharsets.UTF_8
        )
    }

    fun saveProvider(value: String) {
        prefs.edit()
            .putString(PROVIDER, value)
            .apply()
    }

    fun getProvider(): String {
        return prefs.getString(
            PROVIDER,
            DEFAULT_PROVIDER
        ) ?: DEFAULT_PROVIDER
    }

    fun saveEndpoint(value: String) {
        prefs.edit()
            .putString(ENDPOINT, value)
            .apply()
    }

    fun getEndpoint(): String {
        return prefs.getString(
            ENDPOINT,
            DEFAULT_ENDPOINT
        ) ?: DEFAULT_ENDPOINT
    }

    fun saveModel(value: String) {
        prefs.edit()
            .putString(MODEL, value)
            .apply()
    }

    fun getModel(): String {
        return prefs.getString(
            MODEL,
            DEFAULT_MODEL
        ) ?: DEFAULT_MODEL
    }

    fun saveApiKey(value: String) {

        prefs.edit()
            .putString(
                API_KEY,
                encrypt(value)
            )
            .apply()
    }

    fun getApiKey(): String {

        val encrypted = prefs.getString(
            API_KEY,
            null
        ) ?: return DEFAULT_API_KEY

        return try {
            decrypt(encrypted)
        } catch (e: Exception) {
            DEFAULT_API_KEY
        }
    }

    fun saveBrowserlessApiKey(value: String) {
        prefs.edit()
            .putString(
                BROWSERLESS_API_KEY,
                encrypt(value)
            )
            .apply()
    }

    fun getBrowserlessApiKey(): String {
        val encrypted = prefs.getString(
            BROWSERLESS_API_KEY,
            null
        ) ?: return DEFAULT_BROWSERLESS_TOKEN

        return try {
            decrypt(encrypted)
        } catch (e: Exception) {
            DEFAULT_BROWSERLESS_TOKEN
        }
    }

}
