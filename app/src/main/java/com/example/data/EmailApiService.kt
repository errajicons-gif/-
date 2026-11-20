package com.example.data

import com.squareup.moshi.JsonClass
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

@JsonClass(generateAdapter = true)
data class EmailSendResponse(
    val success: Boolean = false,
    val message: String? = null,
    val messageId: String? = null,
    val error: String? = null
)

@JsonClass(generateAdapter = true)
data class TestEmailRequest(
    val email: String,
    val eventTitle: String,
    val edition: String
)

@JsonClass(generateAdapter = true)
data class HealthResponse(
    val ok: Boolean = false,
    val service: String? = null,
    val provider: String? = null
)

interface EmailApiService {
    @Multipart
    @POST("send-photo")
    suspend fun sendPhoto(
        @Header("X-Client-Token") clientToken: String?,
        @Part photo: MultipartBody.Part,
        @Part("email") email: RequestBody,
        @Part("eventTitle") eventTitle: RequestBody,
        @Part("edition") edition: RequestBody
    ): Response<EmailSendResponse>

    @POST("test-email")
    suspend fun sendTestEmail(
        @Header("X-Client-Token") clientToken: String?,
        @retrofit2.http.Body body: TestEmailRequest
    ): Response<EmailSendResponse>

    @GET("health")
    suspend fun checkHealth(): Response<HealthResponse>
}
