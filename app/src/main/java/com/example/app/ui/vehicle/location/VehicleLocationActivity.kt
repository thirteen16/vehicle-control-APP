package com.example.app.ui.vehicle.location

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
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.util.Locale

class VehicleLocationActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_VEHICLE_ID = "extra_vehicle_id"
    }

    private lateinit var tvBack: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvTitle: TextView
    private lateinit var tvVehicleId: TextView
    private lateinit var tvOnlineStatus: TextView
    private lateinit var tvLongitude: TextView
    private lateinit var tvLatitude: TextView
    private lateinit var tvUpdatedTime: TextView
    private lateinit var btnRefresh: Button
    private lateinit var btnCenterToVehicle: Button
    private lateinit var mapView: MapView

    private lateinit var viewModel: VehicleLocationViewModel
    private lateinit var vehicleId: String

    private var vehicleMarker: Marker? = null
    private var latestVehiclePoint: GeoPoint? = null
    private var firstMapCenterDone = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().load(
            applicationContext,
            getSharedPreferences("osmdroid_config", MODE_PRIVATE)
        )
        Configuration.getInstance().userAgentValue = packageName

        setContentView(R.layout.activity_vehicle_location)

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
            VehicleLocationViewModel.Factory(vehicleRepository)
        )[VehicleLocationViewModel::class.java]

        initViews()
        initMap()
        initListeners()
        observeUiState()

        viewModel.loadVehicleLocation(vehicleId)
    }

    private fun initViews() {
        tvBack = findViewById(R.id.tvBack)
        progressBar = findViewById(R.id.progressBar)
        tvTitle = findViewById(R.id.tvTitle)
        tvVehicleId = findViewById(R.id.tvVehicleId)
        tvOnlineStatus = findViewById(R.id.tvOnlineStatus)
        tvLongitude = findViewById(R.id.tvLongitude)
        tvLatitude = findViewById(R.id.tvLatitude)
        tvUpdatedTime = findViewById(R.id.tvUpdatedTime)
        btnRefresh = findViewById(R.id.btnRefresh)
        btnCenterToVehicle = findViewById(R.id.btnCenterToVehicle)
        mapView = findViewById(R.id.mapView)
    }

    private fun initMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.zoomController.setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT)
        mapView.controller.setZoom(15.0)
    }

    private fun initListeners() {
        tvBack.setOnClickListener {
            finish()
        }

        btnRefresh.setOnClickListener {
            viewModel.loadVehicleLocation(vehicleId)
        }

        btnCenterToVehicle.setOnClickListener {
            val point = latestVehiclePoint
            if (point == null) {
                Toast.makeText(this, "当前暂无车辆坐标", Toast.LENGTH_SHORT).show()
            } else {
                mapView.controller.animateTo(point)
                mapView.controller.setZoom(17.0)
            }
        }
    }

    private fun observeUiState() {
        viewModel.uiState.observe(this) { state ->
            progressBar.visibility = if (state.isLoading) android.view.View.VISIBLE else android.view.View.GONE

            val data = state.location
            val onlineText = when (data?.onlineStatus) {
                1 -> "在线"
                0 -> "离线"
                else -> "-"
            }

            tvTitle.text = data?.name?.ifBlank { state.vehicleId } ?: state.vehicleId
            tvVehicleId.text = "vehicleId：${state.vehicleId}"
            tvOnlineStatus.text = "在线状态：$onlineText"
            tvLongitude.text = "经度：${formatCoordinate(data?.longitude)}"
            tvLatitude.text = "纬度：${formatCoordinate(data?.latitude)}"
            tvUpdatedTime.text = "更新时间：${data?.updatedTime ?: "-"}"

            updateMapMarker(
                title = data?.name?.ifBlank { state.vehicleId } ?: state.vehicleId,
                longitude = data?.longitude,
                latitude = data?.latitude,
                updatedTime = data?.updatedTime
            )

            state.errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.clearErrorMessage()
            }
        }
    }

    private fun updateMapMarker(
        title: String,
        longitude: Double?,
        latitude: Double?,
        updatedTime: String?
    ) {
        if (longitude == null || latitude == null) {
            latestVehiclePoint = null
            if (vehicleMarker != null) {
                mapView.overlays.remove(vehicleMarker)
                vehicleMarker = null
                mapView.invalidate()
            }
            return
        }

        val point = GeoPoint(latitude, longitude)
        latestVehiclePoint = point

        val marker = vehicleMarker ?: Marker(mapView).also { newMarker ->
            newMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            mapView.overlays.add(newMarker)
            vehicleMarker = newMarker
        }

        marker.position = point
        marker.title = title
        marker.subDescription = "更新时间：${updatedTime ?: "-"}"

        if (!firstMapCenterDone) {
            mapView.controller.setCenter(point)
            mapView.controller.setZoom(17.0)
            firstMapCenterDone = true
        }

        mapView.invalidate()
    }

    private fun formatCoordinate(value: Double?): String {
        return if (value == null) "-" else String.format(Locale.US, "%.6f", value)
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        mapView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        mapView.overlays.clear()
        mapView.onDetach()
        super.onDestroy()
    }
}