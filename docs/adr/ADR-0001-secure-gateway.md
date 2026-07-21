# ADR-0001: Gateway seguro para chaves sensíveis

- Status: Aceito
- Data: 2026-07-15

## Decisão

Serviços que recomendam uso por backend serão acessados por um Gateway Seguro. As chaves reais não serão incluídas no APK, no banco Room nem nos logs do Android.

O Gateway controla segredos, limites, métricas, retentativas e chamadas externas. O aplicativo armazena somente endereço do Gateway, estado da integração e token limitado da instalação.

## Consequência

CRM, favoritos, tarefas e demais dados pessoais continuam locais. Um modo direto somente poderá ser acrescentado como alternativa explicitamente marcada como segurança reduzida.
