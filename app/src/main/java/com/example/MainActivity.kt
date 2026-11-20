package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.PhotoBoothViewModel
import com.example.ui.screens.CameraScreen
import com.example.ui.screens.EmailEntryScreen
import com.example.ui.screens.OrganizerSettingsScreen
import com.example.ui.screens.PreviewScreen
import com.example.ui.screens.SuccessScreen
import com.example.ui.screens.WelcomeScreen
import com.example.ui.theme.TaounatePhotoBoothTheme

object BoothRoutes {
    const val WELCOME = "welcome"
    const val CAMERA = "camera"
    const val CAMERA_TEST = "camera_test"
    const val PREVIEW = "preview"
    const val EMAIL = "email"
    const val SUCCESS = "success"
    const val ORGANIZER = "organizer"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Keep screen awake during continuous event photo-booth kiosk operation
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            TaounatePhotoBoothTheme {
                val viewModel: PhotoBoothViewModel = viewModel()
                val config by viewModel.config.collectAsStateWithLifecycle()
                val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
                val organizerState by viewModel.organizerState.collectAsStateWithLifecycle()
                val navController = rememberNavController()

                val resetToWelcome = {
                    viewModel.clearVisitorSession()
                    navController.navigate(BoothRoutes.WELCOME) {
                        popUpTo(BoothRoutes.WELCOME) { inclusive = true }
                        launchSingleTop = true
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = BoothRoutes.WELCOME
                ) {
                    composable(BoothRoutes.WELCOME) {
                        WelcomeScreen(
                            config = config,
                            onStartPhotoExperience = {
                                viewModel.clearVisitorSession()
                                viewModel.startInactivityMonitor(onTimeout = resetToWelcome)
                                navController.navigate(BoothRoutes.CAMERA)
                            },
                            onVerifyOrganizerPin = { pin -> viewModel.verifyOrganizerPin(pin) },
                            onOpenOrganizerSettings = {
                                viewModel.stopInactivityMonitor()
                                navController.navigate(BoothRoutes.ORGANIZER)
                            }
                        )
                    }

                    composable(BoothRoutes.CAMERA) {
                        CameraScreen(
                            config = config,
                            isProcessing = sessionState.isProcessingPhoto,
                            processingError = sessionState.processingError,
                            inactivityRemainingSeconds = sessionState.inactivityRemainingSeconds,
                            isOrganizerTestMode = false,
                            onUserInteraction = { viewModel.touchSessionActivity(resetToWelcome) },
                            onBack = resetToWelcome,
                            onCaptureRawFile = { rawFile, isFront ->
                                viewModel.processCapturedPhoto(rawFile, isFront) {
                                    viewModel.touchSessionActivity(resetToWelcome)
                                    navController.navigate(BoothRoutes.PREVIEW)
                                }
                            }
                        )
                    }

                    composable(BoothRoutes.CAMERA_TEST) {
                        CameraScreen(
                            config = config,
                            isProcessing = sessionState.isProcessingPhoto,
                            processingError = sessionState.processingError,
                            inactivityRemainingSeconds = null,
                            isOrganizerTestMode = true,
                            onUserInteraction = {},
                            onBack = {
                                viewModel.discardCurrentPhotoForRetake()
                                navController.popBackStack()
                            },
                            onCaptureRawFile = { rawFile, isFront ->
                                viewModel.processCapturedPhoto(rawFile, isFront) {
                                    navController.navigate(BoothRoutes.PREVIEW)
                                }
                            }
                        )
                    }

                    composable(BoothRoutes.PREVIEW) {
                        PreviewScreen(
                            compositedPhotoPath = sessionState.compositedPhotoPath,
                            inactivityRemainingSeconds = sessionState.inactivityRemainingSeconds,
                            onRetake = {
                                viewModel.touchSessionActivity(resetToWelcome)
                                viewModel.discardCurrentPhotoForRetake()
                                navController.popBackStack()
                            },
                            onApproveAndProceed = {
                                viewModel.touchSessionActivity(resetToWelcome)
                                navController.navigate(BoothRoutes.EMAIL)
                            }
                        )
                    }

                    composable(BoothRoutes.EMAIL) {
                        EmailEntryScreen(
                            sessionState = sessionState,
                            onEmailChanged = viewModel::updateEmailInput,
                            onSendClick = {
                                viewModel.submitPhotoEmail {
                                    viewModel.stopInactivityMonitor()
                                    navController.navigate(BoothRoutes.SUCCESS)
                                }
                            },
                            onBackToPreview = {
                                viewModel.touchSessionActivity(resetToWelcome)
                                navController.popBackStack()
                            },
                            onUserInteraction = { viewModel.touchSessionActivity(resetToWelcome) }
                        )
                    }

                    composable(BoothRoutes.SUCCESS) {
                        SuccessScreen(onStartNewSession = resetToWelcome)
                    }

                    composable(BoothRoutes.ORGANIZER) {
                        OrganizerSettingsScreen(
                            config = config,
                            organizerState = organizerState,
                            onBackToKiosk = resetToWelcome,
                            onSaveTexts = viewModel::saveOrganizerTexts,
                            onSaveServerSettings = viewModel::saveServerSettings,
                            onTestBackend = viewModel::testBackendEmail,
                            onPickLogo = viewModel::pickCustomLogo,
                            onResetLogo = viewModel::resetCustomLogo,
                            onPickFrame = viewModel::pickCustomFrame,
                            onResetFrame = viewModel::resetCustomFrame,
                            onSaveInactivityTimeout = viewModel::saveInactivityTimeout,
                            onUpdatePin = viewModel::updateOrganizerPin,
                            onTestCamera = { navController.navigate(BoothRoutes.CAMERA_TEST) },
                            onWipeTempFiles = viewModel::wipeAllTemporaryFilesNow,
                            onClearFeedback = viewModel::clearOrganizerFeedback
                        )
                    }
                }
            }
        }
    }
}
