package com.prospectai.gateway

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GatewayConfigTest {
    @Test
    fun `places storage authorization is explicit and disabled by default`() {
        val disabled = GatewayConfig.fromEnvironment(mapOf("GOOGLE_PLACES_API_KEY" to "test-key"))
        val enabled = GatewayConfig.fromEnvironment(
            mapOf(
                "GOOGLE_PLACES_API_KEY" to "test-key",
                "PLACES_DATA_STORAGE_ALLOWED" to "true",
            ),
        )

        assertFalse(disabled.placesDataStorageAllowed)
        assertTrue(enabled.placesDataStorageAllowed)
    }

    @Test
    fun `gemini is the default AI provider and OpenAI remains selectable`() {
        val defaults = GatewayConfig.fromEnvironment(emptyMap())
        val openAI = GatewayConfig.fromEnvironment(
            mapOf(
                "AI_PROVIDER" to "OPENAI",
                "AI_API_KEY" to "openai-test-key",
                "GEMINI_API_KEY" to "gemini-test-key",
            ),
        )

        assertEquals("gemini", defaults.aiProvider)
        assertEquals("gemini-3.5-flash-lite", defaults.geminiModel)
        assertEquals("openai", openAI.aiProvider)
        assertEquals("openai-test-key", openAI.aiApiKey)
        assertEquals("gemini-test-key", openAI.geminiApiKey)
    }
}
