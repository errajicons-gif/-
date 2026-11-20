package com.example.ui

import android.app.Application
import android.net.Uri
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BoothConfig
import com.example.data.BoothPreferencesRepository
import com.example.data.EmailDeliveryResult
import com.example.data.EmailRepository
import com.example.photo.PhotoFrameCompositor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class PhotoSessionState(
    val compositedPhotoPath: String? = null,
    val isProcessingPhoto: Boolean = false,
    val processingError: String? = null,
    val emailInput: String = "",
    val emailError: String? = null,
    val isSendingEmail: Boolean = false,
    val sendErrorMessage: String? = null,
    val isOfflineError: Boolean = false,
    val isBackendNotConfigured: Boolean = false,
    val sentMessageId: String? = null,
    val inactivityRemainingSeconds: Int? = null
)

data class OrganizerUiState(
    val isTestingBackend: Boolean = false,
    val testFeedback: String? = null,
    val testSuccess: Boolean? = null,
    val assetFeedback: String? = null
)

class PhotoBoothViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsRepo = BoothPreferencesRepository(application.applicationContext)
    private val emailRepo = EmailRepository(application.applicationContext)

    val config: StateFlow<BoothConfig> = prefsRepo.configFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = BoothConfig()
    )

    private val _sessionState = MutableStateFlow(PhotoSessionState())
    val sessionState: StateFlow<PhotoSessionState> = _sessionState.asStateFlow()

    private val _organizerState = MutableStateFlow(OrganizerUiState())
    val organizerState: StateFlow<OrganizerUiState> = _organizerState.asStateFlow()

    private var inactivityJob: Job? = null

    companion object {
        private val FALLBACK_EMAIL_REGEX =
            Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

        fun isValidEmail(email: String): Boolean {
            val trimmed = email.trim()
            if (trimmed.isEmpty()) return false
            return Patterns.EMAIL_ADDRESS?.matcher(trimmed)?.matches()
                ?: FALLBACK_EMAIL_REGEX.matches(trimmed)
        }
    }

    fun startInactivityMonitor(onTimeout: () -> Unit) {
        inactivityJob?.cancel()
        val timeout = config.value.inactivityTimeoutSeconds
        if (timeout <= 0) {
            _sessionState.update { it.copy(inactivityRemainingSeconds = null) }
            return
        }
        inactivityJob = viewModelScope.launch {
            var remaining = timeout
            while (remaining > 0) {
                _sessionState.update { it.copy(inactivityRemainingSeconds = remaining) }
                delay(1000L)
                remaining--
            }
            clearVisitorSession()
            onTimeout()
        }
    }

    fun touchSessionActivity(onTimeout: () -> Unit) {
        if (inactivityJob?.isActive == true) {
            startInactivityMonitor(onTimeout)
        }
    }

    fun stopInactivityMonitor() {
        inactivityJob?.cancel()
        inactivityJob = null
        _sessionState.update { it.copy(inactivityRemainingSeconds = null) }
    }

    fun processCapturedPhoto(
        rawFile: File,
        isFrontCamera: Boolean,
        onReady: () -> Unit
    ) {
        viewModelScope.launch {
            _sessionState.update {
                it.copy(
                    isProcessingPhoto = true,
                    processingError = null
                )
            }
            val result = PhotoFrameCompositor.compositeWithOfficialFrame(
                context = getApplication(),
                rawPhotoFile = rawFile,
                config = config.value,
                isFrontCamera = isFrontCamera
            )
            result.onSuccess { compositedFile ->
                _sessionState.update {
                    it.copy(
                        compositedPhotoPath = compositedFile.absolutePath,
                        isProcessingPhoto = false,
                        processingError = null
                    )
                }
                onReady()
            }.onFailure { err ->
                rawFile.delete()
                _sessionState.update {
                    it.copy(
                        isProcessingPhoto = false,
                        processingError = err.localizedMessage ?: "تعذر معالجة الصورة مع الإطار."
                    )
                }
            }
        }
    }

    fun discardCurrentPhotoForRetake() {
        val oldPath = _sessionState.value.compositedPhotoPath
        if (oldPath != null) {
            File(oldPath).delete()
        }
        _sessionState.update {
            it.copy(
                compositedPhotoPath = null,
                processingError = null,
                sendErrorMessage = null,
                isOfflineError = false,
                isBackendNotConfigured = false
            )
        }
    }

    fun updateEmailInput(newValue: String) {
        val clean = newValue.replace("\n", "")
        val error = if (clean.isNotBlank() && !isValidEmail(clean)) {
            "يرجى إدخال بريد إلكتروني صحيح."
        } else {
            null
        }
        _sessionState.update {
            it.copy(
                emailInput = clean,
                emailError = error,
                sendErrorMessage = null,
                isOfflineError = false,
                isBackendNotConfigured = false
            )
        }
    }

    fun submitPhotoEmail(onSuccess: () -> Unit) {
        val current = _sessionState.value
        if (current.isSendingEmail) return // Guard against duplicate taps

        val trimmedEmail = current.emailInput.trim()
        if (!isValidEmail(trimmedEmail)) {
            _sessionState.update {
                it.copy(emailError = "يرجى إدخال بريد إلكتروني صحيح قبل الإرسال.")
            }
            return
        }

        val photoPath = current.compositedPhotoPath
        if (photoPath.isNullOrBlank()) {
            _sessionState.update {
                it.copy(sendErrorMessage = "لم يتم العثور على الصورة. يرجى التقاط صورة جديدة.")
            }
            return
        }

        viewModelScope.launch {
            _sessionState.update {
                it.copy(
                    isSendingEmail = true,
                    sendErrorMessage = null,
                    isOfflineError = false,
                    isBackendNotConfigured = false
                )
            }

            when (val res = emailRepo.sendPhotoEmail(config.value, trimmedEmail, File(photoPath))) {
                is EmailDeliveryResult.Success -> {
                    _sessionState.update {
                        it.copy(
                            isSendingEmail = false,
                            sentMessageId = res.messageId,
                            sendErrorMessage = null
                        )
                    }
                    onSuccess()
                }
                is EmailDeliveryResult.Failure -> {
                    _sessionState.update {
                        it.copy(
                            isSendingEmail = false,
                            sendErrorMessage = res.userMessage,
                            isOfflineError = res.isOffline,
                            isBackendNotConfigured = res.isNotConfigured
                        )
                    }
                }
            }
        }
    }

    fun clearVisitorSession() {
        stopInactivityMonitor()
        PhotoFrameCompositor.wipeSessionPhotos(getApplication())
        _sessionState.value = PhotoSessionState()
    }

    // Organizer Settings actions
    suspend fun verifyOrganizerPin(pin: String): Boolean = prefsRepo.verifyOrganizerPin(pin)

    fun saveOrganizerTexts(
        eventTitle: String,
        editionLabel: String,
        mainHeadline: String,
        welcomePhrase: String,
        footerCaption: String
    ) {
        viewModelScope.launch {
            prefsRepo.updateTexts(eventTitle, editionLabel, mainHeadline, welcomePhrase, footerCaption)
            _organizerState.update { it.copy(assetFeedback = "تم حفظ نصوص الملتقى بنجاح.") }
        }
    }

    fun saveServerSettings(backendUrl: String, clientToken: String?) {
        viewModelScope.launch {
            prefsRepo.updateServerSettings(backendUrl, clientToken)
            _organizerState.update { it.copy(assetFeedback = "تم تحديث إعدادات الخادم.") }
        }
    }

    fun saveInactivityTimeout(seconds: Int) {
        viewModelScope.launch {
            prefsRepo.updateInactivityTimeout(seconds)
            _organizerState.update { it.copy(assetFeedback = "تم تحديث مؤقت الخمول التلقائي.") }
        }
    }

    fun updateOrganizerPin(newPin: String) {
        viewModelScope.launch {
            runCatching { prefsRepo.updateOrganizerPin(newPin) }
                .onSuccess {
                    _organizerState.update { it.copy(assetFeedback = "تم تغيير رمز دخول المنظم بنجاح.") }
                }
                .onFailure {
                    _organizerState.update { it.copy(assetFeedback = "يجب أن يتكون رمز الدخول من 4 أحرف/أرقام على الأقل.") }
                }
        }
    }

    fun pickCustomLogo(uri: Uri) {
        viewModelScope.launch {
            prefsRepo.saveCustomLogo(uri)
                .onSuccess {
                    _organizerState.update { it.copy(assetFeedback = "تم تحديث شعار الملتقى بنجاح.") }
                }
                .onFailure { e ->
                    _organizerState.update { it.copy(assetFeedback = e.localizedMessage ?: "تعذر حفظ الشعار.") }
                }
        }
    }

    fun resetCustomLogo() {
        viewModelScope.launch {
            prefsRepo.clearCustomLogo()
            _organizerState.update { it.copy(assetFeedback = "تمت استعادة الشعار الرسمي الافتراضي.") }
        }
    }

    fun pickCustomFrame(uri: Uri, portrait: Boolean) {
        viewModelScope.launch {
            prefsRepo.saveCustomFrame(uri, portrait)
                .onSuccess {
                    val label = if (portrait) "العمودي" else "الأفقي"
                    _organizerState.update { it.copy(assetFeedback = "تم تحديث إطار التصوير $label بنجاح.") }
                }
                .onFailure { e ->
                    _organizerState.update { it.copy(assetFeedback = e.localizedMessage ?: "تعذر تحميل ملف الإطار.") }
                }
        }
    }

    fun resetCustomFrame(portrait: Boolean) {
        viewModelScope.launch {
            prefsRepo.clearCustomFrame(portrait)
            val label = if (portrait) "العمودي" else "الأفقي"
            _organizerState.update { it.copy(assetFeedback = "تمت استعادة الإطار الرسمي $label.") }
        }
    }

    fun testBackendEmail(backendUrl: String, clientToken: String, testEmail: String?) {
        viewModelScope.launch {
            _organizerState.update {
                it.copy(isTestingBackend = true, testFeedback = null, testSuccess = null)
            }
            val cfg = config.value
            val result = emailRepo.testBackendConnection(
                backendUrl = backendUrl,
                clientToken = clientToken,
                testRecipientEmail = testEmail?.takeIf { it.isNotBlank() },
                eventTitle = cfg.eventTitle,
                edition = cfg.editionLabel
            )
            when (result) {
                is EmailDeliveryResult.Success -> {
                    val msg = if (!testEmail.isNullOrBlank()) {
                        "تم إرسال البريد التجريبي بنجاح إلى $testEmail."
                    } else {
                        "الخادم متصل ويعمل بشكل سليم (${result.messageId ?: "HTTPS OK"})."
                    }
                    _organizerState.update {
                        it.copy(isTestingBackend = false, testFeedback = msg, testSuccess = true)
                    }
                }
                is EmailDeliveryResult.Failure -> {
                    _organizerState.update {
                        it.copy(isTestingBackend = false, testFeedback = result.userMessage, testSuccess = false)
                    }
                }
            }
        }
    }

    fun wipeAllTemporaryFilesNow() {
        PhotoFrameCompositor.wipeSessionPhotos(getApplication())
        _organizerState.update { it.copy(assetFeedback = "تم مسح جميع الملفات المؤقتة من ذاكرة الجهاز.") }
    }

    fun clearOrganizerFeedback() {
        _organizerState.update { it.copy(assetFeedback = null, testFeedback = null, testSuccess = null) }
    }
}
