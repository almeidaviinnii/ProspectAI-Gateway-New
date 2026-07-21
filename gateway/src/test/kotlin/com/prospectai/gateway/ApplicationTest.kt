package com.prospectai.gateway

import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.junit.Assert.assertEquals
import org.junit.Test

class ApplicationTest {
    @Test
    fun `health is available without exposing secrets`() = testApplication {
        application {
            prospectAiGateway(
                GatewayConfig(
                    host = "127.0.0.1",
                    port = 8080,
                    gatewayToken = "test-token",
                    googlePlacesApiKey = null,
                    placesDataStorageAllowed = false,
                    placesDataTtlDays = 30,
                    aiApiKey = null,
                    aiBaseUrl = "https://example.invalid/v1",
                    aiModel = "test-model",
                    dailyRequestLimit = 10,
                    websiteTimeoutSeconds = 5,
                ),
            )
        }
        assertEquals(HttpStatusCode.OK, client.get("/v1/health").status)
        assertEquals(HttpStatusCode.Unauthorized, client.get("/v1/usage").status)
    }
}
