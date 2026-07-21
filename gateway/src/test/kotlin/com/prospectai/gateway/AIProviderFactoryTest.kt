package com.prospectai.gateway

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class AIProviderFactoryTest {
    @Test
    fun `factory selects Gemini by default`() {
        val config = config(aiProvider = "gemini", geminiApiKey = "gemini-key")
        val provider = AIProviderFactory.create(config, Json, usage())

        assertEquals("gemini", provider.id)
        assertTrue(provider.isConfigured)
    }

    @Test
    fun `factory preserves OpenAI selection and legacy key`() {
        val config = config(aiProvider = "openai", aiApiKey = "openai-key")
        val provider = AIProviderFactory.create(config, Json, usage())

        assertEquals("openai", provider.id)
        assertTrue(provider.isConfigured)
    }

    @Test
    fun `selected provider reports missing key as not configured`() {
        val provider = AIProviderFactory.create(config(aiProvider = "gemini"), Json, usage())

        assertEquals("gemini", provider.id)
        assertFalse(provider.isConfigured)
    }

    private fun usage() = UsageRegistry(10, Files.createTempFile("prospectai-ai-test", ".csv").toString(), 100)

    private fun config(
        aiProvider: String,
        aiApiKey: String? = null,
        geminiApiKey: String? = null,
    ) = GatewayConfig(
        host = "127.0.0.1",
        port = 8080,
        gatewayToken = "test-token",
        googlePlacesApiKey = null,
        placesDataStorageAllowed = false,
        placesDataTtlDays = 30,
        aiApiKey = aiApiKey,
        aiBaseUrl = "https://api.openai.com/v1",
        aiModel = "test-openai-model",
        dailyRequestLimit = 10,
        websiteTimeoutSeconds = 5,
        aiProvider = aiProvider,
        geminiApiKey = geminiApiKey,
        geminiModel = "test-gemini-model",
    )
}
