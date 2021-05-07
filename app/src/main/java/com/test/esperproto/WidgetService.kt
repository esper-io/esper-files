@file:Suppress("DEPRECATION")

package com.test.esperproto

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.util.Log
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import java.io.File

val list1: ArrayList<String> = ArrayList()

class WidgetService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return ExampleWidgetItemFactory(applicationContext, intent)
    }

    internal class ExampleWidgetItemFactory(private val context: Context, intent: Intent) :
        RemoteViewsFactory {


        private val DEFAULT_FILE_DIRECTORY: String = Environment.getExternalStorageDirectory()
            .path + File.separator + "esperfiles" + File.separator

        private val appWidgetId: Int = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )

        override fun onCreate() {
            //connect to data source
            //SystemClock.sleep(3000);

            Log.d("Files", "Path: $DEFAULT_FILE_DIRECTORY")
            val directory = File(DEFAULT_FILE_DIRECTORY)
            val files = directory.listFiles()
            if (directory.canRead() && files != null) {
                Log.d("Files", "Size: " + files.size)
                for (file in files) {
                    Log.d("FILE", file.name)
                    list1.add(file.name)
                }
            } else Log.d("Null?", "it is null")
        }

        override fun onDataSetChanged() {
//            Toast.makeText(
//                context,
//                "Data Set Changed",
//                Toast.LENGTH_LONG
//            )
//                .show()
        }
        override fun onDestroy() {
            //close data source
        }

        override fun getCount(): Int {
            return list1.size
        }

        override fun getViewAt(position: Int): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.example_widget_item)
            views.setTextViewText(R.id.example_widget_item_text, list1[position])
            //SystemClock.sleep(500);
            return views
        }

        override fun getLoadingView(): RemoteViews? {
            return null
        }

        override fun getViewTypeCount(): Int {
            return 1
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun hasStableIds(): Boolean {
            return true
        }

        init {
            //list1.clear()
        }
    }
}