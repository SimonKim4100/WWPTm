package com.example.wwptm

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import android.widget.RemoteViews
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class PlateCounterWidget : AppWidgetProvider() {

    private val maxPlateCount = 240
    private val TAG = "PlateCounterWidget"

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Initialize Firebase for the widget
        FirebaseApp.initializeApp(context)

        Log.d(TAG, "onUpdate called: updating widgets")

        for (appWidgetId in appWidgetIds) {
            // Create an Intent to launch when the refresh button is clicked
            val intent = Intent(context, PlateCounterWidget::class.java)
            intent.action = "com.example.wwptm.ACTION_REFRESH_WIDGET"

            val pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Get the layout for the widget and attach the refresh button's click listener
            val views = RemoteViews(context.packageName, R.layout.plate_counter_widget)
            views.setOnClickPendingIntent(R.id.button_refresh, pendingIntent)

            // Fetch the username and then proceed with the data fetching
            fetchUsernameAndUpdatePlateCount(context, views, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        Log.d(TAG, "onReceive called with action: ${intent.action}")

        if (intent.action == "com.example.wwptm.ACTION_REFRESH_WIDGET") {
            // Log the button click event
            Log.d(TAG, "Refresh button clicked!")

            val appWidgetManager = AppWidgetManager.getInstance(context)
            val views = RemoteViews(context.packageName, R.layout.plate_counter_widget)
            val appWidgetIds = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, PlateCounterWidget::class.java))

            // Update all the widgets with new data
            for (appWidgetId in appWidgetIds) {
                Log.d(TAG, "Updating widget with ID: $appWidgetId after refresh button click")
                fetchUsernameAndUpdatePlateCount(context, views, appWidgetManager, appWidgetId)
            }
        } else if (intent.action == "com.example.wwptm.ACTION_APP_REFRESH") {
            // Extract username from intent
            val username = intent.getStringExtra("username")
            if (username != null) {
                // Save the username in SharedPreferences
                val sharedPref = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
                with(sharedPref.edit()) {
                    putString("username", username)
                    apply()
                }
                Log.d(TAG, "Username saved in SharedPreferences: $username")
            } else {
                Log.e(TAG, "Received null username in onReceive")
            }
        }
    }

    private fun fetchUsernameAndUpdatePlateCount(
        context: Context,
        views: RemoteViews,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        val username = sharedPreferences.getString("username", null)

        if (username != null) {
            Log.d(TAG, "Fetched username from SharedPreferences: $username")

            // Now fetch and update the plate count based on the username
            fetchAndUpdatePlateCount(context, username, views, appWidgetManager, appWidgetId)
        } else {
            Log.e(TAG, "Username not found in SharedPreferences")
            views.setTextViewText(R.id.text_plate_count, "No Username")
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    // Fetch the plate count and time from Firebase and update the widget
    private fun fetchAndUpdatePlateCount(
        context: Context,
        username: String,
        views: RemoteViews,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val firestore = FirebaseFirestore.getInstance()

        Log.d(TAG, "Fetching plate count from Firebase for widget ID: $appWidgetId")

        firestore.collection(username)  // Use the username as the collection name
            .document("save")
            .get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot != null) {
                    Log.d(TAG, "DocumentSnapshot data: ${documentSnapshot.data}")

                    val plateData = documentSnapshot.getString("plate_number") ?: "0/240"
                    val plateParts = plateData.split("/")
                    val plate = plateParts[0].toIntOrNull() ?: 0

                    val timestamp = documentSnapshot.getString("timestamp") ?: ""

                    // Log the fetched timestamp for debugging
                    Log.d(TAG, "Fetched timestamp: $timestamp")

                    val dbTime = parseTimestamp(timestamp)
                    val currentTime = Date()

                    // Log the parsed dbTime for further debugging
                    Log.d(TAG, "Parsed dbTime: $dbTime")

                    // Calculate time difference in minutes
                    val timeDifferenceInMinutes = ((currentTime.time - dbTime.time) / (1000 * 60)).toInt()

                    // Perform the plate calculation
                    var updatedPlateCount = plate + (timeDifferenceInMinutes / 6)
                    if (updatedPlateCount > maxPlateCount) {
                        updatedPlateCount = maxPlateCount
                    }

                    Log.d(TAG, "Updated plate count: $updatedPlateCount")

                    // Update the widget text with the actual plate count
                    views.setTextViewText(R.id.text_plate_count, "$updatedPlateCount/$maxPlateCount")
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to fetch plate count: ${exception.message}")
                views.setTextViewText(R.id.text_plate_count, "Error")
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
    }

    // Helper method to parse the timestamp from Firestore
    private fun parseTimestamp(timestamp: String): Date {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            format.parse(timestamp) ?: Date()  // Return current date on failure
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse timestamp: $timestamp")
            Date()  // Return current date if parsing fails
        }
    }
}
