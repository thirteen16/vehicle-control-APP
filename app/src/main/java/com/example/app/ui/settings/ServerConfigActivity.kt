package com.example.app.ui.settings

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.app.R
import com.example.app.common.Constants
import com.example.app.data.local.ServerConfigStore

class ServerConfigActivity : AppCompatActivity() {

    private lateinit var tvBack: TextView
    private lateinit var tvCurrentBaseUrl: TextView
    private lateinit var tvCurrentWsUrl: TextView
    private lateinit var etBaseUrl: EditText
    private lateinit var btnSave: Button
    private lateinit var btnResetDefault: Button

    private lateinit var serverConfigStore: ServerConfigStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_server_config)

        serverConfigStore = ServerConfigStore(applicationContext)

        initViews()
        initListeners()
        renderCurrentUrls()
    }

    private fun initViews() {
        tvBack = findViewById(R.id.tvBack)
        tvCurrentBaseUrl = findViewById(R.id.tvCurrentBaseUrl)
        tvCurrentWsUrl = findViewById(R.id.tvCurrentWsUrl)
        etBaseUrl = findViewById(R.id.etBaseUrl)
        btnSave = findViewById(R.id.btnSave)
        btnResetDefault = findViewById(R.id.btnResetDefault)
    }

    private fun initListeners() {
        tvBack.setOnClickListener {
            finish()
        }

        btnSave.setOnClickListener {
            val input = etBaseUrl.text.toString().trim()
            if (input.isBlank()) {
                Toast.makeText(this, "请输入服务器地址", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val savedBaseUrl = serverConfigStore.saveBaseUrl(input)
            Toast.makeText(this, "服务器地址已保存", Toast.LENGTH_SHORT).show()

            setResult(Activity.RESULT_OK)
            renderCurrentUrls()
            etBaseUrl.setText(savedBaseUrl)
        }

        btnResetDefault.setOnClickListener {
            val defaultUrl = serverConfigStore.resetToDefault()
            Toast.makeText(this, "已恢复默认服务器地址", Toast.LENGTH_SHORT).show()

            setResult(Activity.RESULT_OK)
            renderCurrentUrls()
            etBaseUrl.setText(defaultUrl)
        }
    }

    private fun renderCurrentUrls() {
        val currentBaseUrl = serverConfigStore.getBaseUrl()
        tvCurrentBaseUrl.text = "当前 REST 地址：$currentBaseUrl"
        tvCurrentWsUrl.text = "当前 WebSocket 地址：${Constants.WS_BASE_URL}"
        etBaseUrl.setText(currentBaseUrl)
        etBaseUrl.setSelection(etBaseUrl.text.length)
    }
}