package jp.co.testopencv.detect

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import io.reactivex.Single
import io.reactivex.SingleOnSubscribe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import jp.co.testopencv.R
import jp.co.testopencv.shapes.Shape
import kotlinx.android.synthetic.main.activity_detect.*
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc


class DetectActivity : AppCompatActivity(), SeekBar.OnSeekBarChangeListener {
    private var sizeRef = 2 //cm
    private val triangleList = mutableListOf<Triangle>()
    private val trapezoidList = mutableListOf<Trapezoid>()
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
            isDraw = false
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(Intent.createChooser(intent, "SELECT"), 1)
        }


        btnCalculate?.setOnClickListener {
            addShape()
        }


        seekBar1?.setOnSeekBarChangeListener(this)
        seekBar2?.setOnSeekBarChangeListener(this)


    }

    private var offset = 10.0
    private var bitmap: Bitmap? = null
    private var threshold1 = 150.0
    private var threshold2 = 200.0
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == 1) {
            val uri = data?.data
            uri?.let { uri ->
                calDisplay(uri)
                bitmap = validateBitmap(BitmapFactory.decodeStream(contentResolver.openInputStream(uri)))
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

//            getBitmapTest(frameDilate)
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

    //increase edge
    fun improve(bitmap: Bitmap) {
        val src = Mat(bitmap.width, bitmap.height, CvType.CV_8UC4)
        val grayMat = Mat(bitmap.width, bitmap.height, CvType.CV_8UC4)
        val thresh = Mat()
        val dilate = Mat()
        Utils.bitmapToMat(bitmap, src)
        //First gray image
        Imgproc.cvtColor(src, grayMat, Imgproc.COLOR_BGR2GRAY)
        Imgproc.threshold(grayMat, thresh, 180.0, 255.0, Imgproc.THRESH_BINARY)
        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(5.0, 5.0))
        Imgproc.morphologyEx(thresh, dilate, Imgproc.MORPH_DILATE, kernel)


        imgView?.setImageBitmap(getBitmapTest(dilate))

    }

    private fun getBitmapTest(mat: Mat): Bitmap {
        val process =
            Bitmap.createBitmap(
                mat.cols(),
                mat.rows(),
                Bitmap.Config.ARGB_8888
            )
        Utils.matToBitmap(mat, process)
        return process
    }

    private var listPoint = mutableListOf<Point>()
    val rects = mutableListOf<Rect>()

    private var isDraw = false

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
            Imgproc.CHAIN_APPROX_NONE
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
            if (area > 3000) {
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
                Imgproc.approxPolyDP(matOfPoint2F, approx, 0.02 * peri, true)
                val rect = Imgproc.boundingRect(approx)
                // draw rect bound
                rects.add(rect)
                if(approx.total().toInt() == 3) {
                    Imgproc.rectangle(
                        matContour,
                        Point(rect.x.toDouble(), rect.y.toDouble()),
                        Point(
                            rect.x.toDouble() + rect.width.toDouble(),
                            rect.y.toDouble() + rect.height.toDouble()
                        ),
                        Scalar(0.0, 255.0, 0.0, 255.0),
                        2
                    )
                }
            }
        }
    }

    private fun validateBitmap(bitmap: Bitmap): Bitmap {
        val matrix = Matrix()
//        matrix.postRotate(90F)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {

    }

    override fun onStartTrackingTouch(p0: SeekBar?) {

    }

    private var dialog: ProgressDialog? = null
    private fun showDialog() {
        if (dialog == null) {
            dialog = ProgressDialog(this)
        }
        dialog?.show()
    }

    private fun dissmisDialog() {
        dialog?.dismiss()
        dialog = null
    }

    override fun onStopTrackingTouch(p0: SeekBar?) {
        val threshold1 = seekBar1?.progress?.toDouble() ?: 0.0
        val threshold2 = seekBar2?.progress?.toDouble() ?: 0.0
//        showDialog()
        Single.create(SingleOnSubscribe<Bitmap> { emitter ->
            val image = opencvValidateImage(
                bitmap,
                threshold1, threshold2
            )
            image?.let { emitter.onSuccess(it) }
        }).subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ image ->
//                dissmisDialog()
                imgView?.setImageBitmap(image)
            }, { error ->

            })

//        val bitmap =
//        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
//        paint.strokeJoin = Paint.Join.ROUND
//        paint.strokeCap = Paint.Cap.ROUND // set the paint cap to round too
//        paint.style = Paint.Style.STROKE
//        paint.strokeWidth = 2F
//        paint.color = Color.RED
//        val canvas = Canvas(bitmap!!)
////        triangleList.forEach {
////            val path = Path()
////            path.moveTo(it.top.x.toFloat(), it.top.y.toFloat())
////            path.lineTo(it.left.x.toFloat(), it.left.y.toFloat())
////            path.lineTo(it.right.x.toFloat(), it.right.y.toFloat())
////            path.lineTo(it.top.x.toFloat(), it.top.y.toFloat())
////            canvas.drawPath(path, paint)
//////            path.reset()
////        }
//
//        //draw trap
//        trapezoidList.forEach {
//            val path = Path()
//            path.moveTo(it.left!!.x.toFloat(), it.left!!.y.toFloat())
//            path.lineTo(it.right!!.x.toFloat(), it.right!!.y.toFloat())
//            path.lineTo(it.bottomRight!!.x.toFloat(), it.bottomRight!!.y.toFloat())
//            path.lineTo(it.bottomLeft!!.x.toFloat(), it.bottomLeft!!.y.toFloat())
//            path.lineTo(it.left!!.x.toFloat(), it.left!!.y.toFloat())
//
//            canvas.drawPath(path, paint)
//        }
//        imgView?.setImageBitmap(
//            bitmap
//        )
    }

    fun calDisplay(uri: Uri) {
        val sizeBitmap = getSizeBitmapFromUri(this, uri)
        val rotation = getRotationOfBitmapByUri(this, uri)
        var width = sizeBitmap.first
        var height = sizeBitmap.second
        if (rotation == 90 || rotation == 270) {
            width = sizeBitmap.second
            height = sizeBitmap.first
        }

        val widthDisplay = frameDraw.width
        val heightDisplay = frameDraw.height
        var widthCal = 0
        var heightCal = 0
        if (height.toFloat() * widthDisplay.toFloat() / width.toFloat() <= heightDisplay.toFloat()) {
            widthCal = widthDisplay
            heightCal = (height.toFloat() * widthDisplay.toFloat() / width.toFloat()).toInt()
        } else {
            heightCal = heightDisplay
            widthCal = (width.toFloat() * heightDisplay.toFloat() / height.toFloat()).toInt()
        }
        frameDraw.layoutParams.width = widthCal
        frameDraw.layoutParams.height = heightCal
        frameDraw.requestLayout()
    }

    fun addShape() {
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

    fun getRotationOfBitmapByUri(context: Context, uri: Uri): Int {
        val exif = context.contentResolver.openInputStream(uri)?.let { ExifInterface(it) }
        val rotation = exif?.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        return rotation?.let { exifToDegrees(it) } ?: 0
    }

    fun getSizeBitmapFromUri(context: Context, uri: Uri): Pair<Int, Int> {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        context.contentResolver.openInputStream(uri)?.let { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }
        return options.outWidth to options.outHeight
    }

    private fun exifToDegrees(exifOrientation: Int): Int {
        return when (exifOrientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> {
                90
            }
            ExifInterface.ORIENTATION_ROTATE_180 -> {
                180
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> {
                270
            }
            else -> 0
        }
    }
}