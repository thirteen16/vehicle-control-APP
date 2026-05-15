package com.example.app.ui.home

import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.BitmapFactory
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
import java.io.File

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var progressBar: ProgressBar
    private lateinit var tvStateBanner: TextView

    private lateinit var tvVehicleCount: TextView

    private lateinit var ivCurrentVehicleImage: ImageView
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

    private lateinit var layoutOtherVehicleHeader: LinearLayout
    private lateinit var tvOtherVehicleTitle: TextView
    private lateinit var tvOtherVehicleCount: TextView
    private lateinit var ivToggleOtherVehicles: ImageView
    private lateinit var tvEmptyOtherVehicles: TextView
    private lateinit var otherVehicleContainer: LinearLayout

    private lateinit var viewModel: HomeViewModel
    private lateinit var realtimeViewModel: AppRealtimeViewModel

    private var lastOtherVehicleSignature: String? = null
    private var lastSelectedVehicleId: String? = null
    private var refreshAnimator: ObjectAnimator? = null
    private var isOtherVehiclesExpanded = false

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

        ivCurrentVehicleImage = view.findViewById(R.id.ivCurrentVehicleImage)
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

        layoutOtherVehicleHeader = view.findViewById(R.id.layoutOtherVehicleHeader)
        tvOtherVehicleTitle = view.findViewById(R.id.tvOtherVehicleTitle)
        tvOtherVehicleCount = view.findViewById(R.id.tvOtherVehicleCount)
        ivToggleOtherVehicles = view.findViewById(R.id.ivToggleOtherVehicles)
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

        layoutOtherVehicleHeader.setOnClickListener {
            toggleOtherVehicles()
        }

        ivToggleOtherVehicles.setOnClickListener {
            toggleOtherVehicles()
        }
    }

    private fun toggleOtherVehicles() {
        val state = viewModel.uiState.value ?: return
        val otherCount = state.vehicles.count { it.vehicleId != state.selectedVehicleId }
        if (otherCount == 0) return

        isOtherVehiclesExpanded = !isOtherVehiclesExpanded
        updateOtherVehicleSectionVisibility(otherCount)
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
        val brand = selectedState?.brand ?: selectedVehicle?.brand ?: ""
        val model = selectedState?.model ?: selectedVehicle?.model ?: ""

        val brandModelText = listOf(brand, model)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifBlank { "-" }

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
        tvCurrentVehicleId.text = vehicleId
        tvCurrentBrandModel.text = brandModelText
        tvCurrentOnlineStatus.text = onlineText
        tvCurrentLockStatus.text = lockText
        tvCurrentEngineStatus.text = engineText
        tvCurrentHvacStatus.text = hvacText
        tvCurrentWindowStatus.text = windowText
        tvCurrentMileage.text = mileageText
        tvCurrentFuelLevel.text = fuelText
        tvCurrentUpdatedTime.text = updatedText

        loadVehicleImage(vehicleId = state.selectedVehicleId, target = ivCurrentVehicleImage)

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
                refreshAnimator = ObjectAnimator.ofFloat(
                    ivRefreshCurrent,
                    View.ROTATION,
                    0f,
                    360f
                ).apply {
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
        val otherCount = otherVehicles.size

        tvOtherVehicleTitle.text = "其他绑定车辆"
        tvOtherVehicleCount.text = otherCount.toString()

        val inflater = LayoutInflater.from(requireContext())

        otherVehicles.forEach { vehicle ->
            val itemView = inflater.inflate(R.layout.item_home_vehicle, otherVehicleContainer, false)

            val root = itemView.findViewById<View>(R.id.rootVehicleCard)
            val ivVehicleImage = itemView.findViewById<ImageView>(R.id.ivVehicleImage)
            val tvName = itemView.findViewById<TextView>(R.id.tvVehicleName)
            val tvBrandModel = itemView.findViewById<TextView>(R.id.tvBrandModel)
            val tvMileage = itemView.findViewById<TextView>(R.id.tvMileage)
            val tvVehicleOnlineStatus = itemView.findViewById<TextView>(R.id.tvVehicleOnlineStatus)
            val tvVehicleFuelLevel = itemView.findViewById<TextView>(R.id.tvVehicleFuelLevel)
            val tvVehicleId = itemView.findViewById<TextView>(R.id.tvVehicleId)
            val tvHint = itemView.findViewById<TextView>(R.id.tvHint)
            val ivLocation = itemView.findViewById<ImageView>(R.id.ivLocation)

            val displayName = vehicle.name.ifBlank { vehicle.vehicleId }
            val brandModelText = listOfNotNull(vehicle.brand, vehicle.model)
                .filter { it.isNotBlank() }
                .joinToString(" ")
                .ifBlank { "-" }

            val onlineText = when (vehicle.onlineStatus) {
                1 -> "在线"
                0 -> "离线"
                else -> "-"
            }

            val fuelText = "${vehicle.fuelLevel ?: "-"}%"
            val mileageText = "${vehicle.mileage ?: "-"} km"

            tvName.text = displayName
            tvBrandModel.text = brandModelText
            tvVehicleOnlineStatus.text = onlineText
            tvVehicleFuelLevel.text = fuelText
            tvMileage.text = mileageText

            // 这两个 id 保留，但页面不显示
            tvVehicleId.text = vehicle.vehicleId
            tvHint.text = ""

            loadVehicleImage(vehicleId = vehicle.vehicleId, target = ivVehicleImage)

            root.setBackgroundResource(R.drawable.bg_car_card)

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

        updateOtherVehicleSectionVisibility(otherCount)
    }

    private fun updateOtherVehicleSectionVisibility(otherCount: Int) {
        tvOtherVehicleCount.text = otherCount.toString()

        if (otherCount == 0) {
            tvEmptyOtherVehicles.visibility = View.VISIBLE
            otherVehicleContainer.visibility = View.GONE
            ivToggleOtherVehicles.setImageResource(android.R.drawable.arrow_down_float)
            ivToggleOtherVehicles.alpha = 0.35f
            layoutOtherVehicleHeader.alpha = 1f
            return
        }

        tvEmptyOtherVehicles.visibility = View.GONE
        ivToggleOtherVehicles.alpha = 1f

        if (isOtherVehiclesExpanded) {
            otherVehicleContainer.visibility = View.VISIBLE
            ivToggleOtherVehicles.setImageResource(android.R.drawable.arrow_up_float)
        } else {
            otherVehicleContainer.visibility = View.GONE
            ivToggleOtherVehicles.setImageResource(android.R.drawable.arrow_down_float)
        }
    }

    private fun loadVehicleImage(vehicleId: String?, target: ImageView) {
        if (vehicleId.isNullOrBlank()) {
            target.setImageResource(R.drawable.bg_login_car)
            return
        }

        // 第一优先级：读取打包进 App 的图片
        // 例如 vehicleId = v001，对应图片名就是 res/drawable-nodpi/vehicle_v001.jpg
        val drawableName = "vehicle_${vehicleId.lowercase()}"
        val drawableId = resources.getIdentifier(
            drawableName,
            "drawable",
            requireContext().packageName
        )

        if (drawableId != 0) {
            target.setImageResource(drawableId)
            return
        }

        // 第二优先级：读取手机本地目录图片
        val imageDir = requireContext().getExternalFilesDir("vehicle_images")
        if (imageDir != null) {
            val candidates = listOf(
                File(imageDir, "$vehicleId.jpg"),
                File(imageDir, "$vehicleId.jpeg"),
                File(imageDir, "$vehicleId.png")
            )

            val imageFile = candidates.firstOrNull { it.exists() && it.isFile }
            if (imageFile != null) {
                val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                if (bitmap != null) {
                    target.setImageBitmap(bitmap)
                    return
                }
            }
        }

        // 没有找到车辆图片时，显示默认图
        target.setImageResource(R.drawable.bg_login_car)
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
                append(vehicle.onlineStatus)
                append("|")
                append(vehicle.fuelLevel)
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