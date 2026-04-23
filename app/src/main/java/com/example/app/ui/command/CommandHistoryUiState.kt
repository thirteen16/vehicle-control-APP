package com.example.app.ui.command

import com.example.app.data.model.response.CommandHistoryItemResponse

data class CommandHistoryUiState(
    val isLoading: Boolean = false,
    val selectedVehicleId: String? = null,
    val isAllHistory: Boolean = false,
    val items: List<CommandHistoryItemResponse> = emptyList(),
    val infoMessage: String? = null,
    val errorMessage: String? = null
)