package com.sawelo.onfake.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.sawelo.onfake.service.AlarmService
import com.sawelo.onfake.service.NotificationService

class DeclineReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("DeclineReceiver", "Starting DeclineReceiver")

        val alarmIntent = Intent(context, AlarmService::class.java)
        val notificationIntent = Intent(context, NotificationService::class.java)

        context.stopService(alarmIntent)
        context.stopService(notificationIntent)
    }
}