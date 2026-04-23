package com.example.app.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.app.data.model.response.WsCommandAckData
import com.example.app.data.model.response.WsVehicleStateData
import com.example.app.data.remote.ws.WsEventListener
import com.example.app.data.repository.RealtimeRepository
import com.example.app.di.NetworkModule
import kotlinx.coroutines.launch

class AppRealtimeViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext

    private val tokenStore = NetworkModule.provideTokenStore(appContext)
    private val authInterceptor = NetworkModule.provideAuthInterceptor(tokenStore)
    private val loggingInterceptor = NetworkModule.provideLoggingInterceptor()
    private val okHttpClient = NetworkModule.provideOkHttpClient(authInterceptor, loggingInterceptor)
    private val appWebSocketClient = NetworkModule.provideAppWebSocketClient(okHttpClient)
    private val realtimeRepository = RealtimeRepository(tokenStore, appWebSocketClient)

    private val _uiState = MutableLiveData(AppRealtimeUiState())
    val uiState: LiveData<AppRealtimeUiState> = _uiState

    init {
        connectIfNeeded()
    }

    fun connectIfNeeded() {
        if (realtimeRepository.isConnected()) {
            _uiState.value = (_uiState.value ?: AppRealtimeUiState()).copy(
                isConnecting = false,
                wsConnected = true,
                errorMessage = null
            )
            return
        }

        _uiState.value = (_uiState.value ?: AppRealtimeUiState()).copy(
            isConnecting = true,
            errorMessage = null
        )

        viewModelScope.launch {
            realtimeRepository.connect(object : WsEventListener {
                override fun onConnected(userId: Long?) {
                    postState {
                        it.copy(
                            isConnecting = false,
                            wsConnected = true,
                            errorMessage = null
                        )
                    }
                }

                override fun onCommandAck(data: WsCommandAckData) {
                    postState {
                        it.copy(
                            isConnecting = false,
                            wsConnected = true,
                            latestCommandAck = data,
                            errorMessage = null
                        )
                    }
                }

                override fun onVehicleState(data: WsVehicleStateData) {
                    postState {
                        it.copy(
                            isConnecting = false,
                            wsConnected = true,
                            latestVehicleState = data,
                            latestVehicleStateText = buildVehicleStateSummary(data),
                            errorMessage = null
                        )
                    }
                }

                override fun onError(message: String) {
                    postState {
                        it.copy(
                            isConnecting = false,
                            wsConnected = false,
                            errorMessage = message
                        )
                    }
                }

                override fun onClosed() {
                    postState {
                        it.copy(
                            isConnecting = false,
                            wsConnected = false
                        )
                    }
                }
            })
        }
    }

    fun reconnect() {
        realtimeRepository.disconnect()
        _uiState.value = (_uiState.value ?: AppRealtimeUiState()).copy(
            isConnecting = false,
            wsConnected = false,
            errorMessage = null
        )
        connectIfNeeded()
    }

    fun sendPing(): Boolean {
        return realtimeRepository.sendPing()
    }

    private fun buildVehicleStateSummary(data: WsVehicleStateData): String {
        return "锁:${data.lockStatus ?: "-"} " +
                "发动机:${data.engineStatus ?: "-"} " +
                "空调:${data.hvacStatus ?: "-"} " +
                "车窗:${data.windowStatus ?: "-"} " +
                "油量:${data.fuelLevel ?: "-"} " +
                "更新时间:${data.updatedTime ?: "-"}"
    }

    private fun postState(transform: (AppRealtimeUiState) -> AppRealtimeUiState) {
        val current = _uiState.value ?: AppRealtimeUiState()
        _uiState.postValue(transform(current))
    }

    override fun onCleared() {
        super.onCleared()
        realtimeRepository.disconnect()
    }
}