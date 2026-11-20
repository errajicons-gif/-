package com.example.data

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

private val Context.boothDataStore: DataStore<Preferences> by preferencesDataStore(name = "taounate_booth_prefs")

class BoothPreferencesRepository(private val context: Context) {

    private object Keys {
        val BRAND_HEADER = stringPreferencesKey("brand_header")
        val EVENT_TITLE = stringPreferencesKey("event_title")
        val EDITION_LABEL = stringPreferencesKey("edition_label")
        val MAIN_HEADLINE = stringPreferencesKey("main_headline")
        val WELCOME_PHRASE = stringPreferencesKey("welcome_phrase")
        val FOOTER_CAPTION = stringPreferencesKey("footer_caption")
        val BACKEND_BASE_URL = stringPreferencesKey("backend_base_url")
        val CLIENT_TOKEN = stringPreferencesKey("client_token")
        val INACTIVITY_TIMEOUT = intPreferencesKey("inactivity_timeout_seconds")
        val CUSTOM_LOGO_PATH = stringPreferencesKey("custom_logo_path")
        val CUSTOM_PORTRAIT_FRAME_PATH = stringPreferencesKey("custom_portrait_frame_path")
        val CUSTOM_LANDSCAPE_FRAME_PATH = stringPreferencesKey("custom_landscape_frame_path")
        val ORGANIZER_PIN_HASH = stringPreferencesKey("organizer_pin_hash")
    }

    companion object {
        private const val DEFAULT_ORGANIZER_PIN = "2026"
    }

    val configFlow: Flow<BoothConfig> = context.boothDataStore.data.map { prefs ->
        BoothConfig(
            brandHeader = prefs[Keys.BRAND_HEADER] ?: "TAOUNATE PHOTO BOOTH",
            eventTitle = prefs[Keys.EVENT_TITLE] ?: "ملتقى التوجيه المدرسي والمهني",
            editionLabel = prefs[Keys.EDITION_LABEL] ?: "الدورة الثانية عشرة – تاونات",
            mainHeadline = prefs[Keys.MAIN_HEADLINE] ?: "خلّد مشاركتك في ملتقى التوجيه 12 بتاونات",
            welcomePhrase = prefs[Keys.WELCOME_PHRASE] ?: "لحظتك في الملتقى… ذكرى تبقى.",
            footerCaption = prefs[Keys.FOOTER_CAPTION]
                ?: "التقط صورتك، واحتفظ بذكرى مشاركتك، واستقبلها عبر بريدك الإلكتروني",
            backendBaseUrl = prefs[Keys.BACKEND_BASE_URL] ?: "",
            clientToken = prefs[Keys.CLIENT_TOKEN] ?: "",
            inactivityTimeoutSeconds = prefs[Keys.INACTIVITY_TIMEOUT] ?: 60,
            customLogoPath = prefs[Keys.CUSTOM_LOGO_PATH]?.takeIf { File(it).exists() },
            customPortraitFramePath = prefs[Keys.CUSTOM_PORTRAIT_FRAME_PATH]?.takeIf { File(it).exists() },
            customLandscapeFramePath = prefs[Keys.CUSTOM_LANDSCAPE_FRAME_PATH]?.takeIf { File(it).exists() }
        )
    }

    suspend fun updateTexts(
        eventTitle: String,
        editionLabel: String,
        mainHeadline: String,
        welcomePhrase: String,
        footerCaption: String
    ) {
        context.boothDataStore.edit { prefs ->
            prefs[Keys.EVENT_TITLE] = eventTitle.trim()
            prefs[Keys.EDITION_LABEL] = editionLabel.trim()
            prefs[Keys.MAIN_HEADLINE] = mainHeadline.trim()
            prefs[Keys.WELCOME_PHRASE] = welcomePhrase.trim()
            prefs[Keys.FOOTER_CAPTION] = footerCaption.trim()
        }
    }

