package com.sawelo.onfake.service

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.sawelo.onfake.AppDatabase
import com.sawelo.onfake.MainActivity
import com.sawelo.onfake.R
import com.sawelo.onfake.data_class.CallProfileData
import com.sawelo.onfake.data_class.ClockType
import com.sawelo.onfake.receiver.DeclineReceiver
import kotlinx.coroutines.*
import java.util.*

class AlarmService : Service() {

    private lateinit var callProfile: CallProfileData
    private lateinit var alarmManager: AlarmManager
    private lateinit var notificationManager: NotificationManager
    private lateinit var builder: NotificationCompat.Builder

    private lateinit var callIntent: Intent
    private lateinit var notificationIntent: Intent
    private lateinit var callPendingIntent: PendingIntent

    private var coroutineJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        IS_RUNNING = true
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
            .setContentText(DEFAULT_NOTIFICATION_TEXT)
            .setSmallIcon(R.drawable.ic_baseline_notifications)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOnlyAlertOnce(true)
            .addAction(
                R.drawable.ic_baseline_cancel, "Cancel",
                declinePendingIntent
            )
        startForeground(NOTIFICATION_ID, builder.build())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        callProfile = intent?.getParcelableExtra(MainActivity.PROFILE_EXTRA) ?: CallProfileData()

        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getInstance(this@AlarmService)
            var callProfileCheck: CallProfileData? = null
            while (callProfileCheck == null) {
                delay(500)
                db.callProfileDao().insertAll(callProfile)
                callProfileCheck = db.callProfileDao().getCallProfile().firstOrNull()
            }
        }

        settingIntent()
        setCallAlarm()
        if (callProfile.showNotificationText) updateNotification()

        return START_STICKY
    }

    private fun settingIntent() {
        //Setting intent for ScheduleNotificationService
        notificationIntent = Intent(this, ScheduleNotificationService::class.java)

        // Setting intent for CallNotificationService
        callIntent = Intent(this, CallNotificationService::class.java)

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
        coroutineJob = CoroutineScope(Dispatchers.IO).launch {
            while (IS_RUNNING) {
                delay(1000)
                startService(notificationIntent)
            }
        }
        coroutineJob?.start()
    }

    // Setting RCT_Wakeup directly to FlutterReceiver
    private fun setCallAlarm() {
        val c: Calendar = Calendar.getInstance()
        when (callProfile.scheduleData.clockType) {
            ClockType.TIMER -> {
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
                c.apply {
                    timeInMillis = System.currentTimeMillis()
                    val startHour = callProfile.scheduleData.startTime?.hour
                    val targetHour = callProfile.scheduleData.targetTime.hour
                    if (startHour != null && targetHour < startHour) {
                        set(Calendar.DAY_OF_YEAR,(c.get(Calendar.DAY_OF_YEAR) + 1))
                    }
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
        coroutineJob?.cancel()
        alarmManager.cancel(callPendingIntent)
        notificationManager.cancelAll()
        IS_RUNNING = false

        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "onfake_id"
        const val CHANNEL_NAME = "Onfake Channel"
        const val DEFAULT_NOTIFICATION_TEXT = "Setting up the alarm"
        const val NOTIFICATION_ID = 1
        const val DECLINE_PENDING_INTENT_CODE = 21
        const val CALL_PENDING_INTENT_CODE = 11
        var IS_RUNNING = false
    }
}