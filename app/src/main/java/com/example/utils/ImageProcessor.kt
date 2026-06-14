package com.example.utils

import android.graphics.*
import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import java.io.ByteArrayOutputStream
import kotlin.math.*

data class PhotoState(
    val brightness: Float = 0f, // -100 to 100
    val contrast: Float = 0f,   // -100 to 100
    val saturation: Float = 0f, // -100 to 100
    val exposure: Float = 0f,   // -100 to 100
    val temperature: Float = 0f,// -100 to 100 (cool to warm)
    val tint: Float = 0f,       // -100 to 100
    val vibrance: Float = 0f,   // -100 to 100
    val vignette: Float = 0f,   // 0 to 100
    val sharpness: Float = 0f,  // 0 to 100
    val rotation: Float = 0f,   // 0 / 90 / 180 / 270
    val isFlippedHorizontally: Boolean = false,
    val isFlippedVertically: Boolean = false,
    val faceBeautifySmooth: Float = 0f, // 0 to 100 (skin smoothing filter)
    val faceTeethWhite: Float = 0f,     // 0 to 100 (teeth whitening)
    val faceEyeBright: Float = 0f,      // 0 to 100 (eye brightening)
    val portraitBlur: Float = 0f,       // 0 to 100 (lens blur)
    val chosenFilter: String = "Normal"
)

object ImageProcessor {

    fun getFilterPreset(filter: String): PhotoState {
        return when (filter) {
            "Cinematic" -> PhotoState(contrast = 20f, saturation = -10f, temperature = -5f, tint = 5f, chosenFilter = "Cinematic")
            "Moody" -> PhotoState(brightness = -15f, contrast = 30f, saturation = -15f, temperature = 15f, vignette = 15f, chosenFilter = "Moody")
            "Vintage" -> PhotoState(contrast = -10f, saturation = -20f, temperature = 25f, tint = -10f, vignette = 10f, chosenFilter = "Vintage")
            "B&W" -> PhotoState(saturation = -100f, contrast = 25f, exposure = 5f, chosenFilter = "B&W")
            "Portrait" -> PhotoState(brightness = 10f, contrast = 5f, temperature = 8f, faceBeautifySmooth = 40f, chosenFilter = "Portrait")
            "Summer" -> PhotoState(brightness = 10f, saturation = 25f, temperature = 20f, vibrance = 15f, chosenFilter = "Summer")
            "Night" -> PhotoState(brightness = -5f, contrast = 15f, temperature = -25f, exposure = -10f, vignette = 25f, chosenFilter = "Night")
            else -> PhotoState(chosenFilter = "Normal")
        }
    }

    /**
     * Rule-Based Auto Enhance engine. Analyzes nominal parameters and balance highlights.
     */
    fun getAutoEnhancedState(src: Bitmap): PhotoState {
        // Run simple heuristic on downsampled pixels to avoid performance issues
        val scaled = Bitmap.createScaledBitmap(src, 50, 50, false)
        var totalLum = 0L
        var rSum = 0L
        var gSum = 0L
        var bSum = 0L
        val size = scaled.width * scaled.height
        val pixels = IntArray(size)
        scaled.getPixels(pixels, 0, scaled.width, 0, 0, scaled.width, scaled.height)

        for (pix in pixels) {
            val r = pix.red
            val g = pix.green
            val b = pix.blue
            rSum += r
            gSum += g
            bSum += b
            totalLum += (0.299 * r + 0.587 * g + 0.114 * b).toLong()
        }
        val avgLum = totalLum / size
        val avgRed = rSum / size
        val avgGreen = gSum / size
        val avgBlue = bSum / size

        // If very dark screen: increase exposure and brightness
        // If very bright: lower exposure slightly and boost saturation/contrast
        val targetExposure = if (avgLum < 80) 30f else if (avgLum > 180) -15f else 10f
        val targetBrightness = if (avgLum < 80) 15f else 0f
        val targetContrast = if (avgLum > 180) 15f else 25f
        val targetSaturation = if (avgLum < 80) 5f else 15f

        // White balance adjustment
        val avgR = avgRed.toFloat()
        val avgG = avgGreen.toFloat()
        val avgB = avgBlue.toFloat()
        val temperatureShift = if (avgR > avgB) -10f else 12f
        val tintShift = if (avgG > avgR) -5f else 5f

        return PhotoState(
            brightness = targetBrightness,
            contrast = targetContrast,
            saturation = targetSaturation,
            exposure = targetExposure,
            temperature = temperatureShift,
            tint = tintShift,
            vibrance = 15f,
            faceBeautifySmooth = 15f
        )
    }

