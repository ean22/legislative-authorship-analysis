package org.entryPoint.benchmark.algoritmos;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.entryPoint.service.agent.AgenteGemini;

/**
 * Utilitário para inferência de rótulos temáticos legíveis por humanos
 * a partir dos termos mais salientes de cada cluster legislativo.
 *
 * Suporta três fluxos operacionais:
 * 1. REGRAS_LEXICAS: Dicionário fixo com pesos forte/médio e decaimento posicional.
 * 2. LLM_GEMINI (Alternativa 1): Inferência semântica contextual em lote via Google Gemini.
 * 3. HIBRIDO_EMBEDDINGS (Alternativa 2): Regras léxicas prioritárias + fallback/desempate
 *    por Embeddings Semânticos e Similaridade de Cosseno.
 */
public class InferidorRotuloTema {

  public record CategoriaTematica(
      String nome,
      String descricaoSemantica,
      Set<String> palavrasChaveFortes,
      Set<String> palavrasChaveMedias
  ) {}

  public record ResultadoAvaliacaoLexica(CategoriaTematica categoria, double score) {}

  private static final List<CategoriaTematica> CATEGORIAS = List.of(
      new CategoriaTematica(
          "Crédito Orçamentário e Finanças",
          "abertura de crédito suplementar e especial dotações orçamentárias finanças públicas receitas despesas municipais tributos impostos",
          Set.of("crédito", "suplementar", "adicional", "orçamento", "subvenções", "receita", "despesa", "tribut", "imposto"),
          Set.of("abre", "abertura", "recursos", "financeira", "suplementação")
      ),
      new CategoriaTematica(
          "Declaração de Utilidade Pública",
          "declaração de utilidade pública para entidades associações civis beneficentes institutos fundações sem fins lucrativos",
          Set.of("utilidade", "declara", "entidade", "associação", "instituto"),
          Set.of("pública", "beneficente", "fundação")
      ),
      new CategoriaTematica(
          "Educação e Creches",
          "educação infantil creches municipais escolas centros de educação infantil CEI ensino fundamental pedagógico alunos matrículas",
          Set.of("educação", "infantil", "creche", "escola", "ensino", "cei", "pedagógica"),
          Set.of("alunos", "professores", "matrícula")
      ),
      new CategoriaTematica(
          "Bens Imóveis e Desapropriação",
          "desapropriação de imóveis particulares declaração de utilidade pública para desapropriação desafetação de bens públicos terrenos áreas",
          Set.of("imóveis", "imóvel", "desapropriação", "desapropria", "desafeta", "patrimônio"),
          Set.of("particulares", "necessários", "implantação", "terreno", "área")
      ),
      new CategoriaTematica(
          "Denominação de Vias e Logradouros",
          "denominação e nomeação oficial de vias públicas praças ruas avenidas travessas e logradouros públicos do município",
          Set.of("denomina", "logradouro", "praça", "avenida", "travessa"),
          Set.of("rua", "distrito", "situado", "situados", "oficial", "especifica")
      ),
      new CategoriaTematica(
          "Datas Comemorativas e Calendário",
          "inclusão de datas comemorativas semanas de conscientização dias temáticos e eventos no calendário oficial do município",
          Set.of("calendário", "eventos", "comemorado", "conscientização"),
          Set.of("incluir", "institui", "semana", "dia", "anualmente", "oficial")
      ),
      new CategoriaTematica(
          "Saúde Pública e Assistência Hospitalar",
          "saúde pública municipal postos de saúde unidades básicas UBS hospitais atendimento médico consultas SUS vacinação medicamentos",
          Set.of("saúde", "médic", "sanitár", "hospital", "sus", "enfermagem", "clínica"),
          Set.of("atendimento", "vacinação", "medicamentos")
      ),
      new CategoriaTematica(
          "Habitação e Regularização Fundiária",
          "habitação popular moradia social regularização fundiária urbana zonas especiais de interesse social ZEIS e assentamentos urbanos",
          Set.of("zeis", "fundiária", "habitação", "moradia"),
          Set.of("social", "interesse", "assentamento")
      ),
      new CategoriaTematica(
          "Transportes e Trânsito Urbano",
          "trânsito tráfego mobilidade urbana circulação de veículos transporte coletivo linhas de ônibus tarifas estacionamento rotativo",
          Set.of("trânsito", "transporte", "veículo", "mobilidade", "estacionamento"),
          Set.of("linhas", "itinerário", "tarifa")
      ),
      new CategoriaTematica(
          "Meio Ambiente e Áreas Verdes",
          "preservação ambiental parques municipais áreas verdes arborização urbana sustentabilidade proteção da fauna e flora ecológica",
          Set.of("meio ambiente", "parque", "verde", "ambiental", "sustentab", "preservação"),
          Set.of("árvores", "fauna", "flora")
      ),
      new CategoriaTematica(
          "Serviços Públicos e Gestão Municipal",
          "prestação e concessão de serviços públicos municipais permissão de uso postos de atendimento feiras funcionamento e gestão pública",
          Set.of("serviços", "serviço", "concessão", "permissão", "municipais", "postos", "pontos"),
          Set.of("funcionamento", "prestação", "fixa", "rua", "atividades", "exercício")
      )
  );

