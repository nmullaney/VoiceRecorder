package com.osunick.voicerecorder.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.map
import com.osunick.voicerecorder.db.LogDao
import com.osunick.voicerecorder.db.LogEntity
import com.osunick.voicerecorder.model.VoiceMessage
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class VoiceMessageRepository @Inject constructor(private val logDao: LogDao) {

    suspend fun addMessage(message: VoiceMessage) {
        logDao.insert(MessageMapper.mapToLogEntity(message))
        pagingDataSource?.invalidate()
    }

    suspend fun getAllMessages() =
        logDao.getAll().map {
            MessageMapper.mapToVoiceMessage(it)
        }

    private var pagingDataSource: LogPagingDataSource? = null

    fun messagePagerFlow() =
        Pager(
            PagingConfig(pageSize = PAGE_SIZE)
        ) {
            pagingDataSource = LogPagingDataSource(logDao)
            pagingDataSource!!
        }.flow.map { pagingData ->
            pagingData.map {
                MessageMapper.mapToVoiceMessage(it)
            }
        }

    companion object {
        const val PAGE_SIZE = 10
    }

}

object MessageMapper {

    fun mapToLogEntity(voiceMessage: VoiceMessage): LogEntity {
        return LogEntity(null, voiceMessage.dateTime, voiceMessage.text)
    }

    fun mapToVoiceMessage(logEntity: LogEntity): VoiceMessage {
        return VoiceMessage(logEntity.text, logEntity.datetime)
    }
}