package com.ss.arap

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

object FileUploader {
    private const val TAG = "ARAP"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    fun upload(
        filePath: String,
        serverUrl: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val file = File(filePath)
                if (!file.exists()) {
                    onError("File doesn't exist: $filePath")
                    return@launch
                }

                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        "file",
                        file.name,
                        file.asRequestBody("audio/*".toMediaTypeOrNull())
                    )
                    .build()

                val request = Request.Builder()
                    .url(serverUrl)
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string() ?: "No response body"
                        Log.d(TAG, "FileUploader: Upload successful: $responseBody")
                        onSuccess(responseBody)
                    } else {
                        val errorMsg = "FileUploader: Server error: ${response.code}"
                        Log.e(TAG, errorMsg)
                        onError(errorMsg)
                    }
                }
            } catch (e: Exception) {
                val errorMsg = "FileUploader: Upload failed: ${e.message}"
                Log.e(TAG, errorMsg)
                onError(errorMsg)
            }
        }
    }
}