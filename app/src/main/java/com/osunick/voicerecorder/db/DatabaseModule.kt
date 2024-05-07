package com.osunick.voicerecorder.db

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Singleton
    @Provides
    fun providesLogDatabase(
        @ApplicationContext app: Context
    ) = Room.databaseBuilder(
        app,
        LogDatabase::class.java,
        "log_database"
    ).build()

    @Singleton
    @Provides
    fun providesLogDao(db: LogDatabase): LogDao = db.logDao()
}