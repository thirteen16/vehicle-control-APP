package com.example.app.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.app.data.local.TokenStore
import kotlinx.coroutines.launch

class HomeViewModel(
    private val tokenStore: TokenStore
) : ViewModel() {

    private val _uiState = MutableLiveData(HomeUiState(isLoading = true))
    val uiState: LiveData<HomeUiState> = _uiState

    init {
        loadUserInfo()
    }

    fun loadUserInfo() {
        viewModelScope.launch {
            try {
                val username = tokenStore.getUsername() ?: "用户"
                _uiState.value = HomeUiState(
                    isLoading = false,
                    username = username
                )
            } catch (e: Exception) {
                _uiState.value = HomeUiState(
                    isLoading = false,
                    username = "用户",
                    errorMessage = e.message ?: "加载用户信息失败"
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            tokenStore.clearLoginSession()
            _uiState.value = _uiState.value?.copy(loggedOut = true)
        }
    }

    class Factory(
        private val tokenStore: TokenStore
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(tokenStore) as T
        }
    }
}