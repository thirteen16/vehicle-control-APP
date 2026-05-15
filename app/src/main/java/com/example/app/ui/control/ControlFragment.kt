package com.example.app.ui.control

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
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
import com.example.app.data.repository.VehicleRepository
import com.example.app.di.NetworkModule
import com.example.app.ui.main.AppRealtimeViewModel
import com.example.app.ui.pin.PinSetupActivity
import com.example.app.ui.pin.PinVerifyActivity

class ControlFragment : Fragment(R.layout.fragment_control) {

    private lateinit var progressBar: ProgressBar

    private lateinit var tvSelectedVehicle: TextView
    private lateinit var tvVehicleOnlineStatus: TextView
    private lateinit var tvVehicleLockStatus: TextView
    private lateinit var tvVehicleEngineStatus: TextView
    private lateinit var tvVehicleHvacStatus: TextView
    private lateinit var tvVehicleWindowStatus: TextView
    private lateinit var tvControlHint: TextView
    private lateinit var tvLatestActionResult: TextView
    private lateinit var ivRefreshVehicleState: ImageView

    private lateinit var btnLockOn: Button
    private lateinit var btnLockOff: Button
    private lateinit var btnHvacOn: Button
    private lateinit var btnHvacOff: Button
    private lateinit var btnWindowOpen: Button
    private lateinit var btnWindowClose: Button
    private lateinit var btnEngineOn: Button
    private lateinit var btnEngineOff: Button
    private lateinit var btnStatusQuery: Button

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
        progressBar = view.findViewById(R.id.progressBar)

        tvSelectedVehicle = view.findViewById(R.id.tvSelectedVehicle)
        tvVehicleOnlineStatus = view.findViewById(R.id.tvVehicleOnlineStatus)
        tvVehicleLockStatus = view.findViewById(R.id.tvVehicleLockStatus)
        tvVehicleEngineStatus = view.findViewById(R.id.tvVehicleEngineStatus)
        tvVehicleHvacStatus = view.findViewById(R.id.tvVehicleHvacStatus)
        tvVehicleWindowStatus = view.findViewById(R.id.tvVehicleWindowStatus)
        tvControlHint = view.findViewById(R.id.tvControlHint)
        tvLatestActionResult = view.findViewById(R.id.tvLatestActionResult)
        ivRefreshVehicleState = view.findViewById(R.id.ivRefreshVehicleState)

