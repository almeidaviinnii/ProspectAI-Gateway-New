package com.prospectai.gateway

import com.prospectai.core.model.GatewaySearchRequest
import com.prospectai.core.model.GatewaySearchResponse
import io.ktor.http.HttpStatusCode
import java.util.concurrent.ConcurrentHashMap

class IntegrationManager(
    private val providers: List<CompanySearchProvider>,
    cacheTtlMinutes: Int,
    private val maxAttempts: Int,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val cacheTtlMs = cacheTtlMinutes * 60L * 1_000
    private val cache = ConcurrentHashMap<String, CacheEntry>()

    fun search(request: GatewaySearchRequest): GatewaySearchResponse {
        val now = clock()
        val cacheKey = request.cacheKey()
        cache[cacheKey]?.takeIf { cacheTtlMs > 0 && it.expiresAt > now }?.let { cached ->
            return cached.response.copy(warnings = cached.response.warnings + "Resultado reutilizado do cache seguro do Gateway.")
        }
        cache.remove(cacheKey)

        val available = providers.filter(CompanySearchProvider::isConfigured)
        if (available.isEmpty()) {
            throw GatewayException("Nenhum provedor de pesquisa está configurado.", HttpStatusCode.ServiceUnavailable)
        }

        val warnings = mutableListOf<String>()
        var lastFailure: GatewayException? = null
        for (provider in available) {
            for (attempt in 1..maxAttempts) {
                try {
                    val response = provider.search(request)
                    if (cacheTtlMs > 0) cache[cacheKey] = CacheEntry(response, now + cacheTtlMs)
                    return response.copy(warnings = warnings + response.warnings)
                } catch (error: GatewayException) {
                    lastFailure = error
                    val retryable = error.status.value >= 500
                    if (!retryable || attempt == maxAttempts) break
                    warnings += "${provider.id}: tentativa $attempt falhou; nova tentativa executada."
                }
            }
            warnings += "${provider.id}: indisponível; próximo adaptador avaliado."
        }
        throw lastFailure ?: GatewayException("As integrações de pesquisa estão indisponíveis.", HttpStatusCode.BadGateway)
    }

    private fun GatewaySearchRequest.cacheKey(): String = listOf(
        filters.niche.trim().lowercase(),
        filters.city?.trim()?.lowercase(),
        filters.state?.trim()?.lowercase(),
        filters.district?.trim()?.lowercase(),
        filters.postalCode?.filter(Char::isDigit),
        filters.radiusKm?.toString(),
        filters.maxResults.toString(),
        pageToken,
    ).joinToString("|")

    private data class CacheEntry(val response: GatewaySearchResponse, val expiresAt: Long)
}
