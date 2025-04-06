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
                            Screens.Dashboard.route -> Text("DASHBOARD")
                            Screens.Vehicles.route -> Text("VEHICLES OWNED")
                            Screens.Claims.route -> Text("CLAIMS")
                            Screens.Profile.route -> Text("PROFILE")
                            Screens.RiskScore.route -> Text("RISK SCORE")
                            Screens.Chat.route -> Text("AI ASSISTANT")
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

        }
    }
}





