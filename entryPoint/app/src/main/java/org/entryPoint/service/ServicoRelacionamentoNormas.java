package org.entryPoint.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.entryPoint.model.RelacionamentoNorma;
import org.entryPoint.repository.RepositorioBancoDados;
import org.entryPoint.repository.RepositorioBancoDados.NormaComIntegra;

public class ServicoRelacionamentoNormas {

  private static final Pattern TAG_LINK = Pattern.compile(
      "(?i)<a\\b(?<attrs>[^>]*)>(?<texto>.*?)<\\/a>",
      Pattern.DOTALL
  );

  private static final Pattern ATTR_DATA_ID = Pattern.compile(
      "(?i)\\bdata-id=[\"']?(\\d+)[\"']?"
  );

  private static final Pattern ATTR_HREF = Pattern.compile(
      "(?i)\\bhref=[\"']?([^\"'\\s>]+)[\"']?"
  );

  // Ex: https://leis.org/municipais/sp/sao-paulo/lei/lei-ordinaria/2013/15764
  // Ex: https://leismunicipais.com.br/a/sp/s/sao-paulo/decreto/2013/5368/53685/...
  private static final Pattern URL_INFO = Pattern.compile(
      "(?i)/sao-paulo/(?:lei/)?(?<tipoSlug>[a-z0-9\\-]+)(?:/(?<ano>\\d{4}))?(?:/[^/]+)?(?:/(?<numero>\\d+))?"
  );

  private static final Pattern REGEX_REVOGACAO = Pattern.compile(
      "(?iU)\\b(revoga(?:m-se|-se|da|das|do|dos|r)?|revoga[çc][ãa]o|ficam?\\s+revogad[ao]s?)\\b"
  );

  private static final Pattern REGEX_ALTERACAO = Pattern.compile(
      "(?iU)\\b(altera(?:m-se|-se|da|do|r)?|altera[çc][ãa]o|alterad[ao]s?|passa\\s+a\\s+vigorar|reda[çc][ãa]o\\s+dada|nova\\s+reda[çc][ãa]o|com\\s+a\\s+seguinte\\s+reda[çc][ãa]o|substitui(?:-se|m-se|r|da|do)?|substitui[çc][ãa]o)\\b"
  );

  private static final Pattern REGEX_ACRESCIMO = Pattern.compile(
      "(?iU)\\b(acrescenta(?:m-se|-se|r|do|da)?|acrescido|acrescida|acrescentad[ao]s?|acresce(?:-se)?|inserid[ao]s?|inclu[ií]d[ao]s?)\\b"
  );

  private static final Pattern REGEX_REGULAMENTACAO = Pattern.compile(
      "(?iU)\\b(regulamenta(?:m-se|-se|r|do|da)?|regulamenta[çc][ãa]o|d[aá]\\s+regulamento)\\b"
  );

  private static final Pattern REGEX_SUSPENSAO_PRORROGACAO = Pattern.compile(
      "(?iU)\\b(suspende(?:m-se|-se|r|do|da)?|suspens[ãa]o|prorroga(?:m-se|-se|r|do|da)?|prorroga[çc][ãa]o)\\b"
  );

  private static final Pattern REGEX_BASE_LEGAL = Pattern.compile(
      "(?iU)\\b(de\\s+acordo\\s+com|nos\\s+termos|com\\s+base|com\\s+fundamento|em\\s+cumprimento|em\\s+conformidade)\\b"
  );

  private static final Pattern REGEX_VIDE = Pattern.compile(
      "(?iU)\\b(vide|veja)\\b"
  );

  private static final Pattern TAGS_HTML = Pattern.compile("<[^>]+>");

  private final RepositorioBancoDados repositorioBancoDados = new RepositorioBancoDados();

