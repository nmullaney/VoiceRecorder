package com.osunick.voicerecorder

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.RemoteViews
import com.osunick.voicerecorder.data.VoiceMessageRepository
import com.osunick.voicerecorder.model.VoiceMessage
import com.osunick.voicerecorder.speech.OnSpeechEventListener
import com.osunick.voicerecorder.speech.VRSpeechRecognizer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import java.time.ZonedDateTime
import javax.inject.Inject


/**
 * Implementation of App Widget functionality.
 */
@AndroidEntryPoint
class VoiceRecorderWidget : AppWidgetProvider() {

    @Inject
    lateinit var messageRepository: VoiceMessageRepository

    private lateinit var speechRecognizer: VRSpeechRecognizer

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d(TAG, "onUpdate")
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
        val views = RemoteViews(context.packageName, R.layout.voice_recorder_widget)

        // Set the click listener for the button
        views.setOnClickPendingIntent(
            R.id.record_button,
            getPendingSelfIntent(context, RECORD_ACTION)
        )
        views.setOnClickPendingIntent(
            R.id.last_log,
            getPendingSelfIntent(context, REFRESH_ACTION)
        )
        views.setOnClickPendingIntent(
            R.id.open_button,
            openAppPendingIntent(context)
        )
        refreshLogTextUI(context)
        appWidgetManager.updateAppWidget(
            ComponentName(
                context,
                VoiceRecorderWidget::class.java
            ), views
        )
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }
    // Define a method to create a PendingIntent that will trigger an action in your widget provider
    private fun getPendingSelfIntent(context: Context, action: String): PendingIntent {
        val intent = Intent(context, VoiceRecorderWidget::class.java)
        intent.action = action
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    private fun openAppPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, VoiceRecorderActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    // Handle the button click action here
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        Log.d(TAG, "onReceive: ${intent.action}")
        if (RECORD_ACTION == intent.action) {
            updateUIForRecordingStart(context)
            Log.d(TAG, "onReceive: handling ${intent.action}")
            speechRecognizer = VRSpeechRecognizer(context, object: OnSpeechEventListener {
                override fun onRecognitionNotAvailable() {
                    // this is an error
                    Log.e(TAG, "No recognition available")
                }

                override fun onSpeechRecognized(speech: String) {
                    updateUIForRecordingStop(context)
                    Log.d(TAG, "Got speech: $speech")
                    // it would be nicer to do this in a WorkManager, but this will work
                    runBlocking {
                        Log.d(TAG, "Saving speech: $speech")
                        messageRepository.addMessage(
                            VoiceMessage(
                                text = speech,
                                dateTime = ZonedDateTime.now()
                            )
                        )
                        Log.d(TAG, "Speech Saved!")
                    }
                    refreshLogTextUI(context)
                }

                override fun onSpeechError(error: Int) {
                    updateUIForRecordingStop(context)
                    val errorMessage = context.getString(
                        when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH -> R.string.no_speech_detected
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> R.string.open_and_allow_audio
                            else -> R.string.recording_failed
                    })
                    updateLogTextUIWithText(context, errorMessage)
                }

                override fun onSpeechEnded() {
                    updateUIForRecordingStop(context)
                }

            })
            speechRecognizer.init()
            speechRecognizer.startListening()
        } else if (REFRESH_ACTION == intent.action) {
            refreshLogTextUI(context)
        }
    }

    private fun refreshLogTextUI(context: Context) {
        updateLogTextUI(context) {
            val lastMessage = messageRepository.getLastMessage()
            lastMessage?.text ?: context.getString(R.string.no_logs_saved)
        }
    }

    private fun updateLogTextUI(context: Context, getText: suspend () -> String) {
        val views = RemoteViews(context.packageName, R.layout.voice_recorder_widget)
        updateLogText(views, getText)
        val appWidgetManager = AppWidgetManager.getInstance(context)
        appWidgetManager.updateAppWidget(
            ComponentName(
                context,
                VoiceRecorderWidget::class.java
            ), views
        )
    }

    private fun updateLogText(views: RemoteViews, getText: suspend () -> String) {
        val logText = runBlocking {
            getText()
        }
        views.setTextViewText(R.id.last_log,logText)
    }

    private fun updateLogTextUIWithText(context: Context, textToDisplay: String) {
        val views = RemoteViews(context.packageName, R.layout.voice_recorder_widget)
        updateLogText(views) {
            textToDisplay
        }
        val appWidgetManager = AppWidgetManager.getInstance(context)
        appWidgetManager.updateAppWidget(
            ComponentName(
                context,
                VoiceRecorderWidget::class.java
            ), views
        )
    }

    private fun updateUIForRecordingStop(context: Context) {
        val views = RemoteViews(context.packageName, R.layout.voice_recorder_widget)
        views.setTextViewText(R.id.record_button, context.getString(R.string.record))
        views.setInt(
            R.id.record_button,
            "setBackgroundColor",
            context.getColor(R.color.light_blue_600))
        val appWidgetManager = AppWidgetManager.getInstance(context)
        appWidgetManager.updateAppWidget(
            ComponentName(
                context,
                VoiceRecorderWidget::class.java
            ), views
        )
    }

    private fun updateUIForRecordingStart(context: Context) {
        val views = RemoteViews(context.packageName, R.layout.voice_recorder_widget)
        views.setTextViewText(R.id.record_button, context.getString(R.string.recording))
        views.setInt(
            R.id.record_button,
            "setBackgroundColor",
            context.getColor(R.color.teal_200))
        val appWidgetManager = AppWidgetManager.getInstance(context)
        appWidgetManager.updateAppWidget(
            ComponentName(
                context,
                VoiceRecorderWidget::class.java
            ), views
        )
    }


    override fun onDeleted(context: Context?, appWidgetIds: IntArray?) {
        super.onDeleted(context, appWidgetIds)
        if (this::speechRecognizer.isInitialized) {
            speechRecognizer.destroy()
        }
    }

    companion object {
        const val TAG = "Widget"
        const val RECORD_ACTION = "RecordAction"
        const val REFRESH_ACTION = "RefreshAction"
    }
}

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    val widgetText = context.getString(R.string.record)
    // Construct the RemoteViews object
    val views = RemoteViews(context.packageName, R.layout.voice_recorder_widget)
    views.setTextViewText(R.id.record_button, widgetText)

    // Instruct the widget manager to update the widget
    appWidgetManager.updateAppWidget(appWidgetId, views)
}