package com.sawelo.onfake.`object`

import android.content.Context
import android.content.Intent
import com.sawelo.onfake.CallScreenActivity
import com.sawelo.onfake.MainActivity
import com.sawelo.onfake.data_class.DeclineData
import com.sawelo.onfake.service.AlarmService
import com.sawelo.onfake.service.CallNotificationService

object DeclineObject {
    fun declineFunction(context: Context, declineData: DeclineData) {
        val alarmIntent = Intent(context, AlarmService::class.java)
        val callNotificationIntent = Intent(context, CallNotificationService::class.java)
        val callScreenActivityIntent =  Intent(CallScreenActivity.DESTROY_CALL_SCREEN_ACTIVITY)

        if (declineData.isDestroyAlarmService) context.stopService(alarmIntent)
        if (declineData.isDestroyCallNotification) context.stopService(callNotificationIntent)
        if (declineData.isDestroyCallScreenActivity) context.sendBroadcast(callScreenActivityIntent)
        if (declineData.isDeactivateCallMainActivity) context.sendBroadcast(Intent(MainActivity.DEACTIVATE_CALL))
    }
}