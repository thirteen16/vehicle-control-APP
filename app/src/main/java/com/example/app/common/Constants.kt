package com.example.app.common

object Constants {

    /**
     * 如果你用 Android 模拟器访问本机 Spring Boot：
     * 10.0.2.2 = 宿主机 localhost
     *10.67.68.31
     * 如果你用真机调试，需要改成你电脑的局域网 IP，例如：
     * http://10.67.68.31:8080/
     */
    const val BASE_URL = "http://10.67.68.31:8080/"

    const val WS_BASE_URL = "ws://10.67.68.31:8080/ws/app?token="

    const val PREFS_TOKEN = "prefs_token"
    const val PREFS_USER_ID = "prefs_user_id"
    const val PREFS_USERNAME = "prefs_username"

    const val AUTH_HEADER = "Authorization"
    const val TOKEN_PREFIX = "Bearer "
}