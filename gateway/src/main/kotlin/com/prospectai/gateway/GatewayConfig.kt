package com.prospectai.gateway

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall

data class GatewayConfig(
    val host: String,
    val port: Int,
    val gatewayToken: String?,
    val googlePlacesApiKey: String?,
    val placesDataStorageAllowed: Boolean,
    val placesDataTtlDays: Int,
    val aiApiKey: String?,
    val aiBaseUrl: String,
    val aiModel: String,
    val dailyRequestLimit: Int,
    val websiteTimeoutSeconds: Long,
    val monthlyRequestLimit: Int = 50_000,
    val searchCacheTtlMinutes: Int = 10,
    val providerMaxAttempts: Int = 2,
    val usageLogPath: String = "data/usage.csv",
    val latestApkVersionCode: Int? = null,
    val latestApkVersionName: String? = null,
    val latestApkUrl: String? = null,
    val latestApkSha256: String? = null,
    val latestApkMandatory: Boolean = false,
    val aiProvider: String = "openai",
    val geminiApiKey: String? = null,
    val geminiBaseUrl: String = "https://generativelanguage.googleapis.com/v1beta",
    val geminiModel: String = "gemini-3.5-flash-lite",
    val aiModuleEnabled: Boolean = false,
) {
    companion object {
        fun fromEnvironment(environment: Map<String, String> = System.getenv()): GatewayConfig = GatewayConfig(
            host = environment["HOST"] ?: "0.0.0.0",
            port = environment["PORT"]?.toIntOrNull() ?: 8080,
            gatewayToken = environment["PROSPECTAI_GATEWAY_TOKEN"],
            googlePlacesApiKey = environment["GOOGLE_PLACES_API_KEY"],
            placesDataStorageAllowed = environment["PLACES_DATA_STORAGE_ALLOWED"]?.toBooleanStrictOrNull() ?: false,
            placesDataTtlDays = environment["PLACES_DATA_TTL_DAYS"]?.toIntOrNull()?.coerceIn(1, 365) ?: 30,
            aiApiKey = environment["AI_API_KEY"] ?: environment["OPENAI_API_KEY"],
            aiBaseUrl = (environment["AI_BASE_URL"] ?: "https://api.openai.com/v1").trimEnd('/'),
            aiModel = environment["AI_MODEL"] ?: "gpt-5-mini",
            dailyRequestLimit = environment["DAILY_REQUEST_LIMIT"]?.toIntOrNull()?.coerceAtLeast(1) ?: 2_000,
            websiteTimeoutSeconds = environment["WEBSITE_TIMEOUT_SECONDS"]?.toLongOrNull()?.coerceIn(3, 30) ?: 10,
            monthlyRequestLimit = environment["MONTHLY_REQUEST_LIMIT"]?.toIntOrNull()?.coerceAtLeast(1) ?: 50_000,
            searchCacheTtlMinutes = environment["SEARCH_CACHE_TTL_MINUTES"]?.toIntOrNull()?.coerceIn(0, 1_440) ?: 10,
            providerMaxAttempts = environment["PROVIDER_MAX_ATTEMPTS"]?.toIntOrNull()?.coerceIn(1, 3) ?: 2,
            usageLogPath = environment["USAGE_LOG_PATH"] ?: "data/usage.csv",
            latestApkVersionCode = environment["LATEST_APK_VERSION_CODE"]?.toIntOrNull(),
            latestApkVersionName = environment["LATEST_APK_VERSION_NAME"],
            latestApkUrl = environment["LATEST_APK_URL"],
            latestApkSha256 = environment["LATEST_APK_SHA256"],
            latestApkMandatory = environment["LATEST_APK_MANDATORY"]?.toBooleanStrictOrNull() ?: false,
            aiProvider = environment["AI_PROVIDER"]?.trim()?.lowercase()?.takeIf(String::isNotEmpty)
                ?: if (!(environment["AI_API_KEY"] ?: environment["OPENAI_API_KEY"]).isNullOrBlank()) "openai"
                else if (!environment["GEMINI_API_KEY"].isNullOrBlank()) "gemini"
                else "openai",
            geminiApiKey = environment["GEMINI_API_KEY"],
            geminiBaseUrl = (environment["GEMINI_BASE_URL"] ?: "https://generativelanguage.googleapis.com/v1beta").trimEnd('/'),
            geminiModel = environment["GEMINI_MODEL"]?.trim()?.takeIf(String::isNotEmpty) ?: "gemini-3.5-flash-lite",
            aiModuleEnabled = environment["AI_MODULE_ENABLED"]?.toBooleanStrictOrNull()
                ?: listOf(
                    environment["AI_API_KEY"],
                    environment["OPENAI_API_KEY"],
                    environment["GEMINI_API_KEY"],
                ).any { !it.isNullOrBlank() },
        )
    }
}

class GatewayException(
    override val message: String,
    val status: HttpStatusCode,
) : RuntimeException(message)

@kotlinx.serialization.Serializable
data class ErrorResponse(val message: String)

suspend fun ApplicationCall.requireGatewayAuthorization(config: GatewayConfig) {
    val authLog = application.environment.log
    val expected = config.gatewayToken
    val authorizationHeader = request.headers["Authorization"]

    authLog.info(
        "Gateway authentication debug: authorizationHeaderPresent={}, " +
            "receivedToken={}, environmentTokenLoaded={}, environmentToken={}",
        authorizationHeader != null,
        maskToken(authorizationHeader?.removePrefix("Bearer ")),
        expected != null,
        maskToken(expected),
    )

    if (expected == null) {
        authLog.warn(
            "Gateway authentication rejected: PROSPECTAI_GATEWAY_TOKEN is null; returning HTTP 503.",
        )
        throw GatewayException("Gateway ainda não foi provisionado.", HttpStatusCode.ServiceUnavailable)
    }

    val received = authorizationHeader?.removePrefix("Bearer ")
    if (received == null) {
        authLog.warn(
            "Gateway authentication rejected: Authorization header was not received; returning HTTP 401.",
        )
        throw GatewayException("Token do Gateway inválido.", HttpStatusCode.Unauthorized)
    }
    if (!constantTimeEquals(received, expected)) {
        authLog.warn(
            "Gateway authentication rejected: received token does not match PROSPECTAI_GATEWAY_TOKEN; " +
                "receivedToken={}, environmentToken={}; returning HTTP 401.",
            maskToken(received),
            maskToken(expected),
        )
        throw GatewayException("Token do Gateway inválido.", HttpStatusCode.Unauthorized)
    }

    authLog.info("Gateway authentication accepted: received token matches PROSPECTAI_GATEWAY_TOKEN.")
}

private fun maskToken(token: String?): String = when {
    token == null -> "<null>"
    token.length <= 10 -> "<present:length=${token.length}>"
    else -> "${token.take(6)}...${token.takeLast(4)}"
}

private fun constantTimeEquals(first: String, second: String): Boolean {
    val left = first.toByteArray()
    val right = second.toByteArray()
    var result = left.size xor right.size
    for (index in 0 until maxOf(left.size, right.size)) {
        result = result or (left.getOrElse(index) { 0.toByte() }.toInt() xor right.getOrElse(index) { 0.toByte() }.toInt())
    }
    return result == 0
}
