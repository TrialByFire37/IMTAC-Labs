package com.example.servicelabapp

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class SystemInfoWorker(appContext: Context, params: WorkerParameters)
    : Worker(appContext, params) {

    private val serverUrl = "http://10.0.2.2:8080/upload"

    override fun doWork(): Result {
        return try {
            val json = collectSystemInfo()
            send(json)
            Result.success()
        } catch (e: Exception) {
            Log.e("SystemInfoWorker", "Error: ${e.message}")
            Result.failure()
        }
    }

    private fun collectSystemInfo(): JSONObject {
        val obj = JSONObject()
        obj.put("os_version", android.os.Build.VERSION.RELEASE)
        obj.put("sdk_version", android.os.Build.VERSION.SDK_INT)
        obj.put("free_memory", getFreeMem())
        obj.put("battery_level", getBatteryLevel())
        return obj
    }

    private fun getFreeMem(): Long {
        val am = applicationContext.getSystemService(Context.ACTIVITY_SERVICE)
                as android.app.ActivityManager
        val info = android.app.ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        return info.availMem / (1024 * 1024)
    }

    private fun getBatteryLevel(): Int {
        val intent = applicationContext.registerReceiver(
            null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 1
        return (100 * level / scale)
    }

    private fun send(json: JSONObject) {
        val url = URL(serverUrl)
        val conn = url.openConnection() as HttpURLConnection

        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true

        BufferedOutputStream(conn.outputStream).use {
            it.write(json.toString().toByteArray())
        }

        val response = BufferedReader(
            InputStreamReader(conn.inputStream)
        ).readText()

        Log.d("SystemInfoWorker", "Server responded: $response")

        conn.disconnect()
    }
}
