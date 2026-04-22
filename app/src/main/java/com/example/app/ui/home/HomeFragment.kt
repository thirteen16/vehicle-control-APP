package com.example.app.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.app.R
import com.example.app.di.NetworkModule
import com.example.app.ui.auth.login.LoginActivity

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var tvWelcome: TextView
    private lateinit var tvUserInfo: TextView
    private lateinit var btnVehiclePlaceholder: Button
    private lateinit var btnControlPlaceholder: Button
    private lateinit var btnRealtimePlaceholder: Button
    private lateinit var btnLogout: Button

    private lateinit var viewModel: HomeViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tokenStore = NetworkModule.provideTokenStore(requireContext().applicationContext)
        viewModel = ViewModelProvider(this, HomeViewModel.Factory(tokenStore))[HomeViewModel::class.java]

        initViews(view)
        initListeners()
        observeUiState()
    }

    private fun initViews(view: View) {
        tvWelcome = view.findViewById(R.id.tvWelcome)
        tvUserInfo = view.findViewById(R.id.tvUserInfo)
        btnVehiclePlaceholder = view.findViewById(R.id.btnVehiclePlaceholder)
        btnControlPlaceholder = view.findViewById(R.id.btnControlPlaceholder)
        btnRealtimePlaceholder = view.findViewById(R.id.btnRealtimePlaceholder)
        btnLogout = view.findViewById(R.id.btnLogout)
    }

    private fun initListeners() {
        btnVehiclePlaceholder.setOnClickListener {
            Toast.makeText(requireContext(), "车辆列表下一步再做", Toast.LENGTH_SHORT).show()
        }

        btnControlPlaceholder.setOnClickListener {
            Toast.makeText(requireContext(), "远程控制下一步再做", Toast.LENGTH_SHORT).show()
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
}