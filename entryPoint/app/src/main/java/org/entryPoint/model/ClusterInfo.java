package org.entryPoint.model;

import java.util.List;

public record ClusterInfo(
    int idCluster,
    int totalNormas,
    double percentual,
    double coesaoMedia,
    NormaRepresentativa normaMaisRepresentativa,
    List<NormaRepresentativa> topNormasRepresentativas,
    List<String> termosFrequentes,
    float[] centroide
) {
  public String getResumoFormatado() {
    StringBuilder sb = new StringBuilder();
    sb.append(String.format("╔═══════════════════════════════════════════════════════════════════════════════════════════════╗%n"));
    sb.append(String.format("║ CLUSTER #%02d | %d normas (%.1f%%) | Coesão Média: %.3f%n", idCluster, totalNormas, percentual, coesaoMedia));
    if (termosFrequentes != null && !termosFrequentes.isEmpty()) {
      sb.append(String.format("║ Palavras-chave: %s%n", String.join(", ", termosFrequentes)));
    }
    sb.append(String.format("╠───────────────────────────────────────────────────────────────────────────────────────────────╢%n"));
    if (normaMaisRepresentativa != null) {
      sb.append(String.format("║ ⭐ NORMA MAIS REPRESENTATIVA (MEDOID): %s%n", normaMaisRepresentativa.getIdentificacaoFormatada()));
      sb.append(String.format("║    Similaridade com Centróide: %.4f | Distância: %.4f%n",
          normaMaisRepresentativa.similaridadeComCentroide(), normaMaisRepresentativa.distanciaCentroide()));
      sb.append(String.format("║    Ementa: %s%n", normaMaisRepresentativa.ementa()));
    }
    if (topNormasRepresentativas != null && topNormasRepresentativas.size() > 1) {
      sb.append(String.format("╠───────────────────────────────────────────────────────────────────────────────────────────────╢%n"));
      sb.append(String.format("║ Outras Normas Altamente Representativas no Cluster:%n"));
      for (int i = 1; i < topNormasRepresentativas.size(); i++) {
        NormaRepresentativa nr = topNormasRepresentativas.get(i);
        sb.append(String.format("║   %d. %s (Sim: %.4f) - %s%n",
            i + 1, nr.getIdentificacaoFormatada(), nr.similaridadeComCentroide(),
            truncar(nr.ementa(), 85)));
      }
    }
    sb.append(String.format("╚═══════════════════════════════════════════════════════════════════════════════════════════════╝"));
    return sb.toString();
  }

  private static String truncar(String texto, int maxLen) {
    if (texto == null) return "";
    String t = texto.replaceAll("\\s+", " ").trim();
    if (t.length() <= maxLen) return t;
    return t.substring(0, maxLen - 3) + "...";
  }
}
