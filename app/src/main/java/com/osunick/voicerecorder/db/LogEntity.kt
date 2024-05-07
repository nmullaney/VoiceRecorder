package com.osunick.voicerecorder.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "logs")
data class LogEntity(
    @PrimaryKey val id: Int?,
    val datetime: LocalDateTime,
    val text: String)