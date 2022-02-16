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
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.sawelo.onfake.FirstWhatsAppActivity
import com.sawelo.onfake.MainActivity
import com.sawelo.onfake.R
import com.sawelo.onfake.data_class.CallProfileData
import com.sawelo.onfake.receiver.DeclineReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit


class NotificationService : Service() {

    companion object {
        const val CHANNEL_ID = "call_id"
        const val CHANNEL_NAME = "Call Channel"
        const val NOTIFICATION_ID = 2
    }

    private lateinit var stopTimer: CountDownTimer
    private lateinit var callProfile: CallProfileData
    private lateinit var notificationManager: NotificationManager

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("NotificationService", "Starting NotificationService")

        callProfile = intent?.getParcelableExtra(MainActivity.PROFILE_EXTRA) ?: CallProfileData()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create RemoteViews with custom layout
        val customNotificationLayout = RemoteViews(
            com.sawelo.onfake.BuildConfig.APPLICATION_ID, R.layout.notification_whats_app
        )

        runBlocking(Dispatchers.IO) {
            val bitmap = Glide.with(this@NotificationService)
                .asBitmap()
                .load(callProfile.photoUri)
                .apply(
                    RequestOptions
                        .overrideOf(250, 250)
                        .circleCrop()
                )
                .submit()
                .get()

            customNotificationLayout.apply{
                setImageViewBitmap(R.id.notification_picture, bitmap)
                setTextViewText(R.id.notification_name, callProfile.name)
            }
        }

        // Initialize Intents
        val defaultIntent = Intent(this, FirstWhatsAppActivity::class.java)
            .putExtra(MainActivity.PROFILE_EXTRA, callProfile)
            .putExtra(MainActivity.START_FROM_INCOMING_EXTRA, true)

        val answerIntent = Intent(this, FirstWhatsAppActivity::class.java)
            .putExtra(MainActivity.PROFILE_EXTRA, callProfile)
            .putExtra(MainActivity.START_FROM_INCOMING_EXTRA, false)

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

        // Applying PendingIntents on buttons in customNotification
        customNotificationLayout.setOnClickPendingIntent(
            R.id.notification_layout,
            defaultPendingIntent
        )
        customNotificationLayout.setOnClickPendingIntent(R.id.btnAnswer, answerPendingIntent)
        customNotificationLayout.setOnClickPendingIntent(R.id.btnDecline, declinePendingIntent)

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

        // Build Notification
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_baseline_notifications)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(defaultPendingIntent, true)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(customNotificationLayout)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE))
            .setVibrate(longArrayOf(1000, 1000))

        val buildNotification: Notification = builder.build().apply {
            this.flags = Notification.FLAG_INSISTENT
        }

        // Countdown until NotificationService stops
        stopTimer = object : CountDownTimer(25000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsUntilFinished = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished)
                Log.d("NotificationService", "Countdown: $secondsUntilFinished")
            }

            override fun onFinish() {
                Log.d("NotificationService", "Finished")
                this@NotificationService.sendBroadcast(declineIntent)
            }
        }

        // Stop AlarmService before starting this NotificationService
        val alarmIntent = Intent(this, AlarmService::class.java)
        stopService(alarmIntent)

        stopTimer.start()
        notificationManager.notify(NOTIFICATION_ID, buildNotification)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.d("Destroy", "NotificationService is destroyed")
        notificationManager.cancel(NOTIFICATION_ID)
        stopTimer.cancel()
        super.onDestroy()
    }
}