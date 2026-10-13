package org.entryPoint.benchmark.model;

/**
 * Representa como o texto (reduzido ou integral) é mapeado no espaço vetorial.
 * Separa a representação semântica (esparsa vs densa vs espaço latente)
 * da seleção prévia de texto e do particionamento (chunking).
 */
public enum TipoRepresentacaoSemantica {
  SPARSE_TFIDF("TF-IDF Esparso (Saco de Palavras Ponderado)"),
  DENSE_SVD("Espaço Latente Reduzido (SVD / LSA Contínuo)"),
  DENSE_EMBEDDING("Embeddings Densos (Centroide Semântico)");

  private final String descricao;

  TipoRepresentacaoSemantica(String descricao) {
    this.descricao = descricao;
  }

  public String getDescricao() {
    return descricao;
  }
}
