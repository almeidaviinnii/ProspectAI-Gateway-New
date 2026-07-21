# ADR-0009: Injeção por construtor e composition root explícito

- Status: Aceito
- Data: 2026-07-15

## Contexto

A linha Hilt 2.59+ exige Android Gradle Plugin 9, enquanto a base aprovada utiliza AGP 8.13 e Gradle 8.13. Manter Hilt 2.58 criaria uma dependência de geração de código já congelada somente para preservar o framework.

## Decisão

O aplicativo utiliza injeção por construtor e um único `AppContainer` no processo Android. O container é a composition root: instancia objetos de escopo da aplicação e injeta interfaces nos ViewModels e Workers. Módulos de domínio e dados não dependem do container.

## Consequências

- Não há service locator no domínio nem estado global estático.
- Testes podem fornecer dependências alternativas diretamente nos construtores.
- A troca futura para Hilt/Koin é localizada na composition root e não altera regras de negócio.
- Quando a base migrar para AGP 9, a adoção de Hilt poderá ser reavaliada em novo ADR; não é necessária para funcionalidade ou modularidade.
