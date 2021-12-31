package jp.co.testopencv.camera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.ImageFormat
import android.graphics.Point
import android.graphics.SurfaceTexture
import android.hardware.*
import android.hardware.camera2.*
import android.hardware.camera2.params.StreamConfigurationMap
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.*
import android.view.*
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import jp.co.testopencv.R
import jp.co.testopencv.shapes.CustomShapeActivity
import kotlinx.android.synthetic.main.activity_camera.*
import org.opencv.android.*
import java.util.*
import kotlin.collections.ArrayList


class CameraActivity : AppCompatActivity(), SensorEventListener {
    var characteristics: CameraCharacteristics? = null
    private val MAX_PREVIEW_HEIGHT = 1080


    private val ORIENTATIONS = SparseArray<Int>()

    //    private AutoFitTextureView mTextureView;

    private val cameraInfo: Camera.CameraInfo? = null




    private var captureRequestBuilder: CaptureRequest.Builder? = null
    private var cameraDevice: CameraDevice? = null
    private var cameraID: String? = null
    private val backgroundThread: HandlerThread? = null
    private val backgroundHandler: Handler? = null
    private lateinit var previewSize: Size
    private lateinit var arrayCamera: Array<String>
    private var isMeteringAreaAFSupported = false

    private val cameraStateCallback: CameraDevice.StateCallback =
        object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                cameraDevice = camera
                startPreview()
            }

            override fun onDisconnected(camera: CameraDevice) {
                camera.close()
                cameraDevice = null
            }

            override fun onError(camera: CameraDevice, error: Int) {
                camera.close()
                cameraDevice = null
            }
        }
    private val MAX_PREVIEW_WIDTH = 1920

    private lateinit var largest: Size
    private var swapDimension = false

    private fun setupCamera(with: Int, height: Int) {
        val cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        try {
            for (cameraId in cameraManager.cameraIdList) {
                try {
                    characteristics = cameraManager.getCameraCharacteristics(cameraId)
                } catch (e: Exception) {
                    throw RuntimeException("TEST CAMERA ID " + cameraId + "characteristics " + characteristics)
                }
                if (characteristics!!.get(CameraCharacteristics.LENS_FACING) ==
                    CameraCharacteristics.LENS_FACING_FRONT
                ) {
                    continue
                }
                //                sensorArraySize = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
                val maxAFRegions =
                    characteristics!!.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF)
                if (maxAFRegions != null) {
                    isMeteringAreaAFSupported = maxAFRegions >= 1
                }
                cameraID = cameraId
                val range: Range<Int>? =
                    characteristics!![CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE]


//                captureSession = cameraDevice.createca
                val map: StreamConfigurationMap? =
                    characteristics!![CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]
                val deviceOrientation = windowManager.defaultDisplay.rotation
                //                int totalRotation = sensorDeviceRotation(characteristics, deviceOrientation);
                val totalRotation =
                    characteristics!!.get(CameraCharacteristics.SENSOR_ORIENTATION)!!
                largest = Collections.max(
                    map?.getOutputSizes(ImageFormat.JPEG)!!.toMutableList(),
                    CompareSizesByArea()
                )
                when (deviceOrientation) {
                    Surface.ROTATION_0, Surface.ROTATION_180 -> if (totalRotation == 90 || totalRotation == 270) {
                        swapDimension = true
                    }
                    Surface.ROTATION_90, Surface.ROTATION_270 -> if (totalRotation == 0 || totalRotation == 180) {
                        swapDimension = true
                    }
                    else -> {
                    }
                }
                val swapRotation = totalRotation == 90 || totalRotation == 270
                var rotatedWidth = with
                var rotatedHeight = height
                val displaySize = Point()
                var maxPreviewWidth: Int = getSize().widthPixels
                var maxPreviewHeight: Int = getSize().heightPixels
                if (swapRotation) {
                    rotatedWidth = height
                    rotatedHeight = with
                    maxPreviewWidth = getSize().heightPixels
                    maxPreviewHeight = getSize().widthPixels
                }
                if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                    maxPreviewWidth = MAX_PREVIEW_WIDTH
                }
                if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
                    maxPreviewHeight = MAX_PREVIEW_HEIGHT
                }
                previewSize = chooseOptimalSize(
                    map!!.getOutputSizes(SurfaceTexture::class.java),
                    getSize().widthPixels,
                    getSize().heightPixels,
                    maxPreviewWidth,
                    maxPreviewHeight,
                    largest
                )
                val orientation = resources.configuration.orientation
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    textureView.setAspectRatio(
                        previewSize.width * getSize().heightPixels / getSize().widthPixels,
                        getSize().heightPixels
                    )
                } else {
                    textureView.setAspectRatio(
                        previewSize.height, previewSize.width
                    )
                }
                //                Log.e("TAG", "orientation " + orientation);
