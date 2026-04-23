package com.example.app.ui.pin

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
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
    private lateinit var etPin: EditText
    private lateinit var btnVerifyPin: Button

    private lateinit var viewModel: PinViewModel

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
        viewModel.refreshStatus()
    }

    private fun initViews() {
        tvBack = findViewById(R.id.tvBack)
        tvTitle = findViewById(R.id.tvTitle)
        tvHint = findViewById(R.id.tvHint)
        etPin = findViewById(R.id.etPin)
        btnVerifyPin = findViewById(R.id.btnVerifyPin)
    }

    private fun initListeners() {
        tvBack.setOnClickListener {
            finish()
        }

        btnVerifyPin.setOnClickListener {
            viewModel.verifyPin(etPin.text.toString())
        }
    }

    private fun observeUiState() {
        viewModel.uiState.observe(this) { state ->
            tvTitle.text = "验证 PIN"
            tvHint.text = if (state.hasPin) {
                "请输入本地 PIN，验证通过后继续执行控制命令。"
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
            }

            if (state.verifySuccess) {
                setResult(Activity.RESULT_OK)
                viewModel.consumeVerifySuccess()
                finish()
            }
        }
    }
}