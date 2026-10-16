# Relatório Experimental de Aferição de Assertividade das Normas

- **Arquivo de Referência:** `../../data/normas_classificadas.csv`
- **Data da Aferição:** 2026-10-16 09:06:01
- **Total de Normas Anotadas (Ground Truth):** 500
- **Normas Casadas no Banco SQLite:** 500
- **Normas Classificadas no Banco:** 475

## Síntese Global de Acurácia

| Métrica Global | Total de Acertos | Percentual | Observação Metodológica |
|:---|:---:|:---:|:---|
| **Assertividade Exata** | 255 / 500 | **51.00%** | Casamento idêntico estrito da string do rótulo |
| **Assertividade Canônica (Equivalente)** | 259 / 500 | **51.80%** | Reconhece classes equivalentes (ex.: Bens Imóveis e Gestão Imobiliária) |
| **F1-Score Macro** | - | **0.3131** | Média harmônica balanceada entre classes |
| **F1-Score Ponderado (Weighted)** | - | **0.5160** | Ponderado pelo suporte de normas em cada classe |

## Performance Detalhada por Classe Temática

| Classe Temática | Esperado (CSV) | Predito (Banco) | TP | FP | FN | Precisão | Revocação | F1-Score |
|:---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| Crédito Orçamentário e Finanças | 160 | 175 | 146 | 29 | 14 | 83.4% | 91.3% | **87.2%** |
| Serviços Públicos e Gestão Municipal | 115 | 50 | 27 | 23 | 88 | 54.0% | 23.5% | **32.7%** |
| Denominação de Vias e Logradouros | 76 | 53 | 25 | 28 | 51 | 47.2% | 32.9% | **38.8%** |
| Bens Imóveis e Desapropriação | 71 | 50 | 31 | 19 | 40 | 62.0% | 43.7% | **51.2%** |
| Gestão Imobiliária, Habitação e Regularização Fundiária | 71 | 50 | 31 | 19 | 40 | 62.0% | 43.7% | **51.2%** |
| Educação e Creches | 22 | 46 | 13 | 33 | 9 | 28.3% | 59.1% | **38.2%** |
| Datas Comemorativas e Calendário | 17 | 36 | 2 | 34 | 15 | 5.6% | 11.8% | **7.5%** |
| Declaração de Utilidade Pública | 15 | 65 | 15 | 50 | 0 | 23.1% | 100.0% | **37.5%** |
| Transportes e Trânsito Urbano | 12 | 0 | 0 | 0 | 12 | 0.0% | 0.0% | **0.0%** |
| Meio Ambiente e Áreas Verdes | 7 | 0 | 0 | 0 | 7 | 0.0% | 0.0% | **0.0%** |
| Saúde Pública e Assistência Hospitalar | 5 | 0 | 0 | 0 | 5 | 0.0% | 0.0% | **0.0%** |

## Matriz de Confusão e Mapeamento de Classes

