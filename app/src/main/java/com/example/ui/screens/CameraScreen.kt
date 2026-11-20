package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.FilterFrames
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TimerOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.data.BoothConfig
import com.example.photo.PhotoFrameCompositor
import com.example.ui.components.GoldFestivalButton
import com.example.ui.components.KioskInactivityPill
import com.example.ui.components.MoroccanFestivalBackground
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.IvoryWhite
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RoyalBlueDeep
import com.example.ui.theme.RoyalBlueMid
import com.example.ui.theme.RoyalBlueSurface
import com.example.ui.theme.SoftSlateText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CameraScreen(
    config: BoothConfig,
    isProcessing: Boolean,
    processingError: String?,
    inactivityRemainingSeconds: Int?,
    isOrganizerTestMode: Boolean,
    onUserInteraction: () -> Unit,
    onBack: () -> Unit,
    onCaptureRawFile: (java.io.File, Boolean) -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    MoroccanFestivalBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
        ) {
            if (!hasCameraPermission) {
                CameraPermissionRationaleView(
                    onGrantPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    onOpenSettings = {
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null)
                        )
                        context.startActivity(intent)
                    },
                    onBack = onBack
                )
            } else {
                CameraBoothActiveContent(
                    config = config,
                    isProcessing = isProcessing,
                    processingError = processingError,
                    inactivityRemainingSeconds = inactivityRemainingSeconds,
                    isOrganizerTestMode = isOrganizerTestMode,
                    onUserInteraction = onUserInteraction,
                    onBack = onBack,
                    onCaptureRawFile = onCaptureRawFile
                )
            }
        }
    }
}

