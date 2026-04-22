package com.example.app.ui.auth.forget

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
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

class ForgetPasswordActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PREFILL_PHONE = "extra_prefill_phone"
    }

    private lateinit var etPhone: EditText
    private lateinit var etCode: EditText
    private lateinit var btnSendCode: Button
    private lateinit var etNewPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var tvToggleNewPassword: TextView
    private lateinit var tvToggleConfirmPassword: TextView
    private lateinit var btnResetPassword: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvBackLogin: TextView

    private var newPasswordVisible = false
    private var confirmPasswordVisible = false

    private val viewModel: ForgetPasswordViewModel by viewModels {
        val tokenStore = NetworkModule.provideTokenStore(applicationContext)
        val authInterceptor = NetworkModule.provideAuthInterceptor(tokenStore)
        val loggingInterceptor = NetworkModule.provideLoggingInterceptor()
        val okHttpClient = NetworkModule.provideOkHttpClient(authInterceptor, loggingInterceptor)
        val retrofit = NetworkModule.provideRetrofit(okHttpClient)
        val authApi = NetworkModule.provideAuthApi(retrofit)
        val authRepository = AuthRepository(authApi, tokenStore)
        ForgetPasswordViewModel.Factory(authRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forget_password)

        initViews()
        initListeners()
        observeUiState()
    }

    private fun initViews() {
        etPhone = findViewById(R.id.etPhone)
        etCode = findViewById(R.id.etCode)
        btnSendCode = findViewById(R.id.btnSendCode)
        etNewPassword = findViewById(R.id.etNewPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        tvToggleNewPassword = findViewById(R.id.tvToggleNewPassword)
        tvToggleConfirmPassword = findViewById(R.id.tvToggleConfirmPassword)
        btnResetPassword = findViewById(R.id.btnResetPassword)
        progressBar = findViewById(R.id.progressBar)
        tvBackLogin = findViewById(R.id.tvBackLogin)
    }

    private fun initListeners() {
        btnSendCode.setOnClickListener {
            viewModel.sendResetCode(etPhone.text.toString())
        }

        btnResetPassword.setOnClickListener {
            viewModel.resetPassword(
                phone = etPhone.text.toString(),
                code = etCode.text.toString(),
                newPassword = etNewPassword.text.toString(),
                confirmPassword = etConfirmPassword.text.toString()
            )
        }

        tvBackLogin.setOnClickListener {
            finish()
        }

        tvToggleNewPassword.setOnClickListener {
            newPasswordVisible = !newPasswordVisible
            if (newPasswordVisible) {
                etNewPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
                tvToggleNewPassword.text = "隐藏"
            } else {
                etNewPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                tvToggleNewPassword.text = "显示"
            }
            etNewPassword.setSelection(etNewPassword.text.length)
        }

        tvToggleConfirmPassword.setOnClickListener {
            confirmPasswordVisible = !confirmPasswordVisible
            if (confirmPasswordVisible) {
                etConfirmPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
                tvToggleConfirmPassword.text = "隐藏"
            } else {
                etConfirmPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                tvToggleConfirmPassword.text = "显示"
            }
            etConfirmPassword.setSelection(etConfirmPassword.text.length)
        }
    }

    private fun observeUiState() {
        viewModel.uiState.observe(this) { state ->
            progressBar.visibility = if (state.isResetting) View.VISIBLE else View.GONE
            btnResetPassword.isEnabled = !state.isResetting

            btnSendCode.isEnabled = !state.isSendingCode && state.countdownSeconds == 0
            btnSendCode.text = when {
                state.isSendingCode -> "发送中..."
                state.countdownSeconds > 0 -> "${state.countdownSeconds}s 后重试"
                else -> "发送验证码"
            }

            state.errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }

            state.sendCodeSuccessMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.clearSendCodeSuccessMessage()
            }

            if (state.resetSuccess) {
                Toast.makeText(
                    this,
                    state.successMessage ?: "密码重置成功",
                    Toast.LENGTH_SHORT
                ).show()

                val phone = etPhone.text.toString().trim()
                viewModel.clearResetSuccess()

                Handler(Looper.getMainLooper()).postDelayed({
                    setResult(
                        RESULT_OK,
                        Intent().putExtra(EXTRA_PREFILL_PHONE, phone)
                    )
                    finish()
                }, 1000)
            }
        }
    }
}