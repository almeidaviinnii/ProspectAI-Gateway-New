package com.prospectai.gateway

import com.prospectai.core.model.AiAnalysisRequest
import com.prospectai.core.model.AiAnalysisResponse
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.Json

object AIProviderFactory {
    fun create(config: GatewayConfig, json: Json, usage: UsageRegistry): AIProvider = when (config.aiProvider) {
        "gemini" -> GeminiProvider(config, json, usage)
        "openai" -> OpenAIProvider(config, json, usage)
        else -> MisconfiguredAIProvider(
            config.aiProvider,
            "AI_PROVIDER deve ser 'gemini' ou 'openai'; valor recebido: '${config.aiProvider}'.",
        )
    }
}

object AnalysisProviderFactory {
    fun create(config: GatewayConfig, json: Json, usage: UsageRegistry): AnalysisProvider =
        if (config.aiModuleEnabled) {
            AIProviderFactory.create(config, json, usage)
        } else {
            RuleEngineProvider()
        }
}

internal fun providerNotConfigured(provider: String, requiredVariable: String) = GatewayException(
    "Provedor de IA '$provider' selecionado, mas não configurado. Defina $requiredVariable.",
    HttpStatusCode.ServiceUnavailable,
)

private class MisconfiguredAIProvider(
    override val id: String,
    private val reason: String,
) : AIProvider {
    override val isConfigured = false

    override fun analyze(request: AiAnalysisRequest): AiAnalysisResponse {
        throw GatewayException(reason, HttpStatusCode.ServiceUnavailable)
    }
}