| Classe Esperada (CSV) | Classe Atribuída no Banco | Volume de Normas | Status |
|:---|:---|:---:|:---:|
| Bens Imóveis e Desapropriação | Gestão Imobiliária, Habitação e Regularização Fundiária | 4 | ✅ Consonante |
| Crédito Orçamentário e Finanças | Crédito Orçamentário e Finanças | 146 | ✅ Consonante |
| Crédito Orçamentário e Finanças | Denominação de Vias e Logradouros | 5 | ⚠️ Divergente |
| Crédito Orçamentário e Finanças | Educação e Creches | 1 | ⚠️ Divergente |
| Crédito Orçamentário e Finanças | Serviços Públicos e Gestão Municipal | 8 | ⚠️ Divergente |
| Datas Comemorativas e Calendário | Crédito Orçamentário e Finanças | 1 | ⚠️ Divergente |
| Datas Comemorativas e Calendário | Datas Comemorativas e Calendário | 2 | ✅ Consonante |
| Datas Comemorativas e Calendário | Declaração de Utilidade Pública | 10 | ⚠️ Divergente |
| Datas Comemorativas e Calendário | Educação e Creches | 2 | ⚠️ Divergente |
| Datas Comemorativas e Calendário | Serviços Públicos e Gestão Municipal | 2 | ⚠️ Divergente |
| Declaração de Utilidade Pública | Declaração de Utilidade Pública | 15 | ✅ Consonante |
| Denominação de Vias e Logradouros | Crédito Orçamentário e Finanças | 7 | ⚠️ Divergente |
| Denominação de Vias e Logradouros | Datas Comemorativas e Calendário | 10 | ⚠️ Divergente |
| Denominação de Vias e Logradouros | Declaração de Utilidade Pública | 22 | ⚠️ Divergente |
| Denominação de Vias e Logradouros | Denominação de Vias e Logradouros | 25 | ✅ Consonante |
| Denominação de Vias e Logradouros | Educação e Creches | 2 | ⚠️ Divergente |
| Denominação de Vias e Logradouros | Gestão Imobiliária, Habitação e Regularização Fundiária | 7 | ⚠️ Divergente |
| Denominação de Vias e Logradouros | Serviços Públicos e Gestão Municipal | 3 | ⚠️ Divergente |
| Educação e Creches | Datas Comemorativas e Calendário | 1 | ⚠️ Divergente |
| Educação e Creches | Declaração de Utilidade Pública | 2 | ⚠️ Divergente |
| Educação e Creches | Denominação de Vias e Logradouros | 5 | ⚠️ Divergente |
| Educação e Creches | Educação e Creches | 13 | ✅ Consonante |
| Educação e Creches | Serviços Públicos e Gestão Municipal | 1 | ⚠️ Divergente |
| Gestão Imobiliária, Habitação e Regularização Fundiária | Crédito Orçamentário e Finanças | 5 | ⚠️ Divergente |
| Gestão Imobiliária, Habitação e Regularização Fundiária | Datas Comemorativas e Calendário | 12 | ⚠️ Divergente |
| Gestão Imobiliária, Habitação e Regularização Fundiária | Declaração de Utilidade Pública | 4 | ⚠️ Divergente |
| Gestão Imobiliária, Habitação e Regularização Fundiária | Denominação de Vias e Logradouros | 6 | ⚠️ Divergente |
| Gestão Imobiliária, Habitação e Regularização Fundiária | Educação e Creches | 10 | ⚠️ Divergente |
| Gestão Imobiliária, Habitação e Regularização Fundiária | Gestão Imobiliária, Habitação e Regularização Fundiária | 27 | ✅ Consonante |
| Gestão Imobiliária, Habitação e Regularização Fundiária | Serviços Públicos e Gestão Municipal | 3 | ⚠️ Divergente |
| Meio Ambiente e Áreas Verdes | Crédito Orçamentário e Finanças | 1 | ⚠️ Divergente |
| Meio Ambiente e Áreas Verdes | Datas Comemorativas e Calendário | 1 | ⚠️ Divergente |
| Meio Ambiente e Áreas Verdes | Declaração de Utilidade Pública | 1 | ⚠️ Divergente |
| Meio Ambiente e Áreas Verdes | Denominação de Vias e Logradouros | 4 | ⚠️ Divergente |
| Saúde Pública e Assistência Hospitalar | Crédito Orçamentário e Finanças | 1 | ⚠️ Divergente |
| Saúde Pública e Assistência Hospitalar | Declaração de Utilidade Pública | 1 | ⚠️ Divergente |
| Saúde Pública e Assistência Hospitalar | Denominação de Vias e Logradouros | 2 | ⚠️ Divergente |
| Saúde Pública e Assistência Hospitalar | Gestão Imobiliária, Habitação e Regularização Fundiária | 1 | ⚠️ Divergente |
| Serviços Públicos e Gestão Municipal | (Não Classificada) | 23 | ⚠️ Divergente |
| Serviços Públicos e Gestão Municipal | Crédito Orçamentário e Finanças | 13 | ⚠️ Divergente |
| Serviços Públicos e Gestão Municipal | Datas Comemorativas e Calendário | 10 | ⚠️ Divergente |
| Serviços Públicos e Gestão Municipal | Declaração de Utilidade Pública | 8 | ⚠️ Divergente |
| Serviços Públicos e Gestão Municipal | Denominação de Vias e Logradouros | 6 | ⚠️ Divergente |
| Serviços Públicos e Gestão Municipal | Educação e Creches | 17 | ⚠️ Divergente |
| Serviços Públicos e Gestão Municipal | Gestão Imobiliária, Habitação e Regularização Fundiária | 11 | ⚠️ Divergente |
| Serviços Públicos e Gestão Municipal | Serviços Públicos e Gestão Municipal | 27 | ✅ Consonante |
| Transportes e Trânsito Urbano | (Não Classificada) | 2 | ⚠️ Divergente |
| Transportes e Trânsito Urbano | Crédito Orçamentário e Finanças | 1 | ⚠️ Divergente |
| Transportes e Trânsito Urbano | Declaração de Utilidade Pública | 2 | ⚠️ Divergente |
| Transportes e Trânsito Urbano | Educação e Creches | 1 | ⚠️ Divergente |
| Transportes e Trânsito Urbano | Serviços Públicos e Gestão Municipal | 6 | ⚠️ Divergente |

