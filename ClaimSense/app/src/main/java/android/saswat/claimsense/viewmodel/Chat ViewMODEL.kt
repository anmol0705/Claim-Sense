package android.saswat.claimsense.viewmodel

import android.app.Application
import android.saswat.claimsense.data.repository.GeminiChatService

import android.saswat.claimsense.ui.components.ChatMessage
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    // Instantiate our service
    private val geminiChatService = GeminiChatService()

    private val _chatState = MutableStateFlow<ChatState>(ChatState.Initial)
    val chatState: StateFlow<ChatState> = _chatState.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _riskData = MutableStateFlow<Map<String, Any>?>(null)
    val riskData: StateFlow<Map<String, Any>?> = _riskData.asStateFlow()

    init {
        loadChatHistory()
        fetchRiskData()
    }

    fun sendMessage(userMessage: String) {
        if (userMessage.isBlank()) return

        viewModelScope.launch {
            try {
                _isLoading.value = true

                // Add user message to the chat
                val timestamp = System.currentTimeMillis()
                val userChatMessage = ChatMessage(
                    id = timestamp.toString(),
                    text = userMessage,
                    isUser = true,
                    timestamp = timestamp
                )

                _messages.value = _messages.value + userChatMessage
                saveChatMessage(userChatMessage)

                try {
                    // Create a placeholder for the AI response (will be updated with streaming)
                    val aiTimestamp = System.currentTimeMillis()
                    val initialAiMessage = ChatMessage(
                        id = aiTimestamp.toString(),
                        text = "",
                        isUser = false,
                        timestamp = aiTimestamp
                    )
                    
                    // Add initial empty message
                    _messages.value = _messages.value + initialAiMessage
                    
                    // Use streaming for more responsive UI
                    val riskContext = buildRiskContext()
                    android.util.Log.d("ChatViewModel", "Sending message with risk context")
                    val responseBuilder = StringBuilder()
                    
                    // Always pass the risk context with each message for consistency
                    geminiChatService.sendMessageStream(userMessage, riskContext).collect { chunk ->
                        responseBuilder.append(chunk)
                        
                        // Update the current message content with the new text
                        val updatedMessage = initialAiMessage.copy(text = responseBuilder.toString())
                        val currentMessages = _messages.value.toMutableList()
                        val lastMessageIndex = currentMessages.size - 1
                        currentMessages[lastMessageIndex] = updatedMessage
                        _messages.value = currentMessages
                    }
                    
                    // After stream completes, save the final message
                    val finalMessage = _messages.value.last()
                    saveChatMessage(finalMessage)
                    
                    // Log success
                    android.util.Log.d("ChatViewModel", "Successfully processed message")
                } catch (e: Exception) {
                    // Handle API errors with fallback message
                    android.util.Log.e("ChatViewModel", "Chat error: ${e.message}", e)
                    
                    val aiTimestamp = System.currentTimeMillis()
                    val errorMessage = ChatMessage(
                        id = aiTimestamp.toString(),
                        text = "Sorry, I'm having trouble connecting. Please check your internet connection.",
                        isUser = false,
                        timestamp = aiTimestamp
                    )
                    _messages.value = _messages.value + errorMessage
                    saveChatMessage(errorMessage)
                }

                _chatState.value = ChatState.Success
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "General error: ${e.message}", e)
                _chatState.value = ChatState.Error(e.message ?: "An error occurred")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadChatHistory() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val userId = auth.currentUser?.uid ?: return@launch

                val messages = firestore.collection("users")
                    .document(userId)
                    .collection("chat_messages")
                    .orderBy("timestamp")
                    .get()
                    .await()
                    .documents
                    .mapNotNull { doc ->
                        val data = doc.data ?: return@mapNotNull null
                        ChatMessage(
                            id = doc.id,
                            text = data["text"] as? String ?: "",
                            isUser = data["isUser"] as? Boolean ?: false,
                            timestamp = data["timestamp"] as? Long ?: 0
                        )
                    }

                _messages.value = messages
                _chatState.value = ChatState.Success
            } catch (e: Exception) {
                _chatState.value = ChatState.Error(e.message ?: "Failed to load chat history")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun saveChatMessage(message: ChatMessage) {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch

                firestore.collection("users")
                    .document(userId)
                    .collection("chat_messages")
                    .document(message.id)
                    .set(mapOf(
                        "text" to message.text,
                        "isUser" to message.isUser,
                        "timestamp" to message.timestamp
                    ))
            } catch (e: Exception) {
                // Handle error silently or notify user
            }
        }
    }

    private fun fetchRiskData() {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch

                val riskDoc = firestore.collection("users")
                    .document(userId)
                    .collection("risk_scores")
                    .document("latest")
                    .get()
                    .await()

                if (riskDoc.exists()) {
                    _riskData.value = riskDoc.data
                    android.util.Log.d("ChatViewModel", "Risk data updated: ${riskDoc.data}")
                    
                    // Just update the risk context without sending a message
                }
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }

    private fun buildRiskContext(): String {
        val riskData = _riskData.value ?: return "You are an assistant for ClaimSense, an insurance claims app."

        val riskScore = riskData["riskScore"] as? Double ?: 0.0
        val accelerometerData = riskData["accelerometer"] as? Map<*, *>
        val gyroscopeData = riskData["gyroscope"] as? Map<*, *>

        android.util.Log.d("ChatViewModel", "Building context with risk score: $riskScore")
        
        return """
            You are an assistant for ClaimSense, an insurance claims app.
            The user's current risk score is $riskScore out of 100.
            ${if (accelerometerData != null) "Recent accelerometer readings: X=${accelerometerData["x"]}, Y=${accelerometerData["y"]}, Z=${accelerometerData["z"]}" else ""}
            ${if (gyroscopeData != null) "Recent gyroscope readings: X=${gyroscopeData["x"]}, Y=${gyroscopeData["y"]}, Z=${gyroscopeData["z"]}" else ""}
            
            If the user asks about their driving or risk score:
            - If risk score > 70, emphasize their good driving habits
            - If risk score is between 40-70, suggest moderate improvements
            - If risk score < 40, identify specific concerns based on sensor data
            
            For accelerometer data:
            - High X values (> 5) suggest harsh lateral movements
            - High Y values (> 5) suggest harsh acceleration/braking
            - High Z values (> 5) suggest rough road or bumpy driving
            
            For gyroscope data:
            - High values suggest sharp turns or unstable driving
            
            Keep responses helpful, specific to their data, and under 150 words.
            
            Remember user information across the conversation, including their name if they introduced themselves.
        """.trimIndent()
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val userId = auth.currentUser?.uid ?: return@launch

                // Delete all chat messages from Firestore
                val messages = firestore.collection("users")
                    .document(userId)
                    .collection("chat_messages")
                    .get()
                    .await()

                for (doc in messages.documents) {
                    doc.reference.delete().await()
                }

                // Clear local messages
                _messages.value = emptyList()
                
                // Clear Gemini chat history
                geminiChatService.clearChatHistory()
                
                _chatState.value = ChatState.Success
            } catch (e: Exception) {
                _chatState.value = ChatState.Error(e.message ?: "Failed to clear chat history")
            } finally {
                _isLoading.value = false
            }
        }
    }

    sealed class ChatState {
        object Initial : ChatState()
        object Success : ChatState()
        data class Error(val message: String) : ChatState()
    }
}