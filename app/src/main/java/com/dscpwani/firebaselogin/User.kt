package com.dscpwani.firebaselogin

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class User(
    var name: String? = "",
    var phone: String? = "",
    var imageUrl: String? = ""
)