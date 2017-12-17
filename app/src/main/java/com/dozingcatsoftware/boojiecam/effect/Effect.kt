package com.dozingcatsoftware.boojiecam.effect

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.RectF
import com.dozingcatsoftware.boojiecam.CameraImage

/**
 * Created by brian on 12/16/17.
 */
interface Effect {
    fun createBitmap(cameraImage: CameraImage): Bitmap

    fun createPaintFn(cameraImage: CameraImage): (RectF) -> Paint? {
        return {null}
    }

    fun effectName(): String

    fun effectParameters(): Map<String, Any> = mapOf()

}