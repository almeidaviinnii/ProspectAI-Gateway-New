package com.prospectai.gateway

import com.prospectai.core.model.AiAnalysisRequest
import com.prospectai.core.model.AiAnalysisResponse

/** Common contract for every AI analysis provider exposed through /v1/analyze. */
interface AIProvider {
    val id: String
    val isConfigured: Boolean

    fun analyze(request: AiAnalysisRequest): AiAnalysisResponse
}
