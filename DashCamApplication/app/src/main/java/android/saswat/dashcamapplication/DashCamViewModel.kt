package android.saswat.dashcamapplication

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.app.Application
import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.opencv.android.OpenCVLoader
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.opencv.video.*
import java.nio.FloatBuffer
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sqrt

private const val TAG = "DashCamViewModel"
private const val ACC_X = 0
private const val ACC_Y = 1
private const val ACC_Z = 2
private const val GYRO_X = 3
private const val GYRO_Y = 4
private const val GYRO_Z = 5
private const val DEFAULT_RISK_SCORE = 50f
//private const val SENSORY_WEIGHT = 0.6f
//private const val IMAGE_WEIGHT = 0.4f

private const val SENSORY_WEIGHT = 0.0f
private const val IMAGE_WEIGHT = 1.0f

private const val HISTORY_WEIGHT = 0.4f
private const val CURRENT_WEIGHT = 0.6f

private const val RISK_SMOOTHING_FACTOR = 0.3f
private const val RISK_HISTORY_SIZE = 100
private const val MAX_RISK_CHANGE_THRESHOLD = 10f
private const val CAMERA_RESOLUTION_WIDTH = 320
private const val CAMERA_RESOLUTION_HEIGHT = 320
private const val PROCESS_EVERY_N_FRAMES = 1
private const val AUTO_SIGNOUT_DELAY_MS = 30 * 60 * 1000L
private const val RISK_STORAGE_INTERVAL_MS = 30 * 1000L

private val VEHICLE_CLASSES = intArrayOf(2, 3, 5, 6, 7)

private val weights = mapOf(
    "traffic_density" to 0.3f,
    "lane_discipline" to 0.3f,
    "relative_speed" to 0.4f
)

data class AuthState(val isAuthenticated: Boolean = false, val error: String? = null, val userId: String? = null, val isLoading: Boolean = false)
data class ModelState(val isLoaded: Boolean = false, val error: String? = null)

class DashCamViewModel(application: Application) : AndroidViewModel(application) {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState
    private val _modelState = MutableStateFlow(ModelState())
    val modelState: StateFlow<ModelState> = _modelState
    private val _riskScore = MutableStateFlow(DEFAULT_RISK_SCORE)
    val riskScore: StateFlow<Float> = _riskScore
    private var sensorySession: OrtSession? = null
    private val sensorData = FloatArray(6) { 0f }
    private var lastSensorUpdate = 0L
    private val riskHistory = ConcurrentLinkedDeque<Float>()
    private var lastCalculatedRisk = DEFAULT_RISK_SCORE
    private var autoSignoutJob: Job? = null
    private val _isSignedOut = MutableStateFlow(false)
    val isSignedOut: StateFlow<Boolean> = _isSignedOut
    private var lastRiskStorageTime = 0L
    private var yoloSession: OrtSession? = null
    private val _sensoryRiskScore = MutableStateFlow(DEFAULT_RISK_SCORE)
    val sensoryRiskScore: StateFlow<Float> = _sensoryRiskScore
    private val _imageRiskScore = MutableStateFlow(DEFAULT_RISK_SCORE)
    val imageRiskScore: StateFlow<Float> = _imageRiskScore
    private val modelLock = Any()
    private var isModelClosed = false
    private var frameCounter = 0
    private val imageProcessingActive = AtomicBoolean(false)
    private var laneDetectionEnabled = true
    private var lastProcessedFrame: Mat? = null
    private val relative_speeds = ConcurrentLinkedDeque<Float>()
    private var sensorMinValues: FloatArray = FloatArray(6) { Float.MAX_VALUE }
    private var sensorMaxValues: FloatArray = FloatArray(6) { Float.MIN_VALUE }
    private var isSensorDataInitialized = false

