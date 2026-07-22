package com.prospectai.gateway

import com.prospectai.core.model.AiAnalysisRequest
import com.prospectai.core.model.AiAnalysisResponse
import com.prospectai.core.model.ScoreFactor

/**
 * Deterministic analysis provider. It performs no network calls and preserves
 * the existing /v1/analyze request and response contract used by Android.
 */
class RuleEngineProvider : AnalysisProvider {
    override val id: String = "rule-engine"
    override val isConfigured: Boolean = true

    override fun analyze(request: AiAnalysisRequest): AiAnalysisResponse {
        val facts = NormalizedFacts(request.facts)
        val factorKeys = request.score.factors.map(ScoreFactor::key).toSet()
        val digitalQualityScore = (100 - request.score.score).coerceIn(0, 100)
        val strengths = linkedSetOf<String>()
        val weaknesses = linkedSetOf<String>()
        val opportunities = linkedSetOf<String>()
        val services = linkedSetOf<String>()
        val limitations = linkedSetOf<String>()

        fun issue(weakness: String, opportunity: String, vararg serviceHints: String) {
            weaknesses += weakness
            opportunities += opportunity
            request.offeredServices.firstOrNull { offered ->
                serviceHints.any { hint -> offered.contains(hint, ignoreCase = true) }
            }?.let(services::add)
        }

        when {
            factorKeys.contains("website_absent") || facts.text("website") == "não identificado" ->
                issue("Website não identificado.", "Criar uma landing page ou site institucional.", "landing", "site")
            facts.bool("website_acessivel") == false ->
                issue("O website não respondeu à auditoria.", "Revisar disponibilidade, hospedagem e funcionamento do site.", "site", "landing")
            else -> strengths += "Website público identificado."
        }

        if (factorKeys.contains("website_https") || facts.bool("website_https") == false) {
            issue(
                "O site não utiliza HTTPS, o que pode transmitir menor confiança.",
                "Implantar certificado SSL e redirecionamento obrigatório para HTTPS.",
                "ssl", "site",
            )
        } else if (facts.bool("website_https") == true) strengths += "Website protegido por HTTPS."

        val loadTimeMs = facts.long("website_tempo_ms")
        if (factorKeys.contains("website_slow") || (loadTimeMs != null && loadTimeMs > SLOW_SITE_MS)) {
            issue(
                "O carregamento do site é lento${loadTimeMs?.let { ": ${it} ms" }.orEmpty()} e pode reduzir conversões.",
                "Melhorar velocidade e desempenho do site.",
                "performance", "velocidade", "site",
            )
        } else if (loadTimeMs != null) strengths += "Tempo de carregamento dentro do limite de ${SLOW_SITE_MS} ms."

        if (factorKeys.contains("website_mobile") || facts.bool("website_mobile") == false) {
            issue(
                "A auditoria não confirmou uma experiência adequada em celulares.",
                "Melhorar a experiência mobile e o layout responsivo.",
                "mobile", "site",
            )
        } else if (facts.bool("website_mobile") == true) strengths += "Configuração mobile identificada."

        if (facts.text("website_titulo").isNullOrBlank() && facts.bool("website_acessivel") == true) {
            issue(
                "O site não apresentou um título de página identificável, um sinal básico de SEO ausente.",
                "Corrigir título e fundamentos de SEO local.",
                "seo",
            )
        }

        if (facts.int("redes_identificadas") == 0 || factorKeys.contains("no_social")) {
            issue(
                "Nenhuma rede social foi identificada nas fontes auditadas.",
                "Fortalecer a presença digital com perfis sociais atualizados.",
                "social", "rede",
            )
        } else if ((facts.int("redes_identificadas") ?: 0) > 0) strengths += "Presença social pública identificada."

        if (!facts.hasWhatsApp()) {
            issue(
                "Um canal público de WhatsApp não foi confirmado.",
                "Inserir WhatsApp como canal rápido de atendimento.",
                "whatsapp", "contato",
            )
        } else strengths += "Canal de WhatsApp identificado."

        if (factorKeys.contains("profile_incomplete")) {
            issue(
                "O perfil do Google apresenta informações incompletas ou inconsistentes.",
                "Atualizar e padronizar o Google Meu Negócio.",
                "google", "perfil", "local",
            )
        }
        if (factorKeys.contains("few_reviews")) {
            issue("O volume de avaliações públicas é baixo.", "Criar uma estratégia ética para ampliar avaliações no Google.", "reputação", "google")
        }
        if (factorKeys.contains("low_rating")) {
            issue("A nota pública possui espaço para melhoria.", "Melhorar reputação e resposta às avaliações.", "reputação", "google")
        }

        request.facts["limitacoes_auditoria"]?.takeIf(String::isNotBlank)?.let {
            limitations += "A auditoria registrou limitações: $it"
        }
        if (request.facts.isEmpty()) limitations += "Nenhum fato de auditoria foi fornecido."
        if (weaknesses.isEmpty()) strengths += "Nenhuma fragilidade crítica foi confirmada pelas regras disponíveis."

        val classification = when (digitalQualityScore) {
            in 90..100 -> "Excelente presença digital."
            in 70..89 -> "Boa presença digital."
            in 50..69 -> "Existem oportunidades importantes de melhoria."
            else -> "Alta prioridade comercial."
        }
        return AiAnalysisResponse(
            executiveSummary = "${request.companyName}: qualidade digital $digitalQualityScore/100. $classification",
            strengths = strengths.toList(),
            weaknesses = weaknesses.toList(),
            opportunities = opportunities.toList(),
            recommendedServices = services.toList(),
            suggestedApproach = opportunities.firstOrNull()?.let { "Apresente a evidência confirmada e proponha como primeiro passo: $it" }
                ?: "Reconheça os pontos positivos encontrados e ofereça uma revisão periódica da presença digital.",
            confidence = request.score.confidence,
            limitations = limitations.toList(),
            model = id,
            promptVersion = "rule-engine-v1.0.0",
        )
    }

    private class NormalizedFacts(private val values: Map<String, String>) {
        fun text(key: String): String? = values[key]?.trim()?.lowercase()
        fun bool(key: String): Boolean? = text(key)?.toBooleanStrictOrNull()
        fun int(key: String): Int? = text(key)?.toIntOrNull()
        fun long(key: String): Long? = text(key)?.toLongOrNull()
        fun hasWhatsApp(): Boolean = values.any { (key, value) ->
            key.contains("whatsapp", ignoreCase = true) && value.isNotBlank() &&
                !value.equals("false", ignoreCase = true) && !value.equals("não identificado", ignoreCase = true)
        }
    }

    private companion object {
        const val SLOW_SITE_MS = 3_000L
    }
}
