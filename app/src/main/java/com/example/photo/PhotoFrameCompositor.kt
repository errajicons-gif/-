package com.example.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.media.ExifInterface
import com.example.data.BoothConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.max

object PhotoFrameCompositor {

    private const val SESSION_DIR = "session_photos"
    private const val JPEG_QUALITY = 92

    fun getSessionDir(context: Context): File {
        return File(context.cacheDir, SESSION_DIR).apply { mkdirs() }
    }

    fun wipeSessionPhotos(context: Context) {
        val dir = File(context.cacheDir, SESSION_DIR)
        if (dir.exists()) {
            dir.listFiles()?.forEach { it.delete() }
        }
    }

    fun createRawCaptureFile(context: Context): File {
        return File(getSessionDir(context), "raw_${UUID.randomUUID()}.jpg")
    }

    /**
     * Loads the active transparent PNG frame matching orientation.
     * Supports organizer-replaced custom PNG frames without rebuilding the APK.
     */
    fun loadActiveFrameBitmap(
        context: Context,
        config: BoothConfig,
        isPortrait: Boolean
    ): Bitmap? {
        val customPath = if (isPortrait) config.customPortraitFramePath else config.customLandscapeFramePath
        if (!customPath.isNullOrBlank() && File(customPath).exists()) {
            BitmapFactory.decodeFile(customPath)?.let { return it }
        }
        val assetName = if (isPortrait) "frames/frame_portrait.png" else "frames/frame_landscape.png"
        return runCatching {
            context.assets.open(assetName).use { BitmapFactory.decodeStream(it) }
        }.getOrNull()
    }

    /**
     * Composites the raw captured JPEG with the transparent official PNG frame:
     * 1. Reads EXIF orientation and corrects rotation without distorting aspect ratio.
     * 2. Detects the transparent viewport window inside the PNG frame.
     * 3. Center-crops (aspect-fill) the photo cleanly into that window.
     * 4. Draws the transparent PNG frame over the photo (and optional custom logo badge).
     * 5. Saves the result as a high-quality JPEG and deletes the raw temporary capture.
     */
    suspend fun compositeWithOfficialFrame(
        context: Context,
        rawPhotoFile: File,
        config: BoothConfig,
        isFrontCamera: Boolean
    ): Result<File> = withContext(Dispatchers.Default) {
        runCatching {
            val rawBitmap = decodeOrientedBitmap(rawPhotoFile, isFrontCamera)
                ?: error("تعذر قراءة الصورة الملتقطة.")

            val isPortrait = rawBitmap.height >= rawBitmap.width
            val frameBitmap = loadActiveFrameBitmap(context, config, isPortrait)
                ?: error("تعذر تحميل إطار الصورة الرسمي.")

            val outWidth = frameBitmap.width
            val outHeight = frameBitmap.height

            val outputBitmap = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(outputBitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

            // Detect the transparent inner rectangle inside the PNG frame
            val windowRect = detectTransparentWindow(frameBitmap)

            // Scale-to-fill (center crop) inside the transparent window WITHOUT distorting people
            val srcW = rawBitmap.width.toFloat()
            val srcH = rawBitmap.height.toFloat()
            val dstW = windowRect.width().toFloat()
            val dstH = windowRect.height().toFloat()

            val scale = max(dstW / srcW, dstH / srcH)
            val scaledW = srcW * scale
            val scaledH = srcH * scale
            val left = windowRect.left + (dstW - scaledW) / 2f
            val top = windowRect.top + (dstH - scaledH) / 2f

            canvas.save()
            canvas.clipRect(windowRect)
            canvas.drawBitmap(
                rawBitmap,
                null,
                RectF(left, top, left + scaledW, top + scaledH),
                paint
            )
            canvas.restore()

            // Draw the official transparent PNG frame on top
            canvas.drawBitmap(frameBitmap, 0f, 0f, paint)

            // If the organizer uploaded a custom event logo, stamp it cleanly inside the top-right medallion slot
            if (!config.customLogoPath.isNullOrBlank()) {
                val customLogoFile = File(config.customLogoPath)
                if (customLogoFile.exists()) {
                    BitmapFactory.decodeFile(customLogoFile.absolutePath)?.let { customLogo ->
                        drawCustomLogoBadge(canvas, customLogo, outWidth, isPortrait, paint)
                        customLogo.recycle()
                    }
                }
            }

            val outputFile = File(getSessionDir(context), "souvenir_${UUID.randomUUID()}.jpg")
            FileOutputStream(outputFile).use { fos ->
                outputBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, fos)
            }

            rawBitmap.recycle()
            frameBitmap.recycle()
            outputBitmap.recycle()
            rawPhotoFile.delete()

            outputFile
        }
    }

