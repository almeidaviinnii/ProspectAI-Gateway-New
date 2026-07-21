# Rastreabilidade funcional

| Requisito | Implementação principal | Evidência persistida |
|---|---|---|
| Pesquisa por nicho/localização/raio | `HomeScreen`, `SearchCoordinator`, `GooglePlacesProvider` | `search_runs`, `search_results` |
| Execução em segundo plano/cancelamento/retomada | `SearchWorker` foreground, WorkManager, estado do run | contadores, `nextPageToken` e resultados idempotentes |
| Dados completos da empresa | `GatewayCompany`, `CompanyEntity`, detalhe Compose | `companies`, `source_snapshots` |
| Catálogo por nota/faixa/recência/avaliações/nome/cidade/captura | consulta Room paginada e filtros Compose | parâmetros do `PagingSource` |
| Redes e WhatsApp públicos | auditoria de website e normalização | `social_profiles`, campos de contato |
| Deduplicação forte/média/fraca | `DeduplicationEngine` e seleção local de candidatas | `deduplication_decisions`, evento de merge |
| Nunca fundir por website/telefone isolado | `shouldMergeAutomatically` somente com critério forte | decisão média não mesclada |
| Proveniência e validade | `SourceMetadata`, entidades externas | provedor/captura/atualização/validade/confiança |
| Retenção por política | `ExternalDataRetentionService`, inicialização, backup e `RetentionWorker` | limpeza sem apagar CRM |
| Política Google e atribuição | gate `PLACES_DATA_STORAGE_ALLOWED`, TTL e rótulo “Google Maps” | configuração do Gateway e ADR-0014 |
| Score determinístico | `ScoringEngine` versionado | `score_history`, fatores e explicação |
| IA sem definir nota | `AIProvider` recebe `ScoreResult`; prompt comum proíbe recálculo | `ai_analyses.scoreVersion` |
| Favoritos independentes | `favorites` separado de `companies.status` | evento favoritar/desfavoritar |
| Favoritos com coração, busca, filtros, resumo e ordenação | `FavoritesScreen`, consulta reativa | `favorites`, status e score atuais |
| CRM e bloqueio de reprospecção | `CompanyRepositoryImpl`, `CrmPolicy` | `status_history`, `suppressionUntil` |
| Renovação de oportunidade perdida | `SearchCoordinator` após expiração | `OPPORTUNITY_RENEWED` |
| Observações e tarefas | detalhe, repositório, lembrete periódico | `notes`, `tasks`, `audit_events` |
| Fila priorizada | `WorkQueuePolicy` | favoritos/status/score/prazos |
| Dashboard | `observeDashboard` e `DashboardScreen` | agregação SQL reativa |
| Busca global | consulta por empresa, nota, tarefa, IA e social | resultado Room reativo |
| Configuração de serviços | catálogo editável e ativável | `service_catalog` |
| Proteção de chaves | Gateway e `SecureTokenStore` | chaves somente no servidor |
| Limites e registro de uso | `UsageRegistry` | CSV persistente e metadados locais |
| Cache/retry/adaptadores alternativos | `IntegrationManager`, `CompanySearchProvider` | métricas, warnings e cache temporário |
| Backup/restauração | `BackupService` e seletor de documento | ZIP versionado, sem token |
| Atualizações | manifesto do Gateway e ação de download | assinatura conferida pelo Android |
| Tema e notificações | Material 3, DataStore, canais e Workers | preferências locais |
| Inicialização/sem rede/erro/lista vazia/conclusão | splash, erro recuperável, fila WorkManager, estados Compose e Snackbar | estado local/WorkManager |
| ADR evolutivo | `docs/adr` | arquivos versionados |
