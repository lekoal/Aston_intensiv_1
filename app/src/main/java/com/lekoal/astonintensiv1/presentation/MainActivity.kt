package com.lekoal.astonintensiv1.presentation

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.lekoal.astonintensiv1.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun onResume() {
        super.onResume()

    }
}