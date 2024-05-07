package com.osunick.voicerecorder.db

import androidx.room.TypeConverter
import java.time.LocalDateTime

class DateTimeTypeConverter {

    @TypeConverter
    fun fromDateTime(value: String?): LocalDateTime? {
        return value?.let { LocalDateTime.parse(value) }
    }

    @TypeConverter
    fun toDateTime(date: LocalDateTime?): String? {
        return date?.toString()
    }
}