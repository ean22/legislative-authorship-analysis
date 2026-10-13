package org.entryPoint.benchmark.algoritmos;

import java.util.*;

/**
 * Utilitário para inferência de rótulos temáticos legíveis por humanos
 * a partir dos termos mais salientes de cada tópico.
 * Utiliza pontuação ponderada por posição (rank) para evitar que termos
 * periféricos/polissêmicos ofusquem o tema central do cluster.
 */
public class InferidorRotuloTema {

  private record CategoriaTematica(
      String nome,
      Set<String> palavrasChaveFortes,
      Set<String> palavrasChaveMedias
  ) {}

  private static final List<CategoriaTematica> CATEGORIAS = List.of(
      new CategoriaTematica(
          "Crédito Orçamentário e Finanças",
          Set.of("crédito", "suplementar", "adicional", "orçamento", "subvenções", "receita", "despesa", "tribut", "imposto"),
          Set.of("abre", "abertura", "recursos", "financeira", "suplementação")
      ),
      new CategoriaTematica(
          "Declaração de Utilidade Pública",
          Set.of("utilidade", "declara", "entidade", "associação", "instituto"),
          Set.of("pública", "beneficente", "fundação")
      ),
      new CategoriaTematica(
          "Educação e Creches",
          Set.of("educação", "infantil", "creche", "escola", "ensino", "cei", "pedagógica"),
          Set.of("alunos", "professores", "matrícula")
      ),
      new CategoriaTematica(
          "Bens Imóveis e Desapropriação",
          Set.of("imóveis", "imóvel", "desapropriação", "desapropria", "desafeta", "patrimônio"),
          Set.of("particulares", "necessários", "implantação", "terreno", "área")
      ),
      new CategoriaTematica(
          "Denominação de Vias e Logradouros",
          Set.of("denomina", "logradouro", "praça", "avenida", "travessa"),
          Set.of("rua", "distrito", "situado", "situados", "oficial", "especifica")
      ),
      new CategoriaTematica(
          "Datas Comemorativas e Calendário",
          Set.of("calendário", "eventos", "comemorado", "conscientização"),
          Set.of("incluir", "institui", "semana", "dia", "anualmente", "oficial")
      ),
      new CategoriaTematica(
          "Saúde Pública e Assistência Hospitalar",
          Set.of("saúde", "médic", "sanitár", "hospital", "sus", "enfermagem", "clínica"),
          Set.of("atendimento", "vacinação", "medicamentos")
      ),
      new CategoriaTematica(
          "Habitação e Regularização Fundiária",
          Set.of("zeis", "fundiária", "habitação", "moradia"),
          Set.of("social", "interesse", "assentamento")
      ),
      new CategoriaTematica(
          "Transportes e Trânsito Urbano",
          Set.of("trânsito", "transporte", "veículo", "mobilidade", "estacionamento"),
          Set.of("linhas", "itinerário", "tarifa")
      ),
      new CategoriaTematica(
          "Meio Ambiente e Áreas Verdes",
          Set.of("meio ambiente", "parque", "verde", "ambiental", "sustentab", "preservação"),
          Set.of("árvores", "fauna", "flora")
      ),
      new CategoriaTematica(
          "Serviços Públicos e Gestão Municipal",
          Set.of("serviços", "serviço", "concessão", "permissão", "municipais", "postos", "pontos"),
          Set.of("funcionamento", "prestação", "fixa", "rua", "atividades", "exercício")
      )
  );

  public static String inferirRotuloTema(List<String> topPalavras) {
    if (topPalavras == null || topPalavras.isEmpty()) {
      return "Tópico Geral / Indefinido";
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

    if (melhorCategoria != null && melhorScore >= 0.5) {
      return melhorCategoria.nome();
    }

    return "Administração e Normas Gerais (" + topPalavras.get(0) + ")";
  }
}
