package com.osunick.voicerecorder.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.ZonedDateTime

@Entity(tableName = "logs")
data class LogEntity(
    @PrimaryKey val id: Int?,
    val datetime: ZonedDateTime,
    val text: String,
    val label: String?)