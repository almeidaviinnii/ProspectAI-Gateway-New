# ADR-0008: Atualizações assinadas e migrações

- Status: Aceito
- Data: 2026-07-15

## Decisão

Todas as versões usam o mesmo application ID e a mesma chave de assinatura. O versionCode sempre aumenta. Alterações de banco usam migrações testadas e nunca dependem de reinstalação.

A chave de assinatura de produção deve possuir backup seguro fora do repositório.
