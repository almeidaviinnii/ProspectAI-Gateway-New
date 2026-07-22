# Motor de Regras

O `RuleEngineProvider` substitui a análise externa na rota existente `POST /v1/analyze` sem alterar seu contrato. Ele interpreta fatos da auditoria e fatores do score por regras determinísticas, sem rede, chave ou custo de IA.

## Regras principais

- website ausente ou inacessível: landing page, hospedagem e disponibilidade;
- ausência de HTTPS: certificado SSL;
- carregamento acima de 3.000 ms: performance e conversão;
- ausência de viewport mobile: experiência responsiva;
- ausência de título: fundamento de SEO local;
- redes não identificadas: presença social;
- WhatsApp não confirmado: canal rápido de atendimento;
- perfil Google incompleto, poucas avaliações ou nota baixa: Google Meu Negócio e reputação.

O Score Geral de Qualidade Digital é calculado como `100 - score de oportunidade` já recebido do aplicativo. Assim, o significado original do score do Android permanece intacto:

- 90–100: excelente presença digital;
- 70–89: boa presença digital;
- 50–69: oportunidades importantes;
- 0–49: alta prioridade comercial.

As respostas mantêm o modelo `AiAnalysisResponse`, garantindo compatibilidade integral com o Android.

## IA opcional

`AIProvider`, `GeminiProvider`, `OpenAIProvider` e a fábrica continuam no projeto. Sem nenhuma chave de IA, o módulo fica automaticamente desabilitado e o Motor de Regras é usado.

```env
AI_API_KEY=
GEMINI_API_KEY=
```

Nesse modo nenhuma chamada a Gemini ou OpenAI pode ocorrer. Para reativar a OpenAI futuramente, basta adicionar:

```env
AI_API_KEY=sua-chave
```

O controle `AI_MODULE_ENABLED=false` continua disponível como bloqueio explícito caso seja necessário manter uma chave armazenada sem utilizá-la. Para Gemini, selecione `AI_PROVIDER=gemini` e configure `GEMINI_API_KEY`.