        btnLockOn = view.findViewById(R.id.btnLockOn)
        btnLockOff = view.findViewById(R.id.btnLockOff)
        btnHvacOn = view.findViewById(R.id.btnHvacOn)
        btnHvacOff = view.findViewById(R.id.btnHvacOff)
        btnWindowOpen = view.findViewById(R.id.btnWindowOpen)
        btnWindowClose = view.findViewById(R.id.btnWindowClose)
        btnEngineOn = view.findViewById(R.id.btnEngineOn)
        btnEngineOff = view.findViewById(R.id.btnEngineOff)
        btnStatusQuery = view.findViewById(R.id.btnStatusQuery)
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
            requestStatusQueryCommand()
        }

        ivRefreshVehicleState.setOnClickListener {
            viewModel.refreshSelectedVehicleState()
        }
    }

    private fun requestProtectedCommand(commandType: String) {
        val state = viewModel.uiState.value ?: return

        if (state.selectedVehicleId.isNullOrBlank()) {
            Toast.makeText(requireContext(), "请先在主页选择一辆车", Toast.LENGTH_SHORT).show()
            return
        }

        if (state.onlineStatus != 1) {
            Toast.makeText(requireContext(), "车辆离线，暂不可执行远程控制", Toast.LENGTH_SHORT).show()
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

    private fun requestStatusQueryCommand() {
        val state = viewModel.uiState.value ?: return

        if (state.selectedVehicleId.isNullOrBlank()) {
            Toast.makeText(requireContext(), "请先在主页选择一辆车", Toast.LENGTH_SHORT).show()
            return
        }

        if (state.onlineStatus != 1) {
            Toast.makeText(requireContext(), "车辆离线，不能向车辆发送状态查询命令", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.sendCommand("STATUS_QUERY")
    }

    private fun observeUiState() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            val busy = state.isLoading || state.isPolling
            progressBar.visibility = if (busy) View.VISIBLE else View.GONE

            tvSelectedVehicle.text = state.selectedVehicleId ?: "未选择车辆"
            tvVehicleOnlineStatus.text = "在线状态：${onlineText(state.onlineStatus)}"
            tvVehicleLockStatus.text = "车锁状态：${state.lockStatus ?: "-"}"
            tvVehicleEngineStatus.text = "发动机：${state.engineStatus ?: "-"}"
            tvVehicleHvacStatus.text = "空调：${state.hvacStatus ?: "-"}"
            tvVehicleWindowStatus.text = "车窗：${state.windowStatus ?: "-"}"
            tvControlHint.text = buildControlHint(state)
            tvLatestActionResult.text = buildLatestActionResult(state)

            renderButtonStates(state)
            renderRefreshState(state)

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

        val canOperate = hasVehicle && online && !busy

        applyButtonState(btnLockOn, canOperate && !state.lockStatus.equals("LOCKED", true))
        applyButtonState(btnLockOff, canOperate && !state.lockStatus.equals("UNLOCKED", true))

        applyButtonState(btnHvacOn, canOperate && !state.hvacStatus.equals("ON", true))
        applyButtonState(btnHvacOff, canOperate && !state.hvacStatus.equals("OFF", true))

        applyButtonState(btnWindowOpen, canOperate && !state.windowStatus.equals("OPEN", true))
        applyButtonState(btnWindowClose, canOperate && !state.windowStatus.equals("CLOSED", true))

        applyButtonState(btnEngineOn, canOperate && !state.engineStatus.equals("ON", true))
        applyButtonState(btnEngineOff, canOperate && !state.engineStatus.equals("OFF", true))

        /*
         * 状态查询也是向车辆发送 MQTT 命令。
         * 所以车辆离线时也不允许点击。
         */
        applyButtonState(btnStatusQuery, canOperate)

        btnLockOn.text = if (state.lockStatus.equals("LOCKED", true)) "已上锁" else "上锁"
        btnLockOff.text = if (state.lockStatus.equals("UNLOCKED", true)) "已解锁" else "解锁"

        btnHvacOn.text = if (state.hvacStatus.equals("ON", true)) "空调已开启" else "开启空调"
        btnHvacOff.text = if (state.hvacStatus.equals("OFF", true)) "空调已关闭" else "关闭空调"

        btnWindowOpen.text = if (state.windowStatus.equals("OPEN", true)) "车窗已打开" else "打开车窗"
        btnWindowClose.text = if (state.windowStatus.equals("CLOSED", true)) "车窗已关闭" else "关闭车窗"

        btnEngineOn.text = if (state.engineStatus.equals("ON", true)) "发动机已启动" else "启动发动机"
        btnEngineOff.text = if (state.engineStatus.equals("OFF", true)) "发动机已关闭" else "关闭发动机"
    }

    private fun renderRefreshState(state: ControlUiState) {
        /*
         * 刷新按钮只是从后端数据库读取车辆状态，不是远程控制车辆。
         * 所以离线时仍然允许点击刷新。
         */
        val canRefresh = !state.selectedVehicleId.isNullOrBlank() && !state.isLoading && !state.isPolling
        ivRefreshVehicleState.isEnabled = canRefresh
        ivRefreshVehicleState.alpha = if (canRefresh) 1f else 0.45f
        ivRefreshVehicleState.rotation = if (state.isLoading || state.isPolling) 180f else 0f
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

    private fun buildControlHint(state: ControlUiState): String {
        return when {
            state.selectedVehicleId.isNullOrBlank() -> "请先在主页中选择当前车辆"
            state.isLoading -> "正在同步车辆状态…"
            state.pendingCommandType != null -> "正在执行：${commandDisplayName(state.pendingCommandType)}"
            state.isPolling -> "正在等待车辆返回操作结果…"
            state.onlineStatus == 0 -> "车辆当前离线，远程控制按钮已禁用。请先启动车机或等待车辆上线"
            else -> "车辆在线，可执行远程控制操作"
        }
    }

    private fun buildLatestActionResult(state: ControlUiState): String {
        val commandType = state.lastCommandType
        val commandResult = state.lastCommandResult

        if (commandType.isNullOrBlank() || commandResult.isNullOrBlank()) {
            return "最近操作：暂无"
        }

        val resultText = when {
            commandResult.equals("SUCCESS", true) -> "成功"
            commandResult.equals("FAILED", true) -> "失败"
            commandResult.equals("PENDING", true) -> "执行中"
            else -> commandResult
        }

        return "最近操作：${commandDisplayName(commandType)} · $resultText"
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