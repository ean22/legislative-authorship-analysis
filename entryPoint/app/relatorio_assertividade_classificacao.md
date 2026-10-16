# Relatório Experimental de Aferição de Assertividade das Normas

- **Arquivo de Referência:** `../../data/normas_classificadas.csv`
- **Data da Aferição:** 2026-10-16 09:07:58
- **Total de Normas Anotadas (Ground Truth):** 500
- **Normas Casadas no Banco SQLite:** 500
- **Normas Classificadas no Banco:** 283

## Síntese Global de Acurácia

| Métrica Global | Total de Acertos | Percentual | Observação Metodológica |
|:---|:---:|:---:|:---|
| **Assertividade Exata** | 67 / 500 | **13.40%** | Casamento idêntico estrito da string do rótulo |
| **Assertividade Canônica (Equivalente)** | 67 / 500 | **13.40%** | Reconhece classes equivalentes (ex.: Bens Imóveis e Gestão Imobiliária) |
| **F1-Score Macro** | - | **0.0773** | Média harmônica balanceada entre classes |
| **F1-Score Ponderado (Weighted)** | - | **0.1524** | Ponderado pelo suporte de normas em cada classe |

## Performance Detalhada por Classe Temática

| Classe Temática | Esperado (CSV) | Predito (Banco) | TP | FP | FN | Precisão | Revocação | F1-Score |
|:---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| Crédito Orçamentário e Finanças | 160 | 102 | 48 | 54 | 112 | 47.1% | 30.0% | **36.6%** |
| Serviços Públicos e Gestão Municipal | 115 | 0 | 0 | 0 | 115 | 0.0% | 0.0% | **0.0%** |
| Denominação de Vias e Logradouros | 76 | 68 | 5 | 63 | 71 | 7.4% | 6.6% | **6.9%** |
| Bens Imóveis e Desapropriação | 71 | 75 | 11 | 64 | 60 | 14.7% | 15.5% | **15.1%** |
| Gestão Imobiliária, Habitação e Regularização Fundiária | 71 | 75 | 11 | 64 | 60 | 14.7% | 15.5% | **15.1%** |
| Educação e Creches | 22 | 0 | 0 | 0 | 22 | 0.0% | 0.0% | **0.0%** |
| Datas Comemorativas e Calendário | 17 | 0 | 0 | 0 | 17 | 0.0% | 0.0% | **0.0%** |
| Declaração de Utilidade Pública | 15 | 38 | 3 | 35 | 12 | 7.9% | 20.0% | **11.3%** |
| Transportes e Trânsito Urbano | 12 | 0 | 0 | 0 | 12 | 0.0% | 0.0% | **0.0%** |
| Meio Ambiente e Áreas Verdes | 7 | 0 | 0 | 0 | 7 | 0.0% | 0.0% | **0.0%** |
| Saúde Pública e Assistência Hospitalar | 5 | 0 | 0 | 0 | 5 | 0.0% | 0.0% | **0.0%** |

## Matriz de Confusão e Mapeamento de Classes

