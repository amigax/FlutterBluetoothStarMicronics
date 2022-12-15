package com.example.attempt_one

import android.content.Intent
import android.util.Log
import androidx.annotation.NonNull
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity: FlutterActivity()
{
    private val CHANNEL = "com.example.attempt_one/doprint"

    override fun configureFlutterEngine(@NonNull flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(flutterEngine.dartExecutor, CHANNEL).setMethodCallHandler {
            // This method is invoked on the main thread.
                call, result ->
            if (call.method == "doPrint") {
                doPrint();
            } else {
                result.notImplemented()
            }
        }
    }

    private fun doPrint()
    {
        Log.d("GazNative", "this is native kotlin yeh..")
        Log.d("GazNative", "launching activity..")
        val discoveryIntent = Intent(this, MenuActivity::class.java)
        startActivity(discoveryIntent)
    }
}
