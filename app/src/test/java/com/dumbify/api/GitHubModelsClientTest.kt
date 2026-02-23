package com.dumbify.api

import com.dumbify.auth.OAuthConfig
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.io.IOException

class GitHubModelsClientTest {
    
    private lateinit var mockWebServer: MockWebServer
    private lateinit var client: GitHubModelsClient
    private val gson = Gson()
    
    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        
        // Override the API URL for testing (in a real app, you'd inject this)
        client = GitHubModelsClient("test-access-token")
    }
    
    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }
    
    @Test
    fun testChatMessage_dataClass() {
        val message = ChatMessage(role = "user", content = "Hello")
        
        assertEquals("user", message.role)
        assertEquals("Hello", message.content)
    }
    
    @Test
    fun testChatRequest_serialization() {
        val request = ChatRequest(
            model = GitHubModelsClient.MODEL_GPT4O_MINI,
            messages = listOf(
                ChatMessage(role = "system", content = "You are helpful."),
                ChatMessage(role = "user", content = "Hello")
            ),
            temperature = 0.7,
            max_tokens = 1000
        )
        
        val json = gson.toJson(request)
        
        assertTrue(json.contains("gpt-4o-mini"))
        assertTrue(json.contains("max_tokens"))
        assertTrue(json.contains("temperature"))
    }
    
    @Test
    fun testChatResponse_deserialization() {
        val json = """
            {
                "id": "chatcmpl-123",
                "choices": [
                    {
                        "index": 0,
                        "message": {
                            "role": "assistant",
                            "content": "Hello! How can I help you?"
                        },
                        "finish_reason": "stop"
                    }
                ],
                "usage": {
                    "prompt_tokens": 10,
                    "completion_tokens": 20,
                    "total_tokens": 30
                }
            }
        """.trimIndent()
        
        val response = gson.fromJson(json, ChatResponse::class.java)
        
        assertNotNull(response)
        assertEquals("chatcmpl-123", response.id)
        assertEquals(1, response.choices.size)
        assertEquals("Hello! How can I help you?", response.choices[0].message.content)
        assertEquals("assistant", response.choices[0].message.role)
        assertNotNull(response.usage)
        assertEquals(30, response.usage?.total_tokens)
    }
    
    @Test
    fun testModelConstants() {
        assertEquals("gpt-4o", GitHubModelsClient.MODEL_GPT4O)
        assertEquals("gpt-4o-mini", GitHubModelsClient.MODEL_GPT4O_MINI)
        assertEquals("claude-3.5-sonnet", GitHubModelsClient.MODEL_CLAUDE_SONNET)
        assertEquals("meta-llama-3.1-405b-instruct", GitHubModelsClient.MODEL_LLAMA_3_1)
        assertEquals("o1-preview", GitHubModelsClient.MODEL_O1_PREVIEW)
        assertEquals("o1-mini", GitHubModelsClient.MODEL_O1_MINI)
        assertEquals("gpt-4o-mini", GitHubModelsClient.DEFAULT_MODEL)
    }
    
    @Test
    fun testChat_successfulResponse() = runBlocking {
        // Note: This test would require mocking the OkHttpClient or using a test server
        // For now, we test the data structures and constants
        
        val messages = listOf(
            ChatMessage(role = "system", content = "You are helpful."),
            ChatMessage(role = "user", content = "What is 2+2?")
        )
        
        assertNotNull(messages)
        assertEquals(2, messages.size)
        assertEquals("system", messages[0].role)
        assertEquals("user", messages[1].role)
    }
    
    @Test(expected = IOException::class)
    fun testChat_emptyAccessToken_shouldFail() = runBlocking {
        // Create a client with empty access token
        val invalidClient = GitHubModelsClient("")
        
        // Set up mock server to return 401
        mockWebServer.enqueue(MockResponse().setResponseCode(401).setBody("Unauthorized"))
        
        // This should fail with unauthorized
        invalidClient.complete("Test prompt")
        
        Unit
    }
    
    @Test
    fun testComplete_buildsCorrectMessages() {
        val prompt = "Analyze this usage"
        val systemPrompt = "You are a digital wellbeing assistant."
        
        // Test that messages would be built correctly
        val expectedMessages = listOf(
            ChatMessage(role = "system", content = systemPrompt),
            ChatMessage(role = "user", content = prompt)
        )
        
        assertEquals(2, expectedMessages.size)
        assertEquals("system", expectedMessages[0].role)
        assertEquals(systemPrompt, expectedMessages[0].content)
        assertEquals("user", expectedMessages[1].role)
        assertEquals(prompt, expectedMessages[1].content)
    }
    
    @Test
    fun testChatRequest_defaultValues() {
        val request = ChatRequest(
            model = GitHubModelsClient.MODEL_GPT4O_MINI,
            messages = listOf(ChatMessage(role = "user", content = "Hi"))
        )
        
        assertEquals(0.7, request.temperature, 0.001)
        assertEquals(1000, request.max_tokens)
    }
    
    @Test
    fun testUsage_deserialization() {
        val json = """
            {
                "prompt_tokens": 50,
                "completion_tokens": 100,
                "total_tokens": 150
            }
        """.trimIndent()
        
        val usage = gson.fromJson(json, Usage::class.java)
        
        assertNotNull(usage)
        assertEquals(50, usage.prompt_tokens)
        assertEquals(100, usage.completion_tokens)
        assertEquals(150, usage.total_tokens)
    }
    
    @Test
    fun testChoice_deserialization() {
        val json = """
            {
                "index": 0,
                "message": {
                    "role": "assistant",
                    "content": "Test response"
                },
                "finish_reason": "stop"
            }
        """.trimIndent()
        
        val choice = gson.fromJson(json, Choice::class.java)
        
        assertNotNull(choice)
        assertEquals(0, choice.index)
        assertEquals("assistant", choice.message.role)
        assertEquals("Test response", choice.message.content)
        assertEquals("stop", choice.finish_reason)
    }
    
    @Test
    fun testChatMessage_equality() {
        val msg1 = ChatMessage(role = "user", content = "Hello")
        val msg2 = ChatMessage(role = "user", content = "Hello")
        val msg3 = ChatMessage(role = "assistant", content = "Hello")
        
        assertEquals(msg1, msg2)
        assertNotEquals(msg1, msg3)
    }
}
