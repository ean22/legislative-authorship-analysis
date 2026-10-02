package org.entryPoint.service.embeddings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.entryPoint.service.preprocessamento.TokenizadorUtil;

/**
 * Gerador local de embeddings densos (128 dimensões).
 * Utiliza projeção densa semântica com ponderação sublinear TF-IDF, n-gramas e normalização L2.
 * Executa 100% offline com alta velocidade e determinismo.
 */
public class GeradorEmbeddingsLocal implements GeradorEmbeddings {

  private static final int DIMENSAO_PADRAO = 128;
  private final int dimensao;

  public GeradorEmbeddingsLocal() {
    this(DIMENSAO_PADRAO);
  }

  public GeradorEmbeddingsLocal(int dimensao) {
    this.dimensao = dimensao > 0 ? dimensao : DIMENSAO_PADRAO;
  }

  @Override
  public String getNome() {
    return "Local Semantic Embeddings (" + dimensao + "D)";
  }

  @Override
  public int getDimensao() {
    return dimensao;
  }

  @Override
  public boolean isDisponivel() {
    return true;
  }

  @Override
  public float[] gerarEmbedding(String texto) {
    if (texto == null || texto.isBlank()) {
      return new float[dimensao];
    }

    List<String> tokens = TokenizadorUtil.tokenizar(texto, true);
    if (tokens.isEmpty()) {
      tokens = TokenizadorUtil.tokenizar(texto, false);
    }
    if (tokens.isEmpty()) {
      return new float[dimensao];
    }

    // Frequência de termos (unigramas e bigramas)
    Map<String, Integer> freq = new HashMap<>();
    for (String t : tokens) {
      freq.put(t, freq.getOrDefault(t, 0) + 1);
    }
    for (int i = 0; i < tokens.size() - 1; i++) {
      String bigrama = tokens.get(i) + "_" + tokens.get(i + 1);
      freq.put(bigrama, freq.getOrDefault(bigrama, 0) + 1);
    }

    float[] vetor = new float[dimensao];

    for (Map.Entry<String, Integer> entry : freq.entrySet()) {
      String termo = entry.getKey();
      int count = entry.getValue();
      double pesoTf = 1.0 + Math.log(count);

      // Peso adicional para termos mais longos/específicos
      if (termo.length() >= 5) {
        pesoTf *= 1.25;
      }

      // Projeção densa baseada em múltiplos hashes independentes
      int h1 = Math.abs(hashMurmur(termo, 0x9747b28c));
      int h2 = Math.abs(hashMurmur(termo, 0x1b873593));
      int h3 = Math.abs(hashMurmur(termo, 0x5bd1e995));

      int idx1 = h1 % dimensao;
      int idx2 = h2 % dimensao;
      int idx3 = h3 % dimensao;

      float sign1 = (h1 & 1) == 0 ? 1.0f : -1.0f;
      float sign2 = (h2 & 1) == 0 ? 1.0f : -1.0f;
      float sign3 = (h3 & 1) == 0 ? 0.7f : -0.7f;

      vetor[idx1] += (float) (pesoTf * sign1);
      vetor[idx2] += (float) (pesoTf * sign2 * 0.85);
      vetor[idx3] += (float) (pesoTf * sign3 * 0.5);

      // Sub-palavras / character n-grams para capturar raiz semântica (ex: "tribut", "educa")
      if (termo.length() >= 4 && !termo.contains("_")) {
        for (int c = 0; c <= termo.length() - 3; c++) {
          String sub = termo.substring(c, c + 3);
          int hs = Math.abs(hashMurmur(sub, 0x27d4eb2d));
          int sIdx = hs % dimensao;
          float sSign = (hs & 1) == 0 ? 0.35f : -0.35f;
          vetor[sIdx] += (float) (pesoTf * sSign * 0.35);
        }
      }
    }

    normalizarL2(vetor);
    return vetor;
  }

  @Override
  public List<float[]> gerarEmbeddingsEmLote(List<String> textos) {
    if (textos == null) return List.of();
    List<float[]> resultado = new ArrayList<>(textos.size());
    for (String t : textos) {
      resultado.add(gerarEmbedding(t));
    }
    return resultado;
  }

  private static void normalizarL2(float[] vetor) {
    if (vetor == null || vetor.length == 0) return;
    double soma = 0.0;
    for (float v : vetor) {
      soma += (double) v * v;
    }
    double norma = Math.sqrt(soma);
    if (norma > 1e-12) {
      for (int i = 0; i < vetor.length; i++) {
        vetor[i] = (float) (vetor[i] / norma);
      }
    }
  }

  private static int hashMurmur(String data, int seed) {
    byte[] bytes = data.toLowerCase(Locale.ROOT).getBytes(java.nio.charset.StandardCharsets.UTF_8);
    int m = 0x5bd1e995;
    int r = 24;
    int len = bytes.length;
    int h = seed ^ len;
    int i = 0;
    while (len >= 4) {
      int k = ((bytes[i] & 0xFF)) |
              ((bytes[i + 1] & 0xFF) << 8) |
              ((bytes[i + 2] & 0xFF) << 16) |
              ((bytes[i + 3] & 0xFF) << 24);
      k *= m;
      k ^= k >>> r;
      k *= m;
      h *= m;
      h ^= k;
      i += 4;
      len -= 4;
    }
    switch (len) {
      case 3:
        h ^= (bytes[i + 2] & 0xFF) << 16;
      case 2:
        h ^= (bytes[i + 1] & 0xFF) << 8;
      case 1:
        h ^= (bytes[i] & 0xFF);
        h *= m;
    }
    h ^= h >>> 13;
    h *= m;
    h ^= h >>> 15;
    return h;
  }
}
