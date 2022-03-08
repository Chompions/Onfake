package com.sawelo.onfake.data_class

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class DeclineData(
    val originInformation: String,
    val isDestroyAlarmService: Boolean,
    val isDestroyCallNotification: Boolean,
    val isDestroyCallScreenActivity: Boolean,
): Parcelable
