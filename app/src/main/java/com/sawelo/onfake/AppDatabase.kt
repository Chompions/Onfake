package com.sawelo.onfake

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.sawelo.onfake.data_class.CallProfileData
import com.sawelo.onfake.data_class.ScheduleConverter

@Database(entities = [CallProfileData::class], version = 1)
@TypeConverters(ScheduleConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun callProfileDao(): CallProfileDao

    companion object {
        private var databaseInstance: AppDatabase? = null
        fun getInstance(context: Context): AppDatabase {
            if (databaseInstance == null) {
                databaseInstance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java, "call_profile"
                ).build()
            }
            return databaseInstance as AppDatabase
        }
    }
}