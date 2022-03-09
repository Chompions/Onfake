package com.sawelo.onfake.data_class

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sawelo.onfake.BuildConfig
import com.sawelo.onfake.R
import kotlinx.parcelize.Parcelize

@Entity
@Parcelize
data class CallProfileData(
    var name: String = CallProfileDefaultValue.nameValue,
    var photoUri: String = CallProfileDefaultValue.photoUriValue,
    var callScreen: CallScreen? = null,
    @PrimaryKey val scheduleData: ScheduleData = ScheduleData(ClockType.TIMER, TimeData()),
    val showNotificationText: Boolean = false
) : Parcelable

enum class CallScreen {
    WHATSAPP_FIRST, WHATSAPP_SECOND
}

object CallProfileDefaultValue {
    const val nameValue: String = "Citra"
    const val photoUriValue: String =
        ("android.resource://" + BuildConfig.APPLICATION_ID + "/" + R.drawable.default_profile_picture)
}