# Gateway Seguro

O Gateway protege credenciais e intermedeia Google Places, auditoria de websites e IA.

## Provisionamento

1. Gere um token longo e aleatório para `PROSPECTAI_GATEWAY_TOKEN`.
2. Configure as chaves reais em um cofre de segredos do ambiente de hospedagem.
3. Nunca copie `.env` para o APK ou para o repositório.
4. Use HTTPS em produção.
5. Configure no aplicativo apenas a URL HTTPS e o token limitado da instalação.
6. Monte um volume persistente para `USAGE_LOG_PATH`; o Gateway recompõe contadores diários, mensais e métricas desse registro após reinícios.
7. Ajuste `SEARCH_CACHE_TTL_MINUTES`, `PROVIDER_MAX_ATTEMPTS`, `DAILY_REQUEST_LIMIT` e `MONTHLY_REQUEST_LIMIT` às políticas e quotas contratadas.
8. Para Google Places, habilite `PLACES_DATA_STORAGE_ALLOWED=true` somente depois de validar que o contrato permite a persistência local realizada pelo aplicativo.

## Integration Manager

`IntegrationManager` recebe apenas adaptadores que implementam `CompanySearchProvider`. Ele normaliza o fluxo, reaproveita consultas equivalentes dentro do TTL, repete apenas falhas de servidor e tenta o próximo adaptador configurado. O endpoint autenticado `GET /v1/usage` expõe contagem diária/mensal, sucessos, falhas consecutivas e latência média sem revelar chaves ou payloads.

## Limitações registradas

- O Google Business Profile não é utilizado para geração de leads.
- Uma chave Google válida não ativa o adaptador sozinha; a autorização explícita de armazenamento também é obrigatória.
- A pesquisa por raio usa Geocoding para resolver o centro; se não houver coordenada confiável, a pesquisa textual continua e registra aviso explícito.
- A auditoria de website não executa JavaScript e limita a leitura a 1 MB.
- Redes sociais são identificadas somente quando publicamente vinculadas no website ou retornadas por provedor autorizado.
- O contador é persistente em arquivo e adequado a uma única instância. Hospedagens com múltiplas instâncias devem substituir `UsageRegistry` por um contador compartilhado/atômico (por exemplo, Redis ou banco transacional), preservando o mesmo contrato.
