package com.example.app.data.remote.ws

import com.example.app.data.model.response.WsCommandAckData
import com.example.app.data.model.response.WsVehicleStateData

interface WsEventListener {
    fun onConnected(userId: Long?)
    fun onCommandAck(data: WsCommandAckData)
    fun onVehicleState(data: WsVehicleStateData)
    fun onError(message: String)
    fun onClosed()
}