package com.osunick.voicerecorder.db

import androidx.room.TypeConverter
import java.time.ZoneId
import java.time.ZonedDateTime

class DateTimeTypeConverter {

    @TypeConverter
    fun fromDateTime(value: String?): ZonedDateTime? {
        return value?.let { ZonedDateTime.parse(value).withZoneSameInstant(ZoneId.of("UTC")) }
    }

    @TypeConverter
    fun toDateTime(date: ZonedDateTime?): String? {
        return date?.withZoneSameInstant(ZoneId.of("UTC")).toString()
    }
}