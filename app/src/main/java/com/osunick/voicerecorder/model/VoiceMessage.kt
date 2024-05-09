package com.osunick.voicerecorder.model

import java.time.ZonedDateTime

data class VoiceMessage(
    val id: Int? = null,
    val text: String,
    val label: String? = null,
    val dateTime: ZonedDateTime)
