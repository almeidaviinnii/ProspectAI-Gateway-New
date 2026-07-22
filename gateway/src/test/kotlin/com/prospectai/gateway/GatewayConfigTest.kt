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
    fun `AI is disabled without a key and AI API key reactivates OpenAI`() {
        val defaults = GatewayConfig.fromEnvironment(emptyMap())
        val openAI = GatewayConfig.fromEnvironment(
            mapOf(
                "AI_API_KEY" to "openai-test-key",
            ),
        )

        assertEquals("openai", defaults.aiProvider)
        assertFalse(defaults.aiModuleEnabled)
        assertEquals("openai", openAI.aiProvider)
        assertTrue(openAI.aiModuleEnabled)
        assertEquals("openai-test-key", openAI.aiApiKey)
    }

    @Test
    fun `explicit false keeps AI disabled even when a legacy key exists`() {
        val config = GatewayConfig.fromEnvironment(
            mapOf(
                "AI_MODULE_ENABLED" to "false",
                "OPENAI_API_KEY" to "legacy-key",
            ),
        )

        assertFalse(config.aiModuleEnabled)
    }
}
