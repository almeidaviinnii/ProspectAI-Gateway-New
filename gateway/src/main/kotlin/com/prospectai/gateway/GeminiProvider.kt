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

class GeminiProvider(
    private val config: GatewayConfig,
    private val json: Json,
    private val usage: UsageRegistry,
) : AIProvider {
    override val id = "gemini"
    override val isConfigured get() = !config.geminiApiKey.isNullOrBlank()

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    override fun analyze(request: AiAnalysisRequest): AiAnalysisResponse {
        val key = config.geminiApiKey?.takeIf { it.isNotBlank() }
            ?: throw providerNotConfigured(id, "GEMINI_API_KEY")
        usage.record("ai_gemini")
        val requestBody = buildJsonObject {
            put("systemInstruction", buildJsonObject {
                put("parts", buildJsonArray { add(buildJsonObject { put("text", AIAnalysisSupport.SYSTEM_PROMPT) }) })
            })
            put("contents", buildJsonArray {
                add(buildJsonObject {
                    put("role", "user")
                    put("parts", buildJsonArray {
                        add(buildJsonObject { put("text", AIAnalysisSupport.buildPrompt(request)) })
                    })
                })
            })
            put("generationConfig", buildJsonObject {
                put("responseMimeType", "application/json")
            })
        }
        val httpRequest = Request.Builder()
            .url("${config.geminiBaseUrl}/models/${config.geminiModel}:generateContent")
            .header("x-goog-api-key", key)
            .header("Content-Type", "application/json")
            .post(requestBody.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        return usage.track("ai_gemini", { response -> !response.model.contains("fallback", ignoreCase = true) }) {
            client.newCall(httpRequest).execute().use { response ->
                val body = response.body.string()
                if (!response.isSuccessful) {
                    AIAnalysisSupport.fallback(request, "Google Gemini respondeu com HTTP ${response.code}.")
                } else {
                    runCatching {
                        val root = json.parseToJsonElement(body).jsonObject
                        val content = root["candidates"]!!.jsonArray.first().jsonObject["content"]!!.jsonObject["parts"]!!
                            .jsonArray.first().jsonObject["text"]!!.jsonPrimitive.content
                        AIAnalysisSupport.parse(json, content, request, config.geminiModel)
                    }.getOrElse { AIAnalysisSupport.fallback(request, "A resposta do Google Gemini não seguiu o formato obrigatório.") }
                }
            }
        }
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}
