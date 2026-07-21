package com.prospectai.gateway

import com.prospectai.core.model.AiAnalysisRequest
import com.prospectai.core.model.GatewayHealth
import com.prospectai.core.model.GatewaySearchRequest
import com.prospectai.core.model.GatewayUsageMetrics
import com.prospectai.core.model.WebsiteAuditRequest
import com.prospectai.core.model.UpdateManifest
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.slf4j.event.Level

fun main() {
    val config = GatewayConfig.fromEnvironment()
    embeddedServer(Netty, port = config.port, host = config.host) {
        prospectAiGateway(config)
    }.start(wait = true)
}

fun Application.prospectAiGateway(config: GatewayConfig = GatewayConfig.fromEnvironment()) {
    val gatewayLog = environment.log
    val jsonCodec = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }
    val usage = UsageRegistry(config.dailyRequestLimit, config.usageLogPath, config.monthlyRequestLimit)
    val places = GooglePlacesProvider(config, jsonCodec, usage)
    val searchProviders = buildList<CompanySearchProvider> {
        if (places.isConfigured()) add(places)
    }
    val integrations = IntegrationManager(searchProviders, config.searchCacheTtlMinutes, config.providerMaxAttempts)
    val websiteAuditor = WebsiteAuditor(config, usage)
    val ai = AIProviderFactory.create(config, jsonCodec, usage)

    gatewayLog.info(
        "Gateway providers initialized: searchProviders={}, googlePlacesKeyLoaded={}, " +
            "placesDataStorageAllowed={}, aiProvider={}, aiProviderConfigured={}",
        searchProviders.joinToString { provider -> provider.id }.ifEmpty { "<none>" },
        !config.googlePlacesApiKey.isNullOrBlank(),
        config.placesDataStorageAllowed,
        ai.id,
        ai.isConfigured,
    )
    if (!ai.isConfigured) {
        val requiredVariable = when (ai.id) {
            "gemini" -> "GEMINI_API_KEY"
            "openai" -> "AI_API_KEY (ou OPENAI_API_KEY)"
            else -> "AI_PROVIDER válido"
        }
        gatewayLog.error(
            "AI provider configuration error: selectedProvider={}, missingOrInvalidConfiguration={}",
            ai.id,
            requiredVariable,
        )
    }

    install(ContentNegotiation) { json(jsonCodec) }
    install(CallLogging) {
        level = Level.INFO
        filter { call -> !call.request.local.uri.contains("token", ignoreCase = true) }
    }
    install(StatusPages) {
        exception<GatewayException> { call, error ->
            call.respond(error.status, ErrorResponse(error.message))
        }
        exception<Throwable> { call, error ->
            gatewayLog.error("Gateway request failed", error)
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Falha interna no Gateway Seguro."))
        }
    }

    routing {
        route("/v1") {
            get("/health") {
                call.respond(
                    GatewayHealth(
                        status = if (config.gatewayToken.isNullOrBlank()) "provisioning_required" else "ok",
                        version = "1.0.0",
                        providers = mapOf(
                            "google_places" to (!config.googlePlacesApiKey.isNullOrBlank() && config.placesDataStorageAllowed),
                            "ai" to ai.isConfigured,
                            "ai_${ai.id}" to ai.isConfigured,
                            "website_audit" to true,
                        ),
                    ),
                )
            }

            get("/releases/latest") {
                call.requireGatewayAuthorization(config)
                val versionCode = config.latestApkVersionCode
                val versionName = config.latestApkVersionName
                val url = config.latestApkUrl
                if (versionCode == null || versionName.isNullOrBlank() || url.isNullOrBlank()) {
                    throw GatewayException("Nenhuma atualização foi publicada.", HttpStatusCode.NotFound)
                }
                if (!url.startsWith("https://")) {
                    throw GatewayException("A URL da atualização precisa usar HTTPS.", HttpStatusCode.ServiceUnavailable)
                }
                call.respond(
                    UpdateManifest(
                        versionCode = versionCode,
                        versionName = versionName,
                        downloadUrl = url,
                        sha256 = config.latestApkSha256,
                        mandatory = config.latestApkMandatory,
                    ),
                )
            }

            get("/usage") {
                call.requireGatewayAuthorization(config)
                call.respond(GatewayUsageMetrics(System.currentTimeMillis(), usage.snapshot()))
            }

            post("/search") {
                try {
                    call.requireGatewayAuthorization(config)
                    val request = call.receive<GatewaySearchRequest>()
                    call.respond(withContext(Dispatchers.IO) { integrations.search(request) })
                } catch (error: GatewayException) {
                    if (error.status == HttpStatusCode.ServiceUnavailable) {
                        gatewayLog.error(
                            "POST /v1/search returning HTTP 503: " +
                                "message=${error.message}; " +
                                "cause=${error.cause?.let { cause -> "${cause::class.qualifiedName}: ${cause.message}" } ?: "<none>"}",
                            error,
                        )
                    }
                    throw error
                }
            }

            post("/audit/website") {
                call.requireGatewayAuthorization(config)
                val request = call.receive<WebsiteAuditRequest>()
                call.respond(withContext(Dispatchers.IO) { websiteAuditor.audit(request) })
            }

            post("/analyze") {
                try {
                    call.requireGatewayAuthorization(config)
                    val request = call.receive<AiAnalysisRequest>()
                    call.respond(withContext(Dispatchers.IO) { ai.analyze(request) })
                } catch (error: GatewayException) {
                    if (error.status == HttpStatusCode.ServiceUnavailable) {
                        gatewayLog.error(
                            "POST /v1/analyze unavailable: selectedProvider={}, configured={}, reason={}",
                            ai.id,
                            ai.isConfigured,
                            error.message,
                            error,
                        )
                    }
                    throw error
                }
            }
        }
    }
}
