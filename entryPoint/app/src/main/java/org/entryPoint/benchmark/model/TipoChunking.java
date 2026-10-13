package org.entryPoint.benchmark.model;

/**
 * Define como os textos longos e dispositivos legais são particionados.
 * Não deve ser confundido com redução de informação ou clustering.
 */
public enum TipoChunking {
  DOCUMENT_LEVEL("Documento Integral (Nível de Ementa/Norma)"),
  SENTENCE_LEVEL("Segmentação por Sentenças/Períodos");

  private final String descricao;

  TipoChunking(String descricao) {
    this.descricao = descricao;
  }

  public String getDescricao() {
    return descricao;
  }
}
