package com.osunick.voicerecorder.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [LogEntity::class], version = 1)
@TypeConverters(DateTimeTypeConverter::class)
abstract class LogDatabase: RoomDatabase() {
    abstract fun logDao(): LogDao
}