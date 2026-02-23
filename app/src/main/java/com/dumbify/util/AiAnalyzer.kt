package com.dumbify.util

import android.content.Context
import android.util.Log
import com.dumbify.api.AiProviderFactory
import com.dumbify.model.AppUsageData
import com.dumbify.model.DailyStats
import com.dumbify.repository.AppConfigRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AiAnalyzer(private val context: Context) {
    
    private val repository = AppConfigRepository(context)
    
    // Get the appropriate AI provider based on user selection
    private val provider by lazy {
        AiProviderFactory.createProvider(context)
    }
    
    companion object {
        private const val TAG = "AiAnalyzer"
    }
    
    /**
     * Sets the Gemini API key (for backwards compatibility)
     */
    fun setApiKey(key: String) {
        val prefs = context.getSharedPreferences("dumbify_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("gemini_api_key", key).apply()
    }
    
    /**
     * Checks if any AI provider is configured
     */
    fun isConfigured(): Boolean {
        return provider.isConfigured()
    }
    
    suspend fun analyzeUsagePattern(
        currentApp: String,
        usageData: AppUsageData,
        recentSessions: List<String>
    ): String {
        if (!provider.isConfigured()) {
            return "AI analysis disabled. Please configure AI provider in settings."
        }
        
        return withContext(Dispatchers.IO) {
            try {
                val prompt = buildUsagePrompt(currentApp, usageData, recentSessions)
                val systemPrompt = "You are a digital wellbeing assistant. Provide brief, actionable feedback to help users maintain healthy digital habits."
                provider.complete(prompt, systemPrompt)
            } catch (e: Exception) {
                Log.e(TAG, "AI analysis failed", e)
                "AI analysis unavailable: ${e.message}"
            }
        }
    }
    
    private fun buildUsagePrompt(
        currentApp: String,
        usageData: AppUsageData,
        recentSessions: List<String>
    ): String {
        val usageMinutes = usageData.usageTimeMillis / 60000
        return """
            Analyze this app usage and provide brief, actionable feedback.
            
            Current App: $currentApp
            Time spent today: $usageMinutes minutes
            Times opened: ${usageData.openCount}
            Recent app switches: ${recentSessions.takeLast(5).joinToString(", ")}
            
            Provide a short (2-3 sentences) assessment:
            1. Is this usage productive or potentially problematic?
            2. Any patterns suggesting distraction or procrastination?
            3. One actionable suggestion to improve focus.
            
            Keep the tone encouraging but honest. Be concise.
        """.trimIndent()
    }
    
    suspend fun generateDailyInsight(stats: DailyStats): String {
        if (!provider.isConfigured()) {
            return generateBasicInsight(stats)
        }
        
        return withContext(Dispatchers.IO) {
            try {
                val prompt = buildDailyPrompt(stats)
                val systemPrompt = "You are a digital wellbeing coach providing daily insights."
                provider.complete(prompt, systemPrompt)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate daily insight", e)
                generateBasicInsight(stats)
            }
        }
    }
    
    private fun buildDailyPrompt(stats: DailyStats): String {
        val totalHours = stats.totalScreenTime / 3600000.0
        val productiveHours = stats.productiveTime / 3600000.0
        val distractingHours = stats.distractingTime / 3600000.0
        
        return """
            Provide a brief daily digital wellbeing summary for this user.
            
            Total screen time: %.1f hours
            Productive time: %.1f hours
            Distracting time: %.1f hours
            Top apps: ${stats.topApps.take(3).joinToString(", ") { it.appName }}
            
            Provide:
            1. A one-sentence summary of the day
            2. One specific insight about their usage pattern
            3. One actionable suggestion for tomorrow
            
            Keep it under 100 words, encouraging but truthful.
        """.trimIndent().format(totalHours, productiveHours, distractingHours)
    }
    
    private fun generateBasicInsight(stats: DailyStats): String {
        val totalHours = stats.totalScreenTime / 3600000.0
        val productivePercent = if (stats.totalScreenTime > 0) {
            (stats.productiveTime * 100.0 / stats.totalScreenTime).toInt()
        } else 0
        
        return when {
            productivePercent >= 70 -> 
                "Great work! ${productivePercent}% of your ${String.format("%.1f", totalHours)} hours was productive time."
            productivePercent >= 50 -> 
                "Decent balance. ${productivePercent}% productive out of ${String.format("%.1f", totalHours)} hours. Room for improvement."
            else -> 
                "Only ${productivePercent}% productive today. Consider reducing time on ${stats.topApps.firstOrNull()?.appName ?: "distracting apps"}."
        }
    }
    
    suspend fun shouldWarnUser(
        appName: String,
        currentUsageMinutes: Int,
        threshold: Int
    ): Pair<Boolean, String> {
        val shouldWarn = currentUsageMinutes >= threshold
        
        if (!shouldWarn) return Pair(false, "")
        
        val message = if (provider.isConfigured()) {
            try {
                val prompt = """
                    The user has been on $appName for $currentUsageMinutes minutes (threshold: $threshold).
                    Generate a SHORT (one sentence) motivational message to help them refocus.
                    Be supportive but direct. No emojis.
                """.trimIndent()
                
                val systemPrompt = "You are a supportive digital wellbeing assistant."
                
                withContext(Dispatchers.IO) {
                    provider.complete(prompt, systemPrompt)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate warning", e)
                getDefaultWarning(appName, currentUsageMinutes)
            }
        } else {
            getDefaultWarning(appName, currentUsageMinutes)
        }
        
        return Pair(true, message)
    }
    
    private fun getDefaultWarning(appName: String, minutes: Int): String {
        return "You've been using $appName for $minutes minutes. Time to focus on something productive."
    }
}
