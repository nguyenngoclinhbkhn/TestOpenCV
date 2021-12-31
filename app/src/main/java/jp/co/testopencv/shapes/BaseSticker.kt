package jp.co.testopencv.shapes

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import jp.co.testopencv.R
import java.util.*
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * COPYRIGHT ZYYX. ALL RIGHTS RESERVED,2020
 */
abstract class BaseSticker : FrameLayout {
    private lateinit var imageViewBorder: BorderView
    private lateinit var imgViewScaleTopLeft: ImageView
    private lateinit var imgViewScaleTopRight: ImageView
    private lateinit var imgViewScaleBottomRight: ImageView
    private lateinit var imgViewScaleBottomLeft: ImageView

    private var isPinchZoom = false
    private var isPinchRotate = false
    private var isMoving = false
    private var isStickerSelected = false


    var designScale: Double = 1.0
    private var rotationNext = 0F
    private var timeDown = 0L
    private var timeUp = 0L
    private var offsetTimeUpDown = 0L
    private var isClick = false
    private var isDoubleClick = false
    private var ratio = 0.0

    abstract val mainView: View
    abstract fun isGoneActionForCrop() : Boolean

    fun setupParent() {
        this.parent?.let { parent ->
            if ((parent as View) is FrameLayout) {
                (parent as View).tag = TAG_PARENT
                (parent as View).setOnTouchListener(mTouchListener)
            }
        }
    }

    private var TAG = "TAG"

    private var path: Path?= null
    private var paint: Paint? = null

    private var scale_orgWidth = -1.0
    private var scale_orgHeight = -1.0
    private var length1: Double = 0.0
    private var scale_orgX = -1f
    private var scale_orgY = -1f
    private val NONE = 0
    private val DRAG = 1
    private val ZOOM = 2
    private var mode = NONE
    var offsetAngle = 0.0
    private var offsetFist = false

    private var oldDistance = 1f

    // For rotating
    private var rotateStartX = -1f
    private var rotateStartY = -1f
    private var rotateNewX = -1f
    private var rotateNewY = -1f

    // For moving
    private var moveStartX = -1f
    private var moveStartY = -1f
    private var centerX = 0.0
    private var centerY = 0.0

    //For moving to crop
    private var startX = 0.0
    private var startY = 0.0

    private var widthOld = 0
    private var heightOld = 0
    private var positionX = 0F
    private var positionY = 0F

    private var heightTest = 0
    private var widthTest = 0

    private var rotationFirst = 0F
    private var frameOffset: Int = 0

    private var stickerListener: OnStickerListener? = null
    private var onStickerChange: OnStickerChange? = null

    fun setStickerChanged(onStickerChange: OnStickerChange) {
        this.onStickerChange = onStickerChange
    }

    interface OnStickerChange {
        fun onBeforeChanged()
        fun onAfterChanged()
    }

    interface OnStickerListener {
        fun onRectCrop(rect: Rect?)
    }

