package com.example.app.ui.control

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.app.R
import com.example.app.data.local.SelectedVehicleStore
import com.example.app.data.repository.CommandRepository
import com.example.app.data.repository.RealtimeRepository
import com.example.app.di.NetworkModule

class ControlFragment : Fragment(R.layout.fragment_control) {

    private lateinit var tvSelectedVehicle: TextView
    private lateinit var tvWsStatus: TextView
    private lateinit var tvRealtimeState: TextView
    private lateinit var tvCommandId: TextView
    private lateinit var tvCommandType: TextView
    private lateinit var tvCommandResult: TextView
    private lateinit var tvRequestTime: TextView
    private lateinit var tvResponseTime: TextView
    private lateinit var progressBar: ProgressBar

    private lateinit var btnLockOn: Button
    private lateinit var btnLockOff: Button
    private lateinit var btnHvacOn: Button
    private lateinit var btnHvacOff: Button
    private lateinit var btnWindowOpen: Button
    private lateinit var btnWindowClose: Button
    private lateinit var btnEngineOn: Button
    private lateinit var btnEngineOff: Button
    private lateinit var btnStatusQuery: Button
    private lateinit var btnRefreshResult: Button
    private lateinit var btnPing: Button

    private lateinit var viewModel: ControlViewModel

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

        val appWebSocketClient = NetworkModule.provideAppWebSocketClient(okHttpClient)
        val realtimeRepository = RealtimeRepository(tokenStore, appWebSocketClient)

        viewModel = ViewModelProvider(
            this,
            ControlViewModel.Factory(commandRepository, realtimeRepository)
        )[ControlViewModel::class.java]

        initViews(view)
        initListeners()
        observeUiState()
    }

    private fun initViews(view: View) {
        tvSelectedVehicle = view.findViewById(R.id.tvSelectedVehicle)
        tvWsStatus = view.findViewById(R.id.tvWsStatus)
        tvRealtimeState = view.findViewById(R.id.tvRealtimeState)
        tvCommandId = view.findViewById(R.id.tvCommandId)
        tvCommandType = view.findViewById(R.id.tvCommandType)
        tvCommandResult = view.findViewById(R.id.tvCommandResult)
        tvRequestTime = view.findViewById(R.id.tvRequestTime)
        tvResponseTime = view.findViewById(R.id.tvResponseTime)
        progressBar = view.findViewById(R.id.progressBar)

        btnLockOn = view.findViewById(R.id.btnLockOn)
        btnLockOff = view.findViewById(R.id.btnLockOff)
        btnHvacOn = view.findViewById(R.id.btnHvacOn)
        btnHvacOff = view.findViewById(R.id.btnHvacOff)
        btnWindowOpen = view.findViewById(R.id.btnWindowOpen)
        btnWindowClose = view.findViewById(R.id.btnWindowClose)
        btnEngineOn = view.findViewById(R.id.btnEngineOn)
        btnEngineOff = view.findViewById(R.id.btnEngineOff)
        btnStatusQuery = view.findViewById(R.id.btnStatusQuery)
        btnRefreshResult = view.findViewById(R.id.btnRefreshResult)
        btnPing = view.findViewById(R.id.btnPing)
    }

    private fun initListeners() {
        btnLockOn.setOnClickListener { viewModel.sendCommand("LOCK_ON") }
        btnLockOff.setOnClickListener { viewModel.sendCommand("LOCK_OFF") }
        btnHvacOn.setOnClickListener { viewModel.sendCommand("HVAC_ON") }
        btnHvacOff.setOnClickListener { viewModel.sendCommand("HVAC_OFF") }
        btnWindowOpen.setOnClickListener { viewModel.sendCommand("WINDOW_OPEN") }
        btnWindowClose.setOnClickListener { viewModel.sendCommand("WINDOW_CLOSE") }
        btnEngineOn.setOnClickListener { viewModel.sendCommand("ENGINE_ON") }
        btnEngineOff.setOnClickListener { viewModel.sendCommand("ENGINE_OFF") }
        btnStatusQuery.setOnClickListener { viewModel.sendCommand("STATUS_QUERY") }
        btnRefreshResult.setOnClickListener { viewModel.refreshLastCommandResult() }
        btnPing.setOnClickListener { viewModel.sendPing() }
    }

    private fun observeUiState() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            val loading = state.isLoading || state.isPolling
            progressBar.visibility = if (loading) View.VISIBLE else View.GONE

            tvSelectedVehicle.text = "当前车辆：${state.selectedVehicleId ?: "未选择"}"
            tvWsStatus.text = "实时通道：${if (state.wsConnected) "已连接" else "未连接"}"
            tvRealtimeState.text = "最近收到的实时状态：${state.latestVehicleStateText ?: "-"}"

            tvCommandId.text = "commandId：${state.lastCommandId ?: "-"}"
            tvCommandType.text = "命令类型：${state.lastCommandType ?: "-"}"
            tvCommandResult.text = "命令结果：${state.lastCommandResult ?: "-"}"
            tvRequestTime.text = "请求时间：${state.lastRequestTime ?: "-"}"
            tvResponseTime.text = "响应时间：${state.lastResponseTime ?: "-"}"

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
}