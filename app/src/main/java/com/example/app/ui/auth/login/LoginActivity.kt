package com.example.app.ui.auth.login

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
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

    private lateinit var layoutPasswordLogin: LinearLayout
    private lateinit var layoutSmsLogin: LinearLayout
    private lateinit var layoutRememberOptions: LinearLayout
    private lateinit var layoutPasswordLinks: LinearLayout

    private lateinit var etAccount: EditText
    private lateinit var etPassword: EditText
    private lateinit var etPhone: EditText
    private lateinit var etSmsCode: EditText

    private lateinit var btnTogglePassword: ImageButton
    private lateinit var cbRememberPassword: CheckBox
    private lateinit var cbAutoLogin: CheckBox
    private lateinit var btnLogin: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvForgetPassword: TextView
    private lateinit var tvGoRegister: TextView
    private lateinit var tvSendLoginCode: TextView
    private lateinit var tvSwitchLoginMode: TextView

    private var passwordVisible = false
    private var smsLoginMode = false
    private var prefillApplied = false
    private var countDownTimer: CountDownTimer? = null

    private val registerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val account = result.data?.getStringExtra(RegisterActivity.EXTRA_PREFILL_ACCOUNT)
            val password = result.data?.getStringExtra(RegisterActivity.EXTRA_PREFILL_PASSWORD)

            switchLoginMode(false)

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

            switchLoginMode(false)

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
        switchLoginMode(false)

        viewModel.checkAutoLogin()
    }

    private fun initViews() {
        layoutPasswordLogin = findViewById(R.id.layoutPasswordLogin)
        layoutSmsLogin = findViewById(R.id.layoutSmsLogin)
        layoutRememberOptions = findViewById(R.id.layoutRememberOptions)
        layoutPasswordLinks = findViewById(R.id.layoutPasswordLinks)

        etAccount = findViewById(R.id.etAccount)
        etPassword = findViewById(R.id.etPassword)
        etPhone = findViewById(R.id.etPhone)
        etSmsCode = findViewById(R.id.etSmsCode)

        btnTogglePassword = findViewById(R.id.btnTogglePassword)
        cbRememberPassword = findViewById(R.id.cbRememberPassword)
        cbAutoLogin = findViewById(R.id.cbAutoLogin)
        btnLogin = findViewById(R.id.btnLogin)
        progressBar = findViewById(R.id.progressBar)
        tvForgetPassword = findViewById(R.id.tvForgetPassword)
        tvGoRegister = findViewById(R.id.tvGoRegister)
        tvSendLoginCode = findViewById(R.id.tvSendLoginCode)
        tvSwitchLoginMode = findViewById(R.id.tvSwitchLoginMode)
    }

    private fun initListeners() {
        btnLogin.setOnClickListener {
            if (smsLoginMode) {
                viewModel.smsLogin(
                    phone = etPhone.text.toString(),
                    code = etSmsCode.text.toString()
                )
            } else {
                viewModel.login(
                    account = etAccount.text.toString(),
                    password = etPassword.text.toString(),
                    rememberPassword = cbRememberPassword.isChecked,
                    autoLoginEnabled = cbAutoLogin.isChecked
                )
            }
        }

        tvSwitchLoginMode.setOnClickListener {
            switchLoginMode(!smsLoginMode)
        }

        tvSendLoginCode.setOnClickListener {
            val phone = etPhone.text.toString().trim()
            if (!isValidPhone(phone)) {
                Toast.makeText(this, "请输入正确的11位手机号", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.sendLoginCode(phone)
            startCodeCountDown()
        }

        tvGoRegister.setOnClickListener {
            registerLauncher.launch(Intent(this, RegisterActivity::class.java))
        }

        tvForgetPassword.setOnClickListener {
            forgetPasswordLauncher.launch(Intent(this, ForgetPasswordActivity::class.java))
        }

        btnTogglePassword.setOnClickListener {
            togglePasswordVisible()
        }

        cbAutoLogin.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !cbRememberPassword.isChecked) {
                cbRememberPassword.isChecked = true
            }
        }

        cbRememberPassword.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked && cbAutoLogin.isChecked) {
                cbAutoLogin.isChecked = false
            }
        }
    }

    private fun switchLoginMode(toSmsMode: Boolean) {
        smsLoginMode = toSmsMode

        if (toSmsMode) {
            layoutPasswordLogin.visibility = View.GONE
            layoutSmsLogin.visibility = View.VISIBLE
            btnLogin.text = "登录"
            tvSwitchLoginMode.text = "账号密码登录  >"

            val accountText = etAccount.text.toString().trim()
            if (etPhone.text.isNullOrBlank() && isValidPhone(accountText)) {
                etPhone.setText(accountText)
                etPhone.setSelection(accountText.length)
            }
        } else {
            layoutPasswordLogin.visibility = View.VISIBLE
            layoutSmsLogin.visibility = View.GONE
            btnLogin.text = "登录"
            tvSwitchLoginMode.text = "手机短信登录  >"

            val phoneText = etPhone.text.toString().trim()
            if (etAccount.text.isNullOrBlank() && isValidPhone(phoneText)) {
                etAccount.setText(phoneText)
                etAccount.setSelection(phoneText.length)
            }
        }
    }

    private fun togglePasswordVisible() {
        passwordVisible = !passwordVisible

        if (passwordVisible) {
            etPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
            btnTogglePassword.setImageResource(R.drawable.ic_eye_open_24)
        } else {
            etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
            btnTogglePassword.setImageResource(R.drawable.ic_eye_closed_24)
        }

        etPassword.setSelection(etPassword.text.length)
    }

    private fun startCodeCountDown() {
        tvSendLoginCode.isEnabled = false
        tvSendLoginCode.alpha = 0.65f

        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(60_000L, 1_000L) {
            override fun onTick(millisUntilFinished: Long) {
                tvSendLoginCode.text = "${millisUntilFinished / 1000}s"
            }

            override fun onFinish() {
                tvSendLoginCode.isEnabled = true
                tvSendLoginCode.alpha = 1f
                tvSendLoginCode.text = "获取验证码"
            }
        }.start()
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
            etPhone.isEnabled = !state.isLoading
            etSmsCode.isEnabled = !state.isLoading
            cbRememberPassword.isEnabled = !state.isLoading
            cbAutoLogin.isEnabled = !state.isLoading
            tvForgetPassword.isEnabled = !state.isLoading
            tvGoRegister.isEnabled = !state.isLoading
            tvSwitchLoginMode.isEnabled = !state.isLoading
            btnTogglePassword.isEnabled = !state.isLoading

            state.errorMessage?.let { message ->
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                viewModel.clearTransientMessages()
            }

            state.infoMessage?.let { message ->
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                viewModel.clearTransientMessages()
            }

            if (state.autoLogin) {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
                return@observe
            }

            if (state.loginSuccess) {
                val displayName = state.loginData?.nickname
                    ?: state.loginData?.username
                    ?: if (smsLoginMode) etPhone.text.toString().trim() else etAccount.text.toString().trim()

                Toast.makeText(this, "登录成功，欢迎 $displayName", Toast.LENGTH_SHORT).show()

                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
    }

    private fun isValidPhone(phone: String): Boolean {
        return phone.matches(Regex("^1[3-9]\\d{9}$"))
    }

    override fun onDestroy() {
        countDownTimer?.cancel()
        countDownTimer = null
        super.onDestroy()
    }
}
