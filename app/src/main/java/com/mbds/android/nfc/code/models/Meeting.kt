package com.mbds.android.nfc.code.models

import java.util.Date

data class Meeting (
        val name: String,
        val site: String,
        val date: Date,
        val _id: String? = null
)