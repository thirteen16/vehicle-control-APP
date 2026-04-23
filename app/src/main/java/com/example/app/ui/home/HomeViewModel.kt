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
            val current = _uiState.value ?: HomeUiState()

            _uiState.value = current.copy(
                isLoading = true,
                errorMessage = null
            )

            when (val vehicleResult = vehicleRepository.getVehicles()) {
                is ResultState.Success -> {
                    val vehicles = vehicleResult.data

                    if (vehicles.isEmpty()) {
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
                    val selectedVehicle = vehicles.firstOrNull { it.vehicleId == savedVehicleId }
                        ?: vehicles.first()

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
                                selectedState = VehicleStateResponse(
                                    vehicleId = selectedVehicle.vehicleId,
                                    name = selectedVehicle.name.ifBlank { selectedVehicle.vehicleId },
                                    brand = selectedVehicle.brand,
                                    model = selectedVehicle.model,
                                    onlineStatus = selectedVehicle.onlineStatus,
                                    lockStatus = selectedVehicle.lockStatus,
                                    engineStatus = selectedVehicle.engineStatus,
                                    hvacStatus = selectedVehicle.hvacStatus,
                                    windowStatus = selectedVehicle.windowStatus,
                                    mileage = selectedVehicle.mileage,
                                    fuelLevel = selectedVehicle.fuelLevel,
                                    updatedTime = selectedVehicle.updatedTime
                                ),
                                errorMessage = stateResult.message
                            )
                        }

                        ResultState.Loading -> {
                            _uiState.value = current.copy(isLoading = true)
                        }
                    }
                }

                is ResultState.Error -> {
                    _uiState.value = current.copy(
                        isLoading = false,
                        errorMessage = vehicleResult.message
                    )
                }

                ResultState.Loading -> {
                    _uiState.value = current.copy(isLoading = true)
                }
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
                selectedState = VehicleStateResponse(
                    vehicleId = targetVehicle.vehicleId,
                    name = targetVehicle.name.ifBlank { targetVehicle.vehicleId },
                    brand = targetVehicle.brand,
                    model = targetVehicle.model,
                    onlineStatus = targetVehicle.onlineStatus,
                    lockStatus = targetVehicle.lockStatus,
                    engineStatus = targetVehicle.engineStatus,
                    hvacStatus = targetVehicle.hvacStatus,
                    windowStatus = targetVehicle.windowStatus,
                    mileage = targetVehicle.mileage,
                    fuelLevel = targetVehicle.fuelLevel,
                    updatedTime = targetVehicle.updatedTime
                ),
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
                        selectedState = VehicleStateResponse(
                            vehicleId = targetVehicle.vehicleId,
                            name = targetVehicle.name.ifBlank { targetVehicle.vehicleId },
                            brand = targetVehicle.brand,
                            model = targetVehicle.model,
                            onlineStatus = targetVehicle.onlineStatus,
                            lockStatus = targetVehicle.lockStatus,
                            engineStatus = targetVehicle.engineStatus,
                            hvacStatus = targetVehicle.hvacStatus,
                            windowStatus = targetVehicle.windowStatus,
                            mileage = targetVehicle.mileage,
                            fuelLevel = targetVehicle.fuelLevel,
                            updatedTime = targetVehicle.updatedTime
                        ),
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
            _uiState.value = current.copy(
                isRefreshingCurrent = true,
                errorMessage = null
            )

            when (val result = vehicleRepository.getVehicleState(selectedVehicleId)) {
                is ResultState.Success -> {
                    val latest = _uiState.value ?: current
                    val mergedVehicles = mergeVehicleListWithSelectedState(
                        latest.vehicles,
                        selectedVehicleId,
                        result.data
                    )

                    _uiState.value = latest.copy(
                        isRefreshingCurrent = false,
                        vehicles = mergedVehicles,
                        selectedState = result.data,
                        errorMessage = null
                    )
                }

                is ResultState.Error -> {
                    val latest = _uiState.value ?: current
                    _uiState.value = latest.copy(
                        isRefreshingCurrent = false,
                        errorMessage = result.message
                    )
                }

                ResultState.Loading -> Unit
            }
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