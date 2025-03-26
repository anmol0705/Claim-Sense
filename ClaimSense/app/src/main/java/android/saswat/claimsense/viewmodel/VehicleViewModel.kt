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
        licensePlate: String
    ) {
        viewModelScope.launch {
            try {
                Log.d("VehicleViewModel", "Starting to add vehicle: $make $model $year $licensePlate")
                _state.value = VehicleState.Loading
                
                val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
                Log.d("VehicleViewModel", "Current user ID: $userId")
                
                val vehicle = Vehicle(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    make = make,
                    model = model,
                    year = year,
                    licensePlate = licensePlate
                )
                Log.d("VehicleViewModel", "Created vehicle object: $vehicle")

                firestoreRepository.addVehicle(vehicle)
                Log.d("VehicleViewModel", "Vehicle added to Firestore successfully")
                
                _state.value = VehicleState.Success
                Log.d("VehicleViewModel", "State updated to Success")
                
                loadVehicles() // Refresh the vehicles list
                Log.d("VehicleViewModel", "Triggered vehicles reload")
            } catch (e: Exception) {
                Log.e("VehicleViewModel", "Error adding vehicle", e)
                _state.value = VehicleState.Error(e.message ?: "Failed to add vehicle")
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