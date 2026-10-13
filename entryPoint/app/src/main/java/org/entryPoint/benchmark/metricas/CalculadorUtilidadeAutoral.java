package org.entryPoint.benchmark.metricas;

import java.util.*;
import org.entryPoint.benchmark.model.PerfilTematicoEntidade;
import org.entryPoint.model.TopicoTematico;

/**
 * Avalia a utilidade temática dos tópicos para caracterização de autores e órgãos emissores.
 * Mede se o modelo separa e caracteriza fidedignamente os órgãos especializados (Saúde, Educação, Fazenda)
 * e captura a natureza multitemática do Prefeito.
 */
public class CalculadorUtilidadeAutoral {

  public record ResultadoUtilidadeAutoral(
      double purezaEspecializada,
      double discriminabilidadeOrgaos,
      double multitematicidadePrefeito,
      double utilidadeGlobal,
      List<PerfilTematicoEntidade> perfisOrgaos,
      List<PerfilTematicoEntidade> perfisAutores
  ) {}

  public static ResultadoUtilidadeAutoral avaliar(
      List<Long> docIds,
      double[][] matrizDocTopico,
      List<TopicoTematico> topicos,
      Map<String, List<Long>> orgaosComNormas,
      Map<Long, List<Long>> autoresComNormas,
      Map<Long, String> nomesAutores
  ) {
    if (docIds == null || matrizDocTopico == null || topicos == null || topicos.isEmpty()) {
      return new ResultadoUtilidadeAutoral(0.0, 0.0, 0.0, 0.0, List.of(), List.of());
    }

    int k = topicos.size();
    Map<Long, Integer> docIndex = new HashMap<>();
    for (int i = 0; i < docIds.size(); i++) {
      docIndex.put(docIds.get(i), i);
    }

    // 1. Construir perfis temáticos de órgãos emissores
    Map<String, double[]> distribuicoesOrgaos = new HashMap<>();
    List<PerfilTematicoEntidade> listaPerfisOrgaos = new ArrayList<>();

    if (orgaosComNormas != null) {
      for (Map.Entry<String, List<Long>> entry : orgaosComNormas.entrySet()) {
        String orgao = entry.getKey();
        List<Long> normasOrgao = entry.getValue();

        double[] somaTopicos = new double[k];
        int normasEncontradas = 0;

        for (Long idNorma : normasOrgao) {
          Integer dIdx = docIndex.get(idNorma);
          if (dIdx != null && dIdx < matrizDocTopico.length) {
            for (int t = 0; t < k; t++) {
              somaTopicos[t] += matrizDocTopico[dIdx][t];
            }
            normasEncontradas++;
          }
        }

        if (normasEncontradas >= 3) {
          double somaGeral = 0.0;
          for (int t = 0; t < k; t++) {
            somaGeral += somaTopicos[t];
          }
          if (somaGeral > 0) {
            for (int t = 0; t < k; t++) {
              somaTopicos[t] /= somaGeral;
            }
          }
          distribuicoesOrgaos.put(orgao, somaTopicos);

          PerfilTematicoEntidade perfil = criarPerfil(orgao, "ORGAO_EMISSOR", normasEncontradas, somaTopicos, topicos);
          listaPerfisOrgaos.add(perfil);
        }
      }
    }

    // Ordenar órgãos pelos que possuem mais normas
    listaPerfisOrgaos.sort((a, b) -> Integer.compare(b.totalNormas(), a.totalNormas()));

    // 2. Construir perfis de autores individuais
    List<PerfilTematicoEntidade> listaPerfisAutores = new ArrayList<>();
    if (autoresComNormas != null && nomesAutores != null) {
      for (Map.Entry<Long, List<Long>> entry : autoresComNormas.entrySet()) {
        Long idAutor = entry.getKey();
        String nomeAutor = nomesAutores.getOrDefault(idAutor, "Autor #" + idAutor);
        List<Long> normasAutor = entry.getValue();

        double[] somaTopicos = new double[k];
        int normasEncontradas = 0;

        for (Long idNorma : normasAutor) {
          Integer dIdx = docIndex.get(idNorma);
          if (dIdx != null && dIdx < matrizDocTopico.length) {
            for (int t = 0; t < k; t++) {
              somaTopicos[t] += matrizDocTopico[dIdx][t];
            }
            normasEncontradas++;
          }
        }

        if (normasEncontradas >= 5) {
          double somaGeral = 0.0;
          for (int t = 0; t < k; t++) {
            somaGeral += somaTopicos[t];
          }
          if (somaGeral > 0) {
            for (int t = 0; t < k; t++) {
              somaTopicos[t] /= somaGeral;
            }
          }
          PerfilTematicoEntidade perfil = criarPerfil(nomeAutor, "AUTOR", normasEncontradas, somaTopicos, topicos);
          listaPerfisAutores.add(perfil);
        }
      }
    }
    listaPerfisAutores.sort((a, b) -> Integer.compare(b.totalNormas(), a.totalNormas()));

    // 3. Avaliar métricas reais de qualidade temática:
    // Identificar órgãos temáticos especializados (Saúde, Educação, Finanças, Meio Ambiente, etc.)
    List<double[]> vetoresEspecializados = new ArrayList<>();
    double somaPicosEspecializados = 0.0;

    for (Map.Entry<String, double[]> entry : distribuicoesOrgaos.entrySet()) {
      String orgao = entry.getKey().toUpperCase(Locale.ROOT);
      if (orgao.contains("SAUDE") || orgao.contains("EDUCACAO") || orgao.contains("FAZENDA")
          || orgao.contains("FINANCAS") || orgao.contains("VERDE") || orgao.contains("MEIO AMBIENTE")
          || orgao.contains("TRANSPORTES") || orgao.contains("HABITACAO") || orgao.contains("JUSTICA")) {
        double[] dist = entry.getValue();
        vetoresEspecializados.add(dist);

        double maxP = 0.0;
        for (double p : dist) {
          if (p > maxP) maxP = p;
        }
        somaPicosEspecializados += maxP;
      }
    }

    double pureza = vetoresEspecializados.isEmpty() ? 0.3 : (somaPicosEspecializados / vetoresEspecializados.size());

    // Divergência e separabilidade entre os órgãos especializados
    double discriminabilidade = 0.5;
    if (vetoresEspecializados.size() >= 2) {
      double somaJsd = 0.0;
      int pares = 0;
      for (int i = 0; i < vetoresEspecializados.size(); i++) {
        for (int j = i + 1; j < vetoresEspecializados.size(); j++) {
          somaJsd += calcularJensenShannon(vetoresEspecializados.get(i), vetoresEspecializados.get(j));
          pares++;
        }
      }
      discriminabilidade = pares > 0 ? (somaJsd / pares) : 0.5;
    }

    // Multitematicidade do Prefeito
    double multitematismoPrefeito = 0.8;
    for (Map.Entry<String, double[]> entry : distribuicoesOrgaos.entrySet()) {
      if (entry.getKey().equalsIgnoreCase("PREFEITO")) {
        double entropia = calcularEntropiaNormalizada(entry.getValue());
        multitematismoPrefeito = entropia;
        break;
      }
    }

    // Score composto ponderado de utilidade autoral
    double utilidadeGlobal = (0.45 * pureza) + (0.40 * discriminabilidade) + (0.15 * multitematismoPrefeito);
    utilidadeGlobal = Math.max(0.0, Math.min(1.0, utilidadeGlobal));

    return new ResultadoUtilidadeAutoral(
        pureza,
        discriminabilidade,
        multitematismoPrefeito,
        utilidadeGlobal,
        listaPerfisOrgaos,
        listaPerfisAutores
    );
  }