## Amostras de Divergência para Auditoria Manual

### 1. Lei Ordinária N° 15683/2013

- **Classe Esperada (Ground Truth):** `Gestão Imobiliária, Habitação e Regularização Fundiária`
- **Classe Predita no Banco:** `Serviços Públicos e Gestão Municipal`
- **Confiança/Score de Pertinência:** `0.3795`
- **Ementa:** *"AUTORIZA A TRANSFERÊNCIA, A TÍTULO NÃO ONEROSO, À COMPANHIA DE DESENVOLVIMENTO HABITACIONAL E URBANO DO ESTADO DE SÃO PAULO - CDHU, DA PROPRIEDADE DAS ÁREAS MUNICIPAIS QUE ESPECIFICA, SITUADAS EM PARAISÓPOLIS."*

### 2. Lei Ordinária N° 15689/2013

- **Classe Esperada (Ground Truth):** `Gestão Imobiliária, Habitação e Regularização Fundiária`
- **Classe Predita no Banco:** `Educação e Creches`
- **Confiança/Score de Pertinência:** `0.2282`
- **Ementa:** *"AUTORIZA A TRANSFERÊNCIA, A TÍTULO NÃO ONEROSO, À COMPANHIA DE DESENVOLVIMENTO HABITACIONAL E URBANO DO ESTADO DE SÃO PAULO - CDHU, DA PROPRIEDADE DA ÁREA MUNICIPAL QUE ESPECIFICA."*

### 3. Decreto N° 53847/2013

- **Classe Esperada (Ground Truth):** `Gestão Imobiliária, Habitação e Regularização Fundiária`
- **Classe Predita no Banco:** `Educação e Creches`
- **Confiança/Score de Pertinência:** `0.3625`
- **Ementa:** *"DECLARA DE UTILIDADE PÚBLICA, PARA DESAPROPRIAÇÃO, IMÓVEL PARTICULAR SITUADO NO DISTRITO DO RIO PEQUENO, SUBPREFEITURA DE BUTANTÃ, NECESSÁRIO À IMPLANTAÇÃO DE CENTRO DE EDUCAÇÃO INFANTIL."*

### 4. Decreto N° 53848/2013

- **Classe Esperada (Ground Truth):** `Gestão Imobiliária, Habitação e Regularização Fundiária`
- **Classe Predita no Banco:** `Denominação de Vias e Logradouros`
- **Confiança/Score de Pertinência:** `0.2463`
- **Ementa:** *"DECLARA DE UTILIDADE PÚBLICA, PARA DESAPROPRIAÇÃO, IMÓVEL PARTICULAR SITUADO NO DISTRITO DE PEDREIRA, SUBPREFEITURA DE CIDADE ADEMAR, NECESSÁRIO À IMPLANTAÇÃO DE CENTRO DE EDUCAÇÃO INFANTIL."*

