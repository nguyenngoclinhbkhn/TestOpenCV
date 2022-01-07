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
import android.widget.Toast
import androidx.annotation.Nullable
import jp.co.testopencv.detect.Triangle
import org.opencv.core.Rect

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
        paint?.strokeWidth = 1F
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

    fun addShape(shape: Shape, rect: Rect? = null, ratio: Float = 1F, triangle: Triangle? = null) {
        shapeList.add(shape)
        this.shapeSelected = shape
        shapeSelected?.let { shape1 ->
            val offset = 50F / 2F
            if (!shape1.isTriangle()) {
                rect?.let { rect ->
                    shape1 as RectangleShape
                    addView(shape1.leftTop, 50, 50)
                    addView(shape1.rightTop, 50, 50)
                    addView(shape1.leftBottom, 50, 50)
                    addView(shape1.rightBottom, 50, 50)
//                addView(shape1.editSize, 30, 30)
                    shape1.editSize.tag = shape1.tagShape
                    val leftTopX = rect.x.toFloat() / ratio - offset
                    val leftTopY = rect.y.toFloat() / ratio - offset
                    shape1.leftTop.x = leftTopX
                    shape1.leftTop.y = leftTopY
                    shape1.rightTop.x = leftTopX + (rect.width.toFloat() / ratio)
                    shape1.rightTop.y = leftTopY
                    shape1.rightBottom.x = leftTopX + (rect.width.toFloat() / ratio)
                    shape1.rightBottom.y = leftTopY + (rect.height.toFloat() / ratio)
                    shape1.leftBottom.x = leftTopX
                    shape1.leftBottom.y = leftTopY + (rect.height.toFloat() / ratio)

                    shape1.editSize.x =
                        ((shape1.rightTop.x - shape1.leftTop.x).toFloat() / 2F)
                    shape1.editSize.y =
                        (((shape1.leftBottom.y + shape1.rightBottom.y) / 2F)+ 30)
                }
            } else {
                shape1 as TriangleShape

                triangle?.let { tri ->
                    addView(shape1.top, 50, 50)
                    addView(shape1.leftBottom, 50, 50)
                    addView(shape1.rightBottom, 50, 50)
                    val topX = tri.findTop().x.toFloat() / ratio - offset
                    val topY = tri.findTop().y.toFloat() / ratio - offset
                    shape1.top.x = topX
                    shape1.top.y = topY
                    shape1.rightBottom.x = tri.findRightBottom().x.toFloat() / ratio - offset
                    shape1.rightBottom.y = tri.findRightBottom().y.toFloat() / ratio - offset
                    shape1.leftBottom.x = tri.findLeftBottom().x.toFloat() / ratio - offset
                    shape1.leftBottom.y = tri.findLeftBottom().y.toFloat() / ratio - offset
                }

            }
        }
        initAction()
        invalidate()
    }

    fun addShapeTest(shape: Shape) {
        shapeList.add(shape)
        this.shapeSelected = shape
        shapeSelected?.let { shape1 ->
            val offset = 50F / 2F
            if (!shape1.isTriangle()) {

                shape1 as RectangleShape
                addView(shape1.leftTop, 50, 50)
                addView(shape1.rightTop, 50, 50)
                addView(shape1.leftBottom, 50, 50)
                addView(shape1.rightBottom, 50, 50)
//                addView(shape1.editSize, 30, 30)
                shape1.editSize.tag = shape1.tagShape
                val leftTopX = 734F
                val leftTopY = 364F
                shape1.leftTop.x = leftTopX
                shape1.leftTop.y = leftTopY
                shape1.rightTop.x = 806F
                shape1.rightTop.y = leftTopY
                shape1.rightBottom.x = 906F
                shape1.rightBottom.y = 400F
                shape1.leftBottom.x = 639F
                shape1.leftBottom.y = 400F

                shape1.editSize.x =
                    ((shape1.rightTop.x - shape1.leftTop.x).toFloat() / 2F)
                shape1.editSize.y =
                    (((shape1.leftBottom.y + shape1.rightBottom.y) / 2F) + 30)
            }
        }
        initAction()
        invalidate()
    }

    fun getShapeList() = shapeList

    private fun initAction() {
        shapeSelected?.let { shape ->
            if (!shape.isTriangle()) {
                shape as RectangleShape
                shape?.leftTop?.setOnTouchListener(onTouchCornerShapeListener)
                shape?.rightTop?.setOnTouchListener(onTouchCornerShapeListener)
                shape?.leftBottom?.setOnTouchListener(onTouchCornerShapeListener)
                shape?.rightBottom?.setOnTouchListener(onTouchCornerShapeListener)
                shape?.editSize?.setOnClickListener(onDeleteShapeListener)
            } else {
                shape as TriangleShape
                shape.top.setOnTouchListener(onTouchCornerShapeListener)
                shape.leftBottom.setOnTouchListener(onTouchCornerShapeListener)
                shape.rightBottom.setOnTouchListener(onTouchCornerShapeListener)
            }
        }

    }

    var leftX = 0F
    var rightX = 0F
    var topY = 0F
    fun drawTest(leftX: Float, rightX: Float) {
        this.topY = 404.4F + 25F
        this.leftX = leftX
        this.rightX = rightX
        invalidate()
    }
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        val offset = 25
//        canvas?.drawLine(leftX, topY, rightX, topY, paint!!)
        shapeList.forEach { it ->
            if (!it.isTriangle()) {
                it as RectangleShape
                val path = it.path
                path.reset()
                path.moveTo(it.leftTop.x + offset, it.leftTop.y + offset)
                path.lineTo(it.rightTop.x + offset, it.rightTop.y + offset)
                path.lineTo(it.rightBottom.x + offset, it.rightBottom.y + offset)
                path.lineTo(it.leftBottom.x + offset, it.leftBottom.y + offset)
                path.lineTo(it.leftTop.x + offset, it.leftTop.y + offset)
                paint?.let { it1 -> canvas?.drawPath(path, it1) }
            } else {
                it as TriangleShape
                val path = it.path
                path.reset()
                path.moveTo(it.top.x + offset, it.top.y + offset)
                path.lineTo(it.rightBottom.x + offset, it.rightBottom.y + offset)
                path.lineTo(it.leftBottom.x + offset, it.leftBottom.y + offset)
                path.lineTo(it.top.x + offset, it.top.y + offset)
                paint?.let { it1 -> canvas?.drawPath(path, it1) }
            }
        }
    }

    private var positionX = 0F
    private var positionY = 0F
    private var positionRawX = 0F
    private var positionRawY = 0F

    private val onTouchCornerShapeListener = object : OnTouchListener {
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
    private var time = 0L
    private val onTouchMain = object : OnTouchListener {
        override fun onTouch(v: View?, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    time = System.currentTimeMillis()
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
                        invalidate()
                        downX = xMove
                        downY = yMove
                    }

                }
                MotionEvent.ACTION_UP -> {
                    val timeUp = System.currentTimeMillis()
                    if (timeUp - time < 200) {
//                        Toast.makeText(context, "Click" ,Toast.LENGTH_SHORT).show()
                    }
                    isCanMoveMain = false
                }
            }
            return true
        }
    }

    private val onDeleteShapeListener = object : OnClickListener {
        override fun onClick(v: View?) {
            Log.e("TAG", "View ")
//            val shapeRemove = shapeList.find { it.tagShape == v!!.tag }
//            shapeRemove?.path?.reset()
//            shapeList.remove(shapeRemove)
//            shapeRemove?.let { removeShape(it) }
            invalidate()
        }
    }

    private fun removeShape(rectangleShape: RectangleShape) {
        removeView(rectangleShape.leftTop)
        removeView(rectangleShape.rightTop)
        removeView(rectangleShape.leftBottom)
        removeView(rectangleShape.rightBottom)
        removeView(rectangleShape.editSize)
    }

    private fun isInsideShape(pointerDownX: Float, pointerDowY: Float, shape: Shape?): Boolean {
        return shape?.let {
            if (!it.isTriangle()) {
                it as RectangleShape
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
            } else {
                it as TriangleShape
                val minY = it.top.y
                val minX = it.leftBottom.x
                val maxX = it.leftBottom.y
                val maxY = Math.max(it.leftBottom.y, it.rightBottom.y)
                if (pointerDowY > minY && pointerDowY < maxY) {
                    pointerDownX > minX && pointerDownX < maxX
                } else {
                    false
                }
            }
        } ?: false

    }

    private fun updateShapeSelected(pointerDownX: Float, pointerDowY: Float) {
        shapeList.forEach { it ->
            if (!it.isTriangle()) {
                it as RectangleShape
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
            } else {
                it as TriangleShape
                val maxY = Math.max(it.leftBottom.y, it.rightBottom.y)
                if (pointerDowY > it.top.y && pointerDowY < maxY) {
                    if (pointerDownX > it.leftBottom.x && pointerDownX < (it.rightBottom.x + it.rightBottom.width)) {
                        it.isSelected = true
                        shapeSelected = it
                    }
                }
            }
        }
    }

    private fun updateShape(offsetX: Float, offsetY: Float) {
        shapeSelected?.let { shape ->
            if (!shape.isTriangle()) {
                shape as RectangleShape
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
            } else {
                shape as TriangleShape
                shape.top.x += offsetX
                shape.top.y += offsetY
                shape.leftBottom.x += offsetX
                shape.leftBottom.y += offsetY
                shape.rightBottom.x += offsetX
                shape.rightBottom.y += offsetY
            }
        }
    }

    fun clearAll() {
        shapeSelected = null
        this.shapeList.forEach {
            if (it.isTriangle()) {
                it as TriangleShape
                removeView(it.top)
                removeView(it.leftBottom)
                removeView(it.rightBottom)
            } else {
                it as RectangleShape
                removeView(it.leftTop)
                removeView(it.rightTop)
                removeView(it.leftBottom)
                removeView(it.rightBottom)
            }
        }
        this.shapeList.clear()
        invalidate()
    }
}