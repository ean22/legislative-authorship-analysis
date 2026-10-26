# Relatório Experimental de Aferição de Assertividade das Normas

- **Arquivo de Referência:** `../../data/normas_classificadas.csv`
- **Data da Aferição:** 2026-10-25 17:06:57
- **Total de Normas Anotadas (Ground Truth):** 500
- **Normas Casadas no Banco SQLite:** 500
- **Normas Classificadas no Banco:** 495

## Síntese Global de Acurácia

| Métrica Global | Total de Acertos | Percentual | Observação Metodológica |
|:---|:---:|:---:|:---|
| **Assertividade Exata** | 367 / 500 | **73.40%** | Casamento idêntico estrito da string do rótulo |
| **Assertividade Canônica (Equivalente)** | 371 / 500 | **74.20%** | Reconhece classes equivalentes (ex.: Bens Imóveis e Gestão Imobiliária) |
| **F1-Score Macro** | - | **0.5918** | Média harmônica balanceada entre classes |
| **F1-Score Ponderado (Weighted)** | - | **0.7838** | Ponderado pelo suporte de normas em cada classe |

## Performance Detalhada por Classe Temática

| Classe Temática | Esperado (CSV) | Predito (Banco) | TP | FP | FN | Precisão | Revocação | F1-Score |
|:---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| Crédito Orçamentário e Finanças | 160 | 147 | 145 | 2 | 15 | 98.6% | 90.6% | **94.5%** |
| Serviços Públicos e Gestão Municipal | 115 | 71 | 50 | 21 | 65 | 70.4% | 43.5% | **53.8%** |
| Denominação de Vias e Logradouros | 76 | 72 | 64 | 8 | 12 | 88.9% | 84.2% | **86.5%** |
| Bens Imóveis e Desapropriação | 71 | 65 | 61 | 4 | 10 | 93.8% | 85.9% | **89.7%** |
| Gestão Imobiliária, Habitação e Regularização Fundiária | 71 | 65 | 61 | 4 | 10 | 93.8% | 85.9% | **89.7%** |
| Educação e Creches | 22 | 45 | 20 | 25 | 2 | 44.4% | 90.9% | **59.7%** |
| Datas Comemorativas e Calendário | 17 | 19 | 16 | 3 | 1 | 84.2% | 94.1% | **88.9%** |
| Declaração de Utilidade Pública | 15 | 19 | 15 | 4 | 0 | 78.9% | 100.0% | **88.2%** |
| Transportes e Trânsito Urbano | 12 | 0 | 0 | 0 | 12 | 0.0% | 0.0% | **0.0%** |
| Meio Ambiente e Áreas Verdes | 7 | 0 | 0 | 0 | 7 | 0.0% | 0.0% | **0.0%** |
| Saúde Pública e Assistência Hospitalar | 5 | 0 | 0 | 0 | 5 | 0.0% | 0.0% | **0.0%** |
| Administração e Normas Gerais (classe) | 0 | 57 | 0 | 57 | 0 | 0.0% | 0.0% | **0.0%** |

## Matriz de Confusão e Mapeamento de Classes

