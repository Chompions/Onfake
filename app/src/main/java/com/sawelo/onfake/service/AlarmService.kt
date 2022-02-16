package com.sawelo.onfake.service

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.sawelo.onfake.MainActivity
import com.sawelo.onfake.R
import com.sawelo.onfake.`object`.UpdateTextObject
import com.sawelo.onfake.data_class.CallProfileData
import com.sawelo.onfake.data_class.ClockType
import com.sawelo.onfake.receiver.DeclineReceiver
import java.util.*

class AlarmService : Service() {

    companion object {
        const val CHANNEL_ID = "infake_id"
        const val CHANNEL_NAME = "Infake Channel"
        const val NOTIFICATION_ID = 1
    }

    private lateinit var builder: NotificationCompat.Builder
    private lateinit var callProfile: CallProfileData
    private lateinit var alarmManager: AlarmManager
    private lateinit var pendingIntent: PendingIntent
    private lateinit var notificationManager: NotificationManager

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("AlarmService", "Starting AlarmService")

        callProfile = intent?.getParcelableExtra(MainActivity.PROFILE_EXTRA) ?: CallProfileData()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create NotificationChannel only on API 26+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_MIN
            )

            // Register the channel with the system
            notificationManager.createNotificationChannel(channel)
        }

        val (_, notificationText) =
            UpdateTextObject.updateMainText(callProfile.scheduleData)
        println("SharedPref Data: ${callProfile.scheduleData}")
        println("NotificationText: $notificationText")

        val declineIntent = Intent(this, DeclineReceiver::class.java)

        @SuppressLint("UnspecifiedImmutableFlag")
        val declinePendingIntent = if (Build.VERSION.SDK_INT >= 23) {
            PendingIntent.getBroadcast(
                this,
                9,
                declineIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        } else {
            PendingIntent.getBroadcast(this, 9, declineIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        // Build Notification
        builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Infake")
            .setContentText(notificationText)
            .setSmallIcon(R.drawable.ic_baseline_notifications)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOnlyAlertOnce(true)
            .addAction(
                R.drawable.ic_baseline_cancel, "Cancel",
                declinePendingIntent
            )

        setAlarm()
        startForeground(NOTIFICATION_ID, builder.build())
        return START_STICKY
    }

    // Setting RCT_Wakeup directly to FlutterReceiver
    private fun setAlarm() {
        Log.d("AlarmService", "Run setAlarm()")

        // Create AlarmManager
        alarmManager = this.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val callIntent = Intent(this, NotificationService::class.java)
            .putExtra(MainActivity.PROFILE_EXTRA, callProfile)

        @SuppressLint("UnspecifiedImmutableFlag")
        pendingIntent = if (Build.VERSION.SDK_INT >= 23) {
            PendingIntent.getService(
                this, 0, callIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
            )
        } else {
            PendingIntent.getService(
                this, 0, callIntent, PendingIntent.FLAG_CANCEL_CURRENT
            )
        }

        val c: Calendar = Calendar.getInstance()
        when (callProfile.scheduleData.clockType) {
            ClockType.TIMER -> {
                Log.d("AlarmService", "Using timerType")
                c.apply {
                    timeInMillis = System.currentTimeMillis()
                    val setHour = get(Calendar.HOUR_OF_DAY) + callProfile.scheduleData.hour
                    val setMinute = get(Calendar.MINUTE) + callProfile.scheduleData.minute
                    val setSecond = get(Calendar.SECOND) + callProfile.scheduleData.second

                    set(Calendar.HOUR_OF_DAY, setHour)
                    set(Calendar.MINUTE, setMinute)
                    set(Calendar.SECOND, setSecond)
                }
            }
            ClockType.ALARM -> {
                Log.d("AlarmService", "Using alarmType")
                c.apply {
                    timeInMillis = System.currentTimeMillis()
                    set(Calendar.HOUR_OF_DAY, callProfile.scheduleData.hour)
                    set(Calendar.MINUTE, callProfile.scheduleData.minute)
                }
            }
        }

        /**
         * while setAndAllowWhileIdle() or setExactAndAllowWhileIdle() is meant
         * to guarantee alarms execution, should be noted it's not exact while in
         * idle mode, it runs only in every 15 minutes
         *
         * To avoid Doze mode, use setAlarmClock()
         * */

        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(
                c.timeInMillis,
                pendingIntent
            ),
            pendingIntent
        )
    }

    override fun onBind(p0: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.d("Destroy", "AlarmService is destroyed")
        notificationManager.cancel(NOTIFICATION_ID)
        alarmManager.cancel(pendingIntent)
        super.onDestroy()
    }
}