package com.sawelo.onfake.service

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.sawelo.onfake.AppDatabase
import com.sawelo.onfake.R
import com.sawelo.onfake.`object`.UpdateTextObject
import com.sawelo.onfake.receiver.DeclineReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ScheduleNotificationService : Service() {

    private lateinit var builder: NotificationCompat.Builder
    private lateinit var notificationManager: NotificationManager

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(THIS_CLASS, "Starting $THIS_CLASS")

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val declineIntent = Intent(this, DeclineReceiver::class.java)

        @SuppressLint("UnspecifiedImmutableFlag")
        val declinePendingIntent = if (Build.VERSION.SDK_INT >= 23) {
            PendingIntent.getBroadcast(
                this,
                AlarmService.DECLINE_PENDING_INTENT_CODE,
                declineIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        } else {
            PendingIntent.getBroadcast(
                this,
                AlarmService.DECLINE_PENDING_INTENT_CODE,
                declineIntent,
                PendingIntent.FLAG_UPDATE_CURRENT)
        }

        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getInstance(this@ScheduleNotificationService)
            val callProfile = db.callProfileDao().getCallProfile().first()

            Log.d(THIS_CLASS, "Current target data: ${callProfile.scheduleData.targetTime}")
            Log.d(THIS_CLASS, "Current start data: ${callProfile.scheduleData.startTime}")

            val notificationText = UpdateTextObject.updateMainText(
                callProfile.scheduleData).second

            // Build Notification
            builder = NotificationCompat.Builder(this@ScheduleNotificationService, AlarmService.CHANNEL_ID)
                .setContentTitle("Onfake")
                .setContentText(notificationText)
                .setSmallIcon(R.drawable.ic_baseline_notifications)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setOnlyAlertOnce(true)
                .addAction(
                    R.drawable.ic_baseline_cancel, "Cancel",
                    declinePendingIntent
                )

            notificationManager.notify(AlarmService.NOTIFICATION_ID, builder.build())
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? = null

    override fun onDestroy() {
        Log.d(THIS_CLASS, "$THIS_CLASS is destroyed")
        super.onDestroy()
    }

    companion object {
        const val THIS_CLASS = "ScheduleService"
    }
}