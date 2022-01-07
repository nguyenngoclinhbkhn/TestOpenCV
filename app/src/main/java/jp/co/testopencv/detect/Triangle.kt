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
    companion object {
        fun getDefaultTriangle(widthScreen: Int): Triangle {
            val topX = widthScreen.toDouble() / 2.0
            val offset = 100.0
            val topY = 50.0
            return Triangle(
                top = Point(topX, topY),
                left = Point(topX - offset, topY + offset),
                right = Point(topX + offset, topY + offset)
            )
        }
    }
    fun findTop(): Point {
        var top = top
        var topFake = if (left.y < right.y) left else right
        return if (topFake.y < top.y) topFake else top
    }

    fun findLeftBottom(): Point {
        val left = left
        var left1 = if (left.x < right.y) left else right
        return if (left1.x < top.x) left1 else top
    }

    fun findRightBottom(): Point {
        val right = right
        val right1 = if (right.x > left.x) right else left
        return if (right1.x > top.x) right1 else top
    }

}