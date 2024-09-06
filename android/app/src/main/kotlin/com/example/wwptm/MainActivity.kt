package com.example.wwptm

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.annotation.NonNull
import io.flutter.embedding.android.FlutterActivity
import io.flutter.plugin.common.MethodChannel

class MainActivity: FlutterActivity() {

    private val CHANNEL = "com.example.wwptm/widget"

    override fun configureFlutterEngine(@NonNull flutterEngine: io.flutter.embedding.engine.FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            if (call.method == "sendUsername") {
                val username = call.argument<String>("username")
                if (username != null) {
                    // Store the username in SharedPreferences and notify the widget
                    storeUsernameAndNotifyWidget(username)
                    result.success("Username sent to widget")
                } else {
                    result.error("ERROR", "Username is null", null)
                }
            } else {
                result.notImplemented()
            }
        }
    }

    private fun storeUsernameAndNotifyWidget(username: String) {
        // Store username in SharedPreferences
        val sharedPref: SharedPreferences = getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("username", username)
            apply()
        }

        // Send broadcast to update the widget
        val intent = Intent(this, PlateCounterWidget::class.java)
        intent.action = "com.example.wwptm.ACTION_APP_REFRESH"
        intent.putExtra("username", username)
        sendBroadcast(intent)
    }
}
