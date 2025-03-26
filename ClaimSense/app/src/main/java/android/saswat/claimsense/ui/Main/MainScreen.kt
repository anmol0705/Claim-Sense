package android.saswat.claimsense.ui.main

import android.annotation.SuppressLint
import android.saswat.claimsense.ui.Screens
import android.saswat.claimsense.ui.dashboard.DashboardScreen
import android.saswat.claimsense.ui.vehicles.VehiclesScreen
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.saswat.claimsense.viewmodel.AuthViewModel
import android.saswat.claimsense.viewmodel.VehicleViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import android.saswat.claimsense.ui.dashboard.DashboardScreen
import android.saswat.claimsense.ui.vehicles.VehiclesScreen
import android.saswat.claimsense.R
import android.saswat.claimsense.viewmodel.UpdateState
import android.util.Log
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.launch

@SuppressLint("StateFlowValueCalledInComposition")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onSignOut: () -> Unit,
    authViewModel: AuthViewModel = viewModel(),
    vehicleViewModel: VehicleViewModel = viewModel()
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val currentScreen = navController.currentBackStackEntryAsState().value?.destination?.route ?: Screens.Dashboard.route
    var showAddVehicleDialog by remember { mutableStateOf(false) }
    val vehicleState by vehicleViewModel.state.collectAsState()
    val context = LocalContext.current
    val userData by authViewModel.userData.collectAsState()
    LaunchedEffect(vehicleState) {
        when (vehicleState) {
            is VehicleViewModel.VehicleState.Success -> {
                showAddVehicleDialog = false // This will close the dialog
                Toast.makeText(context, "Vehicle added successfully!", Toast.LENGTH_SHORT).show()
            }
            is VehicleViewModel.VehicleState.Error -> {
                Toast.makeText(
                    context,
                    (vehicleState as VehicleViewModel.VehicleState.Error).message,
                    Toast.LENGTH_SHORT
                ).show()
            }
            is VehicleViewModel.VehicleState.Loading -> {
                Log.d("MainScreen", "Loading...")
            }
            else -> {
                Log.d("MainScreen", "Other state: $vehicleState")
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black)
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "ClaimSense",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                    label = { Text("Dashboard") },
                    selected = navController.currentBackStackEntry?.destination?.route == Screens.Dashboard.route,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            navController.navigate(Screens.Dashboard.route)
                        }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(painter = painterResource(R.drawable.baseline_directions_car_24), contentDescription = null) },
                    label = { Text("Vehicles") },
                    selected = navController.currentBackStackEntry?.destination?.route == Screens.Vehicles.route,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            navController.navigate(Screens.Vehicles.route)
                        }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(painter = painterResource(R.drawable.baseline_description_24), contentDescription = null) },
                    label = { Text("Claims") },
                    selected = navController.currentBackStackEntry?.destination?.route == Screens.Claims.route,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            navController.navigate(Screens.Claims.route)
                        }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    label = { Text("Profile") },
                    selected = navController.currentBackStackEntry?.destination?.route == Screens.Profile.route,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            navController.navigate(Screens.Profile.route)
                        }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                Spacer(modifier = Modifier.weight(1f))

                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.ExitToApp, contentDescription = null) },
                    label = { Text("Sign Out") },
                    selected = false,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            onSignOut()
                        }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        when(currentScreen) {
                            Screens.Dashboard.route -> Text("Dashboard")
                            Screens.Vehicles.route -> Text("My Vehicles")
                            Screens.Claims.route -> Text("Claims")
                            Screens.Profile.route -> Text(
                                text = "Welcome back, ${userData?.username ?: "user"}",
                                fontSize = 20.sp,  
                                color = Color.White
                            )
                            else -> Text("ClaimSense")
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    if (drawerState.isClosed) {
                                        drawerState.open()
                                    } else {
                                        drawerState.close()
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        if (currentScreen == Screens.Vehicles.route) {
                            IconButton(
                                onClick = { showAddVehicleDialog = true }
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Add Vehicle",
                                    tint = Color.White
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White,

                    )
                )
            }
        ) { paddingValues -> 
            NavHost(
                navController = navController,
                startDestination = Screens.Dashboard.route,
                modifier = Modifier.padding(paddingValues)
            ) {
                composable(Screens.Dashboard.route) {
                    DashboardScreen(
                        onMenuClick = {
                            scope.launch {
                                drawerState.open()
                            }
                        },
                        authViewModel = authViewModel
                    )
                }
                composable(Screens.Vehicles.route) {
                    VehiclesScreen(
                        onMenuClick = {
                            scope.launch {
                                drawerState.open()
                            }
                        },
                        vehicleViewModel = vehicleViewModel
                    )
                }
                composable(Screens.Claims.route) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Claims Screen - Coming Soon")
                    }
                }
                composable(Screens.Profile.route) {
                    ProfileContent(authViewModel, onSignOut)
                }
            }
            if (showAddVehicleDialog) {
                AddVehicleDialog(
                    onDismiss = { 
                        showAddVehicleDialog = false 
                    },
                    onAdd = { make, model, year, licensePlate ->
                        vehicleViewModel.addVehicle(
                            make = make,
                            model = model,
                            year = year,
                            licensePlate = licensePlate
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun AddVehicleDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, Int, String) -> Unit
) {
    var make by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var licensePlate by remember { mutableStateOf("") }
    var hasError by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        properties = DialogProperties(
            dismissOnClickOutside = !isSubmitting,
            dismissOnBackPress = !isSubmitting
        ),
        title = { 
            Text(
                "Add New Vehicle",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = make,
                    onValueChange = { 
                        make = it
                        hasError = false
                    },
                    label = { Text("Make") },
                    placeholder = { Text("e.g., Toyota") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSubmitting,
                    isError = hasError && make.isBlank(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Black,
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = Color.Black,
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = model,
                    onValueChange = { 
                        model = it
                        hasError = false
                    },
                    label = { Text("Model") },
                    placeholder = { Text("e.g., Camry") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSubmitting,
                    isError = hasError && model.isBlank(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Black,
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = Color.Black,
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = year,
                    onValueChange = { 
                        if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                            year = it
                            hasError = false
                        }
                    },
                    label = { Text("Year") },
                    placeholder = { Text("e.g., 2024") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSubmitting,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = hasError && (year.toIntOrNull() == null || year.toInt() < 1900),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Black,
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = Color.Black,
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = licensePlate,
                    onValueChange = { 
                        licensePlate = it.uppercase()
                        hasError = false
                    },
                    label = { Text("License Plate") },
                    placeholder = { Text("e.g., ABC123") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSubmitting,
                    isError = hasError && licensePlate.isBlank(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Black,
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = Color.Black,
                    ),
                    singleLine = true
                )

                if (hasError) {
                    Text(
                        "Please fill all fields correctly",
                        color = Color.Red,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (make.isBlank() || model.isBlank() || 
                        year.toIntOrNull() == null || licensePlate.isBlank()) {
                        hasError = true
                        Toast.makeText(context, "Please fill all fields correctly", Toast.LENGTH_SHORT).show()
                    } else {
                        try {
                            val yearInt = year.toInt()
                            if (yearInt < 1900 || yearInt > 2024) {
                                hasError = true
                                Toast.makeText(context, "Please enter a valid year", Toast.LENGTH_SHORT).show()
                            } else {
                                isSubmitting = true
                                onAdd(make, model, yearInt, licensePlate)
                            }
                        } catch (e: Exception) {
                            hasError = true
                            Toast.makeText(context, "Invalid year format", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black,
                    disabledContainerColor = Color.Gray
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        "Add Vehicle",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        dismissButton = {
            if (!isSubmitting) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.Black
                    )
                ) {
                    Text(
                        "Cancel",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    )
}

@Composable
private fun ProfileContent(
    authViewModel: AuthViewModel,
    onSignOut: () -> Unit
) {
    val userData by authViewModel.userData.collectAsState()
    val updateState by authViewModel.updateState.collectAsState()
    var isEditing by remember { mutableStateOf(false) }
    var editedUsername by remember { mutableStateOf(userData?.username ?: "") }
    var editedDriverLicense by remember { mutableStateOf(userData?.driverLicense ?: "") }
    val context = LocalContext.current

    LaunchedEffect(userData) {
        editedUsername = userData?.username ?: ""
        editedDriverLicense = userData?.driverLicense ?: ""
    }

    LaunchedEffect(updateState) {
        when (updateState) {
            is UpdateState.Success -> {
                Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                isEditing = false
                authViewModel.resetUpdateState()
            }
            is UpdateState.Error -> {
                Toast.makeText(context, (updateState as UpdateState.Error).message, Toast.LENGTH_SHORT).show()
                authViewModel.resetUpdateState()
            }
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            item {
                
                // Info Card with enhanced styling
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                        // Add subtle animation when appearing
                        .animateContentSize()
                        // Add elevation effect
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(16.dp),
                            spotColor = Color(0xFF00E5FF).copy(alpha = 0.2f)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF1E1E1E),
                                        Color(0xFF252525)
                                    ),
                                    start = Offset(0f, 0f),
                                    end = Offset(1000f, 1000f)
                                )
                            )
                            // Add subtle border glow
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(
                                    listOf(
                                        Color(0xFF303030),
                                        Color(0xFF00E5FF).copy(alpha = 0.3f),
                                        Color(0xFF303030)
                                    )
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        ) {
                            // Header with accent line
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Your Information",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .width(60.dp)
                                            .height(2.dp)
                                            .background(
                                                brush = Brush.horizontalGradient(
                                                    colors = listOf(
                                                        Color(0xFF00E5FF),
                                                        Color(0xFF00E5FF).copy(alpha = 0.3f)
                                                    )
                                                ),
                                                shape = RoundedCornerShape(1.dp)
                                            )
                                    )
                                }
                                
                                TextButton(
                                    onClick = {
                                        if (isEditing) {
                                            if (editedUsername.isNotBlank()) {
                                                authViewModel.updateUserData(
                                                    editedUsername,
                                                    editedDriverLicense
                                                )
                                            } else {
                                                Toast.makeText(context, "Username cannot be empty", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            isEditing = true
                                        }
                                    },
                                    enabled = updateState !is UpdateState.Loading,
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = Color(0xFF00E5FF)
                                    ),
                                    modifier = Modifier
                                        .background(
                                            color = Color(0xFF2A2A2A),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 8.dp)
                                ) {
                                    if (updateState is UpdateState.Loading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = Color(0xFF00E5FF),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        when {
                                            updateState is UpdateState.Loading -> "Saving..."
                                            isEditing -> "Save"
                                            else -> "Edit"
                                        },
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            // Edit mode or display mode content
                            if (isEditing) {
                                // Edit Fields with enhanced styling
                                EditField(
                                    label = "Username",
                                    value = editedUsername, 
                                    onValueChange = { editedUsername = it }
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                EditField(
                                    label = "Driver's License",
                                    value = editedDriverLicense, 
                                    onValueChange = { editedDriverLicense = it }
                                )

                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Button(
                                    onClick = { 
                                        isEditing = false
                                        authViewModel.resetUpdateState()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF2A2A2A),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Cancel")
                                }
                            } else {
                                // Enhanced info display
                                EnhancedInfoRow(label = "Username", value = userData?.username ?: "Not set")
                                EnhancedInfoRow(label = "Email", value = userData?.email ?: "Not set")
                                EnhancedInfoRow(label = "Driver's License", value = userData?.driverLicense ?: "Not set")
                                EnhancedInfoRow(label = "User ID", value = userData?.userId ?: "Not set", isLast = true)
                            }
                        }
                    }
                }

                // Enhanced Sign Out Button
                Button(
                    onClick = onSignOut,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .height(56.dp)
                        .shadow(
                            elevation = 4.dp,
                            spotColor = Color(0xFF00E5FF).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF00E5FF),
                                        Color(0xFF00B3CC)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp),
                                tint = Color.Black
                            )
                            Text(
                                "Sign Out", 
                                fontSize = 16.sp, 
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun EditField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color(0xFFBBBBBB),
            modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
        )
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF00E5FF).copy(alpha = 0.7f),
                            Color(0xFF00E5FF).copy(alpha = 0.3f)
                        )
                    ),
                    shape = RoundedCornerShape(12.dp)
                ),
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = Color(0xFF2A2A2A),
                focusedContainerColor = Color(0xFF2A2A2A),
                unfocusedIndicatorColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                cursorColor = Color(0xFF00E5FF),
                unfocusedTextColor = Color.White,
                focusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
    }
}

@Composable
private fun EnhancedInfoRow(
    label: String,
    value: String,
    isLast: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = if (isLast) 0.dp else 16.dp)
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color(0xFFBBBBBB),
            modifier = Modifier.padding(bottom = 2.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = Color(0xFF252525),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(vertical = 12.dp, horizontal = 16.dp)
        ) {
            Text(
                text = value,
                fontSize = 16.sp,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
        }
    }
}