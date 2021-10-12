package jp.co.testopencv.measure

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.hardware.Camera
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import jp.co.testopencv.R
import org.opencv.android.*
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.text.DecimalFormat
import java.text.NumberFormat

class MeasureActivity : AppCompatActivity() {

    private var TAG = "Sample::SOP::Activity"
    private var mOpenCvCameraView: CameraBridgeViewBase? = null

    private var detecting = true
    private var saving = false

    // Intent check-varues
    var GET_INPUT_DATA = 1

    // MainActivity parameters
    private var frameskip = 30
    private var frame_i = 0
    private var cmToPxRatio = 1.0
    private var measSide1 = 0.0
    private var measSide2 = 0.0
    private var measRectShortSide = 0.0
    private var measRectLongSide = 0.0
    private var refRectShortSide = 0.0
    private var refRectLongSide = 0.0

    // RefObjDetector parameters
    // variables which are set by slide bar are ints instead of doubles because of poor support
    private var refObjHue = 56
    private var refObjColThreshold = 12
    private var refObjSatMinimum = 120
    private var numberOfDilations = 1
    private var refObjMinContourArea = 500.0
    private var refObjMaxContourArea = 800000.0
    private var refObjSideRatioLimit = 1.45


    // MeasObjDetector variables
    private var measObjBound = 100
    private var measObjMaxBound = 255
    private var measObjMinArea = 10000
    private var measObjMaxArea = 100000

    var c = Camera.open()

    var rotRect: RotatedRect? = RotatedRect()
    var measRect: RotatedRect? = RotatedRect()
    var measDrawRect: List<MatOfPoint> = ArrayList()
    var measRects: ArrayList<RotatedRect>? = null
    var measDrawRects: ArrayList<ArrayList<MatOfPoint>>? = null

    var nF1: NumberFormat = DecimalFormat("#0.0")
    var nF2: NumberFormat = DecimalFormat("#0.00")
    var nF4: NumberFormat = DecimalFormat("#0.0000")

    var textCol = Scalar(10.0, 255.0, 10.0)
    var graphCol = Scalar(255.0, 0.0, 255.0)
    var measCol = Scalar(0.0, 127.0, 255.0)

    var uiFont: Int = Imgproc.FONT_HERSHEY_SIMPLEX
    var uiTextScale = 2
    var uiTextThickness = 2

    var cubeDetector: RefObjDetector? = null
    var measDetector: MeasObjDetector? = null

    var debugMode = false
    var circleOption = false

    var menu_rect: MenuItem? = null
    var menu_circle: MenuItem? = null


