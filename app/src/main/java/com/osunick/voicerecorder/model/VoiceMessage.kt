package com.osunick.voicerecorder.model

import android.os.Parcelable
import java.time.ZonedDateTime
import kotlinx.parcelize.Parcelize

@Parcelize
data class VoiceMessage(
    val id: Int? = null,
    val text: String,
    val label: String? = null,
    val dateTime: ZonedDateTime) : Parcelable
