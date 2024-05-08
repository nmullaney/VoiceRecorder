package com.osunick.voicerecorder

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import android.widget.RemoteViews

/**
 * Implementation of App Widget functionality.
 */
class VoiceRecorderWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
        val views = RemoteViews(context.packageName, R.layout.voice_recorder_widget)

        // Set the click listener for the button
        views.setOnClickPendingIntent(
            R.id.appwidget_button,
            getPendingSelfIntent(context, "MY_ACTION")
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
        val intent = Intent(context, javaClass)
        intent.action = action
        return PendingIntent.getBroadcast(context, 0, intent, 0)
    }

    // Handle the button click action here
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if ("MY_ACTION" == intent.action) {
            // Call your function here
        }
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