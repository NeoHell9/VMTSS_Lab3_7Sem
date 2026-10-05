package com.example.passwordlocker.crypto

import android.content.Context
import android.util.Base64
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import java.security.MessageDigest

object KeyManager {
    private const val PREFS = "key_prefs"
    private const val SALT = "salt"
    private const val VERIFY = "verify"
    private const val TAG = "verify-tag"

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isInitialized(c: Context) = prefs(c).contains(VERIFY)

    fun getOrCreateSalt(c: Context): ByteArray {
        val p = prefs(c)
        val s = p.getString(SALT, null)
        if (s != null) return Base64.decode(s, Base64.NO_WRAP)
        val salt = CryptoManager.randomSalt()
        p.edit().putString(SALT, Base64.encodeToString(salt, Base64.NO_WRAP)).apply()
        return salt
    }

    fun saveVerification(c: Context, key: SecretKey) {
        val mac = Mac.getInstance("HmacSHA256").apply {
            init(SecretKeySpec(key.encoded, "HmacSHA256"))
        }
        val tag = mac.doFinal(TAG.toByteArray())
        prefs(c).edit().putString(VERIFY, Base64.encodeToString(tag, Base64.NO_WRAP)).apply()
    }

    fun verify(c: Context, key: SecretKey): Boolean {
        val stored = prefs(c).getString(VERIFY, null) ?: return false
        val mac = Mac.getInstance("HmacSHA256").apply {
            init(SecretKeySpec(key.encoded, "HmacSHA256"))
        }
        val tag = mac.doFinal(TAG.toByteArray())
        return MessageDigest.isEqual(Base64.decode(stored, Base64.NO_WRAP), tag)
    }

    fun reset(c: Context) {
        prefs(c).edit().clear().apply()
    }
}