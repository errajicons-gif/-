package com.example.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

sealed class EmailDeliveryResult {
    data class Success(val messageId: String?) : EmailDeliveryResult()
    data class Failure(val userMessage: String, val isOffline: Boolean = false, val isNotConfigured: Boolean = false) : EmailDeliveryResult()
}

class EmailRepository(private val context: Context) {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    // No HTTP body logging to strictly protect visitor email addresses & photos from appearing in logs
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    fun isNetworkConnected(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun buildService(rawBaseUrl: String): EmailApiService? {
        val trimmed = rawBaseUrl.trim()
        if (trimmed.isEmpty()) return null
        val normalized = if (trimmed.endsWith("/")) trimmed else "$trimmed/"
        if (!normalized.startsWith("https://") && !normalized.startsWith("http://")) {
            return null
        }
        return runCatching {
            Retrofit.Builder()
                .baseUrl(normalized)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(EmailApiService::class.java)
        }.getOrNull()
    }

    suspend fun sendPhotoEmail(
        config: BoothConfig,
        recipientEmail: String,
        photoFile: File
    ): EmailDeliveryResult = withContext(Dispatchers.IO) {
        if (config.backendBaseUrl.isBlank()) {
            return@withContext EmailDeliveryResult.Failure(
                userMessage = "لم يتم ربط خادم البريد الإلكتروني بعد. يرجى من المنظم إدخال رابط الخادم الآمن (HTTPS) في لوحة إعدادات المنظم.",
                isNotConfigured = true
            )
        }

        if (!isNetworkConnected()) {
            return@withContext EmailDeliveryResult.Failure(
                userMessage = "لا يوجد اتصال بالإنترنت حالياً. يرجى التحقق من الشبكة والمحاولة مرة أخرى.",
                isOffline = true
            )
        }

        if (!photoFile.exists() || photoFile.length() == 0L) {
            return@withContext EmailDeliveryResult.Failure(
                userMessage = "تعذر العثور على ملف الصورة المؤقت. يرجى إعادة التقاط الصورة."
            )
        }

        val service = buildService(config.backendBaseUrl)
            ?: return@withContext EmailDeliveryResult.Failure(
                userMessage = "رابط الخادم غير صالح. يجب أن يبدأ بـ https://",
                isNotConfigured = true
            )

        if (!config.backendBaseUrl.trim().startsWith("https://")) {
            return@withContext EmailDeliveryResult.Failure(
                userMessage = "لأمان بيانات الزوار، يجب أن يستخدم الخادم بروتوكول HTTPS المشفر.",
                isNotConfigured = true
            )
        }

        try {
            val mediaTypeJpeg = "image/jpeg".toMediaType()
            val mediaTypeText = "text/plain; charset=utf-8".toMediaType()
            val fileBody = photoFile.asRequestBody(mediaTypeJpeg)
            val filePart = MultipartBody.Part.createFormData(
                name = "photo",
                filename = "taounate-forum-12-souvenir.jpg",
                body = fileBody
            )

            val response = service.sendPhoto(
                clientToken = config.clientToken.takeIf { it.isNotBlank() },
                photo = filePart,
                email = recipientEmail.trim().toRequestBody(mediaTypeText),
                eventTitle = config.eventTitle.toRequestBody(mediaTypeText),
                edition = config.editionLabel.toRequestBody(mediaTypeText)
            )

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true) {
                    EmailDeliveryResult.Success(body.messageId)
                } else {
                    EmailDeliveryResult.Failure(
                        userMessage = body?.error ?: body?.message ?: "رفض الخادم إتمام الإرسال."
                    )
                }
            } else {
                val errDetail = parseErrorBody(response.errorBody()?.string())
                EmailDeliveryResult.Failure(
                    userMessage = errDetail ?: "تعذر إرسال البريد الإلكتروني (رمز الخادم: ${response.code()})."
                )
            }
        } catch (_: SocketTimeoutException) {
            EmailDeliveryResult.Failure(
                userMessage = "انتهت مهلة الاتصال بالخادم أثناء رفع الصورة. يرجى المحاولة مرة أخرى.",
                isOffline = true
            )
        } catch (_: IOException) {
            EmailDeliveryResult.Failure(
                userMessage = "تعذر الوصول إلى خادم البريد الإلكتروني. تحقق من اتصال الإنترنت.",
                isOffline = true
            )
        } catch (e: Exception) {
            EmailDeliveryResult.Failure(
                userMessage = "حدث خطأ غير متوقع أثناء الإرسال: ${e.localizedMessage ?: "يرجى المحاولة لاحقاً"}"
            )
        }
    }

    suspend fun testBackendConnection(
        backendUrl: String,
        clientToken: String,
        testRecipientEmail: String?,
        eventTitle: String,
        edition: String
    ): EmailDeliveryResult = withContext(Dispatchers.IO) {
        if (!isNetworkConnected()) {
            return@withContext EmailDeliveryResult.Failure(
                userMessage = "الجهاز غير متصل بالإنترنت.",
                isOffline = true
            )
        }
        val trimmed = backendUrl.trim()
        if (!trimmed.startsWith("https://")) {
            return@withContext EmailDeliveryResult.Failure(
                userMessage = "يجب أن يبدأ رابط الخادم بـ https://"
            )
        }
        val service = buildService(trimmed)
            ?: return@withContext EmailDeliveryResult.Failure("رابط الخادم غير صالح.")

        try {
            if (!testRecipientEmail.isNullOrBlank()) {
                val res = service.sendTestEmail(
                    clientToken = clientToken.takeIf { it.isNotBlank() },
                    body = TestEmailRequest(
                        email = testRecipientEmail.trim(),
                        eventTitle = eventTitle,
                        edition = edition
                    )
                )
                if (res.isSuccessful && res.body()?.success == true) {
                    EmailDeliveryResult.Success(res.body()?.messageId)
                } else {
                    val err = parseErrorBody(res.errorBody()?.string())
                    EmailDeliveryResult.Failure(err ?: "فشل إرسال البريد التجريبي (${res.code()}).")
                }
            } else {
                val health = service.checkHealth()
                if (health.isSuccessful && health.body()?.ok == true) {
                    EmailDeliveryResult.Success(health.body()?.provider)
                } else {
                    EmailDeliveryResult.Failure("الخادم استجاب بحالة غير متوقعة (${health.code()}).")
                }
            }
        } catch (e: Exception) {
            EmailDeliveryResult.Failure("تعذر الاتصال بالخادم: ${e.localizedMessage ?: "خطأ في الشبكة"}")
        }
    }

    private fun parseErrorBody(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        return runCatching {
            val adapter = moshi.adapter(EmailSendResponse::class.java)
            val parsed = adapter.fromJson(raw)
            parsed?.error ?: parsed?.message
        }.getOrNull()
    }
}
