package com.osunick.voicerecorder.date

import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DateTimeConstants {
    val UTCZoneId = ZoneId.of("UTC")
    val PrettyDateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a")
}