package com.example.purrytify.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import androidx.core.net.toUri

object BitmapUtils {

    suspend fun getBitmapFromString(context: Context, source: String): Bitmap? {
        val data = if (source.startsWith("http")) {
            source // dianggap sebagai URL
        } else {
            source.toUri() // dianggap sebagai URI
        }

        val request = ImageRequest.Builder(context)
            .data(data)
            .allowHardware(false)
            .build()

        val result = context.imageLoader.execute(request)

        return if (result is SuccessResult) {
            (result.drawable as? BitmapDrawable)?.bitmap
        } else null
    }
}
