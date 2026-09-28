package org.entryPoint.model;

import java.util.Map;

public record PerfilAutor(
  long idAutor,
  String nomeAutor,
  String cargoNormalizado,
  long totalNormas,
  long totalNormasClassificadas,
  Map<Integer, Long> contagemPorTopico,
  Map<Integer, Double> distribuicaoPercentual,
  double entropiaEspecializacao,
  String topicoPredominante
) {
  /**
   * Entropia normalizada de Shannon (0.0 = ultra especializado em 1 tema, 1.0 = perfeitamente generalista).
   */
  public String getClassificacaoAtuacao() {
    if (entropiaEspecializacao < 0.35) {
      return "Altamente Especializado / Focado";
    } else if (entropiaEspecializacao < 0.70) {
      return "Moderadamente Temático";
    } else {
      return "Generalista / Multidisciplinar";
    }
  }
}