| Classe Esperada (CSV) | Classe Atribuída no Banco | Volume de Normas | Status |
|:---|:---|:---:|:---:|
| Bens Imóveis e Desapropriação | Crédito Orçamentário e Finanças | 2 | ⚠️ Divergente |
| Bens Imóveis e Desapropriação | Declaração de Utilidade Pública | 1 | ⚠️ Divergente |
| Bens Imóveis e Desapropriação | Denominação de Vias e Logradouros | 1 | ⚠️ Divergente |
| Crédito Orçamentário e Finanças | (Não Classificada) | 61 | ⚠️ Divergente |
| Crédito Orçamentário e Finanças | Crédito Orçamentário e Finanças | 48 | ✅ Consonante |
| Crédito Orçamentário e Finanças | Declaração de Utilidade Pública | 12 | ⚠️ Divergente |
| Crédito Orçamentário e Finanças | Denominação de Vias e Logradouros | 17 | ⚠️ Divergente |
| Crédito Orçamentário e Finanças | Gestão Imobiliária, Habitação e Regularização Fundiária | 22 | ⚠️ Divergente |
| Datas Comemorativas e Calendário | (Não Classificada) | 15 | ⚠️ Divergente |
| Datas Comemorativas e Calendário | Denominação de Vias e Logradouros | 2 | ⚠️ Divergente |
| Declaração de Utilidade Pública | (Não Classificada) | 6 | ⚠️ Divergente |
| Declaração de Utilidade Pública | Crédito Orçamentário e Finanças | 4 | ⚠️ Divergente |
| Declaração de Utilidade Pública | Declaração de Utilidade Pública | 3 | ✅ Consonante |
| Declaração de Utilidade Pública | Denominação de Vias e Logradouros | 1 | ⚠️ Divergente |
| Declaração de Utilidade Pública | Gestão Imobiliária, Habitação e Regularização Fundiária | 1 | ⚠️ Divergente |
| Denominação de Vias e Logradouros | (Não Classificada) | 50 | ⚠️ Divergente |
| Denominação de Vias e Logradouros | Crédito Orçamentário e Finanças | 10 | ⚠️ Divergente |
| Denominação de Vias e Logradouros | Declaração de Utilidade Pública | 4 | ⚠️ Divergente |
| Denominação de Vias e Logradouros | Denominação de Vias e Logradouros | 5 | ✅ Consonante |
| Denominação de Vias e Logradouros | Gestão Imobiliária, Habitação e Regularização Fundiária | 7 | ⚠️ Divergente |
| Educação e Creches | (Não Classificada) | 4 | ⚠️ Divergente |
| Educação e Creches | Crédito Orçamentário e Finanças | 2 | ⚠️ Divergente |
| Educação e Creches | Declaração de Utilidade Pública | 1 | ⚠️ Divergente |
| Educação e Creches | Denominação de Vias e Logradouros | 3 | ⚠️ Divergente |
| Educação e Creches | Gestão Imobiliária, Habitação e Regularização Fundiária | 12 | ⚠️ Divergente |
| Gestão Imobiliária, Habitação e Regularização Fundiária | (Não Classificada) | 26 | ⚠️ Divergente |
| Gestão Imobiliária, Habitação e Regularização Fundiária | Crédito Orçamentário e Finanças | 14 | ⚠️ Divergente |
| Gestão Imobiliária, Habitação e Regularização Fundiária | Declaração de Utilidade Pública | 6 | ⚠️ Divergente |
| Gestão Imobiliária, Habitação e Regularização Fundiária | Denominação de Vias e Logradouros | 10 | ⚠️ Divergente |
| Gestão Imobiliária, Habitação e Regularização Fundiária | Gestão Imobiliária, Habitação e Regularização Fundiária | 11 | ✅ Consonante |
| Meio Ambiente e Áreas Verdes | (Não Classificada) | 3 | ⚠️ Divergente |
| Meio Ambiente e Áreas Verdes | Crédito Orçamentário e Finanças | 1 | ⚠️ Divergente |
| Meio Ambiente e Áreas Verdes | Denominação de Vias e Logradouros | 2 | ⚠️ Divergente |
| Meio Ambiente e Áreas Verdes | Gestão Imobiliária, Habitação e Regularização Fundiária | 1 | ⚠️ Divergente |
| Saúde Pública e Assistência Hospitalar | (Não Classificada) | 2 | ⚠️ Divergente |
| Saúde Pública e Assistência Hospitalar | Crédito Orçamentário e Finanças | 1 | ⚠️ Divergente |
| Saúde Pública e Assistência Hospitalar | Gestão Imobiliária, Habitação e Regularização Fundiária | 2 | ⚠️ Divergente |
| Serviços Públicos e Gestão Municipal | (Não Classificada) | 41 | ⚠️ Divergente |
| Serviços Públicos e Gestão Municipal | Crédito Orçamentário e Finanças | 19 | ⚠️ Divergente |
| Serviços Públicos e Gestão Municipal | Declaração de Utilidade Pública | 11 | ⚠️ Divergente |
| Serviços Públicos e Gestão Municipal | Denominação de Vias e Logradouros | 25 | ⚠️ Divergente |
| Serviços Públicos e Gestão Municipal | Gestão Imobiliária, Habitação e Regularização Fundiária | 19 | ⚠️ Divergente |
| Transportes e Trânsito Urbano | (Não Classificada) | 9 | ⚠️ Divergente |
| Transportes e Trânsito Urbano | Crédito Orçamentário e Finanças | 1 | ⚠️ Divergente |
| Transportes e Trânsito Urbano | Denominação de Vias e Logradouros | 2 | ⚠️ Divergente |