  private static volatile ModoInferidorTema modoAtivo = ModoInferidorTema.HIBRIDO_EMBEDDINGS;
  private static final Map<String, double[]> CACHE_EMBEDDINGS_LOCAIS = new ConcurrentHashMap<>();
  private static final Map<String, double[]> CACHE_EMBEDDINGS_GEMINI = new ConcurrentHashMap<>();

  public static ModoInferidorTema getModoAtivo() {
    return modoAtivo;
  }

  public static void setModoAtivo(ModoInferidorTema modo) {
    if (modo != null) {
      modoAtivo = modo;
    }
  }

  public static List<CategoriaTematica> getCategorias() {
    return CATEGORIAS;
  }

  // =========================================================================
  // PONTO DE ENTRADA PRINCIPAL (EM LOTE)
  // =========================================================================

  /**
   * Infere rótulos para uma lista de clusters em lote, despachando para a estratégia ativa.
   */
  public static List<String> inferirRotulosEmLote(List<List<String>> topPalavrasPorCluster) {
    return inferirRotulosEmLote(topPalavrasPorCluster, modoAtivo);
  }

  /**
   * Infere rótulos para uma lista de clusters em lote com modo explícito.
   */
  public static List<String> inferirRotulosEmLote(List<List<String>> topPalavrasPorCluster, ModoInferidorTema modo) {
    if (topPalavrasPorCluster == null || topPalavrasPorCluster.isEmpty()) {
      return List.of();
    }

    ModoInferidorTema estrategia = modo != null ? modo : modoAtivo;
    return switch (estrategia) {
      case LLM_GEMINI -> inferirRotulosViaLlm(topPalavrasPorCluster);
      case HIBRIDO_EMBEDDINGS -> inferirRotulosHibridoEmbeddingsEmLote(topPalavrasPorCluster);
      case REGRAS_LEXICAS -> topPalavrasPorCluster.stream()
          .map(InferidorRotuloTema::inferirRotuloViaRegras)
          .toList();
    };
  }

  /**
   * Ponto de entrada compatível com chamadas individuais (delega ao modo ativo).
   */
  public static String inferirRotuloTema(List<String> topPalavras) {
    return inferirRotuloTema(topPalavras, modoAtivo);
  }

  public static String inferirRotuloTema(List<String> topPalavras, ModoInferidorTema modo) {
    if (topPalavras == null || topPalavras.isEmpty()) {
      return "Tópico Geral / Indefinido";
    }

    ModoInferidorTema estrategia = modo != null ? modo : modoAtivo;
    return switch (estrategia) {
      case LLM_GEMINI -> {
        var lote = inferirRotulosViaLlm(List.of(topPalavras));
        yield lote.isEmpty() ? inferirRotuloHibridoEmbeddings(topPalavras) : lote.get(0);
      }
      case HIBRIDO_EMBEDDINGS -> inferirRotuloHibridoEmbeddings(topPalavras);
      case REGRAS_LEXICAS -> inferirRotuloViaRegras(topPalavras);
    };
  }

  // =========================================================================
  // FLUXO 1 (ALTERNATIVA 1): ROTULAÇÃO VIA LLM GOOGLE GEMINI
  // =========================================================================

