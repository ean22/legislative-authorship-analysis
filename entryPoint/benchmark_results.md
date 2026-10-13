# Relatório Experimental de Benchmark: Modelagem de Tópicos e Autoria Legislativa

- **Data da Execução:** 2026-10-13 10:24:51
- **Corpus Analisado:** 300 normas legislativas
- **Total de Configurações Comparadas:** 10 experimentos
- **Baseline:** Baseline: TF-IDF + NMF (K=12) (Score: 0.591)
- **Melhor Combinação Selecionada:** Cluster c-TF-IDF K=8 (Score: 0.701)
- **Ganho sobre Baseline:** +18.6%

## Tabela Comparativa de Desempenho e Métricas Reais

| Rank | Configuração | Redução Textual | Algoritmo | K | Score Global | Coerência (UMass) | Diversidade | Estabilidade | Utilidade Autores | Por que escolher? (Diagnóstico Empírico) |
|:---:|:---|:---|:---|:---:|:---:|:---:|:---:|:---:|:---:|:---|
| 1 | Cluster c-TF-IDF K=8 🏆 *(Vencedor)* | TEXTRANK | CLUSTER_C_TFIDF | 8 | **0.701** | -1.36 | 0.94 | 0.40 | 0.46 | Clustering c-TF-IDF obteve alta pureza de assuntos e vocabulário discriminante. Alta diversidade temática (94% termos únicos). Alinhamento institucional moderado (pureza: 42%). |
| 2 | MMR Diversificado + NMF K=8 | MMR | NMF | 8 | **0.674** | -1.38 | 0.56 | 0.65 | 0.51 | NMF compacto (K=8) equilibrou estabilidade e coerência, superando K=12. Alinhamento institucional moderado (pureza: 40%). |
| 3 | NMF K=8 | STOPWORDS_TFIDF | NMF | 8 | **0.667** | -1.90 | 0.75 | 0.45 | 0.50 | NMF compacto (K=8) equilibrou estabilidade e coerência, superando K=12. Alinhamento institucional moderado (pureza: 42%). |
| 4 | Chunking Sentencial + NMF K=8 | CHUNKING_SENTENCAS | NMF | 8 | **0.650** | -1.31 | 0.54 | 0.58 | 0.49 | NMF compacto (K=8) equilibrou estabilidade e coerência, superando K=12. Alinhamento institucional moderado (pureza: 35%). |
| 5 | Embeddings SVD + NMF K=8 | EMBEDDINGS_SVD | NMF | 8 | **0.639** | -1.78 | 0.65 | 0.47 | 0.47 | NMF compacto (K=8) equilibrou estabilidade e coerência, superando K=12. Alinhamento institucional moderado (pureza: 39%). |
| 6 | Clustering Semântico + NMF K=8 | CLUSTERING_SEMANTICO | NMF | 8 | **0.635** | -1.40 | 0.42 | 0.67 | 0.49 | NMF compacto (K=8) equilibrou estabilidade e coerência, superando K=12. Alerta: baixa diversidade (42%), risco de redundância. Alinhamento institucional moderado (pureza: 43%). |
| 7 | Qui-Quadrado + NMF K=8 | CHI_SQUARE | NMF | 8 | **0.604** | -1.25 | 0.48 | 0.46 | 0.47 | NMF compacto (K=8) equilibrou estabilidade e coerência, superando K=12. Alerta: baixa diversidade (48%), risco de redundância. Alinhamento institucional moderado (pureza: 35%). |
| 8 | TextRank + NMF K=8 | TEXTRANK | NMF | 8 | **0.599** | -2.27 | 0.38 | 0.69 | 0.45 | NMF compacto (K=8) equilibrou estabilidade e coerência, superando K=12. Alerta: baixa diversidade (38%), risco de redundância. Alinhamento institucional moderado (pureza: 38%). |
| 9 | Baseline: TF-IDF + NMF (K=12) *(Baseline)* | STOPWORDS_TFIDF | NMF | 12 | **0.591** | -1.73 | 0.58 | 0.26 | 0.50 | Baseline de controle. Subótimo: K=12 gerou fragmentação e repetição vocabular em finanças. Alinhamento institucional moderado (pureza: 33%). |
| 10 | LDA K=8 | STOPWORDS_TFIDF | LDA | 8 | **0.551** | -2.47 | 0.85 | 0.23 | 0.22 | LDA probabilístico apresentou menor coerência para textos concisos de ementas legislativas. Alta diversidade temática (85% termos únicos). Alinhamento institucional moderado (pureza: 13%). |

