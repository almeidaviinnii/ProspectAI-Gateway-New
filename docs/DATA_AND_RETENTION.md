# Dados, proveniência e retenção

## Fonte de verdade

O banco `prospectai.db` é local ao sandbox do aplicativo. O Android desativa backup automático para impedir cópia involuntária. O backup explícito é controlado pelo usuário.

## Principais tabelas

| Grupo | Tabelas |
|---|---|
| Empresas | `companies`, `company_external_identifiers`, `favorites`, `social_profiles` |
| CRM | `status_history`, `notes`, `tasks`, `audit_events` |
| Pesquisa | `search_runs`, `search_results` |
| Proveniência | `source_snapshots`, `website_audits`, `integration_metadata` |
| Inteligência | `score_history`, `ai_analyses`, `deduplication_decisions` |
| Configuração | `service_catalog`; preferências não relacionais no DataStore |

## Metadados externos

Snapshots e registros derivados guardam provedor, captura, atualização, validade e confiança. Identificadores oficiais são preservados por provedor em `company_external_identifiers`; dados normalizados e payload bruto ficam separados dos dados criados pelo usuário.

## Política de retenção

- CRM, favoritos, observações, tarefas, histórico e configurações permanecem até ação explícita do usuário.
- A integração Places só é ativada com `PLACES_DATA_STORAGE_ALLOWED=true`, após validação do contrato aplicável; o TTL é configurado por `PLACES_DATA_TTL_DAYS`.
- A política é executada na inicialização, antes de exportar backup e por um Worker diário. Ela remove identificadores externos vencidos, snapshots e perfis sociais vencidos, remove auditorias antigas e elimina análises derivadas cuja evidência expirou, inclusive o fallback local transparente.
- A empresa do CRM não é apagada quando a fonte expira. Campos públicos voláteis são limpos e a confiança é marcada como baixa.
- Place IDs do Google Maps são preservados sem prazo porque a política oficial os isenta das restrições de cache; identificadores de outros provedores usam sua validade declarada.
- Nome, status, favoritos, observações, tarefas e eventos próprios permanecem; contatos, localização, presença digital, score corrente e resumo derivado são removidos quando a evidência externa vence.
- O histórico numérico do motor permanece para auditoria, mas fatores textuais que contenham evidência externa são redigidos após o vencimento da fonte. IDs externos em eventos permanentes são registrados apenas como fingerprints.

Referência operacional: [políticas e atribuições da Places API](https://developers.google.com/maps/documentation/places/web-service/policies). A autorização deve ser revalidada antes de cada release.

## Backup

O arquivo ZIP contém um manifesto versionado, preferências não sensíveis e uma cópia consistente do SQLite após checkpoint do WAL. Token do Gateway, chaves e material do Keystore nunca são exportados. A restauração valida integridade e identidade do banco e reinicia o processo.
