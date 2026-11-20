package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.data.BoothConfig
import com.example.photo.PhotoFrameCompositor
import com.example.ui.PhotoBoothViewModel
import com.example.ui.PhotoSessionState
import com.example.ui.screens.EmailEntryScreen
import com.example.ui.screens.WelcomeScreen
import com.example.ui.theme.TaounatePhotoBoothTheme
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PhotoBoothTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `email validation accepts valid emails and rejects invalid ones`() {
        assertTrue(PhotoBoothViewModel.isValidEmail("visitor@taounate.ma"))
        assertTrue(PhotoBoothViewModel.isValidEmail("student.forum12@gmail.com"))
        assertFalse(PhotoBoothViewModel.isValidEmail(""))
        assertFalse(PhotoBoothViewModel.isValidEmail("invalid-email"))
        assertFalse(PhotoBoothViewModel.isValidEmail("missing@domain"))
        assertFalse(PhotoBoothViewModel.isValidEmail("@nodomain.com"))
    }

    @Test
    fun `bundled portrait and landscape frames load with transparent center windows`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val config = BoothConfig()

        val portrait = PhotoFrameCompositor.loadActiveFrameBitmap(context, config, isPortrait = true)
        assertNotNull(portrait)
        portrait!!
        val portraitCenterAlpha = (portrait.getPixel(portrait.width / 2, portrait.height / 2) ushr 24) and 0xFF
        assertEquals(0, portraitCenterAlpha)
        val portraitWin = PhotoFrameCompositor.detectTransparentWindow(portrait)
        assertTrue(portraitWin.width() > portrait.width / 2)
        assertTrue(portraitWin.height() > portrait.height / 2)
        portrait.recycle()

        val landscape = PhotoFrameCompositor.loadActiveFrameBitmap(context, config, isPortrait = false)
        assertNotNull(landscape)
        landscape!!
        val landscapeCenterAlpha = (landscape.getPixel(landscape.width / 2, landscape.height / 2) ushr 24) and 0xFF
        assertEquals(0, landscapeCenterAlpha)
        val landscapeWin = PhotoFrameCompositor.detectTransparentWindow(landscape)
        assertTrue(landscapeWin.width() > landscape.width / 2)
        assertTrue(landscapeWin.height() > landscape.height / 2)
        landscape.recycle()
    }

    @Test
    fun `compositor merges captured photo with official frame and wipes session files cleanly`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val rawFile = PhotoFrameCompositor.createRawCaptureFile(context)

        val dummyPhoto = Bitmap.createBitmap(900, 1200, Bitmap.Config.ARGB_8888)
        dummyPhoto.eraseColor(Color.CYAN)
        FileOutputStream(rawFile).use { out ->
            dummyPhoto.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        dummyPhoto.recycle()

        val result = PhotoFrameCompositor.compositeWithOfficialFrame(
            context = context,
            rawPhotoFile = rawFile,
            config = BoothConfig(),
            isFrontCamera = true
        )
        assertTrue(result.isSuccess)
        val outputFile = result.getOrThrow()
        assertTrue(outputFile.exists())
        assertTrue(outputFile.length() > 10_000L)

        val decoded = BitmapFactory.decodeFile(outputFile.absolutePath)
        assertNotNull(decoded)
        assertEquals(1200, decoded.width)
        assertEquals(1600, decoded.height)
        decoded.recycle()

        PhotoFrameCompositor.wipeSessionPhotos(context)
        assertFalse(outputFile.exists())
    }

    @Test
    fun `welcome screen renders Arabic event branding and starts session on CTA click`() {
        var started = false
        composeTestRule.setContent {
            TaounatePhotoBoothTheme {
                WelcomeScreen(
                    config = BoothConfig(),
                    onStartPhotoExperience = { started = true },
                    onVerifyOrganizerPin = { true },
                    onOpenOrganizerSettings = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("TAOUNATE PHOTO BOOTH").assertExists()
        composeTestRule.onNodeWithText("ملتقى التوجيه المدرسي والمهني").assertExists()
        composeTestRule.onNodeWithTag("start_experience_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertTrue(started)
    }

    @Test
    fun `email screen disables send button for invalid email and enables for valid email`() {
        composeTestRule.setContent {
            TaounatePhotoBoothTheme {
                EmailEntryScreen(
                    sessionState = PhotoSessionState(
                        compositedPhotoPath = "/tmp/nonexistent.jpg",
                        emailInput = "bad-email"
                    ),
                    onEmailChanged = {},
                    onSendClick = {},
                    onBackToPreview = {},
                    onUserInteraction = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("send_email_button").assertIsNotEnabled()
    }

    @Test
    fun `email screen enables send button when valid email is provided`() {
        composeTestRule.setContent {
            TaounatePhotoBoothTheme {
                EmailEntryScreen(
                    sessionState = PhotoSessionState(
                        compositedPhotoPath = "/tmp/nonexistent.jpg",
                        emailInput = "guest@taounate-forum.ma"
                    ),
                    onEmailChanged = {},
                    onSendClick = {},
                    onBackToPreview = {},
                    onUserInteraction = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("send_email_button").assertIsEnabled()
    }

    @Test
    fun `main activity launches without ClassNotFoundException`() {
        val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java).setup()
        assertNotNull(controller.get())
    }
}