### 5. Decreto N° 53849/2013

- **Classe Esperada (Ground Truth):** `Gestão Imobiliária, Habitação e Regularização Fundiária`
- **Classe Predita no Banco:** `Datas Comemorativas e Calendário`
- **Confiança/Score de Pertinência:** `0.2815`
- **Ementa:** *"DECLARA DE UTILIDADE PÚBLICA, PARA DESAPROPRIAÇÃO, IMÓVEL PARTICULAR SITUADO NO DISTRITO DO GRAJAÚ, SUBPREFEITURA DE CAPELA DO SOCORRO, NECESSÁRIO À IMPLANTAÇÃO DE CENTRO DE EDUCAÇÃO INFANTIL."*

### 6. Decreto N° 53855/2013

- **Classe Esperada (Ground Truth):** `Gestão Imobiliária, Habitação e Regularização Fundiária`
- **Classe Predita no Banco:** `Denominação de Vias e Logradouros`
- **Confiança/Score de Pertinência:** `0.2588`
- **Ementa:** *"DECLARA DE UTILIDADE PÚBLICA, PARA DESAPROPRIAÇÃO, IMÓVEL PARTICULAR SITUADO NO DISTRITO DE SAPOPEMBA, SUBPREFEITURA DE VILA PRUDENTE/SAPOPEMBA, NECESSÁRIO À IMPLANTAÇÃO DE CENTRO DE EDUCAÇÃO INFANTIL."*

### 7. Lei Ordinária N° 15728/2013

- **Classe Esperada (Ground Truth):** `Gestão Imobiliária, Habitação e Regularização Fundiária`
- **Classe Predita no Banco:** `Serviços Públicos e Gestão Municipal`
- **Confiança/Score de Pertinência:** `0.5719`
- **Ementa:** *"AUTORIZA A TRANSFERÊNCIA, A TÍTULO NÃO ONEROSO, À COMPANHIA METROPOLITANA DE HABITAÇÃO DE SÃO PAULO - COHAB-SP, DA PROPRIEDADE DE IMÓVEIS MUNICIPAIS QUE INTEGRARÃO O FUNDO MUNICIPAL DE HABITAÇÃO, E DÁ PROVIDÊNCIAS CORRELATAS."*

### 8. Lei Ordinária N° 15729/2013

- **Classe Esperada (Ground Truth):** `Gestão Imobiliária, Habitação e Regularização Fundiária`
- **Classe Predita no Banco:** `Datas Comemorativas e Calendário`
- **Confiança/Score de Pertinência:** `0.1957`
- **Ementa:** *"AUTORIZA A TRANSFERÊNCIA, A TÍTULO NÃO ONEROSO, À COMPANHIA METROPOLITANA DE HABITAÇÃO DE SÃO PAULO - COHAB-SP, DA PROPRIEDADE DE IMÓVEIS MUNICIPAIS INTEGRANTES DO CONJUNTO HABITACIONAL SÍTIO CASA BLANCA, E DÁ PROVIDÊNCIAS CORRELATAS."*

### 9. Decreto N° 53892/2013

- **Classe Esperada (Ground Truth):** `Gestão Imobiliária, Habitação e Regularização Fundiária`
- **Classe Predita no Banco:** `Datas Comemorativas e Calendário`
- **Confiança/Score de Pertinência:** `0.2439`
- **Ementa:** *"DECLARA DE UTILIDADE PÚBLICA, PARA DESAPROPRIAÇÃO, IMÓVEL PARTICULAR SITUADO NO DISTRITO DE VILA BRASILÂNDIA, SUBPREFEITURA DE FREGUESIA/BRASILÂNDIA, NECESSÁRIO À IMPLANTAÇÃO DE HOSPITAL MUNICIPAL."*

### 10. Decreto N° 53908/2013

