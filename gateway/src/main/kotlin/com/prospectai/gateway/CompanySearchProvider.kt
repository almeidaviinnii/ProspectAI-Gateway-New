package com.prospectai.gateway

import com.prospectai.core.model.GatewaySearchRequest
import com.prospectai.core.model.GatewaySearchResponse

/** Adapter contract that prevents the application pipeline from depending on a provider payload. */
interface CompanySearchProvider {
    val id: String
    fun isConfigured(): Boolean
    fun search(request: GatewaySearchRequest): GatewaySearchResponse
}
