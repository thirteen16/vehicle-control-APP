package com.example.app.ui.profile

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.app.R
import com.example.app.common.ResultState
import com.example.app.data.model.request.SendChangePhoneCodeRequest
import com.example.app.data.remote.api.UserProfileApi
import com.example.app.data.repository.UserProfileRepository
import com.example.app.di.NetworkModule
import kotlinx.coroutines.launch

class ChangePhoneActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CURRENT_PHONE = "extra_current_phone"
    }

    private lateinit var tvBack: TextView
    private lateinit var tvCurrentPhone: TextView
    private lateinit var etNewPhone: EditText
    private lateinit var etCode: EditText
    private lateinit var btnSendCode: Button
    private lateinit var btnSave: Button
    private lateinit var progressBar: ProgressBar

    private lateinit var userProfileApi: UserProfileApi
    private lateinit var userProfileRepository: UserProfileRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_phone)

        val appContext = applicationContext
        val tokenStore = NetworkModule.provideTokenStore(appContext)
        val authInterceptor = NetworkModule.provideAuthInterceptor(tokenStore)
        val loggingInterceptor = NetworkModule.provideLoggingInterceptor()
        val okHttpClient = NetworkModule.provideOkHttpClient(authInterceptor, loggingInterceptor)
        val retrofit = NetworkModule.provideRetrofit(okHttpClient)

        userProfileApi = retrofit.create(UserProfileApi::class.java)
        userProfileRepository = UserProfileRepository(userProfileApi)

        initViews()
        initData()
        initListeners()
    }

    private fun initViews() {
        tvBack = findViewById(R.id.tvBack)
        tvCurrentPhone = findViewById(R.id.tvCurrentPhone)
        etNewPhone = findViewById(R.id.etNewPhone)
        etCode = findViewById(R.id.etCode)
        btnSendCode = findViewById(R.id.btnSendCode)
        btnSave = findViewById(R.id.btnSave)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun initData() {
        val currentPhone = intent.getStringExtra(EXTRA_CURRENT_PHONE).orEmpty()
        tvCurrentPhone.text = if (currentPhone.isBlank()) {
            "当前手机号：未设置"
        } else {
            "当前手机号：$currentPhone"
        }
    }

    private fun initListeners() {
        tvBack.setOnClickListener { finish() }

        btnSendCode.setOnClickListener {
            val newPhone = etNewPhone.text.toString().trim()

            if (newPhone.isBlank()) {
                Toast.makeText(this, "请输入新手机号", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            sendCode(newPhone)
        }

        btnSave.setOnClickListener {
            val newPhone = etNewPhone.text.toString().trim()
            val code = etCode.text.toString().trim()

            if (newPhone.isBlank()) {
                Toast.makeText(this, "请输入新手机号", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (code.isBlank()) {
                Toast.makeText(this, "请输入验证码", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            changePhone(newPhone, code)
        }
    }

    private fun sendCode(newPhone: String) {
        setLoading(true)

        lifecycleScope.launch {
            try {
                val response = userProfileApi.sendChangePhoneCode(
                    SendChangePhoneCodeRequest(newPhone = newPhone)
                )

                setLoading(false)

                if (response.code == 200) {
                    Toast.makeText(
                        this@ChangePhoneActivity,
                        response.message.ifBlank { "验证码发送成功" },
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this@ChangePhoneActivity,
                        response.message.ifBlank { "验证码发送失败" },
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                setLoading(false)
                Toast.makeText(
                    this@ChangePhoneActivity,
                    e.javaClass.simpleName + ": " + (e.message ?: "网络请求失败"),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun changePhone(newPhone: String, code: String) {
        setLoading(true)

        lifecycleScope.launch {
            when (val result = userProfileRepository.changePhone(newPhone, code)) {
                is ResultState.Success -> {
                    setLoading(false)
                    Toast.makeText(this@ChangePhoneActivity, result.data, Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK)
                    finish()
                }

                is ResultState.Error -> {
                    setLoading(false)
                    Toast.makeText(this@ChangePhoneActivity, result.message, Toast.LENGTH_SHORT).show()
                }

                ResultState.Loading -> Unit
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        btnSendCode.isEnabled = !loading
        btnSave.isEnabled = !loading
    }
}