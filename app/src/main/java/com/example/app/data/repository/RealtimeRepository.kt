package com.example.app.data.repository

import com.example.app.data.local.TokenStore
import com.example.app.data.remote.ws.AppWebSocketClient
import com.example.app.data.remote.ws.WsEventListener

class RealtimeRepository(
    private val tokenStore: TokenStore,
    private val appWebSocketClient: AppWebSocketClient
) {

    suspend fun connect(listener: WsEventListener) {
        val token = tokenStore.getToken()
        if (token.isNullOrBlank()) {
            listener.onError("未获取到登录 token，请重新登录")
            return
        }
        appWebSocketClient.connect(token, listener)
    }

    fun disconnect() {
        appWebSocketClient.disconnect()
    }

    fun isConnected(): Boolean {
        return appWebSocketClient.isConnected()
    }

    fun sendPing(): Boolean {
        return appWebSocketClient.sendPing()
    }
}