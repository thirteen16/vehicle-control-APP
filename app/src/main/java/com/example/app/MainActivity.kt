package com.example.app

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.app.ui.home.HomeFragment
import com.example.app.ui.main.MainTabState
import com.example.app.ui.main.MainViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var tvMainTitle: TextView
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvMainTitle = findViewById(R.id.tvMainTitle)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        observeTabState()

        if (savedInstanceState == null) {
            viewModel.selectTab(MainTabState.HOME)
        }
    }

    private fun observeTabState() {
        viewModel.currentTab.observe(this) { tab ->
            when (tab) {
                MainTabState.HOME -> {
                    tvMainTitle.text = "CarControlAPP"
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, HomeFragment())
                        .commit()
                }
            }
        }
    }
}