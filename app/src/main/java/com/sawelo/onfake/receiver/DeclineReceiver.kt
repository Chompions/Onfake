package com.sawelo.onfake.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sawelo.onfake.`object`.DeclineObject
import com.sawelo.onfake.data_class.DeclineData

class DeclineReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val declineData = DeclineData(
            originInformation = "DeclineReceiver to destroy everything",
            isDestroyAlarmService = true,
            isDestroyCallNotification = true,
            isDestroyCallScreenActivity = true
        )
        DeclineObject.declineFunction(context, declineData)
    }
}