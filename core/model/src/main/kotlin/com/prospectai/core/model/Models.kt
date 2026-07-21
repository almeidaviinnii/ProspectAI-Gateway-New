package com.prospectai.core.model

import kotlinx.serialization.Serializable

const val DEFAULT_WORKSPACE_ID = "local-default-workspace"

@Serializable
enum class CompanyStatus {
    NEW,
    CONTACTED,
    CONVERSING,
    MEETING,
    PROPOSAL,
    CLIENT,
    LOST,
}

@Serializable
enum class OpportunityBand {
    HIGH,
    MEDIUM,
    LOW,
}

@Serializable
enum class ConfidenceLevel {
    HIGH,
    MEDIUM,
    LOW,
}

@Serializable
enum class MatchStrength {
    STRONG,
    MEDIUM,
    WEAK,
    NONE,
}

@Serializable
enum class SearchState {
    QUEUED,
    RUNNING,
    COMPLETED,
    CANCELLED,
    FAILED,
}

@Serializable
enum class TaskStatus {
    PENDING,
    COMPLETED,
    CANCELLED,
}

@Serializable
enum class DashboardPeriod {
    TODAY,
    LAST_7_DAYS,
    LAST_30_DAYS,
    LAST_90_DAYS,
    ALL,
}

@Serializable
enum class CompanySort {
    SCORE_DESC,
    SCORE_ASC,
    RECENTLY_UPDATED,
    REVIEWS_DESC,
    NAME_ASC,
    CITY_ASC,
    CAPTURED_DESC,
}

@Serializable
enum class ScoreRangeFilter(val minimum: Int?, val maximum: Int?) {
    ALL(null, null),
    FROM_90(90, 100),
    FROM_80(80, 89),
    FROM_70(70, 79),
    FROM_60(60, 69),
    BELOW_60(0, 59),
}

@Serializable
enum class SocialNetwork {
    INSTAGRAM,
    FACEBOOK,
    LINKEDIN,
    YOUTUBE,
    TIKTOK,
    X,
    OTHER,
}

@Serializable
data class SourceMetadata(
    val provider: String,
    val capturedAt: Long,
    val updatedAt: Long = capturedAt,
    val validUntil: Long? = null,
    val confidence: ConfidenceLevel = ConfidenceLevel.MEDIUM,
    val attribution: String? = null,
)

@Serializable
data class Company(
    val id: String,
    val workspaceId: String = DEFAULT_WORKSPACE_ID,
    val placeId: String? = null,
    val providerId: String? = null,
    val name: String,
    val category: String? = null,
    val description: String? = null,
    val address: String? = null,
    val district: String? = null,
    val city: String? = null,
    val state: String? = null,
    val postalCode: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val primaryPhone: String? = null,
    val secondaryPhone: String? = null,
    val whatsapp: String? = null,
    val whatsappVerified: Boolean = false,
    val email: String? = null,
    val website: String? = null,
    val googleMapsUrl: String? = null,
    val googleRating: Double? = null,
    val reviewCount: Int? = null,
    val openingHours: List<String> = emptyList(),
    val businessStatus: String? = null,
    val publicPhotoReferences: List<String> = emptyList(),
    val firstCapturedAt: Long,
    val updatedAt: Long,
    val lastProspectedAt: Long? = null,
    val suppressionUntil: Long? = null,
    val status: CompanyStatus = CompanyStatus.NEW,
    val isFavorite: Boolean = false,
    val favoriteAt: Long? = null,
    val score: Int? = null,
    val opportunityBand: OpportunityBand? = null,
    val scoreConfidence: ConfidenceLevel? = null,
    val analysisSummary: String? = null,
    val source: SourceMetadata? = null,
    val active: Boolean = true,
)

@Serializable
data class SocialProfile(
    val id: String,
    val companyId: String,
    val network: SocialNetwork,
    val url: String,
    val username: String? = null,
    val active: Boolean = true,
    val lastVerifiedAt: Long? = null,
    val source: SourceMetadata? = null,
)

@Serializable
data class SearchFilters(
    val niche: String,
    val city: String? = null,
    val state: String? = null,
    val district: String? = null,
    val postalCode: String? = null,
    val radiusKm: Double? = null,
    val maxResults: Int = 20,
)

@Serializable
data class SearchRun(
    val id: String,
    val filters: SearchFilters,
    val state: SearchState,
    val found: Int = 0,
    val analyzed: Int = 0,
    val ignored: Int = 0,
    val added: Int = 0,
    val updated: Int = 0,
    val startedAt: Long,
    val finishedAt: Long? = null,
    val errorMessage: String? = null,
)

@Serializable
data class TimelineEvent(
    val id: String,
    val companyId: String,
    val type: String,
    val description: String,
    val previousValue: String? = null,
    val newValue: String? = null,
    val occurredAt: Long,
)

@Serializable
data class Note(
    val id: String,
    val companyId: String,
    val text: String,
    val category: String? = null,
    val createdAt: Long,
    val author: String = "Usuário local",
)

@Serializable
data class ProspectTask(
    val id: String,
    val companyId: String,
    val title: String,
    val description: String? = null,
    val status: TaskStatus = TaskStatus.PENDING,
    val priority: Int = 0,
    val dueAt: Long? = null,
    val createdAt: Long,
    val completedAt: Long? = null,
)

@Serializable
data class ScoreFactor(
    val key: String,
    val label: String,
    val points: Int,
    val evidence: String,
)

@Serializable
data class ScoreResult(
    val version: String,
    val score: Int,
    val band: OpportunityBand,
    val confidence: ConfidenceLevel,
    val factors: List<ScoreFactor>,
    val explanation: String,
    val calculatedAt: Long,
    val source: SourceMetadata? = null,
)

