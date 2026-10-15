# Relatório Experimental de Benchmark: Modelagem de Tópicos e Autoria Legislativa

- **Data da Execução:** 2026-10-15 14:47:20
- **Corpus Analisado:** 300 normas legislativas
- **Total de Configurações Comparadas:** 10 experimentos
- **Baseline:** Baseline: TF-IDF + NMF (K=12) (Score: 0.543)
- **Melhor Combinação Selecionada:** Cluster c-TF-IDF K=8 (Score: 0.727)
- **Ganho sobre Baseline:** +34.0%

## Tabela Comparativa de Desempenho e Métricas Reais

| Rank | Configuração | Redução Textual | Algoritmo | K | Score Global | Coerência (UMass) | Diversidade | Estabilidade | Utilidade Autores | Por que escolher? (Diagnóstico Empírico) |
|:---:|:---|:---|:---|:---:|:---:|:---:|:---:|:---:|:---:|:---|
| 1 | Cluster c-TF-IDF K=8 🏆 *(Vencedor)* | TEXTRANK | CLUSTER_C_TFIDF | 8 | **0.727** | -1.67 | 0.97 | 0.48 | 0.47 | Clustering c-TF-IDF obteve alta pureza de assuntos e vocabulário discriminante. Alta diversidade temática (97% termos únicos). Alinhamento institucional moderado (pureza: 30%). |
| 2 | Qui-Quadrado + NMF K=8 | CHI_SQUARE | NMF | 8 | **0.673** | -1.35 | 0.52 | 0.77 | 0.46 | NMF compacto (K=8) equilibrou estabilidade e coerência, superando K=12. Alinhamento institucional moderado (pureza: 30%). |
| 3 | Chunking Sentencial + NMF K=8 | CHUNKING_SENTENCAS | NMF | 8 | **0.636** | -1.38 | 0.58 | 0.70 | 0.34 | NMF compacto (K=8) equilibrou estabilidade e coerência, superando K=12. Alinhamento institucional moderado (pureza: 30%). |
| 4 | LDA K=8 | STOPWORDS_TFIDF | LDA | 8 | **0.631** | -2.47 | 0.85 | 0.23 | 0.48 | LDA probabilístico apresentou menor coerência para textos concisos de ementas legislativas. Alta diversidade temática (85% termos únicos). Alinhamento institucional moderado (pureza: 30%). |
| 5 | TextRank + NMF K=8 | TEXTRANK | NMF | 8 | **0.629** | -2.09 | 0.50 | 0.84 | 0.34 | NMF compacto (K=8) equilibrou estabilidade e coerência, superando K=12. Alerta: baixa diversidade (50%), risco de redundância. Alinhamento institucional moderado (pureza: 30%). |
| 6 | NMF K=8 | STOPWORDS_TFIDF | NMF | 8 | **0.619** | -1.90 | 0.75 | 0.45 | 0.34 | NMF compacto (K=8) equilibrou estabilidade e coerência, superando K=12. Alinhamento institucional moderado (pureza: 30%). |
| 7 | MMR Diversificado + NMF K=8 | MMR | NMF | 8 | **0.611** | -1.52 | 0.52 | 0.61 | 0.37 | NMF compacto (K=8) equilibrou estabilidade e coerência, superando K=12. Alinhamento institucional moderado (pureza: 30%). |
| 8 | Embeddings SVD + NMF K=8 | EMBEDDINGS_SVD | NMF | 8 | **0.599** | -1.78 | 0.65 | 0.47 | 0.34 | NMF compacto (K=8) equilibrou estabilidade e coerência, superando K=12. Alinhamento institucional moderado (pureza: 30%). |
| 9 | Clustering Semântico + NMF K=8 | CLUSTERING_SEMANTICO | NMF | 8 | **0.562** | -1.57 | 0.46 | 0.49 | 0.34 | NMF compacto (K=8) equilibrou estabilidade e coerência, superando K=12. Alerta: baixa diversidade (46%), risco de redundância. Alinhamento institucional moderado (pureza: 30%). |
| 10 | Baseline: TF-IDF + NMF (K=12) *(Baseline)* | STOPWORDS_TFIDF | NMF | 12 | **0.543** | -1.73 | 0.58 | 0.26 | 0.34 | Baseline de controle. Subótimo: K=12 gerou fragmentação e repetição vocabular em finanças. Alinhamento institucional moderado (pureza: 30%). |

## Tópicos Descobertos pela Melhor Combinação

