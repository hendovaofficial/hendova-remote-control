package com.hendova.remotecontrol

import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val deviceIdText = findViewById<TextView>(R.id.deviceIdText)
        val connectButton = findViewById<Button>(R.id.connectButton)
        val statusText = findViewById<TextView>(R.id.statusText)

        val androidId = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "UNKNOWN"

        val deviceId = "HDV-${androidId.takeLast(8).uppercase()}"
        deviceIdText.text = deviceId

        connectButton.setOnClickListener {
            statusText.text =
                "Status: Menunggu persetujuan perangkat lain.\n" +
                "Fitur koneksi real-time akan ditambahkan pada tahap berikutnya."
        }
    }
}
