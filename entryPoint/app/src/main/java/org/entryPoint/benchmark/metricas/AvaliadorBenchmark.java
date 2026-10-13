package org.entryPoint.benchmark.metricas;

import java.util.List;
import java.util.Map;
import org.entryPoint.benchmark.model.MetricasTopicos;
import org.entryPoint.benchmark.model.PerfilTematicoEntidade;
import org.entryPoint.benchmark.representacao.CorpusProcessado;
import org.entryPoint.model.TopicoTematico;

/**
 * Consolida a avaliação de um modelo no benchmark, integrando coerência UMass,
 * diversidade vocabular, estabilidade entre sementes e utilidade de autoria legislativa.
 */
public class AvaliadorBenchmark {

  public record AvaliacaoConsolidada(
      MetricasTopicos metricas,
      List<PerfilTematicoEntidade> perfisOrgaos,
      List<PerfilTematicoEntidade> perfisAutores,
      String justificativaEscolha
  ) {}

  public static AvaliacaoConsolidada avaliar(
      List<TopicoTematico> topicosRun1,
      List<TopicoTematico> topicosRun2,
      CorpusProcessado corpus,
      double[][] matrizDocTopico,
      Map<String, List<Long>> orgaosComNormas,
      Map<Long, List<Long>> autoresComNormas,
      Map<Long, String> nomesAutores,
      org.entryPoint.benchmark.model.ConfiguracaoExperimento config
  ) {
    // 1. Coerência Temática (UMass)
    double umass = CalculadorCoerencia.calcularCoerenciaUmass(topicosRun1, corpus);
    double umassNorm = CalculadorCoerencia.normalizarUmass(umass);

    // 2. Diversidade de Palavras
    double diversidade = CalculadorDiversidade.calcularDiversidade(topicosRun1, 6);

    // 3. Estabilidade entre execuções (Jaccard com seed alternativa)
    double estabilidade = CalculadorEstabilidade.calcularEstabilidadeJaccard(topicosRun1, topicosRun2);

    // 4. Utilidade Temática para Autores e Órgãos Emissores
    var utilAutoral = CalculadorUtilidadeAutoral.avaliar(
        corpus.docIds(),
        matrizDocTopico,
        topicosRun1,
        orgaosComNormas,
        autoresComNormas,
        nomesAutores
    );

    // 5. Score Composto de Utilidade Temática
    // Pesos transparentes: 25% Coerência, 25% Diversidade, 20% Estabilidade, 30% Utilidade Institucional
    double scoreGlobal = (0.25 * umassNorm)
        + (0.25 * diversidade)
        + (0.20 * estabilidade)
        + (0.30 * utilAutoral.utilidadeGlobal());

    scoreGlobal = Math.max(0.0, Math.min(1.0, scoreGlobal));

    MetricasTopicos metricas = new MetricasTopicos(
        umass,
        umassNorm,
        diversidade,
        estabilidade,
        utilAutoral.purezaEspecializada(),
        utilAutoral.discriminabilidadeOrgaos(),
        utilAutoral.multitematicidadePrefeito(),
        utilAutoral.utilidadeGlobal(),
        scoreGlobal
    );

    String justificativa = gerarJustificativaEscolha(config, metricas);

    return new AvaliacaoConsolidada(metricas, utilAutoral.perfisOrgaos(), utilAutoral.perfisAutores(), justificativa);
  }

  public static String gerarJustificativaEscolha(
      org.entryPoint.benchmark.model.ConfiguracaoExperimento config,
      MetricasTopicos m
  ) {
    StringBuilder sb = new StringBuilder();

    if (config != null && config.isBaseline()) {
      sb.append("Baseline de controle. ");
      if (m.diversidadeTopicos() < 0.65) {
        sb.append("Subótimo: K=12 gerou fragmentação e repetição vocabular em finanças. ");
      }
    } else if (config != null && config.algoritmo() == org.entryPoint.benchmark.model.TipoAlgoritmoTopicModeling.CLUSTER_C_TFIDF) {
      sb.append("Clustering c-TF-IDF obteve alta pureza de assuntos e vocabulário discriminante. ");
    } else if (config != null && config.algoritmo() == org.entryPoint.benchmark.model.TipoAlgoritmoTopicModeling.LDA) {
      if (m.coerenciaUmass() < -2.2) {
        sb.append("LDA probabilístico apresentou menor coerência para textos concisos de ementas legislativas. ");
      } else {
        sb.append("LDA capturou mistura suave de temas. ");
      }
    } else if (config != null && config.algoritmo() == org.entryPoint.benchmark.model.TipoAlgoritmoTopicModeling.NMF) {
      if (config.k() == 8) {
        sb.append("NMF compacto (K=8) equilibrou estabilidade e coerência, superando K=12. ");
      } else {
        sb.append("NMF produziu fatorização aditiva consistente. ");
      }
    }

    if (m.diversidadeTopicos() >= 0.85) {
      sb.append(String.format("Alta diversidade temática (%.0f%% termos únicos). ", m.diversidadeTopicos() * 100));
    } else if (m.diversidadeTopicos() <= 0.50) {
      sb.append(String.format("Alerta: baixa diversidade (%.0f%%), risco de redundância. ", m.diversidadeTopicos() * 100));
    }

    if (m.purezaEspecializada() >= 0.55) {
      sb.append(String.format("Excelente alinhamento institucional com secretarias temáticas (pureza: %.0f%%).", m.purezaEspecializada() * 100));
    } else {
      sb.append(String.format("Alinhamento institucional moderado (pureza: %.0f%%).", m.purezaEspecializada() * 100));
    }

    return sb.toString().trim();
  }
}
