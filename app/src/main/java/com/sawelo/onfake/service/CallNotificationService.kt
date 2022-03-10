package com.sawelo.onfake.service

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.sawelo.onfake.AppDatabase
import com.sawelo.onfake.CallScreenActivity
import com.sawelo.onfake.MainActivity
import com.sawelo.onfake.R
import com.sawelo.onfake.data_class.CallProfileData
import com.sawelo.onfake.receiver.DeclineReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class CallNotificationService : Service() {

    private lateinit var stopTimer: CountDownTimer
    private lateinit var notificationManager: NotificationManager
    private lateinit var builder: NotificationCompat.Builder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Initialize Intents
        val defaultIntent = Intent(this, CallScreenActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            .putExtra(MainActivity.IS_START_FROM_INCOMING_EXTRA, true)

        val answerIntent = Intent(this, CallScreenActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            .putExtra(MainActivity.IS_START_FROM_INCOMING_EXTRA, false)

        val declineIntent = Intent(this, DeclineReceiver::class.java)

        val defaultPendingIntent: PendingIntent?
        val answerPendingIntent: PendingIntent?
        val declinePendingIntent: PendingIntent?

        // Initialize PendingIntents for API level 23 or else
        @SuppressLint("UnspecifiedImmutableFlag")
        if (Build.VERSION.SDK_INT >= 23) {
            defaultPendingIntent = PendingIntent.getActivity(
                this,
                1,
                defaultIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            answerPendingIntent = PendingIntent.getActivity(
                this,
                2,
                answerIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            declinePendingIntent = PendingIntent.getBroadcast(
                this,
                3,
                declineIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        } else {
            defaultPendingIntent = PendingIntent.getActivity(
                this, 1, defaultIntent, PendingIntent.FLAG_UPDATE_CURRENT
            )
            answerPendingIntent = PendingIntent.getActivity(
                this, 2, answerIntent, PendingIntent.FLAG_UPDATE_CURRENT
            )
            declinePendingIntent = PendingIntent.getBroadcast(
                this, 3, declineIntent, PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        // Create NotificationChannel only on API 26+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableLights(true)
                lightColor = Color.RED
                vibrationPattern = longArrayOf(1000, 2000)
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .build()
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE),
                    audioAttributes
                )
            }
            // Register the channel with the system
            notificationManager.createNotificationChannel(channel)
        }

        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getInstance(this@CallNotificationService)
            var callProfile: CallProfileData? = null
            while (callProfile == null) {
                delay(500)
                callProfile = db.callProfileDao().getCallProfile().firstOrNull()
            }

            runCatching {
                val bitmap = Glide.with(this@CallNotificationService)
                    .asBitmap()
                    .load(callProfile.photoUri)
                    .apply(
                        RequestOptions
                            .overrideOf(250, 250)
                            .circleCrop()
                    )
                    .submit()
                    .get()

                builder = NotificationCompat.Builder(this@CallNotificationService, CHANNEL_ID)
                    .addAction(R.drawable.ic_baseline_call_end_24, "Decline", declinePendingIntent)
                    .addAction(R.drawable.ic_baseline_call_24, "Answer", answerPendingIntent)
                    .setContentTitle(callProfile.name)
                    .setContentText("Incoming voice call")
                    .setLargeIcon(bitmap)
                    .setSmallIcon(R.drawable.ic_baseline_notifications)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_CALL)
                    .setFullScreenIntent(defaultPendingIntent, true)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setVibrate(longArrayOf(1000, 2000))
                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE))
                    .setAutoCancel(true)
                    .setDefaults(Notification.DEFAULT_LIGHTS)
            }

            val buildNotification: Notification = builder.build().apply {
                this.flags = Notification.FLAG_INSISTENT
            }

            startForeground(NOTIFICATION_ID, buildNotification)
        }

        // Countdown until CallNotificationService stops
        stopTimer = object : CountDownTimer(25000, 1000) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                this@CallNotificationService.sendBroadcast(declineIntent)
            }
        }

        // Stop AlarmService before starting this CallNotificationService
        val alarmIntent = Intent(this, AlarmService::class.java)
        stopService(alarmIntent)

        stopTimer.start()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopTimer.cancel()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "call_id"
        const val CHANNEL_NAME = "Call Channel"
        const val NOTIFICATION_ID = 2
    }
}