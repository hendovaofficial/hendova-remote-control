package com.hendova.remotecontrol

import android.app.AlertDialog
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
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

        val hendovaId = "HDV-${androidId.takeLast(6).uppercase()}"

        deviceIdText.text = hendovaId

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
                    statusText.text = "Gagal: ${error.message}"
                }
        }

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

            val requestData = hashMapOf(
                "controllerId" to hendovaId,
                "targetId" to remoteId,
                "status" to "pending",
                "timestamp" to System.currentTimeMillis()
            )

            statusText.text = "Mengirim permintaan..."

            db.collection("connection_requests")
                .add(requestData)
                .addOnSuccessListener {
                    statusText.text = "Permintaan berhasil dikirim"
                }
                .addOnFailureListener { error ->
                    statusText.text = "Gagal: ${error.message}"
                }
        }

        requestListener = db.collection("connection_requests")
            .whereEqualTo("targetId", hendovaId)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshots, error ->

                if (error != null || snapshots == null) return@addSnapshotListener

                for (document in snapshots.documents) {

                    val controllerId =
                        document.getString("controllerId") ?: continue

                    showConnectionDialog(
                        document.id,
                        controllerId
                    )

                    break
                }
            }
    }

    private fun showConnectionDialog(
        requestId: String,
        controllerId: String
    ) {

        val view = LayoutInflater.from(this)
            .inflate(R.layout.dialog_connection, null)

        val message = view.findViewById<TextView>(
            R.id.requestMessage
        )

        val acceptButton = view.findViewById<Button>(
            R.id.acceptConnectionButton
        )

        val rejectButton = view.findViewById<Button>(
            R.id.rejectConnectionButton
        )

        message.text =
            "Perangkat $controllerId ingin terhubung ke perangkat Anda."

        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .setCancelable(false)
            .create()

        acceptButton.setOnClickListener {

            db.collection("connection_requests")
                .document(requestId)
                .update("status", "accepted")

            dialog.dismiss()
        }

        rejectButton.setOnClickListener {

            db.collection("connection_requests")
                .document(requestId)
                .update("status", "rejected")

            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        requestListener?.remove()
    }
}