    suspend fun updateServerSettings(
        backendBaseUrl: String,
        clientToken: String?
    ) {
        context.boothDataStore.edit { prefs ->
            prefs[Keys.BACKEND_BASE_URL] = backendBaseUrl.trim()
            if (clientToken != null) {
                prefs[Keys.CLIENT_TOKEN] = clientToken.trim()
            }
        }
    }

    suspend fun updateInactivityTimeout(seconds: Int) {
        context.boothDataStore.edit { prefs ->
            prefs[Keys.INACTIVITY_TIMEOUT] = seconds
        }
    }

    suspend fun verifyOrganizerPin(pin: String): Boolean {
        val prefs = context.boothDataStore.data.first()
        val storedHash = prefs[Keys.ORGANIZER_PIN_HASH] ?: sha256(DEFAULT_ORGANIZER_PIN)
        return sha256(pin.trim()) == storedHash
    }

    suspend fun updateOrganizerPin(newPin: String) {
        require(newPin.trim().length >= 4) { "PIN must be at least 4 characters" }
        context.boothDataStore.edit { prefs ->
            prefs[Keys.ORGANIZER_PIN_HASH] = sha256(newPin.trim())
        }
    }

    suspend fun saveCustomLogo(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.filesDir, "custom_assets").apply { mkdirs() }
            val dest = File(dir, "custom_logo_${System.currentTimeMillis()}.png")
            context.contentResolver.openInputStream(uri)?.use { input ->
                val bmp = BitmapFactory.decodeStream(input)
                    ?: error("تعذر قراءة ملف الشعار المختار.")
                dest.outputStream().use { out ->
                    bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                }
                bmp.recycle()
            } ?: error("تعذر فتح ملف الشعار.")

            // Remove older logo files
            dir.listFiles { f -> f.name.startsWith("custom_logo_") && f != dest }
                ?.forEach { it.delete() }

            context.boothDataStore.edit { prefs ->
                prefs[Keys.CUSTOM_LOGO_PATH] = dest.absolutePath
            }
            dest.absolutePath
        }
    }

    suspend fun clearCustomLogo() {
        val current = configFlow.first().customLogoPath
        if (current != null) File(current).delete()
        context.boothDataStore.edit { it.remove(Keys.CUSTOM_LOGO_PATH) }
    }

    suspend fun saveCustomFrame(uri: Uri, portrait: Boolean): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val dir = File(context.filesDir, "custom_assets").apply { mkdirs() }
                val prefix = if (portrait) "custom_frame_portrait_" else "custom_frame_landscape_"
                val dest = File(dir, "${prefix}${System.currentTimeMillis()}.png")

                // Stream raw PNG bytes directly to preserve alpha transparency intact
                context.contentResolver.openInputStream(uri)?.use { input ->
                    dest.outputStream().use { output -> input.copyTo(output) }
                } ?: error("تعذر فتح ملف الإطار.")

                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(dest.absolutePath, options)
                if (options.outWidth <= 0 || options.outHeight <= 0) {
                    dest.delete()
                    error("الملف المختار ليس صورة PNG صالحة.")
                }

                dir.listFiles { f -> f.name.startsWith(prefix) && f != dest }
                    ?.forEach { it.delete() }

                context.boothDataStore.edit { prefs ->
                    val key = if (portrait) Keys.CUSTOM_PORTRAIT_FRAME_PATH else Keys.CUSTOM_LANDSCAPE_FRAME_PATH
                    prefs[key] = dest.absolutePath
                }
                dest.absolutePath
            }
        }

    suspend fun clearCustomFrame(portrait: Boolean) {
        val cfg = configFlow.first()
        val path = if (portrait) cfg.customPortraitFramePath else cfg.customLandscapeFramePath
        if (path != null) File(path).delete()
        context.boothDataStore.edit { prefs ->
            prefs.remove(if (portrait) Keys.CUSTOM_PORTRAIT_FRAME_PATH else Keys.CUSTOM_LANDSCAPE_FRAME_PATH)
        }
    }

    private fun sha256(value: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
