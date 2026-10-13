package org.entryPoint.benchmark.model;

/**
 * Algoritmos de topic modeling avaliados no benchmark.
 * Não assume que NMF é a melhor solução, comparando-o contra LDA
 * e Topic Modeling baseado em Clustering Vetorial + c-TF-IDF.
 */
public enum TipoAlgoritmoTopicModeling {
  NMF("Fatorização de Matrizes Não-Negativas (NMF)"),
  LDA("Alocação Latente de Dirichlet (LDA)"),
  CLUSTER_C_TFIDF("Clustering Semântico + c-TF-IDF (Estilo BERTopic)");

  private final String descricao;

  TipoAlgoritmoTopicModeling(String descricao) {
    this.descricao = descricao;
  }

  public String getDescricao() {
    return descricao;
  }
}
