package com.ss.arap

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "ARAP"
        private const val PERMISSION_REQUEST_CODE = 200
    }

    private val requiredPermissions = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.INTERNET
    )

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(TAG, "Received broadcast: ${intent.action}")
            when (intent.action) {
                "com.ss.arap.START_RECORDING" -> {
                    val fileName = intent.getStringExtra("fileName") ?: "recording.mp3"
                    startRecording(fileName)
                }
                "com.ss.arap.STOP_RECORDING" -> {
                    stopRecording()
                }
                "com.ss.arap.PLAY_AUDIO" -> {
                    val filePath = intent.getStringExtra("filePath")
                    if (filePath != null) {
                        playAudio(filePath)
                    } else {
                        Log.e(TAG, "File path is missing")
                    }
                }
                "com.ss.arap.UPLOAD_AUDIO" -> {
                    val filePath = intent.getStringExtra("filePath")
                    val serverUrl = intent.getStringExtra("serverUrl") ?: "https://example.com/upload"
                    if (filePath != null) {
                        uploadAudio(filePath, serverUrl)
                    } else {
                        Log.e(TAG, "File path is missing")
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        AudioRecorder.init(applicationContext)

        if (!hasPermissions()) {
            requestPermissions()
        }
        registerBroadcastReceiver()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(broadcastReceiver)
        AudioRecorder.release()
        AudioPlayer.release()
    }

    private fun registerBroadcastReceiver() {
        val intentFilter = IntentFilter().apply {
            addAction("com.ss.arap.START_RECORDING")
            addAction("com.ss.arap.STOP_RECORDING")
            addAction("com.ss.arap.PLAY_AUDIO")
            addAction("com.ss.arap.UPLOAD_AUDIO")
        }
        registerReceiver(broadcastReceiver, intentFilter, RECEIVER_NOT_EXPORTED)
        Log.d(TAG, "Broadcast receiver registered")
    }

    private fun hasPermissions(): Boolean {
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, requiredPermissions, PERMISSION_REQUEST_CODE)
    }

    private fun startRecording(fileName: String) {
        if (!hasPermissions()) {
            Log.e(TAG, "Missing permissions for recording")
            return
        }

        val filePath = "${getExternalFilesDir(null)?.absolutePath}/$fileName"
        AudioRecorder.startRecording(filePath)
        Log.d(TAG, "Started recording to $filePath")
    }

    private fun stopRecording() {
        val filePath = AudioRecorder.stopRecording()
        Log.d(TAG, "Stopped recording, saved to $filePath")
    }

    private fun playAudio(filePath: String) {
        AudioPlayer.play(filePath)
        Log.d(TAG, "Playing audio from $filePath")
    }

    private fun uploadAudio(filePath: String, serverUrl: String) {
        FileUploader.upload(filePath, serverUrl,
            onSuccess = { response ->
                Log.d(TAG, "Upload successful: $response")
            },
            onError = { error ->
                Log.e(TAG, "Upload failed: $error")
            }
        )
        Log.d(TAG, "Started uploading $filePath to $serverUrl")
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Log.d(TAG, "All permissions granted")
            } else {
                Log.e(TAG, "Permissions denied")
            }
        }
    }
}