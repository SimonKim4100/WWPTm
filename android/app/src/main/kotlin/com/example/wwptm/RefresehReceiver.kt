package com.example.wwptm

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences

class RefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == "com.example.wwptm.ACTION_APP_REFRESH") {
            // Trigger app-level refresh logic here
            refreshPlateCountInApp(context)
        }
    }

    // Example of refresh logic (e.g., fetching new plate count)
    private fun refreshPlateCountInApp(context: Context) {
        // Update plate count in SharedPreferences or trigger any other refresh logic
        val sharedPref = context.getSharedPreferences("plate_data", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()

        // Example: Refresh the plate count (can include real data refresh logic here)
        val updatedPlateCount = 165  // Example logic for new plate count
        editor.putString("plate_count", updatedPlateCount.toString())
        editor.apply()

        // Notify that the data is updated, so the widget can also update
        val widgetIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
        widgetIntent.component = ComponentName(context, PlateCounterWidget::class.java)
        context.sendBroadcast(widgetIntent)
    }
}
