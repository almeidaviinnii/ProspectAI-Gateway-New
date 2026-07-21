package com.prospectai.gateway

import com.prospectai.core.model.GatewaySearchRequest
import com.prospectai.core.model.GatewaySearchResponse
import com.prospectai.core.model.SearchFilters
import io.ktor.http.HttpStatusCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IntegrationManagerTest {
    @Test
    fun `retry succeeds and subsequent equivalent request uses cache`() {
        val provider = RecoveringProvider()
        val manager = IntegrationManager(listOf(provider), cacheTtlMinutes = 10, maxAttempts = 2, clock = { 100L })
        val request = GatewaySearchRequest("run-1", SearchFilters("Dentistas", city = "Campinas"))

        val first = manager.search(request)
        val second = manager.search(request.copy(searchRunId = "run-2"))

        assertEquals(2, provider.calls)
        assertEquals("fake", first.provider)
        assertTrue(first.warnings.any { "nova tentativa" in it })
        assertTrue(second.warnings.any { "cache seguro" in it })
    }

    private class RecoveringProvider : CompanySearchProvider {
        override val id = "fake"
        var calls = 0

        override fun isConfigured() = true

        override fun search(request: GatewaySearchRequest): GatewaySearchResponse {
            calls += 1
            if (calls == 1) throw GatewayException("temporary", HttpStatusCode.BadGateway)
            return GatewaySearchResponse(emptyList(), provider = id)
        }
    }
}
