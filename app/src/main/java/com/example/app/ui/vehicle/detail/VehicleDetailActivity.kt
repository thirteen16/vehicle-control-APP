package com.example.app.ui.vehicle.detail

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.app.R
import com.example.app.data.local.SelectedVehicleStore
import com.example.app.data.repository.VehicleRepository
import com.example.app.di.NetworkModule
import com.example.app.ui.vehicle.location.VehicleLocationActivity

class VehicleDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_VEHICLE_ID = "extra_vehicle_id"
    }

    private lateinit var tvBack: TextView
    private lateinit var progressBar: ProgressBar

    private lateinit var tvTitle: TextView
    private lateinit var tvVehicleId: TextView
    private lateinit var tvCurrentBadge: TextView
    private lateinit var tvBrandModel: TextView
    private lateinit var tvOnlineStatus: TextView
    private lateinit var tvLockStatus: TextView
    private lateinit var tvEngineStatus: TextView
    private lateinit var tvHvacStatus: TextView
    private lateinit var tvWindowStatus: TextView
    private lateinit var tvMileage: TextView
    private lateinit var tvFuelLevel: TextView
    private lateinit var tvUpdatedTime: TextView

    private lateinit var btnSetCurrentVehicle: Button
    private lateinit var btnViewLocation: Button
    private lateinit var btnRefresh: Button

    private lateinit var viewModel: VehicleDetailViewModel
    private lateinit var vehicleId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vehicle_detail)

        vehicleId = intent.getStringExtra(EXTRA_VEHICLE_ID).orEmpty()
        if (vehicleId.isBlank()) {
            Toast.makeText(this, "缺少 vehicleId", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val appContext = applicationContext
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
            VehicleDetailViewModel.Factory(vehicleRepository)
        )[VehicleDetailViewModel::class.java]

        initViews()
        initListeners()
        observeUiState()

        viewModel.loadVehicleDetail(vehicleId)
    }

    private fun initViews() {
        tvBack = findViewById(R.id.tvBack)
        progressBar = findViewById(R.id.progressBar)

        tvTitle = findViewById(R.id.tvTitle)
        tvVehicleId = findViewById(R.id.tvVehicleId)
        tvCurrentBadge = findViewById(R.id.tvCurrentBadge)
        tvBrandModel = findViewById(R.id.tvBrandModel)
        tvOnlineStatus = findViewById(R.id.tvOnlineStatus)
        tvLockStatus = findViewById(R.id.tvLockStatus)
        tvEngineStatus = findViewById(R.id.tvEngineStatus)
        tvHvacStatus = findViewById(R.id.tvHvacStatus)
        tvWindowStatus = findViewById(R.id.tvWindowStatus)
        tvMileage = findViewById(R.id.tvMileage)
        tvFuelLevel = findViewById(R.id.tvFuelLevel)
        tvUpdatedTime = findViewById(R.id.tvUpdatedTime)

        btnSetCurrentVehicle = findViewById(R.id.btnSetCurrentVehicle)
        btnViewLocation = findViewById(R.id.btnViewLocation)
        btnRefresh = findViewById(R.id.btnRefresh)
    }

    private fun initListeners() {
        tvBack.setOnClickListener {
            finish()
        }

        btnSetCurrentVehicle.setOnClickListener {
            viewModel.setAsCurrentVehicle()
        }

        btnViewLocation.setOnClickListener {
            startActivity(
                Intent(this, VehicleLocationActivity::class.java).apply {
                    putExtra(VehicleLocationActivity.EXTRA_VEHICLE_ID, vehicleId)
                }
            )
        }

        btnRefresh.setOnClickListener {
            viewModel.loadVehicleDetail(vehicleId)
        }
    }

    private fun observeUiState() {
        viewModel.uiState.observe(this) { state ->
            progressBar.visibility = if (state.isLoading) android.view.View.VISIBLE else android.view.View.GONE

            val detail = state.vehicleState
            val title = detail?.name?.ifBlank { state.vehicleId } ?: state.vehicleId

            tvTitle.text = title
            tvVehicleId.text = "vehicleId：${state.vehicleId}"
            tvCurrentBadge.text = if (state.isCurrentVehicle) "当前选中车辆" else "未设为当前车辆"

            val brand = detail?.brand ?: "-"
            val model = detail?.model ?: "-"
            tvBrandModel.text = "品牌 / 型号：$brand / $model"

            val onlineText = when (detail?.onlineStatus) {
                1 -> "在线"
                0 -> "离线"
                else -> "-"
            }

            tvOnlineStatus.text = "在线状态：$onlineText"
            tvLockStatus.text = "车锁状态：${detail?.lockStatus ?: "-"}"
            tvEngineStatus.text = "发动机：${detail?.engineStatus ?: "-"}"
            tvHvacStatus.text = "空调：${detail?.hvacStatus ?: "-"}"
            tvWindowStatus.text = "车窗：${detail?.windowStatus ?: "-"}"
            tvMileage.text = "里程：${detail?.mileage ?: "-"} km"
            tvFuelLevel.text = "油量：${detail?.fuelLevel ?: "-"}%"
            tvUpdatedTime.text = "更新时间：${detail?.updatedTime ?: "-"}"

            btnSetCurrentVehicle.isEnabled = !state.isCurrentVehicle
            btnSetCurrentVehicle.text = if (state.isCurrentVehicle) "已是当前车辆" else "设为当前车辆"

            state.infoMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.clearInfoMessage()
            }

            state.errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.clearErrorMessage()
            }
        }
    }
}