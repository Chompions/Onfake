package com.sawelo.onfake.data_class

import android.net.Uri
import com.sawelo.onfake.BuildConfig
import com.sawelo.onfake.R

data class ContactData(
    val name: String = "Citra",
    val photoBitmap: Uri = Uri.parse(
        "android.resource://" + BuildConfig.APPLICATION_ID + "/" + R.drawable.default_profile_picture)
)
