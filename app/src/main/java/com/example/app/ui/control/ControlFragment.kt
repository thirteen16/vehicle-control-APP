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
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.app.R
import com.example.app.data.local.PinStore
import com.example.app.data.local.SelectedVehicleStore
import com.example.app.data.repository.CommandRepository
import com.example.app.data.repository.VehicleRepository
import com.example.app.di.NetworkModule
import com.example.app.ui.main.AppRealtimeViewModel
import com.example.app.ui.pin.PinSetupActivity
import com.example.app.ui.pin.PinVerifyActivity

class ControlFragment : Fragment(R.layout.fragment_control) {

    private lateinit var tvSelectedVehicle: TextView
    private lateinit var tvWsStatus: TextView
    private lateinit var tvVehicleOnlineStatus: TextView
    private lateinit var tvVehicleLockStatus: TextView
    private lateinit var tvVehicleEngineStatus: TextView
    private lateinit var tvVehicleHvacStatus: TextView
    private lateinit var tvVehicleWindowStatus: TextView
    private lateinit var tvRealtimeState: TextView
    private lateinit var tvActionStatus: TextView
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
    private lateinit var btnRefreshVehicleState: Button
    private lateinit var btnPing: Button

    private lateinit var viewModel: ControlViewModel
    private lateinit var realtimeViewModel: AppRealtimeViewModel
    private lateinit var pinStore: PinStore

    private var pendingProtectedCommand: String? = null

