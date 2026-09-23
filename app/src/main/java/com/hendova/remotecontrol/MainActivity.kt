package com.hendova.remotecontrol

import android.app.AlertDialog
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class MainActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    private var requestListener: ListenerRegistration? = null

    private lateinit var mediaProjectionManager: MediaProjectionManager

    private val screenCaptureLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->

            if (result.resultCode == RESULT_OK && result.data != null) {

                val serviceIntent = Intent(
                    this,
                    ScreenCaptureService::class.java
                )

                serviceIntent.putExtra(
                    "resultCode",
                    result.resultCode
                )

                serviceIntent.putExtra(
                    "data",
                    result.data
                )

                ContextCompat.startForegroundService(
                    this,
                    serviceIntent
                )

                Toast.makeText(
                    this,
                    "Berbagi layar dimulai",
                    Toast.LENGTH_SHORT
                ).show()

            } else {

                Toast.makeText(
                    this,
                    "Izin berbagi layar ditolak",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        mediaProjectionManager =
            getSystemService(
                MEDIA_PROJECTION_SERVICE
            ) as MediaProjectionManager

        val deviceIdText =
            findViewById<TextView>(R.id.deviceIdText)

        val connectButton =
            findViewById<Button>(R.id.connectButton)

        val controlButton =
            findViewById<Button>(R.id.controlButton)

        val screenShareButton =
            findViewById<Button>(R.id.screenShareButton)

        val remoteIdInput =
            findViewById<EditText>(R.id.remoteIdInput)

        val statusText =
            findViewById<TextView>(R.id.statusText)

        val androidId = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ANDROID_ID
        )

        val hendovaId =
            "HDV-${androidId.takeLast(6).uppercase()}"

        deviceIdText.text = hendovaId

        // DAFTARKAN PERANGKAT
        connectButton.setOnClickListener {

            val deviceData = hashMapOf(
                "deviceId" to hendovaId,
                "status" to "ONLINE",
                "timestamp" to System.currentTimeMillis()
            )

            statusText.text = "Mendaftarkan perangkat..."

            db.collection("devices")
                .document(hendovaId)
                .set(deviceData)
                .addOnSuccessListener {

                    statusText.text =
                        "Status: Perangkat terdaftar"
                }
                .addOnFailureListener { error ->

                    statusText.text =
                        "Gagal: ${error.message}"
                }
        }

        // MINTA KONEKSI
        controlButton.setOnClickListener {

            val targetId =
                remoteIdInput.text.toString()
                    .trim()
                    .uppercase()

            if (targetId.isEmpty()) {

                remoteIdInput.error =
                    "Masukkan ID perangkat"

                return@setOnClickListener
            }

            val requestData = hashMapOf(
                "requesterId" to hendovaId,
                "targetId" to targetId,
                "status" to "PENDING",
                "timestamp" to System.currentTimeMillis()
            )

            statusText.text =
                "Mengirim permintaan..."

            db.collection("connection_requests")
                .add(requestData)
                .addOnSuccessListener {

                    statusText.text =
                        "Permintaan koneksi dikirim"
                }
                .addOnFailureListener { error ->

                    statusText.text =
                        "Gagal: ${error.message}"
                }
        }

        // BAGIKAN LAYAR
        screenShareButton.setOnClickListener {

            val captureIntent =
                mediaProjectionManager
                    .createScreenCaptureIntent()

            screenCaptureLauncher.launch(
                captureIntent
            )
        }

        // MENERIMA PERMINTAAN KONEKSI
        listenForConnectionRequests(hendovaId)
    }

    private fun listenForConnectionRequests(
        deviceId: String
    ) {

        requestListener =
            db.collection("connection_requests")
                .whereEqualTo(
                    "targetId",
                    deviceId
                )
                .whereEqualTo(
                    "status",
                    "PENDING"
                )
                .addSnapshotListener { snapshots, error ->

                    if (
                        error != null ||
                        snapshots == null
                    ) {
                        return@addSnapshotListener
                    }

                    for (
                        document in snapshots.documents
                    ) {

                        val requesterId =
                            document.getString(
                                "requesterId"
                            )
                                ?: "Tidak diketahui"

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

                db.collection(
                    "connection_requests"
                )
                    .document(requestId)
                    .update(
                        "status",
                        "ACCEPTED"
                    )
            }
            .setNegativeButton("TOLAK") { _, _ ->

                db.collection(
                    "connection_requests"
                )
                    .document(requestId)
                    .update(
                        "status",
                        "REJECTED"
                    )
            }
            .setCancelable(false)
            .show()
    }

    override fun onDestroy() {

        requestListener?.remove()

        super.onDestroy()
    }
}