## Amostras de Divergência para Auditoria Manual

### 1. Lei Ordinária N° 15683/2013

- **Classe Esperada (Ground Truth):** `Gestão Imobiliária, Habitação e Regularização Fundiária`
- **Classe Predita no Banco:** `Crédito Orçamentário e Finanças`
- **Confiança/Score de Pertinência:** `0.8416`
- **Ementa:** *"AUTORIZA A TRANSFERÊNCIA, A TÍTULO NÃO ONEROSO, À COMPANHIA DE DESENVOLVIMENTO HABITACIONAL E URBANO DO ESTADO DE SÃO PAULO - CDHU, DA PROPRIEDADE DAS ÁREAS MUNICIPAIS QUE ESPECIFICA, SITUADAS EM PARAISÓPOLIS."*

### 2. Decreto N° 53772/2013

- **Classe Esperada (Ground Truth):** `Gestão Imobiliária, Habitação e Regularização Fundiária`
- **Classe Predita no Banco:** `Crédito Orçamentário e Finanças`
- **Confiança/Score de Pertinência:** `0.8416`
- **Ementa:** *"DECLARA DE UTILIDADE PÚBLICA, PARA DESAPROPRIAÇÃO, IMÓVEIS PARTICULARES SITUADOS NO DISTRITO DE SANTO AMARO, SUBPREFEITURA DE SANTO AMARO, NECESSÁRIOS AO ALARGAMENTO DE PASSEIO PÚBLICO."*

### 3. Decreto N° 53799/2013

- **Classe Esperada (Ground Truth):** `Gestão Imobiliária, Habitação e Regularização Fundiária`
- **Classe Predita no Banco:** `Crédito Orçamentário e Finanças`
- **Confiança/Score de Pertinência:** `0.8416`
- **Ementa:** *"ESTABELECE O PROCEDIMENTO A SER OBSERVADO PARA A DESAPROPRIAÇÃO DE BENS ÚTEIS OU NECESSÁRIOS AOS INTERESSES DA ADMINISTRAÇÃO MUNICIPAL."*

### 4. Lei Ordinária N° 15689/2013

- **Classe Esperada (Ground Truth):** `Gestão Imobiliária, Habitação e Regularização Fundiária`
- **Classe Predita no Banco:** `Declaração de Utilidade Pública`
- **Confiança/Score de Pertinência:** `0.5951`
- **Ementa:** *"AUTORIZA A TRANSFERÊNCIA, A TÍTULO NÃO ONEROSO, À COMPANHIA DE DESENVOLVIMENTO HABITACIONAL E URBANO DO ESTADO DE SÃO PAULO - CDHU, DA PROPRIEDADE DA ÁREA MUNICIPAL QUE ESPECIFICA."*

### 5. Decreto N° 53847/2013

