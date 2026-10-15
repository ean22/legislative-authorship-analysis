package org.entryPoint.benchmark.algoritmos;

/**
 * Estratégias disponíveis para inferir o rótulo temático de cada cluster legislativo.
 */
public enum ModoInferidorTema {
  /**
   * Fluxo unificado padrão: Envia as palavras-chave dos clusters em lote para o LLM Google Gemini
   * com raciocínio semântico contextual e fallback automático transparente para Regras Léxicas.
   */
  LLM_GEMINI("LLM Google Gemini (com fallback para Regras Léxicas)"),

  /**
   * Abordagem tradicional baseada em conjuntos léxicos com pesos fortes/médios e decaimento posicional.
   */
  REGRAS_LEXICAS("Regras Léxicas e Posição (Tradicional)"),

  /**
   * Abordagem híbrida: Regras léxicas combinadas com Embeddings Semânticos e Similaridade de Cosseno.
   */
  HIBRIDO_EMBEDDINGS("Híbrido (Regras Léxicas + Embeddings e Cosseno)");

  private final String descricao;

  ModoInferidorTema(String descricao) {
    this.descricao = descricao;
  }

  public String getDescricao() {
    return descricao;
  }
}
