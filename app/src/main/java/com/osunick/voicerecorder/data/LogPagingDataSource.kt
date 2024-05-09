package com.osunick.voicerecorder.data

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.osunick.voicerecorder.db.LogDao
import com.osunick.voicerecorder.db.LogEntity

class LogPagingDataSource(
    private val labelQuery: String?,
    private val logDao: LogDao
): PagingSource<Int, LogEntity>() {

    override fun getRefreshKey(state: PagingState<Int, LogEntity>): Int = 0

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, LogEntity> {
        Log.d("LogPagingDataSource","Loading with key: ${params.key}, loadSize: ${params.loadSize} ")
        val limit = params.loadSize
        val offset = params.key ?: 0
        Log.d("LogPagingDataSource","Loading with offset: $offset, limit: $limit")
        return try {
            val logEntities = logDao.getMostRecentWithLabel(labelQuery, offset, limit)
            val nextKey = if (logEntities.size < limit) null else offset + logEntities.size
            Log.d("LogPagingDataSource","Result with nextKey: $nextKey")
            LoadResult.Page(
                data = logEntities,
                nextKey = nextKey,
                prevKey = null
            )
        } catch (exception: Exception) {
            Log.e("LogPagingDataSource", "Failed to get logs", exception)
            LoadResult.Error(exception)
        }

    }
}