- **Classe Esperada (Ground Truth):** `Gestão Imobiliária, Habitação e Regularização Fundiária`
- **Classe Predita no Banco:** `Denominação de Vias e Logradouros`
- **Confiança/Score de Pertinência:** `0.1667`
- **Ementa:** *"DECLARA DE UTILIDADE PÚBLICA, PARA DESAPROPRIAÇÃO, IMÓVEL PARTICULAR SITUADO NO DISTRITO DO RIO PEQUENO, SUBPREFEITURA DE BUTANTÃ, NECESSÁRIO À IMPLANTAÇÃO DE CENTRO DE EDUCAÇÃO INFANTIL."*

### 6. Decreto N° 53848/2013

- **Classe Esperada (Ground Truth):** `Gestão Imobiliária, Habitação e Regularização Fundiária`
- **Classe Predita no Banco:** `Denominação de Vias e Logradouros`
- **Confiança/Score de Pertinência:** `0.3421`
- **Ementa:** *"DECLARA DE UTILIDADE PÚBLICA, PARA DESAPROPRIAÇÃO, IMÓVEL PARTICULAR SITUADO NO DISTRITO DE PEDREIRA, SUBPREFEITURA DE CIDADE ADEMAR, NECESSÁRIO À IMPLANTAÇÃO DE CENTRO DE EDUCAÇÃO INFANTIL."*

### 7. Decreto N° 53849/2013

- **Classe Esperada (Ground Truth):** `Gestão Imobiliária, Habitação e Regularização Fundiária`
- **Classe Predita no Banco:** `Denominação de Vias e Logradouros`
- **Confiança/Score de Pertinência:** `0.3499`
- **Ementa:** *"DECLARA DE UTILIDADE PÚBLICA, PARA DESAPROPRIAÇÃO, IMÓVEL PARTICULAR SITUADO NO DISTRITO DO GRAJAÚ, SUBPREFEITURA DE CAPELA DO SOCORRO, NECESSÁRIO À IMPLANTAÇÃO DE CENTRO DE EDUCAÇÃO INFANTIL."*

### 8. Decreto N° 53852/2013

- **Classe Esperada (Ground Truth):** `Gestão Imobiliária, Habitação e Regularização Fundiária`
- **Classe Predita no Banco:** `Denominação de Vias e Logradouros`
- **Confiança/Score de Pertinência:** `0.1667`
- **Ementa:** *"DECLARA DE UTILIDADE PÚBLICA, PARA DESAPROPRIAÇÃO, IMÓVEIS PARTICULARES SITUADOS NO DISTRITO DE CAMPO LIMPO, SUBPREFEITURA DE CAMPO LIMPO, NECESSÁRIOS À IMPLANTAÇÃO DE CENTRO DE EDUCAÇÃO INFANTIL."*

### 9. Decreto N° 53853/2013

- **Classe Esperada (Ground Truth):** `Gestão Imobiliária, Habitação e Regularização Fundiária`
- **Classe Predita no Banco:** `Denominação de Vias e Logradouros`
- **Confiança/Score de Pertinência:** `0.6556`
- **Ementa:** *"DECLARA DE UTILIDADE PÚBLICA, PARA DESAPROPRIAÇÃO, IMÓVEIS PARTICULARES SITUADOS NO DISTRITO DE PIRITUBA, SUBPREFEITURA DE PIRITUBA/JARAGUÁ, NECESSÁRIOS À IMPLANTAÇÃO DE CENTRO DE EDUCAÇÃO INFANTIL."*

### 10. Decreto N° 53855/2013

- **Classe Esperada (Ground Truth):** `Gestão Imobiliária, Habitação e Regularização Fundiária`
- **Classe Predita no Banco:** `Crédito Orçamentário e Finanças`
- **Confiança/Score de Pertinência:** `0.8416`
- **Ementa:** *"DECLARA DE UTILIDADE PÚBLICA, PARA DESAPROPRIAÇÃO, IMÓVEL PARTICULAR SITUADO NO DISTRITO DE SAPOPEMBA, SUBPREFEITURA DE VILA PRUDENTE/SAPOPEMBA, NECESSÁRIO À IMPLANTAÇÃO DE CENTRO DE EDUCAÇÃO INFANTIL."*

