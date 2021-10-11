package jp.co.testopencv.game

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import org.opencv.android.*
import org.opencv.core.Mat

class GameActivity : AppCompatActivity(), CameraBridgeViewBase.CvCameraViewListener,
    View.OnTouchListener {

    private var mOpenCvCameraView: CameraBridgeViewBase? = null
    private var mPuzzle15: Puzzle15Processor? = null
    private var mItemHideNumbers: MenuItem? = null
    private var mItemStartNewGame: MenuItem? = null


    private var mGameWidth = 0
    private var mGameHeight = 0

    private val mLoaderCallback: BaseLoaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                SUCCESS -> {
                    Log.e(
                        "TAG",
                        "OpenCV loaded successfully"
                    )

                    /* Now enable camera view to start receiving frames */
                    mOpenCvCameraView!!.setOnTouchListener(
                        this@GameActivity
                    )
                    mOpenCvCameraView!!.enableView()
                }
                else -> {
                    super.onManagerConnected(status)
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        mItemHideNumbers = menu!!.add("Show/hide tile numbers")
        mItemStartNewGame = menu.add("Start new game")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item == mItemStartNewGame) {
            /* We need to start new game */
            mPuzzle15!!.prepareNewGame()
        } else if (item == mItemHideNumbers) {
            /* We need to enable or disable drawing of the tile numbers */
            mPuzzle15!!.toggleTileNumbers()
        }
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        Log.e("TAG", "Creating and setting view")
        mOpenCvCameraView = JavaCameraView(this, -1)
        setContentView(mOpenCvCameraView)
        mOpenCvCameraView!!.setCameraIndex(1)
        mOpenCvCameraView!!.visibility = View.VISIBLE
        mOpenCvCameraView!!.setCameraPermissionGranted()
        mOpenCvCameraView!!.setCvCameraViewListener(this)
        mPuzzle15 = Puzzle15Processor()
        mPuzzle15!!.prepareNewGame()
    }

    override fun onResume() {
        super.onResume()
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback)
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }
    }
    
    override fun onCameraViewStarted(width: Int, height: Int) {
        mGameWidth = width
        mGameHeight = height
        mPuzzle15!!.prepareGameSize(width, height)
    }

    override fun onCameraViewStopped() {
        Log.e("TAG", "Camera stopped")
    }

    protected fun getCameraViewList(): List<CameraBridgeViewBase?>? {
        return listOf(mOpenCvCameraView)
    }


    override fun onCameraFrame(inputFrame: Mat?): Mat {
        return mPuzzle15!!.puzzleFrame(inputFrame)
    }

    override fun onTouch(p0: View, p1: MotionEvent): Boolean {

        var xpos: Float = (p0.width - mGameWidth).toFloat() / 2F
        xpos = p1.x - xpos

        var ypos: Float = (p0.height - mGameHeight).toFloat() / 2F
        ypos = p1.y - ypos

        Log.e("TAG", "x ${p0.x} y ${p0.y}")
        if (xpos >= 0 && xpos <= mGameWidth && ypos >= 0 && ypos <= mGameHeight) {
            Log.e("TAG", "Processor")
            /* click is inside the picture. Deliver this event to processor */
            mPuzzle15!!.deliverTouchEvent(xpos.toInt(), ypos.toInt())
        }

        return false
    }
}