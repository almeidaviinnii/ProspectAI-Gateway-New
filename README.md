# ProspectAI

Aplicativo Android nativo de prospecção comercial inteligente, local-first, auditável e acompanhado por um Gateway Seguro para integrações que não devem expor chaves no APK.

## Componentes

- `app`: interface Compose, navegação, notificações e execução Android.
- `core:model`: modelos compartilhados e contratos serializáveis.
- `core:domain`: regras de negócio e motor determinístico de pontuação.
- `core:data`: Room, repositórios, integrações, cache e jobs.
- `core:designsystem`: identidade visual e componentes reutilizáveis.
- `gateway`: serviço Ktor para proteger segredos e intermediar provedores.
- `docs/adr`: decisões arquiteturais oficiais.

## Capacidades implementadas

- pesquisa por nicho e localização, persistida e retomável;
- normalização, deduplicação em níveis de confiança e histórico de decisões;
- enriquecimento público por provedor autorizado e auditoria segura de websites;
- CRM local, favoritos independentes, fila priorizada, observações, tarefas e lembretes;
- motor determinístico versionado com fatores, pesos, confiança e histórico;
- diagnóstico de IA que recebe a nota pronta e nunca a altera;
- dashboard, histórico global, busca global, temas claro/escuro e notificações;
- catálogo paginado com faixas de pontuação e ordenações completas;
- favoritos com coração, busca, filtros, resumo e fila operacional;
- proveniência, validade e limpeza automática de dados externos;
- backup/restauração local sem credenciais e verificação de atualização assinada;
- Gateway com autenticação, SSRF protection, limites e registro persistente de consumo.

## Requisitos

- JDK 17.
- Android SDK 36 e Build Tools 36.0.0.
- Gateway configurado para integrações reais.

## Build

```bash
./gradlew test :gateway:test :app:assembleDebug
```

O APK de desenvolvimento é produzido em `app/build/outputs/apk/debug/app-debug.apk`. Consulte `docs/RUNBOOK.md` para provisionamento do Gateway, release assinada e operação.

O status de validação desta entrega está em `docs/VALIDATION_REPORT.md`; um APK só deve ser distribuído depois que os gates Android descritos ali forem aprovados.

## Segurança

Segredos reais não devem ser adicionados ao APK, ao repositório ou aos arquivos de configuração versionados.

## Documentação

- `docs/ARCHITECTURE.md`: visão executável da arquitetura.
- `docs/DATA_AND_RETENTION.md`: banco, proveniência e expiração.
- `docs/SECURITY.md`: modelo de ameaças e controles.
- `docs/REQUIREMENTS_TRACEABILITY.md`: rastreabilidade funcional.
- `docs/TEST_STRATEGY.md`: níveis e comandos de teste.
- `docs/VALIDATION_REPORT.md`: evidências executadas e gates ainda pendentes.
- `IMPLEMENTATION_REPORT.md`: escopo implementado e situação da entrega.
- `docs/adr`: decisões oficiais e seu histórico.
