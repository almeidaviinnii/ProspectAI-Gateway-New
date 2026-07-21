package com.prospectai.gateway

import com.prospectai.core.model.AiAnalysisRequest
import com.prospectai.core.model.AiAnalysisResponse
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class OpenAIProvider(
    private val config: GatewayConfig,
    private val json: Json,
    private val usage: UsageRegistry,
) : AIProvider {
    override val id = "openai"
    override val isConfigured get() = !config.aiApiKey.isNullOrBlank()

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    override fun analyze(request: AiAnalysisRequest): AiAnalysisResponse {
        val key = config.aiApiKey?.takeIf { it.isNotBlank() }
            ?: throw providerNotConfigured(id, "AI_API_KEY (ou OPENAI_API_KEY)")
        usage.record("ai_openai")
        val requestBody = buildJsonObject {
            put("model", config.aiModel)
            put("temperature", 0.2)
            put("response_format", buildJsonObject { put("type", "json_object") })
            put("messages", buildJsonArray {
                add(buildJsonObject { put("role", "system"); put("content", AIAnalysisSupport.SYSTEM_PROMPT) })
                add(buildJsonObject { put("role", "user"); put("content", AIAnalysisSupport.buildPrompt(request)) })
            })
        }
        val httpRequest = Request.Builder()
            .url("${config.aiBaseUrl}/chat/completions")
            .header("Authorization", "Bearer $key")
            .header("Content-Type", "application/json")
            .post(requestBody.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        return usage.track("ai_openai", { response -> !response.model.contains("fallback", ignoreCase = true) }) {
            client.newCall(httpRequest).execute().use { response ->
                val body = response.body.string()
                if (!response.isSuccessful) {
                    AIAnalysisSupport.fallback(request, "OpenAI respondeu com HTTP ${response.code}.")
                } else {
                    runCatching {
                        val root = json.parseToJsonElement(body).jsonObject
                        val content = root["choices"]!!.jsonArray.first().jsonObject["message"]!!.jsonObject["content"]!!.jsonPrimitive.content
                        AIAnalysisSupport.parse(json, content, request, config.aiModel)
                    }.getOrElse { AIAnalysisSupport.fallback(request, "A resposta da OpenAI não seguiu o formato obrigatório.") }
                }
            }
        }
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}
