package org.entryPoint.model.perfil;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Representação da Rede de Colaboração Política e Coautoria Legislativa.
 * Permite a inspeção textual no console, bem como exportação em DOT (Graphviz)
 * e JSON (D3.js / Vis.js / Cytoscape) para visualização gráfica.
 */
public record GrafoColaboracaoPolitica(
    List<NoAutor> nos,
    List<ArestaColaboracao> arestas
) {

  public record NoAutor(
      long id,
      String nome,
      String cargo,
      int totalNormas,
      String temaDominante,
      int grauConexoes
  ) {}

  public record ArestaColaboracao(
      long idOrigem,
      String nomeOrigem,
      long idDestino,
      String nomeDestino,
      int pesoNormas,
      List<String> topicosCompartilhados
  ) {}

  /**
   * Exporta a rede em formato DOT (Graphviz) para renderização com `dot -Tpng`.
   */
  public String exportarDot(String titulo, int pesoMinimo) {
    StringBuilder sb = new StringBuilder();
    String safeTitulo = (titulo != null && !titulo.isBlank()) ? titulo.replaceAll("[^a-zA-Z0-9_]", "_") : "RedeColaboracaoPolitica";

    sb.append("graph ").append(safeTitulo).append(" {\n");
    sb.append("  layout=neato;\n");
    sb.append("  overlap=false;\n");
    sb.append("  splines=true;\n");
    sb.append("  node [shape=box, style=\"filled,rounded\", fontname=\"Helvetica\", fontsize=10, fillcolor=\"#E8F0FE\", color=\"#1A73E8\"];\n");
    sb.append("  edge [fontname=\"Helvetica\", fontsize=8, color=\"#5F6368\"];\n\n");

    for (NoAutor no : nos) {
      String label = String.format("%s\\n(%s)\\n%d normas",
          escapar(no.nome()),
          escapar(no.cargo() != null ? no.cargo() : "AUTOR"),
          no.totalNormas());
      sb.append(String.format("  \"%d\" [label=\"%s\"];\n", no.id(), label));
    }
    sb.append("\n");

    for (ArestaColaboracao e : arestas) {
      if (e.pesoNormas() >= pesoMinimo) {
        double penwidth = Math.min(8.0, 1.0 + Math.log10(e.pesoNormas() + 1.0) * 2.0);
        String temasLabel = (e.topicosCompartilhados() != null && !e.topicosCompartilhados().isEmpty())
            ? escapar(e.topicosCompartilhados().get(0))
            : "";
        sb.append(String.format(Locale.ROOT,
            "  \"%d\" -- \"%d\" [weight=%d, penwidth=%.2f, label=\"%d normas\"];\n",
            e.idOrigem(), e.idDestino(), e.pesoNormas(), penwidth, e.pesoNormas()));
      }
    }

    sb.append("}\n");
    return sb.toString();
  }

  /**
   * Exporta a rede em JSON estruturado compatível com bibliotecas web (D3.js / Vis.js).
   */
  public String exportarJsonD3(int pesoMinimo) {
    StringBuilder sb = new StringBuilder();
    sb.append("{\n");
    sb.append("  \"nodes\": [\n");

    for (int i = 0; i < nos.size(); i++) {
      NoAutor n = nos.get(i);
      sb.append(String.format(
          "    {\"id\": %d, \"name\": \"%s\", \"cargo\": \"%s\", \"totalNormas\": %d, \"temaDominante\": \"%s\", \"degree\": %d}%s\n",
          n.id(), escaparJson(n.nome()), escaparJson(n.cargo() != null ? n.cargo() : ""),
          n.totalNormas(), escaparJson(n.temaDominante() != null ? n.temaDominante() : ""),
          n.grauConexoes(),
          (i < nos.size() - 1 ? "," : "")));
    }

    sb.append("  ],\n");
    sb.append("  \"links\": [\n");

    List<ArestaColaboracao> arestasFiltradas = arestas.stream()
        .filter(a -> a.pesoNormas() >= pesoMinimo)
        .toList();

    for (int i = 0; i < arestasFiltradas.size(); i++) {
      ArestaColaboracao a = arestasFiltradas.get(i);
      String temas = (a.topicosCompartilhados() != null)
          ? String.join("; ", a.topicosCompartilhados())
          : "";
      sb.append(String.format(
          "    {\"source\": %d, \"target\": %d, \"sourceName\": \"%s\", \"targetName\": \"%s\", \"weight\": %d, \"sharedTopics\": \"%s\"}%s\n",
          a.idOrigem(), a.idDestino(), escaparJson(a.nomeOrigem()), escaparJson(a.nomeDestino()),
          a.pesoNormas(), escaparJson(temas),
          (i < arestasFiltradas.size() - 1 ? "," : "")));
    }

    sb.append("  ]\n");
    sb.append("}\n");
    return sb.toString();
  }

  /**
   * Apresenta um resumo analítico e uma visualização em tabela no console.
   */
  public String gerarVisualizacaoConsole(int limiteArestas) {
    StringBuilder sb = new StringBuilder();
    String divisor = "=".repeat(95);

    sb.append("\n").append(divisor).append("\n");
    sb.append("🌐  REDE DE COLABORAÇÃO POLÍTICA E COAUTORIA LEGISLATIVA (GRAFO)\n");
    sb.append(divisor).append("\n");
    sb.append(String.format("• Total de Atores (Nós): %d | Total de Vínculos de Coautoria (Arestas): %d%n",
        nos.size(), arestas.size()));

    if (!nos.isEmpty()) {
      var topHubs = nos.stream()
          .sorted(Comparator.comparingInt(NoAutor::grauConexoes).reversed())
          .limit(5)
          .toList();
      sb.append("\n🏛️  PRINCIPAIS HUBS POLÍTICOS DA REDE (MAIOR GRAU DE CONECTIVIDADE):\n");
      for (NoAutor h : topHubs) {
        sb.append(String.format("   • %-32s | Grau: %2d parceiros | %4d normas | Pauta: %s%n",
            h.nome(), h.grauConexoes(), h.totalNormas(), h.temaDominante()));
      }
    }

    if (!arestas.isEmpty()) {
      int limit = limiteArestas > 0 ? limiteArestas : 15;
      var topArestas = arestas.stream()
          .sorted(Comparator.comparingInt(ArestaColaboracao::pesoNormas).reversed())
          .limit(limit)
          .toList();

      sb.append(String.format("%n🤝  TOP %d VÍNCULOS DE COAUTORIA MAIS FORTES (PESO DE NORMAS CONJUNTAS):%n", topArestas.size()));
      sb.append("-".repeat(95)).append("\n");
      sb.append(String.format("%-32s <===> %-32s | %-12s | %s%n",
          "Autor A", "Autor B", "Peso Normas", "Eixo Temático Compartilhado"));
      sb.append("-".repeat(95)).append("\n");

      for (ArestaColaboracao a : topArestas) {
        String tema = (a.topicosCompartilhados() != null && !a.topicosCompartilhados().isEmpty())
            ? a.topicosCompartilhados().get(0)
            : "Diversos";
        sb.append(String.format("%-32s <===> %-32s | %4d normas  | %s%n",
            cortar(a.nomeOrigem(), 32), cortar(a.nomeDestino(), 32), a.pesoNormas(), tema));
      }
    }
    sb.append(divisor).append("\n");
    return sb.toString();
  }

  private String escapar(String s) {
    if (s == null) return "";
    return s.replace("\"", "\\\"").replace("\n", " ");
  }

  private String escaparJson(String s) {
    if (s == null) return "";
    return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
  }

  private String cortar(String s, int max) {
    if (s == null) return "";
    return s.length() > max ? s.substring(0, max - 3) + "..." : s;
  }
}
