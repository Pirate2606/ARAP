package com.ss.arap

import android.media.MediaPlayer
import android.util.Log
import java.io.IOException

object AudioPlayer {
    private const val TAG = "ARAP"
    private var mediaPlayer: MediaPlayer? = null

    fun play(filePath: String) {
        if (mediaPlayer != null) {
            release()
        }

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(filePath)

                setOnCompletionListener {
                    Log.d(TAG, "AudioPlayer: Playback completed")
                    release()
                }

                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "AudioPlayer: Playback error: what=$what, extra=$extra")
                    release()
                    true
                }

                try {
                    prepare()
                    start()
                    Log.d(TAG, "AudioPlayer: Playback started")
                } catch (e: IOException) {
                    Log.e(TAG, "AudioPlayer: prepare() failed: ${e.message}")
                    release()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "AudioPlayer: play error: ${e.message}")
        }
    }

    fun release() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "AudioPlayer: release error: ${e.message}")
        } finally {
            mediaPlayer = null
        }
    }
}