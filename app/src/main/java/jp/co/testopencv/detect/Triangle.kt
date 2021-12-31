package jp.co.testopencv.detect

import org.opencv.core.Point

/**
 * COPYRIGHT ZYYX. ALL RIGHTS RESERVED,2020
 */
data class Triangle(
    val top: Point,
    val left: Point,
    val right: Point
) {
}