package com.example.app.ui.home

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.app.R
import com.example.app.data.local.SelectedVehicleStore
import com.example.app.data.repository.VehicleRepository
import com.example.app.di.NetworkModule
import com.example.app.ui.auth.login.LoginActivity
import com.example.app.ui.control.ControlActivity

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var tvWelcome: TextView
    private lateinit var tvUserInfo: TextView
    private lateinit var vehicleContainer: LinearLayout
    private lateinit var tvEmptyTip: TextView

    private lateinit var tvSelectedVehicleTitle: TextView
    private lateinit var tvBrandModel: TextView
    private lateinit var tvOnlineStatus: TextView
    private lateinit var tvLockStatus: TextView
    private lateinit var tvEngineStatus: TextView
    private lateinit var tvHvacStatus: TextView
    private lateinit var tvWindowStatus: TextView
    private lateinit var tvMileage: TextView
    private lateinit var tvFuelLevel: TextView
    private lateinit var tvUpdatedTime: TextView

    private lateinit var btnRefreshState: Button
    private lateinit var btnControlPlaceholder: Button
    private lateinit var btnRealtimePlaceholder: Button
    private lateinit var btnLogout: Button

    private lateinit var viewModel: HomeViewModel

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

        initViews(view)
        initListeners()
        observeUiState()
    }

    private fun initViews(view: View) {
        tvWelcome = view.findViewById(R.id.tvWelcome)
        tvUserInfo = view.findViewById(R.id.tvUserInfo)
        vehicleContainer = view.findViewById(R.id.vehicleContainer)
        tvEmptyTip = view.findViewById(R.id.tvEmptyTip)

        tvSelectedVehicleTitle = view.findViewById(R.id.tvSelectedVehicleTitle)
        tvBrandModel = view.findViewById(R.id.tvBrandModel)
        tvOnlineStatus = view.findViewById(R.id.tvOnlineStatus)
        tvLockStatus = view.findViewById(R.id.tvLockStatus)
        tvEngineStatus = view.findViewById(R.id.tvEngineStatus)
        tvHvacStatus = view.findViewById(R.id.tvHvacStatus)
        tvWindowStatus = view.findViewById(R.id.tvWindowStatus)
        tvMileage = view.findViewById(R.id.tvMileage)
        tvFuelLevel = view.findViewById(R.id.tvFuelLevel)
        tvUpdatedTime = view.findViewById(R.id.tvUpdatedTime)

        btnRefreshState = view.findViewById(R.id.btnRefreshState)
        btnControlPlaceholder = view.findViewById(R.id.btnControlPlaceholder)
        btnRealtimePlaceholder = view.findViewById(R.id.btnRealtimePlaceholder)
        btnLogout = view.findViewById(R.id.btnLogout)
    }

    private fun initListeners() {
        btnRefreshState.setOnClickListener {
            viewModel.refreshSelectedVehicleState()
        }

        btnControlPlaceholder.setOnClickListener {
            startActivity(Intent(requireContext(), ControlActivity::class.java))
        }

        btnRealtimePlaceholder.setOnClickListener {
            Toast.makeText(requireContext(), "实时推送下一步再做", Toast.LENGTH_SHORT).show()
        }

        btnLogout.setOnClickListener {
            viewModel.logout()
        }
    }

    private fun observeUiState() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            tvWelcome.text = "欢迎使用 CarControlAPP"
            tvUserInfo.text = "当前登录用户：${state.username}"

            renderVehicleList(state)
            renderSelectedVehicleState(state)

            tvEmptyTip.visibility = if (state.vehicles.isEmpty()) View.VISIBLE else View.GONE
            tvEmptyTip.text = state.emptyMessage ?: ""

            state.errorMessage?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }

            if (state.loggedOut) {
                val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
            }
        }
    }

    private fun renderVehicleList(state: HomeUiState) {
        vehicleContainer.removeAllViews()

        val inflater = LayoutInflater.from(requireContext())

        state.vehicles.forEach { vehicle ->
            val itemView = inflater.inflate(R.layout.item_vehicle, vehicleContainer, false)
            val root = itemView.findViewById<LinearLayout>(R.id.rootVehicleItem)
            val tvVehicleName = itemView.findViewById<TextView>(R.id.tvVehicleName)
            val tvVehicleSub = itemView.findViewById<TextView>(R.id.tvVehicleSub)
            val tvSelectedBadge = itemView.findViewById<TextView>(R.id.tvSelectedBadge)

            tvVehicleName.text = vehicle.name.ifBlank { vehicle.vehicleId }

            val onlineText = if (vehicle.onlineStatus == 1) "在线" else "离线"
            val brandModelText = listOfNotNull(vehicle.brand, vehicle.model)
                .filter { it.isNotBlank() }
                .joinToString(" / ")
                .ifBlank { "未命名车型" }

            tvVehicleSub.text = "$brandModelText · $onlineText"

            val isSelected = state.selectedVehicleId == vehicle.vehicleId
            tvSelectedBadge.visibility = if (isSelected) View.VISIBLE else View.GONE

            if (isSelected) {
                root.setBackgroundColor(Color.parseColor("#DFF3E4"))
            } else {
                root.setBackgroundColor(Color.parseColor("#F5F5F5"))
            }

            root.setOnClickListener {
                viewModel.selectVehicle(vehicle.vehicleId)
            }

            vehicleContainer.addView(itemView)
        }
    }

    private fun renderSelectedVehicleState(state: HomeUiState) {
        val selectedState = state.selectedState
        val selectedVehicle = state.vehicles.firstOrNull { it.vehicleId == state.selectedVehicleId }

        val title = selectedState?.name?.ifBlank { null }
            ?: selectedVehicle?.name?.ifBlank { null }
            ?: state.selectedVehicleId
            ?: "暂无车辆"

        tvSelectedVehicleTitle.text = "当前车辆：$title"

        val brand = selectedState?.brand ?: selectedVehicle?.brand
        val model = selectedState?.model ?: selectedVehicle?.model
        tvBrandModel.text = "品牌 / 型号：${brand ?: "-"} / ${model ?: "-"}"

        val onlineStatus = when (selectedState?.onlineStatus ?: selectedVehicle?.onlineStatus) {
            1 -> "在线"
            0 -> "离线"
            else -> "-"
        }
        tvOnlineStatus.text = "在线状态：$onlineStatus"
        tvLockStatus.text = "锁状态：${selectedState?.lockStatus ?: selectedVehicle?.lockStatus ?: "-"}"
        tvEngineStatus.text = "发动机：${selectedState?.engineStatus ?: selectedVehicle?.engineStatus ?: "-"}"
        tvHvacStatus.text = "空调：${selectedState?.hvacStatus ?: selectedVehicle?.hvacStatus ?: "-"}"
        tvWindowStatus.text = "车窗：${selectedState?.windowStatus ?: selectedVehicle?.windowStatus ?: "-"}"
        tvMileage.text = "里程：${selectedState?.mileage ?: selectedVehicle?.mileage ?: "-"} km"
        tvFuelLevel.text = "油量：${selectedState?.fuelLevel ?: selectedVehicle?.fuelLevel ?: "-"}%"
        tvUpdatedTime.text = "更新时间：${selectedState?.updatedTime ?: selectedVehicle?.updatedTime ?: "-"}"
    }
}