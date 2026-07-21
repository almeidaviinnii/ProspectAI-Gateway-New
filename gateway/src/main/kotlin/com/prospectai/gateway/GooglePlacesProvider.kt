package com.prospectai.gateway

import com.prospectai.core.model.GatewayCompany
import com.prospectai.core.model.GatewaySearchRequest
import com.prospectai.core.model.GatewaySearchResponse
import io.ktor.http.HttpStatusCode
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class GooglePlacesProvider(
    private val config: GatewayConfig,
    private val json: Json,
    private val usage: UsageRegistry,
) : CompanySearchProvider {
    override val id: String = "google-places-new"
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    override fun isConfigured(): Boolean = !config.googlePlacesApiKey.isNullOrBlank()

    override fun search(request: GatewaySearchRequest): GatewaySearchResponse {
        val key = config.googlePlacesApiKey
            ?: throw GatewayException("A integração Google Places não está configurada.", HttpStatusCode.ServiceUnavailable)
        if (!config.placesDataStorageAllowed) {
            throw GatewayException(
                "Google Places está configurado, mas PLACES_DATA_STORAGE_ALLOWED não está habilitado.",
                HttpStatusCode.ServiceUnavailable,
            )
        }
        usage.record("google_places")

        val query = buildList {
            add(request.filters.niche.trim())
            request.filters.district?.takeIf { it.isNotBlank() }?.let(::add)
            request.filters.city?.takeIf { it.isNotBlank() }?.let(::add)
            request.filters.state?.takeIf { it.isNotBlank() }?.let(::add)
            request.filters.postalCode?.takeIf { it.isNotBlank() }?.let(::add)
        }.joinToString(", ")
        val center = if (request.filters.radiusKm != null) resolveCenter(request, key) else null

        val body = buildJsonObject {
            put("textQuery", query)
            put("pageSize", request.filters.maxResults.coerceIn(1, 20))
            put("languageCode", "pt-BR")
            put("regionCode", "BR")
            request.pageToken?.let { put("pageToken", it) }
            if (center != null && request.pageToken == null) {
                put("locationBias", buildJsonObject {
                    put("circle", buildJsonObject {
                        put("center", buildJsonObject {
                            put("latitude", center.first)
                            put("longitude", center.second)
                        })
                        put("radius", ((request.filters.radiusKm ?: 1.0) * 1_000).coerceIn(1_000.0, 50_000.0))
                    })
                })
            }
        }
        val httpRequest = Request.Builder()
            .url("https://places.googleapis.com/v1/places:searchText")
            .header("Content-Type", "application/json")
            .header("X-Goog-Api-Key", key)
            .header("X-Goog-FieldMask", FIELD_MASK)
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        return usage.track("google_places") {
            client.newCall(httpRequest).execute().use { response ->
                val responseText = response.body.string()
                if (!response.isSuccessful) {
                    throw GatewayException("Google Places retornou ${response.code}: ${safeProviderMessage(responseText)}", HttpStatusCode.BadGateway)
                }
                val root = json.parseToJsonElement(responseText).jsonObject
                val capturedAt = System.currentTimeMillis()
                val validUntil = capturedAt + config.placesDataTtlDays * 24L * 60 * 60 * 1_000
                val companies = root["places"]?.jsonArray.orEmpty().mapNotNull { place ->
                    parseCompany(place.jsonObject, capturedAt, validUntil)
                }
                GatewaySearchResponse(
                    companies = companies,
                    nextPageToken = root.string("nextPageToken"),
                    provider = "google-places-new",
                    requestsUsed = 1,
                    warnings = buildList {
                        if (request.filters.radiusKm != null && center == null) {
                            add("O centro geográfico não pôde ser resolvido; a consulta foi executada sem o raio solicitado.")
                        }
                    },
                )
            }
        }
    }

    private fun resolveCenter(request: GatewaySearchRequest, key: String): Pair<Double, Double>? {
        val location = listOfNotNull(
            request.filters.district,
            request.filters.city,
            request.filters.state,
            request.filters.postalCode,
            "Brasil",
        ).filter(String::isNotBlank).joinToString(", ")
        if (location == "Brasil") return null
        usage.record("google_geocoding")
        val url = "https://maps.googleapis.com/maps/api/geocode/json".toHttpUrl().newBuilder()
            .addQueryParameter("address", location)
            .addQueryParameter("region", "br")
            .addQueryParameter("language", "pt-BR")
            .addQueryParameter("key", key)
            .build()
        val geocodeRequest = Request.Builder().url(url).get().build()
        return runCatching {
            usage.track("google_geocoding") {
                client.newCall(geocodeRequest).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw GatewayException("Falha ao resolver o centro geográfico.", HttpStatusCode.BadGateway)
                    }
                    val root = json.parseToJsonElement(response.body.string()).jsonObject
                    val point = root["results"]?.jsonArray?.firstOrNull()?.jsonObject
                        ?.get("geometry")?.jsonObject?.get("location")?.jsonObject
                    val latitude = point?.get("lat")?.jsonPrimitive?.doubleOrNull
                    val longitude = point?.get("lng")?.jsonPrimitive?.doubleOrNull
                    if (latitude == null || longitude == null) null else latitude to longitude
                }
            }
        }.getOrNull()
    }

    private fun parseCompany(place: JsonObject, capturedAt: Long, validUntil: Long): GatewayCompany? {
        val name = place["displayName"]?.jsonObject?.string("text") ?: return null
        val addressComponents = place["addressComponents"] as? JsonArray
        return GatewayCompany(
            provider = "google-places-new",
            providerId = place.string("id"),
            placeId = place.string("id"),
            name = name,
            category = place["primaryTypeDisplayName"]?.jsonObject?.string("text"),
            formattedAddress = place.string("formattedAddress"),
            district = addressComponents.component("sublocality") ?: addressComponents.component("administrative_area_level_3"),
            city = addressComponents.component("locality") ?: addressComponents.component("administrative_area_level_2"),
            state = addressComponents.componentShort("administrative_area_level_1"),
            postalCode = addressComponents.component("postal_code"),
            latitude = place["location"]?.jsonObject?.get("latitude")?.jsonPrimitive?.doubleOrNull,
            longitude = place["location"]?.jsonObject?.get("longitude")?.jsonPrimitive?.doubleOrNull,
            primaryPhone = place.string("nationalPhoneNumber") ?: place.string("internationalPhoneNumber"),
            website = place.string("websiteUri"),
            googleMapsUrl = place.string("googleMapsUri"),
            googleRating = place["rating"]?.jsonPrimitive?.doubleOrNull,
            reviewCount = place["userRatingCount"]?.jsonPrimitive?.intOrNull,
            openingHours = place["regularOpeningHours"]?.jsonObject?.get("weekdayDescriptions")?.jsonArray
                ?.mapNotNull { it.jsonPrimitive.contentOrNull }.orEmpty(),
            businessStatus = place.string("businessStatus"),
            publicPhotoReferences = place["photos"]?.jsonArray
                ?.mapNotNull { it.jsonObject.string("name") }
                ?.take(10)
                .orEmpty(),
            capturedAt = capturedAt,
            validUntil = validUntil,
        )
    }

    private fun safeProviderMessage(value: String): String = runCatching {
        json.parseToJsonElement(value).jsonObject["error"]?.jsonObject?.string("message")
    }.getOrNull() ?: "falha no provedor"

    private fun JsonObject.string(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull

    private fun JsonArray?.component(type: String): String? = this?.firstNotNullOfOrNull { element ->
        val item = element.jsonObject
        val types = item["types"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull }.orEmpty()
        item.string("longText").takeIf { type in types }
    }

    private fun JsonArray?.componentShort(type: String): String? = this?.firstNotNullOfOrNull { element ->
        val item = element.jsonObject
        val types = item["types"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull }.orEmpty()
        item.string("shortText").takeIf { type in types }
    }

    private companion object {
        const val FIELD_MASK = "places.id,places.displayName,places.primaryTypeDisplayName,places.formattedAddress,places.addressComponents,places.location,places.nationalPhoneNumber,places.internationalPhoneNumber,places.websiteUri,places.googleMapsUri,places.rating,places.userRatingCount,places.regularOpeningHours.weekdayDescriptions,places.businessStatus,places.photos.name,nextPageToken"
    }
}
