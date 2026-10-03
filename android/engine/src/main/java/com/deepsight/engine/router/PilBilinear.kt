package com.deepsight.engine.router

import com.deepsight.engine.quality.PixelImage
import kotlin.math.ceil

/**
 * Pillow's `Image.resize(size, BILINEAR)` for RGB images, ported so the phone feeds the router the pixels it was
 * trained on (src/libImaging/Resample.c: precompute_coeffs, normalize_coeffs_8bpc, ImagingResampleInner).
 * Downscaling widens the triangle filter by the scale factor (antialiasing). Coefficients are fixed-point with
 * 22 fractional bits; the horizontal pass is rounded to 8 bits before the vertical pass, as Pillow does.
 * Alpha is ignored and written as opaque.
 */
object PilBilinear {
    private const val PRECISION_BITS = 32 - 8 - 2
    private const val HALF = 1 shl (PRECISION_BITS - 1)

    /** Per output pixel: first input index and tap count in [bounds]; fixed-point weights in [weights], [taps] per pixel. */
    private class Coefficients(val taps: Int, val bounds: IntArray, val weights: IntArray)

    fun resize(image: PixelImage, width: Int, height: Int): PixelImage {
        require(width > 0 && height > 0) { "target size must be positive" }
        if (width == image.width && height == image.height) return PixelImage(width, height, image.argb)

        val horizontal = coefficients(image.width, width)
        val vertical = coefficients(image.height, height)
        val verticalBounds = vertical.bounds.copyOf()
        var pixels = image.argb
        var rowWidth = image.width

        if (width != image.width) {
            // Only the rows the vertical pass reads; its bounds then start at the first of them.
            val first = vertical.bounds[0]
            val last = vertical.bounds[height * 2 - 2] + vertical.bounds[height * 2 - 1]
            for (i in 0 until height) verticalBounds[i * 2] -= first
            val rows = last - first
            val out = IntArray(width * rows)
            for (y in 0 until rows) {
                val row = (y + first) * rowWidth
                for (x in 0 until width) {
                    val start = horizontal.bounds[x * 2]
                    val count = horizontal.bounds[x * 2 + 1]
                    val k = x * horizontal.taps
                    var r = HALF; var g = HALF; var b = HALF
                    for (i in 0 until count) {
                        val p = pixels[row + start + i]
                        val w = horizontal.weights[k + i]
                        r += (p shr 16 and 0xff) * w
                        g += (p shr 8 and 0xff) * w
                        b += (p and 0xff) * w
                    }
                    out[y * width + x] = rgb(r, g, b)
                }
            }
            pixels = out
            rowWidth = width
        }

        if (height != image.height) {
            val out = IntArray(rowWidth * height)
            for (y in 0 until height) {
                val start = verticalBounds[y * 2]
                val count = verticalBounds[y * 2 + 1]
                val k = y * vertical.taps
                for (x in 0 until rowWidth) {
                    var r = HALF; var g = HALF; var b = HALF
                    for (i in 0 until count) {
                        val p = pixels[(start + i) * rowWidth + x]
                        val w = vertical.weights[k + i]
                        r += (p shr 16 and 0xff) * w
                        g += (p shr 8 and 0xff) * w
                        b += (p and 0xff) * w
                    }
                    out[y * rowWidth + x] = rgb(r, g, b)
                }
            }
            pixels = out
        }
        return PixelImage(width, height, pixels)
    }

    private fun coefficients(inSize: Int, outSize: Int): Coefficients {
        val scale = inSize.toDouble() / outSize
        val filterScale = maxOf(scale, 1.0)
        val support = 1.0 * filterScale // bilinear support is 1
        val taps = ceil(support).toInt() * 2 + 1
        val bounds = IntArray(outSize * 2)
        val pre = DoubleArray(outSize * taps)
        for (xx in 0 until outSize) {
            val center = (xx + 0.5) * scale
            val ss = 1.0 / filterScale
            // C casts truncate toward zero; toInt() does the same.
            var xmin = (center - support + 0.5).toInt()
            if (xmin < 0) xmin = 0
            var xmax = (center + support + 0.5).toInt()
            if (xmax > inSize) xmax = inSize
            xmax -= xmin
            var sum = 0.0
            for (x in 0 until xmax) {
                val w = triangle(((x + xmin) - center + 0.5) * ss)
                pre[xx * taps + x] = w
                sum += w
            }
            if (sum != 0.0) for (x in 0 until xmax) pre[xx * taps + x] /= sum
            bounds[xx * 2] = xmin
            bounds[xx * 2 + 1] = xmax
        }
        val weights = IntArray(pre.size) { i ->
            val v = pre[i] * (1 shl PRECISION_BITS)
            (if (pre[i] < 0) -0.5 + v else 0.5 + v).toInt()
        }
        return Coefficients(taps, bounds, weights)
    }

    private fun triangle(x: Double): Double {
        val a = if (x < 0.0) -x else x
        return if (a < 1.0) 1.0 - a else 0.0
    }

    private fun clip8(sum: Int): Int = (sum shr PRECISION_BITS).coerceIn(0, 255)

    private fun rgb(r: Int, g: Int, b: Int): Int = (0xff shl 24) or (clip8(r) shl 16) or (clip8(g) shl 8) or clip8(b)
}
