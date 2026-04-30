package com.vula.app.core.network

import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

@JsonClass(generateAdapter = true)
data class RequestCodeBody(val phone: String)

@JsonClass(generateAdapter = true)
data class RequestCodeResponse(val message: String)

@JsonClass(generateAdapter = true)
data class VerifyCodeBody(val phone: String, val code: String)

@JsonClass(generateAdapter = true)
data class VerifyCodeResponse(
    val message: String,
    val token: String
)

interface VulaApiService {

    @POST("/api/auth/request-code")
    suspend fun requestCode(@Body body: RequestCodeBody): Response<RequestCodeResponse>

    @POST("/api/auth/verify-code")
    suspend fun verifyCode(@Body body: VerifyCodeBody): Response<VerifyCodeResponse>
}
