package com.prospectai.gateway

import com.prospectai.core.model.AiAnalysisRequest
import com.prospectai.core.model.ConfidenceLevel
import com.prospectai.core.model.OpportunityBand
import com.prospectai.core.model.ScoreFactor
import com.prospectai.core.model.ScoreResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleEngineProviderTest {
    @Test
    fun `generates deterministic commercial opportunities without AI`() {
        val request = AiAnalysisRequest(
            companyId = "company-1",
            companyName = "Empresa Teste",
            facts = mapOf(
                "website" to "https://example.com",
                "website_acessivel" to "true",
                "website_https" to "false",
                "website_mobile" to "false",
                "website_tempo_ms" to "4200",
                "redes_identificadas" to "0",
            ),
            score = ScoreResult(
                version = "score-v1.0.0",
                score = 58,
                band = OpportunityBand.MEDIUM,
                confidence = ConfidenceLevel.HIGH,
                factors = listOf(
                    ScoreFactor("website_https", "Website sem HTTPS", 8, "Sem HTTPS"),
                    ScoreFactor("website_slow", "Website lento", 8, "4200 ms"),
                    ScoreFactor("website_mobile", "Website não adaptado", 12, "Sem viewport"),
                    ScoreFactor("no_social", "Sem redes", 10, "Nenhum perfil"),
                ),
                explanation = "Oportunidade moderada.",
                calculatedAt = 1L,
            ),
            offeredServices = listOf("SEO Local", "Criação de sites", "Gestão de redes sociais"),
        )

        val result = RuleEngineProvider().analyze(request)

        assertEquals("rule-engine", result.model)
        assertTrue(result.executiveSummary.contains("42/100"))
        assertTrue(result.opportunities.any { it.contains("SSL") })
        assertTrue(result.opportunities.any { it.contains("velocidade") })
        assertTrue(result.opportunities.any { it.contains("mobile") })
        assertTrue(result.opportunities.any { it.contains("WhatsApp") })
    }

    @Test
    fun `analysis factory keeps external AI disabled by default`() {
        val config = GatewayConfig.fromEnvironment(
            mapOf(
                "AI_API_KEY" to "key-that-must-not-be-used",
                "AI_PROVIDER" to "openai",
            ),
        )
        val provider = AnalysisProviderFactory.create(config, kotlinx.serialization.json.Json, UsageRegistry(10, tempFile()))

        assertEquals("rule-engine", provider.id)
        assertTrue(provider.isConfigured)
    }

    private fun tempFile(): String = kotlin.io.path.createTempDirectory("rule-engine-test").resolve("usage.csv").toString()
}