@Composable
private fun CameraPermissionRationaleView(
    onGrantPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 500.dp),
            shape = RoundedCornerShape(28.dp),
            color = RoyalBlueSurface,
            border = BorderStroke(1.5.dp, GoldPrimary.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = RoyalBlueMid,
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.CameraAlt,
                            contentDescription = null,
                            tint = GoldBright,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.camera_permission_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = IvoryWhite,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.camera_permission_rationale),
                    style = MaterialTheme.typography.bodyLarge,
                    color = SoftSlateText,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                GoldFestivalButton(
                    text = stringResource(R.string.camera_permission_grant),
                    onClick = onGrantPermission,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("grant_camera_permission_button")
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.6f))
                    ) {
                        Text(stringResource(R.string.camera_permission_settings), color = GoldBright)
                    }
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, SoftSlateText.copy(alpha = 0.4f))
                    ) {
                        Text(stringResource(R.string.back), color = IvoryWhite)
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraBoothActiveContent(
    config: BoothConfig,
    isProcessing: Boolean,
    processingError: String?,
    inactivityRemainingSeconds: Int?,
    isOrganizerTestMode: Boolean,
    onUserInteraction: () -> Unit,
    onBack: () -> Unit,
    onCaptureRawFile: (java.io.File, Boolean) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var useFrontCamera by remember { mutableStateOf(true) }
    var use3sTimer by remember { mutableStateOf(true) }
    var showFrameOverlay by remember { mutableStateOf(true) }
    var countdownValue by remember { mutableIntStateOf(0) }
    var showFlash by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .build()
    }

    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    DisposableEffect(lifecycleOwner, useFrontCamera) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val executor = ContextCompat.getMainExecutor(context)

        val listener = Runnable {
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preferredSelector = if (useFrontCamera) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }

                val selector = when {
                    cameraProvider.hasCamera(preferredSelector) -> preferredSelector
                    cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> CameraSelector.DEFAULT_BACK_CAMERA
                    else -> CameraSelector.DEFAULT_FRONT_CAMERA
                }

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    selector,
                    preview,
                    imageCapture
                )
                cameraError = null
            } catch (e: Exception) {
                cameraError = "تعذر تشغيل الكاميرا: ${e.localizedMessage ?: "يرجى المحاولة"}"
            }
        }
        cameraProviderFuture.addListener(listener, executor)

        onDispose {
            runCatching { cameraProviderFuture.get().unbindAll() }
        }
    }

    fun triggerCaptureNow() {
        if (isProcessing || countdownValue > 0) return
        onUserInteraction()
        scope.launch {
            if (use3sTimer) {
                for (sec in 3 downTo 1) {
                    countdownValue = sec
                    delay(950L)
                }
                countdownValue = 0
            }
            showFlash = true
            val rawFile = PhotoFrameCompositor.createRawCaptureFile(context)
            val outputOptions = ImageCapture.OutputFileOptions.Builder(rawFile).build()
            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        scope.launch {
                            delay(150L)
                            showFlash = false
                            onCaptureRawFile(rawFile, useFrontCamera)
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        showFlash = false
                        rawFile.delete()
                        cameraError = "تعذر التقاط الصورة: ${exception.localizedMessage ?: "خطأ في الكاميرا"}"
                    }
                }
            )
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideLandscape = maxWidth > maxHeight && maxWidth >= 600.dp

        val overlayBitmap: Bitmap? = remember(config, isWideLandscape) {
            PhotoFrameCompositor.loadActiveFrameBitmap(context, config, isPortrait = !isWideLandscape)
        }

        if (isWideLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Viewfinder takes primary space
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    CameraViewfinderBox(
                        previewView = previewView,
                        overlayBitmap = overlayBitmap,
                        showFrameOverlay = showFrameOverlay,
                        countdownValue = countdownValue,
                        showFlash = showFlash,
                        isPortraitFrame = false,
                        modifier = Modifier.fillMaxHeight()
                    )
                }

                // Ergonomic right/side booth controls for tablet
                Surface(
                    modifier = Modifier
                        .width(240.dp)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(28.dp),
                    color = RoyalBlueSurface.copy(alpha = 0.94f),
                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CameraTopMeta(
                                isOrganizerTestMode = isOrganizerTestMode,
                                inactivityRemainingSeconds = inactivityRemainingSeconds,
                                onBack = onBack
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ShutterButton(
                                enabled = !isProcessing && countdownValue == 0 && cameraError == null,
                                onClick = { triggerCaptureNow() }
                            )
                            Text(
                                text = stringResource(R.string.camera_capture),
                                style = MaterialTheme.typography.labelLarge,
                                color = GoldBright
                            )
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CameraOptionPill(
                                icon = Icons.Rounded.Cameraswitch,
                                label = stringResource(R.string.camera_switch),
                                active = true,
                                onClick = {
                                    onUserInteraction()
                                    useFrontCamera = !useFrontCamera
                                },
                                testTag = "switch_camera_button"
                            )
                            CameraOptionPill(
                                icon = if (use3sTimer) Icons.Rounded.Timer else Icons.Rounded.TimerOff,
                                label = if (use3sTimer) stringResource(R.string.camera_timer_3s) else stringResource(R.string.camera_timer_off),
                                active = use3sTimer,
                                onClick = {
                                    onUserInteraction()
                                    use3sTimer = !use3sTimer
                                },
                                testTag = "toggle_timer_button"
                            )
                            CameraOptionPill(
                                icon = Icons.Rounded.FilterFrames,
                                label = if (showFrameOverlay) stringResource(R.string.camera_frame_on) else stringResource(R.string.camera_frame_off),
                                active = showFrameOverlay,
                                onClick = {
                                    onUserInteraction()
                                    showFrameOverlay = !showFrameOverlay
                                },
                                testTag = "toggle_frame_button"
                            )
                        }
                    }
                }
            }
        } else {
            // Vertical handheld / portrait layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                CameraTopMeta(
                    isOrganizerTestMode = isOrganizerTestMode,
                    inactivityRemainingSeconds = inactivityRemainingSeconds,
                    onBack = onBack
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CameraViewfinderBox(
                        previewView = previewView,
                        overlayBitmap = overlayBitmap,
                        showFrameOverlay = showFrameOverlay,
                        countdownValue = countdownValue,
                        showFlash = showFlash,
                        isPortraitFrame = true,
                        modifier = Modifier.fillMaxHeight()
                    )
                }

                // Bottom camera controls
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 560.dp),
                    shape = RoundedCornerShape(26.dp),
                    color = RoyalBlueSurface.copy(alpha = 0.94f),
                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        CameraIconAction(
                            icon = Icons.Rounded.Cameraswitch,
                            label = "الكاميرا",
                            active = true,
                            onClick = {
                                onUserInteraction()
                                useFrontCamera = !useFrontCamera
                            },
                            testTag = "switch_camera_button"
                        )

                        ShutterButton(
                            enabled = !isProcessing && countdownValue == 0 && cameraError == null,
                            onClick = { triggerCaptureNow() }
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            CameraIconAction(
                                icon = if (use3sTimer) Icons.Rounded.Timer else Icons.Rounded.TimerOff,
                                label = if (use3sTimer) "3 ث" else "فوري",
                                active = use3sTimer,
                                onClick = {
                                    onUserInteraction()
                                    use3sTimer = !use3sTimer
                                },
                                testTag = "toggle_timer_button"
                            )
                            CameraIconAction(
                                icon = Icons.Rounded.FilterFrames,
                                label = "الإطار",
                                active = showFrameOverlay,
                                onClick = {
                                    onUserInteraction()
                                    showFrameOverlay = !showFrameOverlay
                                },
                                testTag = "toggle_frame_button"
                            )
                        }
                    }
                }
            }
        }

        // Error banner if camera or processing failed
        val visibleError = cameraError ?: processingError
        if (visibleError != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 64.dp, start = 20.dp, end = 20.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Text(
                    text = visibleError,
                    style = MaterialTheme.typography.bodyMedium,
                    color = IvoryWhite,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }

        // Processing overlay while compositing high-res frame
        if (isProcessing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(RoyalBlueDeep.copy(alpha = 0.82f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = RoyalBlueSurface,
                    border = BorderStroke(1.5.dp, GoldPrimary)
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = GoldBright)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.camera_processing),
                            style = MaterialTheme.typography.titleMedium,
                            color = IvoryWhite
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraViewfinderBox(
    previewView: PreviewView,
    overlayBitmap: Bitmap?,
    showFrameOverlay: Boolean,
    countdownValue: Int,
    showFlash: Boolean,
    isPortraitFrame: Boolean,
    modifier: Modifier = Modifier
) {
    val ratio = if (isPortraitFrame) 3f / 4f else 4f / 3f
    Box(
        modifier = modifier
            .aspectRatio(ratio, matchHeightConstraintsFirst = true)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Black)
            .border(2.dp, GoldPrimary, RoundedCornerShape(24.dp)),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        if (showFrameOverlay && overlayBitmap != null) {
            Image(
                bitmap = overlayBitmap.asImageBitmap(),
                contentDescription = "معاينة إطار الملتقى",
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize()
            )
        }

        AnimatedVisibility(
            visible = countdownValue > 0,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(150))
        ) {
            Surface(
                shape = CircleShape,
                color = RoyalBlueDeep.copy(alpha = 0.78f),
                border = BorderStroke(3.dp, GoldBright),
                modifier = Modifier.size(120.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = countdownValue.toString(),
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 58.sp),
                        color = GoldBright
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = showFlash,
            enter = fadeIn(tween(60)),
            exit = fadeOut(tween(180))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PureWhite.copy(alpha = 0.85f))
            )
        }
    }
}

