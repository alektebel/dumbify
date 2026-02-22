package com.dumbify.util

import android.content.Context
import com.dumbify.model.AppUsageData
import com.dumbify.model.DailyStats
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AiAnalyzer(private val context: Context) {
    
    // Note: Users need to add their own API key in a secure manner
    // This should be stored in local.properties or secure storage
    private val apiKey = getApiKey()
    
    private val model by lazy {
        GenerativeModel(
            modelName = "gemini-pro",
            apiKey = apiKey
        )
    }
    
    private fun getApiKey(): String {
        // Try to get from resources or return empty
        // Users should add their API key
        val prefs = context.getSharedPreferences("dumbify_prefs", Context.MODE_PRIVATE)
        return prefs.getString("gemini_api_key", "") ?: ""
    }
    
    fun setApiKey(key: String) {
        val prefs = context.getSharedPreferences("dumbify_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("gemini_api_key", key).apply()
    }
    
    suspend fun analyzeUsagePattern(
        currentApp: String,
        usageData: AppUsageData,
        recentSessions: List<String>
    ): String {
        if (apiKey.isEmpty()) {
            return "AI analysis disabled. Please configure API key in settings."
        }
        
        return withContext(Dispatchers.IO) {
            try {
                val prompt = buildUsagePrompt(currentApp, usageData, recentSessions)
                val response = model.generateContent(prompt)
                response.text ?: "Unable to analyze usage pattern"
            } catch (e: Exception) {
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
            You are a digital wellbeing assistant. Analyze this app usage and provide brief, actionable feedback.
            
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
        if (apiKey.isEmpty()) {
            return generateBasicInsight(stats)
        }
        
        return withContext(Dispatchers.IO) {
            try {
                val prompt = buildDailyPrompt(stats)
                val response = model.generateContent(prompt)
                response.text ?: generateBasicInsight(stats)
            } catch (e: Exception) {
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
        
        val message = if (apiKey.isNotEmpty()) {
            try {
                val prompt = """
                    The user has been on $appName for $currentUsageMinutes minutes (threshold: $threshold).
                    Generate a SHORT (one sentence) motivational message to help them refocus.
                    Be supportive but direct. No emojis.
                """.trimIndent()
                
                withContext(Dispatchers.IO) {
                    model.generateContent(prompt).text ?: getDefaultWarning(appName, currentUsageMinutes)
                }
            } catch (e: Exception) {
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
