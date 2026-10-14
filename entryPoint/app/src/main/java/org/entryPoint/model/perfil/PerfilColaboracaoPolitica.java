package org.entryPoint.model.perfil;

import java.util.List;
import java.util.Map;

/**
 * Perfil de Colaboração Política: reflete os laços de coautoria do autor com seus pares,
 * o grau de interdependência institucional e os órgãos/secretarias com quem mais articula.
 */
public record PerfilColaboracaoPolitica(
    int totalParceirosDistintos,
    List<ParceriaPolitica> principaisParcerias,
    Map<String, Integer> parceriasPorCargo,
    double intensidadeMediaColaboracao
) {

  public String classificarDensidadeRede() {
    if (totalParceirosDistintos >= 35) {
      return "Hub Político / Articulador Central (Ampla transversalidade)";
    } else if (totalParceirosDistintos >= 12) {
      return "Articulação Moderada / Coautoria Departamental";
    } else if (totalParceirosDistintos >= 1) {
      return "Rede Compacta / Parcerias Específicas";
    } else {
      return "Isolado (Sem histórico de coautoria no acervo)";
    }
  }

  public String getOrgaoParceiroMaisFrequente() {
    if (parceriasPorCargo == null || parceriasPorCargo.isEmpty()) {
      return "N/A";
    }
    return parceriasPorCargo.entrySet().stream()
        .max(Map.Entry.comparingByValue())
        .map(Map.Entry::getKey)
        .orElse("N/A");
  }
}
