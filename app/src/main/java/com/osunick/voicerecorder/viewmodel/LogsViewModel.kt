package com.osunick.voicerecorder.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.osunick.voicerecorder.data.VoiceMessageRepository
import com.osunick.voicerecorder.model.VoiceMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
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

    fun saveMessage() {
        val voiceMessage = VoiceMessage(uiState.value.currentMessage ?: "", LocalDateTime.now())
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
        val voiceMessage = VoiceMessage(text, LocalDateTime.now())
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
}

data class LogsUiState(
    val currentMessage: String?,
    val isRecording: Boolean
)

sealed class LogEvent {
    data object None : LogEvent()
    data object Save: LogEvent()
    data object Share: LogEvent()
    data object StartRecording: LogEvent()
    data class UpdateLog(val logText: String): LogEvent()
}