  /**
   * Alternativa 1: Envia as palavras-chave de todos os clusters em lote para o Agente Gemini.
   * Se a chave API não estiver configurada ou a chamada falhar, recorre de forma transparente ao fluxo Híbrido.
   */
  public static List<String> inferirRotulosViaLlm(List<List<String>> topPalavrasPorCluster) {
    if (topPalavrasPorCluster == null || topPalavrasPorCluster.isEmpty()) {
      return List.of();
    }

    AgenteGemini agente = new AgenteGemini();
    if (!agente.isConfigurado()) {
      System.out.println("[InferidorRotuloTema] GEMINI_API_KEY não configurada. Alternando automaticamente para Alternativa 2 (Híbrido Embeddings)...");
      return inferirRotulosHibridoEmbeddingsEmLote(topPalavrasPorCluster);
    }

    try {
      System.out.printf("[InferidorRotuloTema] Classificando %d tópicos com LLM %s em lote...%n",
          topPalavrasPorCluster.size(), agente.getNome());

      Map<Integer, List<String>> mapaTopicos = new LinkedHashMap<>();
      for (int i = 0; i < topPalavrasPorCluster.size(); i++) {
        mapaTopicos.put(i + 1, topPalavrasPorCluster.get(i));
      }

      List<String> nomesSugeridos = CATEGORIAS.stream().map(CategoriaTematica::nome).toList();
      Map<Integer, String> mapaClassificado = agente.classificarTopicosTematicos(mapaTopicos, nomesSugeridos);

      List<String> resultado = new ArrayList<>(topPalavrasPorCluster.size());
      for (int i = 0; i < topPalavrasPorCluster.size(); i++) {
        int id = i + 1;
        String rotulo = mapaClassificado.get(id);
        if (rotulo == null || rotulo.isBlank()) {
          rotulo = inferirRotuloHibridoEmbeddings(topPalavrasPorCluster.get(i));
        }
        resultado.add(rotulo);
      }
      return resultado;
    } catch (Exception e) {
      System.err.println("[InferidorRotuloTema] Erro na chamada ao Gemini (" + e.getMessage() + "). Recorrendo ao fluxo Híbrido...");
      return inferirRotulosHibridoEmbeddingsEmLote(topPalavrasPorCluster);
    }
  }

  // =========================================================================
  // FLUXO 2 (ALTERNATIVA 2): HÍBRIDO (REGRAS LÉXICAS + EMBEDDINGS E COSSENO)
  // =========================================================================

  /**
   * Alternativa 2 em Lote: Processa cada cluster combinando casamento léxico rápido com desempate por embeddings.
   */
  public static List<String> inferirRotulosHibridoEmbeddingsEmLote(List<List<String>> topPalavrasPorCluster) {
    if (topPalavrasPorCluster == null || topPalavrasPorCluster.isEmpty()) {
      return List.of();
    }
    List<String> resultado = new ArrayList<>(topPalavrasPorCluster.size());
    for (List<String> palavras : topPalavrasPorCluster) {
      resultado.add(inferirRotuloHibridoEmbeddings(palavras));
    }
    return resultado;
  }

  /**
   * Alternativa 2 Individual:
   * 1. Executa avaliação léxica com decaimento posicional.
   * 2. Se score >= 0.5: aceita o match léxico (alta precisão).
   * 3. Se score < 0.5: projeta termos em embeddings semânticos e busca categoria por Similaridade de Cosseno.
   */
  public static String inferirRotuloHibridoEmbeddings(List<String> topPalavras) {
    if (topPalavras == null || topPalavras.isEmpty()) {
      return "Tópico Geral / Indefinido";
    }

    // 1. Passo Léxico Rápido
    ResultadoAvaliacaoLexica avaliacao = avaliarScoreLexico(topPalavras);
    if (avaliacao.categoria() != null && avaliacao.score() >= 0.5) {
      return avaliacao.categoria().nome();
    }

    // 2. Passo Vetorial Semântico por Similaridade de Cosseno
    return inferirPorSimilaridadeCosseno(topPalavras);
  }

  /**
   * Realiza a classificação puramente vetorial via embeddings e similaridade de cosseno.
   * Prioriza embeddings da API Gemini quando configurada, com fallback automático
   * e imediato para embeddings locais densos do VetorizadorSemanticoLocal.
   */
  public static String inferirPorSimilaridadeCosseno(List<String> topPalavras) {
    if (topPalavras == null || topPalavras.isEmpty()) {
      return "Tópico Geral / Indefinido";
    }

    String textoConsulta = String.join(" ", topPalavras);

    // 1. Tentar Embeddings densos do Google Gemini (se chave presente)
    AgenteGemini agente = new AgenteGemini();
    if (agente.isConfigurado()) {
      try {
        double[] vetorGemini = agente.gerarEmbedding(textoConsulta);
        if (vetorGemini != null) {
          CategoriaTematica melhorGemini = null;
          double maiorSimGemini = -1.0;

          for (CategoriaTematica cat : CATEGORIAS) {
            double[] vetorCat = obterEmbeddingGeminiCategoria(agente, cat);
            if (vetorCat != null) {
              double sim = calcularSimilaridadeCosseno(vetorGemini, vetorCat);
              if (sim > maiorSimGemini) {
                maiorSimGemini = sim;
                melhorGemini = cat;
              }
            }
          }

          if (melhorGemini != null && maiorSimGemini >= 0.55) {
            return melhorGemini.nome();
          }
        }
      } catch (Exception e) {
        // Fallback silencioso para o vetorizador local
      }
    }

    // 2. Motor Vetorial Semântico Local (Offline / Zero Dependências)
    double[] vetorLocal = VetorizadorSemanticoLocal.vetorizar(textoConsulta);
    CategoriaTematica melhorLocal = null;
    double maiorSimLocal = -1.0;

    for (CategoriaTematica cat : CATEGORIAS) {
      double[] vetorCat = obterEmbeddingLocalCategoria(cat);
      double sim = VetorizadorSemanticoLocal.similaridadeCosseno(vetorLocal, vetorCat);
      if (sim > maiorSimLocal) {
        maiorSimLocal = sim;
        melhorLocal = cat;
      }
    }

    if (melhorLocal != null && maiorSimLocal >= 0.30) {
      return melhorLocal.nome();
    }

    // 3. Fallback rotulado
    return "Administração e Normas Gerais (" + topPalavras.get(0) + ")";
  }