  private static PerfilTematicoEntidade criarPerfil(
      String nome,
      String tipo,
      int totalNormas,
      double[] dist,
      List<TopicoTematico> topicos
  ) {
    Map<Integer, Double> mapaDist = new LinkedHashMap<>();
    int top1 = 0;
    int top2 = 0;
    double p1 = -1.0;
    double p2 = -1.0;

    for (int t = 0; t < dist.length; t++) {
      double p = dist[t];
      mapaDist.put(t + 1, p);
      if (p > p1) {
        p2 = p1;
        top2 = top1;
        p1 = p;
        top1 = t + 1;
      } else if (p > p2) {
        p2 = p;
        top2 = t + 1;
      }
    }

    String nomeTopico1 = top1 > 0 && top1 <= topicos.size() ? topicos.get(top1 - 1).nome() : "Tópico #" + top1;
    String nomeTopico2 = top2 > 0 && top2 <= topicos.size() ? topicos.get(top2 - 1).nome() : "Tópico #" + top2;
    double entropia = calcularEntropiaNormalizada(dist);

    return new PerfilTematicoEntidade(
        nome,
        tipo,
        totalNormas,
        mapaDist,
        top1,
        nomeTopico1,
        Math.max(0.0, p1),
        top2,
        nomeTopico2,
        Math.max(0.0, p2),
        entropia
    );
  }

  private static double calcularJensenShannon(double[] p, double[] q) {
    int len = p.length;
    double[] m = new double[len];
    for (int i = 0; i < len; i++) {
      m[i] = 0.5 * (p[i] + q[i]);
    }

    double klPm = 0.0;
    double klQm = 0.0;
    for (int i = 0; i < len; i++) {
      if (p[i] > 1e-9 && m[i] > 1e-9) {
        klPm += p[i] * Math.log(p[i] / m[i]);
      }
      if (q[i] > 1e-9 && m[i] > 1e-9) {
        klQm += q[i] * Math.log(q[i] / m[i]);
      }
    }
    double jsd = 0.5 * (klPm + klQm);
    // Normalizar sqrt(JSD / ln(2))
    return Math.min(1.0, Math.sqrt(Math.max(0.0, jsd / Math.log(2.0))));
  }

  private static double calcularEntropiaNormalizada(double[] dist) {
    if (dist.length <= 1) return 1.0;
    double h = 0.0;
    for (double p : dist) {
      if (p > 1e-9) {
        h -= p * (Math.log(p) / Math.log(2.0));
      }
    }
    double hMax = Math.log(dist.length) / Math.log(2.0);
    return Math.max(0.0, Math.min(1.0, h / hMax));
  }
}
