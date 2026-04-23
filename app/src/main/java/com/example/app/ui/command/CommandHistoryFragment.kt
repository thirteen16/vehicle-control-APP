package com.example.app.ui.command

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.app.R
import com.example.app.common.UiStateTextResolver
import com.example.app.data.local.SelectedVehicleStore
import com.example.app.data.model.response.CommandHistoryItemResponse
import com.example.app.data.repository.CommandRepository
import com.example.app.di.NetworkModule

class CommandHistoryFragment : Fragment(R.layout.fragment_command_history) {

    private lateinit var progressBar: ProgressBar
    private lateinit var tvStateBanner: TextView
    private lateinit var tvCurrentFilter: TextView
    private lateinit var tvResultFilter: TextView
    private lateinit var tvTotalCount: TextView
    private lateinit var tvEmptyTip: TextView
    private lateinit var historyContainer: LinearLayout

    private lateinit var btnRefreshCurrent: Button
    private lateinit var btnLoadAll: Button
    private lateinit var btnFilterAll: Button
    private lateinit var btnFilterSuccess: Button
    private lateinit var btnFilterFailed: Button
    private lateinit var btnFilterPending: Button

    private lateinit var viewModel: CommandHistoryViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val appContext = requireContext().applicationContext
        val tokenStore = NetworkModule.provideTokenStore(appContext)
        val authInterceptor = NetworkModule.provideAuthInterceptor(tokenStore)
        val loggingInterceptor = NetworkModule.provideLoggingInterceptor()
        val okHttpClient = NetworkModule.provideOkHttpClient(authInterceptor, loggingInterceptor)
        val retrofit = NetworkModule.provideRetrofit(okHttpClient)
        val commandApi = NetworkModule.provideCommandApi(retrofit)
        val selectedVehicleStore = SelectedVehicleStore(appContext)
        val commandRepository = CommandRepository(commandApi, selectedVehicleStore)

        viewModel = ViewModelProvider(
            this,
            CommandHistoryViewModel.Factory(commandRepository)
        )[CommandHistoryViewModel::class.java]

