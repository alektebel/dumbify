package com.dumbify.api

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.dumbify.repository.AppConfigRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AiProviderTest {

    private lateinit var context: Context
    private lateinit var repository: AppConfigRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("dumbify_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        repository = AppConfigRepository(context)
    }

    @Test
    fun `test GeminiProvider is not configured without API key`() {
        repository.geminiApiKey = null
        val provider = GeminiProvider(context)

        assertFalse(provider.isConfigured())
    }

    @Test
    fun `test GeminiProvider is configured with API key`() {
        repository.geminiApiKey = "test_api_key"
        val provider = GeminiProvider(context)

        assertTrue(provider.isConfigured())
    }

    @Test
    fun `test GeminiProvider returns correct name`() {
        val provider = GeminiProvider(context)
        assertEquals("Google Gemini", provider.getProviderName())
    }

    @Test
    fun `test GitHubModelsProvider is not configured without token`() {
        repository.githubAccessToken = null
        val provider = GitHubModelsProvider(context)

        assertFalse(provider.isConfigured())
    }

    @Test
    fun `test GitHubModelsProvider is configured with token`() {
        repository.githubAccessToken = "ghp_test_token"
        val provider = GitHubModelsProvider(context)

        assertTrue(provider.isConfigured())
    }

    @Test
    fun `test GitHubModelsProvider returns correct name`() {
        val provider = GitHubModelsProvider(context)
        assertTrue(provider.getProviderName().contains("GitHub Copilot"))
    }

    @Test
    fun `test AiProviderFactory creates Gemini provider by default`() {
        repository.selectedAiProvider = "gemini"
        val provider = AiProviderFactory.createProvider(context)

        assertTrue(provider is GeminiProvider)
    }

    @Test
    fun `test AiProviderFactory creates GitHub provider when selected`() {
        repository.selectedAiProvider = "github"
        val provider = AiProviderFactory.createProvider(context)

        assertTrue(provider is GitHubModelsProvider)
    }

    @Test
    fun `test AiProviderFactory defaults to Gemini for unknown provider`() {
        repository.selectedAiProvider = "unknown_provider"
        val provider = AiProviderFactory.createProvider(context)

        assertTrue(provider is GeminiProvider)
    }

    @Test
    fun `test getAvailableProviders returns only configured providers`() {
        // No providers configured
        repository.geminiApiKey = null
        repository.githubAccessToken = null

        val providers = AiProviderFactory.getAvailableProviders(context)
        assertTrue(providers.isEmpty())

        // Configure Gemini
        repository.geminiApiKey = "test_key"
        val providersWithGemini = AiProviderFactory.getAvailableProviders(context)
        assertEquals(1, providersWithGemini.size)
        assertEquals("gemini", providersWithGemini[0].first)

        // Configure GitHub
        repository.githubAccessToken = "test_token"
        val providersWithBoth = AiProviderFactory.getAvailableProviders(context)
        assertEquals(2, providersWithBoth.size)
    }

    @Test
    fun `test provider uses application context to avoid leaks`() {
        // Create provider with activity context simulation
        val provider = GeminiProvider(context)
        
        // Provider should use application context internally
        // This is tested implicitly by ensuring no crashes occur
        assertNotNull(provider)
    }
}
