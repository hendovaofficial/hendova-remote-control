package com.hendova.remotecontrol

import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
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
        val controlButton = findViewById<Button>(R.id.controlButton)
        val remoteIdInput = findViewById<EditText>(R.id.remoteIdInput)
        val statusText = findViewById<TextView>(R.id.statusText)

        val androidId = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ANDROID_ID
        )

        val hendovaId = "HDV-${androidId.takeLast(6).uppercase()}"

        deviceIdText.text = hendovaId

        // Mendaftarkan perangkat ke Firebase
        connectButton.setOnClickListener {

            statusText.text = "Mendaftarkan perangkat..."

            val deviceData = hashMapOf(
                "deviceId" to hendovaId,
                "status" to "online",
                "timestamp" to System.currentTimeMillis()
            )

            db.collection("devices")
                .document(hendovaId)
                .set(deviceData)
                .addOnSuccessListener {
                    statusText.text = "Perangkat berhasil terdaftar"
                }
                .addOnFailureListener { error ->
                    statusText.text =
                        "Gagal: ${error.message}"
                }
        }

        // Mengirim permintaan koneksi
        controlButton.setOnClickListener {

            val remoteId = remoteIdInput.text.toString()
                .trim()
                .uppercase()

            if (remoteId.isEmpty()) {
                statusText.text = "Masukkan ID perangkat target"
                return@setOnClickListener
            }

            if (remoteId == hendovaId) {
                statusText.text = "Tidak bisa menghubungkan perangkat sendiri"
                return@setOnClickListener
            }

            statusText.text = "Mengirim permintaan koneksi..."

            val requestData = hashMapOf(
                "controllerId" to hendovaId,
                "targetId" to remoteId,
                "status" to "pending",
                "timestamp" to System.currentTimeMillis()
            )

            db.collection("connection_requests")
                .add(requestData)
                .addOnSuccessListener {
                    statusText.text =
                        "Permintaan koneksi berhasil dikirim"
                }
                .addOnFailureListener { error ->
                    statusText.text =
                        "Gagal mengirim: ${error.message}"
                }
        }
    }
}
