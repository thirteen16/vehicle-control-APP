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
import com.example.app.data.repository.UserProfileRepository
import com.example.app.di.NetworkModule
import kotlinx.coroutines.launch

class EditNicknameActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CURRENT_NICKNAME = "extra_current_nickname"
    }

    private lateinit var tvBack: TextView
    private lateinit var tvCurrentNickname: TextView
    private lateinit var etNickname: EditText
    private lateinit var btnSave: Button
    private lateinit var progressBar: ProgressBar

    private lateinit var repository: UserProfileRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_nickname)

        val appContext = applicationContext
        val tokenStore = NetworkModule.provideTokenStore(appContext)
        val authInterceptor = NetworkModule.provideAuthInterceptor(tokenStore)
        val loggingInterceptor = NetworkModule.provideLoggingInterceptor()
        val okHttpClient = NetworkModule.provideOkHttpClient(authInterceptor, loggingInterceptor)
        val retrofit = NetworkModule.provideRetrofit(okHttpClient)
        val api = retrofit.create(com.example.app.data.remote.api.UserProfileApi::class.java)
        repository = UserProfileRepository(api)

        initViews()
        initData()
        initListeners()
    }

    private fun initViews() {
        tvBack = findViewById(R.id.tvBack)
        tvCurrentNickname = findViewById(R.id.tvCurrentNickname)
        etNickname = findViewById(R.id.etNickname)
        btnSave = findViewById(R.id.btnSave)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun initData() {
        val currentNickname = intent.getStringExtra(EXTRA_CURRENT_NICKNAME).orEmpty()
        tvCurrentNickname.text = if (currentNickname.isBlank()) "当前昵称：未设置" else "当前昵称：$currentNickname"
        etNickname.setText(currentNickname)
        etNickname.setSelection(etNickname.text.length)
    }

    private fun initListeners() {
        tvBack.setOnClickListener { finish() }

        btnSave.setOnClickListener {
            val nickname = etNickname.text.toString().trim()

            if (nickname.isBlank()) {
                Toast.makeText(this, "请输入昵称", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (nickname.length > 20) {
                Toast.makeText(this, "昵称最多 20 个字符", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            updateNickname(nickname)
        }
    }

    private fun updateNickname(nickname: String) {
        setLoading(true)

        lifecycleScope.launch {
            when (val result = repository.updateNickname(nickname)) {
                is ResultState.Success -> {
                    setLoading(false)
                    Toast.makeText(this@EditNicknameActivity, result.data, Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK)
                    finish()
                }

                is ResultState.Error -> {
                    setLoading(false)
                    Toast.makeText(this@EditNicknameActivity, result.message, Toast.LENGTH_SHORT).show()
                }

                ResultState.Loading -> Unit
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        btnSave.isEnabled = !loading
    }
}