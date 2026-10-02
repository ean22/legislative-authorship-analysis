package org.entryPoint.model;

import java.util.Arrays;

public record EmbeddingNorma(
    long idNorma,
    float[] vetor,
    String ementa,
    String titulo,
    int numero,
    int ano,
    String tipoEscrito
) {
  @Override
  public String toString() {
    return "EmbeddingNorma{" +
        "idNorma=" + idNorma +
        ", dim=" + (vetor != null ? vetor.length : 0) +
        ", tipoEscrito='" + tipoEscrito + '\'' +
        ", numero=" + numero +
        ", ano=" + ano +
        '}';
  }
}
