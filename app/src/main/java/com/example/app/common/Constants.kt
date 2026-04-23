package com.example.app.common

object Constants {

    const val DEFAULT_BASE_URL = "http://10.67.68.31:8080/"

    const val PREFS_TOKEN = "prefs_token"
    const val PREFS_USER_ID = "prefs_user_id"
    const val PREFS_USERNAME = "prefs_username"

    const val AUTH_HEADER = "Authorization"
    const val TOKEN_PREFIX = "Bearer "

    @Volatile
    var BASE_URL: String = DEFAULT_BASE_URL
        private set

    @Volatile
    var WS_BASE_URL: String = toWsBaseUrl(DEFAULT_BASE_URL)
        private set

    fun applyServerBaseUrl(rawUrl: String): String {
        val normalized = normalizeHttpBaseUrl(rawUrl)
        BASE_URL = normalized
        WS_BASE_URL = toWsBaseUrl(normalized)
        return normalized
    }

    fun normalizeHttpBaseUrl(rawUrl: String): String {
        var url = rawUrl.trim()

        if (url.isBlank()) {
            return DEFAULT_BASE_URL
        }

        if (!url.startsWith("http://", ignoreCase = true) &&
            !url.startsWith("https://", ignoreCase = true)
        ) {
            url = "http://$url"
        }

        if (!url.endsWith("/")) {
            url += "/"
        }

        return url
    }

    fun toWsBaseUrl(httpBaseUrl: String): String {
        val normalized = normalizeHttpBaseUrl(httpBaseUrl)
        return when {
            normalized.startsWith("https://", ignoreCase = true) ->
                normalized.replaceFirst("https://", "wss://", ignoreCase = true)

            normalized.startsWith("http://", ignoreCase = true) ->
                normalized.replaceFirst("http://", "ws://", ignoreCase = true)

            else -> normalized
        }
    }
}