package com.sawelo.onfake

import androidx.compose.runtime.compositionLocalOf
import com.sawelo.onfake.data_class.ContactData

val LocalContact = compositionLocalOf { ContactData() }