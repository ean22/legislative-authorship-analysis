package org.entryPoint.benchmark.algoritmos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.entryPoint.service.agent.AgenteGemini;
import org.junit.jupiter.api.Test;

public class InferidorRotuloTemaTest {

  @Test
  void testFluxoRegrasLexicas() {
    List<String> palavras = List.of("crédito", "suplementar", "adicional", "orçamento", "abertura", "recursos");
    String rotulo = InferidorRotuloTema.inferirRotuloTema(palavras, ModoInferidorTema.REGRAS_LEXICAS);
    assertEquals("Crédito Orçamentário e Finanças", rotulo);

    List<String> vias = List.of("denomina", "logradouro", "praça", "rua", "oficial", "especifica");
    String rotuloVias = InferidorRotuloTema.inferirRotuloTema(vias, ModoInferidorTema.REGRAS_LEXICAS);
    assertEquals("Denominação de Vias e Logradouros", rotuloVias);
  }

  @Test
  void testFluxoHibridoComEmbeddingsSemanticosLocais() {
    // Caso com flexões/sinônimos que capturam a categoria via similaridade de cosseno de embeddings
    List<String> palavrasFinanceiras = List.of("dotação", "custeio", "empenho", "repasses", "receitas", "despesas");
    String rotulo = InferidorRotuloTema.inferirRotuloTema(palavrasFinanceiras, ModoInferidorTema.HIBRIDO_EMBEDDINGS);
    assertEquals("Crédito Orçamentário e Finanças", rotulo);

    List<String> palavrasEscolares = List.of("pedagogia", "escolar", "matrículas", "professores", "educacional");
    String rotuloEduc = InferidorRotuloTema.inferirRotuloTema(palavrasEscolares, ModoInferidorTema.HIBRIDO_EMBEDDINGS);
    assertEquals("Educação e Creches", rotuloEduc);
  }

  @Test
  void testVetorizadorSemanticoLocal() {
    double[] v1 = VetorizadorSemanticoLocal.vetorizar("abertura de crédito suplementar e orçamento");
    double[] v2 = VetorizadorSemanticoLocal.vetorizar("crédito suplementar orçamentário finanças");
    double[] v3 = VetorizadorSemanticoLocal.vetorizar("denominação de praça rua e avenida logradouro");

    double sim12 = VetorizadorSemanticoLocal.similaridadeCosseno(v1, v2);
    double sim13 = VetorizadorSemanticoLocal.similaridadeCosseno(v1, v3);
    double simAuto = VetorizadorSemanticoLocal.similaridadeCosseno(v1, v1);

    assertEquals(1.0, simAuto, 0.001, "Auto-similaridade deve ser 1.0");
    assertTrue(sim12 > 0.45, "Textos semanticamente correlacionados devem ter alta similaridade: " + sim12);
    assertTrue(sim12 > sim13, "Similaridade temática deve ser superior à de temas distintos: " + sim12 + " vs " + sim13);
  }

  @Test
  void testInferirRotulosEmLote() {
    List<List<String>> clusters = List.of(
        List.of("crédito", "suplementar", "adicional", "orçamento"),
        List.of("utilidade", "declara", "entidade", "associação"),
        List.of("denomina", "logradouro", "rua", "avenida")
    );

    List<String> rotulos = InferidorRotuloTema.inferirRotulosEmLote(clusters, ModoInferidorTema.HIBRIDO_EMBEDDINGS);
    assertEquals(3, rotulos.size());
    assertEquals("Crédito Orçamentário e Finanças", rotulos.get(0));
    assertEquals("Declaração de Utilidade Pública", rotulos.get(1));
    assertEquals("Denominação de Vias e Logradouros", rotulos.get(2));
  }

  @Test
  void testParsearRespostaRotulacaoTopicosGemini() {
    String jsonSimulado = """
      ```json
      {
        "topicos": [
          { "id": 1, "tema": "Crédito Orçamentário e Finanças" },
          { "id": 2, "tema": "Denominação de Vias e Logradouros" },
          { "id": 3, "tema": "Cultura, Festas e Tradições Populares" }
        ]
      }
      ```
      """;

    Map<Integer, String> mapa = AgenteGemini.parsearRespostaRotulacaoTopicos(jsonSimulado, Set.of(1, 2, 3));
    assertNotNull(mapa);
    assertEquals(3, mapa.size());
    assertEquals("Crédito Orçamentário e Finanças", mapa.get(1));
    assertEquals("Denominação de Vias e Logradouros", mapa.get(2));
    assertEquals("Cultura, Festas e Tradições Populares", mapa.get(3));
  }

  @Test
  void testLlmFallbackTransparenteQuandoSemChave() {
    // Se GEMINI_API_KEY não estiver configurada no ambiente de testes,
    // o fluxo LLM deve alternar transparentemente para o Híbrido sem lançar exceção
    List<List<String>> clusters = List.of(
        List.of("crédito", "suplementar", "adicional", "orçamento")
    );

    List<String> rotulos = InferidorRotuloTema.inferirRotulosEmLote(clusters, ModoInferidorTema.LLM_GEMINI);
    assertFalse(rotulos.isEmpty());
    assertEquals("Crédito Orçamentário e Finanças", rotulos.get(0));
  }
}