| Classe Esperada (CSV) | Classe Atribuída no Banco | Volume de Normas | Status |
|:---|:---|:---:|:---:|
| Bens Imóveis e Desapropriação | Gestão Imobiliária, Habitação e Regularização Fundiária | 4 | ✅ Consonante |
| Crédito Orçamentário e Finanças | Administração e Normas Gerais (classe) | 9 | ⚠️ Divergente |
| Crédito Orçamentário e Finanças | Crédito Orçamentário e Finanças | 145 | ✅ Consonante |
| Crédito Orçamentário e Finanças | Educação e Creches | 1 | ⚠️ Divergente |
| Crédito Orçamentário e Finanças | Serviços Públicos e Gestão Municipal | 5 | ⚠️ Divergente |
| Datas Comemorativas e Calendário | Datas Comemorativas e Calendário | 16 | ✅ Consonante |
| Datas Comemorativas e Calendário | Educação e Creches | 1 | ⚠️ Divergente |
| Declaração de Utilidade Pública | Declaração de Utilidade Pública | 15 | ✅ Consonante |
| Denominação de Vias e Logradouros | Declaração de Utilidade Pública | 2 | ⚠️ Divergente |
| Denominação de Vias e Logradouros | Denominação de Vias e Logradouros | 64 | ✅ Consonante |
| Denominação de Vias e Logradouros | Educação e Creches | 8 | ⚠️ Divergente |
| Denominação de Vias e Logradouros | Gestão Imobiliária, Habitação e Regularização Fundiária | 2 | ⚠️ Divergente |
| Educação e Creches | Administração e Normas Gerais (classe) | 1 | ⚠️ Divergente |
| Educação e Creches | Educação e Creches | 20 | ✅ Consonante |
| Educação e Creches | Serviços Públicos e Gestão Municipal | 1 | ⚠️ Divergente |
| Gestão Imobiliária, Habitação e Regularização Fundiária | Administração e Normas Gerais (classe) | 2 | ⚠️ Divergente |
| Gestão Imobiliária, Habitação e Regularização Fundiária | Educação e Creches | 3 | ⚠️ Divergente |
| Gestão Imobiliária, Habitação e Regularização Fundiária | Gestão Imobiliária, Habitação e Regularização Fundiária | 57 | ✅ Consonante |
| Gestão Imobiliária, Habitação e Regularização Fundiária | Serviços Públicos e Gestão Municipal | 5 | ⚠️ Divergente |
| Meio Ambiente e Áreas Verdes | Administração e Normas Gerais (classe) | 1 | ⚠️ Divergente |
| Meio Ambiente e Áreas Verdes | Denominação de Vias e Logradouros | 3 | ⚠️ Divergente |
| Meio Ambiente e Áreas Verdes | Serviços Públicos e Gestão Municipal | 3 | ⚠️ Divergente |
| Saúde Pública e Assistência Hospitalar | Administração e Normas Gerais (classe) | 2 | ⚠️ Divergente |
| Saúde Pública e Assistência Hospitalar | Educação e Creches | 1 | ⚠️ Divergente |
| Saúde Pública e Assistência Hospitalar | Serviços Públicos e Gestão Municipal | 2 | ⚠️ Divergente |
| Serviços Públicos e Gestão Municipal | (Não Classificada) | 5 | ⚠️ Divergente |
| Serviços Públicos e Gestão Municipal | Administração e Normas Gerais (classe) | 37 | ⚠️ Divergente |
| Serviços Públicos e Gestão Municipal | Crédito Orçamentário e Finanças | 1 | ⚠️ Divergente |
| Serviços Públicos e Gestão Municipal | Datas Comemorativas e Calendário | 3 | ⚠️ Divergente |
| Serviços Públicos e Gestão Municipal | Declaração de Utilidade Pública | 2 | ⚠️ Divergente |
| Serviços Públicos e Gestão Municipal | Denominação de Vias e Logradouros | 5 | ⚠️ Divergente |
| Serviços Públicos e Gestão Municipal | Educação e Creches | 11 | ⚠️ Divergente |
| Serviços Públicos e Gestão Municipal | Gestão Imobiliária, Habitação e Regularização Fundiária | 1 | ⚠️ Divergente |
| Serviços Públicos e Gestão Municipal | Serviços Públicos e Gestão Municipal | 50 | ✅ Consonante |
| Transportes e Trânsito Urbano | Administração e Normas Gerais (classe) | 5 | ⚠️ Divergente |
| Transportes e Trânsito Urbano | Crédito Orçamentário e Finanças | 1 | ⚠️ Divergente |
| Transportes e Trânsito Urbano | Gestão Imobiliária, Habitação e Regularização Fundiária | 1 | ⚠️ Divergente |
| Transportes e Trânsito Urbano | Serviços Públicos e Gestão Municipal | 5 | ⚠️ Divergente |

## Amostras de Divergência para Auditoria Manual

### 1. Lei Ordinária N° 15683/2013

- **Classe Esperada (Ground Truth):** `Gestão Imobiliária, Habitação e Regularização Fundiária`
- **Classe Predita no Banco:** `Educação e Creches`
- **Confiança/Score de Pertinência:** `0.1427`
- **Ementa:** *"AUTORIZA A TRANSFERÊNCIA, A TÍTULO NÃO ONEROSO, À COMPANHIA DE DESENVOLVIMENTO HABITACIONAL E URBANO DO ESTADO DE SÃO PAULO - CDHU, DA PROPRIEDADE DAS ÁREAS MUNICIPAIS QUE ESPECIFICA, SITUADAS EM PARAISÓPOLIS."*

### 2. Lei Ordinária N° 15689/2013

- **Classe Esperada (Ground Truth):** `Gestão Imobiliária, Habitação e Regularização Fundiária`
- **Classe Predita no Banco:** `Educação e Creches`
- **Confiança/Score de Pertinência:** `0.1545`
- **Ementa:** *"AUTORIZA A TRANSFERÊNCIA, A TÍTULO NÃO ONEROSO, À COMPANHIA DE DESENVOLVIMENTO HABITACIONAL E URBANO DO ESTADO DE SÃO PAULO - CDHU, DA PROPRIEDADE DA ÁREA MUNICIPAL QUE ESPECIFICA."*

### 3. Lei Ordinária N° 15728/2013