    private val pinSetupLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val commandType = pendingProtectedCommand
            if (result.resultCode == Activity.RESULT_OK && !commandType.isNullOrBlank()) {
                showCommandConfirmDialog(commandType)
            } else if (!commandType.isNullOrBlank()) {
                Toast.makeText(requireContext(), "未完成 PIN 设置，命令已取消", Toast.LENGTH_SHORT).show()
                pendingProtectedCommand = null
            }
        }

    private val pinVerifyLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val commandType = pendingProtectedCommand
            if (result.resultCode == Activity.RESULT_OK && !commandType.isNullOrBlank()) {
                showCommandConfirmDialog(commandType)
            } else if (!commandType.isNullOrBlank()) {
                Toast.makeText(requireContext(), "PIN 验证未通过，命令已取消", Toast.LENGTH_SHORT).show()
                pendingProtectedCommand = null
            }
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
        val vehicleApi = NetworkModule.provideVehicleApi(retrofit)
        val selectedVehicleStore = SelectedVehicleStore(appContext)

        val commandRepository = CommandRepository(commandApi, selectedVehicleStore)
        val vehicleRepository = VehicleRepository(vehicleApi, selectedVehicleStore)

        pinStore = PinStore(appContext)

        viewModel = ViewModelProvider(
            this,
            ControlViewModel.Factory(commandRepository, vehicleRepository)
        )[ControlViewModel::class.java]

        realtimeViewModel = ViewModelProvider(requireActivity())[AppRealtimeViewModel::class.java]

        initViews(view)
        initListeners()
        observeUiState()
        observeRealtimeState()

        realtimeViewModel.connectIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadSelectedVehicle()
    }

    private fun initViews(view: View) {
        tvSelectedVehicle = view.findViewById(R.id.tvSelectedVehicle)
        tvWsStatus = view.findViewById(R.id.tvWsStatus)
        tvVehicleOnlineStatus = view.findViewById(R.id.tvVehicleOnlineStatus)
        tvVehicleLockStatus = view.findViewById(R.id.tvVehicleLockStatus)
        tvVehicleEngineStatus = view.findViewById(R.id.tvVehicleEngineStatus)
        tvVehicleHvacStatus = view.findViewById(R.id.tvVehicleHvacStatus)
        tvVehicleWindowStatus = view.findViewById(R.id.tvVehicleWindowStatus)
        tvRealtimeState = view.findViewById(R.id.tvRealtimeState)
        tvActionStatus = view.findViewById(R.id.tvActionStatus)
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
        btnRefreshVehicleState = view.findViewById(R.id.btnRefreshVehicleState)
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

        btnRefreshVehicleState.setOnClickListener {
            viewModel.refreshSelectedVehicleState()
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
        val state = viewModel.uiState.value ?: return

        if (state.selectedVehicleId.isNullOrBlank()) {
            Toast.makeText(requireContext(), "请先在车辆页选择一辆车", Toast.LENGTH_SHORT).show()
            return
        }

        if (state.onlineStatus != 1) {
            Toast.makeText(requireContext(), "车辆离线，暂不可执行该操作", Toast.LENGTH_SHORT).show()
            return
        }

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

        pinVerifyLauncher.launch(Intent(requireContext(), PinVerifyActivity::class.java))
    }

    private fun showCommandConfirmDialog(commandType: String) {
        val title = commandDisplayName(commandType)
        AlertDialog.Builder(requireContext())
            .setTitle("确认操作")
            .setMessage("确定要执行“$title”吗？")
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
                pendingProtectedCommand = null
            }
            .setPositiveButton("确认") { dialog, _ ->
                dialog.dismiss()
                viewModel.sendCommand(commandType)
                pendingProtectedCommand = null
            }
            .show()
    }

    private fun observeUiState() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            val busy = state.isLoading || state.isPolling
            progressBar.visibility = if (busy) View.VISIBLE else View.GONE

            tvSelectedVehicle.text = "当前车辆：${state.selectedVehicleId ?: "未选择"}"
            tvWsStatus.text = "实时通道：${if (state.wsConnected) "已连接" else "未连接"}"
            tvVehicleOnlineStatus.text = "在线状态：${onlineText(state.onlineStatus)}"
            tvVehicleLockStatus.text = "车锁状态：${state.lockStatus ?: "-"}"
            tvVehicleEngineStatus.text = "发动机：${state.engineStatus ?: "-"}"
            tvVehicleHvacStatus.text = "空调：${state.hvacStatus ?: "-"}"
            tvVehicleWindowStatus.text = "车窗：${state.windowStatus ?: "-"}"
            tvRealtimeState.text = "最近实时状态：${state.latestVehicleStateText ?: "-"}"
            tvActionStatus.text = buildActionStatusText(state)
            tvCommandId.text = "commandId：${state.lastCommandId ?: "-"}"
            tvCommandType.text = "命令类型：${state.lastCommandType ?: "-"}"
            tvCommandResult.text = "命令结果：${state.lastCommandResult ?: "-"}"
            tvRequestTime.text = "请求时间：${state.lastRequestTime ?: "-"}"
            tvResponseTime.text = "响应时间：${state.lastResponseTime ?: "-"}"

            renderButtonStates(state)

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

    private fun renderButtonStates(state: ControlUiState) {
        val hasVehicle = !state.selectedVehicleId.isNullOrBlank()
        val busy = state.isLoading || state.isPolling
        val online = state.onlineStatus == 1

        val canProtectedOperate = hasVehicle && online && !busy
        val canQuery = hasVehicle && !busy
        val canRefreshResult = !state.lastCommandId.isNullOrBlank() && !busy
        val canRefreshVehicleState = hasVehicle && !busy

        applyButtonState(btnLockOn, canProtectedOperate && !state.lockStatus.equals("LOCKED", true))
        applyButtonState(btnLockOff, canProtectedOperate && !state.lockStatus.equals("UNLOCKED", true))

        applyButtonState(btnHvacOn, canProtectedOperate && !state.hvacStatus.equals("ON", true))
        applyButtonState(btnHvacOff, canProtectedOperate && !state.hvacStatus.equals("OFF", true))

        applyButtonState(btnWindowOpen, canProtectedOperate && !state.windowStatus.equals("OPEN", true))
        applyButtonState(btnWindowClose, canProtectedOperate && !state.windowStatus.equals("CLOSED", true))

        applyButtonState(btnEngineOn, canProtectedOperate && !state.engineStatus.equals("ON", true))
        applyButtonState(btnEngineOff, canProtectedOperate && !state.engineStatus.equals("OFF", true))

        applyButtonState(btnStatusQuery, canQuery)
        applyButtonState(btnRefreshResult, canRefreshResult)
        applyButtonState(btnRefreshVehicleState, canRefreshVehicleState)
        applyButtonState(btnPing, !busy)

        btnLockOn.text = if (state.lockStatus.equals("LOCKED", true)) "已上锁" else "上锁"
        btnLockOff.text = if (state.lockStatus.equals("UNLOCKED", true)) "已解锁" else "解锁"

        btnHvacOn.text = if (state.hvacStatus.equals("ON", true)) "空调已开启" else "开启空调"
        btnHvacOff.text = if (state.hvacStatus.equals("OFF", true)) "空调已关闭" else "关闭空调"

        btnWindowOpen.text = if (state.windowStatus.equals("OPEN", true)) "车窗已打开" else "打开车窗"
        btnWindowClose.text = if (state.windowStatus.equals("CLOSED", true)) "车窗已关闭" else "关闭车窗"

        btnEngineOn.text = if (state.engineStatus.equals("ON", true)) "发动机已启动" else "启动发动机"
        btnEngineOff.text = if (state.engineStatus.equals("OFF", true)) "发动机已关闭" else "关闭发动机"
    }

    private fun applyButtonState(button: Button, enabled: Boolean) {
        button.isEnabled = enabled
        button.alpha = if (enabled) 1f else 0.45f
    }

    private fun onlineText(onlineStatus: Int?): String {
        return when (onlineStatus) {
            1 -> "在线"
            0 -> "离线"
            else -> "-"
        }
    }

    private fun buildActionStatusText(state: ControlUiState): String {
        return when {
            state.pendingCommandType != null -> "执行中：${commandDisplayName(state.pendingCommandType)}，请等待结果"
            state.isPolling -> "正在轮询命令结果，请稍候"
            state.selectedVehicleId.isNullOrBlank() -> "请先在车辆页选择一辆车"
            state.onlineStatus == 0 -> "车辆当前离线，仅建议使用“状态查询”"
            state.wsConnected -> "控制通道正常，可执行操作"
            else -> "实时通道未连接，仍可尝试控制，但回执可能延迟"
        }
    }

    private fun commandDisplayName(commandType: String?): String {
        return when (commandType) {
            "LOCK_ON" -> "上锁"
            "LOCK_OFF" -> "解锁"
            "HVAC_ON" -> "开启空调"
            "HVAC_OFF" -> "关闭空调"
            "WINDOW_OPEN" -> "打开车窗"
            "WINDOW_CLOSE" -> "关闭车窗"
            "ENGINE_ON" -> "启动发动机"
            "ENGINE_OFF" -> "关闭发动机"
            "STATUS_QUERY" -> "状态查询"
            else -> commandType ?: "-"
        }
    }
}