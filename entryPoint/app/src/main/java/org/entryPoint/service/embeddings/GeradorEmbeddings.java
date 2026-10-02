package org.entryPoint.service.embeddings;

import java.util.List;

public interface GeradorEmbeddings {
  /**
   * Gera o vetor de embedding para um único texto.
   */
  float[] gerarEmbedding(String texto) throws Exception;

  /**
   * Gera vetores de embedding para uma lista de textos em lote.
   */
  List<float[]> gerarEmbeddingsEmLote(List<String> textos) throws Exception;

  /**
   * Retorna o nome identificador do gerador/modelo.
   */
  String getNome();

  /**
   * Retorna a dimensionalidade do vetor gerado.
   */
  int getDimensao();

  /**
   * Retorna se o gerador está pronto para uso (ex: credenciais configuradas).
   */
  boolean isDisponivel();
}
