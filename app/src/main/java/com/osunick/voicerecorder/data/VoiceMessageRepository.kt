package com.osunick.voicerecorder.data

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.log
import androidx.paging.map
import com.osunick.voicerecorder.db.LogDao
import com.osunick.voicerecorder.db.LogEntity
import com.osunick.voicerecorder.model.LogLabels
import com.osunick.voicerecorder.model.VoiceMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class VoiceMessageRepository @Inject constructor(
    private val logDao: LogDao,
    private val dataStore: DataStore<Preferences>) {

    suspend fun addMessage(message: VoiceMessage) {
        logDao.insert(MessageMapper.mapToLogEntity(message, getSelectedLabel()))
        pagingDataSource?.invalidate()
    }

    suspend fun deleteMessage(id: Int) {
        logDao.delete(id)
        pagingDataSource?.invalidate()
    }

    suspend fun deleteAllMessage() {
        logDao.deleteAllByLabel(getSelectedLabel())
        pagingDataSource?.invalidate()
    }

    suspend fun getAllMessages() =
        logDao.getAll().map {
            MessageMapper.mapToVoiceMessage(it)
        }

    suspend fun getLastMessage() =
        logDao.getMostRecentWithLabel(getSelectedLabel(),0, 1).firstOrNull()

    fun refresh() {
        pagingDataSource?.invalidate()
    }

    private var pagingDataSource: LogPagingDataSource? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    fun messagePagerFlow(): Flow<PagingData<VoiceMessage>> =
        dataStore.data.flatMapLatest {
            Pager(
                PagingConfig(pageSize = PAGE_SIZE)
            ) {
                val selectedLabel = getSelectedLabel(it)
                Log.d("Repo", "Selected label for fetching: $selectedLabel")
                pagingDataSource = LogPagingDataSource(selectedLabel, logDao)
                pagingDataSource!!
            }.flow.map { pagingData ->
                pagingData.map {
                    MessageMapper.mapToVoiceMessage(it)
                }
            }
        }

    fun labelFlow(): Flow<LogLabels> =
        dataStore.data.map {
            val selectedLabel = getSelectedLabel(it)
            val dbLabels = logDao.getLabels()
            // ensure that we always include the selected label, even if there's no
            // logs with that label yet
            val allLabels = if (dbLabels.contains(selectedLabel)) dbLabels else {
                dbLabels.toMutableList().apply{ add(selectedLabel) }
            }
            LogLabels(
                selectedLabel = selectedLabel,
                allLabels = allLabels
            )
        }.flowOn(Dispatchers.IO)

    suspend fun editSelectedLabel(oldName: String?, newName: String?) {
        logDao.updateLabel(oldName, newName)
        setSelectedLabel(newName)
    }

    suspend fun setSelectedLabel(label: String?) {
        dataStore.edit {
            it[LABEL_PREF_KEY] = label ?: DEFAULT_LABEL
        }
    }

    private fun getSelectedLabel(preferences: Preferences): String =
        preferences[LABEL_PREF_KEY] ?: DEFAULT_LABEL

    private suspend fun getSelectedLabel(): String =
       dataStore.data.firstOrNull()?.get(LABEL_PREF_KEY) ?: DEFAULT_LABEL

    companion object {
        const val PAGE_SIZE = 10
        val LABEL_PREF_KEY = stringPreferencesKey("selected_label")
        const val DEFAULT_LABEL = ""
    }

}

object MessageMapper {

    fun mapToLogEntity(voiceMessage: VoiceMessage, label: String): LogEntity {
        return LogEntity(
            voiceMessage.id,
            voiceMessage.dateTime,
            voiceMessage.text,
            voiceMessage.label ?: label)
    }

    fun mapToVoiceMessage(logEntity: LogEntity): VoiceMessage {
        return VoiceMessage(logEntity.id, logEntity.text, logEntity.label, logEntity.datetime)
    }
}