package android.saswat.claimsense.ui.Main

import android.annotation.SuppressLint
import android.saswat.claimsense.R
import android.saswat.claimsense.ui.components.RiskScoreScreen
import android.saswat.claimsense.ui.dashboard.DashboardScreen
import android.saswat.claimsense.ui.screens.Screens
import android.saswat.claimsense.ui.vehicles.VehiclesScreen
import android.saswat.claimsense.viewmodel.AuthViewModel
import android.saswat.claimsense.viewmodel.ChatViewModel
import android.saswat.claimsense.viewmodel.RiskViewModel
import android.saswat.claimsense.viewmodel.VehicleViewModel
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch

@SuppressLint("StateFlowValueCalledInComposition")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onSignOut: () -> Unit,
    authViewModel: AuthViewModel = viewModel(),
    vehicleViewModel: VehicleViewModel = viewModel(),
    riskViewModel: RiskViewModel = viewModel(),
    chatViewModel: ChatViewModel = viewModel()
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
                    icon = { Icon(painter = painterResource(R.drawable.baseline_analytics_24), contentDescription = null) },
                    label = { Text("Risk Score") },
                    selected = navController.currentBackStackEntry?.destination?.route == Screens.RiskScore.route,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            navController.navigate(Screens.RiskScore.route)
                        }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(painter = painterResource(id = R.drawable.baseline_chat_24), contentDescription = null) },
                    label = { Text("Assistant") },
                    selected = navController.currentBackStackEntry?.destination?.route == Screens.Chat.route,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            navController.navigate(Screens.Chat.route)
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
                    icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null) },
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
                            Screens.RiskScore.route -> Text("Risk Score")
                            Screens.Chat.route -> Text("AI Assistant")
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
                composable(Screens.RiskScore.route) {
                    LaunchedEffect(Unit) {
                        riskViewModel.fetchRiskData()
                    }
                    RiskScoreScreen(riskViewModel)
                }
                composable(Screens.Chat.route) {
                    ChatScreen(chatViewModel)
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
                        } catch (_: Exception) {
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