## Tópicos Descobertos pela Melhor Combinação

- **Tópico 01 [Educação e Creches]:** conselho, construção, ensino, escola, serviços, parque
- **Tópico 02 [Denominação de Vias e Logradouros]:** secretaria, uso, veículos, cargos, desenvolvimento, denominação
- **Tópico 03 [Administração e Normas Gerais (comitê)]:** comitê, programa, governamental, funcionamento, gestão, municipais
- **Tópico 04 [Declaração de Utilidade Pública]:** pública, declara, entidade, utilidade, codlog, denominação
- **Tópico 05 [Educação e Creches]:** suplementar, adicional, acordo, infantil, criação, educação
- **Tópico 06 [Denominação de Vias e Logradouros]:** público, logradouro, espaço, distrito, livre, localizado
- **Tópico 07 [Denominação de Vias e Logradouros]:** particular, imóvel, necessário, desapropriação, subprefeitura, situado
- **Tópico 08 [Bens Imóveis e Patrimônio Público]:** necessários, imóveis, particulares, cargos, implantação, desapropriação

## Perfis Temáticos dos Principais Órgãos Emissores (Melhor Combinação)

- **PREFEITO** (284 normas):
  - Tópico Dominante: *Educação e Creches* (33.2%)
  - Tópico Secundário: *Denominação de Vias e Logradouros* (12.9%)
- **SECRETARIO DO GOVERNO MUNICIPAL** (281 normas):
  - Tópico Dominante: *Educação e Creches* (33.5%)
  - Tópico Secundário: *Denominação de Vias e Logradouros* (13.0%)
- **SECRETARIO MUNICIPAL DOS NEGOCIOS JURIDICOS** (140 normas):
  - Tópico Dominante: *Educação e Creches* (55.4%)
  - Tópico Secundário: *Denominação de Vias e Logradouros* (13.2%)
- **SECRETARIA MUNICIPAL DE PLANEJAMENTO, ORCAMENTO E GESTAO** (126 normas):
  - Tópico Dominante: *Educação e Creches* (62.1%)
  - Tópico Secundário: *Denominação de Vias e Logradouros* (7.1%)
- **SECRETARIO MUNICIPAL DE FINANCAS E DESENVOLVIMENTO ECONOMICO** (105 normas):
  - Tópico Dominante: *Educação e Creches* (73.1%)
  - Tópico Secundário: *Denominação de Vias e Logradouros* (6.5%)
- **SECRETARIO MUNICIPAL DE EDUCACAO** (41 normas):
  - Tópico Dominante: *Educação e Creches* (32.7%)
  - Tópico Secundário: *Denominação de Vias e Logradouros* (15.7%)
- **SECRETARIO MUNICIPAL DE HABITACAO** (36 normas):
  - Tópico Dominante: *Educação e Creches* (27.2%)
  - Tópico Secundário: *Denominação de Vias e Logradouros* (26.7%)
- **SECRETARIO MUNICIPAL DA SAUDE** (14 normas):
  - Tópico Dominante: *Educação e Creches* (54.2%)
  - Tópico Secundário: *Denominação de Vias e Logradouros* (17.2%)
- **SECRETARIO MUNICIPAL DE CULTURA** (13 normas):
  - Tópico Dominante: *Educação e Creches* (81.3%)
  - Tópico Secundário: *Denominação de Vias e Logradouros* (3.4%)
- **SECRETARIO MUNICIPAL DE INFRAESTRUTURA URBANA E OBRAS** (11 normas):
  - Tópico Dominante: *Educação e Creches* (47.2%)
  - Tópico Secundário: *Denominação de Vias e Logradouros* (12.6%)
- **SECRETARIO MUNICIPAL DE COORDENACAO DAS SUBPREFEITURAS** (11 normas):
  - Tópico Dominante: *Educação e Creches* (54.6%)
  - Tópico Secundário: *Denominação de Vias e Logradouros* (11.4%)
- **SECRETARIO MUNICIPAL DE SERVICOS** (9 normas):
  - Tópico Dominante: *Educação e Creches* (48.1%)
  - Tópico Secundário: *Administração e Normas Gerais (comitê)* (11.2%)
- **SECRETARIO MUNICIPAL DE TRANSPORTES** (8 normas):
  - Tópico Dominante: *Educação e Creches* (43.0%)
  - Tópico Secundário: *Denominação de Vias e Logradouros* (20.3%)
