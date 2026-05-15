package com.example.app.ui.control

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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
            renderOnlineStatus(state.onlineStatus)

            tvVehicleLockStatus.text = lockUiText(state.lockStatus)
            tvVehicleEngineStatus.text = onOffUiText(state.engineStatus)
            tvVehicleHvacStatus.text = onOffUiText(state.hvacStatus)
            tvVehicleWindowStatus.text = windowUiText(state.windowStatus)
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

        val lockOpened = isLockOpened(state.lockStatus)
        val lockClosed = isLockClosed(state.lockStatus)
        applyCommandButtonState(btnLockOff, canOperate && !lockOpened, isOpenButton = true)
        applyCommandButtonState(btnLockOn, canOperate && !lockClosed, isOpenButton = false)

        val engineOn = isOn(state.engineStatus)
        val engineOff = isOff(state.engineStatus)
        applyCommandButtonState(btnEngineOn, canOperate && !engineOn, isOpenButton = true)
        applyCommandButtonState(btnEngineOff, canOperate && !engineOff, isOpenButton = false)

        val hvacOn = isOn(state.hvacStatus)
        val hvacOff = isOff(state.hvacStatus)
        applyCommandButtonState(btnHvacOn, canOperate && !hvacOn, isOpenButton = true)
        applyCommandButtonState(btnHvacOff, canOperate && !hvacOff, isOpenButton = false)

        val windowOpen = isWindowOpen(state.windowStatus)
        val windowClosed = isWindowClosed(state.windowStatus)
        applyCommandButtonState(btnWindowOpen, canOperate && !windowOpen, isOpenButton = true)
        applyCommandButtonState(btnWindowClose, canOperate && !windowClosed, isOpenButton = false)

        applyCommandButtonState(btnStatusQuery, canOperate, isOpenButton = true)

        btnLockOff.text = "开"
        btnLockOn.text = "关"
        btnEngineOn.text = "开"
        btnEngineOff.text = "关"
        btnHvacOn.text = "开"
        btnHvacOff.text = "关"
        btnWindowOpen.text = "开"
        btnWindowClose.text = "关"
    }

    private fun renderOnlineStatus(onlineStatus: Int?) {
        when (onlineStatus) {
            1 -> {
                tvVehicleOnlineStatus.text = "在线"
                tvVehicleOnlineStatus.setBackgroundResource(R.drawable.bg_status_success)
                tvVehicleOnlineStatus.setTextColor(Color.parseColor("#159A86"))
            }

            0 -> {
                tvVehicleOnlineStatus.text = "离线"
                tvVehicleOnlineStatus.setBackgroundResource(R.drawable.bg_status_failed)
                tvVehicleOnlineStatus.setTextColor(Color.parseColor("#F04438"))
            }

            else -> {
                tvVehicleOnlineStatus.text = "-"
                tvVehicleOnlineStatus.setBackgroundResource(R.drawable.bg_status_pending)
                tvVehicleOnlineStatus.setTextColor(Color.parseColor("#7B8794"))
            }
        }
    }

    private fun renderRefreshState(state: ControlUiState) {
        val canRefresh = !state.selectedVehicleId.isNullOrBlank() && !state.isLoading && !state.isPolling
        ivRefreshVehicleState.isEnabled = canRefresh
        ivRefreshVehicleState.alpha = if (canRefresh) 1f else 0.45f
        ivRefreshVehicleState.rotation = if (state.isLoading || state.isPolling) 180f else 0f
    }

    private fun applyCommandButtonState(button: Button, enabled: Boolean, isOpenButton: Boolean) {
        button.isEnabled = enabled
        button.alpha = 1f

        if (enabled) {
            button.setBackgroundResource(
                if (isOpenButton) R.drawable.bg_button_primary else R.drawable.bg_button_danger
            )
            button.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        } else {
            button.setBackgroundResource(R.drawable.bg_button_secondary)
            button.setTextColor(Color.parseColor("#9AA5B1"))
        }
    }

    private fun lockUiText(status: String?): String {
        return when {
            isLockOpened(status) -> "开"
            isLockClosed(status) -> "关"
            else -> "-"
        }
    }

    private fun onOffUiText(status: String?): String {
        return when {
            isOn(status) -> "开"
            isOff(status) -> "关"
            else -> "-"
        }
    }

    private fun windowUiText(status: String?): String {
        return when {
            isWindowOpen(status) -> "开"
            isWindowClosed(status) -> "关"
            else -> "-"
        }
    }

    private fun isLockOpened(status: String?): Boolean {
        return status.equals("UNLOCKED", true) ||
                status.equals("LOCK_OFF", true) ||
                status == "解锁" ||
                status == "开"
    }

    private fun isLockClosed(status: String?): Boolean {
        return status.equals("LOCKED", true) ||
                status.equals("LOCK_ON", true) ||
                status == "上锁" ||
                status == "关"
    }

    private fun isOn(status: String?): Boolean {
        return status.equals("ON", true) ||
                status == "开启" ||
                status == "启动" ||
                status == "开"
    }

    private fun isOff(status: String?): Boolean {
        return status.equals("OFF", true) ||
                status == "关闭" ||
                status == "关"
    }

    private fun isWindowOpen(status: String?): Boolean {
        return status.equals("OPEN", true) ||
                status == "打开" ||
                status == "开"
    }

    private fun isWindowClosed(status: String?): Boolean {
        return status.equals("CLOSED", true) ||
                status.equals("CLOSE", true) ||
                status == "关闭" ||
                status == "关"
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
            state.selectedVehicleId.isNullOrBlank() -> "请先在主页选择当前车辆"
            state.isLoading -> "正在同步车辆状态"
            state.pendingCommandType != null -> "正在执行：${commandDisplayName(state.pendingCommandType)}"
            state.isPolling -> "正在等待车辆返回结果"
            state.onlineStatus == 0 -> "当前离线，暂不可使用控制功能"
            else -> "当前在线，可以远程控制"
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
            "LOCK_ON" -> "车锁关"
            "LOCK_OFF" -> "车锁开"
            "HVAC_ON" -> "空调开"
            "HVAC_OFF" -> "空调关"
            "WINDOW_OPEN" -> "车窗开"
            "WINDOW_CLOSE" -> "车窗关"
            "ENGINE_ON" -> "发动机开"
            "ENGINE_OFF" -> "发动机关"
            "STATUS_QUERY" -> "状态查询"
            else -> commandType ?: "-"
        }
    }
}