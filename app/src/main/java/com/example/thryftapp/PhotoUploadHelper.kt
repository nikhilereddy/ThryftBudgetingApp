package com.example.thryftapp

import android.content.Context import android.net.Uri import java.io.File import java.text.SimpleDateFormat import java.util.Date import java.util.Locale

class PhotoUploadHelper(private val context: Context) {

    fun savePhoto(uri: Uri): String? {
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "IMG_$timeStamp.jpg"
            val outputFile = File(context.filesDir, fileName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                outputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            return outputFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

}