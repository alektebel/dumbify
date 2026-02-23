package com.dumbify.api

import android.content.Context
import android.util.Log
import com.dumbify.repository.AppConfigRepository
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Interface for AI providers to abstract different AI services
 */
interface AiProvider {
    /**
     * Generates text completion from a prompt
     * @param prompt The user's prompt
     * @param systemPrompt Optional system prompt to guide the AI
     * @return Generated text response
     */
    suspend fun complete(prompt: String, systemPrompt: String = ""): String
    
    /**
     * Checks if the provider is properly configured
     */
    fun isConfigured(): Boolean
    
    /**
     * Returns the name of the provider for display
     */
    fun getProviderName(): String
}

/**
 * Gemini AI provider using Google's Generative AI SDK
 */
class GeminiProvider(context: Context) : AiProvider {
    
    // Use application context to avoid memory leaks
    private val appContext = context.applicationContext
    private val repository = AppConfigRepository(appContext)
    
    private val model by lazy {
        val apiKey = repository.geminiApiKey ?: ""
        GenerativeModel(
            modelName = "gemini-pro",
            apiKey = apiKey
        )
    }
    
    companion object {
        private const val TAG = "GeminiProvider"
    }
    
    override suspend fun complete(prompt: String, systemPrompt: String): String = withContext(Dispatchers.IO) {
        try {
            // Gemini doesn't have separate system prompts, so we combine them
            val fullPrompt = if (systemPrompt.isNotEmpty()) {
                "$systemPrompt\n\n$prompt"
            } else {
                prompt
            }
            
            val response = model.generateContent(fullPrompt)
            response.text ?: "Unable to generate response"
        } catch (e: Exception) {
            Log.e(TAG, "Error generating content with Gemini", e)
            throw e
        }
    }
    
    override fun isConfigured(): Boolean {
        val apiKey = repository.geminiApiKey ?: ""
        return apiKey.isNotEmpty()
    }
    
    override fun getProviderName(): String = "Google Gemini"
}

/**
 * GitHub Models AI provider using GitHub Copilot Pro subscription
 */
class GitHubModelsProvider(context: Context) : AiProvider {
    
    // Use application context to avoid memory leaks
    private val appContext = context.applicationContext
    private val repository = AppConfigRepository(appContext)
    
    private val client by lazy {
        val token = repository.githubAccessToken ?: ""
        GitHubModelsClient(token)
    }
    
    companion object {
        private const val TAG = "GitHubModelsProvider"
    }
    
    override suspend fun complete(prompt: String, systemPrompt: String): String {
        try {
            val messages = mutableListOf<ChatMessage>()
            
            if (systemPrompt.isNotEmpty()) {
                messages.add(ChatMessage(role = "system", content = systemPrompt))
            }
            
            messages.add(ChatMessage(role = "user", content = prompt))
            
            // Use GPT-4o-mini for quick thinking tasks, or upgrade to GPT-4o/o1 for complex reasoning
            return client.chat(
                messages = messages,
                model = GitHubModelsClient.MODEL_GPT4O_MINI,
                temperature = 0.7,
                maxTokens = 500
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error generating content with GitHub Models", e)
            throw e
        }
    }
    
    override fun isConfigured(): Boolean {
        return !repository.githubAccessToken.isNullOrEmpty()
    }
    
    override fun getProviderName(): String = "GitHub Copilot (${GitHubModelsClient.MODEL_GPT4O_MINI})"
}

/**
 * Factory for creating AI provider instances
 */
object AiProviderFactory {
    fun createProvider(context: Context): AiProvider {
        val repository = AppConfigRepository(context)
        val selectedProvider = repository.selectedAiProvider
        
        return when (selectedProvider) {
            "github" -> GitHubModelsProvider(context)
            else -> GeminiProvider(context)
        }
    }
    
    fun getAvailableProviders(context: Context): List<Pair<String, String>> {
        // Returns list of (id, displayName) pairs
        val providers = mutableListOf<Pair<String, String>>()
        
        val gemini = GeminiProvider(context)
        if (gemini.isConfigured()) {
            providers.add("gemini" to gemini.getProviderName())
        }
        
        val github = GitHubModelsProvider(context)
        if (github.isConfigured()) {
            providers.add("github" to github.getProviderName())
        }
        
        return providers
    }
}
