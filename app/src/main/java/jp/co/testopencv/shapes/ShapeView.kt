package jp.co.testopencv.shapes

import android.content.Context
import android.graphics.Color
import android.view.View

/**
 * COPYRIGHT ZYYX. ALL RIGHTS RESERVED,2020
 */
class ShapeView(context: Context): BaseSticker(context) {
    override val mainView: View
        get() {
            val view = View(context)
            view.setBackgroundColor(Color.GRAY)
            view.alpha = 0.8F
            return view
        }

    override fun isGoneActionForCrop() = false

    override fun isCropView() = true
}