- **Classe Esperada (Ground Truth):** `Gestão Imobiliária, Habitação e Regularização Fundiária`
- **Classe Predita no Banco:** `Serviços Públicos e Gestão Municipal`
- **Confiança/Score de Pertinência:** `0.1590`
- **Ementa:** *"AUTORIZA A TRANSFERÊNCIA, A TÍTULO NÃO ONEROSO, À COMPANHIA METROPOLITANA DE HABITAÇÃO DE SÃO PAULO - COHAB-SP, DA PROPRIEDADE DE IMÓVEIS MUNICIPAIS QUE INTEGRARÃO O FUNDO MUNICIPAL DE HABITAÇÃO, E DÁ PROVIDÊNCIAS CORRELATAS."*

### 4. Lei Ordinária N° 15729/2013

- **Classe Esperada (Ground Truth):** `Gestão Imobiliária, Habitação e Regularização Fundiária`
- **Classe Predita no Banco:** `Serviços Públicos e Gestão Municipal`
- **Confiança/Score de Pertinência:** `0.1625`
- **Ementa:** *"AUTORIZA A TRANSFERÊNCIA, A TÍTULO NÃO ONEROSO, À COMPANHIA METROPOLITANA DE HABITAÇÃO DE SÃO PAULO - COHAB-SP, DA PROPRIEDADE DE IMÓVEIS MUNICIPAIS INTEGRANTES DO CONJUNTO HABITACIONAL SÍTIO CASA BLANCA, E DÁ PROVIDÊNCIAS CORRELATAS."*

### 5. Lei Ordinária N° 15765/2013

- **Classe Esperada (Ground Truth):** `Gestão Imobiliária, Habitação e Regularização Fundiária`
- **Classe Predita no Banco:** `Educação e Creches`
- **Confiança/Score de Pertinência:** `0.1361`
- **Ementa:** *"AUTORIZA A TRANSFERÊNCIA, A TÍTULO NÃO ONEROSO, À COMPANHIA DE DESENVOLVIMENTO HABITACIONAL E URBANO DO ESTADO DE SÃO PAULO - CDHU, DA PROPRIEDADE DE ÁREA MUNICIPAL SITUADA NA AVENIDA ALMIRANTE MARIATH, DISTRITO DO IPIRANGA."*

### 6. Lei Ordinária N° 15812/2013

- **Classe Esperada (Ground Truth):** `Gestão Imobiliária, Habitação e Regularização Fundiária`
- **Classe Predita no Banco:** `Serviços Públicos e Gestão Municipal`
- **Confiança/Score de Pertinência:** `0.1504`
- **Ementa:** *"AUTORIZA A TRANSFERÊNCIA, A TÍTULO NÃO ONEROSO, À COMPANHIA METROPOLITANA DE HABITAÇÃO DE SÃO PAULO - COHAB-SP, DA PROPRIEDADE DE IMÓVEIS MUNICIPAIS INTEGRANTES DO CONJUNTO HABITACIONAL REAL PARQUE - QUADRA H, E DÁ PROVIDÊNCIAS CORRELATAS."*

### 7. Decreto N° 53694/2013

- **Classe Esperada (Ground Truth):** `Crédito Orçamentário e Finanças`
- **Classe Predita no Banco:** `Serviços Públicos e Gestão Municipal`
- **Confiança/Score de Pertinência:** `0.2910`
- **Ementa:** *"FIXA NORMAS REFERENTES À EXECUÇÃO ORÇAMENTÁRIA E FINANCEIRA PARA O EXERCÍCIO DE 2013."*

### 8. Decreto N° 53699/2013

- **Classe Esperada (Ground Truth):** `Crédito Orçamentário e Finanças`
- **Classe Predita no Banco:** `Administração e Normas Gerais (classe)`
- **Confiança/Score de Pertinência:** `0.1305`
- **Ementa:** *"CONFERE NOVA REDAÇÃO AO "CAPUT" DO ARTIGO 1º DO DECRETO Nº 52.064, DE 30 DE DEZEMBRO DE 2010, QUE DISPÕE SOBRE O CUMPRIMENTO DO REGIME ESPECIAL DE PAGAMENTO DE PRECATÓRIOS INSTITUÍDO PELO ARTIGO 97 DO ATO DAS DISPOSIÇÕES CONSTITUCIONAIS TRANSITÓRIAS, INTRODUZIDO PELA EMENDA CONSTITUCIONAL Nº 62/2009."*

### 9. Decreto N° 53710/2013

- **Classe Esperada (Ground Truth):** `Crédito Orçamentário e Finanças`
- **Classe Predita no Banco:** `Serviços Públicos e Gestão Municipal`
- **Confiança/Score de Pertinência:** `0.2376`
- **Ementa:** *"CONFERE NOVA REDAÇÃO AO ARTIGO 36 DO DECRETO Nº 53.694, DE 14 DE JANEIRO DE 2013, QUE FIXA NORMAS REFERENTES À EXECUÇÃO ORÇAMENTÁRIA E FINANCEIRA PARA O EXERCÍCIO DE 2013."*

