package com.lekoal.astonintensiv1.presentation

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.lekoal.astonintensiv1.databinding.ActivityMainBinding
import com.lekoal.astonintensiv1.domain.MusicPlayerService
import com.lekoal.astonintensiv1.model.PlayerState

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var playBtn: AppCompatImageButton
    private lateinit var stopBtn: AppCompatImageButton
    private lateinit var nextBtn: AppCompatImageButton
    private lateinit var prevBtn: AppCompatImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        playBtn = binding.playerBtnPlayPause
        stopBtn = binding.playerBtnStop
        nextBtn = binding.playerBtnNext
        prevBtn = binding.playerBtnPrevious

        playBtn.setOnClickListener {
            checkNotificationPermission()
        }

        stopBtn.setOnClickListener {
            startService(PlayerState.STOP)
        }

        nextBtn.setOnClickListener {
            startService(PlayerState.NEXT)
        }

        prevBtn.setOnClickListener {
            startService(PlayerState.PREV)
        }

    }

    private fun startService(playerState: PlayerState) {
        val serviceIntent = Intent(this, MusicPlayerService::class.java).apply {
            action = playerState.toString()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionState = ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionState == PackageManager.PERMISSION_DENIED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    1
                )

            } else {
                startService(PlayerState.PLAY)
            }

        } else {
            startService(PlayerState.PLAY)
        }
    }

    override fun onResume() {
        super.onResume()

    }
}