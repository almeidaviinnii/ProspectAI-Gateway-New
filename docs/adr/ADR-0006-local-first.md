# ADR-0006: Android local-first

- Status: Aceito
- Data: 2026-07-15

## Decisão

Room é a fonte local de verdade. Interface e domínio não consomem respostas externas diretamente. Integrações normalizam e persistem dados antes que a interface os observe.

Operações locais são imediatas. Operações de rede usam filas persistentes, são idempotentes e podem ser retomadas.
