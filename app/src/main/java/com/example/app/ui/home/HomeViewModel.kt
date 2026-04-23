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

    fun logout() {
        viewModelScope.launch {
            tokenStore.clearLoginSession()
            _uiState.value = _uiState.value?.copy(loggedOut = true)
        }
    }

    private suspend fun loadSelectedVehicleState(
        username: String,
        vehicles: List<Vehicle>,
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
                _uiState.value = _uiState.value?.copy(isLoading = true)
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