    fun setStickerListener(onStickerListener: OnStickerListener) {
        this.stickerListener = onStickerListener
    }

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init(context)
    }

    var statusHeight = 0.0
    abstract fun isCropView(): Boolean

    @SuppressLint("ClickableViewAccessibility")
    private fun init(context: Context) {
        TAG = UUID.randomUUID().toString()
        frameOffset = 0
        path = Path()
        paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint?.color = Color.GREEN
        paint?.strokeWidth = 5F
        paint?.strokeCap = Paint.Cap.ROUND
        paint?.strokeJoin = Paint.Join.ROUND
        paint?.style = Paint.Style.STROKE

        imageViewBorder = BorderView(context)
        imgViewScaleTopLeft = ImageView(context)
        imgViewScaleTopRight = ImageView(context)
        imgViewScaleBottomLeft = ImageView(context)
        imgViewScaleBottomRight = ImageView(context)
        var result = 0
        val resourceId =
            resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId)
        }
        statusHeight = result.toDouble()
        val thisParams =
            LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        this.layoutParams = thisParams

        val ivMainParams =
            LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        ivMainParams.setMargins(0, 0, 0, 0)
        this.addView(mainView, ivMainParams)
        mainView.setOnTouchListener(mTouchListener)

        if (!isCropView()) {
//            setBackgroundColor(Color.GREEN)

        } else {
            imageViewBorder.tag = "iv_border"
            val ivBorderParams =
                LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            ivBorderParams.setMargins(0, 0, 0, 0)
            this.addView(imageViewBorder, ivBorderParams)

            if (!isGoneActionForCrop()) {
                this.setBackgroundColor(Color.RED)
                imgViewScaleTopLeft.tag = TAG_SCALE_TOP_LEFT
                imgViewScaleTopRight.tag = TAG_SCALE_TOP_RIGHT
                imgViewScaleBottomLeft.tag = TAG_SCALE_BOTTOM_LEFT
                imgViewScaleBottomRight.tag = TAG_SCALE_BOTTOM_RIGHT

                imgViewScaleTopLeft.setImageResource(R.drawable.circle)
                imgViewScaleTopRight.setImageResource(R.drawable.circle)
                imgViewScaleBottomRight.setImageResource(R.drawable.circle)
                imgViewScaleBottomLeft.setImageResource(R.drawable.circle)

                val size = 60
                val margin = 0
                val sizeVertical = 60
                //top left
                val ivScaleTopLeftParams = LayoutParams(size, size)
                ivScaleTopLeftParams.setMargins(margin, margin, 0, 0)
                ivScaleTopLeftParams.gravity = Gravity.TOP or Gravity.START

                //center left
                val ivScaleCenterLeftParams = LayoutParams(sizeVertical / 3, sizeVertical)
                ivScaleCenterLeftParams.setMargins(margin, 0, 0, 0)
                ivScaleCenterLeftParams.gravity = Gravity.CENTER_VERTICAL or Gravity.START

                //top right
                val ivScaleTopRightParams = LayoutParams(size, size)
                ivScaleTopRightParams.setMargins(0, margin, margin, 0)
                ivScaleTopRightParams.gravity = Gravity.TOP or Gravity.END

                //center right
                val ivScaleCenterRightParams = LayoutParams(sizeVertical / 3, sizeVertical)
                ivScaleCenterRightParams.setMargins(0, 0, margin, 0)
                ivScaleCenterRightParams.gravity = Gravity.CENTER_VERTICAL or Gravity.END

                //bottom left
                val ivScaleBottomLeftParams = LayoutParams(size, size)
                ivScaleBottomLeftParams.setMargins(margin, 0, 0, margin)
                ivScaleBottomLeftParams.gravity = Gravity.BOTTOM or Gravity.START

                //bottom right
                val ivScaleBottomRightParams = LayoutParams(size, size)
                ivScaleBottomRightParams.setMargins(0, 0, margin, margin)
                ivScaleBottomRightParams.gravity = Gravity.BOTTOM or Gravity.END

                // top center
                val ivScaleTopCenterParams = LayoutParams(sizeVertical, sizeVertical / 3)
                ivScaleTopCenterParams.setMargins(0, margin, 0, 0)
                ivScaleTopCenterParams.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL

                // bottom center
                val ivScaleBottomCenterParams = LayoutParams(sizeVertical, sizeVertical / 3)
                ivScaleBottomCenterParams.setMargins(0, 0, 0, margin)
                ivScaleBottomCenterParams.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL

                this.addView(imgViewScaleTopLeft, ivScaleTopLeftParams)
                this.addView(imgViewScaleTopRight, ivScaleTopRightParams)
                this.addView(imgViewScaleBottomLeft, ivScaleBottomLeftParams)
                this.addView(imgViewScaleBottomRight, ivScaleBottomRightParams)

                imgViewScaleBottomRight.setOnTouchListener(mTouchListener)
                imgViewScaleBottomLeft.setOnTouchListener(mTouchListener)
                imgViewScaleTopLeft.setOnTouchListener(mTouchListener)
                imgViewScaleTopRight.setOnTouchListener(mTouchListener)
            }
        }
    }

    fun isActiveSticker(isActive: Boolean) {
        mainView.isEnabled = isActive
        imgViewScaleTopLeft.isEnabled = isActive
    }

    // this variable to use for scale top left
    var coordinateBottomRightX = 0F
    var coordinateBottomRightY = 0F

    // this variable to use for scale top right
    var coordinateBottomLeftX = 0F
    var coordinateBottomLeftY = 0F

    //this variable to use for scale bottom left
    var coordinateTopRightX = 0F
    var coordinateTopRightY = 0F

    //this variable to use for scale top center
    var coordinateBottomX = 0F
    var coordinateBottomY = 0F

    //this variable to use for scale bottom center
    var coordinateTopX = 0F
    var coordinateTopY = 0F


    //this variable to use for scale right center
    var coordinateRightX = 0F
    var coordinateRightY = 0F


    //this variable to use for zoom in zoom out validate image content
    var widthValidate = 0
    var heightValidate = 0
    var scaleNeed = 1.0


    @SuppressLint("ClickableViewAccessibility")
    open val mTouchListener = OnTouchListener { view, event ->
        when (view.tag) {
            "DraggableViewGroup", TAG_PARENT -> {
                when (event.action and MotionEvent.ACTION_MASK) {
                    MotionEvent.ACTION_DOWN -> {
                        onStickerChange?.onBeforeChanged()
                        moveStartX = event.rawX
                        moveStartY = event.rawY
                        mode = DRAG;
                        timeDown = System.currentTimeMillis()
                    }
                    MotionEvent.ACTION_POINTER_DOWN -> {
                        oldDistance = spacing(event)
                        if (oldDistance > 10F) {
                            mode = ZOOM
                            widthOld = this.width
                            heightOld = this.height
                            heightTest = heightOld
                            widthTest = widthOld
                            widthValidate = widthOld
                            heightValidate = heightOld
                            positionX = this.x
                            positionY = this.y
                            rotationFirst = getRotationFromTwoFinger(event)
                        }
                    }
                    MotionEvent.ACTION_MOVE -> {

//                        Timber.e("$TAG-----------  MOVE Selected $isStickerSelected")
                        if (isStickerSelected) {
                            if (mode == DRAG) {

                                val offsetX = event.rawX - moveStartX
                                val offsetY = event.rawY - moveStartY
                                var coordinateX: Float =
                                    (this.x + offsetX).toFloat()
                                var coordinateY: Float =
                                    (this.y + offsetY).toFloat()

                            } else if (mode == ZOOM) {
                                if (event.pointerCount == 2) {
                                    val newDist = spacing(event)
                                    val scaleZoom: Double = newDist.toDouble() / (oldDistance.toDouble())
                                    isPinchZoom = true
                                    if (isPinchZoom) {
                                        val widthNew =
                                            (((widthOld - frameOffset) * scaleZoom) + frameOffset).toInt()
                                        val heightNew =
                                            (((heightOld - frameOffset) * scaleZoom) + frameOffset).toInt()
                                        val middleXNew = this.x + widthNew.toFloat() / 2F
                                        val middleYNew = this.y + heightNew.toFloat() / 2F

                                        val middleXOld = this.x + widthTest.toFloat() / 2F
                                        val middleYOld = this.y + heightTest.toFloat() / 2F
                                        widthTest = widthNew
                                        heightTest = heightNew
                                        val offsetX = middleXNew - middleXOld
                                        val offsetY = middleYNew - middleYOld
                                        val currentX = x
                                        val currentY = y
                                        val coordinateX = currentX - offsetX
                                        val coordinateY = currentY - offsetY
                                    }
                                }
                            }
                        }
                    }
                    MotionEvent.ACTION_UP -> {
                        scaleNeed = 1.0
                        if (mode == ZOOM && isPinchZoom) {
                        }
                        timeUp = System.currentTimeMillis()
                        isClick = timeUp - timeDown <= OFFSET_TIME_DOUBLE_TAP
                        isDoubleClick = timeDown - timeUp <= OFFSET_TIME_DOUBLE_TAP
                        if (isClick && isDoubleClick) {
                            isClick = false
                            isDoubleClick = false
                        }
//                        txtAngle.gone()
                        isPinchZoom = false
                        isPinchRotate = false
                        isMoving = false
                        isStickerSelected = true
//                        Timber.e("$TAG-----------  UP selected: $isStickerSelected")
                        onStickerChange?.onAfterChanged()
                    }
                }
            }
            TAG_SCALE_BOTTOM_RIGHT -> { // ok
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        scale_orgWidth = this.layoutParams.width.toDouble()
                        scale_orgHeight = this.layoutParams.height.toDouble()
                        scale_orgX = event.rawX
                        scale_orgY = event.rawY

                        widthOld = this.width
                        heightOld = this.height
                        heightTest = heightOld
                        widthTest = widthOld

                        startX = (this.x + (this.parent as View).x).toDouble()
                        startY = this.y + (this.parent as View).y.toDouble()
                        length1 = getLength(
                            startX,
                            startY,
                            scale_orgX.toDouble(),
                            scale_orgY.toDouble() - statusHeight
                        )
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val length2 = getLength(
                            startX,
                            startY,
                            event.rawX.toDouble(), event.rawY.toDouble() - statusHeight
                        )

                        val scale: Double = length2 / (length1)
                        if (scale > 0.1) {
                            var widthNew =
                                (((widthOld - frameOffset) * scale) + frameOffset).toInt()
                            var heightNew =
                                (((heightOld - frameOffset) * scale) + frameOffset).toInt()
                            if (heightNew > (getStickerOffset() * 1.3)) {
                                this.layoutParams.width = widthNew
                                this.layoutParams.height = heightNew
                                stickerListener?.onRectCrop(
                                    Rect(
                                        this.x.toInt(),
                                        this.y.toInt(),
                                        this.x.toInt() + widthNew,
                                        this.y.toInt() + heightNew
                                    )
                                )
                                this.requestLayout()
                                widthTest = widthNew
                                heightTest = heightNew
                            }
                        }
//                        postInvalidate()
                    }
                    MotionEvent.ACTION_UP -> {
                    }
                }
            }
            TAG_SCALE_BOTTOM_LEFT -> { // ok
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        coordinateTopRightX = this.x + width.toFloat()
                        coordinateTopRightY = this.y

                        scale_orgWidth = this.layoutParams.width.toDouble()
                        scale_orgHeight = this.layoutParams.height.toDouble()
                        scale_orgX = event.rawX
                        scale_orgY = event.rawY
                        rotateStartX = event.rawX
                        rotateStartY = event.rawY
                        widthOld = this.width
                        heightOld = this.height
                        heightTest = heightOld
                        widthTest = widthOld

                        startX = this.x + (this.parent as View).x + width.toDouble()
                        startY = this.y + (this.parent as View).y.toDouble()

                        length1 = getLength(
                            startX,
                            startY,
                            scale_orgX.toDouble(),
                            scale_orgY.toDouble()
                        )
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val length2 = getLength(
                            startX,
                            startY,
                            event.rawX.toDouble(),
                            event.rawY.toDouble() - statusHeight
                        )
                        val scale: Double = length2 / (length1)
                        if (scale > 0.1) {
                            var widthNew =
                                (((widthOld - frameOffset) * scale) + frameOffset).toInt()
                            var heightNew =
                                (((heightOld - frameOffset) * scale) + frameOffset).toInt()
                            var coordinateX = coordinateTopRightX - widthNew.toFloat()
                            if (heightNew > (getStickerOffset() * 1.3)) {
                                this.layoutParams.width = widthNew
                                this.layoutParams.height = heightNew
                                this.requestLayout()
                                this.x = coordinateTopRightX - widthNew.toFloat()
                                stickerListener?.onRectCrop(
                                    Rect(
                                        this.x.toInt(),
                                        this.y.toInt(),
                                        this.x.toInt() + widthNew,
                                        this.y.toInt() + heightNew
                                    )
                                )
                            }
                        }
                    }
                    MotionEvent.ACTION_UP -> {
//                        txtAngle.gone()
                        onStickerChange?.onAfterChanged()
                    }
                }
            }
            TAG_SCALE_TOP_LEFT -> {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        coordinateBottomRightX = this.x + this.width.toFloat()
                        coordinateBottomRightY = this.y + this.height.toFloat()

                        scale_orgWidth = this.layoutParams.width.toDouble()
                        scale_orgHeight = this.layoutParams.height.toDouble()
                        scale_orgX = event.rawX
                        scale_orgY = event.rawY
                        rotateStartX = event.rawX
                        rotateStartY = event.rawY
                        widthOld = this.width
                        heightOld = this.height
                        heightTest = heightOld
                        widthTest = widthOld

                        startX = this.x + (this.parent as View).x + width.toDouble()
                        startY = this.y + (this.parent as View).y + height.toDouble()
                        length1 = getLength(
                            startX,
                            startY,
                            scale_orgX.toDouble(),
                            scale_orgY.toDouble() - statusHeight
                        )
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val length2 = getLength(
                            startX,
                            startY,
                            event.rawX.toDouble(),
                            event.rawY.toDouble() - statusHeight
                        )
                        val scale: Double = length2 / (length1)
                        if (scale > 0.1) {
                            var widthNew =
                                (((widthOld - frameOffset) * scale) + frameOffset).toInt()
                            var heightNew =
                                (((heightOld - frameOffset) * scale) + frameOffset).toInt()
                            var coordinateX = coordinateBottomRightX - widthNew.toFloat()
                            var coordinateY = coordinateBottomRightY - heightNew.toFloat()
                            if (heightNew > (getStickerOffset() * 1.3)) {
                                this.layoutParams.width = ((widthNew)).toInt()
                                this.layoutParams.height = (heightNew).toInt()
                                this.requestLayout()
                                this.x = coordinateX
                                this.y = coordinateY
                                stickerListener?.onRectCrop(
                                    Rect(
                                        this.x.toInt(),
                                        this.y.toInt(),
                                        this.x.toInt() + widthNew,
                                        this.y.toInt() + heightNew
                                    )
                                )
                            }
                        }
                    }
                    MotionEvent.ACTION_UP -> {
//                        txtAngle.gone()
                        onStickerChange?.onAfterChanged()
                    }
                }
            }
            TAG_SCALE_TOP_RIGHT -> {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        coordinateBottomLeftX = this.x
                        coordinateBottomLeftY = this.y + height.toFloat()

                        scale_orgWidth = this.layoutParams.width.toDouble()
                        scale_orgHeight = this.layoutParams.height.toDouble()
                        scale_orgX = event.rawX
                        scale_orgY = event.rawY
                        widthOld = this.width
                        heightOld = this.height
                        heightTest = heightOld
                        widthTest = widthOld

                        startX = (this.x + (this.parent as View).x).toDouble()
                        startY = (this.y + (this.parent as View).y) + height.toDouble()
                        length1 = getLength(
                            startX,
                            startY,
                            scale_orgX.toDouble(),
                            scale_orgY.toDouble() - statusHeight
                        )
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val length2 = getLength(
                            startX,
                            startY,
                            event.rawX.toDouble(),
                            event.rawY.toDouble() - statusHeight
                        )
                        val scale: Double = length2 / (length1)

                        if (scale > 0.1) {
                            var widthNew =
                                (((widthOld - frameOffset) * scale) + frameOffset).toInt()
                            var heightNew =
                                (((heightOld - frameOffset) * scale) + frameOffset).toInt()
                            var coordinateY = coordinateBottomLeftY - heightNew.toFloat()
                            if (heightNew > (getStickerOffset() * 1.3)) {
                                this.layoutParams.width = ((widthNew)).toInt()
                                this.layoutParams.height = (heightNew).toInt()
                                this.requestLayout()
                                this.y = coordinateBottomLeftY - heightNew.toFloat()
                                stickerListener?.onRectCrop(
                                    Rect(
                                        this.x.toInt(),
                                        this.y.toInt(),
                                        this.x.toInt() + widthNew,
                                        this.y.toInt() + heightNew
                                    )
                                )
                            }
                        }
                    }
                    MotionEvent.ACTION_UP -> {
                        onStickerChange?.onAfterChanged()
                    }
                }
            }
            TAG_SCALE_TOP_CENTER -> {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        coordinateBottomX = this.x + width.toFloat()
                        coordinateBottomY = this.y + height.toFloat()

                        scale_orgWidth = this.layoutParams.width.toDouble()
                        scale_orgHeight = this.layoutParams.height.toDouble()
                        scale_orgX = event.rawX
                        scale_orgY = event.rawY
                        widthOld = this.width
                        heightOld = this.height
                        heightTest = heightOld
                        widthTest = widthOld

                        startX = (this.x + (this.parent as View).x) + (width.toDouble() / 2.0)
                        startY = (this.y + (this.parent as View).y) + height.toDouble()
                        length1 = getLength(
                            startX,
                            startY,
                            scale_orgX.toDouble(),
                            scale_orgY.toDouble() - statusHeight
                        )
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val length2 = getLength(
                            startX,
                            startY,
                            event.rawX.toDouble(),
                            event.rawY.toDouble() - statusHeight
                        )
                        val scale: Double = length2 / (length1)

                        if (scale > 0.1) {
                            var heightNew =
                                (((heightOld - frameOffset) * scale) + frameOffset).toInt()
                            if (heightNew > (getStickerOffset() * 1.3)) {
                                var coordinateY = coordinateBottomY - heightNew
                                this.layoutParams.height = heightNew
                                this.requestLayout()
                                this.y = coordinateY
                                stickerListener?.onRectCrop(
                                    Rect(
                                        this.x.toInt(),
                                        this.y.toInt(),
                                        this.x.toInt() + this.width,
                                        this.y.toInt() + heightNew
                                    )
                                )
                            }
                        }
                    }
                    MotionEvent.ACTION_UP -> {
                        onStickerChange?.onAfterChanged()
                    }
                }
            }
            TAG_SCALE_BOTTOM_CENTER -> {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        scale_orgWidth = this.layoutParams.width.toDouble()
                        scale_orgHeight = this.layoutParams.height.toDouble()
                        scale_orgX = event.rawX
                        scale_orgY = event.rawY
                        widthOld = this.width
                        heightOld = this.height
                        heightTest = heightOld
                        widthTest = widthOld

                        startX = (this.x + (this.parent as View).x).toDouble() + (width.toDouble() / 2.0)
                        startY = (this.y + (this.parent as View).y).toDouble()
                        length1 = getLength(
                            startX,
                            startY,
                            scale_orgX.toDouble(),
                            scale_orgY.toDouble() - statusHeight
                        )
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val length2 = getLength(
                            startX,
                            startY,
                            event.rawX.toDouble(),
                            event.rawY.toDouble() - statusHeight
                        )
                        val scale: Double = length2 / (length1)
                        if (scale > 0.1) {
                            var heightNew =
                                (((heightOld - frameOffset) * scale) + frameOffset).toInt()
                            if (heightNew > (getStickerOffset() * 1.3)) {
                                this.layoutParams.height = heightNew
                                stickerListener?.onRectCrop(
                                    Rect(
                                        this.x.toInt(),
                                        this.y.toInt(),
                                        this.x.toInt() + this.width,
                                        this.y.toInt() + heightNew
                                    )
                                )
                                this.requestLayout()
                            }
                        }
                    }
                    MotionEvent.ACTION_UP -> {

                    }
                }
            }
            TAG_SCALE_LEFT_CENTER -> {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        coordinateRightX = this.x + width.toFloat()
                        scale_orgWidth = this.layoutParams.width.toDouble()
                        scale_orgHeight = this.layoutParams.height.toDouble()
                        scale_orgX = event.rawX
                        scale_orgY = event.rawY
                        widthOld = this.width
                        heightOld = this.height
                        heightTest = heightOld
                        widthTest = widthOld

                        startX = (this.x + (this.parent as View).x).toDouble() + width.toDouble()
                        startY = (this.y + (this.parent as View).y) + (height.toDouble() / 2.0)
                        length1 = getLength(
                            startX,
                            startY,
                            scale_orgX.toDouble(),
                            scale_orgY.toDouble() - statusHeight
                        )
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val length2 = getLength(
                            startX,
                            startY,
                            event.rawX.toDouble(),
                            event.rawY.toDouble() - statusHeight
                        )
                        val scale: Double = length2 / (length1)

                        if (scale > 0.1) {
                            var widthNew = (((widthOld - frameOffset) * scale) + frameOffset).toInt()
                            if (widthNew > (getStickerOffset() * 1.3)) {
                                var coordinateX = coordinateRightX - widthNew
                                this.layoutParams.width = widthNew
                                this.requestLayout()
                                this.x = coordinateX
                                stickerListener?.onRectCrop(
                                    Rect(
                                        this.x.toInt(),
                                        this.y.toInt(),
                                        this.x.toInt() + widthNew,
                                        this.y.toInt() + this.height
                                    )
                                )
                            }
                        }
                    }
                    MotionEvent.ACTION_UP -> {
                        onStickerChange?.onAfterChanged()
                    }
                }
            }
            TAG_SCALE_RIGHT_CENTER -> {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        scale_orgWidth = this.layoutParams.width.toDouble()
                        scale_orgHeight = this.layoutParams.height.toDouble()
                        scale_orgX = event.rawX
                        scale_orgY = event.rawY
                        widthOld = this.width
                        heightOld = this.height
                        heightTest = heightOld
                        widthTest = widthOld

                        startX = (this.x + (this.parent as View).x).toDouble()
                        startY = (this.y + (this.parent as View).y) + (height.toDouble() / 2.0)
                        length1 = getLength(
                            startX,
                            startY,
                            scale_orgX.toDouble(),
                            scale_orgY.toDouble() - statusHeight
                        )
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val length2 = getLength(
                            startX,
                            startY,
                            event.rawX.toDouble(),
                            event.rawY.toDouble() - statusHeight
                        )
                        val scale: Double = length2 / (length1)

                        if (scale > 0.1) {
                            var widthNew =
                                (((widthOld - frameOffset) * scale) + frameOffset).toInt()
                            if (widthNew > (getStickerOffset() * 1.3)) {
                                this.layoutParams.width = widthNew
                                stickerListener?.onRectCrop(
                                    Rect(
                                        this.x.toInt(),
                                        this.y.toInt(),
                                        this.x.toInt() + widthNew,
                                        this.y.toInt() + this.height
                                    )
                                )
                                this.requestLayout()
                            }
                        }
                    }
                    MotionEvent.ACTION_UP -> {
                        onStickerChange?.onAfterChanged()
                    }
                }
            }
        }
        true
    }


    fun spacing(event: MotionEvent): Float {
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return sqrt(x * x + y * y)
    }

    fun getLength(x1: Double, y1: Double, x2: Double, y2: Double): Double {
        return sqrt((y2 - y1).pow(2.0) + (x2 - x1).pow(2.0))
    }

    fun getRotationFromTwoFinger(event: MotionEvent): Float {
        val deltaX = (event.getX(0) - event.getX(1)).toDouble()
        val deltaY = (event.getY(0) - event.getY(1)).toDouble()
        val radians = atan2(deltaY, deltaX)

        return Math.toDegrees(radians).toFloat()
    }

    fun getRotationBetweenTwoFinger(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val deltaX = (x1 - x2).toDouble()
        val deltaY = (y1 - y2).toDouble()
        val radians = atan2(deltaY, deltaX)

        return Math.toDegrees(radians).toFloat()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        path?.reset()
        val offset = 30
        path?.moveTo(imgViewScaleTopLeft.x + offset, imgViewScaleTopLeft.y + offset)
        path?.lineTo(imgViewScaleTopRight.x + offset, imgViewScaleTopRight.y+ offset)
        path?.lineTo(imgViewScaleBottomRight.x + offset, imgViewScaleBottomRight.y + offset)
        path?.lineTo(imgViewScaleBottomLeft.x + offset, imgViewScaleBottomRight.y + offset)
        path?.lineTo(imgViewScaleTopLeft.x + offset, imgViewScaleTopLeft.y + offset)
        path?.let { paint?.let { it1 -> canvas?.drawPath(it, it1) } }
    }

    fun setGoneBorderAndButton() {
        imageViewBorder.visibility = View.GONE
        imgViewScaleTopRight.visibility = View.GONE
        imgViewScaleBottomRight.visibility = View.GONE
        imgViewScaleTopLeft.visibility = View.GONE
    }

    open fun setVisibleBorderAndButton() {
        imageViewBorder.visibility = View.VISIBLE
        imgViewScaleTopLeft.visibility = View.VISIBLE
    }

    fun setGoneBorder() {
        imageViewBorder.visibility = View.GONE
    }

    open fun setVisibleBorder() {
        setGoneBorderAndButton()
        imageViewBorder.visibility = View.VISIBLE
    }

    fun doUpdateSticker(stickerView: BaseSticker) {
        this.x = stickerView.x
        this.y = stickerView.y
        this.rotation = stickerView.rotation
        this.layoutParams = stickerView.layoutParams
    }

    fun doFlipSticker(stickerView: BaseSticker) {
        val oa1 = ObjectAnimator.ofFloat(stickerView, "scaleX", 1f, 0f)
        val oa2 = ObjectAnimator.ofFloat(stickerView, "scaleX", 0f, 1f)
        oa1.duration = 200
        oa2.duration = 200
        oa1.interpolator = DecelerateInterpolator()
        oa2.interpolator = AccelerateDecelerateInterpolator()
        oa1.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                stickerView.mainView.rotationY =
                    if (stickerView.mainView.rotationY == -180f) 0f else -180f
                stickerView.mainView.invalidate()
                oa2.start()
            }
        })
        oa1.start()
        oa2.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
            }
        })
    }

    fun setBorderColor(color: Int) {
        imageViewBorder.setColorBorder(color)
    }

    fun getBorderRect(): Rect {
        return imageViewBorder.getBorderRect()
    }

    private inner class BorderView : View {
        private lateinit var borderPaint: Paint
        private lateinit var border: Rect

        constructor(context: Context?) : super(context) {
            init()
        }

        constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
            init()
        }

        constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
            context,
            attrs,
            defStyle
        ) {
            init()
        }

        private fun init() {
            borderPaint = Paint()
            borderPaint.strokeWidth = 1F
            borderPaint.color = Color.WHITE
            borderPaint.style = Paint.Style.STROKE
            borderPaint.strokeJoin = Paint.Join.ROUND
            borderPaint.strokeCap = Paint.Cap.ROUND
            border = Rect()

        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            // Draw sticker border
            val params = layoutParams as LayoutParams
            border.left = this.left - params.leftMargin + 1 // for draw border
            border.top = this.top - params.topMargin + 1 // for draw border
            border.right = this.right - params.rightMargin
            border.bottom = this.bottom - params.bottomMargin
            canvas.drawRect(border, borderPaint)
        }

        fun setColorBorder(color: Int) {
            borderPaint.color = color
            invalidate()
        }

        fun getBorderRect(): Rect {
            return border
        }
    }


    companion object {
        const val TAG_PARENT = "view_parent"
        const val TAG_SCALE_TOP_CENTER = "iv_top_center"
        const val TAG_SCALE_LEFT_CENTER = "iv_center_left"
        const val TAG_SCALE_BOTTOM_CENTER = "iv_bottom_center"
        const val TAG_SCALE_RIGHT_CENTER = "iv_center_right"
        const val TAG_SCALE_TOP_LEFT = "iv_top_left"
        const val TAG_SCALE_TOP_RIGHT = "iv_top_right"
        const val TAG_SCALE_BOTTOM_LEFT = "iv_bottom_left"
        const val TAG_SCALE_BOTTOM_RIGHT = "iv_bottom_right"

        const val OFFSET_TIME_DOUBLE_TAP = 250
        const val OFFSET_DEGREES = 5
        const val OFFSET_SCALE_UP = 1.1
        const val OFFSET_SCALE_DOWN = 0.85

        fun convertDpToPixel(dp: Float, context: Context): Int {
            val resources: Resources = context.resources
            val metrics: DisplayMetrics = resources.displayMetrics
            val px = dp * (metrics.densityDpi / 160f)
            return px.toInt()
        }
    }

    fun getStickerOffset() = context?.let {
        convertDpToPixel(
            38f,
            it
        )
    } ?: 0

    fun getWidthHeightBorderView(): Pair<Int, Int> {
        return Pair(imageViewBorder.width, imageViewBorder.height)
    }

    fun clearSelect() {
        isStickerSelected = false
        setGoneBorderAndButton()
    }


    fun setSelect() {
        isStickerSelected = true
    }
}