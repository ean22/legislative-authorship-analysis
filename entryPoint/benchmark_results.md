# Relatório Experimental de Benchmark: Modelagem de Tópicos e Autoria Legislativa

- **Data da Execução:** 2026-10-26 13:43:30
- **Corpus Analisado:** 300 normas legislativas
- **Total de Configurações Comparadas:** 10 experimentos
- **Baseline:** Baseline: TF-IDF + NMF (K=12) (Score: 0.579)
- **Melhor Combinação Selecionada:** Cluster c-TF-IDF K=8 (Score: 0.713)
- **Ganho sobre Baseline:** +23.2%

## Tabela Comparativa de Desempenho e Métricas Reais

| Rank | Configuração | Redução Textual | Algoritmo | K | Score Global | Coerência (UMass) | Diversidade | Estabilidade | Utilidade Autores | Por que escolher? (Diagnóstico Empírico) |
|:---:|:---|:---|:---|:---:|:---:|:---:|:---:|:---:|:---:|:---|
| 1 | Cluster c-TF-IDF K=8 🏆 *(Vencedor)* | TEXTRANK | CLUSTER_C_TFIDF | 8 | **0.713** | -1.24 | 0.98 | 0.37 | 0.48 | Clustering c-TF-IDF obteve alta pureza de assuntos e vocabulário discriminante. Alta diversidade temática (98% termos únicos). Alinhamento institucional moderado (pureza: 30%). |
| 2 | LDA K=8 | STOPWORDS_TFIDF | LDA | 8 | **0.643** | -2.09 | 0.94 | 0.13 | 0.48 | LDA capturou mistura suave de temas. Alta diversidade temática (94% termos únicos). Alinhamento institucional moderado (pureza: 30%). |
| 3 | Qui-Quadrado + NMF K=8 | CHI_SQUARE | NMF | 8 | **0.606** | -1.18 | 0.46 | 0.71 | 0.34 | NMF compacto (K=8) equilibrou estabilidade e coerência, superando K=12. Alerta: baixa diversidade (46%), risco de redundância. Alinhamento institucional moderado (pureza: 30%). |
| 4 | MMR Diversificado + NMF K=8 | MMR | NMF | 8 | **0.600** | -1.36 | 0.56 | 0.54 | 0.34 | NMF compacto (K=8) equilibrou estabilidade e coerência, superando K=12. Alinhamento institucional moderado (pureza: 30%). |
| 5 | TextRank + NMF K=8 | TEXTRANK | NMF | 8 | **0.600** | -2.24 | 0.52 | 0.68 | 0.34 | NMF compacto (K=8) equilibrou estabilidade e coerência, superando K=12. Alinhamento institucional moderado (pureza: 30%). |
| 6 | Clustering Semântico + NMF K=8 | CLUSTERING_SEMANTICO | NMF | 8 | **0.590** | -1.42 | 0.48 | 0.59 | 0.34 | NMF compacto (K=8) equilibrou estabilidade e coerência, superando K=12. Alerta: baixa diversidade (48%), risco de redundância. Alinhamento institucional moderado (pureza: 30%). |
| 7 | Embeddings SVD + NMF K=8 | EMBEDDINGS_SVD | NMF | 8 | **0.584** | -1.67 | 0.79 | 0.20 | 0.34 | NMF compacto (K=8) equilibrou estabilidade e coerência, superando K=12. Alinhamento institucional moderado (pureza: 30%). |
| 8 | Baseline: TF-IDF + NMF (K=12) *(Baseline)* | STOPWORDS_TFIDF | NMF | 12 | **0.579** | -1.61 | 0.67 | 0.32 | 0.34 | Baseline de controle. Alinhamento institucional moderado (pureza: 30%). |
| 9 | NMF K=8 | STOPWORDS_TFIDF | NMF | 8 | **0.561** | -1.72 | 0.63 | 0.26 | 0.36 | NMF compacto (K=8) equilibrou estabilidade e coerência, superando K=12. Alinhamento institucional moderado (pureza: 30%). |
| 10 | Chunking Sentencial + NMF K=8 | CHUNKING_SENTENCAS | NMF | 8 | **0.537** | -1.34 | 0.38 | 0.45 | 0.35 | NMF compacto (K=8) equilibrou estabilidade e coerência, superando K=12. Alerta: baixa diversidade (38%), risco de redundância. Alinhamento institucional moderado (pureza: 30%). |

