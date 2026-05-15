package com.example.app.ui.pin

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.app.R
import com.example.app.common.ResultState
import com.example.app.data.local.PinStore
import com.example.app.data.repository.AuthRepository
import com.example.app.di.NetworkModule
import kotlinx.coroutines.launch

class PinSetupActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_FINISH_AFTER_SAVE = "extra_finish_after_save"
    }

    private enum class SetupStep {
        VERIFY_OLD,
        INPUT_NEW,
        CONFIRM_NEW
    }

    private lateinit var tvBack: TextView
    private lateinit var tvTitle: TextView
    private lateinit var tvHint: TextView
    private lateinit var tvStepLabel: TextView
    private lateinit var tvPhoneCodeHint: TextView

    private lateinit var etPhoneCode: EditText
    private lateinit var btnSendPinCode: Button

    private lateinit var pinBoxes: List<TextView>
    private lateinit var btnSavePin: Button
    private lateinit var btnClearPin: Button

    private lateinit var viewModel: PinViewModel
    private lateinit var pinStore: PinStore
    private lateinit var authRepository: AuthRepository

    private var finishAfterSave: Boolean = false
    private var hasPinOnEntry: Boolean = false
    private var setupStep: SetupStep = SetupStep.INPUT_NEW

    private var inputPin: String = ""
    private var firstNewPin: String = ""
    private var currentPhone: String = ""
    private var codeSending: Boolean = false
    private var codeChecking: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin_setup)

        finishAfterSave = intent.getBooleanExtra(EXTRA_FINISH_AFTER_SAVE, false)

        val appContext = applicationContext
        val tokenStore = NetworkModule.provideTokenStore(appContext)
        val authInterceptor = NetworkModule.provideAuthInterceptor(tokenStore)
        val loggingInterceptor = NetworkModule.provideLoggingInterceptor()
        val okHttpClient = NetworkModule.provideOkHttpClient(authInterceptor, loggingInterceptor)
        val retrofit = NetworkModule.provideRetrofit(okHttpClient)
        val authApi = NetworkModule.provideAuthApi(retrofit)

        authRepository = AuthRepository(authApi, tokenStore)

        pinStore = PinStore(appContext)
        viewModel = ViewModelProvider(
            this,
            PinViewModel.Factory(pinStore)
        )[PinViewModel::class.java]

        hasPinOnEntry = pinStore.hasPin()
        setupStep = if (hasPinOnEntry) SetupStep.VERIFY_OLD else SetupStep.INPUT_NEW

        initViews()
        initListeners()
        observeUiState()
        renderPage()
        loadCurrentUserPhone()
        viewModel.refreshStatus()
    }

    private fun initViews() {
        tvBack = findViewById(R.id.tvBack)
        tvTitle = findViewById(R.id.tvTitle)
        tvHint = findViewById(R.id.tvHint)
        tvStepLabel = findViewById(R.id.tvStepLabel)
        tvPhoneCodeHint = findViewById(R.id.tvPhoneCodeHint)

        etPhoneCode = findViewById(R.id.etPhoneCode)
        btnSendPinCode = findViewById(R.id.btnSendPinCode)

        pinBoxes = listOf(
            findViewById(R.id.pinBox1),
            findViewById(R.id.pinBox2),
            findViewById(R.id.pinBox3),
            findViewById(R.id.pinBox4),
            findViewById(R.id.pinBox5),
            findViewById(R.id.pinBox6)
        )

        btnSavePin = findViewById(R.id.btnSavePin)
        btnClearPin = findViewById(R.id.btnClearPin)
    }

    private fun initListeners() {
        tvBack.setOnClickListener {
            finish()
        }

        btnSendPinCode.setOnClickListener {
            sendPinCode()
        }

        btnSavePin.setOnClickListener {
            handlePrimaryAction()
        }

        btnClearPin.setOnClickListener {
            showClearPinDialog()
        }

        val digitButtonIds = intArrayOf(
            R.id.btnKey0,
            R.id.btnKey1,
            R.id.btnKey2,
            R.id.btnKey3,
            R.id.btnKey4,
            R.id.btnKey5,
            R.id.btnKey6,
            R.id.btnKey7,
            R.id.btnKey8,
            R.id.btnKey9
        )

        digitButtonIds.forEach { id ->
            findViewById<Button>(id).setOnClickListener { view ->
                appendDigit((view as Button).text.toString())
            }
        }

        findViewById<Button>(R.id.btnKeyDelete).setOnClickListener {
            deleteLastDigit()
        }
    }

    private fun observeUiState() {
        viewModel.uiState.observe(this) { state ->
            state.infoMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.clearInfoMessage()
            }

            state.errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.clearErrorMessage()
            }

            if (state.saveSuccess) {
                setResult(Activity.RESULT_OK)
                viewModel.consumeSaveSuccess()

                if (finishAfterSave) {
                    finish()
                } else {
                    hasPinOnEntry = true
                    setupStep = SetupStep.VERIFY_OLD
                    firstNewPin = ""
                    clearPinInput()
                    etPhoneCode.setText("")
                    renderPage()
                    viewModel.refreshStatus()
                }
            }

            if (state.clearSuccess) {
                viewModel.consumeClearSuccess()
                hasPinOnEntry = false
                setupStep = SetupStep.INPUT_NEW
                firstNewPin = ""
                clearPinInput()
                etPhoneCode.setText("")
                renderPage()
                viewModel.refreshStatus()
            }
        }
    }

    private fun loadCurrentUserPhone() {
        lifecycleScope.launch {
            when (val result = authRepository.getCurrentUser()) {
                is ResultState.Success -> {
                    currentPhone = result.data.phone.orEmpty()
                    renderPhoneCodeHint()
                }

                is ResultState.Error -> {
                    currentPhone = ""
                    tvPhoneCodeHint.text = "获取当前账号手机号失败：${result.message}"
                }

                ResultState.Loading -> Unit
            }
        }
    }

    private fun renderPhoneCodeHint() {
        tvPhoneCodeHint.text = if (currentPhone.isBlank()) {
            "验证码会发送到当前账号绑定手机号，并显示在模拟车机管理台。"
        } else {
            "验证码会发送到当前账号绑定手机号：${maskPhone(currentPhone)}，并显示在模拟车机管理台。"
        }
    }

    private fun maskPhone(phone: String): String {
        return if (phone.length == 11) {
            phone.substring(0, 3) + "****" + phone.substring(7)
        } else {
            phone
        }
    }

    private fun sendPinCode() {
        if (codeSending) return

        codeSending = true
        btnSendPinCode.isEnabled = false
        btnSendPinCode.text = "发送中..."

        lifecycleScope.launch {
            when (val result = authRepository.sendPinCode()) {
                is ResultState.Success -> {
                    Toast.makeText(
                        this@PinSetupActivity,
                        "验证码已发送，请到模拟车机管理台查看",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                is ResultState.Error -> {
                    Toast.makeText(
                        this@PinSetupActivity,
                        result.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }

                ResultState.Loading -> Unit
            }

            codeSending = false
            btnSendPinCode.isEnabled = true
            btnSendPinCode.text = "发送验证码"
        }
    }

    private fun appendDigit(digit: String) {
        if (inputPin.length >= PinViewModel.PIN_LENGTH) return

        inputPin += digit
        renderPinBoxes()
        updatePrimaryButtonState()
    }

    private fun deleteLastDigit() {
        if (inputPin.isEmpty()) return

        inputPin = inputPin.dropLast(1)
        renderPinBoxes()
        updatePrimaryButtonState()
    }

    private fun clearPinInput() {
        inputPin = ""
        renderPinBoxes()
        updatePrimaryButtonState()
    }

    private fun handlePrimaryAction() {
        if (inputPin.length != PinViewModel.PIN_LENGTH) {
            Toast.makeText(this, "请输入 6 位数字 PIN", Toast.LENGTH_SHORT).show()
            return
        }

        when (setupStep) {
            SetupStep.VERIFY_OLD -> {
                if (viewModel.isPinCorrect(inputPin)) {
                    setupStep = SetupStep.INPUT_NEW
                    clearPinInput()
                    renderPage()
                    Toast.makeText(this, "原 PIN 验证通过，请输入新 PIN", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "原 PIN 错误，请重新输入", Toast.LENGTH_SHORT).show()
                    clearPinInput()
                }
            }

            SetupStep.INPUT_NEW -> {
                firstNewPin = inputPin
                setupStep = SetupStep.CONFIRM_NEW
                clearPinInput()
                renderPage()
            }

            SetupStep.CONFIRM_NEW -> {
                if (firstNewPin != inputPin) {
                    Toast.makeText(this, "两次输入的 PIN 不一致", Toast.LENGTH_SHORT).show()
                    clearPinInput()
                    return
                }

                verifyPhoneCodeThen {
                    viewModel.savePin(
                        pin = firstNewPin,
                        confirmPin = inputPin
                    )
                }
            }
        }
    }

    private fun showClearPinDialog() {
        AlertDialog.Builder(this)
            .setTitle("清除 PIN")
            .setMessage("清除 PIN 前需要先输入手机验证码。确认清除后，下次执行远程控制命令前需要重新设置 6 位 PIN。")
            .setNegativeButton("取消", null)
            .setPositiveButton("确认清除") { _, _ ->
                verifyPhoneCodeThen {
                    viewModel.clearPin()
                }
            }
            .show()
    }

    private fun verifyPhoneCodeThen(onSuccess: () -> Unit) {
        if (codeChecking) return

        val code = etPhoneCode.text.toString().trim()

        if (code.isBlank()) {
            Toast.makeText(this, "请先输入手机验证码", Toast.LENGTH_SHORT).show()
            return
        }

        if (!code.matches(Regex("^\\d{6}$"))) {
            Toast.makeText(this, "验证码必须是 6 位数字", Toast.LENGTH_SHORT).show()
            return
        }

        codeChecking = true
        btnSavePin.isEnabled = false
        btnClearPin.isEnabled = false

        lifecycleScope.launch {
            when (val result = authRepository.verifyPinCode(code)) {
                is ResultState.Success -> {
                    Toast.makeText(
                        this@PinSetupActivity,
                        "手机验证码校验通过",
                        Toast.LENGTH_SHORT
                    ).show()
                    onSuccess()
                }

                is ResultState.Error -> {
                    Toast.makeText(
                        this@PinSetupActivity,
                        result.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }

                ResultState.Loading -> Unit
            }

            codeChecking = false
            updatePrimaryButtonState()
            btnClearPin.isEnabled = true
        }
    }

    private fun renderPage() {
        tvTitle.text = if (hasPinOnEntry) "修改 PIN" else "设置 PIN"

        tvHint.text = if (hasPinOnEntry) {
            "为了保护远程控制安全，修改 PIN 前需要先验证原 PIN，并通过手机验证码确认。"
        } else {
            "首次使用远程控制前，请设置 6 位数字 PIN，并通过手机验证码确认。"
        }

        tvStepLabel.text = when (setupStep) {
            SetupStep.VERIFY_OLD -> "请输入原 6 位 PIN"
            SetupStep.INPUT_NEW -> "请输入新的 6 位 PIN"
            SetupStep.CONFIRM_NEW -> "请再次输入新的 6 位 PIN"
        }

        btnSavePin.text = when (setupStep) {
            SetupStep.VERIFY_OLD -> "验证原 PIN"
            SetupStep.INPUT_NEW -> "下一步"
            SetupStep.CONFIRM_NEW -> "验证手机验证码并保存"
        }

        btnClearPin.visibility = if (hasPinOnEntry) View.VISIBLE else View.GONE

        renderPinBoxes()
        updatePrimaryButtonState()
        renderPhoneCodeHint()
    }

    private fun renderPinBoxes() {
        pinBoxes.forEachIndexed { index, textView ->
            textView.text = if (index < inputPin.length) "●" else ""

            val backgroundRes = when {
                index < inputPin.length -> R.drawable.bg_pin_input_box_filled
                index == inputPin.length && inputPin.length < PinViewModel.PIN_LENGTH -> {
                    R.drawable.bg_pin_input_box_active
                }
                else -> R.drawable.bg_pin_input_box_empty
            }

            textView.setBackgroundResource(backgroundRes)
        }
    }

    private fun updatePrimaryButtonState() {
        val enabled = inputPin.length == PinViewModel.PIN_LENGTH && !codeChecking
        btnSavePin.isEnabled = enabled
        btnSavePin.alpha = if (enabled) 1f else 0.45f
    }
}