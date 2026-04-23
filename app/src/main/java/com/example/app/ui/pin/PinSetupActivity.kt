package com.example.app.ui.pin

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.app.R
import com.example.app.data.local.PinStore

class PinSetupActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_FINISH_AFTER_SAVE = "extra_finish_after_save"
    }

    private lateinit var tvBack: TextView
    private lateinit var tvTitle: TextView
    private lateinit var tvHint: TextView
    private lateinit var etPin: EditText
    private lateinit var etConfirmPin: EditText
    private lateinit var btnSavePin: Button
    private lateinit var btnClearPin: Button

    private lateinit var viewModel: PinViewModel
    private var finishAfterSave: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin_setup)

        finishAfterSave = intent.getBooleanExtra(EXTRA_FINISH_AFTER_SAVE, false)

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
        etConfirmPin = findViewById(R.id.etConfirmPin)
        btnSavePin = findViewById(R.id.btnSavePin)
        btnClearPin = findViewById(R.id.btnClearPin)
    }

    private fun initListeners() {
        tvBack.setOnClickListener {
            finish()
        }

        btnSavePin.setOnClickListener {
            viewModel.savePin(
                pin = etPin.text.toString(),
                confirmPin = etConfirmPin.text.toString()
            )
        }

        btnClearPin.setOnClickListener {
            viewModel.clearPin()
        }
    }

    private fun observeUiState() {
        viewModel.uiState.observe(this) { state ->
            tvTitle.text = if (state.hasPin) "修改 PIN" else "设置 PIN"
            tvHint.text = if (state.hasPin) {
                "当前已设置 PIN，可重新设置或清除。"
            } else {
                "首次使用远程控制前，请先设置一个数字 PIN。"
            }

            btnClearPin.visibility = if (state.hasPin) View.VISIBLE else View.GONE

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
                    etPin.setText("")
                    etConfirmPin.setText("")
                    viewModel.refreshStatus()
                }
            }

            if (state.clearSuccess) {
                viewModel.consumeClearSuccess()
                etPin.setText("")
                etConfirmPin.setText("")
                viewModel.refreshStatus()
            }
        }
    }
}