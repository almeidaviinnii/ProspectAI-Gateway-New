# ADR-0013: Catálogo de empresas paginado, filtrável e ordenável no banco

- Status: Aceito
- Data: 2026-07-15

## Contexto

A especificação exige dezenas de milhares de empresas, busca instantânea, filtros por faixa de pontuação e múltiplas ordenações. Materializar e ordenar toda a base na interface degradaria memória, tempo de abertura e estabilidade.

## Decisão

O catálogo principal usa Paging 3 sobre um `PagingSource` Room. Busca textual, faixa de score e ordenação são parâmetros da consulta SQL. A UI apenas mantém a seleção e renderiza páginas; não contém regras de ordenação global.

São suportadas as ordenações por maior/menor nota, atualização recente, quantidade de avaliações, nome, cidade e data de captura, além das faixas 90–100, 80–89, 70–79, 60–69 e abaixo de 60.

## Consequências

O consumo permanece previsível com bases extensas e as mudanças no banco invalidam páginas automaticamente. Consultas e índices deverão ser medidos novamente antes de aumentar os limites de volume da V1.