  public void extrairEPopularRelacionamentos() throws SQLException {
    System.out.println("Carregando normas e índices em memória para resolução rápida...");
    List<NormaComIntegra> normas = repositorioBancoDados.listarNormasComIntegra();
    Set<Long> idsExistentes = repositorioBancoDados.listarTodosIdsNormas();
    Map<String, Long> mapaNormasPorChave = repositorioBancoDados.mapearChavesNormasParaId();

    System.out.printf("Total de normas com íntegra para processar: %d\n", normas.size());

    List<RelacionamentoNorma> lote = new ArrayList<>();
    int totalProcessados = 0;
    int tamanhoLote = 1000;

    for (NormaComIntegra norma : normas) {
      String integra = norma.integra();
      if (integra == null || integra.isBlank()) {
        continue;
      }

      Matcher matcher = TAG_LINK.matcher(integra);
      while (matcher.find()) {
        int inicioLink = matcher.start();
        String attrs = matcher.group("attrs");

        // 1. Extrair URL
        String urlAlvo = extrairHref(attrs);
        if (urlAlvo == null || urlAlvo.isBlank()) {
          continue;
        }

        // 2. Extrair data-id se existir
        Long idDestino = extrairDataId(attrs);
        if (idDestino != null && !idsExistentes.contains(idDestino)) {
          // Id do link aponta para uma norma externa não cadastrada no banco local
          idDestino = null;
        }

        // 3. Extrair metadados da URL (tipoSlug, ano, numero)
        String tipoSlugAlvo = null;
        Integer anoAlvo = null;
        Integer numeroAlvo = null;

        Matcher urlMatcher = URL_INFO.matcher(urlAlvo);
        if (urlMatcher.find()) {
          tipoSlugAlvo = urlMatcher.group("tipoSlug");
          String anoStr = urlMatcher.group("ano");
          String numStr = urlMatcher.group("numero");

          if (anoStr != null) {
            try { anoAlvo = Integer.parseInt(anoStr); } catch (NumberFormatException ignored) {}
          }
          if (numStr != null) {
            try { numeroAlvo = Integer.parseInt(numStr); } catch (NumberFormatException ignored) {}
          }

          // Se não veio data-id ou não foi resolvido, tentar via chave (slug:ano:numero)
          if (idDestino == null && tipoSlugAlvo != null && anoAlvo != null && numeroAlvo != null) {
            String chave = gerarChaveNorma(tipoSlugAlvo, anoAlvo, numeroAlvo);
            idDestino = mapaNormasPorChave.get(chave);
          }
        }

        // 4. Capturar trecho de contexto ao redor do link
        int inicioContexto = Math.max(0, inicioLink - 100);
        String trechoBruto = integra.substring(inicioContexto, inicioLink);
        String trechoLimpo = TAGS_HTML.matcher(trechoBruto).replaceAll(" ").replaceAll("\\s+", " ").trim();

        // 5. Identificar tipo de relacionamento com Regex
        String tipoRelacionamento = classificarRelacionamento(trechoLimpo);

        RelacionamentoNorma rel = new RelacionamentoNorma(
            norma.id(),
            idDestino,
            tipoRelacionamento,
            urlAlvo,
            tipoSlugAlvo,
            anoAlvo,
            numeroAlvo,
            trechoLimpo.length() > 200 ? trechoLimpo.substring(trechoLimpo.length() - 200) : trechoLimpo
        );

        lote.add(rel);
        totalProcessados++;

        if (lote.size() >= tamanhoLote) {
          repositorioBancoDados.salvarRelacionamentosEmLote(lote);
          lote.clear();
          System.out.printf("Relacionamentos extraídos até agora: %d\n", totalProcessados);
        }
      }
    }

    if (!lote.isEmpty()) {
      repositorioBancoDados.salvarRelacionamentosEmLote(lote);
      lote.clear();
    }

    long totalNoBanco = repositorioBancoDados.contarRelacionamentos();
    System.out.printf("""
        \n==========================================================
        Extração de relacionamentos concluída com sucesso!
        Total de links/relacionamentos salvos: %d
        ==========================================================\n
        """, totalNoBanco);
  }

  private String extrairHref(String attrs) {
    Matcher m = ATTR_HREF.matcher(attrs);
    if (m.find()) {
      return m.group(1);
    }
    return null;
  }

  private Long extrairDataId(String attrs) {
    Matcher m = ATTR_DATA_ID.matcher(attrs);
    if (m.find()) {
      try {
        return Long.parseLong(m.group(1));
      } catch (NumberFormatException ignored) {}
    }
    return null;
  }

  public String classificarRelacionamento(String textoContexto) {
    if (textoContexto == null || textoContexto.isBlank()) {
      return "REFERENCIA";
    }

    if (REGEX_REVOGACAO.matcher(textoContexto).find()) {
      return "REVOGACAO";
    }
    if (REGEX_ALTERACAO.matcher(textoContexto).find()) {
      return "ALTERACAO";
    }
    if (REGEX_ACRESCIMO.matcher(textoContexto).find()) {
      return "ACRESCIMO";
    }
    if (REGEX_REGULAMENTACAO.matcher(textoContexto).find()) {
      return "REGULAMENTACAO";
    }
    if (REGEX_SUSPENSAO_PRORROGACAO.matcher(textoContexto).find()) {
      return "SUSPENSAO_PRORROGACAO";
    }
    if (REGEX_BASE_LEGAL.matcher(textoContexto).find()) {
      return "BASE_LEGAL";
    }
    if (REGEX_VIDE.matcher(textoContexto).find()) {
      return "VIDE";
    }

    return "REFERENCIA";
  }

  public static String gerarChaveNorma(String tipoSlug, int ano, int numero) {
    return (tipoSlug + ":" + ano + ":" + numero).toLowerCase();
  }
}
