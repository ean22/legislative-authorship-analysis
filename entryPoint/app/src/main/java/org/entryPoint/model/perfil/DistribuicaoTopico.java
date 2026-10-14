package org.entryPoint.model.perfil;

/**
 * Representa a frequência e pertinência de um autor em um determinado tópico temático legislativo.
 */
public record DistribuicaoTopico(
    int idTopico,
    String nomeTopico,
    int quantidadeNormas,
    double percentual,
    double scoreMedioPertinencia
) {
  public String getFormatacaoLinha() {
    return String.format("#%02d %-38s | %5d normas (%5.1f%%) | Confiança: %.3f",
        idTopico, nomeTopico, quantidadeNormas, percentual, scoreMedioPertinencia);
  }
}
