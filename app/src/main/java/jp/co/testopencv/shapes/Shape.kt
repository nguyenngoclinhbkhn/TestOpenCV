package jp.co.testopencv.shapes

import android.graphics.Path
import android.util.Size
import android.widget.ImageView


/**
 * COPYRIGHT ZYYX. ALL RIGHTS RESERVED,2020
 */
data class Shape(
    var top: ImageView? = null,
    val leftTop: ImageView,
    val rightTop: ImageView,
    val leftBottom: ImageView,
    val rightBottom: ImageView,
    val editSize: ImageView,
    val tagShape: String,
    val size: Size = Size(500, 500),
    var path: Path = Path(),
    var isSelected: Boolean = false,
    var isTriangle: Boolean = false
) {
}