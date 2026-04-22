package com.example.app.ui.auth.register

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.app.R
import com.example.app.data.repository.AuthRepository
import com.example.app.di.NetworkModule

class RegisterActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PREFILL_ACCOUNT = "extra_prefill_account"
    }

    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var etPhone: EditText
    private lateinit var etNickname: EditText
    private lateinit var btnRegister: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvGoLogin: TextView

    private val viewModel: RegisterViewModel by viewModels {
        val tokenStore = NetworkModule.provideTokenStore(applicationContext)
        val authInterceptor = NetworkModule.provideAuthInterceptor(tokenStore)
        val loggingInterceptor = NetworkModule.provideLoggingInterceptor()
        val okHttpClient = NetworkModule.provideOkHttpClient(authInterceptor, loggingInterceptor)
        val retrofit = NetworkModule.provideRetrofit(okHttpClient)
        val authApi = NetworkModule.provideAuthApi(retrofit)
        val authRepository = AuthRepository(authApi, tokenStore)
        RegisterViewModel.Factory(authRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        initViews()
        initListeners()
        observeUiState()
    }

    private fun initViews() {
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        etPhone = findViewById(R.id.etPhone)
        etNickname = findViewById(R.id.etNickname)
        btnRegister = findViewById(R.id.btnRegister)
        progressBar = findViewById(R.id.progressBar)
        tvGoLogin = findViewById(R.id.tvGoLogin)
    }

    private fun initListeners() {
        btnRegister.setOnClickListener {
            viewModel.register(
                username = etUsername.text.toString(),
                password = etPassword.text.toString(),
                confirmPassword = etConfirmPassword.text.toString(),
                phone = etPhone.text.toString(),
                nickname = etNickname.text.toString()
            )
        }

        tvGoLogin.setOnClickListener {
            finish()
        }
    }

    private fun observeUiState() {
        viewModel.uiState.observe(this) { state ->
            progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
            btnRegister.isEnabled = !state.isLoading

            state.errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }

            if (state.registerSuccess) {
                Toast.makeText(
                    this,
                    state.successMessage ?: "注册成功",
                    Toast.LENGTH_SHORT
                ).show()

                Handler(Looper.getMainLooper()).postDelayed({
                    setResult(
                        RESULT_OK,
                        Intent().putExtra(
                            EXTRA_PREFILL_ACCOUNT,
                            state.registeredAccount ?: etUsername.text.toString().trim()
                        )
                    )
                    finish()
                }, 1000)
            }
        }
    }
}