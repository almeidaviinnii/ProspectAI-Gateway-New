# Limitações técnicas documentadas

- Resultados dependem da cobertura, quotas e campos autorizados do provedor. O produto nunca inventa dados ausentes.
- A integração Google Places permanece desativada por padrão até que `PLACES_DATA_STORAGE_ALLOWED=true` seja configurado após validação contratual. Sem essa autorização, deve-se usar outro adaptador com licença compatível.
- Uma distribuição que ative Google Places precisa publicar Termos de Uso e Política de Privacidade compatíveis e revisar as atribuições exigidas. URLs públicas não podem ser inventadas pelo código e são um requisito do processo de release.
- Raio usa Geocoding para resolver o centro. Se a resolução falhar, o Gateway registra aviso e mantém a busca textual.
- A auditoria de website não executa JavaScript, lê no máximo 1 MB e trata seus achados como confiança média.
- Redes sociais são registradas apenas quando vinculadas publicamente por fonte permitida; scraping autenticado ou contorno de bloqueios não é realizado.
- A verificação de WhatsApp confirma o formato/link público, não a titularidade do número.
- Fotos do provedor são mantidas apenas como referências oficiais com validade; o APK não armazena os binários nem expõe a chave para baixá-los. Um adaptador de mídia do Gateway pode ser adicionado sem alterar o modelo local.
- Perfis sociais usam um ícone vetorial genérico de link. Assets oficiais de marcas devem ser adicionados no processo de distribuição após revisão das diretrizes de marca, sem alterar o contrato `SocialProfile`.
- `UsageRegistry` em arquivo é deliberadamente single-instance. Escala horizontal requer implementação compartilhada/atômica mantendo o contrato.
- O backup explícito não é criptografado pelo aplicativo; deve ser guardado em destino protegido pelo usuário/MDM. Tokens nunca são incluídos.
- Distribuição do APK e custódia da chave de produção são processos externos de release; o app verifica o manifesto e o Android rejeita assinatura incompatível.
