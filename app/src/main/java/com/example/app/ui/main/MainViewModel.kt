package com.example.app.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {

    private val _currentTab = MutableLiveData(MainTabState.HOME)
    val currentTab: LiveData<MainTabState> = _currentTab

    fun selectTab(tab: MainTabState) {
        _currentTab.value = tab
    }
}