- **SECRETARIO MUNICIPAL DE DESENVOLVIMENTO URBANO** (7 normas):
  - Tópico Dominante: *Educação e Creches* (29.1%)
  - Tópico Secundário: *Denominação de Vias e Logradouros* (17.8%)
- **SECRETARIO MUNICIPAL DO TRABALHO E DO EMPREENDEDORISMO** (6 normas):
  - Tópico Dominante: *Educação e Creches* (56.1%)
  - Tópico Secundário: *Denominação de Vias e Logradouros* (24.2%)
- **SECRETARIO MUNICIPAL DO VERDE E DO MEIO AMBIENTE** (5 normas):
  - Tópico Dominante: *Educação e Creches* (20.7%)
  - Tópico Secundário: *Denominação de Vias e Logradouros* (19.9%)
- **SECRETARIO MUNICIPAL DE SEGURANCA URBANA** (4 normas):
  - Tópico Dominante: *Educação e Creches* (64.1%)
  - Tópico Secundário: *Denominação de Vias e Logradouros* (5.7%)
- **SECRETARIA MUNICIPAL DE ASSISTENCIA E DESENVOLVIMENTO SOCIAL** (3 normas):
  - Tópico Dominante: *Educação e Creches* (54.5%)
  - Tópico Secundário: *Denominação de Vias e Logradouros* (24.9%)

## Perfis Temáticos dos Principais Autores Políticos (Melhor Combinação)

- **FERNANDO HADDAD** (284 normas):
  - Dominante: *Educação e Creches* (33.2%)
  - Secundário: *Denominação de Vias e Logradouros* (12.9%)
- **ANTONIO DONATO MADORMO** (280 normas):
  - Dominante: *Educação e Creches* (33.6%)
  - Secundário: *Denominação de Vias e Logradouros* (13.0%)
- **LUIS FERNANDO MASSONETTO** (138 normas):
  - Dominante: *Educação e Creches* (56.1%)
  - Secundário: *Denominação de Vias e Logradouros* (13.4%)
- **LEDA MARIA PAULANI** (126 normas):
  - Dominante: *Educação e Creches* (62.1%)
  - Secundário: *Denominação de Vias e Logradouros* (7.1%)
- **MARCOS DE BARROS CRUZ** (105 normas):
  - Dominante: *Educação e Creches* (73.1%)
  - Secundário: *Denominação de Vias e Logradouros* (6.5%)
- **ANTONIO CESAR RUSSI CALLEGARI** (42 normas):
  - Dominante: *Educação e Creches* (32.3%)
  - Secundário: *Denominação de Vias e Logradouros* (16.0%)
- **JOSÉ FLORIANO DE AZEVEDO MARQUES NETO** (36 normas):
  - Dominante: *Educação e Creches* (27.2%)
  - Secundário: *Denominação de Vias e Logradouros* (26.7%)
- **JOSÉ DE FILIPPI JUNIOR** (14 normas):
  - Dominante: *Educação e Creches* (54.2%)
  - Secundário: *Denominação de Vias e Logradouros* (17.2%)
- **JOÃO LUIZ SILVA FERREIRA** (13 normas):
  - Dominante: *Educação e Creches* (81.3%)
  - Secundário: *Denominação de Vias e Logradouros* (3.4%)
- **OSVALDO SPURI** (11 normas):
  - Dominante: *Educação e Creches* (47.2%)
  - Secundário: *Denominação de Vias e Logradouros* (12.6%)
- **FRANCISCO MACENA DA SILVA** (11 normas):
  - Dominante: *Educação e Creches* (54.6%)
  - Secundário: *Denominação de Vias e Logradouros* (11.4%)
- **SIMÃO PEDRO CHIOVETTI** (9 normas):
  - Dominante: *Educação e Creches* (48.1%)
  - Secundário: *Administração e Normas Gerais (comitê)* (11.2%)
- **ELISEU GABRIEL DE PIERI** (8 normas):
  - Dominante: *Educação e Creches* (62.4%)
  - Secundário: *Denominação de Vias e Logradouros* (18.8%)
- **JILMAR AUGUSTINHO TATTO** (8 normas):
  - Dominante: *Educação e Creches* (43.0%)
  - Secundário: *Denominação de Vias e Logradouros* (20.3%)
- **FERNANDO DE MELLO FRANCO** (7 normas):
  - Dominante: *Educação e Creches* (29.1%)
  - Secundário: *Denominação de Vias e Logradouros* (17.8%)
- **RICARDO TEIXEIRA** (5 normas):
  - Dominante: *Educação e Creches* (20.7%)
  - Secundário: *Denominação de Vias e Logradouros* (19.9%)
