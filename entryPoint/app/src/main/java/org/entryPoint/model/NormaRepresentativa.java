package org.entryPoint.model;

public record NormaRepresentativa(
    long idNorma,
    int numero,
    int ano,
    String tipoEscrito,
    String titulo,
    String ementa,
    double similaridadeComCentroide,
    double distanciaCentroide
) {
  public String getIdentificacaoFormatada() {
    String tipo = tipoEscrito != null ? tipoEscrito : "Norma";
    String num = numero > 0 ? " nº " + numero : "";
    String a = ano > 0 ? "/" + ano : "";
    return String.format("%s%s%s (ID %d)", tipo, num, a, idNorma);
  }
}
