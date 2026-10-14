package org.entryPoint.model.perfil;

import java.util.Map;

/**
 * Perfil de Atuação Legislativa: caracteriza a produção do autor quanto aos tipos
 * normativos (Leis, Decretos, Portarias), modelo de autoria (individual vs. coletivo)
 * e os cargos exercidos.
 */
public record PerfilAtuacaoLegislativa(
    int totalNormas,
    Map<String, Integer> distribuicaoTiposNormas,
    int normasIndividuais,
    int normasColetivas,
    double taxaCoautoria,
    double mediaAutoresPorNorma,
    Map<String, Integer> cargosExercidos,
    String cargoMaisFrequente
) {

  public String classificarPadraoAutoria() {
    if (taxaCoautoria >= 85.0) {
      return "Predominantemente Colegiada / Articulação em Gabinete Conjunto";
    } else if (taxaCoautoria >= 40.0) {
      return "Mista (Atuação Individual e Coautoria Equilibradas)";
    } else {
      return "Predominantemente Individualista (Autoria Isolada)";
    }
  }

  public String getInstrumentoPredominante() {
    if (distribuicaoTiposNormas == null || distribuicaoTiposNormas.isEmpty()) {
      return "N/A";
    }
    return distribuicaoTiposNormas.entrySet().stream()
        .max(Map.Entry.comparingByValue())
        .map(Map.Entry::getKey)
        .orElse("N/A");
  }
}
