package android.saswat.dashcamapplication

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

import android.content.ContentValues.TAG
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Size
import androidx.compose.material3.Icon
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.graphics.Color

import kotlinx.coroutines.*
import java.nio.FloatBuffer
import java.util.concurrent.Executors
import com.google.firebase.FirebaseApp
import java.util.Locale

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private val viewModel: DashCamViewModel by viewModels {
        DashCamViewModel.Factory(application)
    }
    
    // Debug variables to track sensor activity
    private var lastAccelUpdate = 0L
    private var lastGyroUpdate = 0L
    private var accelCounter = 0
    private var gyroCounter = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            // Ensure Firebase is properly initialized
            if (FirebaseApp.getApps(this).isEmpty()) {
                Log.w(TAG, "Firebase not initialized in Application class, initializing now")
                FirebaseApp.initializeApp(this)
            }
            
            val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                when {
                    permissions[Manifest.permission.CAMERA] == true && permissions[Manifest.permission.RECORD_AUDIO] == true -> {
                        setupUI()
                    }
                    permissions[Manifest.permission.CAMERA] == false || permissions[Manifest.permission.RECORD_AUDIO] == false -> {
                        setContent {
                            MaterialTheme {
                                ErrorScreen("Camera or audio permission denied. The app cannot function without these permissions.")
                            }
                        }
                    }
                }
            }

            val requiredPermissions = arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            )

            if (requiredPermissions.all { permission ->
                    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
                }) {
                setupUI()
            } else {
                permissionLauncher.launch(requiredPermissions)
            }
            
            // Initialize sensor manager
            sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        } catch (e: Exception) {
            Log.e(TAG, "Fatal error during app initialization", e)
            setContent {
                MaterialTheme {
                    ErrorScreen("Fatal error initializing app: ${e.message}")
                }
            }
        }
    }

    private fun setupUI() {
        setContent {
            MaterialTheme {
                DashcamApp()
            }
        }
    }

    @Composable
    private fun DashcamApp() {
        // Check if the app has the required model file
        val assetExists = remember {
            try {
                applicationContext.assets.open("sensory_model.onnx").use { it.close() }
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to verify model existence", e)
                false
            }
        }

        if (!assetExists) {
            ErrorScreen("Required model file is missing. Please reinstall the app.")
            return
        }

        // Observe ViewModel states
        val authState by remember { viewModel.authState }.collectAsState(initial = AuthState())
        val modelState by remember { viewModel.modelState }.collectAsState(initial = ModelState())  
        val riskScore by remember { viewModel.riskScore }.collectAsState(initial = 50f)
        
        if (!authState.isAuthenticated) {
            // Authentication screen
            LoginScreen(
                onLogin = { email, password ->
                    viewModel.signInWithEmail(email, password)
                },
                errorMessage = authState.error
            )
        } else if (!modelState.isLoaded) {
            // Model loading screen
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
                modelState.error?.let { error ->
                    Text(
                        text = "Model Error: $error",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            // Main app UI
            DashcamUI(riskScore)
        }
    }

    @Composable
    private fun LoginScreen(
        onLogin: (String, String) -> Unit,
        errorMessage: String?
    ) {
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .padding(16.dp)
                    .width(320.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "DashCam Login",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                    
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                    
                    errorMessage?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    
                    Button(
                        onClick = {
                            if (email.isNotEmpty() && password.isNotEmpty()) {
                                isLoading = true
                                onLogin(email, password)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White
                            )
                        } else {
                            Text("Login")
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun DashcamUI(riskScore: Float) {
        var errorMessage by remember { mutableStateOf<String?>(null) }
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val previewView = remember { PreviewView(context) }
        var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
        
        // Track sensor data for UI display
        var sensorDebugInfo by remember { mutableStateOf("Waiting for sensor data...") }
        
        // Risk level based on risk score
        val riskLevel = when {
            riskScore >= 70 -> "High Risk"
            riskScore >= 40 -> "Moderate Risk"
            else -> "Low Risk"
        }
        
        // Risk color based on risk score
        val riskColor = when {
            riskScore >= 70 -> Color.Red
            riskScore >= 40 -> Color(0xFFFF9800) // Orange
            else -> Color(0xFF4CAF50) // Green
        }
        
        // Update sensor debug info every second
        LaunchedEffect(Unit) {
            while(true) {
                delay(1000)
                viewModel.getSensorDebugInfo()?.let {
                    sensorDebugInfo = it
                }
            }
        }

        // Cleanup when leaving composition
        DisposableEffect(Unit) {
            onDispose {
                cameraProvider?.unbindAll()
            }
        }

        // Setup camera
        LaunchedEffect(context, lifecycleOwner) {
            try {
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProvider = cameraProviderFuture.get()
                val provider = cameraProvider ?: throw IllegalStateException("Camera initialization failed")

                val cameraSelector = try {
                    CameraSelector.DEFAULT_BACK_CAMERA
                } catch (e: Exception) {
                    Log.e(TAG, "Back camera unavailable, trying front camera", e)
                    try {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } catch (e2: Exception) {
                        Log.e(TAG, "No cameras available", e2)
                        errorMessage = "No cameras available on this device"
                        return@LaunchedEffect
                    }
                }

                val preview = Preview.Builder().build()
                val analysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(320, 320))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                
                analysis.setAnalyzer(Executors.newSingleThreadExecutor()) { image ->
                    try {
                        viewModel.calculateAndStoreRisk()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing image", e)
                    } finally {
                        image.close()
                    }
                }

                try {
                    // Clear all bindings
                    provider.unbindAll()

                    // Bind use cases to camera
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        analysis
                    )

                    // Connect the preview use case to the previewView
                    preview.setSurfaceProvider(previewView.surfaceProvider)
                } catch (exc: Exception) {
                    Log.e(TAG, "Failed to bind camera use cases", exc)
                    errorMessage = "Camera binding failed: ${exc.message}"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Camera setup failed", e)
                errorMessage = "Camera setup failed: ${e.message}"
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            // Camera preview
            AndroidView(
                factory = { previewView },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 160.dp) // Make room for overlay at bottom
            )
            
            // Error message overlay
            errorMessage?.let {
                Card(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.TopCenter),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
                    )
                ) {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            
            // Bottom panel with risk score and sensor data
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Risk score display
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Risk gauge/score
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = riskColor.copy(alpha = 0.2f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = String.format(Locale.US, "%.1f", riskScore),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = riskColor,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                )
                                Text(
                                    text = riskLevel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = riskColor
                                )
                            }
                        }
                        
                        // Calculate risk button
                        Button(
                            onClick = { 
                                viewModel.calculateAndStoreRisk() 
                                viewModel.resetAutoSignoutTimer()  // Reset timer on button click
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Calculate Risk")
                        }
                    }
                    
                    // Expandable sensor debug info
                    var showSensorInfo by remember { mutableStateOf(false) }
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showSensorInfo = !showSensorInfo },
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Sensor Data",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Icon(
                                    imageVector = if (showSensorInfo) 
                                        Icons.Default.KeyboardArrowUp 
                                    else 
                                        Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Expand"
                                )
                            }
                            
                            AnimatedVisibility(visible = showSensorInfo) {
                                Text(
                                    text = sensorDebugInfo,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier
                                        .padding(top = 8.dp)
                                        .fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun ErrorScreen(message: String) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = message,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            try {
                when (it.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> {
                        accelCounter++
                        if (System.currentTimeMillis() - lastAccelUpdate > 1000) {
                            Log.d(TAG, "Accelerometer: x=${it.values[0]}, y=${it.values[1]}, z=${it.values[2]} (${accelCounter} readings/sec)")
                            lastAccelUpdate = System.currentTimeMillis()
                            accelCounter = 0
                        }
                    }
                    Sensor.TYPE_GYROSCOPE -> {
                        gyroCounter++
                        if (System.currentTimeMillis() - lastGyroUpdate > 1000) {
                            Log.d(TAG, "Gyroscope: x=${it.values[0]}, y=${it.values[1]}, z=${it.values[2]} (${gyroCounter} readings/sec)")
                            lastGyroUpdate = System.currentTimeMillis()
                            gyroCounter = 0
                        }
                    }
                }
                viewModel.updateSensorData(it.sensor.type, it.values)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update sensor data", e)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onResume() {
        super.onResume()
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            Log.i(TAG, "Accelerometer sensor registered")
        } ?: Log.w(TAG, "No accelerometer available on this device")
        
        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            Log.i(TAG, "Gyroscope sensor registered")
        } ?: Log.w(TAG, "No gyroscope available on this device")
        
        // Notify ViewModel that app is in foreground
        viewModel.onAppForeground()
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        
        // Notify ViewModel that app is in background
        viewModel.onAppBackground()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}