package com.deepsight.engine.router

import com.deepsight.engine.quality.PixelImage

/**
 * The router's input, as router_meta.json's resize_rule and the notebook's eval_tensor define it: shorter side to
 * [shortSide] with Pillow bilinear ([PilBilinear]), the long side floor(long * shortSide / short) but at least
 * [shortSide], a centre crop of size x size at floor offsets, then RGB / 255 as NCHW float32. Mean/std
 * normalisation is inside router.onnx, so none here. EXIF rotation is the caller's (CaseRunner decodes with it).
 */
object RouterInput {
    fun resizedSize(width: Int, height: Int, shortSide: Int): Pair<Int, Int> =
        if (width <= height) shortSide to maxOf(shortSide, (height.toLong() * shortSide).toDouble().div(width).toInt())
        else maxOf(shortSide, (width.toLong() * shortSide).toDouble().div(height).toInt()) to shortSide

    fun tensor(image: PixelImage, shortSide: Int, size: Int): FloatArray {
        require(size in 1..shortSide) { "crop $size must fit the $shortSide-pixel short side" }
        val (w, h) = resizedSize(image.width, image.height, shortSide)
        val resized = PilBilinear.resize(image, w, h)
        val left = (w - size) / 2
        val top = (h - size) / 2
        val plane = size * size
        val out = FloatArray(3 * plane)
        for (y in 0 until size) {
            val row = (top + y) * w + left
            for (x in 0 until size) {
                val p = resized.argb[row + x]
                val i = y * size + x
                out[i] = (p shr 16 and 0xff) / 255f
                out[plane + i] = (p shr 8 and 0xff) / 255f
                out[2 * plane + i] = (p and 0xff) / 255f
            }
        }
        return out
    }
}
