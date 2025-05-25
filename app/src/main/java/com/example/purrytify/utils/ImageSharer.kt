package com.example.purrytify.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object ImageSharer {

    fun saveBitmapToCache(context: Context, bitmap: Bitmap, fileName: String = "purrytify_qr_song.png"): Uri? {
        val cachePath = File(context.cacheDir, "images")
        cachePath.mkdirs() // Buat direktori jika belum ada
        return try {
            val stream = FileOutputStream("$cachePath/$fileName")
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()
            val imageFile = File(cachePath, fileName)
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imageFile)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    fun shareImageUri(context: Context, imageUri: Uri, title: String = "Share Song QR Code") {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, imageUri)
            type = "image/png"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, title))
    }
}