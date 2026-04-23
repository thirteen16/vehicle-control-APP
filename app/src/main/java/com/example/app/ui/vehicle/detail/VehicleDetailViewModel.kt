package com.example.app.ui.vehicle.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.app.common.ResultState
import com.example.app.data.repository.VehicleRepository
import kotlinx.coroutines.launch

class VehicleDetailViewModel(
    private val vehicleRepository: VehicleRepository
) : ViewModel() {

    private val _uiState = MutableLiveData(VehicleDetailUiState())
    val uiState: LiveData<VehicleDetailUiState> = _uiState

    fun loadVehicleDetail(vehicleId: String) {
        if (vehicleId.isBlank()) {
            _uiState.value = VehicleDetailUiState(
                errorMessage = "vehicleId 不能为空"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = (_uiState.value ?: VehicleDetailUiState()).copy(
                isLoading = true,
                vehicleId = vehicleId,
                infoMessage = null,
                errorMessage = null
            )

            when (val result = vehicleRepository.getVehicleState(vehicleId)) {
                is ResultState.Success -> {
                    val selectedVehicleId = vehicleRepository.getSelectedVehicleId()
                    _uiState.value = VehicleDetailUiState(
                        isLoading = false,
                        vehicleId = vehicleId,
                        vehicleState = result.data,
                        isCurrentVehicle = selectedVehicleId == vehicleId
                    )
                }

                is ResultState.Error -> {
                    _uiState.value = (_uiState.value ?: VehicleDetailUiState()).copy(
                        isLoading = false,
                        vehicleId = vehicleId,
                        errorMessage = result.message
                    )
                }

                ResultState.Loading -> {
                    _uiState.value = (_uiState.value ?: VehicleDetailUiState()).copy(
                        isLoading = true
                    )
                }
            }
        }
    }

    fun setAsCurrentVehicle() {
        val current = _uiState.value ?: return
        val vehicleId = current.vehicleId
        if (vehicleId.isBlank()) return
        if (current.isCurrentVehicle) {
            _uiState.value = current.copy(infoMessage = "当前已是该车辆")
            return
        }

        viewModelScope.launch {
            vehicleRepository.saveSelectedVehicleId(vehicleId)
            val vehicleName = current.vehicleState?.name?.ifBlank { vehicleId } ?: vehicleId
            _uiState.value = current.copy(
                isCurrentVehicle = true,
                infoMessage = "已设为当前车辆：$vehicleName",
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
            return VehicleDetailViewModel(vehicleRepository) as T
        }
    }
}