# ADR-0002: Deduplicação por níveis de confiança

- Status: Aceito
- Data: 2026-07-15

## Decisão

Correspondências fortes podem produzir fusão automática: Place ID, identificador oficial, Place ID equivalente, telefone com endereço e nome com endereço normalizado.

Website isolado, telefone compartilhado e domínio semelhante são correspondências médias. Nome semelhante é correspondência fraca. Correspondências médias ou fracas nunca produzem fusão automática isoladamente.

Toda decisão de fusão registra empresa mantida, empresa recebida, critérios, força, data e dados relevantes.
