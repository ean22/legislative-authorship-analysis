package org.entryPoint.benchmark.model;

/**
 * Define de forma declarativa e reproduzível uma configuração de experimento no benchmark.
 */
public record ConfiguracaoExperimento(
    String id,
    String nome,
    TipoReducaoTextual reducao,
    TipoChunking chunking,
    TipoRepresentacaoSemantica representacao,
    TipoAlgoritmoTopicModeling algoritmo,
    int k,
    int maxIteracoes,
    long seed,
    boolean isBaseline
) {

  public static ConfiguracaoExperimento criarBaseline() {
    return new ConfiguracaoExperimento(
        "baseline_nmf_k12",
        "Baseline: TF-IDF + NMF (K=12)",
        TipoReducaoTextual.STOPWORDS_TFIDF,
        TipoChunking.DOCUMENT_LEVEL,
        TipoRepresentacaoSemantica.SPARSE_TFIDF,
        TipoAlgoritmoTopicModeling.NMF,
        12,
        25,
        42L,
        true
    );
  }

  public String getSumarioCurto() {
    return String.format("[%s | %s | %s | K=%d]",
        reducao.name(), representacao.name(), algoritmo.name(), k);
  }
}
