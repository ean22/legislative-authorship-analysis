package org.entryPoint.benchmark.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Representa a definição oficial e reproduzível do melhor pipeline selecionado pelo benchmark.
 * Fixa as escolhas de redução de texto inteligente, chunking, representação semântica e modelagem de tópicos
 * com base em métricas reais, permitindo o prosseguimento seguro para as próximas etapas do sistema.
 */
public record DefinicaoPipeline(
    long id,
    String nome,
    TipoReducaoTextual reducao,
    TipoChunking chunking,
    TipoRepresentacaoSemantica representacao,
    TipoAlgoritmoTopicModeling algoritmo,
    int k,
    double scoreGlobal,
    double coerenciaUmass,
    double diversidadeTopicos,
    double estabilidadeJaccard,
    double utilidadeAutoral,
    String justificativaEscolha,
    LocalDateTime dataDefinicao,
    boolean ativo
) {

  public static DefinicaoPipeline aPartirDeResultado(ResultadoExperimento r) {
    return new DefinicaoPipeline(
        1L,
        r.configuracao().nome(),
        r.configuracao().reducao(),
        r.configuracao().chunking(),
        r.configuracao().representacao(),
        r.configuracao().algoritmo(),
        r.configuracao().k(),
        r.metricas().scoreCompostoGlobal(),
        r.metricas().coerenciaUmass(),
        r.metricas().diversidadeTopicos(),
        r.metricas().estabilidadeJaccard(),
        r.metricas().utilidadeTematicaAutoral(),
        r.justificativaEscolha(),
        LocalDateTime.now(),
        true
    );
  }

  public String toJson() {
    DateTimeFormatter dtf = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    return String.format("""
        {
          "nome": "%s",
          "reducao_textual": "%s",
          "chunking": "%s",
          "representacao_semantica": "%s",
          "algoritmo_topicos": "%s",
          "k_topicos": %d,
          "score_global": %.4f,
          "metricas": {
            "coerencia_umass": %.4f,
            "diversidade_topicos": %.4f,
            "estabilidade_jaccard": %.4f,
            "utilidade_autoral": %.4f
          },
          "justificativa_escolha": "%s",
          "data_definicao": "%s",
          "ativo": %b
        }
        """,
        nome,
        reducao.name(),
        chunking.name(),
        representacao.name(),
        algoritmo.name(),
        k,
        scoreGlobal,
        coerenciaUmass,
        diversidadeTopicos,
        estabilidadeJaccard,
        utilidadeAutoral,
        justificativaEscolha.replace("\"", "\\\""),
        dataDefinicao != null ? dataDefinicao.format(dtf) : "",
        ativo
    );
  }

  public String getFormatacaoVisual() {
    return String.format("""
        ========================================================================================
        PIPELINE ATIVO DEFINIDO PARA PROSSEGUIMENTO:
        ========================================================================================
        • Configuração:            %s
        • Redução Textual:         %s (%s)
        • Segmentação (Chunking):  %s
        • Representação Semântica: %s
        • Algoritmo de Tópicos:    %s com K=%d tópicos
        • Score Composto Real:     %.3f (Coerência: %.2f | Diversidade: %.2f | Estabilidade: %.2f)
        • Justificativa da Escolha:
          %s
        ========================================================================================
        """,
        nome,
        reducao.name(), reducao.getDescricao(),
        chunking.name(),
        representacao.getDescricao(),
        algoritmo.getDescricao(), k,
        scoreGlobal, coerenciaUmass, diversidadeTopicos, estabilidadeJaccard,
        justificativaEscolha
    );
  }
}
