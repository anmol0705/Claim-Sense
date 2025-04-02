package android.saswat.claimsense.data.repository


import android.saswat.claimsense.BuildConfig
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiChatService @Inject constructor() {
    private val apiKey = BuildConfig.GEMINI_API_KEY
    private val TAG = "GeminiChatService"

    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-2.0-flash",
            apiKey = apiKey,
            generationConfig = generationConfig {
                temperature = 0.7f
                topK = 40
                topP = 0.95f
                maxOutputTokens = 1000
            }
        )
    }

    private val chatHistory = mutableListOf<Content>()
    
    private val systemInstruction = content {
        role = "system"
        text("""
            You are a helpful AI assistant for ClaimSense, an insurance claims app. 
            Your primary role is to help users understand their risk scores and provide 
            insights about their driving patterns.
            
            When discussing risk scores:
            - High scores (70-100) indicate safe driving habits
            - Medium scores (40-70) suggest moderate risk
            - Low scores (0-40) indicate high-risk behaviors
            
            When users ask about their driving patterns or risk factors:
            - Explain how various sensor readings relate to driving behaviors
            - Provide specific, actionable advice for improving safer driving habits
            - Be supportive and encouraging, not judgmental
            
            For accelerometer data:
            - High X values suggest harsh lateral movements or sharp turns
            - High Y values suggest harsh acceleration or sudden braking
            - High Z values suggest rough road conditions or bumpy driving
            
            For gyroscope data:
            - High values indicate sharp turns or unstable driving patterns
            
            If the user asks about specific claims processes, explain that they should:
            1. Document the incident with photos
            2. Contact their insurance agent
            3. File a report in the app
            
            Keep your responses friendly, concise (under 150 words), and focused on insurance and driving safety topics.
        """.trimIndent())
    }

    // Sends a message to Gemini and returns the response
    suspend fun sendMessage(message: String, additionalContext: String? = null): String {
        return try {
            Log.d(TAG, "Sending message to Gemini: $message")

            // Reset chat history to start fresh on each message
            chatHistory.clear()
            
            // Create system instruction, combining with additional context if provided
            val finalSystemInstruction = if (additionalContext != null) {
                val baseText = systemInstruction.parts.joinToString("") { 
                    (it as? com.google.ai.client.generativeai.type.TextPart)?.text ?: ""
                }
                content {
                    role = "system"
                    text("$baseText\n\nAdditional context: $additionalContext")
                }
            } else {
                systemInstruction
            }
            
            // Add the combined system instruction
            chatHistory.add(finalSystemInstruction)

            // Add user message to history
            val userContent = content {
                text(message)
                role = "user"
            }
            chatHistory.add(userContent)

            // Create a chat session with history for better context management
            val chat = generativeModel.startChat(history = chatHistory.toList())
            
            // Send the message and get response
            val response = chat.sendMessage(message)

            // Extract the response text
            val responseText = response.text ?: "Sorry, I couldn't generate a response."
            Log.d(TAG, "Received response from Gemini: ${responseText.take(50)}...")

            // Add model response to history
            val modelContent = content {
                text(responseText)
                role = "model"
            }
            chatHistory.add(modelContent)

            responseText
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message to Gemini", e)
            "Sorry, I encountered an error: ${e.message}"
        }
    }

    // Streaming version for real-time responses
    fun sendMessageStream(message: String, additionalContext: String? = null): Flow<String> = flow {
        try {
            Log.d(TAG, "Starting streaming message to Gemini: $message")

            // Reset chat history to start fresh on each message
            chatHistory.clear()
            
            // Create system instruction, combining with additional context if provided
            val finalSystemInstruction = if (additionalContext != null) {
                val baseText = systemInstruction.parts.joinToString("") { 
                    (it as? com.google.ai.client.generativeai.type.TextPart)?.text ?: ""
                }
                content {
                    role = "system"
                    text("$baseText\n\nAdditional context: $additionalContext")
                }
            } else {
                systemInstruction
            }
            
            // Add the combined system instruction
            chatHistory.add(finalSystemInstruction)

            // Add user message to history
            val userContent = content {
                text(message)
                role = "user"
            }
            chatHistory.add(userContent)

            // Create a chat session with history for better context management
            val chat = generativeModel.startChat(history = chatHistory.toList())
            
            // Stream the response
            val responseStream = chat.sendMessageStream(message)

            val fullResponse = StringBuilder()

            responseStream.collect { chunk ->
                val chunkText = chunk.text ?: ""
                fullResponse.append(chunkText)
                emit(chunkText)
            }

            // Add model response to history
            val modelContent = content {
                text(fullResponse.toString())
                role = "model"
            }
            chatHistory.add(modelContent)

            Log.d(TAG, "Completed streaming response")
        } catch (e: Exception) {
            Log.e(TAG, "Error streaming message from Gemini", e)
            emit("Sorry, I encountered an error: ${e.message}")
        }
    }

    // Clear chat history
    fun clearChatHistory() {
        chatHistory.clear()
        Log.d(TAG, "Chat history cleared")
    }
}
