package jp.co.testopencv.detect

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import jp.co.testopencv.R
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

class DetectActivity : AppCompatActivity() {

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

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == 1) {
            val uri = data?.data
            uri?.let { uri ->
                val bitmapChoose =
                    validateBitmap(BitmapFactory.decodeStream(contentResolver.openInputStream(uri)))
                val src = Mat(bitmapChoose.width, bitmapChoose.height, CvType.CV_8UC4)
                Utils.bitmapToMat(bitmapChoose, src)
                Imgproc.cvtColor(src, src, Imgproc.COLOR_RGBA2GRAY)
                Imgproc.Canny(src, src, 100.0, 80.0)
                val process = Bitmap.createBitmap(src.cols(), src.rows(), Bitmap.Config.ARGB_8888)
                Utils.matToBitmap(src, process)
                findViewById<ImageView>(R.id.imgView).setImageBitmap(process)
            }

        }

    }

    private fun validateBitmap(bitmap: Bitmap): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(90F)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}