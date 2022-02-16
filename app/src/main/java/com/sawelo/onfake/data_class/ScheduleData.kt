package com.sawelo.onfake.data_class

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ScheduleData(
    val clockType: ClockType,
    val hour: Int = 0,
    val minute: Int = 0,
    val second: Int = 0,
) : Parcelable

enum class ClockType {
    TIMER, ALARM
}