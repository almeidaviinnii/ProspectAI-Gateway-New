package com.prospectai.gateway

import org.junit.Assert.assertFalse
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
}
