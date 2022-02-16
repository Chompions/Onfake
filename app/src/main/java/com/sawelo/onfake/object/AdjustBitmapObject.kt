package com.sawelo.onfake.`object`

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.sawelo.onfake.BuildConfig
import com.sawelo.onfake.R


object AdjustBitmapObject {
    fun createCompressedBitmapTrial(context: Context, uri: Uri?): Bitmap {
        Log.d("AdjustBitmapObject", "Compressing bitmap")

        val setUri = uri ?: Uri.parse(
            "android.resource://" + BuildConfig.APPLICATION_ID + "/" + R.drawable.default_profile_picture)

        val bitmap = if (Build.VERSION.SDK_INT < 29) {
            MediaStore.Images.Media.getBitmap(context.contentResolver, setUri)
        } else {
            val source: ImageDecoder.Source =
                ImageDecoder.createSource(context.contentResolver, setUri)
            ImageDecoder.decodeBitmap(source)
        }

        Log.d("AdjustBitmapObject", "BEFORE ${bitmap.allocationByteCount}")
        val scaledBitmap = Bitmap.createScaledBitmap(
            bitmap, bitmap.width / 10, bitmap.height / 10, false)

        Log.d("AdjustBitmapObject", "AFTER ${scaledBitmap.allocationByteCount}")

        return scaledBitmap
    }
}