## Tópicos Descobertos pela Melhor Combinação

- **Tópico 01 [Imóveis]:** desapropriação, necessário, imóvel, particular, subprefeitura, implantação
- **Tópico 02 [Normas Gerais]:** maria, oliveira, cadlog, dona, graça, reis
- **Tópico 03 [Educação]:** educação, criação, infantil, escola, centro, ensino
- **Tópico 04 [Finanças]:** suplementar, acordo, adicional, crédito, interesse, federal
- **Tópico 05 [Utilidade Pública]:** pública, declara, entidade, utilidade, atribuições, organização
- **Tópico 06 [Denominação de Vias]:** público, denomina, logradouro, espaço, distrito, rua
- **Tópico 07 [Serviços Públicos]:** secretaria, comissão, serviços, execução, cargos, gestão
- **Tópico 08 [Normas Gerais]:** desempenho, educacional, unificado, integrante, centro, localizado

## Perfis Temáticos dos Principais Órgãos Emissores (Melhor Combinação)

- **PREFEITO** (1 normas):
  - Tópico Dominante: *Serviços Públicos* (19.0%)
  - Tópico Secundário: *Imóveis* (11.6%)

## Perfis Temáticos dos Principais Autores Políticos (Melhor Combinação)

- **FERNANDO HADDAD** (283 normas):
  - Dominante: *Finanças* (32.2%)
  - Secundário: *Imóveis* (12.5%)
- **ANTONIO DONATO MADORMO** (279 normas):
  - Dominante: *Finanças* (32.5%)
  - Secundário: *Imóveis* (12.5%)
- **LUIS FERNANDO MASSONETTO** (138 normas):
  - Dominante: *Finanças* (55.3%)
  - Secundário: *Imóveis* (14.8%)
- **LEDA MARIA PAULANI** (126 normas):
  - Dominante: *Finanças* (61.4%)
  - Secundário: *Serviços Públicos* (7.5%)
- **MARCOS DE BARROS CRUZ** (105 normas):
  - Dominante: *Finanças* (71.6%)
  - Secundário: *Serviços Públicos* (4.6%)
- **ANTONIO CESAR RUSSI CALLEGARI** (42 normas):
  - Dominante: *Educação* (29.0%)
  - Secundário: *Finanças* (21.9%)
- **JOSÉ FLORIANO DE AZEVEDO MARQUES NETO** (36 normas):
  - Dominante: *Finanças* (27.4%)
  - Secundário: *Imóveis* (24.3%)
- **JOSÉ DE FILIPPI JUNIOR** (14 normas):
  - Dominante: *Finanças* (53.4%)
  - Secundário: *Imóveis* (14.0%)
- **JOÃO LUIZ SILVA FERREIRA** (13 normas):
  - Dominante: *Finanças* (79.9%)
  - Secundário: *Imóveis* (2.9%)
- **OSVALDO SPURI** (11 normas):
  - Dominante: *Finanças* (47.2%)
  - Secundário: *Serviços Públicos* (15.2%)
- **FRANCISCO MACENA DA SILVA** (11 normas):
  - Dominante: *Finanças* (54.9%)
  - Secundário: *Imóveis* (8.7%)
- **SIMÃO PEDRO CHIOVETTI** (9 normas):
  - Dominante: *Finanças* (49.5%)
  - Secundário: *Serviços Públicos* (9.9%)
- **ELISEU GABRIEL DE PIERI** (8 normas):
  - Dominante: *Finanças* (62.4%)
  - Secundário: *Serviços Públicos* (9.8%)
- **JILMAR AUGUSTINHO TATTO** (8 normas):
  - Dominante: *Finanças* (43.7%)
  - Secundário: *Serviços Públicos* (15.8%)
- **FERNANDO DE MELLO FRANCO** (7 normas):
  - Dominante: *Finanças* (30.8%)
  - Secundário: *Serviços Públicos* (15.6%)
- **RICARDO TEIXEIRA** (5 normas):
  - Dominante: *Finanças* (24.2%)
  - Secundário: *Imóveis* (15.2%)
