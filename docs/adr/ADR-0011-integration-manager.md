# ADR-0011: Integration Manager com adaptadores, cache e observabilidade

- Status: Aceito
- Data: 2026-07-15

## Contexto

A V2.0 exige substituição e combinação de provedores sem acoplar o restante do produto, além de cache, recuperação de falhas, limites e métricas.

## Decisão

O Gateway expõe um modelo padronizado e recebe provedores pelo contrato `CompanySearchProvider`. `IntegrationManager` seleciona adaptadores configurados, aplica cache temporário por consulta, repete apenas falhas elegíveis e tenta o próximo provedor. `UsageRegistry` impõe limites diário e mensal e persiste sucesso, falha, falhas consecutivas e latência.

O Android nunca consome payloads originais nem conhece credenciais de provedores.

## Consequências

Novos conectores entram como adaptadores. Em escala horizontal, o arquivo de uso deverá ser substituído por armazenamento compartilhado e atômico, mantendo os mesmos contratos.
