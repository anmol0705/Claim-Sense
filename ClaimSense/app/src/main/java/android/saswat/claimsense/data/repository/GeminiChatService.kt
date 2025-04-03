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

    private val modelName = "gemini-2.0-flash"
    private val generativeModel by lazy {
        createGenerativeModel(null)
    }

    private val chatHistory = mutableListOf<Content>()

    private val baseSystemInstructionText = """
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
    """.trimIndent()


    private fun createSystemInstruction(additionalContext: String?): Content {
        val instructionText = if (additionalContext != null) {
            "$baseSystemInstructionText\n\nAdditional context: $additionalContext"
        } else {
            baseSystemInstructionText
        }
        return content { 
            text(instructionText)
        }
    }

    private fun createGenerativeModel(additionalContext: String?): GenerativeModel {
        Log.d(TAG, "Creating model with context: ${additionalContext?.take(100)}...")
        return GenerativeModel(
            modelName = modelName,
            apiKey = apiKey,
            generationConfig = generationConfig {
                temperature = 0.7f
                topK = 40
                topP = 0.95f
                maxOutputTokens = 1000
            },
            systemInstruction = createSystemInstruction(additionalContext)
        )
    }

    /**
     * Sends a message to Gemini and returns the response as a string.
     * 
     * @param message The user's message to send to Gemini
     * @param additionalContext Optional context to add to the system instruction
     * @return The generated response text
     */
    suspend fun sendMessage(message: String, additionalContext: String? = null): String {
        return try {
            Log.d(TAG, "Sending message to Gemini: $message")

            val modelToUse = if (additionalContext != null) {
                createGenerativeModel(additionalContext)
            } else {
                generativeModel
            }

            val userContent = content("user") { 
                text(message)
            }
            
            chatHistory.add(userContent)

            val chat = modelToUse.startChat(
                history = chatHistory
            )

            val response = chat.sendMessage(userContent) 

            val responseText = response.text ?: "Sorry, I couldn't generate a response."
            Log.d(TAG, "Received response from Gemini: ${responseText.take(50)}...")

            val modelResponse = content("model") {
                text(responseText)
            }
            chatHistory.add(modelResponse)

            responseText
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message to Gemini", e)
            "Sorry, I encountered an error: ${e.localizedMessage ?: e.toString()}"
        }
    }

    /**
     * Sends a message to Gemini and returns the response as a streaming flow of text chunks.
     * 
     * @param message The user's message to send to Gemini
     * @param additionalContext Optional context to add to the system instruction
     * @return Flow of text chunks from the generated response
     */
    fun sendMessageStream(message: String, additionalContext: String? = null): Flow<String> = flow {
        try {
            Log.d(TAG, "Starting streaming message to Gemini: $message")

            val modelToUse = if (additionalContext != null) {
                createGenerativeModel(additionalContext)
            } else {
                generativeModel
            }

            val userContent = content("user") { 
                text(message)
            }
            
            chatHistory.add(userContent)

            val chat = modelToUse.startChat(
                history = chatHistory
            )

            val responseStream = chat.sendMessageStream(userContent) 

            val fullResponse = StringBuilder()

            responseStream.collect { chunk ->
                chunk.text?.let { chunkText -> 
                    if (chunkText.isNotEmpty()) {
                        fullResponse.append(chunkText)
                        emit(chunkText) 
                    }
                }
            }
            
            val modelResponse = content("model") {
                text(fullResponse.toString())
            }
            chatHistory.add(modelResponse)

            Log.d(TAG, "Completed streaming response. Full length: ${fullResponse.length}")

        } catch (e: Exception) {
            val errorMessage = if (e is com.google.ai.client.generativeai.type.GoogleGenerativeAIException) {
                e.message 
            } else {
                e.localizedMessage ?: e.toString()
            }
            Log.e(TAG, "Error streaming message from Gemini: $errorMessage", e) 
            emit("Sorry, I encountered an error.") 
        }
    }

    fun clearChatHistory() {
        chatHistory.clear()
        Log.d(TAG, "Chat history cleared")
    }
}