### 10. Decreto N° 53732/2013

- **Classe Esperada (Ground Truth):** `Crédito Orçamentário e Finanças`
- **Classe Predita no Banco:** `Serviços Públicos e Gestão Municipal`
- **Confiança/Score de Pertinência:** `0.1866`
- **Ementa:** *"AUTORIZA A SECRETARIA MUNICIPAL DE SERVIÇOS A MOVIMENTAR AS DOTAÇÕES ORÇAMENTÁRIAS QUE ESPECIFICA."*

### 11. Decreto N° 53770/2013

- **Classe Esperada (Ground Truth):** `Crédito Orçamentário e Finanças`
- **Classe Predita no Banco:** `Serviços Públicos e Gestão Municipal`
- **Confiança/Score de Pertinência:** `0.2441`
- **Ementa:** *"ALTERA OS §§ 2º E 4º DO ARTIGO 30 DO DECRETO Nº 52.934, DE 20 DE JANEIRO DE 2012, QUE FIXA NORMAS REFERENTES À EXECUÇÃO ORÇAMENTÁRIA E FINANCEIRA PARA O EXERCÍCIO DE 2012."*

### 12. Lei Ordinária N° 15687/2013

- **Classe Esperada (Ground Truth):** `Crédito Orçamentário e Finanças`
- **Classe Predita no Banco:** `Administração e Normas Gerais (classe)`
- **Confiança/Score de Pertinência:** `0.1542`
- **Ementa:** *"ALTERA O "CAPUT" DO ART. 1º DA LEI Nº 15.390/2011, QUE AUTORIZA O EXECUTIVO A CONTRATAR OPERAÇÕES DE CRÉDITO RELATIVAS AO PROGRAMA DE MODERNIZAÇÃO DA ADMINISTRAÇÃO TRIBUTÁRIA E DA GESTÃO DOS SETORES SOCIAIS BÁSICOS - PMAT E AO PROGRAMA NACIONAL DE APOIO À GESTÃO ADMINISTRATIVA E FISCAL DOS MUNICÍPIOS BRASILEIROS - PNAFM E ALTERA A REDAÇÃO DO "CAPUT" DO ART. 9º DA LEI Nº 15.499/2011, PARA ESTENDER O PRAZO DE REQUERIMENTO DO AUTO DE LICENÇA DE FUNCIONAMENTO CONDICIONADO, E DÁ OUTRAS PROVIDÊNCIAS."*

### 13. Lei Ordinária N° 15690/2013

- **Classe Esperada (Ground Truth):** `Crédito Orçamentário e Finanças`
- **Classe Predita no Banco:** `Serviços Públicos e Gestão Municipal`
- **Confiança/Score de Pertinência:** `0.1355`
- **Ementa:** *"ALTERA OS ARTS. 29, 43, 49, 50, 54, 55, 58, 59, 60, 63 E 65, BEM COMO A TABELA "A" DO ANEXO II, TODOS DA LEI Nº 14.107, DE 12 DE DEZEMBRO DE 2005, QUE DISPÕE SOBRE O PROCESSO ADMINISTRATIVO FISCAL E CRIA O CONSELHO MUNICIPAL DE TRIBUTOS; ACRESCE O ART. 44-A E REVOGA O § 6º DO ART. 55, O ART. 56 E O ANEXO I DA MESMA LEI."*

### 14. Decreto N° 53953/2013

- **Classe Esperada (Ground Truth):** `Crédito Orçamentário e Finanças`
- **Classe Predita no Banco:** `Administração e Normas Gerais (classe)`
- **Confiança/Score de Pertinência:** `0.1455`
- **Ementa:** *"DIVULGA O QUADRO DEMONSTRATIVO DA RELAÇÃO DE DESPESAS E RECEITAS ATINENTE AO QUADRIMESTRE DE NOVEMBRO DE 2012 A FEVEREIRO DE 2013, EM CUMPRIMENTO AO DISPOSTO NO § 4º DO ARTIGO 2º DA LEI Nº 13.303, DE 18 DE JANEIRO DE 2002."*

### 15. Decreto N° 53947/2013

- **Classe Esperada (Ground Truth):** `Crédito Orçamentário e Finanças`
- **Classe Predita no Banco:** `Administração e Normas Gerais (classe)`
- **Confiança/Score de Pertinência:** `0.1455`
- **Ementa:** *"DIVULGA O QUADRO DEMONSTRATIVO DA RELAÇÃO DE DESPESAS E RECEITAS ATINENTE AO QUADRIMESTRE DE NOVEMBRO DE 2010 A FEVEREIRO DE 2011, EM CUMPRIMENTO AO DISPOSTO NO § 4º DO ARTIGO 2º DA LEI Nº 13.303, DE 18 DE JANEIRO DE 2002."*

