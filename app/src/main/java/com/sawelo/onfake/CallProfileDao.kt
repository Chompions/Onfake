package com.sawelo.onfake

import androidx.room.*
import com.sawelo.onfake.data_class.CallProfileData

@Dao
interface CallProfileDao {
    @Query("SELECT * FROM callProfileData")
    suspend fun getCallProfile(): List<CallProfileData>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg callProfileData: CallProfileData)

    @Delete
    suspend fun delete(vararg callProfileData: CallProfileData)
}