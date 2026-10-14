package org.entryPoint.benchmark.algoritmos;

/**
 * Estratégias disponíveis para inferir o rótulo temático de cada cluster legislativo.
 */
public enum ModoInferidorTema {
  /**
   * Abordagem tradicional baseada em conjuntos léxicos com pesos fortes/médios e decaimento posicional.
   */
  REGRAS_LEXICAS("Regras Léxicas e Posição (Tradicional)"),

  /**
   * Alternativa 1: Envia as palavras-chave dos clusters em lote para o LLM Google Gemini
   * rotular com raciocínio semântico contextual e sugestão inteligente de novos temas.
   */
  LLM_GEMINI("Alternativa 1: LLM Google Gemini (Prompt Estruturado em Lote)"),

  /**
   * Alternativa 2: Executa matching léxico prioritário; caso a pontuação seja insuficiente ou ambígua,
   * projeta os termos em embeddings semânticos e seleciona a categoria por Similaridade de Cosseno.
   */
  HIBRIDO_EMBEDDINGS("Alternativa 2: Híbrido (Regras Léxicas + Embeddings e Cosseno)");

  private final String descricao;

  ModoInferidorTema(String descricao) {
    this.descricao = descricao;
  }

  public String getDescricao() {
    return descricao;
  }
}