    private var mLoaderCallback: BaseLoaderCallback = object : BaseLoaderCallback(this) {
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
        setContentView(R.layout.activity_measure)

        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback)
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }
        measRects = ArrayList<RotatedRect>()
        measDrawRects = ArrayList<ArrayList<MatOfPoint>>()

        cubeDetector = RefObjDetector(
            refObjHue,
            refObjColThreshold,
            refObjSatMinimum,
            numberOfDilations,
            refObjMinContourArea,
            refObjMaxContourArea,
            refObjSideRatioLimit
        )

        measDetector = MeasObjDetector(
            measObjBound,
            measObjMaxBound,
            measObjMinArea,
            measObjMaxArea
        )

        findViewById<Button>(R.id.btnChooseImage).setOnClickListener {
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(Intent.createChooser(intent, "SELECT"), 1)
        }
    }

    private fun openSelectImage() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "SELECT"), 1)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == 1) {
            data?.data?.let { uri ->
                /**
                 * Detecting reference object through RefObjDetector class
                 */
                val bitmapChoose =
                    validateBitmap(BitmapFactory.decodeStream(contentResolver.openInputStream(uri)))
                var inputFrame = Mat(bitmapChoose.width, bitmapChoose.height, CvType.CV_8UC4)
                Utils.bitmapToMat(bitmapChoose, inputFrame)

                cubeDetector!!.ProcessFrame(inputFrame)
                rotRect = cubeDetector!!.rotRect

                refRectShortSide = cubeDetector!!.shortSideLength
                refRectLongSide = cubeDetector!!.longSideLength

                cmToPxRatio = 5.0 / cubeDetector!!.shortSideLength


                if (rotRect != null) {
                    /**
                     * Detecting possible measured objects through MeasObjDetector class
                     */
                    measRects = ArrayList<RotatedRect>()
                    measDrawRects = ArrayList<ArrayList<MatOfPoint>>()
                    measDetector!!.detectMeasurable(inputFrame)
                    var largestMeasArea = 0.0
                    var largestMeasIndex = -1
                    for (i in 0 until measDetector!!.contours.size) {
                        measDetector!!.ptsDistance(i)
                        measRects!!.add(measDetector!!.get_minAreaRect())
                        measDrawRects!!.add(measDetector!!.getDrawContour() as ArrayList<MatOfPoint>)
                        var a = Imgproc.contourArea(measDetector!!.contours[i])
                        if (a > largestMeasArea && !IsRectInContour(
                                rotRect!!,
                                measDetector!!.contours[i]
                            )
                        ) {
                            largestMeasArea = a
                            largestMeasIndex = i
                            measRectShortSide = measDetector!!.minLen
                            measRectLongSide = measDetector!!.maxLen
                        }
                    }
                    /**
                     * Convert measured object dimension from pixels to centimeters
                     */
                    if (largestMeasIndex > -1) {
                        measDrawRect =
                            measDrawRects!!.get(largestMeasIndex)
                        measRect =
                            measRects!!.get(largestMeasIndex)
                        measSide1 =
                            measRectShortSide * cmToPxRatio
                        measSide2 =
                            measRectLongSide * cmToPxRatio
                    }
                }
                val a = OnScreenDrawings(inputFrame)
                a?.let { src ->
                    val process =
                        Bitmap.createBitmap(src.cols(), src.rows(), Bitmap.Config.ARGB_8888)
                    Utils.matToBitmap(src, process)
                    findViewById<ImageView>(R.id.imgView).setImageBitmap(process)
                }


            }
        }

    }

    private fun OnScreenDrawings(inputFrame: Mat): Mat? {
        /**
         * Drawing of miscellaneous info texts to user interface
         *
         */
        // Write calculated centimeters per pixel ratio on-screen
        Imgproc.putText(
            inputFrame,
            "cm/px ratio: " + nF4.format(cmToPxRatio),
            Point(10.0, 60.0),
            uiFont,
            uiTextScale.toDouble(),
            textCol,
            uiTextThickness
        )
        if (debugMode) {
            // Debugging: Write reference rect side ratio on-screen
            Imgproc.putText(
                inputFrame,
                "rectangle side ratio: " + nF2.format(cubeDetector!!.rectSideRatio),
                Point(10.0, 140.0),
                uiFont,
                uiTextScale.toDouble(),
                textCol,
                uiTextThickness
            )
            // Debugging: Write reference rect area on-screen
            Imgproc.putText(
                inputFrame,
                "area: " + nF2.format(cubeDetector!!.rectArea) + "px^2",
                Point(10.0, 180.0),
                uiFont,
                uiTextScale.toDouble(),
                textCol,
                uiTextThickness
            )

            // Debugging: Draw a circle marker and write color info of rotated rectangle center point
            DrawRotRectCenterData(inputFrame)
        }
        /**
         * Draw the reference rectangle on-screen in magenta color
         *
         */
        Imgproc.drawContours(
            inputFrame,
            cubeDetector!!.rotRectCnt,
            -1,
            graphCol,
            2
        ) // Draw rotated rect into image frame
        if (rotRect != null) {
            Imgproc.putText(
                inputFrame,
                "refObject",
                Point(
                    rotRect!!.center.x - 80,
                    rotRect!!.center.y
                ),
                uiFont,
                2.0,
                textCol,
                1
            )
            if (!circleOption) {
                Imgproc.putText(
                    inputFrame,
                    "meas. angle: " + nF1.format(measRect!!.angle),
                    Point(10.0, 100.0),
                    uiFont,
                    uiTextScale.toDouble(),
                    textCol,
                    uiTextThickness
                )
            }
            if (debugMode) {
                // Debugging:
                Imgproc.putText(
                    inputFrame,
                    "ref angle: " + nF1.format(rotRect!!.angle),
                    Point(10.0, 220.0),
                    uiFont,
                    uiTextScale.toDouble(),
                    textCol,
                    uiTextThickness
                )
            }
        }
        /**
         * Drawing the measured rectangle of circle and informative texts about measured dimensions
         *
         */
        if (circleOption) {
            val circleTextPos1 = Point(
                measRect!!.center.x,
                measRect!!.center.y + (measRect!!.size.height / 2) as Int + 25 * uiTextScale
            )
            val circleTextPos2 = Point(
                measRect!!.center.x,
                measRect!!.center.y + (measRect!!.size.height / 2) as Int + 40 * uiTextScale
            )
            val radius =
                Math.sqrt(measRectLongSide * measRectShortSide) / 2
            val radius_cm: Double = radius * cmToPxRatio
            val area_cm = 3.14 * (radius_cm * radius_cm)
            Imgproc.circle(
                inputFrame, measRect!!.center,
                radius.toInt(), measCol, 1
            )
            Imgproc.putText(
                inputFrame,
                "radius: " + nF1.format(radius_cm) + "cm",
                circleTextPos1,
                uiFont,
                uiTextScale.toDouble(),
                textCol,
                1
            )
            Imgproc.putText(
                inputFrame,
                "area: " + nF1.format(area_cm) + "cm^2",
                circleTextPos2,
                uiFont,
                uiTextScale.toDouble(),
                textCol,
                1
            )
        } else {
            val rectTextPos1 = Point(
                measRect!!.center.x,
                measRect!!.center.y - 10 * uiTextScale
            )
            val rectTextPos2 = Point(
                measRect!!.center.x,
                measRect!!.center.y + 10 * uiTextScale
            )

            // Draw the measured rectangle on-screen in magenta color
            Imgproc.drawContours(
                inputFrame,
                measDrawRect,
                -1,
                measCol,
                2
            )

            // Write dimensions of measured object on-screen
            Imgproc.putText(
                inputFrame,
                "side 1: " + nF1.format(measSide1) + " cm",
                rectTextPos1,
                uiFont,
                uiTextScale.toDouble(),
                textCol,
                1
            )
            Imgproc.putText(
                inputFrame,
                "side 2: " + nF1.format(measSide2) + " cm",
                rectTextPos2,
                uiFont,
                uiTextScale.toDouble(),
                textCol,
                1
            )
        }
        return inputFrame
    }

    private fun DrawRotRectCenterData(frame_in: Mat) {
        val rectCenterCols = cubeDetector!!.rectCenterCols
        // Workaround for app crash in a case where couldn't acquire color values of rect center
        if (rectCenterCols != null) {
            Imgproc.putText(
                frame_in,
                "H: " + rectCenterCols[0].toString(),
                Point(900.0, 40.0),
                uiFont,
                uiTextScale.toDouble(),
                textCol,
                uiTextThickness
            )
            Imgproc.putText(
                frame_in,
                "S: " + rectCenterCols[1].toString(),
                Point(900.0, 80.0),
                uiFont,
                uiTextScale.toDouble(),
                textCol,
                uiTextThickness
            )
            Imgproc.putText(
                frame_in,
                "V: " + rectCenterCols[2].toString(),
                Point(900.0, 120.0),
                uiFont,
                uiTextScale.toDouble(),
                textCol,
                uiTextThickness
            )
            Imgproc.circle(
                frame_in,
                rotRect!!.center,
                10,
                graphCol,
                3
            )
        }
    }

    fun IsRectInContour(rect: RotatedRect, cnt: MatOfPoint): Boolean {
        val cnt2 = MatOfPoint2f(*cnt.toArray())
        for (i in 0..3) {
            if (Imgproc.pointPolygonTest(cnt2, rect.center, false) > 0) {
                Log.e("TAG", "REF INSIDE MEAS")
                return true
            }
        }
        return false
    }

    private fun validateBitmap(bitmap: Bitmap): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(90F)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}