package com.osunick.voicerecorder.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.osunick.voicerecorder.data.VoiceMessageRepository
import com.osunick.voicerecorder.model.VoiceMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject


@HiltViewModel
class LogsViewModel @Inject constructor(
    private val messageRepository: VoiceMessageRepository
): ViewModel() {
    val eventsFlow = MutableStateFlow<LogEvent>(LogEvent.None)
    val messageFlow: Flow<PagingData<VoiceMessage>> = messageRepository.messagePagerFlow()
        .cachedIn(viewModelScope)
    private val _uiState = MutableStateFlow(LogsUiState(currentMessage = null, isRecording = false))
    val uiState = _uiState.asStateFlow()



    fun updateMessage(newMessage: String?) {
        _uiState.update {
            it.copy(currentMessage = newMessage)
        }
    }

    fun deleteMessage(id: Int) {
        viewModelScope.launch {
            messageRepository.deleteMessage(id)
        }
    }

    fun saveMessage() {
        val voiceMessage = VoiceMessage(
            null,
            uiState.value.currentMessage ?: "",
            LocalDateTime.now())
        viewModelScope.launch {
            messageRepository.addMessage(voiceMessage)
        }
        _uiState.update {
            it.copy(currentMessage = null)
        }
    }

    fun setIsRecording() {
        _uiState.update {
            it.copy(isRecording = true)
        }
    }

    fun saveVoiceRecording(text: String) {
        val voiceMessage = VoiceMessage(null, text, LocalDateTime.now())
        viewModelScope.launch {
            messageRepository.addMessage(voiceMessage)
        }
        _uiState.update {
            it.copy(isRecording = false)
        }
    }

    fun clearEvent() {
        viewModelScope.launch {
            eventsFlow.emit(LogEvent.None)
        }
    }

    suspend fun createFile(logsDir: File): File =
        withContext(Dispatchers.IO) {
            val logFile = File.createTempFile("logs", ".txt", logsDir)
            val fos = FileOutputStream(logFile, false)
            try {
                messageRepository.getAllMessages().forEach {
                    val line = "${format(it.dateTime)}\t ${it.text}\n"
                    fos.write(line.toByteArray())
                }
            } catch (ioe: IOException) {
                Log.e("CreateFile", "Something went wrong", ioe)
            } finally {
                fos.flush()
                fos.close()
            }
            logFile
        }

    private fun format(localDateTime: LocalDateTime) =
        localDateTime.format(DateTimeFormatter.ISO_DATE_TIME)
}

data class LogsUiState(
    val currentMessage: String?,
    val isRecording: Boolean
)

sealed class LogEvent {
    data object None : LogEvent()
    data object Save: LogEvent()
    data object Share: LogEvent()

    data object DeleteAllLogs: LogEvent()
    data object StartRecording: LogEvent()
    data class UpdateLog(val logText: String): LogEvent()
    data class DeleteLog(val id: Int): LogEvent()
}