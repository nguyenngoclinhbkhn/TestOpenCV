package jp.co.testopencv.detect

import android.content.Intent
import android.graphics.*
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import jp.co.testopencv.R
import kotlinx.android.synthetic.main.activity_detect.*
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.imgproc.Imgproc


class DetectActivity : AppCompatActivity(), SeekBar.OnSeekBarChangeListener {
    private var sizeRef = 2 //cm

    private val mLoaderCallback: BaseLoaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                SUCCESS -> {
                    Log.e(
                        "TAG",
                        "OpenCV loaded successfully"
                    )
                }
                else -> {
                    super.onManagerConnected(status)
                }
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detect)
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback)
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }
        findViewById<Button>(R.id.btnChooseImage).setOnClickListener {
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(Intent.createChooser(intent, "SELECT"), 1)
        }

        seekBar1?.setOnSeekBarChangeListener(this)
        seekBar2?.setOnSeekBarChangeListener(this)


    }

    private var bitmap: Bitmap? = null
    private var threshold1 = 255.0
    private var threshold2 = 255.0
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == 1) {
            val uri = data?.data
            uri?.let { uri ->
                bitmap =
                    validateBitmap(BitmapFactory.decodeStream(contentResolver.openInputStream(uri)))

                opencvValidateImage(bitmap, threshold1, threshold2)?.let {
                    imgView.setImageBitmap(it)
                    seekBar1.progress = threshold1.toInt()
                    seekBar2.progress = threshold2.toInt()
                }
            }

        }

    }

    private fun opencvValidateImage(
        bitmap: Bitmap?,
        threshold1: Double,
        threshold2: Double
    ): Bitmap? {
        return bitmap?.let { bitmap ->
            // create Mat to use in opencv
            val src = Mat(bitmap.width, bitmap.height, CvType.CV_8UC4)
            val frameCanny = Mat()
            val frameGray = Mat()
            val frameBlur = Mat()
            val frameDilate = Mat()
            val frameContour = Mat()
            // map bitmap to Mat in opencv
            Utils.bitmapToMat(bitmap, src)
            // Copy src to frameContour
            src.copyTo(frameContour)
            //First blur image
            Imgproc.GaussianBlur(src, frameBlur, Size(7.0, 7.0), 1.0)

            //second gray image
            Imgproc.cvtColor(frameBlur, frameGray, Imgproc.COLOR_RGBA2GRAY)

            //third canny image
            Imgproc.Canny(frameGray, frameCanny, threshold1, threshold2)

            //fourth dilate image
            Imgproc.dilate(
                frameCanny,
                frameDilate,
                Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(5.0, 5.0))
            )

            // draw contour to image original
            getContours(frameDilate, frameContour)

            val process =
                Bitmap.createBitmap(
                    frameContour.cols(),
                    frameContour.rows(),
                    Bitmap.Config.ARGB_8888
                )
            Utils.matToBitmap(frameContour, process)
            process
        }

    }

    private var listPoint = mutableListOf<Point>()
    val rects = mutableListOf<Rect>()

    //đầu vào mat là mat là dilate, matContour là src
    private fun getContours(mat: Mat, matContour: Mat) {
        //create list mat
        val contoursList = mutableListOf<MatOfPoint>()

        val hierarchy = Mat()

        //find list contour and add to contoursList
        Imgproc.findContours(
            mat,
            contoursList,
            hierarchy,
            Imgproc.RETR_TREE,
            Imgproc.CHAIN_APPROX_SIMPLE
        )

//        //Loop contoursList
//        Imgproc.drawContours(
//            matContour,
//            contoursList,
//            -1,
//            Scalar(255.0, 0.0, 255.0, 255.0),
//            7
//        )
        rects.clear()
        contoursList.forEach { matOfPoint ->
            val list = mutableListOf<MatOfPoint>()
            list.add(matOfPoint)

            val area = Imgproc.contourArea(matOfPoint)
            if (area > 1000) {
//                Imgproc.drawContours(
//                    matContour,
//                    list,
//                    -1,
//                    Scalar(255.0, 0.0, 255.0, 255.0),
//                    7
//                )
                val matOfPoint2F = MatOfPoint2f()
                matOfPoint.convertTo(matOfPoint2F, CvType.CV_32F)
                val peri = Imgproc.arcLength(matOfPoint2F, true)
                val approx = MatOfPoint2f()
                //apply matOfPoint2F to approx
                Imgproc.approxPolyDP(matOfPoint2F, approx, 0.01 * peri, true)
                val rect = Imgproc.boundingRect(approx)
                // draw rect bound
                rects.add(rect)
//                Imgproc.rectangle(
//                    matContour,
//                    Point(rect.x.toDouble(), rect.y.toDouble()),
//                    Point(
//                        rect.x.toDouble() + rect.width.toDouble(),
//                        rect.y.toDouble() + rect.height.toDouble()
//                    ),
//                    Scalar(0.0, 255.0, 0.0, 255.0),
//                    2
//                )
            }
        }
    }

    private fun validateBitmap(bitmap: Bitmap): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(90F)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {

    }

    override fun onStartTrackingTouch(p0: SeekBar?) {

    }

    override fun onStopTrackingTouch(p0: SeekBar?) {

        val threshold1 = seekBar1?.progress?.toDouble() ?: 0.0
        val threshold2 = seekBar2?.progress?.toDouble() ?: 0.0
        val bitmap = opencvValidateImage(
            bitmap,
            threshold1, threshold2
        )
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeCap = Paint.Cap.ROUND // set the paint cap to round too
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 10F
        paint.color = Color.BLUE
        val canvas = Canvas(bitmap!!)

        //test draw rect
//        rects.forEach {
//            canvas.drawRect(android.graphics.Rect(
//                it.x.toInt(),
//                it.y.toInt(),
//                it.x.toInt() + it.width.toInt(),
//                it.y + it.height
//            ), paint)
//        }

        val ref = rects.firstOrNull{ Math.abs(it.width - it.height) < 80}
        ref?.let {
            val width = it.width
            val scale = sizeRef.toDouble() / width
            canvas.drawRect(android.graphics.Rect(
                it.x.toInt(),
                it.y.toInt(),
                it.x.toInt() + it.width.toInt(),
                it.y + it.height
            ), paint)
            val rectMax = rects.maxByOrNull { it.width }
            rectMax?.let {rectO ->
                val rectObj = android.graphics.Rect(
                    rectO.x.toInt(),
                    rectO.y.toInt(),
                    rectO.x.toInt() + rectO.width.toInt(),
                    rectO.y + rectO.height
                )
                val sizeObj = rectObj.width() * scale
                val text = "${sizeObj.toInt()} cm"
                canvas.drawLine(
                    rectObj.left.toFloat(), rectObj.bottom.toFloat(),
                    rectObj.right.toFloat(), rectObj.bottom.toFloat(), paint
                )
                paint.textSize = 150F
                canvas.drawText(text, (rectObj.right - rectObj.left).toFloat() / 2F,
                rectObj.bottom.toFloat() + 200, paint)
            }
        }
        imgView?.setImageBitmap(
            bitmap
        )
    }
}