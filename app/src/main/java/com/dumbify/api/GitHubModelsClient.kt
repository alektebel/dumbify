package com.dumbify.api

import android.util.Log
import com.dumbify.auth.OAuthConfig
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/**
 * Client for GitHub Models API (powered by Azure AI)
 * 
 * Documentation: https://docs.github.com/en/github-models
 * 
 * Supports various models available through GitHub Copilot Pro:
 * - GPT-4o (OpenAI)
 * - GPT-4o mini (OpenAI) 
 * - Claude 3.5 Sonnet (Anthropic)
 * - Llama 3.1 (Meta)
 * - And more models as they become available
 */
class GitHubModelsClient(private val accessToken: String) {
    
    private val httpClient = OkHttpClient()
    private val gson = Gson()
    
    companion object {
        private const val TAG = "GitHubModelsClient"
        
        // Available models (check GitHub Models for current list)
        const val MODEL_GPT4O = "gpt-4o"
        const val MODEL_GPT4O_MINI = "gpt-4o-mini"
        const val MODEL_CLAUDE_SONNET = "claude-3.5-sonnet"
        const val MODEL_LLAMA_3_1 = "meta-llama-3.1-405b-instruct"
        const val MODEL_O1_PREVIEW = "o1-preview"
        const val MODEL_O1_MINI = "o1-mini"
        
        // Default model for thinking/reasoning tasks
        const val DEFAULT_MODEL = MODEL_GPT4O_MINI
    }
    
    /**
     * Sends a chat completion request to GitHub Models API
     * 
     * @param messages List of chat messages
     * @param model The model to use (default: gpt-4o-mini)
     * @param temperature Controls randomness (0-1, default: 0.7)
     * @param maxTokens Maximum tokens to generate
     * @return The model's response text
     */
    suspend fun chat(
        messages: List<ChatMessage>,
        model: String = DEFAULT_MODEL,
        temperature: Double = 0.7,
        maxTokens: Int = 1000
    ): String = withContext(Dispatchers.IO) {
        try {
            val requestBody = ChatRequest(
                model = model,
                messages = messages,
                temperature = temperature,
                max_tokens = maxTokens
            )
            
            val jsonBody = gson.toJson(requestBody)
            Log.d(TAG, "Sending request to GitHub Models: $model")
            
            val request = Request.Builder()
                .url("${OAuthConfig.GITHUB_MODELS_API_URL}/chat/completions")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Content-Type", "application/json")
                .build()
            
            // Properly close response using .use {}
            httpClient.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                
                if (!response.isSuccessful) {
                    Log.e(TAG, "GitHub Models API error: ${response.code} - $responseBody")
                    throw IOException("API request failed: ${response.code}")
                }
                
                if (responseBody == null) {
                    throw IOException("Empty response from API")
                }
                
                val chatResponse = gson.fromJson(responseBody, ChatResponse::class.java)
                    ?: throw IOException("Failed to parse response")
                    
                val content = chatResponse.choices.firstOrNull()?.message?.content
                    ?: throw IOException("No content in API response")
                
                Log.d(TAG, "Successfully received response from $model")
                content
            }
            
        } catch (e: IOException) {
            Log.e(TAG, "Network error communicating with GitHub Models", e)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing GitHub Models response", e)
            throw IOException("Failed to parse API response: ${e.message}")
        }
    }
    
    /**
     * Convenience method for simple text prompts
     */
    suspend fun complete(
        prompt: String,
        model: String = DEFAULT_MODEL,
        systemPrompt: String = "You are a helpful digital wellbeing assistant."
    ): String {
        val messages = listOf(
            ChatMessage(role = "system", content = systemPrompt),
            ChatMessage(role = "user", content = prompt)
        )
        return chat(messages, model)
    }
}

// Data classes for GitHub Models API

data class ChatMessage(
    val role: String, // "system", "user", or "assistant"
    val content: String
)

data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double = 0.7,
    @SerializedName("max_tokens")
    val max_tokens: Int = 1000
)

data class ChatResponse(
    val id: String,
    val choices: List<Choice>,
    val usage: Usage?
)

data class Choice(
    val index: Int,
    val message: ChatMessage,
    @SerializedName("finish_reason")
    val finish_reason: String?
)

data class Usage(
    @SerializedName("prompt_tokens")
    val prompt_tokens: Int,
    @SerializedName("completion_tokens")
    val completion_tokens: Int,
    @SerializedName("total_tokens")
    val total_tokens: Int
)
