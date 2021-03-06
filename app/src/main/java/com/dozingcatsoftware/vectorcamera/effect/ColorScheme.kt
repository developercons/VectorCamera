package com.dozingcatsoftware.vectorcamera.effect

import android.graphics.*
import android.renderscript.Allocation
import android.renderscript.RenderScript
import com.dozingcatsoftware.vectorcamera.*
import com.dozingcatsoftware.util.addAlpha
import com.dozingcatsoftware.util.intFromArgbList
import com.dozingcatsoftware.util.makeAllocationColorMap
import com.dozingcatsoftware.util.makeAlphaAllocation

data class ColorScheme(val colorMap: Allocation,
                       val backgroundFn: (CameraImage, Canvas, RectF) -> Unit) {

    companion object {
        fun fromParameters(rs: RenderScript, params: Map<String, Any>): ColorScheme {

            fun colorAsInt(vararg keys: String): Int {
                for (k in keys) {
                    if (params.containsKey(k)) {
                        return intFromArgbList(params[k] as List<Int>)
                    }
                }
                throw IllegalArgumentException("Key not found: ${keys}")
            }

            when (params["type"]) {
                // For fixed colors, the input maps directly to an output color.
                "fixed" -> {
                    val minColor = colorAsInt("minColor", "minEdgeColor")
                    val maxColor = colorAsInt("maxColor", "maxEdgeColor")
                    val colorMap = makeAllocationColorMap(rs, minColor, maxColor)
                    return ColorScheme(colorMap, { _, _, _ -> Unit })
                }
                // For gradients, we draw the entire canvas with the gradient first, and then mask
                // out each pixel based on the input value. The color of each "foreground" pixel is
                // given by a map where 0 is entirely opaque to show the "background" color, and 255
                // is entirely transparent to let the gradient through.
                "linear_gradient" -> {
                    val minColor = colorAsInt("minColor", "minEdgeColor")
                    val gradientStartColor = colorAsInt("gradientStartColor")
                    val gradientEndColor = colorAsInt("gradientEndColor")
                    val backgroundFn = fun(_: CameraImage, canvas: Canvas, rect: RectF) {
                        val p = Paint()
                        p.shader = LinearGradient(
                                rect.left, rect.top, rect.right, rect.bottom,
                                addAlpha(gradientStartColor), addAlpha(gradientEndColor),
                                Shader.TileMode.MIRROR)
                        canvas.drawRect(rect, p)
                    }
                    return ColorScheme(makeAlphaAllocation(rs, minColor), backgroundFn)
                }
                "radial_gradient" -> {
                    val minColor = colorAsInt("minColor", "minEdgeColor")
                    val centerColor = colorAsInt("centerColor")
                    val outerColor = colorAsInt("outerColor")
                    val backgroundFn = fun(_: CameraImage, canvas: Canvas, rect: RectF) {
                        val p = Paint()
                        p.shader = RadialGradient(
                                rect.width() / 2, rect.height() / 2,
                                maxOf(rect.width(), rect.height()) / 2f,
                                addAlpha(centerColor), addAlpha(outerColor), Shader.TileMode.MIRROR)
                        canvas.drawRect(rect, p)
                    }
                    return ColorScheme(makeAlphaAllocation(rs, minColor), backgroundFn)
                }
                "grid_gradient" -> {
                    val minColor = colorAsInt("minColor")
                    val gridColors = params["grid"] as List<List<List<Int>>>
                    val speedX = params.getOrElse("speedX", {0}) as Number
                    val speedY = params.getOrElse("speedY", {0}) as Number
                    val sizeX = params.getOrElse("sizeX", {1}) as Number
                    val sizeY = params.getOrElse("sizeY", {1}) as Number
                    val pixPerCell = params.getOrElse(
                            "pixelsPerCell", {Animated2dGradient.DEFAULT_PIXELS_PER_CELL}) as Number
                    val gradient = Animated2dGradient(gridColors, speedX.toInt(), speedY.toInt(),
                            sizeX.toFloat(), sizeY.toFloat(), pixPerCell.toInt())
                    val backgroundFn = fun(cameraImage: CameraImage, canvas: Canvas, rect: RectF) {
                        gradient.drawToCanvas(canvas, rect, cameraImage.timestamp)
                    }
                    return ColorScheme(makeAlphaAllocation(rs, minColor), backgroundFn)
                }
            }
            throw IllegalArgumentException("Unknown parameters: " + params)
        }
    }
}