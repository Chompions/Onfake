package com.sawelo.onfake.data_class

import android.net.Uri
import android.os.Parcelable
import com.sawelo.onfake.BuildConfig
import com.sawelo.onfake.R
import kotlinx.parcelize.Parcelize

@Parcelize
data class CallProfileData(
    var name: String = "Citra",
    var photoUri: Uri = Uri.parse(
        "android.resource://" +
                BuildConfig.APPLICATION_ID + "/" +
                R.drawable.default_profile_picture),
    var callScreen: CallScreen = CallScreen.WHATSAPP_FIRST,
    val scheduleData: ScheduleData = ScheduleData(ClockType.TIMER),
) : Parcelable

enum class CallScreen {
    WHATSAPP_FIRST, WHATSAPP_SECOND
}