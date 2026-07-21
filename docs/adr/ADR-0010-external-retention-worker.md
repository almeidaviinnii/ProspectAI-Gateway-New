# ADR-0010: Retenção externa aplicada por manutenção periódica

- Status: Aceito
- Data: 2026-07-15

## Decisão

Validade não será apenas informativa. Um Worker diário elimina payloads e derivados externos vencidos, desativa perfis expirados e limpa campos públicos voláteis. A entidade da empresa e todos os dados criados pelo usuário permanecem.

O TTL é originado no Gateway e configurado conforme o provedor. Mudanças de política exigem atualização de configuração ou novo ADR quando afetarem o modelo.
