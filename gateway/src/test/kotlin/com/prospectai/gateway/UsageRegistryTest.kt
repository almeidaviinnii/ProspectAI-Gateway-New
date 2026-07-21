package com.prospectai.gateway

import io.ktor.http.HttpStatusCode
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class UsageRegistryTest {
    @Test
    fun `usage survives registry restart and enforces limit`() {
        val file = Files.createTempFile("prospectai-usage", ".csv")
        try {
            UsageRegistry(2, file.toString()).record("provider")
            val restored = UsageRegistry(2, file.toString())
            assertEquals(1, restored.current("provider"))
            restored.record("provider")
            try {
                restored.record("provider")
                fail("The daily limit should have been enforced")
            } catch (error: GatewayException) {
                assertEquals(HttpStatusCode.TooManyRequests, error.status)
            }
        } finally {
            Files.deleteIfExists(file)
        }
    }

    @Test
    fun `monthly limit and provider outcomes are auditable`() {
        val file = Files.createTempFile("prospectai-monthly-usage", ".csv")
        try {
            val registry = UsageRegistry(dailyLimit = 10, logPath = file.toString(), monthlyLimit = 1)
            registry.record("provider")
            assertEquals("ok", registry.track("provider") { "ok" })
            runCatching { registry.track("provider") { error("failure") } }
            val metrics = registry.snapshot().single { it.provider == "provider" }
            assertEquals(1, metrics.dailyRequests)
            assertEquals(1, metrics.monthlyRequests)
            assertEquals(1, metrics.successfulCalls)
            assertEquals(1, metrics.failedCalls)
            assertEquals(1, metrics.consecutiveFailures)

            try {
                registry.record("provider")
                fail("The monthly limit should have been enforced")
            } catch (error: GatewayException) {
                assertEquals(HttpStatusCode.TooManyRequests, error.status)
                assertTrue(error.message.contains("mensal"))
            }
        } finally {
            Files.deleteIfExists(file)
        }
    }
}