  // =========================================================================
  // REGRAS LÉXICAS TRADICIONAIS
  // =========================================================================

  public static String inferirRotuloViaRegras(List<String> topPalavras) {
    if (topPalavras == null || topPalavras.isEmpty()) {
      return "Tópico Geral / Indefinido";
    }
    ResultadoAvaliacaoLexica avaliacao = avaliarScoreLexico(topPalavras);
    if (avaliacao.categoria() != null && avaliacao.score() >= 0.5) {
      return avaliacao.categoria().nome();
    }
    return "Administração e Normas Gerais (" + topPalavras.get(0) + ")";
  }

  public static ResultadoAvaliacaoLexica avaliarScoreLexico(List<String> topPalavras) {
    if (topPalavras == null || topPalavras.isEmpty()) {
      return new ResultadoAvaliacaoLexica(null, 0.0);
    }

    CategoriaTematica melhorCategoria = null;
    double melhorScore = 0.0;

    for (CategoriaTematica cat : CATEGORIAS) {
      double score = 0.0;
      for (int i = 0; i < topPalavras.size(); i++) {
        String palavra = topPalavras.get(i).toLowerCase(Locale.ROOT);
        double pesoPosicao = 1.0 / (i + 1.0);

        for (String forte : cat.palavrasChaveFortes()) {
          if (palavra.contains(forte) || forte.contains(palavra)) {
            score += 3.0 * pesoPosicao;
            break;
          }
        }
        for (String media : cat.palavrasChaveMedias()) {
          if (palavra.contains(media) || media.contains(palavra)) {
            score += 1.0 * pesoPosicao;
            break;
          }
        }
      }

      if (score > melhorScore) {
        melhorScore = score;
        melhorCategoria = cat;
      }
    }

    return new ResultadoAvaliacaoLexica(melhorCategoria, melhorScore);
  }

  // =========================================================================
  // GESTÃO DE EMBEDDINGS E CACHE
  // =========================================================================

  private static double[] obterEmbeddingLocalCategoria(CategoriaTematica cat) {
    return CACHE_EMBEDDINGS_LOCAIS.computeIfAbsent(cat.nome(), k -> {
      String textoCompleto = cat.nome() + " " + cat.descricaoSemantica() + " "
          + String.join(" ", cat.palavrasChaveFortes()) + " "
          + String.join(" ", cat.palavrasChaveMedias());
      return VetorizadorSemanticoLocal.vetorizar(textoCompleto);
    });
  }

  private static double[] obterEmbeddingGeminiCategoria(AgenteGemini agente, CategoriaTematica cat) {
    return CACHE_EMBEDDINGS_GEMINI.computeIfAbsent(cat.nome(), k -> {
      try {
        String texto = cat.nome() + ": " + cat.descricaoSemantica();
        return agente.gerarEmbedding(texto);
      } catch (Exception e) {
        return null;
      }
    });
  }

  public static double calcularSimilaridadeCosseno(double[] a, double[] b) {
    if (a == null || b == null || a.length != b.length) return 0.0;
    double dot = 0.0;
    double normA = 0.0;
    double normB = 0.0;
    for (int i = 0; i < a.length; i++) {
      dot += a[i] * b[i];
      normA += a[i] * a[i];
      normB += b[i] * b[i];
    }
    double denom = Math.sqrt(normA) * Math.sqrt(normB);
    return denom > 1e-9 ? Math.max(-1.0, Math.min(1.0, dot / denom)) : 0.0;
  }
}
