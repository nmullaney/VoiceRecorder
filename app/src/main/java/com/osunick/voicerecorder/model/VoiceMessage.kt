package com.osunick.voicerecorder.model

import java.time.LocalDateTime

data class VoiceMessage(val id: Int? = null, val text: String, val dateTime: LocalDateTime)
