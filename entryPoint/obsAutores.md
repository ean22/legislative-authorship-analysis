# Experimentos de extração de autores

Banco avaliado: `data/leis.db`, com 14.374 normas e 14.374 integrais HTML.

| Rodada | Estratégia | Com autoria | Sem autoria | Cobertura | Autores | Relações | Suspeitos |
|---|---|---:|---:|---:|---:|---:|---:|
| Base | Marcadores fixos de Prefeitura e formato `nome, cargo` | 4.728 | 9.646 | 32,89% | 231 | 17.135 | existentes na base |
| 1 | Marcadores alternativos/fallback ao fim do documento + filtro de cargos | 9.250 | 5.124 | 64,36% | 323 | 31.048 | 0 nomes óbvios |
| 2 | Pareamento de nome e cargo em linhas consecutivas | 13.186 | 1.188 | 91,74% | 402 | 43.609 | cargos concatenados longos |
| 3 escolhida | Câmara Municipal + nomes com pelo menos dois tokens + cargo ate 120 caracteres | 13.946 | 428 | 97,02% | 386 | 44.275 | 0 nos filtros finais |
| 4 descartada | Aceitar nome terminado em virgula antes do cargo | 13.792 | 582 | 95,95% | 380 | 43.671 | 0 nos filtros finais |

## Estratégia escolhida

- Procura marcadores de assinatura da Prefeitura e da Câmara Municipal.
- Aceita marcadores finais específicos; quando ausentes, usa o fim do texto.
- Aceita os formatos `nome, cargo` e nome/cargo em linhas consecutivas.
- Exige nome com pelo menos dois tokens alfabeticos e no maximo dez.
- Exige cargo com vocabulario de funcao publica, sem numeros, HTML ou URL, limitado a 120 caracteres.
- Deduplica candidatos dentro da norma; o schema tambem impede duplicidades em `autores` e `norma_autor`.

## Auditoria final

- `norma_autor` duplicado: 0.
- Nome suspeito por tamanho, numero, virgula, HTML ou URL: 0.
- Cargo suspeito por tamanho, numero, HTML ou URL: 0.
- Exemplos recuperados: `FERNANDO HADDAD, PREFEITO`; `MARIANNA SAMPAIO, SECRETARIA MUNICIPAL DOS NEGOCIOS JURIDICOS - SUBSTITUTA`; `KAREN LIMA VIEIRA, SECRETARIA GERAL PARLAMENTAR`.

As 428 normas restantes concentram-se principalmente em decretos antigos, sobretudo de 2015 e 2017. A amostragem mostrou documentos sem assinatura, HTML com assinaturas concatenadas e formatos sem marcadores reconhecidos; esses casos nao receberam fallback mais permissivo por risco de inserir texto legislativo como autor.
