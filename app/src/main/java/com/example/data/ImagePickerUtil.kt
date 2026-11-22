package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.core.graphics.scale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

object ImagePickerUtil {
    private const val MAX_DIMENSION = 480
    private const val MAX_DATA_URI_LENGTH = 195_000

    suspend fun encodeUriToDataUri(context: Context, uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }
            val origW = boundsOptions.outWidth
            val origH = boundsOptions.outHeight
            if (origW <= 0 || origH <= 0) {
                return@withContext Result.failure(IllegalArgumentException("Unable to decode image"))
            }

            var sampleSize = 1
            while ((origW / sampleSize) > MAX_DIMENSION * 2 || (origH / sampleSize) > MAX_DIMENSION * 2) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return@withContext Result.failure(IllegalArgumentException("Failed to read image stream"))

            val maxSide = max(decoded.width, decoded.height)
            var currentBitmap = if (maxSide > MAX_DIMENSION) {
                val scale = MAX_DIMENSION.toFloat() / maxSide.toFloat()
                decoded.scale(
                    (decoded.width * scale).roundToInt().coerceAtLeast(1),
                    (decoded.height * scale).roundToInt().coerceAtLeast(1),
                    filter = true
                ).also {
                    if (it !== decoded) decoded.recycle()
                }
            } else {
                decoded
            }

            val out = ByteArrayOutputStream()
            var quality = 78
            currentBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            while (out.size() > 110_000 && quality > 22) {
                out.reset()
                quality -= 12
                currentBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }

            var dataUri = "data:image/jpeg;base64," + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            var resizeAttempts = 0
            while (dataUri.length > MAX_DATA_URI_LENGTH && resizeAttempts < 3) {
                resizeAttempts++
                val nextW = (currentBitmap.width * 0.7f).roundToInt().coerceAtLeast(32)
                val nextH = (currentBitmap.height * 0.7f).roundToInt().coerceAtLeast(32)
                val smaller = currentBitmap.scale(nextW, nextH, filter = true)
                if (smaller !== currentBitmap) currentBitmap.recycle()
                currentBitmap = smaller
                out.reset()
                currentBitmap.compress(Bitmap.CompressFormat.JPEG, 40, out)
                dataUri = "data:image/jpeg;base64," + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            }

            currentBitmap.recycle()
            if (dataUri.length > 199_500) {
                return@withContext Result.failure(IllegalArgumentException("Selected image is too large. Choose a smaller image."))
            }
            Result.success(dataUri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun decodeDataUriToBitmap(dataUri: String): Bitmap? {
        if (!dataUri.startsWith("data:image")) return null
        return try {
            val base64Part = dataUri.substringAfter("base64,", "")
            if (base64Part.isBlank()) return null
            val bytes = Base64.decode(base64Part, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        }
    }
}
