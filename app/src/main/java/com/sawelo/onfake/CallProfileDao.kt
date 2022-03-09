package com.sawelo.onfake

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.sawelo.onfake.data_class.CallProfileData

@Dao
interface CallProfileDao {
    @Query("SELECT * FROM callProfileData")
    suspend fun getCallProfile(): List<CallProfileData>

    @Insert
    suspend fun insertAll(vararg callProfileData: CallProfileData)

    @Delete
    suspend fun delete(vararg callProfileData: CallProfileData)
}