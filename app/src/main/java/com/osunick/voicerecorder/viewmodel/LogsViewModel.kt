package com.osunick.voicerecorder.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.osunick.voicerecorder.data.VoiceMessageRepository
import com.osunick.voicerecorder.date.DateTimeConstants.PrettyDateFormatter
import com.osunick.voicerecorder.date.DateTimeConstants.UTCZoneId
import com.osunick.voicerecorder.model.LogLabels
import com.osunick.voicerecorder.model.VoiceMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject


@HiltViewModel
class LogsViewModel @Inject constructor(
    private val messageRepository: VoiceMessageRepository
): ViewModel() {
    val eventsFlow = MutableStateFlow<LogEvent>(LogEvent.None)
    val labelsFlow = messageRepository
        .labelFlow()
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.Eagerly, LogLabels("", listOf("")))
    val messageFlow: Flow<PagingData<VoiceMessage>> = messageRepository.messagePagerFlow()
        .cachedIn(viewModelScope)
    val _navEventsFlow = MutableSharedFlow<NavEvent>(
        replay = 1, extraBufferCapacity = 1,
        onBufferOverflow =
        BufferOverflow.DROP_OLDEST
    )
    val navEventsFlow = _navEventsFlow.asSharedFlow()
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
            _navEventsFlow.emit(NavEvent.Back)
        }
    }

    fun deleteAllMessages() {
        viewModelScope.launch {
            messageRepository.deleteAllMessage()
        }
    }

    fun saveMessage() {
        val message = uiState.value.currentMessage?.trim()
        if (!message.isNullOrBlank()) {
            val voiceMessage = VoiceMessage(
                text = message,
                dateTime = ZonedDateTime.now().withZoneSameInstant(UTCZoneId)
            )
            viewModelScope.launch {
                messageRepository.addMessage(voiceMessage)
            }
        }
        _uiState.update {
            it.copy(currentMessage = null)
        }
    }

    fun updateMessage(voiceMessage: VoiceMessage) {
        viewModelScope.launch {
            messageRepository.updateMessage(voiceMessage)
            _navEventsFlow.emit(NavEvent.Back)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            messageRepository.refresh()
        }
    }

    fun setIsRecording() {
        _uiState.update {
            it.copy(isRecording = true)
        }
    }

    fun endVoiceRecording() {
        _uiState.update {
            it.copy(isRecording = false)
        }
    }

    fun saveVoiceRecording(text: String) {
        val voiceMessage = VoiceMessage(
            text = text,
            dateTime = ZonedDateTime.now().withZoneSameInstant(UTCZoneId)
        )
        viewModelScope.launch {
            messageRepository.addMessage(voiceMessage)
        }
        _uiState.update {
            it.copy(isRecording = false)
        }
    }

    fun addLabel(newLabel: String) =
        viewModelScope.launch {
            messageRepository.setSelectedLabel(newLabel)
        }

    fun renameLabel(oldName: String, newName: String) =
        viewModelScope.launch {
            messageRepository.editSelectedLabel(oldName, newName)
        }

    fun setSelectedLabelToAll() =
        viewModelScope.launch {
            messageRepository.setSelectedLabel("")
        }

    fun setSelectedLabel(selectedLabel: String) =
        viewModelScope.launch {
            messageRepository.setSelectedLabel(selectedLabel)
        }

    fun clearEvent() {
        viewModelScope.launch {
            eventsFlow.emit(LogEvent.None)
        }
    }

    // This is for sharing.  We can only share a max of 1MB, but we're going to assume that the
    // amount of data is less than that and not handle the overflow case (for now).
    suspend fun createDataString(): String =
        withContext(Dispatchers.IO) {
            buildString {
                append("UTC Date\tLocal Date\tLabel\tLog\n")
                messageRepository.getAllMessages().forEach {
                    append("${formatUTC(it.dateTime)}\t${formatLocal(it.dateTime)}\t${it.label}\t${it.text}\n")
                }
            }
        }

    private fun formatUTC(zonedDateTime: ZonedDateTime) =
        zonedDateTime.toString()

    private fun formatLocal(zonedDateTime: ZonedDateTime) =
        zonedDateTime
            .withZoneSameInstant(ZoneId.systemDefault())
            .toLocalDateTime()
            .format(PrettyDateFormatter)
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
    data class UpdateLogMessage(val logText: String): LogEvent()
    data class UpdateLog(val voiceMessage: VoiceMessage): LogEvent()
    data class DeleteLog(val id: Int): LogEvent()
    data class CreateLabel(val newLabel: String): LogEvent()
    data class RenameLabel(val oldLabel: String, val newLabel: String): LogEvent()
    data class SelectLabel(val selectedLabel: String): LogEvent()
    data object SelectAllLabels: LogEvent()
}

sealed class NavEvent {
    data object Back : NavEvent()
    data object None : NavEvent()
}