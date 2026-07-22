# Implantação do Gateway Seguro

## Destino recomendado

O projeto inclui um Blueprint `render.yaml` para um Web Service Docker no Render, com HTTPS público, health check e disco persistente para o registro de consumo. O plano `starter` é obrigatório porque o Gateway precisa preservar `usage.csv` entre reinícios.

## Pré-requisitos

1. Repositório Git privado conectado ao Render.
2. Chave do Google Maps Platform autorizada somente para Places API (New) e Geocoding API.
3. Confirmação contratual antes de definir `PLACES_DATA_STORAGE_ALLOWED=true`.
4. A versão sem IA não exige `GEMINI_API_KEY` nem `AI_API_KEY`. O Motor de Regras é o analisador padrão.
5. Token inicial aleatório de pelo menos 256 bits para `PROSPECTAI_GATEWAY_TOKEN`.

Nunca coloque valores reais em `.env.example`, no repositório ou no APK.

## Implantação pelo Blueprint

1. Envie o projeto para um repositório Git privado.
2. No Render, crie um Blueprint apontando para o `render.yaml` da raiz.
3. Quando solicitado, informe somente `PROSPECTAI_GATEWAY_TOKEN` e `GOOGLE_PLACES_API_KEY`. Não configure chave de IA nesta versão.
4. Confirme o serviço pago `starter` e o disco persistente montado em `/app/data`.
5. Aguarde o deploy e copie a URL HTTPS `https://<servico>.onrender.com/`.
6. Confirme que `GET /v1/health` retorna `status: ok`.
7. Se houver autorização de retenção compatível com o ProspectAI, altere `PLACES_DATA_STORAGE_ALLOWED` para `true`, salve e faça deploy manual.
8. Não configure nenhuma chave de IA nesta versão. Sem `AI_API_KEY`, `OPENAI_API_KEY` ou `GEMINI_API_KEY`, o Motor de Regras é selecionado automaticamente e nenhuma chamada de IA ocorre.

## Validação após o deploy

```bash
curl -fsS https://<servico>.onrender.com/v1/health

curl -fsS \
  -H "Authorization: Bearer <token>" \
  https://<servico>.onrender.com/v1/usage
```

No Android, abra `Ajustes > Integrações`, informe a URL HTTPS com barra final, informe o token e toque em `Testar`. O aplicativo já suporta essa configuração; não é necessário embutir segredos ou URL no APK.

## Troca futura das chaves das APIs

### Google Places

1. Crie uma nova chave no Google Cloud sem revogar a atual.
2. Restrinja a nova chave às APIs Places API (New) e Geocoding API e às origens de saída autorizadas da hospedagem.
3. Troque somente o valor secreto `GOOGLE_PLACES_API_KEY` no Render.
4. Execute deploy manual.
5. Valide `/v1/health` e faça uma pesquisa pequena pelo aplicativo.
6. Consulte `/v1/usage` e os logs para confirmar chamadas bem-sucedidas.
7. Somente então revogue a chave anterior no Google Cloud.

### Google Gemini

Somente necessário quando o módulo opcional de IA for reativado.

1. Crie uma nova chave no Google AI Studio sem revogar a atual.
2. Troque somente `GEMINI_API_KEY` no cofre de segredos do Render.
3. Execute deploy manual e valide uma análise.
4. Revogue a chave antiga após confirmar o funcionamento.

### OpenAI

Somente necessário quando o módulo opcional de IA for reativado.

1. Crie uma nova chave sem revogar a atual.
2. Troque `AI_API_KEY` no cofre de segredos do Render.
3. Execute deploy manual e valide uma análise.
4. Revogue a chave antiga após confirmar o funcionamento.

### Trocar o provider sem alterar código

1. Para Gemini, defina `AI_PROVIDER=gemini` e configure `GEMINI_API_KEY`.
2. Para OpenAI, basta configurar `AI_API_KEY`; `AI_PROVIDER=openai` é opcional porque será selecionado automaticamente.
3. Salve as variáveis e faça um deploy manual.
4. Confira `/v1/health` e execute uma análise pelo aplicativo.

### Token de acesso do Gateway

1. Gere um novo token aleatório de pelo menos 256 bits.
2. Troque `PROSPECTAI_GATEWAY_TOKEN` no Render e execute deploy manual.
3. Atualize o token em `Ajustes > Integrações` no Android.
4. Teste a integração. O token antigo deixa de funcionar após o deploy.

## Rollback

Se uma nova chave falhar, restaure o valor anterior no cofre de segredos e execute deploy manual. Não altere o APK e nunca registre chaves ou tokens em logs, tickets ou commits.
