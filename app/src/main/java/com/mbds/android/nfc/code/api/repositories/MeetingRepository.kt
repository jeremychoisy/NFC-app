package com.mbds.android.nfc.code.api.repositories

import com.mbds.android.nfc.code.api.models.MeetingResponse
import com.mbds.android.nfc.code.api.services.MeetingService
import okhttp3.RequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Date

class MeetingRepository {
    private val service: MeetingService
    init {
        val retrofit = Retrofit.Builder().apply {
            baseUrl("https://nfc-server-1.herokuapp.com/api/")
        }
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        service = retrofit.create(MeetingService::class.java)
    }

    suspend fun get(id: String): MeetingResponse = service.list(id)
    suspend fun create(name: RequestBody, site: RequestBody, date: RequestBody): MeetingResponse = service.create(name, site, date)
}