//                Log.e("TAG", "swapRotation " + swapRotation);
//                Log.e("TAG", "Device orientation " + deviceOrientation);
//                Log.e("TAG", "total rotation " + totalRotation);
//
//                Log.e("TAG", "sensor device " +
//                        sensorDeviceRotation(cameraManager.getCameraCharacteristics(cameraManager.getCameraIdList()[1])
//                                , deviceOrientation));
                return
            }
        } catch (e: CameraAccessException) {
        }
    }

    inner class CompareSizesByArea : Comparator<Size?> {
        override fun compare(lhs: Size?, rhs: Size?): Int {
            return java.lang.Long.signum(
                lhs!!.width.toLong() * lhs.height -
                        rhs!!.width.toLong() * rhs.height
            )
        }

//        override fun compare(o1: Size?, o2: Size?): Int {
//            TODO("Not yet implemented")
//        }
    }

    private fun chooseOptimalSize(
        choices: Array<Size>, textureViewWidth: Int,
        textureViewHeight: Int, maxWidth: Int, maxHeight: Int, aspectRatio: Size
    ): Size {

        // Collect the supported resolutions that are at least as big as the preview Surface
        val bigEnough: MutableList<Size> = ArrayList()
        // Collect the supported resolutions that are smaller than the preview Surface
        val notBigEnough: MutableList<Size> = ArrayList()
        val w = aspectRatio.width
        val h = aspectRatio.height
        for (option in choices) {
            if (option.width <= maxWidth && option.height <= maxHeight && option.height === option.width * h / w) {
                if (option.width >= textureViewWidth &&
                    option.height >= textureViewHeight
                ) {
                    bigEnough.add(option)
                } else {
                    notBigEnough.add(option)
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        return when {
            bigEnough.size > 0 -> {
                Collections.min(bigEnough, CompareSizesByArea())
            }
            notBigEnough.size > 0 -> {
                Collections.max(notBigEnough, CompareSizesByArea())
            }
            else -> {
                choices[0]
            }
        }
    }


    private fun sensorDeviceRotation(
        cameraCharacteristics: CameraCharacteristics,
        deviceRotation: Int
    ): Int {
        var deviceRotation = deviceRotation
        val sensorOrientation = cameraCharacteristics[CameraCharacteristics.SENSOR_ORIENTATION]!!
        deviceRotation = ORIENTATIONS.get(deviceRotation)
        return (sensorOrientation + deviceRotation + 360) % 360
    }

    private fun getSize(): DisplayMetrics {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics
    }

    private fun getListCamera(): List<String>? {
        val cameraManager: CameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        val list: MutableList<String> = ArrayList()
        try {
            for (cameraId in cameraManager.getCameraIdList()) {
                list.add(cameraId)
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
        return list
    }

    private val surfaceTextureListener: TextureView.SurfaceTextureListener =
        object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                setupCamera(width, height)
                connectCamera()
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                return false
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {

            }
        }

    private fun startPreview() {
        val surfaceTexture: SurfaceTexture? = textureView.surfaceTexture
        if (surfaceTexture != null) {
            if (previewSize != null) {
                surfaceTexture.setDefaultBufferSize(previewSize.width, previewSize.height)
            } else {
            }
        }
        val previewSurface = Surface(surfaceTexture)
        try {
            captureRequestBuilder =
                cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            //            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_IDLE);
//            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
//            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
//                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            captureRequestBuilder!!.addTarget(previewSurface)
            cameraDevice!!.createCaptureSession(
                Arrays.asList(previewSurface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        if (cameraDevice == null) {
                            return
                        }
                        try {
                            var previewCaptureRequest = captureRequestBuilder!!.build()
//                                captureRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, getRange());
                            //                                captureRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, getRange());
                            session.setRepeatingRequest(
                                captureRequestBuilder!!.build(),
                                captureCallback, backgroundHandler
                            )
                            previewCaptureRequest = captureRequestBuilder!!.build()
                            session.setRepeatingRequest(
                                previewCaptureRequest,
                                captureCallback,
                                backgroundHandler
                            )
//                                textureView.setOnTouchListener(new CameraFocusOnTouchHandler
//                                        (characteristics, captureRequestBuilder, session, backgroundHandler));
                            //                                textureView.setOnTouchListener(new CameraFocusOnTouchHandler
//                                        (characteristics, captureRequestBuilder, session, backgroundHandler));
                            captureRequestBuilder!!.set(
                                CaptureRequest.CONTROL_AF_MODE,
                                CaptureRequest.CONTROL_AF_MODE_AUTO
                            )


                        } catch (e: CameraAccessException) {
                            e.printStackTrace()
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {

                    }

                    override fun onReady(session: CameraCaptureSession) {
                        super.onReady(session)
                        //focus correct
                    }

                    override fun onClosed(session: CameraCaptureSession) {
                        super.onClosed(session)
                        Log.e("TAG", "session close")
                        //                            textureView.setEnabled(false);
                    }
                }, null
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
            throw RuntimeException(
                "kakak surface " + surfaceTexture + " preview : " +
                        previewSize.width + " : " + previewSize.height
            )
        }
    }

    private fun connectCamera() {
        Log.e("TAG", "CameraID $cameraID")
        val cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        if (cameraID == null) {
            return
        } else {
            Log.e("TAG", "Open $cameraID")
            try {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    cameraManager.openCamera(cameraID!!, cameraStateCallback, backgroundHandler)
                    Log.e("TAG", "CameraID open $cameraID")
                } else {
                    Log.e("TAG", "Open failure")
                }

            } catch (e: CameraAccessException) {
                Log.e("TAG", "Crash")
            }
        }
    }


    private var areWeFocused = false
    private var shouldCapture = false
    private val captureCallback: CameraCaptureSession.CaptureCallback = object : CameraCaptureSession.CaptureCallback() {
        private fun process(result: CaptureResult) {
            val afState = result[CaptureResult.CONTROL_AF_STATE]!!
            areWeFocused = CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED == afState
            if (shouldCapture) {
                if (areWeFocused) {
                    shouldCapture = false
                }
            }
        }

        override fun onCaptureStarted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            timestamp: Long,
            frameNumber: Long
        ) {
            super.onCaptureStarted(session, request, timestamp, frameNumber)
        }

        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            super.onCaptureCompleted(session, request, result)
        }

        override fun onCaptureFailed(
            session: CameraCaptureSession,
            request: CaptureRequest,
            failure: CaptureFailure
        ) {
            super.onCaptureFailed(session, request, failure)
        }
    }

    private lateinit var mSensorManager: SensorManager
    lateinit var accelerometer: Sensor
    lateinit var magnetometer: Sensor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        ORIENTATIONS.append(Surface.ROTATION_0, 90)
        ORIENTATIONS.append(Surface.ROTATION_90, 0)
        ORIENTATIONS.append(Surface.ROTATION_180, 270)
        ORIENTATIONS.append(Surface.ROTATION_270, 180)

        textureView.surfaceTextureListener = surfaceTextureListener

        mSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        findViewById<Button>(R.id.btnConfirm).setOnClickListener {
            height123 = findViewById<EditText>(R.id.editHeight).text.toString().toInt()
        }
        findViewById<Button>(R.id.btnGoto).setOnClickListener {
            startActivity(Intent(this, CustomShapeActivity::class.java))
        }
    }
    var height123 = 0

    override fun onResume() {
        super.onResume()
        mSensorManager.registerListener(this, accelerometer, 5000000);
        mSensorManager.registerListener(this, magnetometer, 5000000);
    }

    override fun onPause() {
        super.onPause()
        mSensorManager.unregisterListener(this)
    }

    var azimut : Float? = null
    private var mAccelerometerData = FloatArray(3)
    private var mMagnetometerData = FloatArray(3)
    override fun onSensorChanged(mSensorEvent: SensorEvent) {

        when(mSensorEvent.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                mAccelerometerData = mSensorEvent.values.clone()
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                mMagnetometerData = mSensorEvent.values.clone()
            }
        }
        val rotationMatrix = FloatArray(9)
        val rotationOk = SensorManager.getRotationMatrix(rotationMatrix, null, mAccelerometerData, mMagnetometerData)
        var floatOrient = FloatArray(3)
        if (rotationOk) {
            SensorManager.getOrientation(rotationMatrix, floatOrient)
            val azimuth = floatOrient[0]
            val pitch = floatOrient[1]
            val roll = floatOrient[2]
            val rotation = 0 - Math.toDegrees(pitch.toDouble()).toInt()
            val heightCal = height123.toFloat() * Math.tan(Math.toRadians(rotation.toDouble()))
            findViewById<TextView>(R.id.txtOrientation)?.apply {
                text = "rotation $rotation \n height $heightCal"//"Height $heightCal"
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }

}