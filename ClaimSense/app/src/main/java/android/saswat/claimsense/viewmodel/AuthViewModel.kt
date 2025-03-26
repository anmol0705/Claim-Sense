package android.saswat.claimsense.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class UserData(
    val username: String = "",
    val email: String = "",
    val driverLicense: String = "",
    val userId: String = ""
)

sealed class UpdateState {
    object Idle : UpdateState()
    object Loading : UpdateState()
    object Success : UpdateState()
    data class Error(val message: String) : UpdateState()
}

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState

    private val _userData = MutableStateFlow<UserData?>(null)
    val userData: StateFlow<UserData?> = _userData

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState

    init {
        // Check if user is already signed in and fetch their data
        auth.currentUser?.let { user ->
            fetchUserData()
        }
    }

    fun updateUserData(newUsername: String, newDriverLicense: String) {
        viewModelScope.launch {
            try {
                _updateState.value = UpdateState.Loading

                val currentUser = auth.currentUser ?: throw Exception("User not authenticated")
                val userRef = firestore.collection("users").document(currentUser.uid)

                val updates = hashMapOf<String, Any>(
                    "username" to newUsername,
                    "driverLicense" to newDriverLicense
                )

                // Update Firestore
                userRef.update(updates).await()

                // Fetch the complete user data to ensure we have all fields
                val updatedDoc = userRef.get().await()
                val updatedUserData = updatedDoc.toObject(UserData::class.java)?.copy(
                    username = newUsername,
                    driverLicense = newDriverLicense
                )

                // Update local state
                _userData.value = updatedUserData

                _updateState.value = UpdateState.Success

            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error updating user data", e)
                _updateState.value = UpdateState.Error(e.message ?: "Failed to update user data")
            }
        }
    }


    fun fetchUserData() {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser ?: return@launch
                val document = firestore.collection("users").document(currentUser.uid).get().await()

                if (document.exists()) {
                    val userData = document.toObject(UserData::class.java)
                    _userData.value = userData?.copy(userId = currentUser.uid)
                } else {
                    Log.e("AuthViewModel", "No user document found for ID: ${currentUser.uid}")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error fetching user data: ${e.message}", e)
            }
        }
    }


    fun signInWithEmailPassword(email: String, password: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                val result = auth.signInWithEmailAndPassword(email, password).await()
                result.user?.let {
                    fetchUserData()
                    _authState.value = AuthState.Success
                    onComplete(true)
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Authentication failed")
                onComplete(false)
            }
        }
    }

    fun signUpWithEmailPassword(
        email: String,
        password: String,
        username: String,
        driverLicense: String,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                val uid = authResult.user?.uid ?: throw Exception("Failed to create user: No UID returned")

                val userData = UserData(
                    username = username,
                    email = email,
                    driverLicense = driverLicense,
                    userId = uid
                )

                firestore.collection("users").document(uid).set(userData)
                fetchUserData()
                _authState.value = AuthState.Success
                onComplete(true)
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Sign up failed: ${e.localizedMessage}", e)
                _authState.value = AuthState.Error(e.message ?: "Sign up failed")
                onComplete(false)
            }
        }
    }

    fun sendPasswordResetEmail(email: String) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                auth.sendPasswordResetEmail(email).await()
                _authState.value = AuthState.PasswordResetEmailSent
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Failed to send reset email")
            }
        }
    }

    fun signOut() {
        auth.signOut()
        _authState.value = AuthState.Initial
        _userData.value = null
        _updateState.value = UpdateState.Idle
    }

    fun resetUpdateState() {
        _updateState.value = UpdateState.Idle
    }

    sealed class AuthState {
        object Initial : AuthState()
        object Loading : AuthState()
        object Success : AuthState()
        object PasswordResetEmailSent : AuthState()
        data class Error(val message: String) : AuthState()
    }
}
