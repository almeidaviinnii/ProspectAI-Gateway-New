# Segurança

## Fronteiras de confiança

- O APK é cliente não confiável para segredos de provedores.
- O Gateway é a única fronteira autorizada a ler chaves reais.
- CRM e preferências do usuário permanecem no armazenamento privado do Android.

## Controles implementados

| Risco | Controle |
|---|---|
| Extração de chave do APK | nenhuma chave real no Android; variáveis/secret manager no Gateway |
| Roubo do token limitado | AES-GCM com chave não exportável no Android Keystore |
| Token em logs | logging HTTP apenas em debug e nível BASIC; Gateway filtra URIs sensíveis |
| Consumo excessivo | limites diário e mensal por provedor, contador atômico e log persistente |
| Uso incompatível com política | ativação explícita por adaptador, TTL, expiração de dados e atribuição visível |
| SSRF pela auditoria | somente HTTP(S), resolução DNS, bloqueio de redes locais/privadas e revalidação em redirects |
| Resposta maliciosa/grande | timeouts, corpo limitado a 1 MB e parsing defensivo |
| Injeção SQL | DAOs Room parametrizados |
| Vazamento por backup automático | `allowBackup=false`; exportação explícita exclui credenciais |
| Atualização adulterada | HTTPS, hash publicável e verificação obrigatória da assinatura pelo instalador Android |
| Toolchain adulterada | Gradle Wrapper com URL validada e SHA-256 oficial fixado |
| Fusão incorreta | somente correspondência forte; website/telefone isolados nunca fundem |

## Produção

Use TLS, token aleatório longo por instalação/organização, rotação, cofre de segredos, volume persistente para uso, firewall de saída e observabilidade sem payload pessoal. Assine todas as versões com a mesma chave, mantida fora do repositório.
