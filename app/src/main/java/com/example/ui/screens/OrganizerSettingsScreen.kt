package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FilterFrames
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.BoothConfig
import com.example.ui.OrganizerUiState
import com.example.ui.components.EventLogoBadge
import com.example.ui.components.MoroccanFestivalBackground
import com.example.ui.theme.CoralError
import com.example.ui.theme.CoralErrorContainer
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.IvoryWhite
import com.example.ui.theme.RoyalBlueDeep
import com.example.ui.theme.RoyalBlueMid
import com.example.ui.theme.RoyalBlueSurface
import com.example.ui.theme.SoftSlateText

@Composable
fun OrganizerSettingsScreen(
    config: BoothConfig,
    organizerState: OrganizerUiState,
    onBackToKiosk: () -> Unit,
    onSaveTexts: (String, String, String, String, String) -> Unit,
    onSaveServerSettings: (String, String?) -> Unit,
    onTestBackend: (String, String, String?) -> Unit,
    onPickLogo: (Uri) -> Unit,
    onResetLogo: () -> Unit,
    onPickFrame: (Uri, Boolean) -> Unit,
    onResetFrame: (Boolean) -> Unit,
    onSaveInactivityTimeout: (Int) -> Unit,
    onUpdatePin: (String) -> Unit,
    onTestCamera: () -> Unit,
    onWipeTempFiles: () -> Unit,
    onClearFeedback: () -> Unit
) {
    BackHandler { onBackToKiosk() }

    var eventTitle by remember(config.eventTitle) { mutableStateOf(config.eventTitle) }
    var editionLabel by remember(config.editionLabel) { mutableStateOf(config.editionLabel) }
    var mainHeadline by remember(config.mainHeadline) { mutableStateOf(config.mainHeadline) }
    var welcomePhrase by remember(config.welcomePhrase) { mutableStateOf(config.welcomePhrase) }
    var footerCaption by remember(config.footerCaption) { mutableStateOf(config.footerCaption) }

    var backendUrl by remember(config.backendBaseUrl) { mutableStateOf(config.backendBaseUrl) }
    var newClientToken by remember { mutableStateOf("") }
    var testEmail by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var selectedTimeout by remember(config.inactivityTimeoutSeconds) {
        mutableIntStateOf(config.inactivityTimeoutSeconds)
    }

    val logoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) onPickLogo(uri) }

    val portraitFramePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) onPickFrame(uri, true) }

    val landscapeFramePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) onPickFrame(uri, false) }

    LaunchedEffect(organizerState.assetFeedback) {
        if (organizerState.assetFeedback != null) {
            kotlinx.coroutines.delay(4500L)
            onClearFeedback()
        }
    }

    MoroccanFestivalBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .imePadding()
        ) {
            // Top bar
            Surface(
                color = RoyalBlueSurface,
                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onBackToKiosk,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("organizer_exit_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                                tint = GoldBright
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.organizer_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = GoldBright
                        )
                    }

                    OutlinedButton(
                        onClick = onTestCamera,
                        border = BorderStroke(1.dp, GoldPrimary),
                        modifier = Modifier.testTag("organizer_test_camera_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CameraAlt,
                            contentDescription = null,
                            tint = GoldBright,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("اختبار الكاميرا", color = IvoryWhite)
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (organizerState.assetFeedback != null) {
                    Surface(
                        modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = EmeraldContainer,
                        border = BorderStroke(1.dp, EmeraldSuccess)
                    ) {
                        Text(
                            text = organizerState.assetFeedback,
                            style = MaterialTheme.typography.bodyMedium,
                            color = IvoryWhite,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }

                // 1. Visual Branding: Logo & Replaceable PNG Frames
                SettingsSectionCard(title = "الشعار وإطار التصوير الرسمي (PNG شفاف)") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        EventLogoBadge(customLogoPath = config.customLogoPath, size = 72.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (config.customLogoPath != null) "شعار مخصص مُفعّل" else "الشعار الرسمي الافتراضي",
                                style = MaterialTheme.typography.titleMedium,
                                color = IvoryWhite
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        logoPicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = GoldPrimary,
                                        contentColor = RoyalBlueDeep
                                    )
                                ) {
                                    Icon(Icons.Rounded.Image, null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("تغيير الشعار", fontWeight = FontWeight.Bold)
                                }
                                if (config.customLogoPath != null) {
                                    TextButton(onClick = onResetLogo) {
                                        Text("استعادة الافتراضي", color = SoftSlateText)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "يمكن استبدال إطار التصوير (PNG بخلفية شفافة) دون إعادة بناء التطبيق:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SoftSlateText
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { portraitFramePicker.launch(arrayOf("image/png")) },
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.7f))
                        ) {
                            Icon(Icons.Rounded.FilterFrames, null, tint = GoldBright, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (config.customPortraitFramePath != null) "تغيير الإطار العمودي (مخصص)" else "رفع إطار عمودي PNG",
                                color = IvoryWhite
                            )
                        }
                        if (config.customPortraitFramePath != null) {
                            TextButton(onClick = { onResetFrame(true) }) {
                                Text("افتراضي", color = GoldBright)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { landscapeFramePicker.launch(arrayOf("image/png")) },
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.7f))
                        ) {
                            Icon(Icons.Rounded.FilterFrames, null, tint = GoldBright, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (config.customLandscapeFramePath != null) "تغيير الإطار الأفقي (مخصص)" else "رفع إطار أفقي PNG",
                                color = IvoryWhite
                            )
                        }
                        if (config.customLandscapeFramePath != null) {
                            TextButton(onClick = { onResetFrame(false) }) {
                                Text("افتراضي", color = GoldBright)
                            }
                        }
                    }
                }

                // 2. Event Titles & Welcome Copy
                SettingsSectionCard(title = "عناوين الملتقى والنصوص الترحيبية") {
                    OutlinedTextField(
                        value = eventTitle,
                        onValueChange = { eventTitle = it },
                        label = { Text("عنوان الملتقى") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = editionLabel,
                        onValueChange = { editionLabel = it },
                        label = { Text("الدورة والسنة / المدينة") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = mainHeadline,
                        onValueChange = { mainHeadline = it },
                        label = { Text("العنوان الرئيسي") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = welcomePhrase,
                        onValueChange = { welcomePhrase = it },
                        label = { Text("العبارة الترحيبية") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = footerCaption,
                        onValueChange = { footerCaption = it },
                        label = { Text("عبارة أسفل الشاشة") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = {
                            onSaveTexts(eventTitle, editionLabel, mainHeadline, welcomePhrase, footerCaption)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldPrimary,
                            contentColor = RoyalBlueDeep
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.Save, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("حفظ نصوص الواجهة", fontWeight = FontWeight.Bold)
                    }
                }

                // 3. Backend Email Service Configuration (No secret API keys stored in app!)
                SettingsSectionCard(title = "إعدادات خادم البريد الإلكتروني (HTTPS)") {
                    Text(
                        text = "مفاتيح مزود البريد (Resend / SendGrid) محفوظة في الخادم الآمن فقط ولا تُخزّن داخل التطبيق.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SoftSlateText
                    )
                    Spacer(Modifier.height(10.dp))

                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        OutlinedTextField(
                            value = backendUrl,
                            onValueChange = { backendUrl = it },
                            label = { Text("Backend HTTPS Base URL") },
                            placeholder = { Text("https://your-region-project.cloudfunctions.net/api") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = newClientToken,
                            onValueChange = { newClientToken = it },
                            label = { Text("Client App Token (اختياري - مخفي)") },
                            placeholder = {
                                Text(if (config.clientToken.isNotBlank()) "•••••••• (محفوظ)" else "أدخل الرمز إذا كان الخادم يطلبه")
                            },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = testEmail,
                            onValueChange = { testEmail = it },
                            label = { Text("بريد إلكتروني لاختبار الإرسال (اختياري)") },
                            placeholder = { Text("organizer@example.com") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                onSaveServerSettings(
                                    backendUrl,
                                    newClientToken.takeIf { it.isNotBlank() }
                                )
                                newClientToken = ""
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldPrimary,
                                contentColor = RoyalBlueDeep
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("حفظ الخادم", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                val effectiveToken = newClientToken.ifBlank { config.clientToken }
                                onTestBackend(backendUrl, effectiveToken, testEmail.ifBlank { null })
                            },
                            enabled = !organizerState.isTestingBackend,
                            border = BorderStroke(1.dp, GoldBright),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            if (organizerState.isTestingBackend) {
                                CircularProgressIndicator(
                                    color = GoldBright,
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Rounded.CloudDone, null, tint = GoldBright, modifier = Modifier.size(18.dp))
                            }
                            Spacer(Modifier.width(6.dp))
                            Text("اختبار البريد/الخادم", color = IvoryWhite)
                        }
                    }

                    if (organizerState.testFeedback != null) {
                        Spacer(Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (organizerState.testSuccess == true) EmeraldContainer else CoralErrorContainer,
                            border = BorderStroke(
                                1.dp,
                                if (organizerState.testSuccess == true) EmeraldSuccess else CoralError
                            )
                        ) {
                            Text(
                                text = organizerState.testFeedback,
                                style = MaterialTheme.typography.bodyMedium,
                                color = IvoryWhite,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                // 4. Kiosk Mode, Security PIN & Privacy Wipe
                SettingsSectionCard(title = "وضع التشغيل المستمر والأمان") {
                    Text(
                        text = "العودة التلقائية للشاشة الرئيسية عند الخمول:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SoftSlateText
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(30 to "30 ث", 60 to "60 ث", 90 to "90 ث", 0 to "إيقاف").forEach { (sec, label) ->
                            FilterChip(
                                selected = selectedTimeout == sec,
                                onClick = {
                                    selectedTimeout = sec
                                    onSaveInactivityTimeout(sec)
                                },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GoldPrimary,
                                    selectedLabelColor = RoyalBlueDeep
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newPin,
                            onValueChange = { newPin = it },
                            label = { Text("تغيير رمز المنظم (PIN)") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                onUpdatePin(newPin)
                                newPin = ""
                            },
                            enabled = newPin.length >= 4,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RoyalBlueMid,
                                contentColor = GoldBright
                            ),
                            modifier = Modifier.height(54.dp)
                        ) {
                            Icon(Icons.Rounded.Lock, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("تحديث")
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = onWipeTempFiles,
                        border = BorderStroke(1.dp, CoralError.copy(alpha = 0.7f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.DeleteSweep, null, tint = CoralError, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("مسح جميع الملفات المؤقتة فوراً", color = IvoryWhite)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 720.dp),
        shape = RoundedCornerShape(22.dp),
        color = RoyalBlueSurface.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = GoldBright
            )
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}
