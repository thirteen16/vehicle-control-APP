package com.example.app.ui.command

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.app.common.ResultState
import com.example.app.data.model.response.CommandHistoryItemResponse
import com.example.app.data.repository.CommandRepository
import kotlinx.coroutines.launch
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CommandHistoryViewModel(
    private val commandRepository: CommandRepository
) : ViewModel() {

    private val _uiState = MutableLiveData(CommandHistoryUiState(isLoading = true))
    val uiState: LiveData<CommandHistoryUiState> = _uiState

    private var sourceItems: List<CommandHistoryItemResponse> = emptyList()

    init {
        loadDefaultHistory()
    }

    fun loadDefaultHistory() {
        loadAllHistory()
    }

    fun loadCurrentVehicleHistory() {
        viewModelScope.launch {
            val selectedVehicleId = commandRepository.getSelectedVehicleId()
            if (selectedVehicleId.isNullOrBlank()) {
                performLoad(
                    vehicleId = null,
                    isAllHistory = true,
                    infoMessage = null
                )
            } else {
                performLoad(
                    vehicleId = selectedVehicleId,
                    isAllHistory = false,
                    infoMessage = null
                )
            }
        }
    }

    fun loadAllHistory() {
        viewModelScope.launch {
            performLoad(
                vehicleId = null,
                isAllHistory = true,
                infoMessage = null
            )
        }
    }

    fun setTimeFilter(filter: CommandTimeFilter) {
        val current = _uiState.value ?: CommandHistoryUiState()

        val filtered = applyFilters(
            list = sourceItems,
            timeFilter = filter,
            resultFilter = current.resultFilter
        )

        _uiState.value = current.copy(
            timeFilter = filter,
            items = filtered,
            totalLoadedCount = sourceItems.size,
            infoMessage = null,
            errorMessage = null
        )
    }

    fun setResultFilter(filter: CommandResultFilter) {
        val current = _uiState.value ?: CommandHistoryUiState()

        val filtered = applyFilters(
            list = sourceItems,
            timeFilter = current.timeFilter,
            resultFilter = filter
        )

        _uiState.value = current.copy(
            resultFilter = filter,
            items = filtered,
            totalLoadedCount = sourceItems.size,
            infoMessage = null,
            errorMessage = null
        )
    }

    private suspend fun performLoad(
        vehicleId: String?,
        isAllHistory: Boolean,
        infoMessage: String?
    ) {
        val currentState = _uiState.value ?: CommandHistoryUiState()

        _uiState.value = currentState.copy(
            isLoading = true,
            selectedVehicleId = vehicleId,
            isAllHistory = isAllHistory,
            infoMessage = null,
            errorMessage = null
        )

        when (val result = commandRepository.getCommandHistory(vehicleId, 50)) {
            is ResultState.Success -> {
                sourceItems = result.data

                val filtered = applyFilters(
                    list = sourceItems,
                    timeFilter = currentState.timeFilter,
                    resultFilter = currentState.resultFilter
                )

                _uiState.value = currentState.copy(
                    isLoading = false,
                    selectedVehicleId = vehicleId,
                    isAllHistory = isAllHistory,
                    items = filtered,
                    totalLoadedCount = sourceItems.size,
                    infoMessage = infoMessage,
                    errorMessage = null
                )
            }

            is ResultState.Error -> {
                _uiState.value = currentState.copy(
                    isLoading = false,
                    selectedVehicleId = vehicleId,
                    isAllHistory = isAllHistory,
                    errorMessage = result.message
                )
            }

            ResultState.Loading -> {
                _uiState.value = currentState.copy(isLoading = true)
            }
        }
    }

    private fun applyFilters(
        list: List<CommandHistoryItemResponse>,
        timeFilter: CommandTimeFilter,
        resultFilter: CommandResultFilter
    ): List<CommandHistoryItemResponse> {
        return list
            .filter { matchTimeFilter(it, timeFilter) }
            .filter { matchResultFilter(it, resultFilter) }
    }

    private fun matchResultFilter(
        item: CommandHistoryItemResponse,
        filter: CommandResultFilter
    ): Boolean {
        return when (filter) {
            CommandResultFilter.ALL -> true

            CommandResultFilter.SUCCESS -> {
                item.result.equals("SUCCESS", ignoreCase = true)
            }

            CommandResultFilter.TIMEOUT -> {
                !item.result.equals("SUCCESS", ignoreCase = true)
            }
        }
    }

    private fun matchTimeFilter(
        item: CommandHistoryItemResponse,
        filter: CommandTimeFilter
    ): Boolean {
        if (filter == CommandTimeFilter.ALL_TIME) {
            return true
        }

        val timeText = historyTimeText(item) ?: return false
        val itemTime = parseTimeMillis(timeText) ?: return false

        val calendar = Calendar.getInstance()

        when (filter) {
            CommandTimeFilter.LAST_DAY -> calendar.add(Calendar.DAY_OF_YEAR, -1)
            CommandTimeFilter.LAST_WEEK -> calendar.add(Calendar.DAY_OF_YEAR, -7)
            CommandTimeFilter.HALF_YEAR -> calendar.add(Calendar.MONTH, -6)
            CommandTimeFilter.ONE_YEAR -> calendar.add(Calendar.YEAR, -1)
            CommandTimeFilter.ALL_TIME -> return true
        }

        return itemTime >= calendar.timeInMillis
    }

    private fun historyTimeText(item: CommandHistoryItemResponse): String? {
        return listOf(
            item.requestTime,
            item.createdTime,
            item.responseTime,
            item.updatedTime
        ).firstOrNull { !it.isNullOrBlank() }
    }

    private fun parseTimeMillis(raw: String?): Long? {
        if (raw.isNullOrBlank()) {
            return null
        }

        val text = raw.trim()

        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy/MM/dd HH:mm:ss",
            "yyyy/MM/dd HH:mm",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm",
            "yyyy-MM-dd",
            "yyyy/MM/dd"
        )

        for (pattern in patterns) {
            try {
                val formatter = SimpleDateFormat(pattern, Locale.getDefault()).apply {
                    isLenient = false
                }

                val position = ParsePosition(0)
                val date = formatter.parse(text, position)

                if (date != null && position.index == text.length) {
                    return date.time
                }
            } catch (_: Exception) {
            }
        }

        return null
    }

    fun clearInfoMessage() {
        _uiState.value = (_uiState.value ?: CommandHistoryUiState()).copy(
            infoMessage = null
        )
    }

    fun clearErrorMessage() {
        _uiState.value = (_uiState.value ?: CommandHistoryUiState()).copy(
            errorMessage = null
        )
    }

    class Factory(
        private val commandRepository: CommandRepository
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CommandHistoryViewModel(commandRepository) as T
        }
    }
}