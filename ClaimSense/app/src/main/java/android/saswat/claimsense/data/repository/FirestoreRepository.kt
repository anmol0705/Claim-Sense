package android.saswat.claimsense.data.repository

import android.saswat.claimsense.data.model.User
import android.saswat.claimsense.data.model.Vehicle
import android.saswat.claimsense.data.model.Claim
import android.saswat.claimsense.data.model.ClaimStatus
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log

class FirestoreRepository {
    private val db = FirebaseFirestore.getInstance()
    
    // Collection references
    private val usersCollection = db.collection("users")
    private val vehiclesCollection = db.collection("vehicles")
    private val claimsCollection = db.collection("claims")

    // User operations
    suspend fun createOrUpdateUser(user: User) = withContext(Dispatchers.IO) {
        usersCollection.document(user.userId)
            .set(user, SetOptions.merge())
            .await()
    }

    suspend fun getUser(userId: String): User? = withContext(Dispatchers.IO) {
        usersCollection.document(userId)
            .get()
            .await()
            .toObject(User::class.java)
    }

    // Vehicle operations
    suspend fun addVehicle(vehicle: Vehicle) = withContext(Dispatchers.IO) {
        try {
            Log.d("FirestoreRepository", "Adding vehicle to Firestore: $vehicle")
            Log.d("FirestoreRepository", "isPaid status: ${vehicle.isPaid}")
            
            // Convert to map to ensure all fields are properly set
            val vehicleMap = mapOf(
                "id" to vehicle.id,
                "userId" to vehicle.userId,
                "make" to vehicle.make,
                "model" to vehicle.model,
                "year" to vehicle.year,
                "licensePlate" to vehicle.licensePlate,
                "isPaid" to vehicle.isPaid,
                "createdAt" to vehicle.createdAt,
                "updatedAt" to vehicle.updatedAt
            )
            
            vehiclesCollection.document(vehicle.id)
                .set(vehicleMap)
                .await()
            Log.d("FirestoreRepository", "Vehicle added successfully with isPaid=${vehicle.isPaid}")
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Error adding vehicle", e)
            throw e
        }
    }

    fun getUserVehicles(userId: String): Flow<List<Vehicle>> = flow {
        Log.d("FirestoreRepository", "Getting vehicles for user: $userId")
        val snapshot = vehiclesCollection
            .whereEqualTo("userId", userId)
            .get()
            .await()
        
        val vehicles = snapshot.toObjects(Vehicle::class.java)
        vehicles.forEach { vehicle ->
            Log.d("FirestoreRepository", "Retrieved vehicle: ${vehicle.make} ${vehicle.model}, isPaid=${vehicle.isPaid}")
        }
        emit(vehicles)
    }

    // Claim operations
    suspend fun createClaim(claim: Claim): Void? = withContext(Dispatchers.IO) {
        claimsCollection.document(claim.id)
            .set(claim)
            .await()
    }

    fun getUserClaims(userId: String): Flow<List<Claim>> = flow {
        val snapshot = claimsCollection
            .whereEqualTo("userId", userId)
            .get()
            .await()
        
        val claims = snapshot.toObjects(Claim::class.java)
        emit(claims)
    }

    suspend fun updateClaimStatus(claimId: String, status: ClaimStatus): Void? = withContext(Dispatchers.IO) {
        claimsCollection.document(claimId)
            .update("status", status)
            .await()
    }
    
    // New direct method to update vehicle payment status
    suspend fun updateVehiclePaymentStatus(vehicleId: String, isPaid: Boolean): Void? = withContext(Dispatchers.IO) {
        try {
            Log.d("FirestoreRepository", "Updating vehicle payment status: $vehicleId to $isPaid")
            vehiclesCollection.document(vehicleId)
                .update("isPaid", isPaid)
                .await()
            Log.d("FirestoreRepository", "Vehicle payment status updated successfully")
            return@withContext null
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Error updating vehicle payment status", e)
            throw e
        }
    }
}