        initViews(view)
        initListeners()
        observeUiState()
    }

    private fun initViews(view: View) {
        progressBar = view.findViewById(R.id.progressBar)
        tvStateBanner = view.findViewById(R.id.tvStateBanner)
        tvCurrentFilter = view.findViewById(R.id.tvCurrentFilter)
        tvResultFilter = view.findViewById(R.id.tvResultFilter)
        tvTotalCount = view.findViewById(R.id.tvTotalCount)
        tvEmptyTip = view.findViewById(R.id.tvEmptyTip)
        historyContainer = view.findViewById(R.id.historyContainer)

        btnRefreshCurrent = view.findViewById(R.id.btnRefreshCurrent)
        btnLoadAll = view.findViewById(R.id.btnLoadAll)
        btnFilterAll = view.findViewById(R.id.btnFilterAll)
        btnFilterSuccess = view.findViewById(R.id.btnFilterSuccess)
        btnFilterFailed = view.findViewById(R.id.btnFilterFailed)
        btnFilterPending = view.findViewById(R.id.btnFilterPending)
    }

    private fun initListeners() {
        btnRefreshCurrent.setOnClickListener {
            viewModel.loadCurrentVehicleHistory()
        }

        btnLoadAll.setOnClickListener {
            viewModel.loadAllHistory()
        }

        btnFilterAll.setOnClickListener {
            viewModel.setResultFilter(CommandResultFilter.ALL)
        }

        btnFilterSuccess.setOnClickListener {
            viewModel.setResultFilter(CommandResultFilter.SUCCESS)
        }

        btnFilterFailed.setOnClickListener {
            viewModel.setResultFilter(CommandResultFilter.FAILED)
        }

        btnFilterPending.setOnClickListener {
            viewModel.setResultFilter(CommandResultFilter.PENDING)
        }
    }

    private fun observeUiState() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

            tvCurrentFilter.text = if (state.isAllHistory) {
                "车辆范围：全部车辆"
            } else {
                "车辆范围：${state.selectedVehicleId ?: "未选择车辆"}"
            }

            tvResultFilter.text = "状态筛选：${displayFilterText(state.resultFilter)}"
            tvTotalCount.text = "当前显示：${state.items.size} / 已加载：${state.totalLoadedCount}"

            renderFilterButtons(state.resultFilter)
            renderStateBanner(state)

            tvEmptyTip.visibility = View.GONE
            historyContainer.visibility = if (state.items.isEmpty()) View.GONE else View.VISIBLE

            renderHistoryList(state.items)
        }
    }

    private fun renderStateBanner(state: CommandHistoryUiState) {
        when {
            !state.errorMessage.isNullOrBlank() -> {
                tvStateBanner.visibility = View.VISIBLE
                tvStateBanner.text = "加载失败：${UiStateTextResolver.resolveError(state.errorMessage)}"
                tvStateBanner.setBackgroundResource(R.drawable.bg_notice_error)
            }

            state.items.isEmpty() -> {
                tvStateBanner.visibility = View.VISIBLE
                tvStateBanner.text = UiStateTextResolver.historyEmptyMessage(
                    isAllHistory = state.isAllHistory,
                    resultFilterText = displayFilterText(state.resultFilter)
                )
                tvStateBanner.setBackgroundResource(R.drawable.bg_notice_empty)
            }

            !state.infoMessage.isNullOrBlank() -> {
                tvStateBanner.visibility = View.VISIBLE
                tvStateBanner.text = state.infoMessage
                tvStateBanner.setBackgroundResource(R.drawable.bg_notice_info)
            }

            else -> {
                tvStateBanner.visibility = View.GONE
            }
        }
    }

    private fun renderFilterButtons(filter: CommandResultFilter) {
        renderSingleFilterButton(btnFilterAll, filter == CommandResultFilter.ALL)
        renderSingleFilterButton(btnFilterSuccess, filter == CommandResultFilter.SUCCESS)
        renderSingleFilterButton(btnFilterFailed, filter == CommandResultFilter.FAILED)
        renderSingleFilterButton(btnFilterPending, filter == CommandResultFilter.PENDING)
    }

    private fun renderSingleFilterButton(button: Button, selected: Boolean) {
        button.isEnabled = !selected
        button.alpha = if (selected) 1f else 0.65f
    }

    private fun renderHistoryList(items: List<CommandHistoryItemResponse>) {
        historyContainer.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())

        items.forEach { item ->
            val itemView = inflater.inflate(R.layout.item_command_history, historyContainer, false)

            val root = itemView.findViewById<View>(R.id.rootHistoryItem)
            val tvType = itemView.findViewById<TextView>(R.id.tvType)
            val tvResultBadge = itemView.findViewById<TextView>(R.id.tvResultBadge)
            val tvVehicleId = itemView.findViewById<TextView>(R.id.tvVehicleId)
            val tvCommandId = itemView.findViewById<TextView>(R.id.tvCommandId)
            val tvTime = itemView.findViewById<TextView>(R.id.tvTime)
            val tvDetailHint = itemView.findViewById<TextView>(R.id.tvDetailHint)

            tvType.text = item.type ?: "-"
            tvResultBadge.text = (item.result ?: "-").uppercase()
            tvVehicleId.text = "车辆：${item.vehicleId ?: "-"}"
            tvCommandId.text = "commandId：${item.commandId ?: "-"}"
            tvTime.text = "请求：${item.requestTime ?: "-"}\n响应：${item.responseTime ?: "-"}"
            tvDetailHint.text = "点击查看完整请求/响应详情"

            applyResultBadgeStyle(tvResultBadge, item.result)

            root.setOnClickListener {
                CommandResultDialog.newInstance(item)
                    .show(childFragmentManager, "command_result_dialog")
            }

            historyContainer.addView(itemView)
        }
    }

    private fun applyResultBadgeStyle(textView: TextView, result: String?) {
        val bgRes = when {
            result.equals("SUCCESS", ignoreCase = true) -> R.drawable.bg_status_success
            result.equals("FAILED", ignoreCase = true) -> R.drawable.bg_status_failed
            result.equals("PENDING", ignoreCase = true) -> R.drawable.bg_status_pending
            else -> R.drawable.bg_status_pending
        }
        textView.background = ContextCompat.getDrawable(requireContext(), bgRes)
    }

    private fun displayFilterText(filter: CommandResultFilter): String {
        return when (filter) {
            CommandResultFilter.ALL -> "全部"
            CommandResultFilter.SUCCESS -> "SUCCESS"
            CommandResultFilter.FAILED -> "FAILED"
            CommandResultFilter.PENDING -> "PENDING"
        }
    }
}