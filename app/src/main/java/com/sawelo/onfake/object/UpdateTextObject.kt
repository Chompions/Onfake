package com.sawelo.onfake.`object`

import android.util.Log
import com.sawelo.onfake.data_class.ClockType
import com.sawelo.onfake.data_class.ScheduleData
import kotlinx.datetime.*

object UpdateTextObject {

    private const val THIS_CLASS = "UpdateTextObject"

    /**
     *  This function will return string for mainScheduleText in MainActivity
     *  and notificationText in AlarmService respectively
     *
     *  It will run while user adjust the alarm settings
     *  Result will vary according to clockType to adjust input time type
     */
    fun updateMainText(
        scheduleData: ScheduleData,
    ): Pair<String, String> {
        val mainScheduleText: String
        val notificationText: String
        val displayText: String

        val timeZone = TimeZone.currentSystemDefault()
        val now = Clock.System.now()
        val nowDateTime = now.toLocalDateTime(timeZone)

        val clockType = scheduleData.clockType
        val targetTime = LocalDateTime(
            nowDateTime.year,
            nowDateTime.monthNumber,
            nowDateTime.dayOfMonth,
            scheduleData.targetTime.hour,
            scheduleData.targetTime.minute,
            scheduleData.targetTime.second
        )

        var startTime: LocalDateTime? = null
        if (scheduleData.startTime != null) {
            startTime = LocalDateTime(
                nowDateTime.year,
                nowDateTime.monthNumber,
                nowDateTime.dayOfMonth,
                scheduleData.startTime?.hour ?: 0,
                scheduleData.startTime?.minute ?: 0,
                scheduleData.startTime?.second ?: 0
            )
        }

        val targetInstantFromNow: Instant
        val differencePeriod: DateTimePeriod

        when (clockType) {
            ClockType.ALARM -> {
                targetInstantFromNow = targetTime.toInstant(timeZone)
                differencePeriod = now.periodUntil(
                    targetInstantFromNow, timeZone)
            }
            ClockType.TIMER -> {
                if (startTime != null) {
                    Log.d(THIS_CLASS, "targetTime: $targetTime")
                    Log.d(THIS_CLASS, "startTime: $startTime")

                    val period = DateTimePeriod(
                        hours = targetTime.hour,
                        minutes = targetTime.minute,
                        seconds = targetTime.second
                    )
                    val startInstant = startTime.toInstant(timeZone)

                    targetInstantFromNow = startInstant.plus(period, timeZone)
                    differencePeriod = now.periodUntil(
                        targetInstantFromNow, timeZone)

                } else {
                    targetInstantFromNow = Instant.DISTANT_FUTURE
                    differencePeriod = DateTimePeriod(
                        hours = targetTime.hour,
                        minutes = targetTime.minute,
                        seconds = targetTime.second
                    )
                }
            }
        }

        Log.d(THIS_CLASS, "differencePeriod: ${differencePeriod.seconds}")

        val targetDateTime = targetInstantFromNow.toLocalDateTime(timeZone)
        val targetText = String.format(
            "%02d:%02d", targetDateTime.hour, targetDateTime.minute)
        val differenceText = String.format(
            "%02d:%02d:%02d", differencePeriod.hours, differencePeriod.minutes, differencePeriod.seconds)

        Log.d(THIS_CLASS, "targetText: $targetText")
        Log.d(THIS_CLASS, "differenceText: $differenceText")

        /**
         * Adjust number to return singular or plural suffix.
         * Example: 1 hour; 2 hours
         */
        fun Int.pluralText(text: String): String {
            return when {
                (this > 1) -> "$this ${text}s"
                (this == 1) -> "$this $text"
                else -> ""
            }
        }

        val textHour = differencePeriod.hours.pluralText("hour")
        val textMinute = differencePeriod.minutes.pluralText("minute")
        val textSecond = differencePeriod.seconds.pluralText("second")

        // Checks if all val is not blank
        val builder = StringBuilder()
        if (textHour.isNotBlank()) builder.append(" ").append(textHour)
        if (textMinute.isNotBlank()) builder.append(" ").append(textMinute)
        if (textSecond.isNotBlank()) builder.append(" ").append(textSecond)
        displayText = builder.toString()

        // Set now or else
        if (differencePeriod.hours <= 0 && differencePeriod.minutes <= 0 && differencePeriod.seconds <= 0) {
            mainScheduleText = "Now"
            notificationText = "Preparing call"
        } else {
            mainScheduleText = when(clockType) {
                ClockType.TIMER -> displayText
                ClockType.ALARM -> targetText
            }
            notificationText = "Setting call for$displayText ($targetText)"
        }

        Log.d("UpdateTextObject", "mainScheduleText is $mainScheduleText")
        Log.d("UpdateTextObject", "notificationText is $notificationText")

        return Pair(mainScheduleText, notificationText)
    }
}