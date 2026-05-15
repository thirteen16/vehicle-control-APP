package com.example.app.ui.pin

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.app.R
import com.example.app.data.local.PinStore

class PinVerifyActivity : AppCompatActivity() {

    private lateinit var tvBack: TextView
    private lateinit var tvTitle: TextView
    private lateinit var tvHint: TextView
    private lateinit var tvStepLabel: TextView

    private lateinit var pinBoxes: List<TextView>
    private lateinit var btnVerifyPin: Button

    private lateinit var viewModel: PinViewModel

    private var inputPin: String = ""
    private var isVerifying: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin_verify)

        val pinStore = PinStore(applicationContext)
        viewModel = ViewModelProvider(
            this,
            PinViewModel.Factory(pinStore)
        )[PinViewModel::class.java]

        initViews()
        initListeners()
        observeUiState()
        renderPinBoxes()
        updateVerifyButtonState()
        viewModel.refreshStatus()
    }

    private fun initViews() {
        tvBack = findViewById(R.id.tvBack)
        tvTitle = findViewById(R.id.tvTitle)
        tvHint = findViewById(R.id.tvHint)
        tvStepLabel = findViewById(R.id.tvStepLabel)

        pinBoxes = listOf(
            findViewById(R.id.pinBox1),
            findViewById(R.id.pinBox2),
            findViewById(R.id.pinBox3),
            findViewById(R.id.pinBox4),
            findViewById(R.id.pinBox5),
            findViewById(R.id.pinBox6)
        )

        btnVerifyPin = findViewById(R.id.btnVerifyPin)
    }

    private fun initListeners() {
        tvBack.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        btnVerifyPin.setOnClickListener {
            verifyCurrentPin()
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
            tvTitle.text = "验证 PIN"

            tvHint.text = if (state.hasPin) {
                "请输入本地 6 位 PIN，验证通过后继续执行远程控制命令。"
            } else {
                "当前尚未设置 PIN，请先返回并设置 PIN。"
            }

            state.infoMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.clearInfoMessage()
            }

            state.errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.clearErrorMessage()
                isVerifying = false
                clearPinInput()
            }

            if (state.verifySuccess) {
                setResult(Activity.RESULT_OK)
                viewModel.consumeVerifySuccess()
                finish()
            }
        }
    }

    private fun appendDigit(digit: String) {
        if (inputPin.length >= PinViewModel.PIN_LENGTH || isVerifying) return

        inputPin += digit
        renderPinBoxes()
        updateVerifyButtonState()

        if (inputPin.length == PinViewModel.PIN_LENGTH) {
            btnVerifyPin.postDelayed({
                verifyCurrentPin()
            }, 120)
        }
    }

    private fun deleteLastDigit() {
        if (inputPin.isEmpty() || isVerifying) return

        inputPin = inputPin.dropLast(1)
        renderPinBoxes()
        updateVerifyButtonState()
    }

    private fun clearPinInput() {
        inputPin = ""
        renderPinBoxes()
        updateVerifyButtonState()
    }

    private fun verifyCurrentPin() {
        if (isVerifying) return

        if (inputPin.length != PinViewModel.PIN_LENGTH) {
            Toast.makeText(this, "请输入 6 位数字 PIN", Toast.LENGTH_SHORT).show()
            return
        }

        isVerifying = true
        updateVerifyButtonState()
        viewModel.verifyPin(inputPin)
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

    private fun updateVerifyButtonState() {
        val enabled = inputPin.length == PinViewModel.PIN_LENGTH && !isVerifying
        btnVerifyPin.isEnabled = enabled
        btnVerifyPin.alpha = if (enabled) 1f else 0.45f
    }
}