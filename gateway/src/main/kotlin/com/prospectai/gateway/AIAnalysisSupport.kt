package com.prospectai.gateway

import com.prospectai.core.model.AiAnalysisRequest
import com.prospectai.core.model.AiAnalysisResponse
import com.prospectai.core.model.ConfidenceLevel
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal object AIAnalysisSupport {
    const val SYSTEM_PROMPT = "Você é um consultor de prospecção. Use somente fatos fornecidos. Nunca altere ou recalcule a nota. Diferencie ausência confirmada de dado indisponível. Não invente atividade social, engajamento, orçamento ou intenção de compra."

    fun buildPrompt(request: AiAnalysisRequest): String = buildString {
        appendLine("EMPRESA: ${request.companyName}")
        appendLine("NOTA DETERMINÍSTICA: ${request.score.score}/100")
        appendLine("JUSTIFICATIVA DA NOTA: ${request.score.explanation}")
        appendLine("FATOS DISPONÍVEIS:")
        request.facts.forEach { (key, value) -> appendLine("- $key: $value") }
        appendLine("SERVIÇOS REALMENTE OFERECIDOS: ${request.offeredServices.joinToString()}")
        appendLine("Retorne somente JSON com executiveSummary, strengths, weaknesses, opportunities, recommendedServices, suggestedApproach, confidence e limitations.")
    }

    fun parse(json: Json, content: String, request: AiAnalysisRequest, model: String): AiAnalysisResponse {
        val result = json.parseToJsonElement(content).jsonObject
        return AiAnalysisResponse(
            executiveSummary = result.string("executiveSummary") ?: request.score.explanation,
            strengths = result.stringList("strengths"),
            weaknesses = result.stringList("weaknesses"),
            opportunities = result.stringList("opportunities"),
            recommendedServices = result.stringList("recommendedServices").filter { it in request.offeredServices },
            suggestedApproach = result.string("suggestedApproach") ?: "Utilize apenas as evidências confirmadas no primeiro contato.",
            confidence = result.string("confidence")?.let { runCatching { enumValueOf<ConfidenceLevel>(it.uppercase()) }.getOrNull() }
                ?: request.score.confidence,
            limitations = result.stringList("limitations"),
            model = model,
            promptVersion = "prospectai-analysis-v1.0.0",
        )
    }

    fun fallback(request: AiAnalysisRequest, limitation: String) = AiAnalysisResponse(
        executiveSummary = "${request.companyName} recebeu ${request.score.score}/100. ${request.score.explanation}",
        strengths = request.score.factors.filter { it.points < 0 }.map { it.label },
        weaknesses = request.score.factors.filter { it.points > 0 && it.key != "base" }.map { it.label },
        opportunities = request.score.factors.filter { it.points > 0 && it.key != "base" }.map { it.evidence },
        recommendedServices = emptyList(),
        suggestedApproach = "Comece apresentando uma oportunidade confirmada e proponha uma análise detalhada, sem afirmar informações ausentes.",
        confidence = request.score.confidence,
        limitations = listOf(limitation),
        model = "gateway-transparent-fallback",
        promptVersion = "fallback-v1.0.0",
    )

    private fun JsonObject.string(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull
    private fun JsonObject.stringList(key: String): List<String> =
        this[key]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull }.orEmpty()
}
