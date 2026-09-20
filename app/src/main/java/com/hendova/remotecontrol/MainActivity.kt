package com.hendova.remotecontrol

import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val deviceIdText = findViewById<TextView>(R.id.deviceIdText)
        val connectButton = findViewById<Button>(R.id.connectButton)
        val statusText = findViewById<TextView>(R.id.statusText)

        val deviceId = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ANDROID_ID
        )

        val hendovaId = "HDV-${deviceId.takeLast(6).uppercase()}"

        deviceIdText.text = "ID Perangkat: $hendovaId"

        connectButton.setOnClickListener {

            statusText.text = "Menghubungkan..."

            val deviceData = hashMapOf(
                "deviceId" to hendovaId,
                "status" to "online",
                "timestamp" to System.currentTimeMillis()
            )

            db.collection("devices")
                .document(hendovaId)
                .set(deviceData)
                .addOnSuccessListener {
                    statusText.text = "Perangkat terhubung ke Firebase"
                }
                .addOnFailureListener { error ->
                    statusText.text =
                        "Gagal terhubung: ${error.message}"
                }
        }
    }
}
