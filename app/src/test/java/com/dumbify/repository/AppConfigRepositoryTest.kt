package com.dumbify.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.test.core.app.ApplicationProvider
import com.dumbify.model.AppCategory
import com.dumbify.model.AppConfig
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28]) // Use SDK 28 for better compatibility with EncryptedSharedPreferences
class AppConfigRepositoryTest {

    private lateinit var context: Context
    private lateinit var repository: AppConfigRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        // Clear any existing data
        context.getSharedPreferences("dumbify_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        repository = AppConfigRepository(context)
    }

    @Test
    fun `test default configs are returned when no configs exist`() {
        val configs = repository.getAllAppConfigs()
        
        assertNotNull(configs)
        assertTrue(configs.isNotEmpty())
        assertTrue(configs.containsKey("com.whatsapp"))
        assertEquals(AppCategory.PRODUCTIVE, configs["com.whatsapp"]?.category)
    }

    @Test
    fun `test save and retrieve app config`() {
        val config = AppConfig(
            packageName = "com.example.test",
            category = AppCategory.SOCIAL_MEDIA,
            timeLimit = 30,
            warningThreshold = 20,
            autoClose = true
        )

        repository.saveAppConfig(config)
        val retrieved = repository.getAppConfig("com.example.test")

        assertNotNull(retrieved)
        assertEquals(config.packageName, retrieved?.packageName)
        assertEquals(config.category, retrieved?.category)
        assertEquals(config.timeLimit, retrieved?.timeLimit)
        assertEquals(config.warningThreshold, retrieved?.warningThreshold)
        assertEquals(config.autoClose, retrieved?.autoClose)
    }

    @Test
    fun `test GitHub access token is stored securely`() {
        val testToken = "ghp_test_token_12345"
        
        repository.githubAccessToken = testToken
        val retrieved = repository.githubAccessToken

        assertEquals(testToken, retrieved)
    }

    @Test
    fun `test GitHub token can be cleared`() {
        repository.githubAccessToken = "test_token"
        assertNotNull(repository.githubAccessToken)

        repository.githubAccessToken = null
        assertNull(repository.githubAccessToken)
    }

    @Test
    fun `test AI provider selection persists`() {
        repository.selectedAiProvider = "github"
        assertEquals("github", repository.selectedAiProvider)

        repository.selectedAiProvider = "gemini"
        assertEquals("gemini", repository.selectedAiProvider)
    }

    @Test
    fun `test default AI provider is gemini`() {
        assertEquals("gemini", repository.selectedAiProvider)
    }

    @Test
    fun `test OAuth state is stored and retrieved`() {
        val state = "random_state_12345"
        repository.oauthState = state
        assertEquals(state, repository.oauthState)
    }

    @Test
    fun `test daily social limit default value`() {
        assertEquals(60, repository.dailySocialMediaLimit)
    }

    @Test
    fun `test daily social limit can be updated`() {
        repository.dailySocialMediaLimit = 120
        assertEquals(120, repository.dailySocialMediaLimit)
    }

    @Test
    fun `test blocked domains default list`() {
        val domains = repository.getBlockedDomains()
        assertNotNull(domains)
        assertTrue(domains.isNotEmpty())
        assertTrue(domains.any { it.domain == "pornhub.com" })
        assertTrue(domains.any { it.domain == "bet365.com" })
    }

    @Test
    fun `test add blocked domain`() {
        val newDomain = com.dumbify.model.BlockedDomain("test.com", "testing")
        repository.addBlockedDomain(newDomain)
        
        val domains = repository.getBlockedDomains()
        assertTrue(domains.any { it.domain == "test.com" })
    }

    @Test
    fun `test getAllAppConfigs returns immutable map`() {
        val configs = repository.getAllAppConfigs()
        val originalSize = configs.size
        
        // This should not affect the stored configs
        (configs as? MutableMap)?.put("test.package", AppConfig(
            packageName = "test.package",
            category = AppCategory.BLOCKED,
            timeLimit = 0,
            warningThreshold = 0,
            autoClose = false
        ))
        
        // Retrieve again and verify size hasn't changed
        val configsAgain = repository.getAllAppConfigs()
        assertEquals(originalSize, configsAgain.size)
    }

    @Test
    fun `test Gemini API key is stored securely`() {
        val testKey = "test_gemini_api_key"
        
        repository.geminiApiKey = testKey
        val retrieved = repository.geminiApiKey

        assertEquals(testKey, retrieved)
    }

    @Test
    fun `test AI enabled default value`() {
        assertTrue(repository.isAiEnabled)
    }

    @Test
    fun `test DNS filter disabled by default`() {
        assertFalse(repository.isDnsFilterEnabled)
    }
}
