package com.vasik96.nonweb.stravacz.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Encrypts remembered credentials with an Android Keystore AES-GCM key. */
class CredentialStore(context: Context) {
    private val prefs = context.getSharedPreferences("strava_credentials", Context.MODE_PRIVATE)
    private val alias = "stravacz_credentials_key"

    fun save(username: String, password: String, canteen: String) {
        val plain = JSONObject().put("username", username).put("password", password).put("canteen", canteen).toString().toByteArray(Charsets.UTF_8)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(plain)
        prefs.edit().putString("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .putString("data", Base64.encodeToString(encrypted, Base64.NO_WRAP)).apply()
    }

    fun load(): SavedCredentials? = runCatching {
        val iv = prefs.getString("iv", null) ?: return null
        val data = prefs.getString("data", null) ?: return null
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)))
        val json = JSONObject(String(cipher.doFinal(Base64.decode(data, Base64.NO_WRAP)), Charsets.UTF_8))
        SavedCredentials(json.getString("username"), json.getString("password"), json.getString("canteen"))
    }.getOrNull()

    fun clear() { prefs.edit().clear().apply() }

    private fun getOrCreateKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setRandomizedEncryptionRequired(true).build())
        return generator.generateKey()
    }
}
data class SavedCredentials(val username: String, val password: String, val canteen: String)
