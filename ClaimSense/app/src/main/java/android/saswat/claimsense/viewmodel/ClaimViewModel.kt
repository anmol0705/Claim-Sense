package android.saswat.claimsense.viewmodel

import android.saswat.claimsense.data.model.Claim
import android.saswat.claimsense.data.model.ClaimStatus
import android.saswat.claimsense.data.repository.FirestoreRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.UUID

class ClaimViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestoreRepository = FirestoreRepository()

    private val _claims = MutableStateFlow<List<Claim>>(emptyList())
    val claims: StateFlow<List<Claim>> = _claims

    private val _state = MutableStateFlow<ClaimState>(ClaimState.Initial)
    val state: StateFlow<ClaimState> = _state

    init {
        loadClaims()
    }

    fun createClaim(
        vehicleId: String,
        description: String,
        amount: Double,
        photos: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            try {
                _state.value = ClaimState.Loading
                val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
                
                val claim = Claim(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    vehicleId = vehicleId,
                    description = description,
                    amount = amount,
                    photos = photos
                )

                firestoreRepository.createClaim(claim)
                _state.value = ClaimState.Success
                loadClaims() // Refresh the claims list
            } catch (e: Exception) {
                _state.value = ClaimState.Error(e.message ?: "Failed to create claim")
            }
        }
    }

    fun updateClaimStatus(claimId: String, newStatus: ClaimStatus) {
        viewModelScope.launch {
            try {
                _state.value = ClaimState.Loading
                firestoreRepository.updateClaimStatus(claimId, newStatus)
                _state.value = ClaimState.Success
                loadClaims() // Refresh the claims list
            } catch (e: Exception) {
                _state.value = ClaimState.Error(e.message ?: "Failed to update claim status")
            }
        }
    }

    private fun loadClaims() {
        viewModelScope.launch {
            try {
                _state.value = ClaimState.Loading
                val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
                
                firestoreRepository.getUserClaims(userId)
                    .catch { e ->
                        _state.value = ClaimState.Error(e.message ?: "Failed to load claims")
                    }
                    .collect { claims ->
                        _claims.value = claims
                        _state.value = ClaimState.Success
                    }
            } catch (e: Exception) {
                _state.value = ClaimState.Error(e.message ?: "Failed to load claims")
            }
        }
    }

    sealed class ClaimState {
        object Initial : ClaimState()
        object Loading : ClaimState()
        object Success : ClaimState()
        data class Error(val message: String) : ClaimState()
    }
}