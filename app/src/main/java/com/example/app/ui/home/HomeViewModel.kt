package com.example.app.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.app.common.ResultState
import com.example.app.data.local.TokenStore
import com.example.app.data.model.response.VehicleStateResponse
import com.example.app.data.model.response.WsVehicleStateData
import com.example.app.data.repository.VehicleRepository
import kotlinx.coroutines.launch

class HomeViewModel(
    private val vehicleRepository: VehicleRepository,
    private val tokenStore: TokenStore
) : ViewModel() {

    private val _uiState = MutableLiveData(HomeUiState(isLoading = true))
    val uiState: LiveData<HomeUiState> = _uiState

    init {
        loadHomeData()
    }

    fun loadHomeData() {
        viewModelScope.launch {
            val username = tokenStore.getUsername() ?: "用户"

            _uiState.value = _uiState.value?.copy(
                isLoading = true,
                username = username,
                errorMessage = null
            )

            when (val vehicleResult = vehicleRepository.getVehicles()) {
                is ResultState.Success -> {
                    val vehicleList = vehicleResult.data

                    if (vehicleList.isEmpty()) {
                        _uiState.value = HomeUiState(
                            isLoading = false,
                            username = username,
                            vehicles = emptyList(),
                            emptyMessage = "当前账号暂无绑定车辆"
                        )
                        return@launch
                    }

                    val savedVehicleId = vehicleRepository.getSelectedVehicleId()
                    val selectedVehicle = vehicleList.firstOrNull { it.vehicleId == savedVehicleId }
                        ?: vehicleList.first()

                    vehicleRepository.saveSelectedVehicleId(selectedVehicle.vehicleId)

                    loadSelectedVehicleState(
                        username = username,
                        vehicles = vehicleList,
                        selectedVehicleId = selectedVehicle.vehicleId
                    )
                }

                is ResultState.Error -> {
                    _uiState.value = HomeUiState(
                        isLoading = false,
                        username = username,
                        errorMessage = vehicleResult.message
                    )
                }

                ResultState.Loading -> {
                    _uiState.value = _uiState.value?.copy(isLoading = true)
                }
            }
        }
    }

    fun selectVehicle(vehicleId: String) {
        val current = _uiState.value ?: return
        val vehicles = current.vehicles
        val username = current.username

        viewModelScope.launch {
            vehicleRepository.saveSelectedVehicleId(vehicleId)

            _uiState.value = current.copy(
                isLoading = true,
                selectedVehicleId = vehicleId,
                errorMessage = null
            )

            loadSelectedVehicleState(
                username = username,
                vehicles = vehicles,
                selectedVehicleId = vehicleId
            )
        }
    }

    fun refreshSelectedVehicleState() {
        val current = _uiState.value ?: return
        val selectedVehicleId = current.selectedVehicleId

        if (selectedVehicleId.isNullOrBlank()) {
            loadHomeData()
            return
        }

        viewModelScope.launch {
            _uiState.value = current.copy(
                isLoading = true,
                errorMessage = null
            )

            loadSelectedVehicleState(
                username = current.username,
                vehicles = current.vehicles,
                selectedVehicleId = selectedVehicleId
            )
        }
    }

    fun applyRealtimeVehicleState(data: WsVehicleStateData) {
        val current = _uiState.value ?: return
        val selectedVehicleId = current.selectedVehicleId ?: return
        if (data.vehicleId != selectedVehicleId) return

        val oldState = current.selectedState
        val selectedVehicle = current.vehicles.firstOrNull { it.vehicleId == selectedVehicleId }

        val mergedState = VehicleStateResponse(
            vehicleId = data.vehicleId ?: selectedVehicleId,
            name = data.name ?: oldState?.name ?: selectedVehicle?.name ?: selectedVehicleId,
            brand = oldState?.brand ?: selectedVehicle?.brand,
            model = oldState?.model ?: selectedVehicle?.model,
            onlineStatus = data.onlineStatus ?: oldState?.onlineStatus ?: selectedVehicle?.onlineStatus,
            lockStatus = data.lockStatus ?: oldState?.lockStatus ?: selectedVehicle?.lockStatus,
            engineStatus = data.engineStatus ?: oldState?.engineStatus ?: selectedVehicle?.engineStatus,
            hvacStatus = data.hvacStatus ?: oldState?.hvacStatus ?: selectedVehicle?.hvacStatus,
            windowStatus = data.windowStatus ?: oldState?.windowStatus ?: selectedVehicle?.windowStatus,
            mileage = data.mileage ?: oldState?.mileage ?: selectedVehicle?.mileage,
            fuelLevel = data.fuelLevel ?: oldState?.fuelLevel ?: selectedVehicle?.fuelLevel,
            updatedTime = data.updatedTime ?: oldState?.updatedTime ?: selectedVehicle?.updatedTime
        )

        val mergedVehicles = current.vehicles.map { vehicle ->
            if (vehicle.vehicleId == selectedVehicleId) {
                vehicle.copy(
                    name = data.name ?: vehicle.name,
                    onlineStatus = data.onlineStatus ?: vehicle.onlineStatus,
                    lockStatus = data.lockStatus ?: vehicle.lockStatus,
                    engineStatus = data.engineStatus ?: vehicle.engineStatus,
                    hvacStatus = data.hvacStatus ?: vehicle.hvacStatus,
                    windowStatus = data.windowStatus ?: vehicle.windowStatus,
                    mileage = data.mileage ?: vehicle.mileage,
                    fuelLevel = data.fuelLevel ?: vehicle.fuelLevel,
                    updatedTime = data.updatedTime ?: vehicle.updatedTime
                )
            } else {
                vehicle
            }
        }

        _uiState.value = current.copy(
            isLoading = false,
            vehicles = mergedVehicles,
            selectedState = mergedState,
            errorMessage = null
        )
    }

    fun logout() {
        viewModelScope.launch {
            tokenStore.clearLoginSession()
            _uiState.value = (_uiState.value ?: HomeUiState()).copy(
                loggedOut = true
            )
        }
    }

    fun clearErrorMessage() {
        _uiState.value = (_uiState.value ?: HomeUiState()).copy(
            errorMessage = null
        )
    }

    private suspend fun loadSelectedVehicleState(
        username: String,
        vehicles: List<com.example.app.data.model.entity.Vehicle>,
        selectedVehicleId: String
    ) {
        when (val stateResult = vehicleRepository.getVehicleState(selectedVehicleId)) {
            is ResultState.Success -> {
                _uiState.value = HomeUiState(
                    isLoading = false,
                    username = username,
                    vehicles = vehicles,
                    selectedVehicleId = selectedVehicleId,
                    selectedState = stateResult.data
                )
            }

            is ResultState.Error -> {
                _uiState.value = HomeUiState(
                    isLoading = false,
                    username = username,
                    vehicles = vehicles,
                    selectedVehicleId = selectedVehicleId,
                    errorMessage = stateResult.message
                )
            }

            ResultState.Loading -> {
                _uiState.value = (_uiState.value ?: HomeUiState()).copy(isLoading = true)
            }
        }
    }

    class Factory(
        private val vehicleRepository: VehicleRepository,
        private val tokenStore: TokenStore
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(vehicleRepository, tokenStore) as T
        }
    }
}