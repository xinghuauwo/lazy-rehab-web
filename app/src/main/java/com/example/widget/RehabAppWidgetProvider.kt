package com.example.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent

class RehabAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        val views = RehabWidgetSync.buildRemoteViews(context)
        appWidgetIds.forEach { appWidgetId ->
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_NEXT_QUOTE -> RehabWidgetSync.cycleQuote(context)
            ACTION_NEXT_SPRITE -> RehabWidgetSync.cycleSprite(context)
        }
    }

    companion object {
        const val ACTION_NEXT_QUOTE = "com.example.widget.ACTION_NEXT_QUOTE"
        const val ACTION_NEXT_SPRITE = "com.example.widget.ACTION_NEXT_SPRITE"
    }
}
