package com.osunick.voicerecorder

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import com.osunick.voicerecorder.data.VoiceMessageRepository
import com.osunick.voicerecorder.model.VoiceMessage
import com.osunick.voicerecorder.speech.OnSpeechEventListener
import com.osunick.voicerecorder.speech.VRSpeechRecognizer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime
import javax.inject.Inject


/**
 * Implementation of App Widget functionality.
 */
@AndroidEntryPoint
class VoiceRecorderWidget() : AppWidgetProvider() {

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
            R.id.appwidget_button,
            getPendingSelfIntent(context, RECORD_ACTION)
        )
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

    // Handle the button click action here
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        Log.d(TAG, "onReceive: ${intent.action}")
        if (RECORD_ACTION == intent.action) {
            Log.d(TAG, "onReceive: handling ${intent.action}")
            speechRecognizer = VRSpeechRecognizer(context, object: OnSpeechEventListener {
                override fun onRecognitionNotAvailable() {
                    // this is an error
                    Log.e(TAG, "No recognition available")
                }

                override fun onSpeechRecognized(speech: String) {
                    Log.d(TAG, "Got speech: $speech")
                    // it would be nicer to do this in a WorkManager, but this will work
                    runBlocking {
                        Log.d(TAG, "Saving speech: $speech")
                        messageRepository.addMessage(
                            VoiceMessage(
                                text = speech,
                                dateTime = LocalDateTime.now()
                            )
                        )
                        Log.d(TAG, "Speech Saved!")
                    }
                }

            })
            speechRecognizer.init()
            speechRecognizer.startListening()
        }
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
    }
}

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    val widgetText = context.getString(R.string.appwidget_text)
    // Construct the RemoteViews object
    val views = RemoteViews(context.packageName, R.layout.voice_recorder_widget)
    views.setTextViewText(R.id.appwidget_button, widgetText)

    // Instruct the widget manager to update the widget
    appWidgetManager.updateAppWidget(appWidgetId, views)
}