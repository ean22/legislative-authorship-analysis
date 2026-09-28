package org.entryPoint.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.entryPoint.model.MetricasClassificacao;
import org.entryPoint.model.NormaTopico;
import org.entryPoint.repository.RepositorioBancoDados;
import org.entryPoint.repository.RepositorioNormas;
import org.entryPoint.repository.RepositorioTopicos;

public class ServicoAvaliacaoClassificacao {

  private final RepositorioNormas repositorioNormas;
  private final RepositorioTopicos repositorioTopicos;

  public ServicoAvaliacaoClassificacao() {
    this.repositorioNormas = new RepositorioNormas();
    this.repositorioTopicos = new RepositorioTopicos();
  }

  public ServicoAvaliacaoClassificacao(RepositorioNormas repositorioNormas, RepositorioTopicos repositorioTopicos) {
    this.repositorioNormas = repositorioNormas;
    this.repositorioTopicos = repositorioTopicos;
  }

  public ServicoAvaliacaoClassificacao(RepositorioBancoDados repositorio) {
    this.repositorioNormas = repositorio.getRepositorioNormas();
    this.repositorioTopicos = repositorio.getRepositorioTopicos();
  }

  /**
   * Avalia a classificação gerada comparando com um conjunto de validação temático
   * baseado em padrões sintático-semânticos das ementas originais (Ground Truth Heurístico / Silver Standard).
   */
  public MetricasClassificacao avaliarDesempenho(long tempoExecucaoMs, long totalTokensProcessados, long memoriaUtilizadaMb) throws SQLException {
    List<NormaTopico> classificacoes = repositorioTopicos.listarNormasTopicos();
    if (classificacoes.isEmpty()) {
      System.out.println("Nenhuma classificação disponível na tabela 'norma_topicos'.");
      return null;
    }

    Map<Long, String> ementasOriginais = repositorioNormas.listarEmentasOriginais();

    // 1. Extrair Ground Truth de referência das ementas originais
    List<String> classesReais = new ArrayList<>();
    List<String> classesPreditas = new ArrayList<>();

    for (NormaTopico nt : classificacoes) {
      String ementa = ementasOriginais.get(nt.idNorma());
      if (ementa == null || ementa.isBlank()) continue;

      String classeReal = inferirClasseReferencia(ementa);
      if (classeReal != null) {
        classesReais.add(classeReal);
        classesPreditas.add(nt.rotuloTopico());
      }
    }

    // 2. Coletar todas as classes únicas
    Set<String> todasClasses = new TreeSet<>();
    todasClasses.addAll(classesReais);
    todasClasses.addAll(classesPreditas);
    List<String> listaClasses = new ArrayList<>(todasClasses);
    int numClasses = listaClasses.size();

    Map<String, Integer> classToIndex = new HashMap<>();
    for (int i = 0; i < numClasses; i++) {
      classToIndex.put(listaClasses.get(i), i);
    }

    // 3. Matriz de Confusão (Linhas = Reais, Colunas = Preditas)
    int[][] matrizConfusao = new int[numClasses][numClasses];
    int totalInstancias = classesReais.size();
    int totalAcertos = 0;

    for (int i = 0; i < totalInstancias; i++) {
      int idxReal = classToIndex.get(classesReais.get(i));
      int idxPred = classToIndex.get(classesPreditas.get(i));
      matrizConfusao[idxReal][idxPred]++;
      if (idxReal == idxPred) {
        totalAcertos++;
      }
    }

    double acuracia = totalInstancias > 0 ? (double) totalAcertos / totalInstancias : 0.0;

    // 4. Métricas por classe (Precision, Recall, F1, Support)
    Map<String, MetricasClassificacao.RelatorioClasse> desempenhoClasses = new LinkedHashMap<>();
    double somaPrecisao = 0.0;
    double somaRevocacao = 0.0;
    double somaF1Macro = 0.0;
    double somaF1Weighted = 0.0;

    for (int i = 0; i < numClasses; i++) {
      String nomeClasse = listaClasses.get(i);
      int tp = matrizConfusao[i][i];

      // Total predito como esta classe (soma coluna i)
      int somaColuna = 0;
      for (int r = 0; r < numClasses; r++) {
        somaColuna += matrizConfusao[r][i];
      }

      // Total real desta classe (soma linha i)
      int somaLinha = 0;
      for (int c = 0; c < numClasses; c++) {
        somaLinha += matrizConfusao[i][c];
      }

      double precision = somaColuna > 0 ? (double) tp / somaColuna : 0.0;
      double recall = somaLinha > 0 ? (double) tp / somaLinha : 0.0;
      double f1 = (precision + recall > 0) ? 2.0 * (precision * recall) / (precision + recall) : 0.0;

      somaPrecisao += precision;
      somaRevocacao += recall;
      somaF1Macro += f1;
      somaF1Weighted += f1 * somaLinha;

      desempenhoClasses.put(nomeClasse, new MetricasClassificacao.RelatorioClasse(
        nomeClasse, precision, recall, f1, somaLinha
      ));
    }

    double precisaoMacro = numClasses > 0 ? somaPrecisao / numClasses : 0.0;
    double revocacaoMacro = numClasses > 0 ? somaRevocacao / numClasses : 0.0;
    double f1Macro = numClasses > 0 ? somaF1Macro / numClasses : 0.0;
    double f1Weighted = totalInstancias > 0 ? somaF1Weighted / totalInstancias : 0.0;

    // Estimativa de tokens economizados pelo pré-processamento
    long tokensOriginaisEstimados = (long) (classificacoes.size() * 32); // média 32 tokens por ementa original
    long tokensEconomizados = Math.max(0, tokensOriginaisEstimados - totalTokensProcessados);
    double pctEconomia = tokensOriginaisEstimados > 0 ? (double) tokensEconomizados / tokensOriginaisEstimados * 100.0 : 0.0;
    double throughput = tempoExecucaoMs > 0 ? (double) classificacoes.size() / (tempoExecucaoMs / 1000.0) : 0.0;

    return new MetricasClassificacao(
      acuracia,
      f1Macro,
      f1Weighted,
      precisaoMacro,
      revocacaoMacro,
      totalTokensProcessados,
      tokensEconomizados,
      pctEconomia,
      tempoExecucaoMs,
      throughput,
      memoriaUtilizadaMb,
      matrizConfusao,
      desempenhoClasses
    );
  }