- **Classe Esperada (Ground Truth):** `Gestão Imobiliária, Habitação e Regularização Fundiária`
- **Classe Predita no Banco:** `Educação e Creches`
- **Confiança/Score de Pertinência:** `0.2354`
- **Ementa:** *"DECLARA DE INTERESSE SOCIAL, PARA DESAPROPRIAÇÃO, IMÓVEL PARTICULAR SITUADO NO DISTRITO DE SACOMÃ, SUBPREFEITURA DE IPIRANGA, NECESSÁRIO À IMPLANTAÇÃO DE NÚCLEO HABITACIONAL."*

### 11. Decreto N° 53910/2013

- **Classe Esperada (Ground Truth):** `Gestão Imobiliária, Habitação e Regularização Fundiária`
- **Classe Predita no Banco:** `Crédito Orçamentário e Finanças`
- **Confiança/Score de Pertinência:** `0.2791`
- **Ementa:** *"DECLARA DE INTERESSE SOCIAL, PARA DESAPROPRIAÇÃO, IMÓVEL PARTICULAR SITUADO NO DISTRITO DA PONTE RASA, SUBPREFEITURA DE ERMELINO MATARAZZO, NECESSÁRIO À IMPLANTAÇÃO DE NÚCLEO HABITACIONAL."*

### 12. Decreto N° 53906/2013

- **Classe Esperada (Ground Truth):** `Gestão Imobiliária, Habitação e Regularização Fundiária`
- **Classe Predita no Banco:** `Declaração de Utilidade Pública`
- **Confiança/Score de Pertinência:** `0.2342`
- **Ementa:** *"DECLARA DE INTERESSE SOCIAL, PARA DESAPROPRIAÇÃO, IMÓVEL PARTICULAR SITUADO NO DISTRITO DO CAPÃO REDONDO, SUBPREFEITURA DE CAMPO LIMPO, NECESSÁRIO À IMPLANTAÇÃO DE NÚCLEO HABITACIONAL."*

### 13. Decreto N° 53909/2013

- **Classe Esperada (Ground Truth):** `Gestão Imobiliária, Habitação e Regularização Fundiária`
- **Classe Predita no Banco:** `Serviços Públicos e Gestão Municipal`
- **Confiança/Score de Pertinência:** `0.2717`
- **Ementa:** *"DECLARA DE INTERESSE SOCIAL, PARA DESAPROPRIAÇÃO, IMÓVEL PARTICULAR SITUADO NO DISTRITO DE JARDIM HELENA, SUBPREFEITURA DE SÃO MIGUEL, NECESSÁRIO À IMPLANTAÇÃO DE NÚCLEO HABITACIONAL."*

### 14. Decreto N° 53905/2013

- **Classe Esperada (Ground Truth):** `Gestão Imobiliária, Habitação e Regularização Fundiária`
- **Classe Predita no Banco:** `Declaração de Utilidade Pública`
- **Confiança/Score de Pertinência:** `0.2342`
- **Ementa:** *"DECLARA DE INTERESSE SOCIAL, PARA DESAPROPRIAÇÃO, IMÓVEL PARTICULAR SITUADO NO DISTRITO DE VILA ANDRADE, SUBPREFEITURA DE CAMPO LIMPO, NECESSÁRIO À IMPLANTAÇÃO DE NÚCLEO HABITACIONAL."*

### 15. Decreto N° 53912/2013

- **Classe Esperada (Ground Truth):** `Gestão Imobiliária, Habitação e Regularização Fundiária`
- **Classe Predita no Banco:** `Educação e Creches`
- **Confiança/Score de Pertinência:** `0.2358`
- **Ementa:** *"DECLARA DE INTERESSE SOCIAL, PARA DESAPROPRIAÇÃO, IMÓVEL PARTICULAR SITUADO NO DISTRITO DO PARQUE DO CARMO, SUBPREFEITURA DE ITAQUERA, NECESSÁRIO À IMPLANTAÇÃO DE NÚCLEO HABITACIONAL."*

