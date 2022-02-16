package com.sawelo.onfake.`object`

import android.util.Log
import com.sawelo.onfake.data_class.ClockType
import com.sawelo.onfake.data_class.ScheduleData
import java.util.*

object UpdateTextObject {

    /**
     *  This function will return string for mainScheduleText in MainActivity
     *  and notificationText in AlarmService respectively
     *
     *  It will run while user adjust the alarm settings
     *  Result will vary according to clockType to adjust input time type
     */
    fun updateMainText(
        scheduleData: ScheduleData
    ): Pair<String, String> {
        var mainScheduleText = ""
        var notificationText = ""
        val displayText: String

        // Get current time from phone
        val c: Calendar = Calendar.getInstance()
        val currentHour = c.get(Calendar.HOUR_OF_DAY)
        val currentMinute = c.get(Calendar.MINUTE)

        // Get time data from constructor
        val hour = scheduleData.hour
        val minute = scheduleData.minute
        val second = scheduleData.second
        val clockType = scheduleData.clockType

        /**
         * Adjust number to return singular or plural suffix.
         * Example: 1 hour; 2 hours
         */
        fun adjustNum(numValue: Int, singular: String, plural: String): String {
            return when (numValue) {
                0 -> ""
                1 -> "1 $singular"
                else -> "$numValue $plural"
            }
        }

        when (clockType) {
            // This will return text for timer type
            ClockType.TIMER -> {
                /**
                 * Setting setHour and setMinute according to input, also limiting upper limit to
                 * 24 hours and 60 minutes each. If setMinute goes beyond upper limit, then the rest
                 * will be added to setHour.
                 */

                // Get incoming call exact starting time
                var setHour = (currentHour + hour) % 24
                var setMinute = (currentMinute + minute)
                while (setMinute > 60) {
                    setHour++
                    setMinute %= 60
                }

                // Set padding for setHour & setMinute
                val hourPad: String = setHour.toString().padStart(2, '0')
                val minutePad: String = setMinute.toString().padStart(2, '0')

                val textHour: String = adjustNum(hour, "hour", "hours")
                val textMinute = adjustNum(minute, "minute", "minutes")
                val textSecond = adjustNum(second, "second", "seconds")

                // Checks if all val is not blank
                displayText =
                    if (textHour.isNotBlank() && textMinute.isNotBlank() && textSecond.isNotBlank()) {
                        // Use already existing data with updateRelativeTime() in digits
                        // (example text format = 00:00:00)
                        val hourPadToDisplay: String = hour.toString().padStart(2, '0')
                        val minutePadToDisplay: String =
                            minute.toString().padStart(2, '0')
                        val secondPadToDisplay: String =
                            second.toString().padStart(2, '0')
                        " $hourPadToDisplay:$minutePadToDisplay:$secondPadToDisplay"
                    } else {
                        // Otherwise use sets of string to represent time
                        // (example text format = 2 hours 3 minutes)
                        val builder = StringBuilder()
                        if (textHour.isNotBlank()) builder.append(" ").append(textHour)
                        if (textMinute.isNotBlank()) builder.append(" ").append(textMinute)
                        if (textSecond.isNotBlank()) builder.append(" ").append(textSecond)
                        builder.toString()
                    }

                // Set now or else
                if (hour == 0 && minute == 0 && second == 0) {
                    mainScheduleText = "Now"
                    notificationText = "The call is starting now"
                } else {
                    mainScheduleText = displayText
                    notificationText = "Preparing call for$displayText ($hourPad:$minutePad)"
                }

            }
            // This will return text for alarm type
            ClockType.ALARM -> {
                val hourPad: String = hour.toString().padStart(2, '0')
                val minutePad: String = minute.toString().padStart(2, '0')

                // dataMinuteOfDay is total time from 00:00 to target time in minutes
                val dataMinuteOfDay = (hour * 60) + minute
                println("dataMinuteDay: $dataMinuteOfDay")
                // currentMinuteOfDay is total time from 00:00 until now in minutes
                val currentMinuteOfDay = (currentHour * 60) + currentMinute
                println("currentMinuteDay: $currentMinuteOfDay")
                // minuteOfDay is total time from now to target time in minutes
                val minuteOfDay = dataMinuteOfDay - currentMinuteOfDay

                val textHour: String = adjustNum(minuteOfDay / 60, "hour", "hours")
                val textMinute = adjustNum(minuteOfDay % 60, "minute", "minutes")

                val builder = StringBuilder()
                if (textHour.isNotBlank()) builder.append(" ").append(textHour)
                if (textMinute.isNotBlank()) builder.append(" ").append(textMinute)
                displayText = builder.toString()

                if (dataMinuteOfDay <= currentMinuteOfDay) {
                    mainScheduleText = "Now"
                    notificationText = "The call is starting now"
                } else {
                    mainScheduleText = "$hourPad:$minutePad"
                    notificationText = "Preparing call for$displayText ($hourPad:$minutePad)"
                }
            }
        }
        Log.d("UpdateTextObject", "mainScheduleText is $mainScheduleText")
        Log.d("UpdateTextObject", "notificationText is $notificationText")

        return Pair(mainScheduleText, notificationText)
    }
}