    /**
     * Applies full PhotoState stack on input Bitmap
     */
    fun processBitmap(src: Bitmap, state: PhotoState): Bitmap = runSafeImageProcessing {
        // 1. Geometry adjustments: Rotation & Flip first
        var edited = src
        if (state.rotation != 0f || state.isFlippedHorizontally || state.isFlippedVertically) {
            val matrix = Matrix()
            if (state.rotation != 0f) {
                matrix.postRotate(state.rotation)
            }
            val sx = if (state.isFlippedHorizontally) -1f else 1f
            val sy = if (state.isFlippedVertically) -1f else 1f
            if (sx != 1f || sy != 1f) {
                matrix.postScale(sx, sy)
            }
            edited = Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
        }

        // Prepare working output bitmap
        val width = edited.width
        val height = edited.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint()

        // 2. Color adjustments via hardware ColorMatrix
        val cm = ColorMatrix()

        // Exposure & Brightness combined matrix
        // [ R' ]   [ 1 0 0 0 exp+bright ]   [ R ]
        // [ G' ] = [ 0 1 0 0 exp+bright ] * [ G ]
        // [ B' ]   [ 0 0 1 0 exp+bright ]   [ B ]
        val bShift = state.brightness + state.exposure
        val brightMatrix = ColorMatrix(floatArrayOf(
            1f, 0f, 0f, 0f, bShift,
            0f, 1f, 0f, 0f, bShift,
            0f, 0f, 1f, 0f, bShift,
            0f, 0f, 0f, 1f, 0f
        ))
        cm.postConcat(brightMatrix)

        // Contrast adjustment matrix
        val scale = (state.contrast + 100f) / 100f
        val translate = 128f * (1f - scale)
        val contrastMatrix = ColorMatrix(floatArrayOf(
            scale, 0f, 0f, 0f, translate,
            0f, scale, 0f, 0f, translate,
            0f, 0f, scale, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        ))
        cm.postConcat(contrastMatrix)

        // Saturation & Vibrance combined matrix
        val satScale = (state.saturation + state.vibrance * 0.5f + 100f) / 100f
        val satMatrix = ColorMatrix()
        satMatrix.setSaturation(satScale)
        cm.postConcat(satMatrix)

        // Temperature (cool to warm) and Tint (green to magenta) adjustments
        // Temperature: Warm increases Red, decreases Blue
        // Tint: Magenta increases Red/Blue, decreases Green
        val temp = state.temperature / 100f // -1 to 1
        val tnt = state.tint / 100f          // -1 to 1
        val rAdd = (temp * 30f + tnt * 15f)
        val gAdd = (-tnt * 30f)
        val bAdd = (-temp * 30f + tnt * 15f)
        val tempMatrix = ColorMatrix(floatArrayOf(
            1f, 0f, 0f, 0f, rAdd,
            0f, 1f, 0f, 0f, gAdd,
            0f, 0f, 1f, 0f, bAdd,
            0f, 0f, 0f, 1f, 0f
        ))
        cm.postConcat(tempMatrix)

        // Apply filters (e.g. Vintage tones)
        if (state.chosenFilter != "Normal") {
            applyFilterTint(cm, state.chosenFilter)
        }

        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(edited, 0f, 0f, paint)

        // 3. Vignette Effect overlay (Canvas drawing gradient)
        if (state.vignette > 0f) {
            val vignettePaint = Paint(Paint.ANTI_ALIAS_FLAG)
            val radius = sqrt((width * width + height * height).toDouble()) / 2f
            val vignetteRatio = state.vignette / 100f
            val radialGradient = RadialGradient(
                width / 2f, height / 2f, radius.toFloat(),
                intArrayOf(Color.TRANSPARENT, Color.argb((vignetteRatio * 180).toInt(), 0, 0, 0)),
                floatArrayOf(1.2f - (vignetteRatio * 0.4f), 1.0f),
                Shader.TileMode.CLAMP
            )
            vignettePaint.shader = radialGradient
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), vignettePaint)
        }

        // 4. Portrait Depth Blur / Lens Blur emulated using custom shaders or convolution
        if (state.portraitBlur > 0f) {
            val blurred = applyRadialBlur(output, state.portraitBlur)
            canvas.drawBitmap(blurred, 0f, 0f, null)
        }

        // 5. Face Beautify & Enhancements: Smoothing & Whitening Layer
        if (state.faceBeautifySmooth > 0f || state.faceTeethWhite > 0f || state.faceEyeBright > 0f) {
            val faceBeautified = applyBeautification(output, state.faceBeautifySmooth, state.faceTeethWhite, state.faceEyeBright)
            return faceBeautified
        }

        output
    }

    private fun applyFilterTint(cm: ColorMatrix, chosenFilter: String) {
        when (chosenFilter) {
            "Cinematic" -> {
                // Symmetrical cyan/blue skew in shadow, yellow skew highlights
                val filmMatrix = ColorMatrix(floatArrayOf(
                    1.05f, 0.0f, 0.0f, 0.0f, 5f,
                    0.0f, 1.02f, 0.0f, 0.0f, 2f,
                    0.0f, 0.0f, 0.95f, 0.0f, -5f,
                    0.0f, 0.0f, 0.0f, 1.0f, 0.0f
                ))
                cm.postConcat(filmMatrix)
            }
            "Moody" -> {
                val moodyMatrix = ColorMatrix(floatArrayOf(
                    0.9f, 0.0f, 0.0f, 0.0f, -2f,
                    0.0f, 0.85f, 0.0f, 0.0f, -5f,
                    0.0f, 0.0f, 0.8f, 0.0f, -10f,
                    0.0f, 0.0f, 0.0f, 1.0f, 0.0f
                ))
                cm.postConcat(moodyMatrix)
            }
            "Vintage" -> {
                val vintageMatrix = ColorMatrix(floatArrayOf(
                    0.95f, 0.0f, 0.0f, 0.0f, 15f,
                    0.0f, 0.9f, 0.0f, 0.0f, 10f,
                    0.0f, 0.0f, 0.8f, 0.0f, -5f,
                    0.0f, 0.0f, 0.0f, 1.0f, 0.0f
                ))
                cm.postConcat(vintageMatrix)
            }
            "Summer" -> {
                val summerMatrix = ColorMatrix(floatArrayOf(
                    1.1f, 0.0f, 0.0f, 0.0f, 10f,
                    0.0f, 1.1f, 0.0f, 0.0f, 5f,
                    0.0f, 0.0f, 0.9f, 0.0f, -10f,
                    0.0f, 0.0f, 0.0f, 1.0f, 0.0f
                ))
                cm.postConcat(summerMatrix)
            }
        }
    }

    /**
     * Radial depth blur for portrait backgrounds
     */
    private fun applyRadialBlur(src: Bitmap, power: Float): Bitmap {
        val w = src.width
        val h = src.height
        val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawBitmap(src, 0f, 0f, null)

        // Make a heavily downscaled and then soft-scaled blurred copy (classic high quality box blur)
        val scaleFactor = 4
        val sw = max(8, w / scaleFactor)
        val sh = max(8, h / scaleFactor)
        val small = Bitmap.createScaledBitmap(src, sw, sh, true)
        val blurredSmall = fastBlur(small, (power / 5f).toInt().coerceAtLeast(1))
        val ambientBlur = Bitmap.createScaledBitmap(blurredSmall, w, h, true)

        // Mask drawing to isolate central portrait focus
        val mask = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val maskCanvas = Canvas(mask)
        val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val radius = min(w, h) * 0.45f
        val gradient = RadialGradient(
            w / 2f, h / 2f, radius,
            intArrayOf(Color.BLACK, Color.TRANSPARENT),
            floatArrayOf(0.4f, 1.0f),
            Shader.TileMode.CLAMP
        )
        maskPaint.shader = gradient
        maskCanvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), maskPaint)

        // Combine src + blurred using alpha mask
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawBitmap(ambientBlur, 0f, 0f, null)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        canvas.drawBitmap(mask, 0f, 0f, paint)

        // Draw original clear source on top with standard SRC_OVER where alpha mask is clear
        val finalPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        finalPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OVER)
        canvas.drawBitmap(src, 0f, 0f, finalPaint)

        return output
    }

    /**
     * Fast box-blur approximation of Gaussian blur for high efficiency offline operation
     */
    private fun fastBlur(sentinel: Bitmap, radius: Int): Bitmap {
        val bitmap = sentinel.copy(sentinel.config ?: Bitmap.Config.ARGB_8888, true)
        if (radius < 1) return sentinel
        val w = bitmap.width
        val h = bitmap.height
        val pix = IntArray(w * h)
        bitmap.getPixels(pix, 0, w, 0, 0, w, h)

        val wm = w - 1
        val hm = h - 1
        val wh = w * h
        val div = radius + radius + 1

        val r = IntArray(wh)
        val g = IntArray(wh)
        val b = IntArray(wh)
        var rsum: Int
        var gsum: Int
        var bsum: Int
        var x: Int
        var y: Int
        var i: Int
        var p: Int
        var yp: Int
        var yi: Int
        var yw: Int

        val vmin = IntArray(max(w, h))
        val divsum = (div + 1) shr 1
        val dv = IntArray(256 * divsum * divsum)
        for (idx in 0 until 256 * divsum * divsum) {
            dv[idx] = idx / (divsum * divsum)
        }

        yw = 0
        yi = 0

        val stack = Array(div) { IntArray(3) }
        var stackpointer: Int
        var stackstart: Int
        var sir: IntArray
        var rbs: Int
        val r1 = radius + 1
        var routsum: Int
        var goutsum: Int
        var boutsum: Int
        var rinsum: Int
        var ginsum: Int
        var binsum: Int

        for (yIdx in 0 until h) {
            bsum = 0
            gsum = bsum
            rsum = gsum
            boutsum = rsum
            goutsum = boutsum
            routsum = goutsum
            binsum = routsum
            ginsum = binsum
            rinsum = ginsum
            for (idx in -radius..radius) {
                p = pix[yi + min(wm, max(idx, 0))]
                sir = stack[idx + radius]
                sir[0] = p.red
                sir[1] = p.green
                sir[2] = p.blue
                rbs = r1 - abs(idx)
                rsum += sir[0] * rbs
                gsum += sir[1] * rbs
                bsum += sir[2] * rbs
                if (idx > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
            }
            stackpointer = radius

            for (xIdx in 0 until w) {
                r[yi] = dv[rsum]
                g[yi] = dv[gsum]
                b[yi] = dv[bsum]

                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum

                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]

                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]

                if (yIdx == 0) {
                    vmin[xIdx] = min(xIdx + radius + 1, wm)
                }
                p = pix[yw + vmin[xIdx]]

                sir[0] = p.red
                sir[1] = p.green
                sir[2] = p.blue

                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]

                rsum += rinsum
                gsum += ginsum
                bsum += binsum

                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer % div]

                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]

                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]

                yi++
            }
            yw += w
        }

        for (xIdx in 0 until w) {
            bsum = 0
            gsum = bsum
            rsum = gsum
            boutsum = rsum
            goutsum = boutsum
            routsum = goutsum
            binsum = routsum
            ginsum = binsum
            rinsum = ginsum
            yp = -radius * w
            for (idx in -radius..radius) {
                yi = max(0, yp) + xIdx
                sir = stack[idx + radius]
                sir[0] = r[yi]
                sir[1] = g[yi]
                sir[2] = b[yi]
                rbs = r1 - abs(idx)
                rsum += r[yi] * rbs
                gsum += g[yi] * rbs
                bsum += b[yi] * rbs
                if (idx > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
                yp += w
            }
            yi = xIdx
            stackpointer = radius
            for (yIdx in 0 until h) {
                pix[yi] = Color.rgb(dv[rsum], dv[gsum], dv[bsum])

                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum

                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]

                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]

                if (xIdx == 0) {
                    vmin[yIdx] = min(yIdx + r1, hm) * w
                }
                p = xIdx + vmin[yIdx]

                sir[0] = r[p]
                sir[1] = g[p]
                sir[2] = b[p]

                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]

                rsum += rinsum
                gsum += ginsum
                bsum += binsum

                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer]

                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]

                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]

                yi += w
            }
        }

        bitmap.setPixels(pix, 0, w, 0, 0, w, h)
        return bitmap
    }

    /**
     * Offline Portrait skin beautification / smoothing filter (using bilayer structure & luminance brightening)
     */
    private fun applyBeautification(src: Bitmap, smooth: Float, teeth: Float, eye: Float): Bitmap {
        val w = src.width
        val h = src.height
        val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawBitmap(src, 0f, 0f, null)

        val pixels = IntArray(w * h)
        output.getPixels(pixels, 0, w, 0, 0, w, h)

        val factorSmooth = (smooth / 100f) * 0.45f
        val factorWhite = (teeth / 100f) * 30f
        val factorEye = (eye / 100f) * 35f

        // Let's create an offline skin-detection heuristic:
        // Human skin tone in RGB usually fits: R > 95, G > 40, B > 20, R > G, R > B, |R-G| > 15
        for (i in pixels.indices) {
            val pix = pixels[i]
            val r = pix.red
            val g = pix.green
            val b = pix.blue

            // Check skin tone boundaries
            val isSkin = r > 95 && g > 40 && b > 20 && r > g && r > b && (r - g) > 15

            if (isSkin && factorSmooth > 0f) {
                // Apply subtle blur interpolation to mimic skin smoothing
                // We'll blend with surrounding pixels
                val surroundingIndex = if (i > w + 1) i - w - 1 else i
                val pSurr = pixels[surroundingIndex]
                val nr = (r * (1 - factorSmooth) + pSurr.red * factorSmooth).toInt().coerceIn(0, 255)
                val ng = (g * (1 - factorSmooth) + pSurr.green * factorSmooth).toInt().coerceIn(0, 255)
                val nb = (b * (1 - factorSmooth) + pSurr.blue * factorSmooth).toInt().coerceIn(0, 255)
                pixels[i] = Color.rgb(nr, ng, nb)
            } else if (!isSkin) {
                // Brighten whites of teeth and eye regions subtly
                // Heuristic: near white areas inside non-skin (high intensity, low saturation)
                val maxV = max(r, max(g, b))
                val minV = min(r, min(g, b))
                val isSlightlyDesaturated = (maxV - minV) < 40 && maxV > 160

                if (isSlightlyDesaturated) {
                    if (factorWhite > 0f) {
                        // Increase brightness, reduce yellow tones (teeth whitening)
                        val nr = (r + factorWhite).toInt().coerceAtMost(255)
                        val ng = (g + factorWhite).toInt().coerceAtMost(255)
                        val nb = (b + factorWhite + 5).toInt().coerceAtMost(255) // extra blue neutralizes yellow tags
                        pixels[i] = Color.rgb(nr, ng, nb)
                    }
                } else if (maxV > 180 && factorEye > 0f) {
                    // Brighten highlight reflections (eye brightening boost)
                    val nr = (r + factorEye).toInt().coerceAtMost(255)
                    val ng = (g + factorEye).toInt().coerceAtMost(255)
                    val nb = (b + factorEye).toInt().coerceAtMost(255)
                    pixels[i] = Color.rgb(nr, ng, nb)
                }
            }
        }

        output.setPixels(pixels, 0, w, 0, 0, w, h)
        return output
    }

    /**
     * Helper to export a Bitmap as byte array conforming to PNG/JPG quality format
     */
    fun exportToFormat(src: Bitmap, format: String, quality: String): ByteArray = runSafeExport {
        val stream = ByteArrayOutputStream()
        val compressFormat = when (format.uppercase()) {
            "PNG" -> Bitmap.CompressFormat.PNG
            "WEBP" -> Bitmap.CompressFormat.WEBP
            else -> Bitmap.CompressFormat.JPEG
        }
        val qVal = when (quality.uppercase()) {
            "LOW" -> 35
            "MEDIUM" -> 65
            "HIGH" -> 85
            "ULTRA HD" -> 100
            else -> 85
        }
        src.compress(compressFormat, qVal, stream)
        stream.toByteArray()
    }
}
