package com.example.app.data.local

import android.content.Context
import com.example.app.common.Constants

class ServerConfigStore(context: Context) {

    private val sp = context.getSharedPreferences("server_config_store", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_BASE_URL = "key_base_url"
    }

    fun getBaseUrl(): String {
        val saved = sp.getString(KEY_BASE_URL, null)
        return if (saved.isNullOrBlank()) {
            Constants.applyServerBaseUrl(Constants.DEFAULT_BASE_URL)
        } else {
            Constants.applyServerBaseUrl(saved)
        }
    }

    fun saveBaseUrl(rawUrl: String): String {
        val normalized = Constants.applyServerBaseUrl(rawUrl)
        sp.edit()
            .putString(KEY_BASE_URL, normalized)
            .apply()
        return normalized
    }

    fun resetToDefault(): String {
        val normalized = Constants.applyServerBaseUrl(Constants.DEFAULT_BASE_URL)
        sp.edit()
            .putString(KEY_BASE_URL, normalized)
            .apply()
        return normalized
    }
}