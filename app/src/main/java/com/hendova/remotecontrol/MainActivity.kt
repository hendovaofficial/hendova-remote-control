package com.hendova.remotecontrol

import android.app.AlertDialog
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class MainActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private var requestListener: ListenerRegistration? = null

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

        val hendovaId =
            "HDV-${androidId.takeLast(6).uppercase()}"

        deviceIdText.text = hendovaId

        connectButton.setOnClickListener {

            val deviceData = hashMapOf(
                "deviceId" to hendovaId,
                "status" to "ONLINE"
            )

            db.collection("devices")
                .document(hendovaId)
                .set(deviceData)
                .addOnSuccessListener {
                    statusText.text = "Status: Perangkat terdaftar"
                }
                .addOnFailureListener {
                    statusText.text = "Gagal mendaftarkan perangkat"
                }
        }

        controlButton.setOnClickListener {

            val targetId = remoteIdInput.text.toString().trim()

            if (targetId.isEmpty()) {
                remoteIdInput.error = "Masukkan ID perangkat"
                return@setOnClickListener
            }

            val requestData = hashMapOf(
                "requesterId" to hendovaId,
                "targetId" to targetId,
                "status" to "PENDING"
            )

            db.collection("connection_requests")
                .add(requestData)
                .addOnSuccessListener {
                    statusText.text = "Permintaan koneksi dikirim"
                }
                .addOnFailureListener {
                    statusText.text = "Gagal mengirim permintaan"
                }
        }

        listenForConnectionRequests(hendovaId)
    }

    private fun listenForConnectionRequests(deviceId: String) {

        requestListener = db.collection("connection_requests")
            .whereEqualTo("targetId", deviceId)
            .whereEqualTo("status", "PENDING")
            .addSnapshotListener { snapshots, error ->

                if (error != null || snapshots == null) {
                    return@addSnapshotListener
                }

                for (document in snapshots.documents) {

                    val requesterId =
                        document.getString("requesterId") ?: "Tidak diketahui"

                    showConnectionDialog(
                        document.id,
                        requesterId
                    )
                }
            }
    }

    private fun showConnectionDialog(
        requestId: String,
        requesterId: String
    ) {

        AlertDialog.Builder(this)
            .setTitle("Permintaan Koneksi")
            .setMessage(
                "Perangkat $requesterId ingin terhubung dengan perangkat Anda."
            )
            .setPositiveButton("TERIMA") { _, _ ->

                db.collection("connection_requests")
                    .document(requestId)
                    .update("status", "ACCEPTED")
            }
            .setNegativeButton("TOLAK") { _, _ ->

                db.collection("connection_requests")
                    .document(requestId)
                    .update("status", "REJECTED")
            }
            .setCancelable(false)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        requestListener?.remove()
    }
}
