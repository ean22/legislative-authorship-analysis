package org.entryPoint.benchmark.algoritmos;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Vetorizador Semântico Local leve, determinístico e autônomo (zero dependências externas).
 * Utiliza projeção densa baseada em n-gramas de caracteres e radicais de palavras (Hashing Trick),
 * gerando embeddings normalizados (L2) para comparação semântica via similaridade de cosseno.
 * 
 * Atua como motor vetorial offline e fallback robusto para a Alternativa 2 (Híbrido).
 */
public class VetorizadorSemanticoLocal {

  public static final int DIMENSAO = 128;

  /**
   * Converte uma string (ou conjunto de palavras) em um vetor de embedding de 128 dimensões, normalizado em L2.
   */
  public static double[] vetorizar(String texto) {
    double[] vetor = new double[DIMENSAO];
    if (texto == null || texto.isBlank()) {
      return vetor;
    }

    String normalizado = Normalizer.normalize(texto.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .replaceAll("[^a-z0-9\\s]", " ")
        .replaceAll("\\s+", " ")
        .trim();

    String[] palavras = normalizado.split(" ");
    for (String palavra : palavras) {
      if (palavra.length() < 2) continue;

      // 1. Ponderação da palavra completa (peso 2.5)
      int hashPalavra = hashString(palavra, 0);
      int dimPalavra = Math.abs(hashPalavra % DIMENSAO);
      double sinalPalavra = (hashPalavra & 1) == 0 ? 1.0 : -1.0;
      vetor[dimPalavra] += sinalPalavra * 2.5;

      // 2. Extração de 3-grams e 4-grams de caracteres para capturar radicais morfológicos
      String envolta = "<" + palavra + ">";
      for (int n = 3; n <= 4; n++) {
        for (int i = 0; i <= envolta.length() - n; i++) {
          String ngram = envolta.substring(i, i + n);
          int hashNgram = hashString(ngram, n);
          int dimNgram = Math.abs(hashNgram % DIMENSAO);
          double sinalNgram = (hashNgram & 1) == 0 ? 1.0 : -1.0;
          vetor[dimNgram] += sinalNgram * 1.0;
        }
      }
    }

    // 3. Normalização L2 (para que o produto interno represente a similaridade de cosseno)
    double somaQuad = 0.0;
    for (double v : vetor) {
      somaQuad += v * v;
    }
    double norma = Math.sqrt(somaQuad);
    if (norma > 1e-9) {
      for (int i = 0; i < DIMENSAO; i++) {
        vetor[i] /= norma;
      }
    }

    return vetor;
  }

  /**
   * Calcula a similaridade de cosseno entre dois vetores normalizados em L2.
   * O resultado é o produto escalar (dot product), variando entre -1.0 e +1.0.
   */
  public static double similaridadeCosseno(double[] a, double[] b) {
    if (a == null || b == null || a.length != b.length) {
      return 0.0;
    }
    double dot = 0.0;
    for (int i = 0; i < a.length; i++) {
      dot += a[i] * b[i];
    }
    return Math.max(-1.0, Math.min(1.0, dot));
  }

  private static int hashString(String str, int seed) {
    int h = seed ^ 0x9747b28c;
    for (int i = 0; i < str.length(); i++) {
      h ^= str.charAt(i);
      h *= 0x5bd1e995;
      h ^= h >>> 15;
    }
    return h;
  }
}
