package org.entryPoint.benchmark.model;

import java.util.Map;

/**
 * Representa o perfil temático consolidado de um autor político ou órgão emissor (secretaria/prefeitura).
 * Mapeia como a produção legislativa dessa entidade se distribui entre os tópicos descobertos.
 */
public record PerfilTematicoEntidade(
    String nome,
    String tipoEntidade, // "AUTOR" ou "ORGAO_EMISSOR"
    int totalNormas,
    Map<Integer, Double> distribuicaoTopicos,
    int topicoPrincipalId,
    String nomeTopicoPrincipal,
    double proporcaoTopicoPrincipal,
    int topicoSecundarioId,
    String nomeTopicoSecundario,
    double proporcaoTopicoSecundario,
    double entropia
) {

  public String getFormatacaoVisual() {
    StringBuilder sb = new StringBuilder();
    sb.append(String.format("%-35s [%4d normas] Dominante: %s (%.1f%%) | Secundário: %s (%.1f%%)",
        nome,
        totalNormas,
        nomeTopicoPrincipal,
        proporcaoTopicoPrincipal * 100.0,
        nomeTopicoSecundario,
        proporcaoTopicoSecundario * 100.0
    ));
    return sb.toString();
  }
}
