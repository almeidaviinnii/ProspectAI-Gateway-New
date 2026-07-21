# ADR-0012: Identificadores oficiais externos preservados por provedor

- Status: Aceito
- Data: 2026-07-15

## Contexto

Uma empresa pode aparecer em provedores diferentes. Manter apenas um `providerId` canônico perderia identidades anteriores e prejudicaria deduplicação futura.

## Decisão

Cada par provedor/identificador e Place ID observado é preservado em `company_external_identifiers`, ligado à empresa e acompanhado de captura e validade. O registro canônico pode refletir a fonte mais recente, enquanto a busca de candidatas consulta todos os identificadores históricos.

## Consequências

Fusões fortes permanecem rastreáveis entre provedores. Retenção de cada identificador deve seguir a política oficial do respectivo adaptador.
