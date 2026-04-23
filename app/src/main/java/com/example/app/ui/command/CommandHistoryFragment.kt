package com.example.app.ui.command

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.app.R
import com.example.app.data.local.SelectedVehicleStore
import com.example.app.data.repository.CommandRepository
import com.example.app.di.NetworkModule

class CommandHistoryFragment : Fragment(R.layout.fragment_command_history) {

    private lateinit var progressBar: ProgressBar
    private lateinit var tvCurrentFilter: TextView
    private lateinit var tvTotalCount: TextView
    private lateinit var tvEmptyTip: TextView
    private lateinit var historyContainer: LinearLayout
    private lateinit var btnRefreshCurrent: Button
    private lateinit var btnLoadAll: Button

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
        tvCurrentFilter = view.findViewById(R.id.tvCurrentFilter)
        tvTotalCount = view.findViewById(R.id.tvTotalCount)
        tvEmptyTip = view.findViewById(R.id.tvEmptyTip)
        historyContainer = view.findViewById(R.id.historyContainer)
        btnRefreshCurrent = view.findViewById(R.id.btnRefreshCurrent)
        btnLoadAll = view.findViewById(R.id.btnLoadAll)
    }

    private fun initListeners() {
        btnRefreshCurrent.setOnClickListener {
            viewModel.loadCurrentVehicleHistory()
        }

        btnLoadAll.setOnClickListener {
            viewModel.loadAllHistory()
        }
    }

    private fun observeUiState() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

            tvCurrentFilter.text = if (state.isAllHistory) {
                "当前筛选：全部车辆"
            } else {
                "当前筛选：${state.selectedVehicleId ?: "未选择车辆"}"
            }

            tvTotalCount.text = "历史数量：${state.items.size}"

            tvEmptyTip.visibility = if (state.items.isEmpty()) View.VISIBLE else View.GONE
            historyContainer.visibility = if (state.items.isEmpty()) View.GONE else View.VISIBLE

            renderHistoryList(state)

            state.infoMessage?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearInfoMessage()
            }

            state.errorMessage?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearErrorMessage()
            }
        }
    }

    private fun renderHistoryList(state: CommandHistoryUiState) {
        historyContainer.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())

        state.items.forEach { item ->
            val itemView = inflater.inflate(R.layout.item_command_history, historyContainer, false)

            val root = itemView.findViewById<View>(R.id.rootHistoryItem)
            val tvType = itemView.findViewById<TextView>(R.id.tvType)
            val tvResult = itemView.findViewById<TextView>(R.id.tvResult)
            val tvVehicleId = itemView.findViewById<TextView>(R.id.tvVehicleId)
            val tvCommandId = itemView.findViewById<TextView>(R.id.tvCommandId)
            val tvTime = itemView.findViewById<TextView>(R.id.tvTime)
            val tvDetailHint = itemView.findViewById<TextView>(R.id.tvDetailHint)

            tvType.text = item.type ?: "-"
            tvResult.text = "结果：${item.result ?: "-"}"
            tvVehicleId.text = "车辆：${item.vehicleId ?: "-"}"
            tvCommandId.text = "commandId：${item.commandId ?: "-"}"
            tvTime.text = "请求：${item.requestTime ?: "-"}\n响应：${item.responseTime ?: "-"}"
            tvDetailHint.text = "点击查看完整请求/响应详情"

            root.setOnClickListener {
                CommandResultDialog.newInstance(item)
                    .show(childFragmentManager, "command_result_dialog")
            }

            historyContainer.addView(itemView)
        }
    }
}