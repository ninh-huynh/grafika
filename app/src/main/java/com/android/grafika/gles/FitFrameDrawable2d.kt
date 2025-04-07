package com.android.grafika.gles

import timber.log.Timber
import java.nio.FloatBuffer
import kotlin.math.min

class FitFrameDrawable2d : Drawable2d(Drawable2d.Prefab.FULL_RECTANGLE) {

    private var tweakedTexCoordArray: FloatBuffer? = null
    private var recalculate = true

    private var textureWidth: Int = 0
    private var textureHeight: Int = 0
    private var textureOrientation: Int = 0
    private var surfaceWidth: Int = 0
    private var surfaceHeight: Int = 0


    fun updateInfo(
        textureWidth: Int,
        textureHeight: Int,
        textureOrientation: Int,
        surfaceWidth: Int,
        surfaceHeight: Int,
    ) {
        this.textureWidth = textureWidth
        this.textureHeight = textureHeight
        this.textureOrientation = textureOrientation
        this.surfaceWidth = surfaceWidth
        this.surfaceHeight = surfaceHeight
        Timber.i(
            "textureWidth: %d, textureHeight: %d, textureOrientation: %d, frameWidth: %d, frameHeight: %d",
            textureWidth, textureHeight, textureOrientation, surfaceWidth, surfaceHeight
        )
        recalculate = true
    }

    override fun getTexCoordArray(): FloatBuffer? {
        if (recalculate) {
            var scaleW = if (textureWidth > 0) {
                surfaceWidth / textureWidth.toFloat()
            } else {
                1.0f
            }
            var scaleH = if (textureHeight > 0) {
                surfaceHeight / textureHeight.toFloat()
            } else {
                1.0f
            }

            if (textureOrientation == 90 || textureOrientation == 270) {
                scaleW = surfaceWidth / textureHeight.toFloat()
                scaleH = surfaceHeight / textureWidth.toFloat()
            }

            val scale = min(scaleW, scaleH)
            Timber.i("before: scale = %f, scaleW: %f, scaleH: %f", scale, scaleW, scaleH)

            scaleW /= scale
            scaleH /= scale

            val left = (1 - scaleW) * 0.5f
            val right = left + scaleW
            val bottom = (1 - scaleH) * 0.5f
            val top = bottom + scaleH

            val floatArr = floatArrayOf(
                left, bottom,
                right, bottom,
                left, top,
                right, top
            )

            Timber.i("after: scale = %f, scaleW: %f, scaleH: %f", scale, scaleW, scaleH)
            Timber.i("updated floatArr: %s", floatArr.joinToString())

            val fb: FloatBuffer = tweakedTexCoordArray ?: GlUtil.createFloatBuffer(floatArr)
                .also { tweakedTexCoordArray = it }

            // the floatArr data is in order: bottom left, bottom right, top left, top right
            // and each pair is x, y.

            fb.position(0)
            fb.put(floatArr)
            fb.position(0)

            recalculate = false
        }
        return tweakedTexCoordArray
    }
}
