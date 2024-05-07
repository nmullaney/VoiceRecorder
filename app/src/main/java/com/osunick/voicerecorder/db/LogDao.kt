package com.osunick.voicerecorder.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface LogDao {
    @Query("SELECT * FROM logs")
    suspend fun getAll(): List<LogEntity>

    @Query("SELECT * from logs ORDER BY datetime DESC LIMIT :limit OFFSET :offset")
    suspend fun getMostRecent(offset: Int, limit: Int): List<LogEntity>

    @Insert
    suspend fun insert(logEntity: LogEntity)
}