    private fun drawCustomLogoBadge(
        canvas: Canvas,
        logo: Bitmap,
        frameWidth: Int,
        isPortrait: Boolean,
        paint: Paint
    ) {
        val sidePad = if (isPortrait) 56f else 64f
        val topH = if (isPortrait) 210f else 175f
        val logoSize = if (isPortrait) 132f else 116f
        val logoX = frameWidth - sidePad - logoSize - 10f
        val logoY = (topH - logoSize) / 2f

        val path = android.graphics.Path().apply {
            addOval(
                RectF(logoX + 3f, logoY + 3f, logoX + logoSize - 3f, logoY + logoSize - 3f),
                android.graphics.Path.Direction.CW
            )
        }
        canvas.save()
        canvas.clipPath(path)
        canvas.drawBitmap(
            logo,
            null,
            RectF(logoX, logoY, logoX + logoSize, logoY + logoSize),
            paint
        )
        canvas.restore()
    }

    private fun decodeOrientedBitmap(file: File, isFrontCamera: Boolean): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        val maxDim = max(bounds.outWidth, bounds.outHeight)
        var sampleSize = 1
        while (maxDim / sampleSize > 2400) {
            sampleSize *= 2
        }

        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = BitmapFactory.decodeFile(file.absolutePath, decodeOpts) ?: return null

        val exif = runCatching { ExifInterface(file.absolutePath) }.getOrNull()
        val orientation = exif?.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        ) ?: ExifInterface.ORIENTATION_NORMAL

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
        }

        // Note: CameraX ImageCapture saves un-mirrored natural photos by default.
        // Keep parameter for future lens-specific overrides without distorting faces.
        @Suppress("UNUSED_VARIABLE")
        val unusedFrontFlag = isFrontCamera

        if (matrix.isIdentity) return decoded
        val rotated = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        if (rotated != decoded) decoded.recycle()
        return rotated
    }

    /**
     * Scans the center horizontal and vertical axes of the PNG frame to find
     * the exact transparent rectangular window where the photo should appear.
     * Works seamlessly with both the bundled frame and any custom PNG frame uploaded later.
     */
    internal fun detectTransparentWindow(frame: Bitmap): Rect {
        val w = frame.width
        val h = frame.height
        val cx = w / 2
        val cy = h / 2

        val alphaThreshold = 40

        var left = cx
        while (left > 0 && ((frame.getPixel(left - 1, cy) ushr 24) and 0xFF) < alphaThreshold) {
            left--
        }

        var right = cx
        while (right < w - 1 && ((frame.getPixel(right + 1, cy) ushr 24) and 0xFF) < alphaThreshold) {
            right++
        }

        var top = cy
        while (top > 0 && ((frame.getPixel(cx, top - 1) ushr 24) and 0xFF) < alphaThreshold) {
            top--
        }

        var bottom = cy
        while (bottom < h - 1 && ((frame.getPixel(cx, bottom + 1) ushr 24) and 0xFF) < alphaThreshold) {
            bottom++
        }

        // Fallback padding if an uploaded PNG has non-standard transparency
        if (right - left < w / 4 || bottom - top < h / 4) {
            val padX = (w * 0.05f).toInt()
            val padTop = (h * 0.13f).toInt()
            val padBottom = (h * 0.15f).toInt()
            return Rect(padX, padTop, w - padX, h - padBottom)
        }

        // Expand slightly (4px) to tuck photo cleanly behind anti-aliased inner frame border
        return Rect(
            (left - 4).coerceAtLeast(0),
            (top - 4).coerceAtLeast(0),
            (right + 4).coerceAtMost(w),
            (bottom + 4).coerceAtMost(h)
        )
    }
}
