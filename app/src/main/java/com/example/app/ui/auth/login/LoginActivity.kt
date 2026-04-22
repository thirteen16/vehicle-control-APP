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
    private lateinit var btnLogin: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvForgetPassword: TextView
    private lateinit var tvGoRegister: TextView

    private var passwordVisible = false
    private var prefillApplied = false

    private val registerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val account = result.data?.getStringExtra(RegisterActivity.EXTRA_PREFILL_ACCOUNT)
            if (!account.isNullOrBlank()) {
                etAccount.setText(account)
                etAccount.setSelection(account.length)
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
                rememberPassword = cbRememberPassword.isChecked
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
    }

    private fun observeUiState() {
        viewModel.uiState.observe(this) { state ->
            if (state.rememberedInfoLoaded && !prefillApplied) {
                etAccount.setText(state.rememberedAccount)
                etPassword.setText(state.rememberedPassword)
                cbRememberPassword.isChecked = state.rememberPassword

                if (state.rememberedAccount.isNotEmpty()) {
                    etAccount.setSelection(state.rememberedAccount.length)
                }

                prefillApplied = true
            }

            progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
            btnLogin.isEnabled = !state.isLoading

            state.errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
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