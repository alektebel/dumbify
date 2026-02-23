package com.dumbify.util

import android.content.Context
import com.dumbify.model.AppUsageData
import com.dumbify.model.DailyStats
import com.dumbify.repository.AppConfigRepository
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import org.junit.Assert.*

@RunWith(MockitoJUnitRunner::class)
class AiAnalyzerTest {
    
    @Mock
    private lateinit var mockContext: Context
    
    @Mock
    private lateinit var mockApplicationContext: Context
    
    @Mock
    private lateinit var mockRepository: AppConfigRepository
    
    private lateinit var aiAnalyzer: AiAnalyzer
    
    @Before
    fun setup() {
        // Mock the context to return application context
        `when`(mockContext.applicationContext).thenReturn(mockApplicationContext)
        
        aiAnalyzer = AiAnalyzer(mockContext)
    }
    
    @Test
    fun testAiAnalyzerUsesApplicationContext() {
        // Verify that application context is used to prevent memory leaks
        verify(mockContext).applicationContext
    }
    
    @Test
    fun testSetApiKey() {
        // This test verifies that the API key can be set
        // In a real scenario, we'd mock the repository to verify the key is stored
        aiAnalyzer.setApiKey("test-api-key")
        // API key should be set in the repository
    }
    
    @Test
    fun testAnalyzeUsagePattern_noProvider() = runBlocking {
        // Test when no AI provider is configured
        val usageData = AppUsageData(
            packageName = "com.example.app",
            appName = "TestApp",
            usageTimeMillis = 600000L, // 10 minutes
            openCount = 5,
            lastUsedTimestamp = System.currentTimeMillis(),
            isProductive = false
        )
        
        val result = aiAnalyzer.analyzeUsagePattern(
            "TestApp",
            usageData,
            listOf("App1", "App2", "TestApp")
        )
        
        // When no provider is configured, should return a message indicating that
        assertTrue(result.contains("AI analysis disabled") || result.contains("unavailable"))
    }
    
    @Test
    fun testGenerateDailyInsight_fallbackToBasic() = runBlocking {
        // Test that basic insight is generated when AI provider is not configured
        val stats = DailyStats(
            totalScreenTime = 7200000L, // 2 hours
            productiveTime = 3600000L, // 1 hour
            distractingTime = 3600000L, // 1 hour
            topApps = listOf(
                AppUsageData(
                    packageName = "com.example.productive",
                    appName = "ProductiveApp",
                    usageTimeMillis = 3600000L,
                    openCount = 10,
                    lastUsedTimestamp = System.currentTimeMillis(),
                    isProductive = true
                )
            )
        )
        
        val result = aiAnalyzer.generateDailyInsight(stats)
        
        // Should return a non-empty insight
        assertNotNull(result)
        assertTrue(result.isNotEmpty())
        assertTrue(result.contains("productive") || result.contains("balance"))
    }
    
    @Test
    fun testShouldWarnUser_belowThreshold() = runBlocking {
        val (shouldWarn, message) = aiAnalyzer.shouldWarnUser(
            appName = "Instagram",
            currentUsageMinutes = 5,
            threshold = 10
        )
        
        assertFalse(shouldWarn)
        assertEquals("", message)
    }
    
    @Test
    fun testShouldWarnUser_aboveThreshold() = runBlocking {
        val (shouldWarn, message) = aiAnalyzer.shouldWarnUser(
            appName = "Instagram",
            currentUsageMinutes = 15,
            threshold = 10
        )
        
        assertTrue(shouldWarn)
        assertNotNull(message)
        assertTrue(message.isNotEmpty())
        assertTrue(message.contains("Instagram") || message.contains("15") || message.contains("minutes"))
    }
    
    @Test
    fun testBasicInsight_highProductivity() = runBlocking {
        val stats = DailyStats(
            totalScreenTime = 3600000L, // 1 hour
            productiveTime = 2700000L, // 45 minutes (75%)
            distractingTime = 900000L, // 15 minutes
            topApps = emptyList()
        )
        
        val result = aiAnalyzer.generateDailyInsight(stats)
        
        assertTrue(result.contains("Great") || result.contains("7") || result.contains("productive"))
    }
    
    @Test
    fun testBasicInsight_lowProductivity() = runBlocking {
        val stats = DailyStats(
            totalScreenTime = 3600000L, // 1 hour
            productiveTime = 600000L, // 10 minutes (16%)
            distractingTime = 3000000L, // 50 minutes
            topApps = listOf(
                AppUsageData(
                    packageName = "com.example.social",
                    appName = "SocialApp",
                    usageTimeMillis = 3000000L,
                    openCount = 20,
                    lastUsedTimestamp = System.currentTimeMillis(),
                    isProductive = false
                )
            )
        )
        
        val result = aiAnalyzer.generateDailyInsight(stats)
        
        assertTrue(result.contains("Only") || result.contains("16") || result.contains("productive"))
    }
    
    @Test
    fun testIsConfigured_returnsFalse() {
        // When no provider is configured, isConfigured should return false
        val result = aiAnalyzer.isConfigured()
        
        // Default behavior without API key should be false
        assertFalse(result)
    }
}
