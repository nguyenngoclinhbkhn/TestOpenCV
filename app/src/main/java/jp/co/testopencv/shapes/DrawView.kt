package jp.co.testopencv.shapes

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.Nullable

/**
 * COPYRIGHT ZYYX. ALL RIGHTS RESERVED,2020
 */
class DrawView : FrameLayout {
    private var paint: Paint? = null
    private var shapeSelected: Shape? = null
    private var statusBarHeight = 0.0
    private var shapeList = mutableListOf<Shape>()

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, @Nullable attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context, @Nullable attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init()
    }

    private fun init() {
        paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint?.strokeWidth = 5F
        paint?.strokeJoin = Paint.Join.ROUND
        paint?.style = Paint.Style.STROKE
        paint?.strokeCap = Paint.Cap.ROUND
        paint?.isDither = true
        paint?.color = Color.RED
        var result = 0
        val resourceId =
            resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId)
        }
        statusBarHeight = result.toDouble()
        this.setOnTouchListener(onTouchMain)
        this.setWillNotDraw(false)
    }

    fun addShape(shape: Shape) {
        shapeList.add(shape)
        this.shapeSelected = shape
        shapeSelected?.let { shape1 ->
            if (!shape1.isTriangle) {
                addView(shape1.leftTop, 60, 60)
                addView(shape1.rightTop, 60, 60)
                addView(shape1.leftBottom, 60, 60)
                addView(shape1.rightBottom, 60, 60)
                addView(shape1.editSize, 60, 60)
                shapeSelected?.editSize?.tag = shape1.tagShape
                shapeSelected?.rightTop?.x = shape1.leftTop.x + shape1.size.width.toFloat()
                shapeSelected?.rightBottom?.y = shape1.rightTop.y + shape1.size.height.toFloat()
                shapeSelected?.rightBottom?.x = shape1.leftTop.x + shape1.size.width.toFloat()
                shapeSelected?.leftBottom?.y = shape1.leftTop.y + shape1.size.height.toFloat()

                shapeSelected?.editSize?.x =
                    ((shape1.rightTop.x - shape1.leftTop.x).toFloat() / 2F)
                shapeSelected?.editSize?.y =
                    (((shape1.leftBottom.y + shape1.rightBottom.y) / 2F)+ 30)
            } else {

            }
        }
        initAction()
        invalidate()
    }

    fun getShapeList() = shapeList

    private fun initAction() {
        shapeSelected?.leftTop?.setOnTouchListener(onTouchTopLeft)
        shapeSelected?.rightTop?.setOnTouchListener(onTouchTopRight)
        shapeSelected?.leftBottom?.setOnTouchListener(onTouchBottomLeft)
        shapeSelected?.rightBottom?.setOnTouchListener(onTouchBottomRight)
        shapeSelected?.editSize?.setOnClickListener(onDeleteShapeListener)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        val offset = 30
        shapeList.forEach { it ->
            val path = it.path
            path.reset()
            path.moveTo(it.leftTop.x + offset, it.leftTop.y + offset)
            path.lineTo(it.rightTop.x + offset, it.rightTop.y + offset)
            path.lineTo(it.rightBottom.x + offset, it.rightBottom.y + offset)
            path.lineTo(it.leftBottom.x + offset, it.leftBottom.y + offset)
            path.lineTo(it.leftTop.x + offset, it.leftTop.y + offset)
            paint?.let { it1 -> canvas?.drawPath(path, it1) }
        }
    }

    private var positionX = 0F
    private var positionY = 0F
    private var positionRawX = 0F
    private var positionRawY = 0F
    private val onTouchBottomRight = object : OnTouchListener {
        override fun onTouch(v: View?, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    positionX = v?.x ?: 0F
                    positionY = v?.y ?: 0F
                    positionRawX = event.rawX
                    positionRawY = event.rawY - statusBarHeight.toFloat()
                }
                MotionEvent.ACTION_MOVE -> {
                    val rawX = event.rawX
                    val rawY = event.rawY - statusBarHeight.toFloat()
                    val offsetRawX = rawX - positionRawX
                    val offsetRawY = rawY - positionRawY
                    val coordinateX = v?.x ?: 0F
                    val coordinateY = v?.y ?: 0F
                    v?.x = coordinateX + offsetRawX
                    v?.y = coordinateY + offsetRawY
                    invalidate()
                    positionRawX = rawX
                    positionRawY = rawY
                }
                MotionEvent.ACTION_UP -> {
                }
            }
            return true
        }
    }

    private val onTouchTopRight = object : OnTouchListener {
        override fun onTouch(v: View?, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    positionX = v?.x ?: 0F
                    positionY = v?.y ?: 0F
                    positionRawX = event.rawX
                    positionRawY = event.rawY - statusBarHeight.toFloat()
                }
                MotionEvent.ACTION_MOVE -> {
                    val rawX = event.rawX
                    val rawY = event.rawY - statusBarHeight.toFloat()
                    val offsetRawX = rawX - positionRawX
                    val offsetRawY = rawY - positionRawY
                    val coordinateX = v?.x ?: 0F
                    val coordinateY = v?.y ?: 0F
                    v?.x = coordinateX + offsetRawX
                    v?.y = coordinateY + offsetRawY
                    invalidate()
                    positionRawX = rawX
                    positionRawY = rawY
                }
                MotionEvent.ACTION_UP -> {
                }
            }
            return true
        }
    }

    private val onTouchTopLeft = object : OnTouchListener {
        override fun onTouch(v: View?, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    positionX = v?.x ?: 0F
                    positionY = v?.y ?: 0F
                    positionRawX = event.rawX
                    positionRawY = event.rawY - statusBarHeight.toFloat()
                }
                MotionEvent.ACTION_MOVE -> {
                    val rawX = event.rawX
                    val rawY = event.rawY - statusBarHeight.toFloat()
                    val offsetRawX = rawX - positionRawX
                    val offsetRawY = rawY - positionRawY
                    val coordinateX = v?.x ?: 0F
                    val coordinateY = v?.y ?: 0F
                    v?.x = coordinateX + offsetRawX
                    v?.y = coordinateY + offsetRawY
                    invalidate()
                    positionRawX = rawX
                    positionRawY = rawY
                }
                MotionEvent.ACTION_UP -> {
                }
            }
            return true
        }
    }

    private val onTouchBottomLeft = object : OnTouchListener {
        override fun onTouch(v: View?, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    positionX = v?.x ?: 0F
                    positionY = v?.y ?: 0F
                    positionRawX = event.rawX
                    positionRawY = event.rawY - statusBarHeight.toFloat()
                }
                MotionEvent.ACTION_MOVE -> {
                    val rawX = event.rawX
                    val rawY = event.rawY - statusBarHeight.toFloat()
                    val offsetRawX = rawX - positionRawX
                    val offsetRawY = rawY - positionRawY
                    val coordinateX = v?.x ?: 0F
                    val coordinateY = v?.y ?: 0F
                    v?.x = coordinateX + offsetRawX
                    v?.y = coordinateY + offsetRawY
                    invalidate()
                    positionRawX = rawX
                    positionRawY = rawY
                }
                MotionEvent.ACTION_UP -> {
                }
            }
            return true
        }
    }

    private var downX = 0F
    private var downY = 0F
    private var isCanMoveMain = false
    private val onTouchMain = object : OnTouchListener {
        override fun onTouch(v: View?, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY - statusBarHeight.toFloat()
                    updateShapeSelected(downX, downY)
                    isCanMoveMain = isInsideShape(downX, downY, shapeSelected)

                }
                MotionEvent.ACTION_MOVE -> {
                    if (isCanMoveMain) {
                        val xMove = event.rawX
                        val yMove = event.rawY - statusBarHeight.toFloat()
                        val offsetMoveX = xMove - downX
                        val offsetMoveY = yMove - downY
                        updateShape(offsetMoveX, offsetMoveY)
                        Log.e("TAG", "x top ${shapeSelected?.leftTop?.x}")
                        invalidate()
                        downX = xMove
                        downY = yMove
                    }

                }
                MotionEvent.ACTION_UP -> {
                    isCanMoveMain = false
                }
            }
            return true
        }
    }

    private val onDeleteShapeListener = object : OnClickListener {
        override fun onClick(v: View?) {
            Log.e("TAG", "View ")
            val shapeRemove = shapeList.find { it.tagShape == v!!.tag }
            shapeRemove?.path?.reset()
            shapeList.remove(shapeRemove)
            shapeRemove?.let { removeShape(it) }
            invalidate()
        }
    }

    private fun removeShape(shape: Shape) {
        removeView(shape.leftTop)
        removeView(shape.rightTop)
        removeView(shape.leftBottom)
        removeView(shape.rightBottom)
        removeView(shape.editSize)
    }

    private fun isInsideShape(pointerDownX: Float, pointerDowY: Float, shape: Shape?): Boolean {
        return shape?.let {
            val minStartX = Math.min(it.leftTop.x, it.leftBottom.x)
            val minStartY = Math.min(it.leftTop.y, it.rightTop.y)
            val maxStartX = Math.max(it.rightTop.x, it.rightBottom.x)
            val maxStartY = Math.max(it.rightTop.y, it.rightBottom.y)
            if (pointerDownX > minStartX && pointerDowY > minStartY) {
                if (pointerDownX < maxStartX && pointerDowY > minStartY) {
                    if (pointerDownX > minStartX && pointerDowY < maxStartY) {
                        pointerDownX < maxStartX && pointerDowY < maxStartY
                    } else {
                        false
                    }
                } else {
                    false
                }
            } else {
                false
            }
        } ?: false
    }

    private fun updateShapeSelected(pointerDownX: Float, pointerDowY: Float) {
        shapeList.forEach { it ->
            if (pointerDownX > it.leftTop.x && pointerDowY > it.leftTop.y) {
                if (pointerDownX < it.rightTop.x && pointerDowY > it.rightTop.y) {
                    if (pointerDownX > it.leftBottom.x && pointerDowY < it.leftBottom.y) {
                        if (pointerDownX < it.rightBottom.x && pointerDowY < it.rightBottom.y) {
                            it.isSelected = true
                            shapeSelected = it
                        }
                    }
                }
            }
        }
    }

    private fun updateShape(offsetX: Float, offsetY: Float) {
        shapeSelected?.let { shape ->
            shape.leftTop.x += offsetX
            shape.leftTop.y += offsetY
            shape.rightTop.x += offsetX
            shape.rightTop.y += offsetY
            shape.leftBottom.x += offsetX
            shape.leftBottom.y += offsetY
            shape.rightBottom.x += offsetX
            shape.rightBottom.y += offsetY
            shape.editSize.x += offsetX
            shape.editSize.y += offsetY
        }
    }
}