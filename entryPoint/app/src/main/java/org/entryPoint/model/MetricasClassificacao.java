package org.entryPoint.model;

import java.util.Map;

public record MetricasClassificacao(
  double acuracia,
  double f1ScoreMacro,
  double f1ScoreWeighted,
  double precisaoMacro,
  double revocacaoMacro,
  long totalTokensProcessados,
  long totalTokensEconomizadosPreProcessamento,
  double percentualEconomiaTokens,
  long tempoProcessamentoMs,
  double throughputNormasPorSegundo,
  long memoriaUtilizadaMb,
  int[][] matrizConfusao,
  Map<String, RelatorioClasse> desempenhoPorClasse
) {
  public record RelatorioClasse(
    String nomeClasse,
    double precisao,
    double revocacao,
    double f1Score,
    long suporte
  ) {}
}
