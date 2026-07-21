# ADR-0015: Retenção aplicada em gates de inicialização e backup

- Status: Aceito
- Data: 2026-07-15

## Contexto

O Worker diário definido no ADR-0010 pode ser adiado pelo sistema operacional. Além disso, um backup criado entre o vencimento da evidência e a próxima execução periódica poderia prolongar indevidamente dados externos.

## Decisão

A mesma política central de retenção será executada em três pontos idempotentes: inicialização do aplicativo, imediatamente antes da exportação de backup e Worker diário. A restauração reinicia o processo; assim, o gate de inicialização limpa evidências vencidas do arquivo restaurado antes do uso normal.

Análises derivadas, inclusive o fallback local transparente, herdam a validade das evidências que utilizaram. O fato de o processamento ocorrer localmente não transforma conteúdo externo derivado em dado permanente do usuário.

## Consequências

Backups não estendem a retenção autorizada e o aplicativo não depende exclusivamente do agendamento do Android para conformidade. CRM, favoritos, observações, tarefas, configurações e eventos criados pelo usuário continuam preservados.
