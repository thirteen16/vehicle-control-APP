package com.example.app.data.local

import android.content.Context
import java.security.MessageDigest

class PinStore(context: Context) {

    private val sp = context.getSharedPreferences("pin_store", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PIN_HASH = "key_pin_hash"
        private const val KEY_PIN_ENABLED = "key_pin_enabled"
        private const val SALT = "vehicle_control_local_pin_v1"
    }

    fun hasPin(): Boolean {
        val enabled = sp.getBoolean(KEY_PIN_ENABLED, false)
        val hash = sp.getString(KEY_PIN_HASH, null)
        return enabled && !hash.isNullOrBlank()
    }

    fun savePin(pin: String) {
        val hash = sha256(pin + SALT)
        sp.edit()
            .putString(KEY_PIN_HASH, hash)
            .putBoolean(KEY_PIN_ENABLED, true)
            .apply()
    }

    fun verifyPin(inputPin: String): Boolean {
        val savedHash = sp.getString(KEY_PIN_HASH, null) ?: return false
        return sha256(inputPin + SALT) == savedHash
    }

    fun clearPin() {
        sp.edit()
            .remove(KEY_PIN_HASH)
            .putBoolean(KEY_PIN_ENABLED, false)
            .apply()
    }

    private fun sha256(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray())
        val sb = StringBuilder()
        for (b in digest) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }
}