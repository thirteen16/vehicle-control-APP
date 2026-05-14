package com.example.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.app.ui.command.CommandHistoryFragment
import com.example.app.ui.control.ControlFragment
import com.example.app.ui.home.HomeFragment
import com.example.app.ui.main.AppRealtimeViewModel
import com.example.app.ui.main.MainTabState
import com.example.app.ui.main.MainViewModel
import com.example.app.ui.profile.ProfileFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: MainViewModel
    private lateinit var realtimeViewModel: AppRealtimeViewModel
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottomNavMain)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        realtimeViewModel = ViewModelProvider(this)[AppRealtimeViewModel::class.java]

        initBottomNav()
        observeTabState()

        if (savedInstanceState == null) {
            viewModel.selectTab(MainTabState.HOME)
        }
    }

    override fun onResume() {
        super.onResume()
        realtimeViewModel.connectIfNeeded()
    }

    private fun initBottomNav() {
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_home -> {
                    viewModel.selectTab(MainTabState.HOME)
                    true
                }

                R.id.menu_control -> {
                    viewModel.selectTab(MainTabState.CONTROL)
                    true
                }

                R.id.menu_history -> {
                    viewModel.selectTab(MainTabState.HISTORY)
                    true
                }

                R.id.menu_profile -> {
                    viewModel.selectTab(MainTabState.PROFILE)
                    true
                }

                else -> false
            }
        }
    }

    private fun observeTabState() {
        viewModel.currentTab.observe(this) { tab ->
            val fragment = when (tab) {
                MainTabState.HOME -> HomeFragment()
                MainTabState.CONTROL -> ControlFragment()
                MainTabState.HISTORY -> CommandHistoryFragment()
                MainTabState.PROFILE -> ProfileFragment()
            }

            val selectedItemId = when (tab) {
                MainTabState.HOME -> R.id.menu_home
                MainTabState.CONTROL -> R.id.menu_control
                MainTabState.HISTORY -> R.id.menu_history
                MainTabState.PROFILE -> R.id.menu_profile
            }

            if (bottomNav.selectedItemId != selectedItemId) {
                bottomNav.selectedItemId = selectedItemId
            }

            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit()
        }
    }
}