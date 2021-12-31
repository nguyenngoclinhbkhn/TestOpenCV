package jp.co.testopencv.learn

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import jp.co.testopencv.R
import kotlinx.android.synthetic.main.activity_learn_opecv.*
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Core.add
import org.opencv.core.Core.subtract
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc




class LearnOpecvActivity : AppCompatActivity() {
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
        setContentView(R.layout.activity_learn_opecv)
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback)
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }
        btnChooseImage?.setOnClickListener {
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
                val bitmap = (BitmapFactory.decodeStream(contentResolver.openInputStream(uri)))
                imgViewOrigin?.setImageBitmap(bitmap)

                val src = Mat()
                Utils.bitmapToMat(bitmap, src)

                val colors = arrayListOf<Mat>()
                val matRgb = Mat()
                Core.split(src, colors)
                val rChannel = colors[0]
                val gChannel = colors[1]
                val bChannel = colors[2]
                val aChannel = colors[3]


//                rChannel.setTo(Scalar(0.0))

//                Core.add(rChannel, Scalar(55.0, 0.0, 255.0, 255.0), src)
                aChannel.setTo(Scalar(250.0, 0.0, 250.0, 250.0))
//                gChannel.setTo(Scalar(55.0, 0.0, 255.0, 255.0))
//                bChannel.setTo(Scalar(55.0, 0.0, 255.0, 255.0))
                Core.merge(colors, matRgb)

                val process =
                    Bitmap.createBitmap(
                        matRgb.cols(),
                        matRgb.rows(),
                        Bitmap.Config.ARGB_8888
                    )
                Utils.matToBitmap(matRgb, process)
                imgViewOpencv.setImageBitmap(process)
            }

        }

    }
}