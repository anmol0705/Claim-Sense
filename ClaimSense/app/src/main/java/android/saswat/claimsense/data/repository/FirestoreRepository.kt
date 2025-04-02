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
            vehiclesCollection.document(vehicle.id)
                .set(vehicle)
                .await()
            Log.d("FirestoreRepository", "Vehicle added successfully")
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
}