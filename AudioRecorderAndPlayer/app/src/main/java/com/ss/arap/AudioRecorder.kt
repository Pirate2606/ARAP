package com.ss.arap

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.IOException

object AudioRecorder {
    private const val TAG = "ARAP"
    private var mediaRecorder: MediaRecorder? = null
    private var currentFilePath: String? = null
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun startRecording(filePath: String) {
        if (mediaRecorder != null) {
            stopRecording()
        }

        currentFilePath = filePath

        try {
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                appContext?.let { MediaRecorder(it) }
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(filePath)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128000)

                try {
                    prepare()
                    start()
                    Log.d(TAG, "AudioRecorder: Recording started")
                } catch (e: IOException) {
                    Log.e(TAG, "AudioRecorder: prepare() failed: ${e.message}")
                    release()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "AudioRecorder: startRecording error: ${e.message}")
        }
    }

    fun stopRecording(): String? {
        try {
            mediaRecorder?.apply {
                stop()
                release()
                Log.d(TAG, "AudioRecorder: Recording stopped")
            }
        } catch (e: Exception) {
            Log.e(TAG, "AudioRecorder: stopRecording error: ${e.message}")
        } finally {
            mediaRecorder = null
        }

        return currentFilePath
    }

    fun release() {
        try {
            mediaRecorder?.release()
        } catch (e: Exception) {
            Log.e(TAG, "AudioRecorder: release error: ${e.message}")
        } finally {
            mediaRecorder = null
        }
    }
}