package com.test.esperproto

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.RemoteViews
import android.widget.Toast
import com.test.esperproto.activity.MainActivity
import java.util.*


class ExampleAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(context, 0, intent, 0)

            val serviceIntent = Intent(context, ExampleWidgetService::class.java)
            serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            serviceIntent.data = Uri.parse(serviceIntent.toUri(Intent.URI_INTENT_SCHEME))
            val views = RemoteViews(context.packageName, R.layout.example_widget)
            views.setOnClickPendingIntent(R.id.example_widget_text, pendingIntent)

            views.setRemoteAdapter(R.id.example_widget_stack_view, serviceIntent)
            views.setEmptyView(R.id.example_widget_stack_view, R.id.example_widget_empty_view)

            val appWidgetOptions = appWidgetManager.getAppWidgetOptions(appWidgetId)
            resizeWidget(appWidgetOptions, views)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

//    override fun onReceive(context: Context?, intent: Intent) {
//        if (intent.action.equals(AppWidgetManager.ACTION_APPWIDGET_UPDATE)) {
//            val extras = intent.extras
//            if (extras != null) {
//                val appWidgetIds = extras.getIntArray(AppWidgetManager.EXTRA_APPWIDGET_IDS)
//                if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
//                    Toast.makeText(context, "REFRESH", Toast.LENGTH_SHORT).show()
//                    onUpdate(context!!, AppWidgetManager.getInstance(context), appWidgetIds)
//                }
//            }
//        }
//    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        val views = RemoteViews(context.packageName, R.layout.example_widget)
        resizeWidget(newOptions, views)
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun resizeWidget(appWidgetOptions: Bundle, views: RemoteViews) {
        val minWidth = appWidgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val maxWidth = appWidgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
        val minHeight = appWidgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
        val maxHeight = appWidgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        //Toast.makeText(context, "onDeleted", Toast.LENGTH_SHORT).show()
    }

    override fun onEnabled(context: Context) {
        //Toast.makeText(context, "onEnabled", Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context) {
        //Toast.makeText(context, "onDisabled", Toast.LENGTH_SHORT).show()
    }
}