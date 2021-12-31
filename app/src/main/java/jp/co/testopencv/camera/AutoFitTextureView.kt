package jp.co.testopencv.camera

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.util.Size
import android.view.TextureView
import androidx.annotation.RequiresApi


/**
 * COPYRIGHT ZYYX. ALL RIGHTS RESERVED,2020
 */
class AutoFitTextureView(context: Context, attrs: AttributeSet?, defStyle: Int) :
    TextureView(context, attrs, defStyle) {
    private var mRatioWidth = 0
    private var mRatioHeight = 0

    constructor(context: Context) : this(context, null) {}
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0) {}

    /**
     * Sets the aspect ratio for this view. The size of the view will be measured based on the ratio
     * calculated from the parameters. Note that the actual sizes of parameters don't matter, that
     * is, calling setAspectRatio(2, 3) and setAspectRatio(4, 6) make the same result.
     *
     * @param width  Relative horizontal size
     * @param height Relative vertical size
     */
    fun setAspectRatio(width: Int, height: Int) {
        require(!(width < 0 || height < 0)) { "Size cannot be negative." }
        mRatioWidth = width
        mRatioHeight = height
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width: Int = MeasureSpec.getSize(widthMeasureSpec)
        val height: Int = MeasureSpec.getSize(heightMeasureSpec)
        if (0 == mRatioWidth || 0 == mRatioHeight) {
            setMeasuredDimension(width, height)
        } else {
            if (width < height * mRatioWidth / mRatioHeight) {
                setMeasuredDimension(
                    height * mRatioWidth / mRatioHeight,
                    height
                )
            } else {
                setMeasuredDimension(height * mRatioWidth / mRatioHeight, height)
            }
        }
    }

    companion object {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        private fun chooseVideoSize(choices: Array<Size>): Size {
            for (size in choices) {
                // Note that it will pick only HD video size, you should create more robust solution depending on screen size and available video sizes
                if (1920 == size.getWidth() && 1080 == size.getHeight()) {
                    return size
                }
            }
            return choices[choices.size - 1]
        }
    }
}