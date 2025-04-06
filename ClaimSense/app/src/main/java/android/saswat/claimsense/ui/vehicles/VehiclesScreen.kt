package android.saswat.claimsense.ui.vehicles

import android.saswat.claimsense.data.model.Vehicle
import android.saswat.claimsense.viewmodel.VehicleViewModel
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Menu
import android.saswat.claimsense.R
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehiclesScreen(
    onMenuClick: () -> Unit,
    vehicleViewModel: VehicleViewModel = viewModel()
) {
    val vehicles by vehicleViewModel.vehicles.collectAsState()
    val state by vehicleViewModel.state.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(state) {
        when (state) {
            is VehicleViewModel.VehicleState.Error -> {
                Toast.makeText(
                    context,
                    (state as VehicleViewModel.VehicleState.Error).message,
                    Toast.LENGTH_SHORT
                ).show()
            }
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        when (state) {
            is VehicleViewModel.VehicleState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {

                    if (vehicles.isEmpty()) {
                        EmptyVehiclesMessage(
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            onAddClick = { showAddDialog = true }
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(vehicles) { vehicle ->
                                VehicleCard(
                                    vehicle = vehicle,
                                    vehicleViewModel = vehicleViewModel
                                )
                            }

                            item {
                                FloatingActionButton(
                                    onClick = { showAddDialog = true },
                                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                    containerColor = MaterialTheme.colorScheme.primary
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Add Vehicle"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddVehicleDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { make, model, year, licensePlate, isPaid ->
                vehicleViewModel.addVehicle(
                    make = make,
                    model = model,
                    year = year,
                    licensePlate = licensePlate,
                    isPaid = isPaid
                )
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun EmptyVehiclesMessage(
    modifier: Modifier = Modifier,
    onAddClick: () -> Unit
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No vehicles added yet",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onAddClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Vehicle")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddVehicleDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, Int, String, Boolean) -> Unit
) {
    var make by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var licensePlate by remember { mutableStateOf("") }
    var isPaid by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF1E2530),
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Add New Vehicle",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                TextField(
                    value = make,
                    onValueChange = {
                        make = it
                        hasError = false
                    },
                    label = { Text("Make") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    isError = hasError && make.isBlank(),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF273548),
                        unfocusedContainerColor = Color(0xFF273548),
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = Color.Gray
                    )
                )

                TextField(
                    value = model,
                    onValueChange = {
                        model = it
                        hasError = false
                    },
                    label = { Text("Model") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    isError = hasError && model.isBlank(),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF273548),
                        unfocusedContainerColor = Color(0xFF273548),
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = Color.Gray
                    )
                )

                TextField(
                    value = year,
                    onValueChange = {
                        year = it
                        hasError = false
                    },
                    label = { Text("Year") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    isError = hasError && (year.toIntOrNull() == null || year.toInt() < 1900),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF273548),
                        unfocusedContainerColor = Color(0xFF273548),
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = Color.Gray
                    )
                )

                TextField(
                    value = licensePlate,
                    onValueChange = {
                        licensePlate = it
                        hasError = false
                    },
                    label = { Text("License Plate") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    isError = hasError && licensePlate.isBlank(),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF273548),
                        unfocusedContainerColor = Color(0xFF273548),
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = Color.Gray
                    )
                )

                // Payment Status Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Payment Status",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isPaid) "PAID" else "UNPAID",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isPaid) Color.Green else Color(0xFFFFA500),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(end = 8.dp)
                        )

                        Switch(
                            checked = isPaid,
                            onCheckedChange = { isPaid = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color.Green,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color.Gray
                            )
                        )
                    }
                }

                if (hasError) {
                    Text(
                        "Please fill all fields correctly",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            "Cancel",
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            hasError = make.isBlank() || model.isBlank() ||
                                    year.toIntOrNull() == null || licensePlate.isBlank()
                            if (!hasError) {
                                onAdd(make, model, year.toInt(), licensePlate, isPaid)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Add Vehicle")
                    }
                }
            }
        }
    }
}

@Composable
private fun VehicleCard(
    vehicle: Vehicle,
    vehicleViewModel: VehicleViewModel
) {
    val scope = rememberCoroutineScope()
    var isUpdating by remember { mutableStateOf(false) }
    var currentIsPaid by remember { mutableStateOf(vehicle.isPaid) }

    LaunchedEffect(vehicle.isPaid) {
        currentIsPaid = vehicle.isPaid
        Log.d("VehicleCard", "Payment status updated: ${vehicle.make} ${vehicle.model}, isPaid=${vehicle.isPaid}")
    }

    Log.d("VehicleCard", "Displaying vehicle: ${vehicle.make} ${vehicle.model}, isPaid=${vehicle.isPaid}")

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E2530)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${vehicle.make} ${vehicle.model}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${vehicle.year}",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White.copy(alpha = 0.7f)
                        )

                        Text(
                            text = " • ",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White.copy(alpha = 0.5f)
                        )

                        Text(
                            text = vehicle.licensePlate,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        val statusColor = if (currentIsPaid) Color.Green else Color(0xFFFFA500) // Orange for unpaid
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    color = statusColor,
                                    shape = CircleShape
                                )
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        Text(
                            text = if (currentIsPaid) "PAID" else "UNPAID",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }

                // Space for car image (to be added by user)
                Box(
                    modifier = Modifier
                        .size(30.dp)
                )

                // Payment toggle button
                Button(
                    onClick = {
                        isUpdating = true
                        currentIsPaid = !currentIsPaid
                        Log.d("VehicleCard", "Toggle payment from ${vehicle.isPaid} to ${!vehicle.isPaid}")
                        scope.launch {
                            try {
                                vehicleViewModel.updatePaymentStatus(vehicle.id, !vehicle.isPaid)
                            } catch (e: Exception) {
                                currentIsPaid = vehicle.isPaid
                                throw e
                            } finally {
                                isUpdating = false
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentIsPaid) Color(0xFF1A3A1A) else Color(0xFF3A2A10)
                    ),
                    modifier = Modifier.padding(start = 8.dp),
                    enabled = !isUpdating
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isUpdating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = if (currentIsPaid) Color.Green else Color(0xFFFFA500),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Toggle Payment Status",
                                tint = if (currentIsPaid) Color.Green else Color(0xFFFFA500),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (currentIsPaid) "PAID" else "UNPAID",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (currentIsPaid) Color.Green else Color(0xFFFFA500)
                        )
                    }
                }
            }
        }
    }
}