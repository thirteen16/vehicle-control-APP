package com.example.app.ui.vehicle.list

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.app.common.ResultState
import com.example.app.data.repository.VehicleRepository
import kotlinx.coroutines.launch

class VehicleListViewModel(
    private val vehicleRepository: VehicleRepository
) : ViewModel() {

    private val _uiState = MutableLiveData(VehicleListUiState(isLoading = true))
    val uiState: LiveData<VehicleListUiState> = _uiState

    init {
        loadVehicles()
    }

    fun loadVehicles() {
        viewModelScope.launch {
            val current = _uiState.value ?: VehicleListUiState()
            _uiState.value = current.copy(
                isLoading = true,
                infoMessage = null,
                errorMessage = null
            )

            when (val result = vehicleRepository.getVehicles()) {
                is ResultState.Success -> {
                    val vehicles = result.data
                    val savedVehicleId = vehicleRepository.getSelectedVehicleId()

                    val finalSelectedVehicleId = when {
                        vehicles.isEmpty() -> null
                        !savedVehicleId.isNullOrBlank() &&
                                vehicles.any { it.vehicleId == savedVehicleId } -> savedVehicleId
                        else -> vehicles.first().vehicleId
                    }

                    if (!finalSelectedVehicleId.isNullOrBlank()) {
                        vehicleRepository.saveSelectedVehicleId(finalSelectedVehicleId)
                    }

                    _uiState.value = VehicleListUiState(
                        isLoading = false,
                        vehicles = vehicles,
                        selectedVehicleId = finalSelectedVehicleId
                    )
                }

                is ResultState.Error -> {
                    _uiState.value = current.copy(
                        isLoading = false,
                        errorMessage = result.message
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
        if (vehicleId.isBlank()) return
        if (current.selectedVehicleId == vehicleId) {
            _uiState.value = current.copy(infoMessage = "当前已是该车辆")
            return
        }

        viewModelScope.launch {
            vehicleRepository.saveSelectedVehicleId(vehicleId)
            val selectedVehicle = current.vehicles.firstOrNull { it.vehicleId == vehicleId }

            _uiState.value = current.copy(
                selectedVehicleId = vehicleId,
                infoMessage = "已切换当前车辆：${selectedVehicle?.name?.ifBlank { vehicleId } ?: vehicleId}",
                errorMessage = null
            )
        }
    }

    fun clearInfoMessage() {
        val current = _uiState.value ?: return
        _uiState.value = current.copy(infoMessage = null)
    }

    fun clearErrorMessage() {
        val current = _uiState.value ?: return
        _uiState.value = current.copy(errorMessage = null)
    }

    class Factory(
        private val vehicleRepository: VehicleRepository
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return VehicleListViewModel(vehicleRepository) as T
        }
    }
}