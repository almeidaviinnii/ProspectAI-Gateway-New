package com.prospectai.gateway

import com.prospectai.core.model.WebsiteAudit
import com.prospectai.core.model.WebsiteAuditRequest
import com.prospectai.core.model.SourceMetadata
import com.prospectai.core.model.ConfidenceLevel
import io.ktor.http.HttpStatusCode
import java.net.InetAddress
import java.net.URI
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request

class WebsiteAuditor(config: GatewayConfig, private val usage: UsageRegistry) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(config.websiteTimeoutSeconds, TimeUnit.SECONDS)
        .readTimeout(config.websiteTimeoutSeconds, TimeUnit.SECONDS)
        .followRedirects(false)
        .dns { hostname -> resolvePublicAddresses(hostname) }
        .build()

    fun audit(request: WebsiteAuditRequest): WebsiteAudit {
        usage.record("website_audit")
        val startedAt = System.nanoTime()
        return usage.track("website_audit") {
            requestFollowingSafeRedirects(request.website).use { response ->
                val elapsedMs = (System.nanoTime() - startedAt) / 1_000_000
                val html = response.peekBody(MAX_BODY_BYTES).string()
                val finalUrl = response.request.url.toString()
                val capturedAt = System.currentTimeMillis()
                WebsiteAudit(
                    companyId = request.companyId,
                    reachable = response.isSuccessful,
                    usesHttps = finalUrl.startsWith("https://", ignoreCase = true),
                    loadTimeMs = elapsedMs,
                    title = TITLE_REGEX.find(html)?.groupValues?.getOrNull(1)?.stripTags()?.trim()?.take(200),
                    hasViewport = VIEWPORT_REGEX.containsMatchIn(html),
                    emailAddresses = EMAIL_REGEX.findAll(html).map { it.value.lowercase() }.distinct().take(10).toList(),
                    socialUrls = SOCIAL_REGEX.findAll(html).map { it.value.trimEnd('"', '\'', '>', ')') }.distinct().take(20).toList(),
                    limitations = buildList {
                        if (response.code >= 400) add("O website respondeu com HTTP ${response.code}.")
                        if (response.body.contentLength() > MAX_BODY_BYTES) add("A análise foi limitada ao primeiro 1 MB da página.")
                        add("Auditoria estrutural; conteúdo carregado somente por JavaScript pode não ser identificado.")
                    },
                    capturedAt = capturedAt,
                    source = SourceMetadata(
                        provider = "public-website",
                        capturedAt = capturedAt,
                        updatedAt = capturedAt,
                        validUntil = capturedAt + 30L * 24 * 60 * 60 * 1_000,
                        confidence = ConfidenceLevel.MEDIUM,
                        attribution = finalUrl,
                    ),
                )
            }
        }
    }

    private fun requestFollowingSafeRedirects(initialUrl: String): okhttp3.Response {
        var current = normalizeUrl(initialUrl)
        repeat(MAX_REDIRECTS + 1) { redirectCount ->
            validatePublicUrl(current)
            val response = client.newCall(
                Request.Builder()
                    .url(current)
                    .header("User-Agent", "ProspectAI-WebsiteAudit/1.0")
                    .header("Accept", "text/html,application/xhtml+xml")
                    .get()
                    .build(),
            ).execute()
            if (response.code !in 300..399) return response
            val location = response.header("Location")
            response.close()
            if (location == null || redirectCount == MAX_REDIRECTS) {
                throw GatewayException("Redirecionamento inválido ou excessivo no website.", HttpStatusCode.BadGateway)
            }
            current = URI(current).resolve(location).toString()
        }
        error("Fluxo de redirecionamento inesperado")
    }

    private fun validatePublicUrl(value: String) {
        val uri = runCatching { URI(value) }.getOrElse {
            throw GatewayException("Website inválido.", HttpStatusCode.BadRequest)
        }
        if (uri.scheme !in setOf("http", "https") || uri.host.isNullOrBlank()) {
            throw GatewayException("Somente websites HTTP ou HTTPS são permitidos.", HttpStatusCode.BadRequest)
        }
        if (uri.port !in setOf(-1, 80, 443)) {
            throw GatewayException("Somente as portas web 80 e 443 podem ser auditadas.", HttpStatusCode.BadRequest)
        }
        resolvePublicAddresses(uri.host)
    }

    private fun resolvePublicAddresses(host: String): List<InetAddress> {
        val addresses = runCatching { InetAddress.getAllByName(host).toList() }.getOrElse {
            throw GatewayException("Não foi possível resolver o website.", HttpStatusCode.BadGateway)
        }
        if (addresses.any {
                it.isAnyLocalAddress || it.isLoopbackAddress || it.isLinkLocalAddress || it.isSiteLocalAddress ||
                    it.isMulticastAddress || (it.address.size == 16 && (it.address[0].toInt() and 0xFE) == 0xFC)
            }
        ) {
            throw GatewayException("Endereços locais ou privados não podem ser auditados.", HttpStatusCode.BadRequest)
        }
        return addresses
    }

    private fun normalizeUrl(value: String): String {
        val trimmed = value.trim()
        return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed else "https://$trimmed"
    }

    private fun String.stripTags(): String = replace("<[^>]+>".toRegex(), "")

    private companion object {
        const val MAX_BODY_BYTES = 1_048_576L
        const val MAX_REDIRECTS = 4
        val TITLE_REGEX = "<title[^>]*>(.*?)</title>".toRegex(setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        val VIEWPORT_REGEX = "<meta[^>]+name=[\\\"']viewport[\\\"'][^>]*>".toRegex(RegexOption.IGNORE_CASE)
        val EMAIL_REGEX = "[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}".toRegex(RegexOption.IGNORE_CASE)
        val SOCIAL_REGEX = "https?://(?:www\\.)?(?:instagram\\.com|facebook\\.com|linkedin\\.com|youtube\\.com|youtu\\.be|tiktok\\.com|x\\.com|twitter\\.com|wa\\.me|api\\.whatsapp\\.com)/[^\\s<]+".toRegex(RegexOption.IGNORE_CASE)
    }
}
