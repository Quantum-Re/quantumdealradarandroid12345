package com.example.util

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Scale
import coil.size.Size
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min

object ImageUtils {
    /**
     * Creates a temporary File in external/internal cache and returns its FileProvider Uri for camera capture.
     */
    fun createCameraImageUri(context: Context): Uri? {
        return try {
            val imageFolder = File(context.filesDir, "property_photos").apply { if (!exists()) mkdirs() }
            val photoFile = File(imageFolder, "photo_${System.currentTimeMillis()}.jpg")
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                photoFile
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Saves a Bitmap (from TakePicturePreview) with downsampling into internal app storage and returns its Uri string.
     * Restricts max dimension to 1280px to prevent OutOfMemory on lower-end devices.
     */
    fun saveBitmapToStorage(context: Context, bitmap: Bitmap, maxDimension: Int = 1280): String? {
        return try {
            val imageFolder = File(context.filesDir, "property_photos").apply { if (!exists()) mkdirs() }
            val photoFile = File(imageFolder, "photo_${System.currentTimeMillis()}.jpg")

            val processedBitmap = downsampleBitmapIfExceeds(bitmap, maxDimension)

            FileOutputStream(photoFile).use { out ->
                processedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }

            if (processedBitmap != bitmap && !processedBitmap.isRecycled) {
                processedBitmap.recycle()
            }

            Uri.fromFile(photoFile).toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Copies an external gallery Uri content to local app internal storage with automatic downsampling
     * to prevent OOM errors and reduce storage footprint.
     */
    fun copyGalleryUriToStorage(context: Context, sourceUri: Uri, maxDimension: Int = 1280): String? {
        return try {
            val imageFolder = File(context.filesDir, "property_photos").apply { if (!exists()) mkdirs() }
            val photoFile = File(imageFolder, "photo_${System.currentTimeMillis()}.jpg")

            // 1. Decode bounds first to compute sample size
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                BitmapFactory.decodeStream(input, null, options)
            }

            // 2. Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, maxDimension, maxDimension)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.RGB_565 // 16-bit to save 50% memory

            // 3. Decode scaled bitmap
            val decodedBitmap = context.contentResolver.openInputStream(sourceUri)?.use { input ->
                BitmapFactory.decodeStream(input, null, options)
            }

            if (decodedBitmap != null) {
                val finalBitmap = downsampleBitmapIfExceeds(decodedBitmap, maxDimension)
                FileOutputStream(photoFile).use { out ->
                    finalBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
                if (finalBitmap != decodedBitmap && !finalBitmap.isRecycled) {
                    finalBitmap.recycle()
                }
                if (!decodedBitmap.isRecycled) {
                    decodedBitmap.recycle()
                }
                Uri.fromFile(photoFile).toString()
            } else {
                // Fallback copy stream directly if decoding fails
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    FileOutputStream(photoFile).use { output ->
                        input.copyTo(output)
                    }
                }
                Uri.fromFile(photoFile).toString()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            sourceUri.toString()
        }
    }

    /**
     * Calculates the sample size for BitmapFactory.Options to bound decoded bitmap.
     */
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return max(1, inSampleSize)
    }

    /**
     * Rescales a bitmap if its dimensions exceed maxDimension.
     */
    private fun downsampleBitmapIfExceeds(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val maxCurrent = max(width, height)

        if (maxCurrent <= maxDimension) return bitmap

        val scale = maxDimension.toFloat() / maxCurrent
        val matrix = Matrix().apply { postScale(scale, scale) }
        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)
    }

    /**
     * Builds a memory-downsampled Coil ImageRequest tailored to the container size,
     * enabling aggressive caching and hardware bitmap rendering.
     */
    fun buildOptimizedImageRequest(
        context: Context,
        data: Any?,
        targetWidthPx: Int = 600,
        targetHeightPx: Int = 400
    ): ImageRequest {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val isLowRam = activityManager?.isLowRamDevice ?: false

        return ImageRequest.Builder(context)
            .data(data)
            .size(targetWidthPx, targetHeightPx)
            .scale(Scale.FILL)
            .precision(Precision.INEXACT)
            .crossfade(true)
            .allowHardware(!isLowRam)
            .allowRgb565(isLowRam)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }
}

