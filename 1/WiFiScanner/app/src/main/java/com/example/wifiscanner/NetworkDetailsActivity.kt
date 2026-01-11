package com.example.wifiscanner

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class NetworkDetailsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_network_details)

        val ssid = intent.getStringExtra("ssid") ?: "Неизвестно"
        val bssid = intent.getStringExtra("bssid") ?: "Неизвестно"
        val signal = intent.getIntExtra("signal", 0)
        val frequency = intent.getIntExtra("frequency", 0)
        val capabilities = intent.getStringExtra("capabilities") ?: ""

        findViewById<TextView>(R.id.textDetailSsid).text = "SSID: $ssid"
        findViewById<TextView>(R.id.textDetailBssid).text = "MAC: $bssid"
        findViewById<TextView>(R.id.textDetailSignal).text = "Сигнал: $signal dBm"
        findViewById<TextView>(R.id.textDetailFrequency).text = "Частота: $frequency MHz"

        val encryption = when {
            capabilities.contains("WPA3") -> "WPA3"
            capabilities.contains("WPA2") -> "WPA2"
            capabilities.contains("WPA") -> "WPA"
            capabilities.contains("WEP") -> "WEP"
            capabilities.contains("ESS") -> "OPEN"
            else -> "UNKNOWN"
        }

        findViewById<TextView>(R.id.textDetailEncryption).text = "Шифрование: $encryption"
        findViewById<TextView>(R.id.textDetailCapabilities).text = "Возможности: $capabilities"
    }
}