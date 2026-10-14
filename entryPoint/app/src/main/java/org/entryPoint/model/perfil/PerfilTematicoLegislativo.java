package org.entryPoint.model.perfil;

import java.util.List;

/**
 * Perfil Temático Legislativo: captura os temas prioritários de um autor legislativo,
 * calculando sua distribuição percentual, grau de especialização (HHI) e entropia tematica.
 */
public record PerfilTematicoLegislativo(
    int totalNormasComTopico,
    List<DistribuicaoTopico> distribuicaoTopicos,
    String topicoDominante,
    double percentualTopicoDominante,
    double indiceEspecializacaoHhi,
    double entropiaTematica,
    List<String> termosMaisFrequentes
) {

  /**
   * Classifica a atuação temática com base no índice HHI (0 a 1).
   */
  public String classificarNivelEspecializacao() {
    if (indiceEspecializacaoHhi >= 0.45) {
      return "Altamente Especializado (Monotemático / Pauta Concentrada)";
    } else if (indiceEspecializacaoHhi >= 0.25) {
      return "Moderadamente Especializado (Foco em 2 ou 3 Eixos Centrais)";
    } else {
      return "Diversificado / Generalista (Amplo Espectro de Políticas Públicas)";
    }
  }

  public String classificarEntropia() {
    if (entropiaTematica <= 0.40) {
      return "Baixa Dispersão (Atuação Focada)";
    } else if (entropiaTematica <= 0.75) {
      return "Dispersão Média (Atuação Balanceada)";
    } else {
      return "Alta Dispersão (Polivalente / Transversal)";
    }
  }
}
