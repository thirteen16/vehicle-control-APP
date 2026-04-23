package com.example.app.ui.vehicle.location

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.app.common.ResultState
import com.example.app.data.repository.VehicleRepository
import kotlinx.coroutines.launch

class VehicleLocationViewModel(
    private val vehicleRepository: VehicleRepository
) : ViewModel() {

    private val _uiState = MutableLiveData(VehicleLocationUiState())
    val uiState: LiveData<VehicleLocationUiState> = _uiState

    fun loadVehicleLocation(vehicleId: String) {
        if (vehicleId.isBlank()) {
            _uiState.value = VehicleLocationUiState(
                errorMessage = "vehicleId 不能为空"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = (_uiState.value ?: VehicleLocationUiState()).copy(
                isLoading = true,
                vehicleId = vehicleId,
                errorMessage = null
            )

            when (val result = vehicleRepository.getVehicleLocation(vehicleId)) {
                is ResultState.Success -> {
                    _uiState.value = VehicleLocationUiState(
                        isLoading = false,
                        vehicleId = vehicleId,
                        location = result.data
                    )
                }

                is ResultState.Error -> {
                    _uiState.value = (_uiState.value ?: VehicleLocationUiState()).copy(
                        isLoading = false,
                        vehicleId = vehicleId,
                        errorMessage = result.message
                    )
                }

                ResultState.Loading -> {
                    _uiState.value = (_uiState.value ?: VehicleLocationUiState()).copy(
                        isLoading = true
                    )
                }
            }
        }
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
            return VehicleLocationViewModel(vehicleRepository) as T
        }
    }
}