@Composable
private fun CameraTopMeta(
    isOrganizerTestMode: Boolean,
    inactivityRemainingSeconds: Int?,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        FilledIconButton(
            onClick = onBack,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = RoyalBlueSurface,
                contentColor = GoldBright
            ),
            modifier = Modifier
                .size(48.dp)
                .testTag("camera_back_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.back)
            )
        }

        if (isOrganizerTestMode) {
            Surface(
                shape = RoundedCornerShape(50),
                color = GoldPrimary,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text(
                    text = "وضع اختبار الكاميرا",
                    style = MaterialTheme.typography.labelMedium,
                    color = RoyalBlueDeep,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        } else {
            KioskInactivityPill(remainingSeconds = inactivityRemainingSeconds)
        }
    }
}

@Composable
private fun ShutterButton(
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = GoldBright,
            contentColor = RoyalBlueDeep
        ),
        border = BorderStroke(4.dp, IvoryWhite),
        modifier = Modifier
            .size(76.dp)
            .testTag("capture_shutter_button")
    ) {
        Icon(
            imageVector = Icons.Rounded.CameraAlt,
            contentDescription = stringResource(R.string.camera_capture),
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
private fun CameraIconAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledIconButton(
            onClick = onClick,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if (active) RoyalBlueMid else RoyalBlueDeep,
                contentColor = if (active) GoldBright else SoftSlateText
            ),
            modifier = Modifier
                .size(48.dp)
                .testTag(testTag)
        ) {
            Icon(imageVector = icon, contentDescription = label)
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (active) GoldBright else SoftSlateText
        )
    }
}

@Composable
private fun CameraOptionPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            1.dp,
            if (active) GoldPrimary else SoftSlateText.copy(alpha = 0.35f)
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (active) RoyalBlueMid else RoyalBlueDeep
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (active) GoldBright else SoftSlateText,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (active) IvoryWhite else SoftSlateText
        )
    }
}
