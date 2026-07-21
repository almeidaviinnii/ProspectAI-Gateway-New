# Relatório de validação

- Data: 2026-07-15
- Versão avaliada: 1.0.0

## Resultado comprovado

Comando executado com recompilação forçada:

```bash
./gradlew :core:domain:test :gateway:test --rerun-tasks --no-daemon
```

Resultado: `BUILD SUCCESSFUL`, 17 testes, zero falhas, zero erros e zero ignorados.

| Suíte | Casos | Resultado |
|---|---:|---|
| `DeduplicationEngineTest` | 7 | Aprovado |
| `ScoringEngineTest` | 2 | Aprovado |
| `WorkQueuePolicyTest` | 3 | Aprovado |
| `ApplicationTest` | 1 | Aprovado |
| `UsageRegistryTest` | 2 | Aprovado |
| `IntegrationManagerTest` | 1 | Aprovado |
| `GatewayConfigTest` | 1 | Aprovado |

O teste Ktor confirmou `200` no health e `401` na rota de uso sem autenticação.

Verificações estruturais adicionais:

- os 44 arquivos Kotlin foram analisados por um parser independente, sem nós sintáticos de erro;
- 21 recursos XML e o catálogo TOML foram parseados com sucesso;
- as 48 consultas anotadas com `@Query` foram preparadas pelo SQLite contra um esquema equivalente às 17 entidades, sem erro de sintaxe ou coluna inexistente;
- o Gradle Wrapper JAR foi validado como ZIP e seu SHA-256 é `81a82aaea5abcc8ff68b3dfcb58b3c3c429378efd98e7433460610fecd7ae45f`;
- a varredura do pacote não encontrou chave real, chave privada, configuração temporária de proxy, `TODO`, `FIXME` ou `HACK`.

A análise sintática independente detecta estrutura inválida, mas não substitui a compilação Android nem o processamento KSP/Room.

## Validação Android não concluída neste ambiente

A tentativa de executar `:app:compileDebugKotlin` parou em `:app:checkDebugAarMetadata`, antes da compilação Kotlin, porque o ambiente não consegue resolver artefatos do Google Maven e não possui em cache AndroidX, Compose, Room, WorkManager e Paging. Uma verificação offline isolada de `:core:data:compileDebugKotlin` confirmou a mesma causa: `room-compiler`, `core-ktx`, Room, DataStore e Paging não estavam disponíveis. Não foi um erro de código Kotlin reportado pelo compilador; as dependências não chegaram a ser fornecidas ao build.

Consequências objetivas:

- a UI Compose, os DAOs/KSP Room e o teste instrumentado foram revisados estaticamente, mas não compilados aqui;
- nenhum APK foi produzido ou apresentado como validado;
- a suíte `ProspectAiDatabaseTest` está pronta, mas precisa de emulador/dispositivo e das dependências Android.

## Gates obrigatórios para release

Em um ambiente com acesso a `google()` e `mavenCentral()`:

```bash
./gradlew clean test :gateway:test :app:assembleDebug
./gradlew :core:data:connectedDebugAndroidTest
```

Depois, executar smoke tests em API 26 e API 36, fluxo real de pesquisa com conta de teste, cancelamento/retomada, CRM, backup/restauração, retenção, notificação, instalação de atualização assinada e varredura de credenciais no APK.

Uma release com Google Places também exige aprovação contratual documentada, Termos de Uso/Privacidade públicos e revisão de atribuições antes de definir `PLACES_DATA_STORAGE_ALLOWED=true`.
