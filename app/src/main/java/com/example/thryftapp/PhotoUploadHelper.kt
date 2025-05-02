package com.example.thryftapp

import android.content.Context
import android.net.Uri
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PhotoUploadHelper(private val context: Context) {

    fun savePhoto(uri: Uri): String? {
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) //generate timestamp
            val fileName = "IMG_$timeStamp.jpg" //create file name
            val outputFile = File(context.filesDir, fileName) //create file path

            context.contentResolver.openInputStream(uri)?.use { input ->
                outputFile.outputStream().use { output ->
                    input.copyTo(output) //copy input stream to output file
                }
            }
            return outputFile.absolutePath //return saved file path
        } catch (e: Exception) {
            e.printStackTrace() //log error
            return null //return null if failed
        }
    }
}
