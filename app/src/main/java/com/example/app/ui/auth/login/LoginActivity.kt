package com.example.app.ui.auth.login

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.app.MainActivity
import com.example.app.R
import com.example.app.data.repository.AuthRepository
import com.example.app.di.NetworkModule
import com.example.app.ui.auth.forget.ForgetPasswordActivity
import com.example.app.ui.auth.register.RegisterActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var etAccount: EditText
    private lateinit var etPassword: EditText
    private lateinit var tvTogglePassword: TextView
    private lateinit var cbRememberPassword: CheckBox
    private lateinit var cbAutoLogin: CheckBox
    private lateinit var btnLogin: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvForgetPassword: TextView
    private lateinit var tvGoRegister: TextView

    private var passwordVisible = false
    private var prefillApplied = false

    private val registerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val account = result.data?.getStringExtra(RegisterActivity.EXTRA_PREFILL_ACCOUNT)
            val password = result.data?.getStringExtra(RegisterActivity.EXTRA_PREFILL_PASSWORD)

            if (!account.isNullOrBlank()) {
                etAccount.setText(account)
                etAccount.setSelection(account.length)
            }

            if (!password.isNullOrBlank()) {
                etPassword.setText(password)
                etPassword.setSelection(password.length)
            }

            if (!account.isNullOrBlank() || !password.isNullOrBlank()) {
                cbRememberPassword.isChecked = false
                cbAutoLogin.isChecked = false
                prefillApplied = true
            }
        }

    private val forgetPasswordLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val phone = result.data?.getStringExtra(ForgetPasswordActivity.EXTRA_PREFILL_PHONE)

            if (!phone.isNullOrBlank()) {
                etAccount.setText(phone)
                etAccount.setSelection(phone.length)
                etPassword.setText("")
                cbRememberPassword.isChecked = false
                cbAutoLogin.isChecked = false
                prefillApplied = true
            }
        }

    private val viewModel: LoginViewModel by viewModels {
        val tokenStore = NetworkModule.provideTokenStore(applicationContext)
        val authInterceptor = NetworkModule.provideAuthInterceptor(tokenStore)
        val loggingInterceptor = NetworkModule.provideLoggingInterceptor()
        val okHttpClient = NetworkModule.provideOkHttpClient(authInterceptor, loggingInterceptor)
        val retrofit = NetworkModule.provideRetrofit(okHttpClient)
        val authApi = NetworkModule.provideAuthApi(retrofit)
        val authRepository = AuthRepository(authApi, tokenStore)

        LoginViewModel.Factory(authRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        initViews()
        initListeners()
        observeUiState()

        viewModel.checkAutoLogin()
    }

    private fun initViews() {
        etAccount = findViewById(R.id.etAccount)
        etPassword = findViewById(R.id.etPassword)
        tvTogglePassword = findViewById(R.id.tvTogglePassword)
        cbRememberPassword = findViewById(R.id.cbRememberPassword)
        cbAutoLogin = findViewById(R.id.cbAutoLogin)
        btnLogin = findViewById(R.id.btnLogin)
        progressBar = findViewById(R.id.progressBar)
        tvForgetPassword = findViewById(R.id.tvForgetPassword)
        tvGoRegister = findViewById(R.id.tvGoRegister)
    }

    private fun initListeners() {
        btnLogin.setOnClickListener {
            viewModel.login(
                account = etAccount.text.toString(),
                password = etPassword.text.toString(),
                rememberPassword = cbRememberPassword.isChecked,
                autoLoginEnabled = cbAutoLogin.isChecked
            )
        }

        tvGoRegister.setOnClickListener {
            registerLauncher.launch(Intent(this, RegisterActivity::class.java))
        }

        tvForgetPassword.setOnClickListener {
            forgetPasswordLauncher.launch(Intent(this, ForgetPasswordActivity::class.java))
        }

        tvTogglePassword.setOnClickListener {
            passwordVisible = !passwordVisible

            if (passwordVisible) {
                etPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
                tvTogglePassword.text = "隐藏"
            } else {
                etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                tvTogglePassword.text = "显示"
            }

            etPassword.setSelection(etPassword.text.length)
        }

        /**
         * 勾选自动登录时，必须自动勾选记住密码。
         *
         * 原因：
         * token 过期后，需要用保存的账号密码重新请求 login 接口，
         * 如果没有保存密码，就无法自动换新 token。
         */
        cbAutoLogin.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !cbRememberPassword.isChecked) {
                cbRememberPassword.isChecked = true
            }
        }

        /**
         * 如果用户手动取消记住密码，那么自动登录也必须取消。
         *
         * 因为自动登录依赖保存的密码。
         */
        cbRememberPassword.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked && cbAutoLogin.isChecked) {
                cbAutoLogin.isChecked = false
            }
        }
    }

    private fun observeUiState() {
        viewModel.uiState.observe(this) { state ->
            if (state.rememberedInfoLoaded && !prefillApplied) {
                etAccount.setText(state.rememberedAccount)
                etPassword.setText(state.rememberedPassword)

                cbRememberPassword.isChecked = state.rememberPassword || state.autoLoginEnabled
                cbAutoLogin.isChecked = state.autoLoginEnabled

                if (state.rememberedAccount.isNotEmpty()) {
                    etAccount.setSelection(state.rememberedAccount.length)
                }

                prefillApplied = true
            }

            if (!state.autoLoginEnabled && cbAutoLogin.isChecked && !state.autoLogin) {
                cbAutoLogin.isChecked = false
            }

            progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

            btnLogin.isEnabled = !state.isLoading
            etAccount.isEnabled = !state.isLoading
            etPassword.isEnabled = !state.isLoading
            cbRememberPassword.isEnabled = !state.isLoading
            cbAutoLogin.isEnabled = !state.isLoading
            tvForgetPassword.isEnabled = !state.isLoading
            tvGoRegister.isEnabled = !state.isLoading
            tvTogglePassword.isEnabled = !state.isLoading

            state.errorMessage?.let { message ->
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }

            if (state.autoLogin) {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
                return@observe
            }

            if (state.loginSuccess) {
                val displayName = state.loginData?.nickname
                    ?: state.loginData?.username
                    ?: etAccount.text.toString().trim()

                Toast.makeText(this, "登录成功，欢迎 $displayName", Toast.LENGTH_SHORT).show()

                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
    }
}