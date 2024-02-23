package com.lekoal.astonintensiv1.domain

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.lekoal.astonintensiv1.R
import com.lekoal.astonintensiv1.model.PlayerState
import com.lekoal.astonintensiv1.model.Tracks
import com.lekoal.astonintensiv1.presentation.IS_SERVICE_RUNNING_KEY
import com.lekoal.astonintensiv1.presentation.S_P_NAME
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.lang.Exception

class MusicPlayerService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private val tracks = Tracks.get()
    private var trackIndex = 0
    private var isPaused = false

    private var isServiceRunning: Boolean = false

    private val binder = PlayerBinder()

    private val _currentPosition = MutableStateFlow(0)
    val currentPosition: StateFlow<Int> = _currentPosition

    private val _currentDuration = MutableStateFlow(0)
    val currentDuration: StateFlow<Int> = _currentDuration

    private val _currentTitle = MutableStateFlow("Track Title")
    val currentTitle: StateFlow<String> = _currentTitle

    private var currentPositionJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        mediaPlayer?.setOnCompletionListener {
            nextTrack()
        }
    }

    companion object {
        private const val CHANNEL_ID = "MusicPlayerChannel"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isServiceRunning = true
        saveSPState()
        when (intent?.action) {
            PlayerState.PLAY.toString() -> {
                isPaused = false
                playTrack()
            }

            PlayerState.STOP.toString() -> {
                isPaused = false
                stopTrack()
            }

            PlayerState.PAUSE.toString() -> {
                isPaused = true
                pauseTrack()
            }

            PlayerState.NEXT.toString() -> {
                isPaused = false
                nextTrack()
            }

            PlayerState.PREV.toString() -> {
                isPaused = false
                prevTrack()
            }
            PlayerState.INITIAL.toString() -> {
                initialService()
            }
        }
        return START_STICKY
    }
    override fun onDestroy() {
        resetPlayer()
        isServiceRunning = false
        saveSPState()
        currentPositionJob?.cancel()
        stopSelf()
        super.onDestroy()
    }

    override fun onBind(p0: Intent?): IBinder {
        return binder
    }

    private fun initialService() {
        startForeground(1, createNotification())
    }

    private fun playTrack() {
        if (!isPaused) {
            startForeground(1, createNotification())
            mediaPlayer = MediaPlayer.create(this, tracks[trackIndex].id)
        }
        mediaPlayer?.start()
        startUpdatingTrackData()
        startUpdatingCurrentPosition()
    }

    private fun stopTrack() {
        resetPlayer()
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        isServiceRunning = false
        saveSPState()
    }

    private fun pauseTrack() {
        mediaPlayer?.pause()
    }

    private fun nextTrack() {
        trackIndex = if (trackIndex < tracks.size - 1) trackIndex + 1 else 0
        if (mediaPlayer?.isPlaying == true) {
            restartPlayer()
            mediaPlayer?.start()
            startUpdatingTrackData()
        } else {
            restartPlayer()
        }
        startForeground(1, createNotification())
    }

    private fun saveSPState() {
        val sharedPreferences = getSharedPreferences(S_P_NAME, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean(IS_SERVICE_RUNNING_KEY, isServiceRunning)
        editor.apply()
    }

    private fun prevTrack() {
        trackIndex = if (trackIndex > 0) trackIndex - 1 else tracks.size - 1
        if (mediaPlayer?.isPlaying == true) {
            restartPlayer()
            mediaPlayer?.start()
            startUpdatingTrackData()
        } else {
            restartPlayer()
        }
        startForeground(1, createNotification())
    }

    private fun resetPlayer() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        _currentPosition.value = 0
        startUpdatingCurrentPosition()
    }

    private fun restartPlayer() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(this, tracks[trackIndex].id)
        _currentPosition.value = 0
    }

    private fun createNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle("Music Player")
            .setContentText(tracks[trackIndex].title)
            .setSilent(true)
            .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Foreground Service",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    inner class PlayerBinder : Binder() {
        fun getService(): MusicPlayerService {
            return this@MusicPlayerService
        }
    }

    private fun startUpdatingCurrentPosition() {
        currentPositionJob = CoroutineScope(Dispatchers.Main).launch {
            while (isServiceRunning) {
                mediaPlayer?.let {
                    _currentPosition.value = it.currentPosition
                }
                delay(1000)
            }
        }
    }

    private fun startUpdatingTrackData() {
        _currentDuration.value = mediaPlayer?.duration ?: 0
        _currentTitle.value = tracks[trackIndex].title
    }
}