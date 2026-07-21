# Runbook

## Android

Requisitos: JDK 17, Android SDK/Build Tools 36 e Gradle Wrapper.

```bash
./gradlew test :gateway:test :app:assembleDebug
```

Para release, configure um keystore fora do repositório e injete suas propriedades pelo ambiente/CI. Mantenha `applicationId`, chave de assinatura e incremento de `versionCode`.

Antes de ativar Google Places em uma distribuição, publique Termos de Uso e Política de Privacidade, valide a licença de armazenamento, revise as atribuições visuais e registre a aprovação no ADR/release checklist.

## Gateway

```bash
./gradlew :gateway:installDist
docker build -f gateway/Dockerfile -t prospectai-gateway .
```

Variáveis obrigatórias para operação real:

- `PROSPECTAI_GATEWAY_TOKEN`;
- `GOOGLE_PLACES_API_KEY` com Places API (New) e Geocoding autorizadas;
- `PLACES_DATA_STORAGE_ALLOWED=true` somente após confirmar que o contrato aplicável permite o armazenamento realizado pelo ProspectAI;
- `PLACES_DATA_TTL_DAYS` conforme o prazo autorizado pelo provedor;
- `AI_API_KEY`, `AI_BASE_URL` e `AI_MODEL` quando a IA for ativada;
- `USAGE_LOG_PATH` em volume persistente;
- `DAILY_REQUEST_LIMIT` e `MONTHLY_REQUEST_LIMIT` conforme contrato/política;
- `SEARCH_CACHE_TTL_MINUTES` e `PROVIDER_MAX_ATTEMPTS` de acordo com custo e tolerância a falhas.

Para publicar atualização configure `LATEST_APK_VERSION_CODE`, `LATEST_APK_VERSION_NAME`, `LATEST_APK_URL` HTTPS e `LATEST_APK_SHA256`.

## Primeira execução

1. Abra Configurações no Android.
2. Informe URL HTTPS e token limitado do Gateway.
3. Teste a integração.
4. Ajuste cidade, UF, raio, bloqueio de reprospecção e catálogo de serviços.
5. Execute uma pesquisa pequena e confira metadados/origem antes de ampliar limites.

## Incidentes

- `401`: rotacione/reconfigure o token limitado.
- `429`: limite diário ou mensal atingido; verifique `usage.csv` antes de ampliar.
- falha de Places/Geocoding: valide APIs habilitadas, billing, restrições e quotas.
- pesquisa interrompida: use Retomar; o run continua do token persistido.
- banco restaurado: o app reinicia; reconfigure o token, que não faz parte do backup.
