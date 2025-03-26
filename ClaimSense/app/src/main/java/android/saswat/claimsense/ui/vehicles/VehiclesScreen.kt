package android.saswat.claimsense.ui.vehicles

import android.saswat.claimsense.data.model.Vehicle
import android.saswat.claimsense.viewmodel.VehicleViewModel
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import android.saswat.claimsense.R
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

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
                if (vehicles.isEmpty()) {
                    EmptyVehiclesMessage(
                        modifier = Modifier.align(Alignment.Center),
                        onAddClick = { showAddDialog = true }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(vehicles) { vehicle ->
                            VehicleCard(vehicle = vehicle)
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddVehicleDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { make, model, year, licensePlate ->
                vehicleViewModel.addVehicle(
                    make = make,
                    model = model,
                    year = year,
                    licensePlate = licensePlate
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
            )
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
    onAdd: (String, String, Int, String) -> Unit
) {
    var make by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var licensePlate by remember { mutableStateOf("") }
    var hasError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Vehicle") },
        text = {
            Column {
                TextField(
                    value = make,
                    onValueChange = { 
                        make = it
                        hasError = false
                    },
                    label = { Text("Make") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = hasError && make.isBlank()
                )
                Spacer(Modifier.height(8.dp))
                TextField(
                    value = model,
                    onValueChange = { 
                        model = it
                        hasError = false
                    },
                    label = { Text("Model") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = hasError && model.isBlank()
                )
                Spacer(Modifier.height(8.dp))
                TextField(
                    value = year,
                    onValueChange = { 
                        year = it
                        hasError = false
                    },
                    label = { Text("Year") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = hasError && (year.toIntOrNull() == null || year.toInt() < 1900)
                )
                Spacer(Modifier.height(8.dp))
                TextField(
                    value = licensePlate,
                    onValueChange = { 
                        licensePlate = it
                        hasError = false
                    },
                    label = { Text("License Plate") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = hasError && licensePlate.isBlank()
                )
                if (hasError) {
                    Text(
                        "Please fill all fields correctly",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    hasError = make.isBlank() || model.isBlank() || 
                            year.toIntOrNull() == null || licensePlate.isBlank()
                    if (!hasError) {
                        onAdd(make, model, year.toInt(), licensePlate)
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun VehicleCard(
    vehicle: Vehicle
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.DarkGray
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "${vehicle.year} ${vehicle.make} ${vehicle.model}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "License: ${vehicle.licensePlate}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            Icon(
                painter = painterResource(id = R.drawable.baseline_directions_car_24),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}