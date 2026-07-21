package com.prospectai.gateway

import com.prospectai.core.model.AiAnalysisRequest
import com.prospectai.core.model.AiAnalysisResponse
import com.prospectai.core.model.ConfidenceLevel
import io.ktor.http.HttpStatusCode
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class AiProvider(
    private val config: GatewayConfig,
    private val json: Json,
    private val usage: UsageRegistry,
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    fun analyze(request: AiAnalysisRequest): AiAnalysisResponse {
        val key = config.aiApiKey ?: return fallback(request, "Provedor de IA não configurado no Gateway.")
        usage.record("ai")
        val prompt = buildPrompt(request)
        val requestBody = buildJsonObject {
            put("model", config.aiModel)
            put("temperature", 0.2)
            put("response_format", buildJsonObject { put("type", "json_object") })
            put("messages", buildJsonArray {
                add(buildJsonObject {
                    put("role", "system")
                    put("content", SYSTEM_PROMPT)
                })
                add(buildJsonObject {
                    put("role", "user")
                    put("content", prompt)
                })
            })
        }
        val httpRequest = Request.Builder()
            .url("${config.aiBaseUrl}/chat/completions")
            .header("Authorization", "Bearer $key")
            .header("Content-Type", "application/json")
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        return usage.track("ai", { response -> !response.model.contains("fallback", ignoreCase = true) }) {
            client.newCall(httpRequest).execute().use { response ->
                val body = response.body.string()
                if (!response.isSuccessful) {
                    fallback(request, "Provedor de IA respondeu com HTTP ${response.code}.")
                } else {
                    runCatching {
                        val root = json.parseToJsonElement(body).jsonObject
                        val content = root["choices"]!!.jsonArray.first().jsonObject["message"]!!.jsonObject["content"]!!.jsonPrimitive.content
                        val result = json.parseToJsonElement(content).jsonObject
                        AiAnalysisResponse(
                            executiveSummary = result.string("executiveSummary") ?: request.score.explanation,
                            strengths = result.stringList("strengths"),
                            weaknesses = result.stringList("weaknesses"),
                            opportunities = result.stringList("opportunities"),
                            recommendedServices = result.stringList("recommendedServices").filter { it in request.offeredServices },
                            suggestedApproach = result.string("suggestedApproach") ?: "Utilize apenas as evidências confirmadas no primeiro contato.",
                            confidence = result.string("confidence")?.let { runCatching { enumValueOf<ConfidenceLevel>(it.uppercase()) }.getOrNull() } ?: request.score.confidence,
                            limitations = result.stringList("limitations"),
                            model = config.aiModel,
                            promptVersion = "prospectai-analysis-v1.0.0",
                        )
                    }.getOrElse { fallback(request, "A resposta da IA não seguiu o formato obrigatório.") }
                }
            }
        }
    }

    private fun buildPrompt(request: AiAnalysisRequest): String = buildString {
        appendLine("EMPRESA: ${request.companyName}")
        appendLine("NOTA DETERMINÍSTICA: ${request.score.score}/100")
        appendLine("JUSTIFICATIVA DA NOTA: ${request.score.explanation}")
        appendLine("FATOS DISPONÍVEIS:")
        request.facts.forEach { (key, value) -> appendLine("- $key: $value") }
        appendLine("SERVIÇOS REALMENTE OFERECIDOS: ${request.offeredServices.joinToString()}")
        appendLine("Retorne somente JSON com executiveSummary, strengths, weaknesses, opportunities, recommendedServices, suggestedApproach, confidence e limitations.")
    }

    private fun fallback(request: AiAnalysisRequest, limitation: String) = AiAnalysisResponse(
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

    private fun kotlinx.serialization.json.JsonObject.string(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull
    private fun kotlinx.serialization.json.JsonObject.stringList(key: String): List<String> =
        this[key]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull }.orEmpty()

    private companion object {
        const val SYSTEM_PROMPT = "Você é um consultor de prospecção. Use somente fatos fornecidos. Nunca altere ou recalcule a nota. Diferencie ausência confirmada de dado indisponível. Não invente atividade social, engajamento, orçamento ou intenção de compra."
    }
}
