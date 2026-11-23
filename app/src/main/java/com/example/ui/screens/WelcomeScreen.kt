package com.example.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.FilterFrames
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MailOutline
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.aspectRatio
import com.example.R
import com.example.data.BoothConfig
import com.example.photo.PhotoFrameCompositor
import com.example.ui.components.EventLogoBadge
import com.example.ui.components.GoldDividerOrnament
import com.example.ui.components.GoldFestivalButton
import com.example.ui.components.MoroccanFestivalBackground
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldDeep
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.IvoryWhite
import com.example.ui.theme.RoyalBlueDeep
import com.example.ui.theme.RoyalBlueMid
import com.example.ui.theme.RoyalBlueSurface
import com.example.ui.theme.SoftSlateText
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WelcomeScreen(
    config: BoothConfig,
    onStartPhotoExperience: () -> Unit,
    onVerifyOrganizerPin: suspend (String) -> Boolean,
    onOpenOrganizerSettings: () -> Unit
) {
    var showPinDialog by remember { mutableStateOf(false) }
    var showFrameShowcase by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var secretTapCount by remember { mutableIntStateOf(0) }

    val pulseTransition = rememberInfiniteTransition(label = "cta_pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.025f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cta_scale"
    )

    MoroccanFestivalBackground {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
        ) {
            val isWideTablet = maxWidth >= 720.dp

            if (isWideTablet) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp, vertical = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Showcase visual hero card on the right (in RTL)
                    FestivalHeroCard(
                        config = config,
                        onSecretTrigger = {
                            secretTapCount++
                            if (secretTapCount >= 5) {
                                secretTapCount = 0
                                showPinDialog = true
                            }
                        },
                        onLongPressTrigger = { showPinDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(0.88f)
                    )

                    // Invitation & CTA card on the left (in RTL)
                    WelcomeActionPanel(
                        config = config,
                        pulseScale = pulseScale,
                        onStartPhotoExperience = onStartPhotoExperience,
                        onPreviewOfficialFrame = { showFrameShowcase = true },
                        onOpenAbout = { showAboutDialog = true },
                        modifier = Modifier
                            .weight(1.05f)
                            .verticalScroll(rememberScrollState())
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    FestivalCompactBanner(
                        config = config,
                        onSecretTrigger = {
                            secretTapCount++
                            if (secretTapCount >= 5) {
                                secretTapCount = 0
                                showPinDialog = true
                            }
                        },
                        onLongPressTrigger = { showPinDialog = true }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    WelcomeActionPanel(
                        config = config,
                        pulseScale = pulseScale,
                        onStartPhotoExperience = onStartPhotoExperience,
                        onPreviewOfficialFrame = { showFrameShowcase = true },
                        onOpenAbout = { showAboutDialog = true },
                        modifier = Modifier.widthIn(max = 560.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }

    if (showFrameShowcase) {
        OfficialFrameShowcaseDialog(
            config = config,
            onDismiss = { showFrameShowcase = false },
            onStartPhotoExperience = {
                showFrameShowcase = false
                onStartPhotoExperience()
            }
        )
    }

    if (showAboutDialog) {
        AboutDesignerDialog(
            config = config,
            onDismiss = { showAboutDialog = false }
        )
    }

    if (showPinDialog) {
        OrganizerPinDialog(
            onDismiss = { showPinDialog = false },
            onVerifyPin = onVerifyOrganizerPin,
            onUnlocked = {
                showPinDialog = false
                onOpenOrganizerSettings()
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FestivalHeroCard(
    config: BoothConfig,
    onSecretTrigger: () -> Unit,
    onLongPressTrigger: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(32.dp),
        color = RoyalBlueSurface,
        border = BorderStroke(1.5.dp, GoldPrimary.copy(alpha = 0.6f)),
        shadowElevation = 16.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.img_hero_backdrop),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.62f)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                RoyalBlueDeep.copy(alpha = 0.55f),
                                RoyalBlueDeep.copy(alpha = 0.35f),
                                RoyalBlueDeep.copy(alpha = 0.92f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                EventLogoBadge(
                    customLogoPath = config.customLogoPath,
                    size = 148.dp,
                    modifier = Modifier.combinedClickable(
                        onClick = onSecretTrigger,
                        onLongClick = onLongPressTrigger
                    )
                )

                Spacer(modifier = Modifier.height(22.dp))

                Surface(
                    shape = RoundedCornerShape(50),
                    color = RoyalBlueDeep.copy(alpha = 0.8f),
                    border = BorderStroke(1.dp, GoldBright.copy(alpha = 0.7f))
                ) {
                    Text(
                        text = config.brandHeader,
                        style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.8.sp),
                        color = GoldBright,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = config.eventTitle,
                    style = MaterialTheme.typography.displaySmall,
                    color = IvoryWhite,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = config.editionLabel,
                    style = MaterialTheme.typography.titleLarge,
                    color = GoldBright,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FestivalCompactBanner(
    config: BoothConfig,
    onSecretTrigger: () -> Unit,
    onLongPressTrigger: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 560.dp),
        shape = RoundedCornerShape(28.dp),
        color = RoyalBlueSurface,
        border = BorderStroke(1.5.dp, GoldPrimary.copy(alpha = 0.55f)),
        shadowElevation = 12.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.img_hero_backdrop),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .alpha(0.45f)
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                RoyalBlueDeep.copy(alpha = 0.4f),
                                RoyalBlueDeep.copy(alpha = 0.88f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = RoyalBlueDeep.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.7f))
                ) {
                    Text(
                        text = config.brandHeader,
                        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp),
                        color = GoldBright,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                EventLogoBadge(
                    customLogoPath = config.customLogoPath,
                    size = 112.dp,
                    modifier = Modifier.combinedClickable(
                        onClick = onSecretTrigger,
                        onLongClick = onLongPressTrigger
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = config.eventTitle,
                    style = MaterialTheme.typography.displaySmall.copy(fontSize = 26.sp),
                    color = IvoryWhite,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = config.editionLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = GoldBright,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun WelcomeActionPanel(
    config: BoothConfig,
    pulseScale: Float,
    onStartPhotoExperience: () -> Unit,
    onPreviewOfficialFrame: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = RoyalBlueSurface.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.35f)),
        shadowElevation = 10.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = GoldBright,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ركن التصوير التذكاري الرسمي",
                    style = MaterialTheme.typography.labelLarge,
                    color = GoldBright
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = config.mainHeadline,
                style = MaterialTheme.typography.headlineLarge,
                color = IvoryWhite,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = config.welcomePhrase,
                style = MaterialTheme.typography.displaySmall.copy(fontSize = 24.sp),
                color = GoldBright,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))
            GoldDividerOrnament()
            Spacer(modifier = Modifier.height(22.dp))

            // 3 quick visual steps for guests of all ages
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StepBadge(icon = Icons.Rounded.PhotoCamera, label = "التقط صورتك")
                StepBadge(icon = Icons.Rounded.AutoAwesome, label = "الإطار الرسمي")
                StepBadge(icon = Icons.Rounded.MailOutline, label = "استقبلها ببريدك")
            }

            Spacer(modifier = Modifier.height(26.dp))

            GoldFestivalButton(
                text = stringResource(R.string.cta_start),
                onClick = onStartPhotoExperience,
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(pulseScale)
                    .testTag("start_experience_button"),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.CameraAlt,
                        contentDescription = null,
                        tint = RoyalBlueDeep,
                        modifier = Modifier.size(24.dp)
                    )
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Custom-designed Moroccan Royal Blue & Brushed Gold secondary button
            OutlinedButton(
                onClick = onPreviewOfficialFrame,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    width = 1.5.dp,
                    brush = Brush.horizontalGradient(listOf(GoldDeep, GoldBright, GoldDeep))
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = RoyalBlueMid.copy(alpha = 0.72f),
                    contentColor = GoldBright
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("preview_official_frame_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.FilterFrames,
                    contentDescription = null,
                    tint = GoldBright,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "استعراض الإطار الرسمي للملتقى",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = IvoryWhite
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = config.footerCaption,
                style = MaterialTheme.typography.bodyMedium,
                color = SoftSlateText,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = onOpenAbout,
                modifier = Modifier
                    .height(48.dp)
                    .testTag("about_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    tint = GoldBright,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "À propos • حول التطبيق",
                    style = MaterialTheme.typography.labelLarge,
                    color = GoldBright
                )
            }
        }
    }
}

@Composable
private fun StepBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = RoyalBlueMid,
            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.45f)),
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = GoldBright,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = IvoryWhite,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun OrganizerPinDialog(
    onDismiss: () -> Unit,
    onVerifyPin: suspend (String) -> Boolean,
    onUnlocked: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun tryUnlock() {
        scope.launch {
            if (onVerifyPin(pin)) {
                onUnlocked()
            } else {
                errorMsg = "رمز الدخول غير صحيح."
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = RoyalBlueSurface,
        icon = {
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = null,
                tint = GoldBright,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = stringResource(R.string.organizer_pin_title),
                style = MaterialTheme.typography.titleLarge,
                color = IvoryWhite
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.organizer_pin_prompt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = SoftSlateText
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        pin = it
                        errorMsg = null
                    },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { tryUnlock() }),
                    label = { Text("رمز المنظم (PIN)") },
                    isError = errorMsg != null,
                    supportingText = errorMsg?.let { msg -> { Text(msg) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("organizer_pin_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { tryUnlock() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldPrimary,
                    contentColor = RoyalBlueDeep
                ),
                modifier = Modifier.testTag("organizer_unlock_button")
            ) {
                Text(
                    text = stringResource(R.string.organizer_unlock),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = SoftSlateText)
            }
        }
    )
}

@Composable
private fun OfficialFrameShowcaseDialog(
    config: BoothConfig,
    onDismiss: () -> Unit,
    onStartPhotoExperience: () -> Unit
) {
    val context = LocalContext.current
    var showPortrait by remember { mutableStateOf(true) }
    val frameBitmap = remember(config, showPortrait) {
        PhotoFrameCompositor.loadActiveFrameBitmap(context, config, isPortrait = showPortrait)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = RoyalBlueSurface,
        title = {
            Text(
                text = "معاينة الإطار التذكاري الرسمي",
                style = MaterialTheme.typography.titleLarge,
                color = GoldBright,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    FilterChip(
                        selected = showPortrait,
                        onClick = { showPortrait = true },
                        label = { Text("عمودي") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldPrimary,
                            selectedLabelColor = RoyalBlueDeep
                        )
                    )
                    FilterChip(
                        selected = !showPortrait,
                        onClick = { showPortrait = false },
                        label = { Text("أفقي") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldPrimary,
                            selectedLabelColor = RoyalBlueDeep
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(if (showPortrait) 3f / 4f else 4f / 3f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(RoyalBlueDeep),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_backdrop),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (frameBitmap != null) {
                        Image(
                            bitmap = frameBitmap.asImageBitmap(),
                            contentDescription = "معاينة إطار الملتقى الرسمي",
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onStartPhotoExperience,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldPrimary,
                    contentColor = RoyalBlueDeep
                )
            ) {
                Text("التقط صورتك الآن", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق", color = SoftSlateText)
            }
        }
    )
}

@Composable
private fun AboutDesignerDialog(
    config: BoothConfig,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = RoyalBlueSurface,
        icon = {
            EventLogoBadge(customLogoPath = config.customLogoPath, size = 64.dp)
        },
        title = {
            Text(
                text = "À propos • حول التطبيق",
                style = MaterialTheme.typography.titleLarge,
                color = GoldBright,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = config.brandHeader,
                    style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.4.sp),
                    color = GoldBright,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "${config.eventTitle}\n${config.editionLabel}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = IvoryWhite,
                    textAlign = TextAlign.Center
                )

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = RoyalBlueMid,
                    border = BorderStroke(1.5.dp, GoldPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Text(
                            text = "تصميم وإعداد",
                            style = MaterialTheme.typography.labelMedium,
                            color = GoldBright
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "طارق الراجي",
                            style = MaterialTheme.typography.headlineMedium,
                            color = IvoryWhite,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Tarik Erraji",
                            style = MaterialTheme.typography.labelLarge,
                            color = SoftSlateText,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Text(
                    text = "تطبيق ركن التصوير التذكاري الرسمي لملتقى التوجيه المدرسي والمهني بتاونات، مصمم مع احترام حماية المعطيات الشخصية (القانون 09-08).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SoftSlateText,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldPrimary,
                    contentColor = RoyalBlueDeep
                )
            ) {
                Text("إغلاق", fontWeight = FontWeight.Bold)
            }
        }
    )
}
