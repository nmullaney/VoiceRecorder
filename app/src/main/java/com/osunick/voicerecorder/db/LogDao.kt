package com.osunick.voicerecorder.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface LogDao {
    @Query("SELECT * FROM logs")
    suspend fun getAll(): List<LogEntity>

    @Query("SELECT * FROM logs where label = :label")
    suspend fun getAllWithLabel(label: String?): List<LogEntity>

    @Query("SELECT * from logs WHERE label = :label ORDER BY datetime DESC LIMIT :limit OFFSET :offset")
    suspend fun getMostRecentWithLabel(label: String?, offset: Int, limit: Int): List<LogEntity>

    @Query("UPDATE logs SET label = :newLabel where label = :oldLabel")
    suspend fun updateLabel(oldLabel: String?, newLabel: String?)

    @Insert
    suspend fun insert(logEntity: LogEntity)

    @Query("DELETE FROM logs WHERE id = :id")
    suspend fun delete(id: Int)

    @Query("DELETE FROM logs where label = :label")
    suspend fun deleteAllByLabel(label: String?)
}