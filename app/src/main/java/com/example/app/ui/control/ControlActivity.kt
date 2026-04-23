package com.example.app.ui.control

import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.example.app.R

class ControlActivity : AppCompatActivity(R.layout.activity_control) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.controlFragmentContainer, ControlFragment())
                .commit()
        }
    }
}