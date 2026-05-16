package com.example.app.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.app.common.ResultState
import com.example.app.data.local.TokenStore
import com.example.app.data.model.entity.Vehicle
import com.example.app.data.model.response.VehicleStateResponse
import com.example.app.data.model.response.WsVehicleStateData
import com.example.app.data.repository.CommandRepository
import com.example.app.data.repository.VehicleRepository
import kotlinx.coroutines.launch

class HomeViewModel(
    private val vehicleRepository: VehicleRepository,
    private val commandRepository: CommandRepository,
    private val tokenStore: TokenStore
) : ViewModel() {

    private val _uiState = MutableLiveData(HomeUiState(isLoading = true))
    val uiState: LiveData<HomeUiState> = _uiState

    init {
        loadHomeData()
    }

    fun loadHomeData() {
        viewModelScope.launch {
            val current = _uiState.value ?: HomeUiState()
            _uiState.value = current.copy(isLoading = true, errorMessage = null)

            when (val vehicleResult = vehicleRepository.getVehicles()) {
                is ResultState.Success -> {
                    val vehicles = vehicleResult.data

                    if (vehicles.isEmpty()) {
                        vehicleRepository.clearSelectedVehicle()
                        _uiState.value = HomeUiState(
                            isLoading = false,
                            vehicles = emptyList(),
                            selectedVehicleId = null,
                            selectedState = null,
                            errorMessage = null
                        )
                        return@launch
                    }

                    val savedVehicleId = vehicleRepository.getSelectedVehicleId()
                    val selectedVehicle = vehicles.firstOrNull { it.vehicleId == savedVehicleId } ?: vehicles.first()
                    vehicleRepository.saveSelectedVehicleId(selectedVehicle.vehicleId)

                    when (val stateResult = vehicleRepository.getVehicleState(selectedVehicle.vehicleId)) {
                        is ResultState.Success -> {
                            _uiState.value = HomeUiState(
                                isLoading = false,
                                isRefreshingCurrent = false,
                                vehicles = vehicles,
                                selectedVehicleId = selectedVehicle.vehicleId,
                                selectedState = stateResult.data,
                                errorMessage = null
                            )
                        }
                        is ResultState.Error -> {
                            _uiState.value = HomeUiState(
                                isLoading = false,
                                isRefreshingCurrent = false,
                                vehicles = vehicles,
                                selectedVehicleId = selectedVehicle.vehicleId,
                                selectedState = fallbackStateFromVehicle(selectedVehicle),
                                errorMessage = stateResult.message
                            )
                        }
                        ResultState.Loading -> _uiState.value = current.copy(isLoading = true)
                    }
                }
                is ResultState.Error -> {
                    _uiState.value = current.copy(isLoading = false, errorMessage = vehicleResult.message)
                }
                ResultState.Loading -> _uiState.value = current.copy(isLoading = true)
            }
        }
    }

    fun requestBindVehicle(vehicleId: String, onResult: (Boolean, String, String?) -> Unit) {
        val trimmedVehicleId = vehicleId.trim()
        if (trimmedVehicleId.isBlank()) {
            onResult(false, "车辆ID不能为空", null)
            return
        }

        viewModelScope.launch {
            when (val result = vehicleRepository.requestBindVehicle(trimmedVehicleId)) {
                is ResultState.Success -> {
                    val requestId = result.data.requestId
                    onResult(true, result.data.statusText ?: "申请已提交，请等待后台审批", requestId)
                }
                is ResultState.Error -> onResult(false, result.message, null)
                ResultState.Loading -> Unit
            }
        }
    }

    fun verifyBindVehicle(requestId: String, code: String, onResult: (Boolean, String) -> Unit) {
        val trimmedRequestId = requestId.trim()
        val trimmedCode = code.trim()
        if (trimmedRequestId.isBlank()) {
            onResult(false, "申请ID不能为空")
            return
        }
        if (trimmedCode.isBlank()) {
            onResult(false, "验证码不能为空")
            return
        }

        viewModelScope.launch {
            when (val result = vehicleRepository.verifyBindVehicle(trimmedRequestId, trimmedCode)) {
                is ResultState.Success -> {
                    onResult(true, result.data.statusText ?: "车辆绑定成功")
                    loadHomeData()
                }
                is ResultState.Error -> onResult(false, result.message)
                ResultState.Loading -> Unit
            }
        }
    }

    fun requestUnbindVehicle(vehicleId: String, onResult: (Boolean, String, String?) -> Unit) {
        val trimmedVehicleId = vehicleId.trim()
        if (trimmedVehicleId.isBlank()) {
            onResult(false, "当前没有可解绑车辆", null)
            return
        }

        viewModelScope.launch {
            when (val result = vehicleRepository.requestUnbindVehicle(trimmedVehicleId)) {
                is ResultState.Success -> {
                    val requestId = result.data.requestId
                    onResult(true, result.data.statusText ?: "解绑申请已提交，请等待后台审批", requestId)
                }
                is ResultState.Error -> onResult(false, result.message, null)
                ResultState.Loading -> Unit
            }
        }
    }

    fun verifyUnbindVehicle(requestId: String, code: String, onResult: (Boolean, String) -> Unit) {
        val trimmedRequestId = requestId.trim()
        val trimmedCode = code.trim()
        if (trimmedRequestId.isBlank()) {
            onResult(false, "申请ID不能为空")
            return
        }
        if (trimmedCode.isBlank()) {
            onResult(false, "验证码不能为空")
            return
        }

        viewModelScope.launch {
            when (val result = vehicleRepository.verifyUnbindVehicle(trimmedRequestId, trimmedCode)) {
                is ResultState.Success -> {
                    vehicleRepository.clearSelectedVehicle()
                    onResult(true, result.data.statusText ?: "车辆解绑成功")
                    loadHomeData()
                }
                is ResultState.Error -> onResult(false, result.message)
                ResultState.Loading -> Unit
            }
        }
    }

    fun selectVehicle(vehicleId: String) {
        val current = _uiState.value ?: return
        if (current.selectedVehicleId == vehicleId) return

        val targetVehicle = current.vehicles.firstOrNull { it.vehicleId == vehicleId } ?: return

        viewModelScope.launch {
            vehicleRepository.saveSelectedVehicleId(vehicleId)

            _uiState.value = current.copy(
                selectedVehicleId = vehicleId,
                selectedState = fallbackStateFromVehicle(targetVehicle),
                isRefreshingCurrent = true,
                errorMessage = null
            )

            when (val result = vehicleRepository.getVehicleState(vehicleId)) {
                is ResultState.Success -> {
                    val latest = _uiState.value ?: current
                    _uiState.value = latest.copy(
                        isRefreshingCurrent = false,
                        selectedState = result.data,
                        errorMessage = null
                    )
                }
                is ResultState.Error -> {
                    val latest = _uiState.value ?: current
                    _uiState.value = latest.copy(
                        isRefreshingCurrent = false,
                        selectedState = fallbackStateFromVehicle(targetVehicle),
                        errorMessage = result.message
                    )
                }
                ResultState.Loading -> Unit
            }
        }
    }

    fun refreshSelectedVehicleState() {
        val current = _uiState.value ?: return
        val selectedVehicleId = current.selectedVehicleId ?: return

        viewModelScope.launch {
            _uiState.value = current.copy(isRefreshingCurrent = true, errorMessage = null)
            refreshSelectedVehicleStateInternal(selectedVehicleId, current)
        }
    }

    fun sendStatusQueryAndRefresh() {
        val current = _uiState.value ?: return
        val selectedVehicleId = current.selectedVehicleId

        if (selectedVehicleId.isNullOrBlank()) {
            _uiState.value = current.copy(errorMessage = "当前没有已绑定车辆")
            return
        }

        viewModelScope.launch {
            _uiState.value = current.copy(isRefreshingCurrent = true, errorMessage = null)
            var commandError: String? = null

            when (val commandResult = commandRepository.submitCommand(selectedVehicleId, "STATUS_QUERY")) {
                is ResultState.Success -> commandError = null
                is ResultState.Error -> commandError = commandResult.message
                ResultState.Loading -> Unit
            }

            val latest = _uiState.value ?: current
            refreshSelectedVehicleStateInternal(selectedVehicleId, latest, commandError)
        }
    }

    private suspend fun refreshSelectedVehicleStateInternal(
        selectedVehicleId: String,
        baseState: HomeUiState,
        extraErrorMessage: String? = null
    ) {
        when (val result = vehicleRepository.getVehicleState(selectedVehicleId)) {
            is ResultState.Success -> {
                val latest = _uiState.value ?: baseState
                val mergedVehicles = mergeVehicleListWithSelectedState(
                    latest.vehicles,
                    selectedVehicleId,
                    result.data
                )

                _uiState.value = latest.copy(
                    isRefreshingCurrent = false,
                    vehicles = mergedVehicles,
                    selectedState = result.data,
                    errorMessage = extraErrorMessage
                )
            }
            is ResultState.Error -> {
                val latest = _uiState.value ?: baseState
                _uiState.value = latest.copy(
                    isRefreshingCurrent = false,
                    errorMessage = extraErrorMessage ?: result.message
                )
            }
            ResultState.Loading -> Unit
        }
    }

    fun applyRealtimeVehicleState(data: WsVehicleStateData) {
        val current = _uiState.value ?: return
        val selectedVehicleId = current.selectedVehicleId ?: return
        if (data.vehicleId != selectedVehicleId) return

        val baseVehicle = current.vehicles.firstOrNull { it.vehicleId == selectedVehicleId }
        val oldState = current.selectedState

        val mergedState = VehicleStateResponse(
            vehicleId = data.vehicleId ?: selectedVehicleId,
            name = data.name ?: oldState?.name ?: baseVehicle?.name ?: selectedVehicleId,
            brand = oldState?.brand ?: baseVehicle?.brand,
            model = oldState?.model ?: baseVehicle?.model,
            onlineStatus = data.onlineStatus ?: oldState?.onlineStatus ?: baseVehicle?.onlineStatus,
            lockStatus = data.lockStatus ?: oldState?.lockStatus ?: baseVehicle?.lockStatus,
            engineStatus = data.engineStatus ?: oldState?.engineStatus ?: baseVehicle?.engineStatus,
            hvacStatus = data.hvacStatus ?: oldState?.hvacStatus ?: baseVehicle?.hvacStatus,
            windowStatus = data.windowStatus ?: oldState?.windowStatus ?: baseVehicle?.windowStatus,
            mileage = data.mileage ?: oldState?.mileage ?: baseVehicle?.mileage,
            fuelLevel = data.fuelLevel ?: oldState?.fuelLevel ?: baseVehicle?.fuelLevel,
            updatedTime = data.updatedTime ?: oldState?.updatedTime ?: baseVehicle?.updatedTime
        )

        val mergedVehicles = current.vehicles.map { vehicle ->
            if (vehicle.vehicleId == selectedVehicleId) {
                vehicle.copy(
                    name = (data.name ?: vehicle.name).ifBlank { vehicle.vehicleId },
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
            isRefreshingCurrent = false,
            vehicles = mergedVehicles,
            selectedState = mergedState,
            errorMessage = null
        )
    }

    private fun mergeVehicleListWithSelectedState(
        vehicles: List<Vehicle>,
        selectedVehicleId: String,
        selectedState: VehicleStateResponse
    ): List<Vehicle> {
        return vehicles.map { vehicle ->
            if (vehicle.vehicleId == selectedVehicleId) {
                vehicle.copy(
                    name = selectedState.name.ifBlank { vehicle.vehicleId },
                    brand = selectedState.brand ?: vehicle.brand,
                    model = selectedState.model ?: vehicle.model,
                    onlineStatus = selectedState.onlineStatus ?: vehicle.onlineStatus,
                    lockStatus = selectedState.lockStatus ?: vehicle.lockStatus,
                    engineStatus = selectedState.engineStatus ?: vehicle.engineStatus,
                    hvacStatus = selectedState.hvacStatus ?: vehicle.hvacStatus,
                    windowStatus = selectedState.windowStatus ?: vehicle.windowStatus,
                    mileage = selectedState.mileage ?: vehicle.mileage,
                    fuelLevel = selectedState.fuelLevel ?: vehicle.fuelLevel,
                    updatedTime = selectedState.updatedTime ?: vehicle.updatedTime
                )
            } else {
                vehicle
            }
        }
    }

    private fun fallbackStateFromVehicle(vehicle: Vehicle): VehicleStateResponse {
        return VehicleStateResponse(
            vehicleId = vehicle.vehicleId,
            name = vehicle.name.ifBlank { vehicle.vehicleId },
            brand = vehicle.brand,
            model = vehicle.model,
            onlineStatus = vehicle.onlineStatus,
            lockStatus = vehicle.lockStatus,
            engineStatus = vehicle.engineStatus,
            hvacStatus = vehicle.hvacStatus,
            windowStatus = vehicle.windowStatus,
            mileage = vehicle.mileage,
            fuelLevel = vehicle.fuelLevel,
            updatedTime = vehicle.updatedTime
        )
    }

    class Factory(
        private val vehicleRepository: VehicleRepository,
        private val commandRepository: CommandRepository,
        private val tokenStore: TokenStore
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(vehicleRepository, commandRepository, tokenStore) as T
        }
    }
}