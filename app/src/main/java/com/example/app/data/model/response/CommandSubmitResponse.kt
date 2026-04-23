package com.example.app.data.model.response

data class CommandSubmitResponse(
    val id: Long? = null,
    val commandId: String? = null,
    val vehicleId: String? = null,
    val userId: Long? = null,
    val type: String? = null,
    val result: String? = null,
    val requestPayload: String? = null,
    val responsePayload: String? = null,
    val requestTime: String? = null,
    val responseTime: String? = null,
    val createdTime: String? = null,
    val updatedTime: String? = null
)