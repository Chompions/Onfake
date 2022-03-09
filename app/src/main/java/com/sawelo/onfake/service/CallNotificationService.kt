package com.sawelo.onfake.service

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.sawelo.onfake.AppDatabase
import com.sawelo.onfake.CallScreenActivity
import com.sawelo.onfake.MainActivity
import com.sawelo.onfake.R
import com.sawelo.onfake.data_class.CallProfileDefaultValue
import com.sawelo.onfake.receiver.DeclineReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit


class CallNotificationService : Service() {

    private lateinit var stopTimer: CountDownTimer
    private lateinit var notificationManager: NotificationManager
    private lateinit var builder: NotificationCompat.Builder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(THIS_CLASS, "Starting CallNotificationService")

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
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .build()
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE),
                    audioAttributes
                )
                vibrationPattern = longArrayOf(1000, 1000)
            }
            // Register the channel with the system
            notificationManager.createNotificationChannel(channel)
        }

        builder = NotificationCompat.Builder(this@CallNotificationService, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_baseline_notifications)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(defaultPendingIntent, true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE))
            .setVibrate(longArrayOf(1000, 1000))
            .setAutoCancel(true)
            .addAction(R.drawable.ic_baseline_call_end_24, "Decline", declinePendingIntent)
            .addAction(R.drawable.ic_baseline_call_24, "Answer", answerPendingIntent)

        runBlocking(Dispatchers.IO) {
            val bitmap = Glide.with(this@CallNotificationService)
                .asBitmap()
                .load(CallProfileDefaultValue.photoUriValue)
                .apply(
                    RequestOptions
                        .overrideOf(250, 250)
                        .circleCrop()
                )
                .submit()
                .get()

            builder
                .setContentTitle("Call")
                .setContentText("Incoming voice call")
                .setLargeIcon(bitmap)
        }

        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getInstance(this@CallNotificationService)
            val callProfile = db.callProfileDao().getCallProfile().first()

            runCatching {
                Log.d(THIS_CLASS, "Setting bitmap started")
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

                builder
                    .setContentTitle(callProfile.name)
                    .setContentText("Incoming voice call")
                    .setLargeIcon(bitmap)
                Log.d(THIS_CLASS, "Setting bitmap finished")
            }

            Log.d(THIS_CLASS, "Building builder in Dispatchers.IO")

            val buildNotification: Notification = builder.build().apply {
                this.flags = Notification.FLAG_INSISTENT
            }

            startForeground(NOTIFICATION_ID, buildNotification)
        }

        val buildNotification: Notification = builder.build().apply {
            this.flags = Notification.FLAG_INSISTENT
        }

        // Countdown until CallNotificationService stops
        stopTimer = object : CountDownTimer(25000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsUntilFinished = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished)
                Log.d(THIS_CLASS, "Countdown: $secondsUntilFinished")
            }

            override fun onFinish() {
                Log.d(THIS_CLASS, "Finished")
                this@CallNotificationService.sendBroadcast(declineIntent)
            }
        }

        // Stop AlarmService before starting this CallNotificationService
        val alarmIntent = Intent(this, AlarmService::class.java)
        stopService(alarmIntent)

        stopTimer.start()
        startForeground(NOTIFICATION_ID, buildNotification)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.d(THIS_CLASS, "CallNotificationService is destroyed")
        stopTimer.cancel()
        super.onDestroy()
    }

    companion object {
        const val THIS_CLASS = "CallNotificationService"
        const val CHANNEL_ID = "call_id"
        const val CHANNEL_NAME = "Call Channel"
        const val NOTIFICATION_ID = 2
    }
}