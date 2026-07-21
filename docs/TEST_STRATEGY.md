# Estratégia de testes

## Pirâmide

- **Unidade:** scoring, deduplicação, políticas de fila e limites do Gateway.
- **Integração:** rotas Ktor sem segredos e DAOs/migrações Room em ambiente Android.
- **Build:** compilação Kotlin, KSP/Room, recursos Compose e empacotamento APK.
- **Aceitação manual:** busca real com conta de teste, cancelamento/retomada, CRM, links externos, backup/restauração e atualização assinada.

## Comandos

```bash
./gradlew :core:domain:test :gateway:test
./gradlew :app:assembleDebug
./gradlew :core:data:connectedDebugAndroidTest
./gradlew test
```

## Cobertura automatizada presente

- motor de pontuação: determinismo, faixas e distinção entre dado ausente e desconhecido;
- deduplicação: website isolado, telefone isolado, nome/endereço, telefone/endereço, IDs equivalentes e escopo por provedor;
- fila de trabalho: prioridade, vencimento e exclusão de clientes/perdidos;
- Gateway: health, proteção de rota, limites diário/mensal, persistência de métricas, retry, cache e autorização explícita de armazenamento Places;
- Room instrumentado: preservação de identificador externo por provedor na seleção de candidatas.

Consulte `VALIDATION_REPORT.md` para distinguir testes executados dos que exigem Android SDK, Google Maven e dispositivo/emulador.

## Critérios de release

- todos os testes verdes;
- schema Room exportado e migração adicionada para qualquer mudança após V1;
- nenhuma credencial encontrada por varredura no APK/repositório;
- APK assinado com a identidade oficial e `versionCode` crescente;
- smoke test em Android 8 (API 26) e Android atual;
- validação das políticas e TTLs dos provedores na data da publicação.
