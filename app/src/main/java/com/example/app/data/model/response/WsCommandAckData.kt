package com.example.app.data.model.response

data class WsCommandAckData(
    val id: Long? = null,
    val commandId: String? = null,
    val vehicleId: String? = null,
    val userId: Long? = null,
    val type: String? = null,
    val result: String? = null,
    val requestPayload: String? = null,
    val responsePayload: String? = null,
    val requestTime: String? = null,
    val responseTime: String? = null
)