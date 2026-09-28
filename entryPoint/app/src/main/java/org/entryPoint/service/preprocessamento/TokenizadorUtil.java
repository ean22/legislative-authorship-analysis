package org.entryPoint.service.preprocessamento;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class TokenizadorUtil {

  private static final Pattern PONTUACAO = Pattern.compile("[^a-zA-Z0-9áàâãéèêíïóôõöúçñÁÀÂÃÉÈÊÍÏÓÔÕÖÚÇÑ\\s]");
  private static final Pattern ESPACOS_DUPLOS = Pattern.compile("\\s+");
  private static final Pattern DELIMITADOR_SENTENCAS = Pattern.compile("(?<=[.!?\\n;])\\s+|(?<=\\s-\\s)|(?<=\\s–\\s)");

  public static final Set<String> STOPWORDS_PORTUGUES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
      "a", "à", "ao", "aos", "aquela", "aquelas", "aquele", "aqueles", "aquilo", "as", "às", "até",
      "com", "como", "da", "das", "de", "dela", "delas", "dele", "deles", "depois", "do", "dos",
      "e", "é", "ela", "elas", "ele", "eles", "em", "entre", "era", "eram", "éramos", "essa", "essas",
      "esse", "esses", "esta", "está", "estamos", "estão", "estas", "estava", "estavam", "estávamos",
      "este", "estes", "estou", "eu", "foi", "fomos", "for", "foram", "fosse", "fossem", "fôssemos",
      "há", "haja", "hajam", "havemos", "havia", "hei", "houve", "houvemos", "houver", "houvera",
      "isso", "isto", "já", "lhe", "lhes", "mais", "mas", "me", "mesmo", "meu", "meus", "minha",
      "minhas", "muito", "na", "não", "nas", "nem", "no", "nos", "nós", "nossa", "nossas", "nosso",
      "nossos", "num", "numa", "o", "os", "ou", "para", "pela", "pelas", "pelo", "pelos", "por",
      "qual", "quando", "que", "quem", "são", "se", "seja", "sejam", "sejamos", "sem", "ser",
      "será", "serão", "serei", "seremos", "seria", "seriam", "seríamos", "seu", "seus", "só",
      "somos", "sou", "sua", "suas", "também", "te", "tem", "tém", "temos", "tenha", "tenham",
      "tenhamos", "tenho", "terá", "terão", "terei", "teremos", "teria", "teriam", "teríamos",
      "teu", "teus", "teve", "tinha", "tinham", "tínhamos", "tive", "tivemos", "tiver", "tivera",
      "tiveram", "tivéssemos", "tu", "tua", "tuas", "um", "uma", "você", "vocês", "vos",
      // Termos jurídicos de fixação formal de ementas frequentemente neutros
      "dispoe", "dispõe", "art", "artigo", "lei", "decreto", "outras", "providencias", "providências",
      "altera", "revoga", "institui", "da", "dá", "nova", "redacao", "redação", "concede"
  )));

  private TokenizadorUtil() {}

  public static String limparTexto(String texto) {
    if (texto == null) {
      return "";
    }
    String semPontuacao = PONTUACAO.matcher(texto).replaceAll(" ");
    return ESPACOS_DUPLOS.matcher(semPontuacao).replaceAll(" ").trim();
  }

  public static List<String> tokenizar(String texto, boolean removerStopwords) {
    if (texto == null || texto.isBlank()) {
      return List.of();
    }
    String limpo = limparTexto(texto).toLowerCase(Locale.ROOT);
    String[] tokens = limpo.split("\\s+");
    List<String> resultado = new ArrayList<>();
    for (String token : tokens) {
      if (token.isBlank() || token.length() <= 1) {
        continue;
      }
      if (removerStopwords && isStopword(token)) {
        continue;
      }
      resultado.add(token);
    }
    return resultado;
  }

  public static boolean isStopword(String token) {
    if (token == null) {
      return false;
    }
    String normalizado = normalizarSemAcentos(token.toLowerCase(Locale.ROOT));
    return STOPWORDS_PORTUGUES.contains(token.toLowerCase(Locale.ROOT)) ||
           STOPWORDS_PORTUGUES.contains(normalizado);
  }

  public static String normalizarSemAcentos(String texto) {
    if (texto == null) {
      return "";
    }
    return Normalizer.normalize(texto, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .toLowerCase(Locale.ROOT);
  }

  public static List<String> dividirEmSentencas(String texto) {
    if (texto == null || texto.isBlank()) {
      return List.of();
    }
    String[] partes = DELIMITADOR_SENTENCAS.split(texto.trim());
    List<String> sentencas = new ArrayList<>();
    for (String parte : partes) {
      String s = parte.trim();
      if (!s.isBlank()) {
        sentencas.add(s);
      }
    }
    if (sentencas.isEmpty() && !texto.isBlank()) {
      sentencas.add(texto.trim());
    }
    return sentencas;
  }
}
