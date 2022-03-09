package com.sawelo.onfake.data_class

import android.net.Uri

data class ContactData(
    var name: String = CallProfileDefaultValue.nameValue,
    var photoBitmap: Uri = Uri.parse(CallProfileDefaultValue.photoUriValue)
)


