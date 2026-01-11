package com.example.servicelabapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.work.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val startButton: Button = findViewById(R.id.start_button)
        val stopButton: Button = findViewById(R.id.stop_button)
        val statusText: TextView = findViewById(R.id.textView)

        startButton.setOnClickListener {
            startWorker()
            statusText.text = "Running (WorkManager)"
        }

        stopButton.setOnClickListener {
            WorkManager.getInstance(this).cancelAllWorkByTag("SYSTEM_INFO")
            statusText.text = "Stopped"
        }
    }

    private fun startWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val work = PeriodicWorkRequestBuilder<SystemInfoWorker>(
            15, TimeUnit.MINUTES
        )
            .addTag("SYSTEM_INFO")
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
                "sysinfo",
                ExistingPeriodicWorkPolicy.UPDATE,
                work
            )
    }
}