### 11. Decreto N° 53856/2013

- **Classe Esperada (Ground Truth):** `Gestão Imobiliária, Habitação e Regularização Fundiária`
- **Classe Predita no Banco:** `Denominação de Vias e Logradouros`
- **Confiança/Score de Pertinência:** `0.7891`
- **Ementa:** *"DECLARA DE UTILIDADE PÚBLICA, PARA DESAPROPRIAÇÃO, IMÓVEL PARTICULAR SITUADO NO DISTRITO DE PARELHEIROS, SUBPREFEITURA DE PARELHEIROS, NECESSÁRIO À IMPLANTAÇÃO DE HOSPITAL MUNICIPAL."*

### 12. Lei Ordinária N° 15728/2013

- **Classe Esperada (Ground Truth):** `Gestão Imobiliária, Habitação e Regularização Fundiária`
- **Classe Predita no Banco:** `Declaração de Utilidade Pública`
- **Confiança/Score de Pertinência:** `0.6012`
- **Ementa:** *"AUTORIZA A TRANSFERÊNCIA, A TÍTULO NÃO ONEROSO, À COMPANHIA METROPOLITANA DE HABITAÇÃO DE SÃO PAULO - COHAB-SP, DA PROPRIEDADE DE IMÓVEIS MUNICIPAIS QUE INTEGRARÃO O FUNDO MUNICIPAL DE HABITAÇÃO, E DÁ PROVIDÊNCIAS CORRELATAS."*

### 13. Lei Ordinária N° 15729/2013

- **Classe Esperada (Ground Truth):** `Gestão Imobiliária, Habitação e Regularização Fundiária`
- **Classe Predita no Banco:** `Crédito Orçamentário e Finanças`
- **Confiança/Score de Pertinência:** `0.2485`
- **Ementa:** *"AUTORIZA A TRANSFERÊNCIA, A TÍTULO NÃO ONEROSO, À COMPANHIA METROPOLITANA DE HABITAÇÃO DE SÃO PAULO - COHAB-SP, DA PROPRIEDADE DE IMÓVEIS MUNICIPAIS INTEGRANTES DO CONJUNTO HABITACIONAL SÍTIO CASA BLANCA, E DÁ PROVIDÊNCIAS CORRELATAS."*

### 14. Decreto N° 53892/2013

- **Classe Esperada (Ground Truth):** `Gestão Imobiliária, Habitação e Regularização Fundiária`
- **Classe Predita no Banco:** `Crédito Orçamentário e Finanças`
- **Confiança/Score de Pertinência:** `0.8416`
- **Ementa:** *"DECLARA DE UTILIDADE PÚBLICA, PARA DESAPROPRIAÇÃO, IMÓVEL PARTICULAR SITUADO NO DISTRITO DE VILA BRASILÂNDIA, SUBPREFEITURA DE FREGUESIA/BRASILÂNDIA, NECESSÁRIO À IMPLANTAÇÃO DE HOSPITAL MUNICIPAL."*

### 15. Decreto N° 53908/2013

- **Classe Esperada (Ground Truth):** `Gestão Imobiliária, Habitação e Regularização Fundiária`
- **Classe Predita no Banco:** `Denominação de Vias e Logradouros`
- **Confiança/Score de Pertinência:** `0.1667`
- **Ementa:** *"DECLARA DE INTERESSE SOCIAL, PARA DESAPROPRIAÇÃO, IMÓVEL PARTICULAR SITUADO NO DISTRITO DE SACOMÃ, SUBPREFEITURA DE IPIRANGA, NECESSÁRIO À IMPLANTAÇÃO DE NÚCLEO HABITACIONAL."*

