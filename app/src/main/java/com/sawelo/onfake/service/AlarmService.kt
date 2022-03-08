package com.sawelo.onfake.service

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import com.sawelo.onfake.MainActivity
import com.sawelo.onfake.R
import com.sawelo.onfake.data_class.CallProfileData
import com.sawelo.onfake.data_class.ClockType
import com.sawelo.onfake.receiver.DeclineReceiver
import java.util.*
import java.util.concurrent.TimeUnit

class AlarmService : Service() {

    private lateinit var callProfile: CallProfileData
    private lateinit var alarmManager: AlarmManager
    private lateinit var notificationManager: NotificationManager
    private lateinit var builder: NotificationCompat.Builder

    private lateinit var callIntent: Intent
    private lateinit var notificationIntent: Intent
    private lateinit var callPendingIntent: PendingIntent
    private lateinit var notificationPendingIntent: PendingIntent

    override fun onCreate() {
        super.onCreate()
        Log.d(THIS_CLASS, "Starting AlarmService")

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        alarmManager = this.getSystemService(Context.ALARM_SERVICE) as AlarmManager

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

        val declineIntent = Intent(this, DeclineReceiver::class.java)

        @SuppressLint("UnspecifiedImmutableFlag")
        val declinePendingIntent = if (Build.VERSION.SDK_INT >= 23) {
            PendingIntent.getBroadcast(
                this,
                DECLINE_PENDING_INTENT_CODE,
                declineIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        } else {
            PendingIntent.getBroadcast(
                this,
                DECLINE_PENDING_INTENT_CODE,
                declineIntent,
                PendingIntent.FLAG_UPDATE_CURRENT)
        }

        // Build Notification
        builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Alarm")
            .setContentText("Setting up the alarm")
            .setSmallIcon(R.drawable.ic_baseline_notifications)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOnlyAlertOnce(true)
            .addAction(
                R.drawable.ic_baseline_cancel, "Cancel",
                declinePendingIntent
            )

        Log.d(THIS_CLASS, "Run startForeground()")
        startForeground(NOTIFICATION_ID, builder.build())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        callProfile = intent?.getParcelableExtra(MainActivity.PROFILE_EXTRA) ?: CallProfileData()

        settingIntent()
        setCallAlarm()
        if (callProfile.showNotificationText) updateNotification()

        return START_STICKY
    }

    private fun settingIntent() {
        //Setting intent for ScheduleNotificationService
        notificationIntent = Intent(this, ScheduleNotificationService::class.java)
            .putExtra(MainActivity.PROFILE_EXTRA, callProfile)

        @SuppressLint("UnspecifiedImmutableFlag")
        notificationPendingIntent = if (Build.VERSION.SDK_INT >= 23) {
            PendingIntent.getService(
                this, NOTIFICATION_PENDING_INTENT_CODE, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        } else {
            PendingIntent.getService(
                this, NOTIFICATION_PENDING_INTENT_CODE, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        // Setting intent for CallNotificationService
        callIntent = Intent(this, CallNotificationService::class.java)
            .putExtra(MainActivity.PROFILE_EXTRA, callProfile)

        @SuppressLint("UnspecifiedImmutableFlag")
        callPendingIntent = if (Build.VERSION.SDK_INT >= 23) {
            PendingIntent.getService(
                this, CALL_PENDING_INTENT_CODE, callIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
            )
        } else {
            PendingIntent.getService(
                this, CALL_PENDING_INTENT_CODE, callIntent, PendingIntent.FLAG_CANCEL_CURRENT
            )
        }
    }

    private fun updateNotification() {
        Log.d(THIS_CLASS, "Run updateNotification()")
        startService(notificationIntent)
        alarmManager.setRepeating(
            AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime() + TimeUnit.MINUTES.toMillis(1),
            TimeUnit.MINUTES.toMillis(1),
            notificationPendingIntent
        )
    }

    // Setting RCT_Wakeup directly to FlutterReceiver
    private fun setCallAlarm() {
        Log.d(THIS_CLASS, "Run setAlarm()")

        val c: Calendar = Calendar.getInstance()
        when (callProfile.scheduleData.clockType) {
            ClockType.TIMER -> {
                Log.d(THIS_CLASS, "Using timerType")
                c.apply {
                    timeInMillis = System.currentTimeMillis()
                    val setHour = get(Calendar.HOUR_OF_DAY) + callProfile.scheduleData.targetTime.hour
                    val setMinute = get(Calendar.MINUTE) + callProfile.scheduleData.targetTime.minute
                    val setSecond = get(Calendar.SECOND) + callProfile.scheduleData.targetTime.second

                    set(Calendar.HOUR_OF_DAY, setHour)
                    set(Calendar.MINUTE, setMinute)
                    set(Calendar.SECOND, setSecond)
                }
            }
            ClockType.ALARM -> {
                Log.d(THIS_CLASS, "Using alarmType")
                c.apply {
                    timeInMillis = System.currentTimeMillis()
                    set(Calendar.HOUR_OF_DAY, callProfile.scheduleData.targetTime.hour)
                    set(Calendar.MINUTE, callProfile.scheduleData.targetTime.minute)
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
                callPendingIntent
            ),
            callPendingIntent
        )
    }

    override fun onBind(p0: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.d(THIS_CLASS, "AlarmService is destroyed")

        alarmManager.cancel(callPendingIntent)
        alarmManager.cancel(notificationPendingIntent)
        notificationManager.cancel(NOTIFICATION_ID)

        super.onDestroy()
    }

    companion object {
        const val THIS_CLASS = "AlarmService"
        const val CHANNEL_ID = "onfake_id"
        const val CHANNEL_NAME = "Onfake Channel"
        const val NOTIFICATION_ID = 1
        const val DECLINE_PENDING_INTENT_CODE = 21
        const val CALL_PENDING_INTENT_CODE = 11
        const val NOTIFICATION_PENDING_INTENT_CODE = 12
    }
}