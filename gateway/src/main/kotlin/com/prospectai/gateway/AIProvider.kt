package com.prospectai.gateway

import com.prospectai.core.model.AiAnalysisRequest
import com.prospectai.core.model.AiAnalysisResponse

/** Stable analysis contract exposed through /v1/analyze. */
interface AnalysisProvider {
    val id: String
    val isConfigured: Boolean

    fun analyze(request: AiAnalysisRequest): AiAnalysisResponse
}

/** Optional external-AI module. Kept separate so it can be re-enabled later. */
interface AIProvider : AnalysisProvider
