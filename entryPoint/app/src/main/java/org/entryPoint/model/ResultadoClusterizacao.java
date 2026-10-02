package org.entryPoint.model;

import java.util.List;

public record ResultadoClusterizacao(
    int k,
    List<ClusterInfo> clusters,
    double inerciaTotal,
    long tempoExecucaoMs,
    int totalNormasClusterizadas
) {
  public NormaRepresentativa obterNormaMaisRepresentativaDoCluster(int idCluster) {
    return clusters.stream()
        .filter(c -> c.idCluster() == idCluster)
        .findFirst()
        .map(ClusterInfo::normaMaisRepresentativa)
        .orElse(null);
  }
}
