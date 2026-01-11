package com.example.wifiscanner

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

// модель данных
data class WifiNetwork(
    val ssid: String,
    val bssid: String,
    val signal: Int,
    val frequency: Int,
    val capabilities: String
)

class MainActivity : AppCompatActivity() {

    private lateinit var wifiListView: ListView
    private lateinit var wifiManager: WifiManager
    private val wifiList = mutableListOf<WifiNetwork>()
    private lateinit var adapter: WifiAdapter

    private val wifiScanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
            if (success) {
                updateWifiList()
            } else {
                Toast.makeText(context, "Ошибка сканирования", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiListView = findViewById(R.id.wifiListView)

        adapter = WifiAdapter(this, wifiList)
        wifiListView.adapter = adapter

        wifiListView.setOnItemClickListener { _, _, position, _ ->
            val network = wifiList[position]
            val intent = Intent(this, NetworkDetailsActivity::class.java).apply {
                putExtra("ssid", network.ssid)
                putExtra("bssid", network.bssid)
                putExtra("signal", network.signal)
                putExtra("frequency", network.frequency)
                putExtra("capabilities", network.capabilities)
            }
            startActivity(intent)
        }

        findViewById<View>(R.id.scanButton).setOnClickListener {
            if (checkPermission()) {
                scanWifi()
            }
        }

        registerReceiver(wifiScanReceiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
    }

    inner class WifiAdapter(
        context: Context,
        private val networks: List<WifiNetwork>
    ) : ArrayAdapter<WifiNetwork>(context, R.layout.item_wifi, networks) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context)
                .inflate(R.layout.item_wifi, parent, false)

            val network = networks[position]

            // ТОЛЬКО основные текстовые поля
            view.findViewById<TextView>(R.id.ssidTextView).text =
                network.ssid.ifEmpty { "Скрытая сеть" }

            view.findViewById<TextView>(R.id.signalTextView).text =
                "Сигнал: ${network.signal} dBm"

            view.findViewById<TextView>(R.id.bssidTextView).text =
                "MAC: ${network.bssid}"

            return view
        }
    }

    private fun updateWifiList() {
        wifiList.clear()

        wifiManager.scanResults.forEach { result ->
            val network = WifiNetwork(
                ssid = result.SSID ?: "Скрытая сеть",
                bssid = result.BSSID ?: "Неизвестно",
                signal = result.level,
                frequency = result.frequency,
                capabilities = result.capabilities
            )
            wifiList.add(network)
        }

        wifiList.sortByDescending { it.signal }
        adapter.notifyDataSetChanged()
        Toast.makeText(this, "Найдено ${wifiList.size} сетей", Toast.LENGTH_SHORT).show()
    }

    private fun checkPermission(): Boolean {
        return if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            true
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                100
            )
            false
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            scanWifi()
        }
    }

    private fun scanWifi() {
        val success = wifiManager.startScan()
        if (success) {
            Toast.makeText(this, "Сканирование...", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Не удалось начать сканирование", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(wifiScanReceiver)
        } catch (e: Exception) {
            // игнорируем
        }
    }
}