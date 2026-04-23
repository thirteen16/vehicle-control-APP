package com.example.app.data.model.response

import com.google.gson.JsonObject

data class WsEnvelope(
    val type: String? = null,
    val timestamp: String? = null,
    val message: String? = null,
    val userId: Long? = null,
    val data: JsonObject? = null
)