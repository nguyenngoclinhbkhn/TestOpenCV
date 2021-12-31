package jp.co.testopencv.shapes

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import jp.co.testopencv.R
import kotlinx.android.synthetic.main.activity_custom_shape.*
import org.opencv.core.Size

class CustomShapeActivity : AppCompatActivity() {
    lateinit var drawView: DrawView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom_shape)
        drawView = findViewById(R.id.frameDraw)

        val imageLeftTop = ImageView(this)
        val imageRightTop = ImageView(this)
        val imageLeftBottom = ImageView(this)
        val imageRightBottom = ImageView(this)
        val deleteButton = ImageView(this)
        imageLeftTop.setImageResource(R.drawable.circle)
        imageRightTop.setImageResource(R.drawable.circle)
        imageLeftBottom.setImageResource(R.drawable.circle)
        imageRightBottom.setImageResource(R.drawable.circle)
        deleteButton.setImageResource(R.drawable.circle)
        val shape = Shape(
            leftTop = imageLeftTop,
            rightTop = imageRightTop,
            leftBottom = imageLeftBottom,
            rightBottom = imageRightBottom,
            editSize = deleteButton,
            tagShape = drawView.getShapeList().size.toString()
        )
        drawView.addShape(shape)


        btnAddShape?.setOnClickListener {
            val imageLeftTop1 = ImageView(this)
            val imageRightTop1 = ImageView(this)
            val imageLeftBottom1 = ImageView(this)
            val imageRightBottom1 = ImageView(this)
            val deleteButton1 = ImageView(this)
            imageLeftTop1.setImageResource(R.drawable.circle)
            imageRightTop1.setImageResource(R.drawable.circle)
            imageLeftBottom1.setImageResource(R.drawable.circle)
            imageRightBottom1.setImageResource(R.drawable.circle)
            deleteButton1.setImageResource(R.drawable.circle)
            val shape1 = Shape(
                leftTop = imageLeftTop1,
                rightTop = imageRightTop1,
                leftBottom = imageLeftBottom1,
                rightBottom = imageRightBottom1,
                editSize = deleteButton1,
                tagShape = drawView.getShapeList().size.toString()
            )
            drawView.addShape(shape1)
        }
    }
}