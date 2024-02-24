package com.lekoal.astonintensiv1.presentation

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatSeekBar
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.lekoal.astonintensiv1.R
import com.lekoal.astonintensiv1.databinding.ActivityMainBinding
import com.lekoal.astonintensiv1.domain.MusicPlayerService
import com.lekoal.astonintensiv1.model.PlayerState
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

const val IS_SERVICE_RUNNING_KEY = "IsServiceRunning"
const val S_P_NAME = "ServiceSPName"

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var playBtn: AppCompatImageButton
    private lateinit var stopBtn: AppCompatImageButton
    private lateinit var nextBtn: AppCompatImageButton
    private lateinit var prevBtn: AppCompatImageButton
    private lateinit var titleTV: AppCompatTextView
    private lateinit var progressIndicator: AppCompatSeekBar

    private var musicPlayerService: MusicPlayerService? = null
    private var serviceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicPlayerService.PlayerBinder
            musicPlayerService = binder.getService()
            serviceBound = true
            jobsCreate()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            serviceBound = false
        }

    }

    private var durationJob: Job? = null
    private var positionJob: Job? = null
    private var titleJob: Job? = null
    private var stateJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkNotificationPermission()

        progressIndicator = binding.songProgressIndicator
        playBtn = binding.playerBtnPlayPause
        stopBtn = binding.playerBtnStop
        nextBtn = binding.playerBtnNext
        prevBtn = binding.playerBtnPrevious
        titleTV = binding.tvSongName

        playBtn.setOnClickListener {
            musicPlayerService?.playTrack()
        }

        stopBtn.setOnClickListener {
            musicPlayerService?.stopTrack()
        }

        nextBtn.setOnClickListener {
            musicPlayerService?.nextTrack()
        }

        prevBtn.setOnClickListener {
            musicPlayerService?.prevTrack()
        }
    }

    override fun onStop() {
        super.onStop()
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }
    }

    private fun startService() {
        val serviceIntent = Intent(this, MusicPlayerService::class.java)
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
            }
        }
    }

    private fun isServiceRunning(): Boolean {
        val sharedPreferences = getSharedPreferences(S_P_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean(IS_SERVICE_RUNNING_KEY, false)
    }

    override fun onResume() {

        super.onResume()
        val serviceIntent = Intent(this, MusicPlayerService::class.java)
        if (!isServiceRunning()) {
            startService()
        }
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        jobsCancel()
        super.onDestroy()
    }

    private fun jobsCreate() {
        durationJob = lifecycleScope.launch {
            musicPlayerService?.currentDuration?.collect {
                progressIndicator.max = it
            }
        }

        positionJob = lifecycleScope.launch {
            musicPlayerService?.currentPosition?.collect {
                progressIndicator.progress = it
            }
        }

        titleJob = lifecycleScope.launch {
            musicPlayerService?.currentTitle?.collect {
                titleTV.text = it
            }
        }

        stateJob = lifecycleScope.launch {
            musicPlayerService?.currentState?.collect {
                when (it) {
                    PlayerState.PLAY -> {
                        playBtn.setImageResource(R.drawable.pause)
                    }

                    PlayerState.STOP -> {
                        playBtn.setImageResource(R.drawable.play)
                    }

                    else -> {
                        playBtn.setImageResource(R.drawable.play)
                    }
                }
            }
        }
    }

    private fun jobsCancel() {
        durationJob?.cancel()
        positionJob?.cancel()
        titleJob?.cancel()

    }
}