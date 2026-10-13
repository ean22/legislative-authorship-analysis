package org.entryPoint.benchmark.model;

/**
 * Representa as técnicas de redução textual e seleção de informação.
 * Diferencia explicitamente a redução textual (seleção de termos ou sentenças relevantes)
 * da representação semântica (vetorização/embeddings) e do agrupamento (clustering).
 */
public enum TipoReducaoTextual {
  RAW_EMENTA("Texto Integral da Ementa", "n.ementa", true),
  TFIDF_TOPK("TF-IDF Top-K", "e.tfidf_topk", false),
  TEXTRANK("TextRank (Baseado em Grafo)", "e.textrank", false),
  MMR("Maximal Marginal Relevance (MMR)", "e.tfidf_mmr", false),
  CHI_SQUARE("Seleção Qui-Quadrado", "e.chi_square_termos", false),
  STOPWORDS_TFIDF("Stopwords Legislativas + TF-IDF", "e.stopwords_tfidf_topk", false),
  RESUMO_EXTRATIVO("Resumo Extrativo TF-IDF", "e.resumo_extrativo_tfidf", false),
  CHUNKING_SENTENCAS("Chunking Sentencial / Dispositivos", "e.tfidf_posicao_sentenca", false),
  EMBEDDINGS_SVD("Embeddings Espaço Latente (SVD/LSA)", "e.svd_lsa", false),
  CLUSTERING_SEMANTICO("Clustering Semântico de Sentenças", "e.clustering_sentencas", false);

  private final String descricao;
  private final String colunaSql;
  private final boolean vemDaTabelaNormas;

  TipoReducaoTextual(String descricao, String colunaSql, boolean vemDaTabelaNormas) {
    this.descricao = descricao;
    this.colunaSql = colunaSql;
    this.vemDaTabelaNormas = vemDaTabelaNormas;
  }

  public String getDescricao() {
    return descricao;
  }

  public String getColunaSql() {
    return colunaSql;
  }

  public boolean isVemDaTabelaNormas() {
    return vemDaTabelaNormas;
  }
}
