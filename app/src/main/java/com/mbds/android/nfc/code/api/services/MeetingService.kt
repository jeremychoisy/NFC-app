package com.mbds.android.nfc.code.api.services

import com.mbds.android.nfc.code.api.models.MeetingResponse
import okhttp3.RequestBody
import retrofit2.http.*

interface MeetingService {
    @GET("meeting/{id}")
    suspend fun list(@Path("id") id: String): MeetingResponse

    @Multipart
    @POST("meeting")
    suspend fun create(@Part("name") name: RequestBody, @Part("site") site: RequestBody, @Part("date") date: RequestBody): MeetingResponse
}