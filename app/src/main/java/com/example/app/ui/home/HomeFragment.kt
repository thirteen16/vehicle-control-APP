package com.example.app.ui.home

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.app.R
import com.example.app.common.UiStateTextResolver
import com.example.app.data.local.SelectedVehicleStore
import com.example.app.data.repository.VehicleRepository
import com.example.app.di.NetworkModule
import com.example.app.ui.main.AppRealtimeViewModel
import com.example.app.ui.vehicle.location.VehicleLocationActivity

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var progressBar: ProgressBar
    private lateinit var tvStateBanner: TextView

    private lateinit var tvVehicleCount: TextView

    private lateinit var tvCurrentVehicleName: TextView
    private lateinit var tvCurrentVehicleId: TextView
    private lateinit var tvCurrentBrandModel: TextView
    private lateinit var tvCurrentOnlineStatus: TextView
    private lateinit var tvCurrentLockStatus: TextView
    private lateinit var tvCurrentEngineStatus: TextView
    private lateinit var tvCurrentHvacStatus: TextView
    private lateinit var tvCurrentWindowStatus: TextView
    private lateinit var tvCurrentMileage: TextView
    private lateinit var tvCurrentFuelLevel: TextView
    private lateinit var tvCurrentUpdatedTime: TextView
    private lateinit var ivRefreshCurrent: ImageView
    private lateinit var ivCurrentLocation: ImageView

    private lateinit var tvOtherVehicleTitle: TextView
    private lateinit var tvEmptyOtherVehicles: TextView
    private lateinit var otherVehicleContainer: LinearLayout

    private lateinit var viewModel: HomeViewModel
    private lateinit var realtimeViewModel: AppRealtimeViewModel

    private var lastOtherVehicleSignature: String? = null
    private var lastSelectedVehicleId: String? = null
    private var refreshAnimator: ObjectAnimator? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val appContext = requireContext().applicationContext
        val tokenStore = NetworkModule.provideTokenStore(appContext)
        val authInterceptor = NetworkModule.provideAuthInterceptor(tokenStore)
        val loggingInterceptor = NetworkModule.provideLoggingInterceptor()
        val okHttpClient = NetworkModule.provideOkHttpClient(authInterceptor, loggingInterceptor)
        val retrofit = NetworkModule.provideRetrofit(okHttpClient)
        val vehicleApi = NetworkModule.provideVehicleApi(retrofit)
        val selectedVehicleStore = SelectedVehicleStore(appContext)
        val vehicleRepository = VehicleRepository(vehicleApi, selectedVehicleStore)

        viewModel = ViewModelProvider(
            this,
            HomeViewModel.Factory(vehicleRepository, tokenStore)
        )[HomeViewModel::class.java]

        realtimeViewModel = ViewModelProvider(requireActivity())[AppRealtimeViewModel::class.java]

        initViews(view)
        initListeners()
        observeUiState()
        observeRealtimeState()
    }

    private fun initViews(view: View) {
        progressBar = view.findViewById(R.id.progressBar)
        tvStateBanner = view.findViewById(R.id.tvStateBanner)

        tvVehicleCount = view.findViewById(R.id.tvVehicleCount)

        tvCurrentVehicleName = view.findViewById(R.id.tvCurrentVehicleName)
        tvCurrentVehicleId = view.findViewById(R.id.tvCurrentVehicleId)
        tvCurrentBrandModel = view.findViewById(R.id.tvCurrentBrandModel)
        tvCurrentOnlineStatus = view.findViewById(R.id.tvCurrentOnlineStatus)
        tvCurrentLockStatus = view.findViewById(R.id.tvCurrentLockStatus)
        tvCurrentEngineStatus = view.findViewById(R.id.tvCurrentEngineStatus)
        tvCurrentHvacStatus = view.findViewById(R.id.tvCurrentHvacStatus)
        tvCurrentWindowStatus = view.findViewById(R.id.tvCurrentWindowStatus)
        tvCurrentMileage = view.findViewById(R.id.tvCurrentMileage)
        tvCurrentFuelLevel = view.findViewById(R.id.tvCurrentFuelLevel)
        tvCurrentUpdatedTime = view.findViewById(R.id.tvCurrentUpdatedTime)
        ivRefreshCurrent = view.findViewById(R.id.ivRefreshCurrent)
        ivCurrentLocation = view.findViewById(R.id.ivCurrentLocation)

        tvOtherVehicleTitle = view.findViewById(R.id.tvOtherVehicleTitle)
        tvEmptyOtherVehicles = view.findViewById(R.id.tvEmptyOtherVehicles)
        otherVehicleContainer = view.findViewById(R.id.otherVehicleContainer)
    }

    private fun initListeners() {
        ivRefreshCurrent.setOnClickListener {
            viewModel.refreshSelectedVehicleState()
        }

        ivCurrentLocation.setOnClickListener {
            val vehicleId = viewModel.uiState.value?.selectedVehicleId
            if (vehicleId.isNullOrBlank()) {
                Toast.makeText(requireContext(), "当前没有已绑定车辆", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            startActivity(
                Intent(requireContext(), VehicleLocationActivity::class.java).apply {
                    putExtra(VehicleLocationActivity.EXTRA_VEHICLE_ID, vehicleId)
                }
            )
        }
    }

    private fun observeUiState() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            progressBar.visibility =
                if (state.isLoading && state.vehicles.isEmpty()) View.VISIBLE else View.GONE

            renderBanner(state)
            renderVehicleCount(state)
            renderCurrentVehicle(state)
            renderRefreshState(state)

            val currentOtherSignature = buildOtherVehicleSignature(state)
            val selectedChanged = lastSelectedVehicleId != state.selectedVehicleId

            if (currentOtherSignature != lastOtherVehicleSignature || selectedChanged) {
                renderOtherVehicles(state)
                lastOtherVehicleSignature = currentOtherSignature
                lastSelectedVehicleId = state.selectedVehicleId
            }
        }
    }

    private fun observeRealtimeState() {
        realtimeViewModel.uiState.observe(viewLifecycleOwner) { state ->
            state.latestVehicleState?.let { wsState ->
                viewModel.applyRealtimeVehicleState(wsState)
            }
        }
    }

    private fun renderBanner(state: HomeUiState) {
        when {
            !state.errorMessage.isNullOrBlank() -> {
                tvStateBanner.visibility = View.VISIBLE
                tvStateBanner.text = "加载失败：${UiStateTextResolver.resolveError(state.errorMessage)}"
                tvStateBanner.setBackgroundResource(R.drawable.bg_notice_error)
            }

            state.vehicles.isEmpty() -> {
                tvStateBanner.visibility = View.VISIBLE
                tvStateBanner.text = UiStateTextResolver.homeEmptyMessage()
                tvStateBanner.setBackgroundResource(R.drawable.bg_notice_empty)
            }

            else -> {
                tvStateBanner.visibility = View.GONE
            }
        }
    }

    private fun renderVehicleCount(state: HomeUiState) {
        tvVehicleCount.text = state.vehicles.size.toString()
    }

    private fun renderCurrentVehicle(state: HomeUiState) {
        val selectedState = state.selectedState
        val selectedVehicle = state.vehicles.firstOrNull { it.vehicleId == state.selectedVehicleId }

        val vehicleName = selectedState?.name?.ifBlank { null }
            ?: selectedVehicle?.name?.ifBlank { null }
            ?: "未绑定车辆"

        val vehicleId = state.selectedVehicleId ?: "-"
        val brand = selectedState?.brand ?: selectedVehicle?.brand ?: "-"
        val model = selectedState?.model ?: selectedVehicle?.model ?: "-"

        val onlineText = when (selectedState?.onlineStatus ?: selectedVehicle?.onlineStatus) {
            1 -> "在线"
            0 -> "离线"
            else -> "-"
        }

        val lockText = selectedState?.lockStatus ?: selectedVehicle?.lockStatus ?: "-"
        val engineText = selectedState?.engineStatus ?: selectedVehicle?.engineStatus ?: "-"
        val hvacText = selectedState?.hvacStatus ?: selectedVehicle?.hvacStatus ?: "-"
        val windowText = selectedState?.windowStatus ?: selectedVehicle?.windowStatus ?: "-"
        val mileageText = "${selectedState?.mileage ?: selectedVehicle?.mileage ?: "-"} km"
        val fuelText = "${selectedState?.fuelLevel ?: selectedVehicle?.fuelLevel ?: "-"}%"
        val updatedText = selectedState?.updatedTime ?: selectedVehicle?.updatedTime ?: "-"

        tvCurrentVehicleName.text = vehicleName
        tvCurrentVehicleId.text = "vehicleId：$vehicleId"
        tvCurrentBrandModel.text = "品牌 / 型号：$brand / $model"
        tvCurrentOnlineStatus.text = "在线状态：$onlineText"
        tvCurrentLockStatus.text = "车锁状态：$lockText"
        tvCurrentEngineStatus.text = "发动机：$engineText"
        tvCurrentHvacStatus.text = "空调：$hvacText"
        tvCurrentWindowStatus.text = "车窗：$windowText"
        tvCurrentMileage.text = "总里程：$mileageText"
        tvCurrentFuelLevel.text = "油量：$fuelText"
        tvCurrentUpdatedTime.text = "更新时间：$updatedText"

        val hasSelectedVehicle = !state.selectedVehicleId.isNullOrBlank()
        ivCurrentLocation.isEnabled = hasSelectedVehicle
        ivCurrentLocation.alpha = if (hasSelectedVehicle) 1f else 0.45f
    }

    private fun renderRefreshState(state: HomeUiState) {
        val canRefresh = !state.selectedVehicleId.isNullOrBlank()
        ivRefreshCurrent.isEnabled = canRefresh && !state.isRefreshingCurrent
        ivRefreshCurrent.alpha = if (canRefresh) 1f else 0.45f

        if (state.isRefreshingCurrent) {
            if (refreshAnimator == null) {
                refreshAnimator = ObjectAnimator.ofFloat(ivRefreshCurrent, View.ROTATION, 0f, 360f).apply {
                    duration = 700
                    repeatCount = ObjectAnimator.INFINITE
                    interpolator = LinearInterpolator()
                }
            }
            if (refreshAnimator?.isStarted != true) {
                refreshAnimator?.start()
            }
        } else {
            refreshAnimator?.cancel()
            ivRefreshCurrent.rotation = 0f
        }
    }

    private fun renderOtherVehicles(state: HomeUiState) {
        otherVehicleContainer.removeAllViews()

        val otherVehicles = state.vehicles.filter { it.vehicleId != state.selectedVehicleId }

        tvOtherVehicleTitle.text = "其他车辆 (${otherVehicles.size})"
        tvEmptyOtherVehicles.visibility = if (otherVehicles.isEmpty()) View.VISIBLE else View.GONE
        otherVehicleContainer.visibility = if (otherVehicles.isEmpty()) View.GONE else View.VISIBLE

        val inflater = LayoutInflater.from(requireContext())

        otherVehicles.forEach { vehicle ->
            val itemView = inflater.inflate(R.layout.item_home_vehicle, otherVehicleContainer, false)

            val root = itemView.findViewById<View>(R.id.rootVehicleCard)
            val tvName = itemView.findViewById<TextView>(R.id.tvVehicleName)
            val tvVehicleId = itemView.findViewById<TextView>(R.id.tvVehicleId)
            val tvBrandModel = itemView.findViewById<TextView>(R.id.tvBrandModel)
            val tvMileage = itemView.findViewById<TextView>(R.id.tvMileage)
            val tvHint = itemView.findViewById<TextView>(R.id.tvHint)
            val ivLocation = itemView.findViewById<ImageView>(R.id.ivLocation)

            val displayName = vehicle.name.ifBlank { vehicle.vehicleId }
            val brandModelText = listOfNotNull(vehicle.brand, vehicle.model)
                .filter { it.isNotBlank() }
                .joinToString(" / ")
                .ifBlank { "暂无车型信息" }

            tvName.text = displayName
            tvVehicleId.text = "vehicleId：${vehicle.vehicleId}"
            tvBrandModel.text = "品牌 / 型号：$brandModelText"
            tvMileage.text = "总里程：${vehicle.mileage ?: "-"} km"
            tvHint.text = "点击卡片可切换为当前车辆"

            root.setOnClickListener {
                viewModel.selectVehicle(vehicle.vehicleId)
                Toast.makeText(requireContext(), "已切换当前车辆：$displayName", Toast.LENGTH_SHORT).show()
            }

            ivLocation.setOnClickListener {
                startActivity(
                    Intent(requireContext(), VehicleLocationActivity::class.java).apply {
                        putExtra(VehicleLocationActivity.EXTRA_VEHICLE_ID, vehicle.vehicleId)
                    }
                )
            }

            otherVehicleContainer.addView(itemView)
        }
    }

    private fun buildOtherVehicleSignature(state: HomeUiState): String {
        val otherVehicles = state.vehicles.filter { it.vehicleId != state.selectedVehicleId }
        return buildString {
            otherVehicles.forEach { vehicle ->
                append(vehicle.vehicleId)
                append("|")
                append(vehicle.name)
                append("|")
                append(vehicle.brand)
                append("|")
                append(vehicle.model)
                append("|")
                append(vehicle.mileage)
                append(";")
            }
        }
    }

    override fun onDestroyView() {
        refreshAnimator?.cancel()
        refreshAnimator = null
        super.onDestroyView()
    }
}