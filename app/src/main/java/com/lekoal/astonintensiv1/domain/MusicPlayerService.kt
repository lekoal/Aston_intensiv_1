package com.lekoal.astonintensiv1.domain

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.lekoal.astonintensiv1.R
import com.lekoal.astonintensiv1.presentation.MainActivity

class MusicPlayerService : Service() {

    companion object {
        private const val CHANNEL_ID = "MusicPlayerChannel"
        private var mediaPlayer: MediaPlayer? = null
        fun startService(context: Context) {
            val startIntent = Intent(context, MusicPlayerService::class.java)
            mediaPlayer = MediaPlayer.create(context, R.raw.metal_race)
            mediaPlayer?.start()
            val trackName = R.raw.metal_race.toString()
            startIntent.putExtra("InputData", trackName)
            ContextCompat.startForegroundService(context, startIntent)
        }

        fun stopService(context: Context) {
            val stopIntent = Intent(context, MusicPlayerService::class.java)
            mediaPlayer?.stop()
            mediaPlayer?.release()
            context.stopService(stopIntent)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val input = intent?.getStringExtra("InputData")
        createNotificationChannel()
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle("Music Player")
            .setContentText(input)
            .setContentIntent(pendingIntent)
            .build()
        startForeground(1, notification)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.stop()
        mediaPlayer?.release()
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Foreground Service",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager!!.createNotificationChannel(serviceChannel)
        }
    }
}