@Serializable
data class CompanySignals(
    val websiteKnown: Boolean = false,
    val websiteExists: Boolean = false,
    val websiteUsesHttps: Boolean? = null,
    val websiteMobileFriendly: Boolean? = null,
    val websiteLoadTimeMs: Long? = null,
    val googleProfileCompleteness: Int? = null,
    val googleRating: Double? = null,
    val reviewCount: Int? = null,
    val socialPresenceKnown: Boolean = false,
    val socialProfileCount: Int = 0,
    val hasPhone: Boolean? = null,
    val hasEmail: Boolean? = null,
    val relevantEvidenceCount: Int = 0,
    val expectedEvidenceCount: Int = 8,
)

@Serializable
data class AiAnalysis(
    val id: String,
    val companyId: String,
    val model: String,
    val promptVersion: String,
    val scoreVersion: String,
    val executiveSummary: String,
    val strengths: List<String>,
    val weaknesses: List<String>,
    val opportunities: List<String>,
    val recommendedServices: List<String>,
    val suggestedApproach: String,
    val confidence: ConfidenceLevel,
    val limitations: List<String>,
    val createdAt: Long,
    val source: SourceMetadata? = null,
)

@Serializable
data class DashboardRanking(
    val label: String,
    val count: Int,
)

@Serializable
data class DashboardMetrics(
    val totalCompanies: Int = 0,
    val favorites: Int = 0,
    val contacted: Int = 0,
    val negotiating: Int = 0,
    val meetings: Int = 0,
    val proposals: Int = 0,
    val clients: Int = 0,
    val lost: Int = 0,
    val addedToday: Int = 0,
    val contactedToday: Int = 0,
    val clientsToday: Int = 0,
    val periodSearches: Int = 0,
    val periodCompaniesFound: Int = 0,
    val periodCompaniesAdded: Int = 0,
    val periodFavorites: Int = 0,
    val periodContacted: Int = 0,
    val periodProposals: Int = 0,
    val periodClients: Int = 0,
    val periodLost: Int = 0,
    val categoryRanking: List<DashboardRanking> = emptyList(),
    val cityRanking: List<DashboardRanking> = emptyList(),
)

@Serializable
data class GatewaySearchRequest(
    val searchRunId: String,
    val filters: SearchFilters,
    val pageToken: String? = null,
)

@Serializable
data class GatewayCompany(
    val provider: String,
    val providerId: String? = null,
    val placeId: String? = null,
    val name: String,
    val category: String? = null,
    val description: String? = null,
    val formattedAddress: String? = null,
    val district: String? = null,
    val city: String? = null,
    val state: String? = null,
    val postalCode: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val primaryPhone: String? = null,
    val website: String? = null,
    val googleMapsUrl: String? = null,
    val googleRating: Double? = null,
    val reviewCount: Int? = null,
    val openingHours: List<String> = emptyList(),
    val businessStatus: String? = null,
    val publicPhotoReferences: List<String> = emptyList(),
    val capturedAt: Long,
    val validUntil: Long? = null,
)

@Serializable
data class GatewaySearchResponse(
    val companies: List<GatewayCompany>,
    val nextPageToken: String? = null,
    val provider: String,
    val requestsUsed: Int = 1,
    val warnings: List<String> = emptyList(),
)

@Serializable
data class WebsiteAuditRequest(
    val companyId: String,
    val website: String,
)

@Serializable
data class WebsiteAudit(
    val companyId: String,
    val reachable: Boolean,
    val usesHttps: Boolean,
    val loadTimeMs: Long? = null,
    val title: String? = null,
    val hasViewport: Boolean? = null,
    val emailAddresses: List<String> = emptyList(),
    val socialUrls: List<String> = emptyList(),
    val limitations: List<String> = emptyList(),
    val capturedAt: Long,
    val source: SourceMetadata? = null,
)

@Serializable
data class AiAnalysisRequest(
    val companyId: String,
    val companyName: String,
    val facts: Map<String, String>,
    val score: ScoreResult,
    val offeredServices: List<String>,
)

@Serializable
data class AiAnalysisResponse(
    val executiveSummary: String,
    val strengths: List<String>,
    val weaknesses: List<String>,
    val opportunities: List<String>,
    val recommendedServices: List<String>,
    val suggestedApproach: String,
    val confidence: ConfidenceLevel,
    val limitations: List<String> = emptyList(),
    val model: String,
    val promptVersion: String,
)

@Serializable
data class GatewayHealth(
    val status: String,
    val version: String,
    val providers: Map<String, Boolean>,
)

@Serializable
data class ProviderUsageMetrics(
    val provider: String,
    val dailyRequests: Int,
    val monthlyRequests: Int,
    val successfulCalls: Int,
    val failedCalls: Int,
    val consecutiveFailures: Int,
    val averageResponseMs: Long,
)

@Serializable
data class GatewayUsageMetrics(
    val generatedAt: Long,
    val providers: List<ProviderUsageMetrics>,
)

@Serializable
data class UpdateManifest(
    val versionCode: Int,
    val versionName: String,
    val downloadUrl: String,
    val sha256: String? = null,
    val mandatory: Boolean = false,
    val releaseNotes: List<String> = emptyList(),
)

@Serializable
data class IntegrationStatus(
    val name: String,
    val active: Boolean,
    val lastValidatedAt: Long? = null,
    val lastUsedAt: Long? = null,
    val requestsUsed: Int = 0,
    val requestLimit: Int? = null,
    val lastError: String? = null,
)

@Serializable
data class ServiceOffering(
    val id: String,
    val name: String,
    val description: String? = null,
    val active: Boolean = true,
    val priority: Int = 0,
)
