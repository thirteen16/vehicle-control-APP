package com.example.app.ui.profile

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
import com.example.app.data.local.TokenStore
import com.example.app.data.repository.AuthRepository
import com.example.app.data.repository.VehicleRepository
import com.example.app.di.NetworkModule
import com.example.app.ui.auth.forget.ForgetPasswordActivity
import com.example.app.ui.auth.login.LoginActivity
import com.example.app.ui.pin.PinSetupActivity

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private lateinit var progressBar: ProgressBar
    private lateinit var tvNickname: TextView
    private lateinit var tvUsername: TextView
    private lateinit var tvPhone: TextView
    private lateinit var tvRole: TextView
    private lateinit var tvUserId: TextView
    private lateinit var tvSelectedVehicle: TextView
    private lateinit var tvPinStatus: TextView
    private lateinit var btnRefreshProfile: Button
    private lateinit var btnResetPassword: Button
    private lateinit var btnManagePin: Button
    private lateinit var btnLogout: Button

    private lateinit var viewModel: ProfileViewModel
    private lateinit var pinStore: PinStore

    private val resetPasswordLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val phone = result.data?.getStringExtra(ForgetPasswordActivity.EXTRA_PREFILL_PHONE)
            if (!phone.isNullOrBlank()) {
                Toast.makeText(
                    requireContext(),
                    "密码重置成功，请使用新密码登录",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    private val pinManageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            refreshPinStatus()
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val appContext = requireContext().applicationContext

        val tokenStore: TokenStore = NetworkModule.provideTokenStore(appContext)
        val authInterceptor = NetworkModule.provideAuthInterceptor(tokenStore)
        val loggingInterceptor = NetworkModule.provideLoggingInterceptor()
        val okHttpClient = NetworkModule.provideOkHttpClient(authInterceptor, loggingInterceptor)
        val retrofit = NetworkModule.provideRetrofit(okHttpClient)

        val authApi = NetworkModule.provideAuthApi(retrofit)
        val vehicleApi = NetworkModule.provideVehicleApi(retrofit)

        val authRepository = AuthRepository(authApi, tokenStore)
        val selectedVehicleStore = SelectedVehicleStore(appContext)
        val vehicleRepository = VehicleRepository(vehicleApi, selectedVehicleStore)

        pinStore = PinStore(appContext)

        viewModel = ViewModelProvider(
            this,
            ProfileViewModel.Factory(
                authRepository = authRepository,
                vehicleRepository = vehicleRepository,
                selectedVehicleStore = selectedVehicleStore
            )
        )[ProfileViewModel::class.java]

        initViews(view)
        initListeners()
        observeUiState()
        refreshPinStatus()
    }

    override fun onResume() {
        super.onResume()
        refreshPinStatus()
    }

    private fun initViews(view: View) {
        progressBar = view.findViewById(R.id.progressBar)
        tvNickname = view.findViewById(R.id.tvNickname)
        tvUsername = view.findViewById(R.id.tvUsername)
        tvPhone = view.findViewById(R.id.tvPhone)
        tvRole = view.findViewById(R.id.tvRole)
        tvUserId = view.findViewById(R.id.tvUserId)
        tvSelectedVehicle = view.findViewById(R.id.tvSelectedVehicle)
        tvPinStatus = view.findViewById(R.id.tvPinStatus)
        btnRefreshProfile = view.findViewById(R.id.btnRefreshProfile)
        btnResetPassword = view.findViewById(R.id.btnResetPassword)
        btnManagePin = view.findViewById(R.id.btnManagePin)
        btnLogout = view.findViewById(R.id.btnLogout)
    }

    private fun initListeners() {
        btnRefreshProfile.setOnClickListener {
            viewModel.loadProfile()
        }

        btnResetPassword.setOnClickListener {
            val phone = viewModel.uiState.value?.phone.orEmpty()
            resetPasswordLauncher.launch(
                Intent(requireContext(), ForgetPasswordActivity::class.java).apply {
                    putExtra(ForgetPasswordActivity.EXTRA_PREFILL_PHONE, phone)
                }
            )
        }

        btnManagePin.setOnClickListener {
            pinManageLauncher.launch(
                Intent(requireContext(), PinSetupActivity::class.java)
            )
        }

        btnLogout.setOnClickListener {
            viewModel.logout()
        }
    }

    private fun refreshPinStatus() {
        tvPinStatus.text = if (pinStore.hasPin()) {
            "PIN 状态：已设置"
        } else {
            "PIN 状态：未设置"
        }
    }

    private fun observeUiState() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

            val nickname = state.nickname.ifBlank { "未设置昵称" }
            val username = state.username.ifBlank { "-" }
            val phone = state.phone.ifBlank { "-" }
            val role = state.role.ifBlank { "-" }
            val userId = state.userId?.toString() ?: "-"
            val vehicleText = when {
                state.selectedVehicleName.isNotBlank() && state.selectedVehicleId.isNotBlank() ->
                    "${state.selectedVehicleName} (${state.selectedVehicleId})"
                state.selectedVehicleId.isNotBlank() ->
                    state.selectedVehicleId
                else -> "未选择车辆"
            }

            tvNickname.text = nickname
            tvUsername.text = "用户名：$username"
            tvPhone.text = "手机号：$phone"
            tvRole.text = "角色：$role"
            tvUserId.text = "用户ID：$userId"
            tvSelectedVehicle.text = "当前车辆：$vehicleText"

            state.infoMessage?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearInfoMessage()
            }

            state.errorMessage?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearErrorMessage()
            }

            if (state.loggedOut) {
                val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
            }
        }
    }
}