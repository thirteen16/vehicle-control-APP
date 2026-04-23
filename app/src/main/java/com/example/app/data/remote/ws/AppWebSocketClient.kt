package com.example.app.data.remote.ws

import com.example.app.common.Constants
import com.example.app.data.model.response.WsCommandAckData
import com.example.app.data.model.response.WsEnvelope
import com.example.app.data.model.response.WsVehicleStateData
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class AppWebSocketClient(
    private val okHttpClient: OkHttpClient
) {

    private val gson = Gson()

    @Volatile
    private var webSocket: WebSocket? = null

    @Volatile
    private var connected = false

    fun connect(token: String, listener: WsEventListener) {
        if (token.isBlank()) {
            listener.onError("WebSocket 连接失败：token 为空")
            return
        }

        disconnect()

        val request = Request.Builder()
            .url(buildWsUrl(token))
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                connected = true
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    if (text.equals("pong", ignoreCase = true)) {
                        return
                    }

                    val envelope = gson.fromJson(text, WsEnvelope::class.java)
                    when (envelope.type) {
                        "CONNECTED" -> {
                            listener.onConnected(envelope.userId)
                        }

                        "COMMAND_ACK" -> {
                            val json = envelope.data
                            if (json != null) {
                                val data = gson.fromJson(json, WsCommandAckData::class.java)
                                listener.onCommandAck(data)
                            }
                        }

                        "VEHICLE_STATE" -> {
                            val json = envelope.data
                            if (json != null) {
                                val data = gson.fromJson(json, WsVehicleStateData::class.java)
                                listener.onVehicleState(data)
                            }
                        }

                        else -> {
                            if (!envelope.message.isNullOrBlank()) {
                                listener.onError("收到未知消息：${envelope.message}")
                            }
                        }
                    }
                } catch (e: Exception) {
                    listener.onError("解析 WebSocket 消息失败：${e.message ?: "unknown"}")
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                connected = false
                listener.onClosed()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                connected = false
                listener.onClosed()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                connected = false
                listener.onError("WebSocket 连接失败：${t.message ?: "unknown"}")
            }
        })
    }

    fun disconnect() {
        try {
            webSocket?.close(1000, "client close")
        } catch (_: Exception) {
        } finally {
            webSocket = null
            connected = false
        }
    }

    fun isConnected(): Boolean = connected

    fun sendPing(): Boolean {
        return try {
            webSocket?.send("ping") == true
        } catch (_: Exception) {
            false
        }
    }

    private fun buildWsUrl(token: String): String {
        val encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8.toString())
        val base = Constants.BASE_URL.trim().removeSuffix("/")

        return when {
            base.startsWith("https://") -> {
                "wss://${base.removePrefix("https://")}/ws/app?token=$encodedToken"
            }

            base.startsWith("http://") -> {
                "ws://${base.removePrefix("http://")}/ws/app?token=$encodedToken"
            }

            base.startsWith("ws://") || base.startsWith("wss://") -> {
                "$base/ws/app?token=$encodedToken"
            }

            else -> {
                "ws://$base/ws/app?token=$encodedToken"
            }
        }
    }
}