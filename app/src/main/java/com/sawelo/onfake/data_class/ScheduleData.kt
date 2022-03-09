package com.sawelo.onfake.data_class

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class ScheduleData(
    val clockType: ClockType,
    var targetTime: TimeData,
    var startTime: TimeData? = null,
): Parcelable

@Serializable
@Parcelize
data class TimeData(
    var hour: Int = 0,
    var minute: Int = 0,
    var second: Int = 0,
): Parcelable

enum class ClockType {
    TIMER, ALARM
}