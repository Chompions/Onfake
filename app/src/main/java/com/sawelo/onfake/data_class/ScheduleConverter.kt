package com.sawelo.onfake.data_class

import androidx.room.TypeConverter
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ScheduleConverter {
    @TypeConverter
    fun scheduleToJson(scheduleData: ScheduleData): String {
        return Json.encodeToString(scheduleData)
    }

    @TypeConverter
    fun jsonToSchedule(json: String): ScheduleData {
        return Json.decodeFromString(json)
    }
}