package com.lekoal.astonintensiv1.domain

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.lekoal.astonintensiv1.R
import com.lekoal.astonintensiv1.model.PlayerState
import com.lekoal.astonintensiv1.model.Tracks

class MusicPlayerService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private val tracks = Tracks.get()
    private var trackIndex = 0
    private var isPaused = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    companion object {
        private const val CHANNEL_ID = "MusicPlayerChannel"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
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
        }
        return START_STICKY
    }

    override fun onDestroy() {
        resetPlayer()
        super.onDestroy()
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    private fun playTrack() {
        if (!isPaused) {
            startForeground(1, createNotification())
            mediaPlayer = MediaPlayer.create(this, tracks[trackIndex].id)
        }
        mediaPlayer?.start()
    }

    private fun stopTrack() {
        resetPlayer()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun pauseTrack() {
        mediaPlayer?.pause()
    }

    private fun nextTrack() {
        trackIndex = if (trackIndex < tracks.size - 1) trackIndex + 1 else 0
        restartPlayer()
    }

    private fun prevTrack() {
        trackIndex = if (trackIndex > 0) trackIndex - 1 else tracks.size - 1
        restartPlayer()
    }

    private fun resetPlayer() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun restartPlayer() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(this, tracks[trackIndex].id)
        mediaPlayer?.start()
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
}