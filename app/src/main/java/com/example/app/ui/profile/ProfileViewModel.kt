package com.example.app.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.app.common.ResultState
import com.example.app.data.local.SelectedVehicleStore
import com.example.app.data.repository.AuthRepository
import com.example.app.data.repository.VehicleRepository
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val vehicleRepository: VehicleRepository,
    private val selectedVehicleStore: SelectedVehicleStore
) : ViewModel() {

    private val _uiState = MutableLiveData(ProfileUiState(isLoading = true))
    val uiState: LiveData<ProfileUiState> = _uiState

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            val current = _uiState.value ?: ProfileUiState()
            _uiState.value = current.copy(
                isLoading = true,
                infoMessage = null,
                errorMessage = null
            )

            val selectedVehicleId = selectedVehicleStore.getSelectedVehicleId().orEmpty()

            var selectedVehicleName = ""
            when (val vehicleResult = vehicleRepository.getVehicles()) {
                is ResultState.Success -> {
                    selectedVehicleName = vehicleResult.data
                        .firstOrNull { it.vehicleId == selectedVehicleId }
                        ?.name
                        .orEmpty()
                }

                is ResultState.Error -> {
                    // 车辆信息这里只是辅助显示，不阻断“我的”页
                }

                ResultState.Loading -> {
                }
            }

            when (val userResult = authRepository.getCurrentUser()) {
                is ResultState.Success -> {
                    val user = userResult.data
                    _uiState.value = ProfileUiState(
                        isLoading = false,
                        userId = user.userId,
                        username = user.username.orEmpty(),
                        nickname = user.nickname.orEmpty(),
                        phone = user.phone.orEmpty(),
                        role = user.role.orEmpty(),
                        selectedVehicleId = selectedVehicleId,
                        selectedVehicleName = selectedVehicleName
                    )
                }

                is ResultState.Error -> {
                    _uiState.value = current.copy(
                        isLoading = false,
                        selectedVehicleId = selectedVehicleId,
                        selectedVehicleName = selectedVehicleName,
                        errorMessage = userResult.message
                    )
                }

                ResultState.Loading -> {
                    _uiState.value = current.copy(isLoading = true)
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            selectedVehicleStore.clear()
            _uiState.value = (_uiState.value ?: ProfileUiState()).copy(
                loggedOut = true
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
        private val authRepository: AuthRepository,
        private val vehicleRepository: VehicleRepository,
        private val selectedVehicleStore: SelectedVehicleStore
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProfileViewModel(
                authRepository = authRepository,
                vehicleRepository = vehicleRepository,
                selectedVehicleStore = selectedVehicleStore
            ) as T
        }
    }
}