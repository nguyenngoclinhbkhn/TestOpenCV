package jp.co.testopencv.shapes

import android.graphics.Path
import android.util.Size
import android.widget.ImageView


/**
 * COPYRIGHT ZYYX. ALL RIGHTS RESERVED,2020
 */
data class RectangleShape(
    val leftTop: ImageView,
    val rightTop: ImageView,
    val leftBottom: ImageView,
    val rightBottom: ImageView,
    val editSize: ImageView,
    val tagShape: String,
    val size: Size = Size(500, 500),
    var path: Path = Path(),
    var isSelected: Boolean = false
) : Shape {
    override fun isTriangle() = false

    companion object {
        fun getRectangleDefault(widthDisplay: Int): org.opencv.core.Rect {
            val sizeDefault = 200
            val topX = (widthDisplay.toDouble() / 2.0) - sizeDefault.toDouble() / 2.0
            val topY = 100.0
            return org.opencv.core.Rect(
                topX.toInt(),
                topY.toInt(),
                sizeDefault,
                sizeDefault
            )
        }
    }
}

data class TriangleShape(
    val top: ImageView,
    val leftBottom: ImageView,
    val rightBottom: ImageView,
    val tagShape: String,
    val size: Size = Size(500, 500),
    var path: Path = Path(),
    var isSelected: Boolean = false
) : Shape {
    override fun isTriangle() = true
}

interface Shape {
    fun isTriangle(): Boolean
}