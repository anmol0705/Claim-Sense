package android.saswat.claimsense.viewmodel

import android.saswat.claimsense.data.model.Vehicle
import android.saswat.claimsense.data.repository.FirestoreRepository
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.UUID

class VehicleViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestoreRepository = FirestoreRepository()

    private val _vehicles = MutableStateFlow<List<Vehicle>>(emptyList())
    val vehicles: StateFlow<List<Vehicle>> = _vehicles

    private val _state = MutableStateFlow<VehicleState>(VehicleState.Initial)
    val state: StateFlow<VehicleState> = _state

    init {
        loadVehicles()
    }

    fun addVehicle(
        make: String,
        model: String,
        year: Int,
        licensePlate: String,
        isPaid: Boolean = false
    ) {
        viewModelScope.launch {
            try {
                Log.d("VehicleViewModel", "Starting to add vehicle: $make $model $year $licensePlate, paid: $isPaid")
                _state.value = VehicleState.Loading
                
                val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
                Log.d("VehicleViewModel", "Current user ID: $userId")
                
                val vehicle = Vehicle(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    make = make,
                    model = model,
                    year = year,
                    licensePlate = licensePlate,
                    isPaid = isPaid
                )
                Log.d("VehicleViewModel", "Created vehicle object: $vehicle")

                firestoreRepository.addVehicle(vehicle)
                Log.d("VehicleViewModel", "Vehicle added to Firestore successfully")
                
                // Update the local list immediately for better UI response
                val currentList = _vehicles.value.toMutableList()
                currentList.add(vehicle)
                _vehicles.value = currentList
                
                _state.value = VehicleState.Success
                Log.d("VehicleViewModel", "State updated to Success")
                
                loadVehicles() // Refresh the vehicles list to ensure consistency
                Log.d("VehicleViewModel", "Triggered vehicles reload")
            } catch (e: Exception) {
                Log.e("VehicleViewModel", "Error adding vehicle", e)
                _state.value = VehicleState.Error(e.message ?: "Failed to add vehicle")
            }
        }
    }

    fun updatePaymentStatus(vehicleId: String, isPaid: Boolean) {
        viewModelScope.launch {
            try {
                Log.d("VehicleViewModel", "Updating payment status for vehicle $vehicleId to $isPaid")
                _state.value = VehicleState.Loading
                
                val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
                
                // Get the current vehicle
                val vehicle = vehicles.value.find { it.id == vehicleId }
                    ?: throw Exception("Vehicle not found")
                
                Log.d("VehicleViewModel", "Found vehicle: ${vehicle.make} ${vehicle.model}, current isPaid=${vehicle.isPaid}")
                
                // Update directly in Firestore using the dedicated method
                firestoreRepository.updateVehiclePaymentStatus(vehicleId, isPaid)
                
                // Create updated vehicle with new payment status for local update
                val updatedVehicle = vehicle.copy(
                    isPaid = isPaid,
                    updatedAt = System.currentTimeMillis()
                )
                
                Log.d("VehicleViewModel", "Created updated vehicle with isPaid=$isPaid")
                
                // Update the local list immediately for better UI response
                val currentList = _vehicles.value.toMutableList()
                val indexToUpdate = currentList.indexOfFirst { it.id == vehicleId }
                if (indexToUpdate != -1) {
                    currentList[indexToUpdate] = updatedVehicle
                    _vehicles.value = currentList
                    Log.d("VehicleViewModel", "Updated local list with new payment status")
                } else {
                    Log.w("VehicleViewModel", "Vehicle not found in local list for updating")
                }
                
                _state.value = VehicleState.Success
                
                // Load vehicles after a short delay to ensure Firestore has updated
                viewModelScope.launch {
                    kotlinx.coroutines.delay(500) // Wait for 500ms
                    loadVehicles()
                }
            } catch (e: Exception) {
                Log.e("VehicleViewModel", "Error updating payment status", e)
                _state.value = VehicleState.Error(e.message ?: "Failed to update payment status")
            }
        }
    }

    private fun loadVehicles() {
        viewModelScope.launch {
            try {
                Log.d("VehicleViewModel", "Starting to load vehicles")
                _state.value = VehicleState.Loading
                
                val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
                Log.d("VehicleViewModel", "Loading vehicles for user: $userId")
                
                firestoreRepository.getUserVehicles(userId)
                    .catch { e ->
                        Log.e("VehicleViewModel", "Error in flow", e)
                        _state.value = VehicleState.Error(e.message ?: "Failed to load vehicles")
                    }
                    .collect { vehicles ->
                        Log.d("VehicleViewModel", "Received ${vehicles.size} vehicles")
                        
                        // Log each vehicle's details, especially the isPaid status
                        vehicles.forEach { vehicle ->
                            Log.d("VehicleViewModel", "Vehicle in collection: ${vehicle.id}, ${vehicle.make} ${vehicle.model}, isPaid=${vehicle.isPaid}")
                        }
                        
                        _vehicles.value = vehicles
                        _state.value = VehicleState.Success
                    }
            } catch (e: Exception) {
                Log.e("VehicleViewModel", "Error loading vehicles", e)
                _state.value = VehicleState.Error(e.message ?: "Failed to load vehicles")
            }
        }
    }

    sealed class VehicleState {
        object Initial : VehicleState()
        object Loading : VehicleState()
        object Success : VehicleState()
        data class Error(val message: String) : VehicleState()
    }
}