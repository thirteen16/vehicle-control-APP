package com.example.app.ui.vehicle.list

import android.content.Intent
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
import com.example.app.data.repository.VehicleRepository
import com.example.app.di.NetworkModule
import com.example.app.ui.main.MainTabState
import com.example.app.ui.main.MainViewModel
import com.example.app.ui.vehicle.detail.VehicleDetailActivity

class VehicleListFragment : Fragment(R.layout.fragment_vehicle_list) {

    private lateinit var progressBar: ProgressBar
    private lateinit var tvCurrentVehicle: TextView
    private lateinit var tvVehicleCount: TextView
    private lateinit var tvEmptyTip: TextView
    private lateinit var vehicleContainer: LinearLayout
    private lateinit var btnRefreshVehicles: Button
    private lateinit var btnGoControl: Button

    private lateinit var viewModel: VehicleListViewModel
    private lateinit var mainViewModel: MainViewModel

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
            VehicleListViewModel.Factory(vehicleRepository)
        )[VehicleListViewModel::class.java]

        mainViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        initViews(view)
        initListeners()
        observeUiState()
    }

    private fun initViews(view: View) {
        progressBar = view.findViewById(R.id.progressBar)
        tvCurrentVehicle = view.findViewById(R.id.tvCurrentVehicle)
        tvVehicleCount = view.findViewById(R.id.tvVehicleCount)
        tvEmptyTip = view.findViewById(R.id.tvEmptyTip)
        vehicleContainer = view.findViewById(R.id.vehicleContainer)
        btnRefreshVehicles = view.findViewById(R.id.btnRefreshVehicles)
        btnGoControl = view.findViewById(R.id.btnGoControl)
    }

    private fun initListeners() {
        btnRefreshVehicles.setOnClickListener {
            viewModel.loadVehicles()
        }

        btnGoControl.setOnClickListener {
            val selectedVehicleId = viewModel.uiState.value?.selectedVehicleId
            if (selectedVehicleId.isNullOrBlank()) {
                Toast.makeText(requireContext(), "请先选择一辆车", Toast.LENGTH_SHORT).show()
            } else {
                mainViewModel.selectTab(MainTabState.CONTROL)
            }
        }
    }

    private fun observeUiState() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

            val selectedVehicle = state.vehicles.firstOrNull { it.vehicleId == state.selectedVehicleId }
            val selectedName = selectedVehicle?.name?.ifBlank { state.selectedVehicleId ?: "未选择" }
                ?: state.selectedVehicleId
                ?: "未选择"

            tvCurrentVehicle.text = "当前车辆：$selectedName"
            tvVehicleCount.text = "车辆数量：${state.vehicles.size}"

            tvEmptyTip.visibility = if (state.vehicles.isEmpty()) View.VISIBLE else View.GONE
            vehicleContainer.visibility = if (state.vehicles.isEmpty()) View.GONE else View.VISIBLE

            renderVehicleList(state)

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

    private fun renderVehicleList(state: VehicleListUiState) {
        vehicleContainer.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())

        state.vehicles.forEach { vehicle ->
            val itemView = inflater.inflate(R.layout.item_vehicle_selectable, vehicleContainer, false)

            val root = itemView.findViewById<View>(R.id.rootVehicleItem)
            val tvVehicleName = itemView.findViewById<TextView>(R.id.tvVehicleName)
            val tvVehicleSub = itemView.findViewById<TextView>(R.id.tvVehicleSub)
            val tvStatusLine = itemView.findViewById<TextView>(R.id.tvStatusLine)
            val tvSelectedBadge = itemView.findViewById<TextView>(R.id.tvSelectedBadge)
            val btnSelectVehicle = itemView.findViewById<Button>(R.id.btnSelectVehicle)

            val displayName = vehicle.name.ifBlank { vehicle.vehicleId }
            val brandModel = listOfNotNull(vehicle.brand, vehicle.model)
                .filter { !it.isNullOrBlank() }
                .joinToString(" / ")
                .ifBlank { "未命名车型" }

            val onlineText = when (vehicle.onlineStatus) {
                1 -> "在线"
                0 -> "离线"
                else -> "-"
            }

            val lockText = vehicle.lockStatus ?: "-"
            val engineText = vehicle.engineStatus ?: "-"
            val hvacText = vehicle.hvacStatus ?: "-"
            val windowText = vehicle.windowStatus ?: "-"

            tvVehicleName.text = displayName
            tvVehicleSub.text = "vehicleId：${vehicle.vehicleId}\n品牌 / 型号：$brandModel"
            tvStatusLine.text = "在线：$onlineText   车锁：$lockText   发动机：$engineText\n空调：$hvacText   车窗：$windowText"

            val isSelected = vehicle.vehicleId == state.selectedVehicleId
            tvSelectedBadge.visibility = if (isSelected) View.VISIBLE else View.GONE
            root.setBackgroundResource(
                if (isSelected) R.drawable.bg_vehicle_card_selected
                else R.drawable.bg_vehicle_card_normal
            )

            btnSelectVehicle.text = if (isSelected) "当前车辆" else "切换为当前车辆"
            btnSelectVehicle.isEnabled = !isSelected

            root.setOnClickListener {
                startActivity(
                    Intent(requireContext(), VehicleDetailActivity::class.java).apply {
                        putExtra(VehicleDetailActivity.EXTRA_VEHICLE_ID, vehicle.vehicleId)
                    }
                )
            }

            btnSelectVehicle.setOnClickListener {
                viewModel.selectVehicle(vehicle.vehicleId)
            }

            vehicleContainer.addView(itemView)
        }
    }
}