package com.sawelo.onfake.data_class

import android.net.Uri
import com.sawelo.onfake.BuildConfig
import com.sawelo.onfake.R

data class ContactData(
    var name: String = ContactDataDefaultValue.nameValue,
    var photoBitmap: Uri = Uri.parse(ContactDataDefaultValue.photoBitmapValue)
)

object ContactDataDefaultValue {
    const val nameValue: String = "Citra"
    const val photoBitmapValue: String =
        ("android.resource://" + BuildConfig.APPLICATION_ID + "/" + R.drawable.default_profile_picture)
}
