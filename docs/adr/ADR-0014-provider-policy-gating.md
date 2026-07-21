# ADR-0014: Ativação de provedor condicionada à política de armazenamento

- Status: Aceito
- Data: 2026-07-15

## Contexto

A Decisão Arquitetural 03 obriga o produto a respeitar retenção e atribuição de cada provedor. A Places API restringe armazenamento de conteúdo e trata Place IDs como exceção. Uma chave válida, isoladamente, não comprova que o contrato permite o modelo local-first do ProspectAI.

## Decisão

O adaptador Google Places permanece indisponível até que o operador configure explicitamente `PLACES_DATA_STORAGE_ALLOWED=true` após validar o contrato aplicável. `PLACES_DATA_TTL_DAYS` representa o prazo autorizado, não uma licença presumida.

Place IDs do Google Maps são preservados sem validade; demais identificadores seguem o TTL do adaptador. Ao vencer a evidência, o worker remove snapshots e campos externos denormalizados, mantendo apenas dados permanentes do CRM. O histórico numérico da pontuação permanece por auditoria, com fatores externos redigidos. IDs em eventos permanentes usam fingerprints. Conteúdo Google exibido recebe atribuição textual “Google Maps”.

## Consequências

Uma instalação sem autorização explícita deve usar outro adaptador com licença compatível. Termos, privacidade, atribuições e políticas devem ser revalidados no processo de release, pois podem mudar independentemente do código.