  /**
   * Imprime no terminal o relatório completo de avaliação de NLP e Desempenho Computacional.
   */
  public void exibirRelatorioAvaliacao(MetricasClassificacao metricas) {
    if (metricas == null) return;

    System.out.println("\n" + "=".repeat(85));
    System.out.println("=== RELATÓRIO DE DESEMPENHO E AVALIAÇÃO DA CLASSIFICAÇÃO ===");
    System.out.println("=".repeat(85));

    System.out.printf("📊 Acurácia Geral:      %6.2f%%%n", metricas.acuracia() * 100.0);
    System.out.printf("🎯 F1-Score (Macro):    %6.2f%%%n", metricas.f1ScoreMacro() * 100.0);
    System.out.printf("⚖️  F1-Score (Weighted): %6.2f%%%n", metricas.f1ScoreWeighted() * 100.0);
    System.out.printf("🔍 Precisão (Macro):    %6.2f%%%n", metricas.precisaoMacro() * 100.0);
    System.out.printf("📡 Revocação (Macro):   %6.2f%%%n", metricas.revocacaoMacro() * 100.0);

    System.out.println("\n" + "-".repeat(85));
    System.out.printf("%-38s | %-10s | %-10s | %-10s | %-8s%n", "Classe Temática", "Precision", "Recall", "F1-Score", "Suporte");
    System.out.println("-".repeat(85));

    for (MetricasClassificacao.RelatorioClasse rel : metricas.desempenhoPorClasse().values()) {
      System.out.printf("%-38s | %9.1f%% | %9.1f%% | %9.1f%% | %8d%n",
        rel.nomeClasse(),
        rel.precisao() * 100.0,
        rel.revocacao() * 100.0,
        rel.f1Score() * 100.0,
        rel.suporte()
      );
    }

    System.out.println("\n" + "=".repeat(85));
    System.out.println("=== MÉTRICAS DE ENGENHARIA E CUSTO COMPUTACIONAL ===");
    System.out.println("=".repeat(85));
    System.out.printf("⚡ Tempo de Processamento:       %d ms (%.2f s)%n", metricas.tempoProcessamentoMs(), metricas.tempoProcessamentoMs() / 1000.0);
    System.out.printf("🚀 Throughput de Classificação:   %.1f normas/segundo%n", metricas.throughputNormasPorSegundo());
    System.out.printf("🧠 Memória Heap Utilizada:        %d MB%n", metricas.memoriaUtilizadaMb());
    System.out.printf("🔢 Tokens Processados:           %d tokens%n", metricas.totalTokensProcessados());
    System.out.printf("💰 Economia de Tokens (NLP):      %d tokens (%.1f%% de redução)%n",
      metricas.totalTokensEconomizadosPreProcessamento(),
      metricas.percentualEconomiaTokens()
    );
  }

  private String inferirClasseReferencia(String ementa) {
    String e = ementa.toUpperCase();
    if (e.contains("CENTRO DE EDUCAÇÃO INFANTIL") || e.contains("CEI ") || e.contains("EMEI ") || e.contains("EMEF ") || e.contains("ESCOLA MUNICIPAL")) {
      return "Educação e Creches";
    } else if (e.contains("DENOMINA") || e.contains("DENOMINA-SE") || e.contains("LOGRADOURO PÚBLICO") || e.contains("PRAÇA ")) {
      return "Denominação de Vias e Logradouros";
    } else if (e.contains("CALENDÁRIO DE EVENTOS") || e.contains("CALENDARIO DE EVENTOS") || e.contains("DIA DO ") || e.contains("SEMANA DA ") || e.contains("SEMANA DO ")) {
      return "Datas Comemorativas e Calendário";
    } else if (e.contains("CRÉDITO SUPLEMENTAR") || e.contains("CREDITO SUPLEMENTAR") || e.contains("CRÉDITOS ADICIONAIS") || e.contains("ABRE O CRÉDITO")) {
      return "Orçamento e Finanças";
    } else if (e.contains("UTILIDADE PÚBLICA") || e.contains("UTILIDADE PUBLICA")) {
      return "Declaração de Utilidade Pública";
    }
    return null; // Não inclusa no subconjunto de alta certeza
  }
}