    init {
        if (OpenCVLoader.initDebug()) {
            Log.i(TAG, "OpenCV initialized successfully")
        } else {
            Log.e(TAG, "OpenCV initialization failed")
        }
        loadOnnxModel()
        loadYoloModel()
        checkCurrentUser()
        lastRiskStorageTime = System.currentTimeMillis()

        viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                delay(30000)
                System.gc()
                Log.d(TAG, "Performed scheduled garbage collection")
            }
        }
    }

    private fun checkCurrentUser() {
        _authState.value = AuthState(isAuthenticated = false)
        if (auth.currentUser != null) {
            signOut()
        }
    }

    fun signInWithEmail(email: String, password: String) {
        _authState.value = _authState.value.copy(isLoading = true, error = null)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                result.user?.let { user ->
                    _authState.value = AuthState(isAuthenticated = true, userId = user.uid, isLoading = false)
                    _isSignedOut.value = false
                    startAutoSignoutTimer()
                } ?: run {
                    _authState.value = AuthState(isAuthenticated = false, error = "Sign in failed. Please try again.", isLoading = false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sign in with email/password", e)
                _authState.value = AuthState(isAuthenticated = false, error = e.message ?: "Authentication failed", isLoading = false)
            }
        }
    }

    fun getSensorDebugInfo(): String {
        val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
        val lastUpdateTime = if (lastSensorUpdate > 0) {
            dateFormat.format(java.util.Date(lastSensorUpdate))
        } else {
            "Never"
        }
        return "Sensor Data:\n" + "ACC(${sensorData[ACC_X]}, ${sensorData[ACC_Y]}, ${sensorData[ACC_Z]}) " + "GYRO(${sensorData[GYRO_X]}, ${sensorData[GYRO_Y]}, ${sensorData[GYRO_Z]})" + "Last Update: $lastUpdateTime"
    }

    private fun loadOnnxModel() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val env = OrtEnvironment.getEnvironment()
                try {
                    getApplication<Application>().assets.open("sensory_model.onnx").use { inputStream ->
                        val modelBytes = inputStream.readBytes()
                        sensorySession = env.createSession(modelBytes, OrtSession.SessionOptions())
                    }
                    _modelState.value = ModelState(isLoaded = true)
                } catch (e: java.io.FileNotFoundException) {
                    Log.e(TAG, "ONNX model file not found in assets", e)
                    _modelState.value = ModelState(isLoaded = false, error = "Model file not found: sensory_model.onnx")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load ONNX model", e)
                _modelState.value = ModelState(error = e.message)
            }
        }
    }

    private fun loadYoloModel() {
        viewModelScope.launch(Dispatchers.IO) {
            synchronized(modelLock) {
                try {
                    yoloSession?.close()
                    val env = OrtEnvironment.getEnvironment()
                    try {
                        getApplication<Application>().assets.open("yolov8n.onnx").use { inputStream ->
                            val modelBytes = inputStream.readBytes()
                            yoloSession = env.createSession(modelBytes, OrtSession.SessionOptions())
                        }
                        isModelClosed = false
                        Log.d(TAG, "Successfully loaded YOLOv8n model")
                    } catch (e: java.io.FileNotFoundException) {
                        Log.e(TAG, "YOLO model file not found in assets", e)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load YOLO model", e)
                }
            }
        }
    }

    fun updateSensorData(sensorType: Int, values: FloatArray) {
        when (sensorType) {
            android.hardware.Sensor.TYPE_ACCELEROMETER -> {
                sensorData[ACC_X] = values[0]
                sensorData[ACC_Y] = values[1]
                sensorData[ACC_Z] = values[2]
            }
            android.hardware.Sensor.TYPE_GYROSCOPE -> {
                sensorData[GYRO_X] = values[0]
                sensorData[GYRO_Y] = values[1]
                sensorData[GYRO_Z] = values[2]
            }
        }
        lastSensorUpdate = System.currentTimeMillis()
        updateSensorMinMax(sensorData)
        resetAutoSignoutTimer()
    }

    private fun updateSensorMinMax(data: FloatArray) {
        if (!isSensorDataInitialized) {
            sensorMinValues = data.copyOf()
            sensorMaxValues = data.copyOf()
            isSensorDataInitialized = true
        } else {
            for (i in 0 until 6) {
                sensorMinValues[i] = minOf(sensorMinValues[i], data[i])
                sensorMaxValues[i] = maxOf(sensorMaxValues[i], data[i])
            }
        }
    }

    private fun normalizeSensorData(data: FloatArray): FloatArray {
        if (!isSensorDataInitialized) {
            return data.copyOf()
        }
        val normalizedData = FloatArray(6)
        for (i in 0 until 6) {
            val range = sensorMaxValues[i] - sensorMinValues[i]
            normalizedData[i] = if (range != 0f) {
                (data[i] - sensorMinValues[i]) / range
            } else {
                0f
            }
        }
        return normalizedData
    }

    fun calculateAndStoreRisk() {
        val normalizedSensorData = normalizeSensorData(sensorData)
        val sensoryRisk = calculateRisk(normalizedSensorData)
        _sensoryRiskScore.value = sensoryRisk
        updateCombinedRiskScore()
        resetAutoSignoutTimer()
    }

    private fun updateCombinedRiskScore() {
        val sensoryRisk = _sensoryRiskScore.value
        val imageRisk = _imageRiskScore.value
        val combinedRisk = combine_risk_scores(sensoryRisk, imageRisk)
        val smoothedRisk = smoothRiskScore(combinedRisk)

        Log.d(TAG, "Risk scores - Sensory: $sensoryRisk, Image: $imageRisk, Combined: $combinedRisk, Smoothed: $smoothedRisk")
        Log.d(TAG, "Risk weights - Sensory: $SENSORY_WEIGHT, Image: $IMAGE_WEIGHT")

        _riskScore.value = smoothedRisk
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastRiskStorageTime >= RISK_STORAGE_INTERVAL_MS) {
            Log.d(TAG, "INTERVAL CHECK: Time to store risk scores (30-second interval reached)")
            storeRiskScoresInFirestore(smoothedRisk, sensoryRisk, imageRisk)
            lastRiskStorageTime = currentTime
        }
    }

    private fun combine_risk_scores(sensoryScore: Float, imageScore: Float): Float {
        return SENSORY_WEIGHT * sensoryScore + IMAGE_WEIGHT * imageScore
    }

    private fun smoothRiskScore(newRisk: Float): Float {
        // Add the new risk to history
        while (riskHistory.size >= RISK_HISTORY_SIZE) {
            riskHistory.removeFirst()
        }
        riskHistory.addLast(newRisk)

        // Calculate historical score
        val historicalScore = if (riskHistory.isNotEmpty()) {
            riskHistory.average().toFloat()
        } else {
            newRisk
        }

        // Apply exponential weighted moving average smoothing
        val ewmSmoothedRisk = lastCalculatedRisk + RISK_SMOOTHING_FACTOR * (newRisk - lastCalculatedRisk)

        // Combine current and historical scores
        val finalScore = CURRENT_WEIGHT * ewmSmoothedRisk + HISTORY_WEIGHT * historicalScore

        // Update the last calculated risk
        lastCalculatedRisk = ewmSmoothedRisk

        // Apply threshold to prevent large jumps
        val previousScore = _riskScore.value
        val scoreDifference = finalScore - previousScore

        val smoothedScore = if (abs(scoreDifference) > MAX_RISK_CHANGE_THRESHOLD) {
            previousScore + sign(scoreDifference) * MAX_RISK_CHANGE_THRESHOLD
        } else {
            finalScore
        }

        // Ensure risk score is never negative
        return maxOf(0f, smoothedScore)
    }

    private fun calculateRisk(data: FloatArray): Float {
        try {
            if (data.all { it == 0f }) {
                Log.w(TAG, "Cannot calculate risk: all sensor data is zero")
                return DEFAULT_RISK_SCORE
            }
            val session = sensorySession
            if (session == null) {
                Log.e(TAG, "Cannot calculate risk: model session is null")
                return DEFAULT_RISK_SCORE
            }
            val env = OrtEnvironment.getEnvironment()
            var sensoryTensor: OnnxTensor? = null
            try {
                sensoryTensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(data), longArrayOf(1, 6))
                val outputs = session.run(mapOf("float_input" to sensoryTensor))
                val outputTensor = outputs.get(0)
                return when (val outputValue = outputTensor.value) {
                    is Array<*> -> {
                        if (outputValue.isNotEmpty()) {
                            when (val firstElement = outputValue[0]) {
                                is FloatArray -> {
                                    val sensoryProba = firstElement
                                    calculateSensoryRisk(sensoryProba)
                                }
                                is DoubleArray -> {
                                    val floatArray = firstElement.map { it.toFloat() }.toFloatArray()
                                    calculateSensoryRisk(floatArray)
                                }
                                else -> DEFAULT_RISK_SCORE
                            }
                        } else DEFAULT_RISK_SCORE
                    }
                    is FloatArray -> calculateSensoryRisk(outputValue)
                    is LongArray -> calculateSensoryRisk(outputValue.map { it.toFloat() }.toFloatArray())
                    else -> DEFAULT_RISK_SCORE
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during model inference", e)
                return DEFAULT_RISK_SCORE
            } finally {
                sensoryTensor?.close()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating risk", e)
            return DEFAULT_RISK_SCORE
        }
    }

    private fun calculateSensoryRisk(proba: FloatArray): Float {
        val predClass = proba.indices.maxByOrNull { proba[it] } ?: 0
        var classProb = proba.maxOrNull() ?: 0f
        classProb = classProb.coerceIn(0f, 1f)
        val risk = when (predClass) {
            0 -> 80 + classProb * 20
            1 -> classProb * 30
            else -> 30 + classProb * 40
        }
        return risk.coerceIn(0f, 100f)
    }

    fun processImageData(imageData: ByteArray, width: Int, height: Int) {
        frameCounter++
        if (frameCounter % PROCESS_EVERY_N_FRAMES != 0) {
            return
        }

        if (imageProcessingActive.get()) {
            Log.d(TAG, "Skipping frame processing - previous task still active")
            return
        }

        viewModelScope.launch(Dispatchers.Default) {
            if (!imageProcessingActive.compareAndSet(false, true)) {
                return@launch
            }

            var frame: Mat? = null
            var prev_frame: Mat? = null
            try {
                val processStartTime = System.currentTimeMillis()
                Log.d(TAG, "Starting image processing for frame #$frameCounter with dimensions $width x $height")
                frame = Mat(height, width, CvType.CV_8UC4)
                frame.put(0, 0, imageData)
                
                // Store previous frame for potential flow calculations in the future
                if (lastProcessedFrame != null) {
                    prev_frame = lastProcessedFrame!!.clone()
                }
                
                val imageRisk = calculateImageRisk(frame, prev_frame, width, height)
                val processDuration = System.currentTimeMillis() - processStartTime
                Log.d(TAG, "Image risk calculated: $imageRisk (processing took $processDuration ms)")
                _imageRiskScore.value = imageRisk
                updateCombinedRiskScore()
                resetAutoSignoutTimer()
                
                // Store this frame for next time
                if (lastProcessedFrame != null) {
                    lastProcessedFrame!!.release()
                }
                lastProcessedFrame = frame.clone()
                
                Log.d(TAG, "Completed image processing for frame #$frameCounter")
            } catch (e: OutOfMemoryError) {
                Log.e(TAG, "OOM in processImageData", e)
                System.gc()
            } catch (e: Exception) {
                Log.e(TAG, "Error processing image data", e)
            } finally {
                frame?.release()
                prev_frame?.release()
                imageProcessingActive.set(false)
                System.gc()
            }
        }
    }

    private fun calculateImageRisk(frame: Mat, prevFrame: Mat?, width: Int, height: Int): Float {
        var preprocessedData: FloatArray? = null
        var outputTensor: OnnxTensor? = null
        try {
            synchronized(modelLock) {
                if (isModelClosed || yoloSession == null) {
                    Log.e(TAG, "Cannot calculate image risk: YOLO model session is null or closed")
                    loadYoloModel()
                    return DEFAULT_RISK_SCORE
                }

                preprocessedData = preprocessImageForYolo(frame)
                if (preprocessedData == null) {
                    Log.e(TAG, "Failed to preprocess image data")
                    return DEFAULT_RISK_SCORE
                }

                outputTensor = runYoloInference(preprocessedData)
                if (outputTensor == null) {
                    Log.e(TAG, "YOLO inference failed to produce output tensor")
                    return DEFAULT_RISK_SCORE
                }

                // Calculate traffic density from detected vehicles
                val boxes = extractBoxes(outputTensor)
                Log.d(TAG, "Detected ${boxes.size} valid vehicle boxes")
                val frameArea = CAMERA_RESOLUTION_WIDTH * CAMERA_RESOLUTION_HEIGHT
                val trafficDensity = calculateTrafficDensity(boxes, frameArea.toFloat())
                Log.d(TAG, "Traffic density: $trafficDensity%")

                // Calculate lane discipline
                val lanes = detectLanes(frame)
                val laneDiscipline = calculateLaneDiscipline(frame, lanes.first, lanes.second, boxes)
                Log.d(TAG, "Lane discipline: $laneDiscipline%")

                // Calculate relative speed if we have a previous frame
                var relativeSpeed = 0f
                if (prevFrame != null) {
                    relativeSpeed = calculateRelativeSpeed(prevFrame, frame)
                    relative_speeds.add(relativeSpeed)
                    while (relative_speeds.size > 10) {
                        relative_speeds.removeFirst()
                    }
                }
                
                // Use average speed if available
                val avgSpeed = if (relative_speeds.isNotEmpty()) {
                    relative_speeds.average().toFloat()
                } else {
                    relativeSpeed
                }
                
                Log.d(TAG, "Relative speed: $avgSpeed km/h")

                // Calculate final image risk using the weights
                val imageRisk = calculateImageRiskScore(trafficDensity, laneDiscipline, avgSpeed)
                Log.d(TAG, "Final image risk score: $imageRisk")
                
                return imageRisk
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in image risk calculation", e)
            return DEFAULT_RISK_SCORE
        } finally {
            outputTensor?.close()
            preprocessedData = null
            System.gc()
        }
    }

    private fun extractBoxes(outputTensor: OnnxTensor): List<FloatArray> {
        try {
            val outputData = outputTensor.floatBuffer.array()
            val numDetections = outputData.size / 6
            val boxes = mutableListOf<FloatArray>()
            
            for (i in 0 until numDetections) {
                val offset = i * 6
                if (offset + 6 > outputData.size) continue
                
                val confidence = outputData[offset + 4]
                val classId = outputData[offset + 5].toInt()
                
                if (confidence > 0.25 && VEHICLE_CLASSES.contains(classId)) {
                    val box = FloatArray(4)
                    box[0] = outputData[offset] * CAMERA_RESOLUTION_WIDTH  // x1
                    box[1] = outputData[offset + 1] * CAMERA_RESOLUTION_HEIGHT  // y1
                    box[2] = outputData[offset + 2] * CAMERA_RESOLUTION_WIDTH  // x2
                    box[3] = outputData[offset + 3] * CAMERA_RESOLUTION_HEIGHT  // y2
                    boxes.add(box)
                }
            }
            return boxes
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting boxes", e)
            return emptyList()
        }
    }
    
    private fun calculateTrafficDensity(boxes: List<FloatArray>, frameArea: Float): Float {
        if (boxes.isEmpty()) {
            return 0f
        }
        
        var totalArea = 0f
        for (box in boxes) {
            val width = box[2] - box[0]
            val height = box[3] - box[1]
            totalArea += width * height
        }
        
        val density = (totalArea / frameArea) * 100f
        return minOf(density, 100f)  // Cap at 100%
    }
    
    private fun calculateLaneDiscipline(frame: Mat, leftLane: Mat?, rightLane: Mat?, boxes: List<FloatArray>): Float {
        if (leftLane == null || rightLane == null || boxes.isEmpty()) {
            return 50f
        }
        
        val laneLeftX = minOf(leftLane.get(0, 0)[0], leftLane.get(0, 2)[0]).toFloat()
        val laneRightX = maxOf(rightLane.get(0, 0)[0], rightLane.get(0, 2)[0]).toFloat()
        
        var disciplineScore = 0f
        val totalVehicles = boxes.size
        
        for (box in boxes) {
            val xLeft = box[0]
            val xRight = box[2]
            val vehicleWidth = xRight - xLeft
            val xCenter = (xLeft + xRight) / 2
            
            var disciplineFactor = 0f
            if (xCenter < laneLeftX) {
                val distanceOutside = laneLeftX - xRight
                disciplineFactor = maxOf(0f, 1f - (distanceOutside / vehicleWidth))
            } else if (xCenter > laneRightX) {
                val distanceOutside = xLeft - laneRightX
                disciplineFactor = maxOf(0f, 1f - (distanceOutside / vehicleWidth))
            } else {
                val leftOverlap = maxOf(0f, laneLeftX - xLeft)
                val rightOverlap = maxOf(0f, xRight - laneRightX)
                val totalOverlap = leftOverlap + rightOverlap
                disciplineFactor = maxOf(0f, 1f - (totalOverlap / vehicleWidth))
            }
            
            disciplineScore += disciplineFactor
        }
        
        return (disciplineScore / totalVehicles) * 100f
    }
    
    private fun calculateImageRiskScore(trafficDensity: Float, laneDiscipline: Float, relativeSpeed: Float): Float {
        val tdRisk = minOf(trafficDensity / 50f, 1f)
        val ldRisk = 1f - (laneDiscipline / 100f)
        val rsRisk = minOf(maxOf(0f, abs(relativeSpeed) - 20f) / 80f, 1f)
        
        val riskScore = 100f * (weights["traffic_density"]!! * tdRisk + 
                     weights["lane_discipline"]!! * ldRisk + 
                     weights["relative_speed"]!! * rsRisk)
        
        // Ensure risk score is never negative
        return maxOf(0f, riskScore)
    }

    private fun preprocessImageForYolo(frame: Mat): FloatArray? {
        var resized: Mat? = null
        var rgbImage: Mat? = null
        try {
            resized = Mat()
            Imgproc.resize(frame, resized, Size(CAMERA_RESOLUTION_WIDTH.toDouble(), CAMERA_RESOLUTION_HEIGHT.toDouble()))
            rgbImage = Mat()
            Imgproc.cvtColor(resized, rgbImage, Imgproc.COLOR_RGBA2RGB)
            val floatBuffer = FloatArray(3 * CAMERA_RESOLUTION_WIDTH * CAMERA_RESOLUTION_HEIGHT)
            val channels = ArrayList<Mat>()
            try {
                Core.split(rgbImage, channels)
                for (c in 0 until channels.size) {
                    val channel = channels[c]
                    val bytes = ByteArray(CAMERA_RESOLUTION_WIDTH * CAMERA_RESOLUTION_HEIGHT)
                    channel.get(0, 0, bytes)
                    for (i in bytes.indices) {
                        floatBuffer[i * 3 + c] = (bytes[i].toInt() and 0xFF) / 255.0f
                    }
                }
                return floatBuffer
            } catch (e: Exception) {
                Log.e(TAG, "Error in preprocessImageForYolo", e)
                return null
            } finally {
                channels.forEach { it.release() }
            }
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "OOM in preprocessImageForYolo", e)
            System.gc()
            return null
        } finally {
            resized?.release()
            rgbImage?.release()
            System.gc()
        }
    }

    private fun runYoloInference(inputData: FloatArray?): OnnxTensor? {
        if (inputData == null) {
            return null
        }
        synchronized(modelLock) {
            if (isModelClosed || yoloSession == null) {
                Log.e(TAG, "YOLO session is closed or null, attempting to reload")
                loadYoloModel()
                return null
            }
            var inputTensor: OnnxTensor? = null
            try {
                val env = OrtEnvironment.getEnvironment()
                val shape = longArrayOf(1, 3, CAMERA_RESOLUTION_HEIGHT.toLong(), CAMERA_RESOLUTION_WIDTH.toLong())
                Log.d(TAG, "Creating input tensor with shape ${shape.joinToString()}")
                inputTensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(inputData), shape)

                val inputNames = yoloSession?.inputNames
                val outputNames = yoloSession?.outputNames
                Log.d(TAG, "Running YOLO model with inputs: ${inputNames?.joinToString()}, expecting outputs: ${outputNames?.joinToString()}")

                val session = yoloSession
                if (session == null) {
                    Log.e(TAG, "YOLO session is null")
                    return null
                }

                val outputs = session.run(mapOf("images" to inputTensor))
                if (outputs == null) {
                    Log.e(TAG, "YOLO model returned null outputs")
                    return null
                }

                Log.d(TAG, "YOLO inference completed successfully")
                val outputTensor = outputs.get(0) as? OnnxTensor
                if (outputTensor == null) {
                    Log.e(TAG, "Failed to get output tensor from YOLO model")
                }
                return outputTensor
            } catch (e: Exception) {
                Log.e(TAG, "Error running YOLO inference", e)
                return null
            } finally {
                inputTensor?.close()
                System.gc()
            }
        }
    }

    private fun analyzeYoloOutput(outputTensor: OnnxTensor): Float {
        try {
            if (outputTensor.info.type != ai.onnxruntime.OnnxJavaType.FLOAT) {
                Log.e(TAG, "YOLO output tensor is not FLOAT type: ${outputTensor.info.type}")
                return DEFAULT_RISK_SCORE
            }
            
            val shape = outputTensor.info.shape
            Log.d(TAG, "YOLO output tensor shape: ${shape.joinToString()}")
            
            val outputData = outputTensor.floatBuffer.array()
            Log.d(TAG, "YOLO output data size: ${outputData.size}")
            
            // Manually extract some values for debug
            if (outputData.size > 20) {
                val sampleData = outputData.take(20).joinToString(", ")
                Log.d(TAG, "Sample output data: $sampleData")
            }
            
            return processYoloFlatOutput(outputData)
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing YOLO output", e)
            return DEFAULT_RISK_SCORE
        }
    }

    private fun processYoloFlatOutput(outputData: FloatArray): Float {
        val boxes = mutableListOf<FloatArray>()
        val numDetections = outputData.size / 6
        var validDetections = 0
        Log.d(TAG, "Processing YOLO output with $numDetections potential detections")
        
        try {
            for (i in 0 until numDetections) {
                val offset = i * 6
                if (offset + 6 > outputData.size) {
                    Log.e(TAG, "YOLO output data is shorter than expected")
                    continue
                }
                val confidence = outputData[offset + 4]
                val classId = outputData[offset + 5].toInt()
                
                // Debug first few detections
                if (i < 5) {
                    Log.d(TAG, "Detection $i: class=$classId, confidence=$confidence")
                }
                
                // Accept any vehicle class from our list with reasonable confidence
                if (confidence > 0.25 && VEHICLE_CLASSES.contains(classId)) {
                    val box = outputData.copyOfRange(offset, offset + 4)
                    boxes.add(box)
                    validDetections++
                }
            }
            Log.d(TAG, "Found $validDetections valid vehicle detections")
            return calculateImageRiskScoreFromDetections(boxes)
        } catch (e: Exception) {
            Log.e(TAG, "Error processing YOLO output", e)
            return DEFAULT_RISK_SCORE
        }
    }

    private fun calculateImageRiskScoreFromDetections(boxes: List<FloatArray>): Float {
        if (boxes.isEmpty()) {
            Log.d(TAG, "No vehicles detected, setting risk to 0")
            return 0f
        }
        val frameArea = CAMERA_RESOLUTION_WIDTH * CAMERA_RESOLUTION_HEIGHT
        var totalArea = 0f
        for (box in boxes) {
            val x1 = box[0] * CAMERA_RESOLUTION_WIDTH
            val y1 = box[1] * CAMERA_RESOLUTION_HEIGHT
            val x2 = box[2] * CAMERA_RESOLUTION_WIDTH
            val y2 = box[3] * CAMERA_RESOLUTION_HEIGHT
            val width = x2 - x1
            val height = y2 - y1
            totalArea += width * height
        }
        val trafficDensity = (totalArea / frameArea) * 100f
        val risk = 100f * (trafficDensity.coerceAtMost(50f) / 50f)
        Log.d(TAG, "Traffic density: $trafficDensity%, risk score: $risk")
        return risk
    }

    private fun calculateLaneDiscipline(frame: Mat, leftLane: Mat?, rightLane: Mat?): Float {
        if (leftLane == null || rightLane == null) {
            return 50f
        }
        val laneLeftX = minOf(leftLane.get(0, 0)[0], leftLane.get(0, 2)[0]).toFloat()
        val laneRightX = maxOf(rightLane.get(0, 0)[0], rightLane.get(0, 2)[0]).toFloat()
        val laneWidth = laneRightX - laneLeftX
        val boxes = mutableListOf<FloatArray>()
        val outputData = runYoloInference(preprocessImageForYolo(frame))?.floatBuffer?.array()
        val numDetections = outputData?.size?.div(6) ?: 0
        outputData?.let { data ->
            for (i in 0 until numDetections) {
                val offset = i * 6
                val box = data.copyOfRange(offset, offset + 4)
                boxes.add(box)
            }
        }
        if (boxes.isEmpty()) {
            return 50f
        }
        var disciplineScore = 0f
        val totalVehicles = boxes.size
        for (box in boxes) {
            val xLeft = box[0] * frame.cols()
            val xRight = box[2] * frame.cols()
            val vehicleWidth = xRight - xLeft
            val xCenter = (xLeft + xRight) / 2
            var disciplineFactor = 0f
            if (xCenter < laneLeftX) {
                val distanceOutside = laneLeftX - xRight
                disciplineFactor = max(0f, 1f - (distanceOutside / vehicleWidth))
            } else if (xCenter > laneRightX) {
                val distanceOutside = xLeft - laneRightX
                disciplineFactor = max(0f, 1f - (distanceOutside / vehicleWidth))
            } else {
                val leftOverlap = max(0f, laneLeftX - xLeft)
                val rightOverlap = max(0f, xRight - laneRightX)
                val totalOverlap = leftOverlap + rightOverlap
                disciplineFactor = max(0f, 1f - (totalOverlap / vehicleWidth))
            }
            disciplineScore += disciplineFactor
        }
        return (disciplineScore / totalVehicles) * 100f
    }

    private fun detectLanes(frame: Mat, scale: Double = 1.0): Pair<Mat?, Mat?> {
        var smallFrame: Mat? = null
        var hsv: Mat? = null
        var maskWhite: Mat? = null
        var gray: Mat? = null
        var edges: Mat? = null
        var roiEdges: Mat? = null
        var lines: Mat? = null

        try {
            smallFrame = Mat()
            Imgproc.resize(frame, smallFrame, Size(), scale, scale)

            hsv = Mat()
            Imgproc.cvtColor(smallFrame, hsv, Imgproc.COLOR_BGR2HSV)

            maskWhite = Mat()
            Core.inRange(hsv, Scalar(0.0, 0.0, 200.0), Scalar(180.0, 30.0, 255.0), maskWhite)

            gray = Mat()
            Core.bitwise_and(smallFrame, smallFrame, gray, maskWhite)
            Imgproc.cvtColor(gray, gray, Imgproc.COLOR_BGR2GRAY)

            edges = Mat()
            Imgproc.Canny(gray, edges, 50.0, 150.0)

            roiEdges = regionOfInterest(edges)

            lines = Mat()
            Imgproc.HoughLinesP(roiEdges, lines, 1.0, Math.PI / 180.0, 50, 50.0, 30.0)

            if (lines.empty()) {
                return Pair(null, null)
            }

            val leftLines = mutableListOf<MatOfInt4>()
            val rightLines = mutableListOf<MatOfInt4>()

            for (i in 0 until lines.rows()) {
                val line = MatOfInt4(lines.row(i))
                try {
                    val lineArray = line.toArray()
                    val x1 = lineArray[0]
                    val y1 = lineArray[1]
                    val x2 = lineArray[2]
                    val y2 = lineArray[3]
                    val slope = (y2 - y1).toDouble() / (x2 - x1 + 1e-5)

                    if (abs(slope) in 0.3..1.5) {
                        if (slope < 0) {
                            leftLines.add(line)
                        } else {
                            rightLines.add(line)
                        }
                    }
                } finally {
                    if (leftLines.isEmpty() || rightLines.isEmpty()) {
                        line.release()
                    }
                }
            }

            val leftLane = if (leftLines.isNotEmpty()) calculateAverageLine(leftLines, scale) else null
            val rightLane = if (rightLines.isNotEmpty()) calculateAverageLine(rightLines, scale) else null

            leftLines.forEach { it.release() }
            rightLines.forEach { it.release() }

            return Pair(leftLane, rightLane)
        } finally {
            smallFrame?.release()
            hsv?.release()
            maskWhite?.release()
            gray?.release()
            edges?.release()
            roiEdges?.release()
            lines?.release()
        }
    }

    private fun regionOfInterest(img: Mat): Mat {
        val height = img.rows()
        val width = img.cols()
        val roi = MatOfPoint(
            Point((width * 0.1).toDouble(), height.toDouble()),
            Point((width * 0.9).toDouble(), height.toDouble()),
            Point((width * 0.6).toDouble(), (height * 0.6).toDouble()),
            Point((width * 0.4).toDouble(), (height * 0.6).toDouble())
        )
        val mask = Mat.zeros(img.size(), img.type())
        val points = MatOfPoint(roi)
        val listOfPoints = listOf(points)
        Imgproc.fillPoly(mask, listOfPoints, Scalar(255.0))
        val result = Mat()
        Core.bitwise_and(img, mask, result)
        return result
    }

    private fun calculateAverageLine(lines: List<MatOfInt4>, scale: Double): Mat {
        val avgLine = MatOfInt4()
        val sum = IntArray(4) { 0 }
        for (line in lines) {
            val lineArray = line.toArray()
            for (i in 0..3) {
                sum[i] += (lineArray[i] / scale).toInt()
            }
        }
        val avg = sum.map { it / lines.size }.toIntArray()
        avgLine.fromArray(*avg)
        return avgLine
    }

    private fun calculateRelativeSpeed(prevFrame: Mat, currFrame: Mat): Float {
        var prevGray: Mat? = null
        var currGray: Mat? = null
        try {
            // Convert frames to grayscale
            prevGray = Mat()
            currGray = Mat()
            Imgproc.cvtColor(prevFrame, prevGray, Imgproc.COLOR_BGR2GRAY)
            Imgproc.cvtColor(currFrame, currGray, Imgproc.COLOR_BGR2GRAY)
            
            // Find good features to track
            val corners = MatOfPoint()
            Imgproc.goodFeaturesToTrack(prevGray, corners, 50, 0.3, 10.0)
            
            if (corners.empty()) {
                return 0f
            }
            
            // Convert to the right format for optical flow
            val p0 = MatOfPoint2f()
            corners.convertTo(p0, CvType.CV_32FC2)
            
            // Calculate optical flow
            val p1 = MatOfPoint2f()
            val status = MatOfByte()
            val err = MatOfFloat()
            
            val lkParams = MatOfByte()
            val criteria = TermCriteria(TermCriteria.COUNT + TermCriteria.EPS, 10, 0.03)
            
            Video.calcOpticalFlowPyrLK(prevGray, currGray, p0, p1, status, err, Size(10.0, 10.0), 1, criteria, 0, 0.001)
            
            // Calculate average movement
            val points1 = p0.toArray()
            val points2 = p1.toArray()
            val statusArray = status.toArray()
            
            var totalDistance = 0f
            var validPoints = 0
            
            for (i in statusArray.indices) {
                if (statusArray[i] == 1.toByte()) {
                    val dist = sqrt(
                        (points2[i].x - points1[i].x).toDouble().pow(2.0) +
                                (points2[i].y - points1[i].y).toDouble().pow(2.0)
                    ).toFloat()
                    
                    totalDistance += dist
                    validPoints++
                }
            }
            
            if (validPoints == 0) {
                return 0f
            }
            
            // Convert to km/h - rough estimate
            val avgMovement = totalDistance / validPoints
            val fps = 30f  // Assuming 30 fps
            val speedFactor = 3.6f  // Convert m/s to km/h
            
            val calculatedSpeed = avgMovement * fps * speedFactor / 20f
            // Ensure we don't return negative speed values
            return maxOf(0f, calculatedSpeed)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating relative speed", e)
            return 0f
        } finally {
            prevGray?.release()
            currGray?.release()
        }
    }

    private fun storeRiskScoresInFirestore(combinedRisk: Float, sensoryRisk: Float, imageRisk: Float) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Log.e(TAG, "Cannot store risk: User not authenticated")
            return
        }
        val data = hashMapOf(
            "uid" to uid,
            "timestamp" to System.currentTimeMillis(),
            "risk" to maxOf(0f, combinedRisk),  // Ensure positive values
            "sensoryRisk" to maxOf(0f, sensoryRisk),    // Ensure positive values
            "imageRisk" to maxOf(0f, imageRisk)         // Ensure positive values
        )
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "STORING RISKS: Attempting to store risk scores (combined: $combinedRisk, sensory: $sensoryRisk, image: $imageRisk) for user $uid")
                val startTime = System.currentTimeMillis()
                val documentId = "risk_${System.currentTimeMillis()}"
                db.collection("risk_scores").document(uid).collection("transactions").document(documentId).set(data).await()
                val duration = System.currentTimeMillis() - startTime
                Log.d(TAG, "STORING RISKS: Successfully stored risk scores in document: risk_scores/$uid/transactions/$documentId in $duration ms")
            } catch (e: Exception) {
                Log.e(TAG, "STORING RISKS: Error storing risk scores", e)
                if (e.message?.contains("host", ignoreCase = true) == true || e.message?.contains("network", ignoreCase = true) == true || e.message?.contains("connect", ignoreCase = true) == true) {
                    Log.w(TAG, "STORING RISKS: This appears to be a network connectivity issue. Check that your emulator/device has internet access.")
                }
                if (e.message?.contains("permission", ignoreCase = true) == true) {
                    Log.w(TAG, "STORING RISKS: This appears to be a permissions issue. Verify your Firestore rules allow writing to this collection.")
                }
                saveRiskScoresLocally(combinedRisk, sensoryRisk, imageRisk)
            }
        }
    }

    private fun saveRiskScoresLocally(combinedRisk: Float, sensoryRisk: Float, imageRisk: Float) {
        try {
            val sharedPrefs = getApplication<Application>().getSharedPreferences("cached_risk_scores", Context.MODE_PRIVATE)
            val cachedScores = sharedPrefs.getString("pending_scores", "")
            val timestamp = System.currentTimeMillis()
            // Ensure positive values
            val safeComboRisk = maxOf(0f, combinedRisk)
            val safeSensoryRisk = maxOf(0f, sensoryRisk)
            val safeImageRisk = maxOf(0f, imageRisk)
            val newEntry = "$timestamp:$safeComboRisk:$safeSensoryRisk:$safeImageRisk"
            val updatedScores = if (cachedScores.isNullOrEmpty()) newEntry else "$cachedScores,$newEntry"
            val startTime = System.currentTimeMillis()
            sharedPrefs.edit { putString("pending_scores", updatedScores) }
            val duration = System.currentTimeMillis() - startTime
            Log.d(TAG, "Saved risk scores locally for later upload in $duration ms")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cache risk scores locally", e)
        }
    }

    fun resetAutoSignoutTimer() {
        autoSignoutJob?.cancel()
        autoSignoutJob = viewModelScope.launch {
            delay(AUTO_SIGNOUT_DELAY_MS)
            signOut()
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                if (auth.currentUser != null && System.currentTimeMillis() - lastRiskStorageTime > 60000) {
                    storeRiskScoresInFirestore(_riskScore.value, _sensoryRiskScore.value, _imageRiskScore.value)
                    Log.d(TAG, "Stored final risk score before sign out")
                }
                auth.signOut()
                getApplication<Application>().getSharedPreferences("auth_prefs", Context.MODE_PRIVATE).edit { clear() }
                _authState.value = AuthState(isAuthenticated = false)
                _isSignedOut.value = true
                Log.d(TAG, "User signed out successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error signing out", e)
            }
        }
    }

    fun onAppBackground() {
        signOut()
        Log.d(TAG, "App went to background, signing out immediately")
    }

    fun onAppForeground() {
        Log.d(TAG, "App came to foreground")
    }

    private fun startAutoSignoutTimer() {
        resetAutoSignoutTimer()
        Log.d(TAG, "Started auto-signout timer")
    }

    fun forceImageUpdate() {
        viewModelScope.launch(Dispatchers.IO) {
            val startRisk = _imageRiskScore.value
            val testImage = loadDebugImage()
            if (testImage != null) {
                imageProcessingActive.set(false)  
                val imageRisk = calculateImageRisk(testImage, null, CAMERA_RESOLUTION_WIDTH, CAMERA_RESOLUTION_HEIGHT)
                Log.d(TAG, "DEBUG: Forced image risk update from $startRisk to $imageRisk")
                _imageRiskScore.value = imageRisk
                updateCombinedRiskScore()
                testImage.release()
            }
        }
    }

    private fun loadDebugImage(): Mat? {
        try {
            val testMat = Mat(CAMERA_RESOLUTION_HEIGHT, CAMERA_RESOLUTION_WIDTH, CvType.CV_8UC3, Scalar(0.0, 0.0, 0.0))
            
            // Draw a car-like shape in the center to trigger the object detector
            // Draw body
            Imgproc.rectangle(testMat, 
                org.opencv.core.Point((CAMERA_RESOLUTION_WIDTH * 0.3).toDouble(), (CAMERA_RESOLUTION_HEIGHT * 0.4).toDouble()),
                org.opencv.core.Point((CAMERA_RESOLUTION_WIDTH * 0.7).toDouble(), (CAMERA_RESOLUTION_HEIGHT * 0.6).toDouble()),
                Scalar(128.0, 128.0, 128.0), -1)
                
            // Draw wheels
            Imgproc.circle(testMat,
                org.opencv.core.Point((CAMERA_RESOLUTION_WIDTH * 0.35).toDouble(), (CAMERA_RESOLUTION_HEIGHT * 0.6).toDouble()),
                (CAMERA_RESOLUTION_WIDTH * 0.05).toInt(), 
                Scalar(0.0, 0.0, 0.0), -1)
                
            Imgproc.circle(testMat,
                org.opencv.core.Point((CAMERA_RESOLUTION_WIDTH * 0.65).toDouble(), (CAMERA_RESOLUTION_HEIGHT * 0.6).toDouble()),
                (CAMERA_RESOLUTION_WIDTH * 0.05).toInt(), 
                Scalar(0.0, 0.0, 0.0), -1)
                
            // Draw windows
            Imgproc.rectangle(testMat, 
                org.opencv.core.Point((CAMERA_RESOLUTION_WIDTH * 0.35).toDouble(), (CAMERA_RESOLUTION_HEIGHT * 0.42).toDouble()),
                org.opencv.core.Point((CAMERA_RESOLUTION_WIDTH * 0.65).toDouble(), (CAMERA_RESOLUTION_HEIGHT * 0.5).toDouble()),
                Scalar(200.0, 200.0, 255.0), -1)
                
            Log.d(TAG, "Created debug test image")
            return testMat
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create debug image", e)
            return null
        }
    }

    override fun onCleared() {
        synchronized(modelLock) {
            isModelClosed = true
            sensorySession?.close()
            sensorySession = null
            yoloSession?.close()
            yoloSession = null
            lastProcessedFrame?.release()
            lastProcessedFrame = null
        }
        autoSignoutJob?.cancel()
        if (auth.currentUser != null) {
            viewModelScope.launch(Dispatchers.IO) {
                storeRiskScoresInFirestore(_riskScore.value, _sensoryRiskScore.value, _imageRiskScore.value)
            }
        }
        super.onCleared()
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DashCamViewModel::class.java)) {
                return DashCamViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}