- **Tópico 01 [Denominação de Vias e Logradouros]:** logradouro, serviços, público, denomina, desempenho, urbana
- **Tópico 02 [Bens Imóveis e Desapropriação]:** necessários, imóveis, particulares, comitê, desapropriação, implantação
- **Tópico 03 [Declaração de Utilidade Pública]:** espaço, declara, entidade, utilidade, conselho, pública
- **Tópico 04 [Bens Imóveis e Desapropriação]:** imóvel, necessário, particular, desapropriação, educação, criação
- **Tópico 05 [Crédito Orçamentário e Finanças]:** suplementar, adicional, acordo, habitação, metropolitana, oneroso
- **Tópico 06 [Denominação de Vias e Logradouros]:** secretaria, comissão, construção, cargos, denominação, provimento

## Perfis Temáticos dos Principais Órgãos Emissores (Melhor Combinação)

- **PREFEITO** (1 normas):
  - Tópico Dominante: *Bens Imóveis e Desapropriação* (31.5%)
  - Tópico Secundário: *Denominação de Vias e Logradouros* (21.7%)

## Perfis Temáticos dos Principais Autores Políticos (Melhor Combinação)

- **FERNANDO HADDAD** (283 normas):
  - Dominante: *Crédito Orçamentário e Finanças* (35.1%)
  - Secundário: *Bens Imóveis e Desapropriação* (17.2%)
- **ANTONIO DONATO MADORMO** (279 normas):
  - Dominante: *Crédito Orçamentário e Finanças* (35.4%)
  - Secundário: *Bens Imóveis e Desapropriação* (17.4%)
- **LUIS FERNANDO MASSONETTO** (138 normas):
  - Dominante: *Crédito Orçamentário e Finanças* (59.8%)
  - Secundário: *Bens Imóveis e Desapropriação* (15.8%)
- **LEDA MARIA PAULANI** (126 normas):
  - Dominante: *Crédito Orçamentário e Finanças* (65.0%)
  - Secundário: *Bens Imóveis e Desapropriação* (9.0%)
- **MARCOS DE BARROS CRUZ** (105 normas):
  - Dominante: *Crédito Orçamentário e Finanças* (76.4%)
  - Secundário: *Bens Imóveis e Desapropriação* (8.8%)
- **ANTONIO CESAR RUSSI CALLEGARI** (42 normas):
  - Dominante: *Bens Imóveis e Desapropriação* (32.9%)
  - Secundário: *Crédito Orçamentário e Finanças* (28.4%)
- **JOSÉ FLORIANO DE AZEVEDO MARQUES NETO** (36 normas):
  - Dominante: *Bens Imóveis e Desapropriação* (32.7%)
  - Secundário: *Crédito Orçamentário e Finanças* (30.1%)
- **JOSÉ DE FILIPPI JUNIOR** (14 normas):
  - Dominante: *Crédito Orçamentário e Finanças* (56.6%)
  - Secundário: *Bens Imóveis e Desapropriação* (21.6%)
- **JOÃO LUIZ SILVA FERREIRA** (13 normas):
  - Dominante: *Crédito Orçamentário e Finanças* (84.2%)
  - Secundário: *Bens Imóveis e Desapropriação* (8.3%)
- **OSVALDO SPURI** (11 normas):
  - Dominante: *Crédito Orçamentário e Finanças* (50.9%)
  - Secundário: *Denominação de Vias e Logradouros* (19.2%)
- **FRANCISCO MACENA DA SILVA** (11 normas):
  - Dominante: *Crédito Orçamentário e Finanças* (57.4%)
  - Secundário: *Bens Imóveis e Desapropriação* (13.6%)
- **SIMÃO PEDRO CHIOVETTI** (9 normas):
  - Dominante: *Crédito Orçamentário e Finanças* (51.1%)
  - Secundário: *Bens Imóveis e Desapropriação* (15.5%)
- **ELISEU GABRIEL DE PIERI** (8 normas):
  - Dominante: *Crédito Orçamentário e Finanças* (63.8%)
  - Secundário: *Denominação de Vias e Logradouros* (20.5%)
- **JILMAR AUGUSTINHO TATTO** (8 normas):
  - Dominante: *Crédito Orçamentário e Finanças* (44.8%)
  - Secundário: *Denominação de Vias e Logradouros* (17.8%)
- **FERNANDO DE MELLO FRANCO** (7 normas):
  - Dominante: *Crédito Orçamentário e Finanças* (34.2%)
  - Secundário: *Denominação de Vias e Logradouros* (23.1%)
- **RICARDO TEIXEIRA** (5 normas):
  - Dominante: *Bens Imóveis e Desapropriação* (32.5%)
  - Secundário: *Denominação de Vias e Logradouros* (29.4%)
