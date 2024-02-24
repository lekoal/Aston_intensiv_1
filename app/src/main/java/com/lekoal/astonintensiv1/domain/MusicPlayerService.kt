package com.lekoal.astonintensiv1.domain

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.widget.RemoteViews
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

class MusicPlayerService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private val tracks = Tracks.get()
    private var trackIndex = 0

    private var isServiceRunning: Boolean = false

    private val binder = PlayerBinder()

    private val _currentState = MutableStateFlow(PlayerState.INITIAL)
    val currentState: StateFlow<PlayerState> = _currentState

    private val _currentPosition = MutableStateFlow(0)
    val currentPosition: StateFlow<Int> = _currentPosition

    private val _currentDuration = MutableStateFlow(0)
    val currentDuration: StateFlow<Int> = _currentDuration

    private val _currentTitle = MutableStateFlow("Track Title")
    val currentTitle: StateFlow<String> = _currentTitle

    private var currentPositionJob: Job? = null

    private var playIcon: Int = R.drawable.play

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    companion object {
        private const val CHANNEL_ID = "MusicPlayerChannel"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isServiceRunning = true
        saveSPState()
        initialService()
        intent?.let {
            when (it.action) {
                "Prev" -> {
                    prevTrack()
                }

                "Play" -> {
                    playTrack()
                }

                "Stop" -> {
                    stopTrack()
                }

                "Next" -> {
                    nextTrack()
                }
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
        createNotification()
    }

    fun playTrack() {
        when (currentState.value) {
            PlayerState.PLAY -> {
                pauseTrack()
                playIcon = R.drawable.play
                _currentState.value = PlayerState.PAUSE
            }

            PlayerState.PAUSE -> {
                resumeTrack()
                playIcon = R.drawable.pause
                _currentState.value = PlayerState.PLAY
            }

            else -> {
                mediaPlayer = MediaPlayer.create(this, tracks[trackIndex].id)
                mediaPlayer?.start()
                onCompleteTrack()
                playIcon = R.drawable.pause
                _currentState.value = PlayerState.PLAY
            }
        }
        startUpdatingTrackData()
        startUpdatingCurrentPosition()
        createNotification()
    }

    fun stopTrack() {
        playIcon = R.drawable.play
        if (currentState.value == PlayerState.PLAY ||
            currentState.value == PlayerState.PAUSE
        ) {
            try {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            resetPlayer()
            isServiceRunning = false
            saveSPState()
            _currentState.value = PlayerState.STOP
        }
        createNotification()
    }

    private fun pauseTrack() {
        if (currentState.value != PlayerState.PAUSE) {
            mediaPlayer?.pause()
        }
        _currentState.value = PlayerState.PAUSE
    }

    private fun resumeTrack() {
        if (currentState.value == PlayerState.PAUSE) {
            mediaPlayer?.start()
            onCompleteTrack()
        }
        _currentState.value = PlayerState.RESUME
    }

    fun nextTrack() {
        trackIndex = if (trackIndex < tracks.size - 1) trackIndex + 1 else 0
        restartPlayer()
        if (currentState.value == PlayerState.PLAY) {
            mediaPlayer?.start()
            onCompleteTrack()
        }
        startUpdatingTrackData()
        createNotification()
    }

    private fun saveSPState() {
        val sharedPreferences = getSharedPreferences(S_P_NAME, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean(IS_SERVICE_RUNNING_KEY, isServiceRunning)
        editor.apply()
    }

    fun prevTrack() {
        trackIndex = if (trackIndex > 0) trackIndex - 1 else tracks.size - 1
        restartPlayer()
        if (currentState.value == PlayerState.PLAY) {
            mediaPlayer?.start()
            onCompleteTrack()
        }
        startUpdatingTrackData()
        createNotification()
    }

    private fun resetPlayer() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        _currentPosition.value = 0
        startUpdatingCurrentPosition()
    }

    private fun onCompleteTrack() {
        mediaPlayer?.setOnCompletionListener {
            nextTrack()
        }
    }

    private fun restartPlayer() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(this, tracks[trackIndex].id)
        _currentPosition.value = 0
    }

    private fun createNotification() {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notificationLayout = RemoteViews(packageName, R.layout.custom_notification_layot)
        notificationLayout.setOnClickPendingIntent(
            R.id.notification_prev_button,
            getPendingIntent("Prev")
        )
        notificationLayout.setOnClickPendingIntent(
            R.id.notification_play_button,
            getPendingIntent("Play")
        )
        notificationLayout.setOnClickPendingIntent(
            R.id.notification_stop_button,
            getPendingIntent("Stop")
        )
        notificationLayout.setOnClickPendingIntent(
            R.id.notification_next_button,
            getPendingIntent("Next")
        )
        notificationLayout.setTextViewText(R.id.notification_title, "Music Player Service")
        notificationLayout.setTextViewText(R.id.notification_track_name, tracks[trackIndex].title)
        notificationLayout.setImageViewResource(R.id.notification_play_button, playIcon)
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setCustomContentView(notificationLayout)
            .setSmallIcon(R.drawable.ic_music)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        notificationManager.notify(1, notification)
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Foreground Service",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
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
            while (true) {
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

    private fun getPendingIntent(action: String): PendingIntent {
        val intent = Intent(this, MusicPlayerService::class.java)
        intent.action = action
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }
}