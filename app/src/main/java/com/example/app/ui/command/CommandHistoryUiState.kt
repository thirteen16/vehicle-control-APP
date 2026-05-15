package com.example.app.ui.command

import com.example.app.data.model.response.CommandHistoryItemResponse

data class CommandHistoryUiState(
    val isLoading: Boolean = false,
    val selectedVehicleId: String? = null,
    val isAllHistory: Boolean = true,
    val timeFilter: CommandTimeFilter = CommandTimeFilter.ALL_TIME,
    val resultFilter: CommandResultFilter = CommandResultFilter.ALL,
    val items: List<CommandHistoryItemResponse> = emptyList(),
    val totalLoadedCount: Int = 0,
    val infoMessage: String? = null,
    val errorMessage: String? = null
)