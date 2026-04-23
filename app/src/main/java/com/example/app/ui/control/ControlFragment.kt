package com.example.app.ui.control

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.app.R
import com.example.app.data.local.PinStore
import com.example.app.data.local.SelectedVehicleStore
import com.example.app.data.repository.CommandRepository
import com.example.app.di.NetworkModule
import com.example.app.ui.main.AppRealtimeViewModel
import com.example.app.ui.pin.PinSetupActivity
import com.example.app.ui.pin.PinVerifyActivity

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
    private lateinit var realtimeViewModel: AppRealtimeViewModel
    private lateinit var pinStore: PinStore

    private var pendingProtectedCommand: String? = null

    private val pinSetupLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val commandType = pendingProtectedCommand
            if (result.resultCode == Activity.RESULT_OK && !commandType.isNullOrBlank()) {
                viewModel.sendCommand(commandType)
            } else if (!commandType.isNullOrBlank()) {
                Toast.makeText(requireContext(), "未完成 PIN 设置，命令已取消", Toast.LENGTH_SHORT).show()
            }
            pendingProtectedCommand = null
        }

    private val pinVerifyLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val commandType = pendingProtectedCommand
            if (result.resultCode == Activity.RESULT_OK && !commandType.isNullOrBlank()) {
                viewModel.sendCommand(commandType)
            } else if (!commandType.isNullOrBlank()) {
                Toast.makeText(requireContext(), "PIN 验证未通过，命令已取消", Toast.LENGTH_SHORT).show()
            }
            pendingProtectedCommand = null
        }

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

        pinStore = PinStore(appContext)

        viewModel = ViewModelProvider(
            this,
            ControlViewModel.Factory(commandRepository)
        )[ControlViewModel::class.java]

        realtimeViewModel = ViewModelProvider(requireActivity())[AppRealtimeViewModel::class.java]

        initViews(view)
        initListeners()
        observeUiState()
        observeRealtimeState()

        realtimeViewModel.connectIfNeeded()
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
        btnLockOn.setOnClickListener { requestProtectedCommand("LOCK_ON") }
        btnLockOff.setOnClickListener { requestProtectedCommand("LOCK_OFF") }
        btnHvacOn.setOnClickListener { requestProtectedCommand("HVAC_ON") }
        btnHvacOff.setOnClickListener { requestProtectedCommand("HVAC_OFF") }
        btnWindowOpen.setOnClickListener { requestProtectedCommand("WINDOW_OPEN") }
        btnWindowClose.setOnClickListener { requestProtectedCommand("WINDOW_CLOSE") }
        btnEngineOn.setOnClickListener { requestProtectedCommand("ENGINE_ON") }
        btnEngineOff.setOnClickListener { requestProtectedCommand("ENGINE_OFF") }

        btnStatusQuery.setOnClickListener {
            viewModel.sendCommand("STATUS_QUERY")
        }

        btnRefreshResult.setOnClickListener {
            viewModel.refreshLastCommandResult()
        }

        btnPing.setOnClickListener {
            val ok = realtimeViewModel.sendPing()
            Toast.makeText(
                requireContext(),
                if (ok) "已发送 ping" else "发送 ping 失败，请先连接实时通道",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun requestProtectedCommand(commandType: String) {
        pendingProtectedCommand = commandType

        if (!pinStore.hasPin()) {
            Toast.makeText(requireContext(), "请先设置 PIN", Toast.LENGTH_SHORT).show()
            pinSetupLauncher.launch(
                Intent(requireContext(), PinSetupActivity::class.java).apply {
                    putExtra(PinSetupActivity.EXTRA_FINISH_AFTER_SAVE, true)
                }
            )
            return
        }

        pinVerifyLauncher.launch(
            Intent(requireContext(), PinVerifyActivity::class.java)
        )
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

    private fun observeRealtimeState() {
        realtimeViewModel.uiState.observe(viewLifecycleOwner) { state ->
            viewModel.applyRealtimeConnection(state.wsConnected)

            state.latestVehicleState?.let { wsState ->
                viewModel.applyRealtimeVehicleState(wsState)
            }

            state.latestCommandAck?.let { ack ->
                viewModel.applyRealtimeCommandAck(ack)
            }
        }
    }
}