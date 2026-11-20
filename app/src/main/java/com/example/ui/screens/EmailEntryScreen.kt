package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.PhotoBoothViewModel
import com.example.ui.PhotoSessionState
import com.example.ui.components.GoldFestivalButton
import com.example.ui.components.KioskInactivityPill
import com.example.ui.components.MoroccanFestivalBackground
import com.example.ui.theme.CoralError
import com.example.ui.theme.CoralErrorContainer
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.IvoryWhite
import com.example.ui.theme.MutedSlateText
import com.example.ui.theme.RoyalBlueDeep
import com.example.ui.theme.RoyalBlueMid
import com.example.ui.theme.RoyalBlueSurface
import com.example.ui.theme.SoftSlateText
import java.io.File

@Composable
fun EmailEntryScreen(
    sessionState: PhotoSessionState,
    onEmailChanged: (String) -> Unit,
    onSendClick: () -> Unit,
    onBackToPreview: () -> Unit,
    onUserInteraction: () -> Unit
) {
    BackHandler(enabled = !sessionState.isSendingEmail) { onBackToPreview() }

    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val emailValid = PhotoBoothViewModel.isValidEmail(sessionState.emailInput)

    fun shareViaAndroidSheet() {
        val path = sessionState.compositedPhotoPath ?: return
        val file = File(path)
        if (!file.exists()) return
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "صورتك التذكارية من ملتقى التوجيه 12 بتاونات")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "مشاركة الصورة التذكارية"))
    }

    MoroccanFestivalBackground {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            val isWide = maxWidth >= 720.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onBackToPreview,
                        enabled = !sessionState.isSendingEmail,
                        modifier = Modifier.testTag("back_to_preview_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = null,
                            tint = GoldBright
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.email_back_preview),
                            color = GoldBright,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    KioskInactivityPill(remainingSeconds = sessionState.inactivityRemainingSeconds)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = if (isWide) 820.dp else 560.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = RoyalBlueSurface.copy(alpha = 0.94f),
                    border = BorderStroke(1.5.dp, GoldPrimary.copy(alpha = 0.5f)),
                    shadowElevation = 12.dp
                ) {
                    if (isWide) {
                        Row(
                            modifier = Modifier.padding(28.dp),
                            horizontalArrangement = Arrangement.spacedBy(28.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ThumbnailFrameBox(
                                photoPath = sessionState.compositedPhotoPath,
                                modifier = Modifier.width(240.dp)
                            )
                            EmailFormColumn(
                                sessionState = sessionState,
                                emailValid = emailValid,
                                onEmailChanged = {
                                    onUserInteraction()
                                    onEmailChanged(it)
                                },
                                onSubmit = {
                                    keyboardController?.hide()
                                    onUserInteraction()
                                    onSendClick()
                                },
                                onShareFallback = { shareViaAndroidSheet() },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier.padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            ThumbnailFrameBox(
                                photoPath = sessionState.compositedPhotoPath,
                                modifier = Modifier.width(150.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            EmailFormColumn(
                                sessionState = sessionState,
                                emailValid = emailValid,
                                onEmailChanged = {
                                    onUserInteraction()
                                    onEmailChanged(it)
                                },
                                onSubmit = {
                                    keyboardController?.hide()
                                    onUserInteraction()
                                    onSendClick()
                                },
                                onShareFallback = { shareViaAndroidSheet() },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThumbnailFrameBox(
    photoPath: String?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = RoyalBlueDeep,
        border = BorderStroke(1.5.dp, GoldPrimary)
    ) {
        if (!photoPath.isNullOrBlank()) {
            AsyncImage(
                model = File(photoPath),
                contentDescription = "معاينة مصغرة لصورتك",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .padding(4.dp)
            )
        }
    }
}

@Composable
private fun EmailFormColumn(
    sessionState: PhotoSessionState,
    emailValid: Boolean,
    onEmailChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onShareFallback: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.email_title),
            style = MaterialTheme.typography.headlineLarge,
            color = GoldBright,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.email_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = IvoryWhite,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Keep email text entry LTR so domain/at-sign order stays natural
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            OutlinedTextField(
                value = sessionState.emailInput,
                onValueChange = onEmailChanged,
                enabled = !sessionState.isSendingEmail,
                singleLine = true,
                label = { Text(stringResource(R.string.email_label)) },
                placeholder = { Text(stringResource(R.string.email_hint), color = MutedSlateText) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Email,
                        contentDescription = null,
                        tint = GoldBright
                    )
                },
                isError = sessionState.emailError != null,
                supportingText = sessionState.emailError?.let { err ->
                    { Text(text = err, color = CoralError) }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = { if (emailValid && !sessionState.isSendingEmail) onSubmit() }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldBright,
                    unfocusedBorderColor = GoldPrimary.copy(alpha = 0.55f),
                    focusedTextColor = IvoryWhite,
                    unfocusedTextColor = IvoryWhite,
                    cursorColor = GoldBright,
                    focusedContainerColor = RoyalBlueDeep.copy(alpha = 0.7f),
                    unfocusedContainerColor = RoyalBlueDeep.copy(alpha = 0.55f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("email_input_field")
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Privacy Notice (Moroccan Law 09-08)
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = RoyalBlueMid.copy(alpha = 0.7f),
            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.25f))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Rounded.Shield,
                    contentDescription = null,
                    tint = GoldBright,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.email_privacy_notice),
                    style = MaterialTheme.typography.bodyMedium,
                    color = SoftSlateText
                )
            }
        }

        if (sessionState.sendErrorMessage != null) {
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CoralErrorContainer,
                border = BorderStroke(1.dp, CoralError)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.WifiOff,
                            contentDescription = null,
                            tint = IvoryWhite,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = sessionState.sendErrorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = IvoryWhite
                        )
                    }
                    if (sessionState.isOfflineError || sessionState.isBackendNotConfigured) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = onShareFallback,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, GoldBright.copy(alpha = 0.7f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = RoyalBlueDeep.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = null,
                                tint = GoldBright,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.email_share_fallback),
                                color = GoldBright,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (sessionState.isSendingEmail) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(18.dp),
                color = RoyalBlueMid,
                border = BorderStroke(1.5.dp, GoldPrimary)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        color = GoldBright,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.email_sending),
                        style = MaterialTheme.typography.titleMedium,
                        color = GoldBright
                    )
                }
            }
        } else {
            GoldFestivalButton(
                text = stringResource(R.string.email_send_button),
                onClick = onSubmit,
                enabled = emailValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("send_email_button"),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = null,
                        tint = RoyalBlueDeep
                    )
                }
            )
        }
    }
}
