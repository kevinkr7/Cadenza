package com.example.repository

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

class CadenzaAuthManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    companion object {
        private const val PREFS_NAME = "cadenza_auth_prefs"
        private const val KEY_IS_LOGGED_IN = "key_is_logged_in"
        private const val KEY_CURRENT_EMAIL = "key_current_email"
        private const val KEY_CURRENT_NAME = "key_current_name"
        private const val KEY_HAS_COMPLETED_ONBOARDING = "key_has_completed_onboarding"
    }

    var isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_LOGGED_IN, value).apply()

    var currentEmail: String?
        get() = prefs.getString(KEY_CURRENT_EMAIL, null)
        set(value) = prefs.edit().putString(KEY_CURRENT_EMAIL, value).apply()

    var currentDisplayName: String?
        get() = prefs.getString(KEY_CURRENT_NAME, null)
        set(value) = prefs.edit().putString(KEY_CURRENT_NAME, value).apply()

    var hasCompletedOnboarding: Boolean
        get() = prefs.getBoolean(KEY_HAS_COMPLETED_ONBOARDING, false)
        set(value) = prefs.edit().putBoolean(KEY_HAS_COMPLETED_ONBOARDING, value).apply()

    fun saveSession(email: String, displayName: String) {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_CURRENT_EMAIL, email)
            .putString(KEY_CURRENT_NAME, displayName)
            .apply()
    }

    fun clearSession() {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .remove(KEY_CURRENT_EMAIL)
            .remove(KEY_CURRENT_NAME)
            .apply()
    }

    fun generateSalt(): String {
        val random = SecureRandom()
        val saltBytes = ByteArray(16)
        random.nextBytes(saltBytes)
        return Base64.getEncoder().encodeToString(saltBytes)
    }

    fun hashPassword(password: String, salt: String): String {
        val combined = "$password:$salt"
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(combined.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
