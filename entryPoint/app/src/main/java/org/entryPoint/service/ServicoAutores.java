package org.entryPoint.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.entryPoint.repository.RepositorioBancoDados;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class ServicoAutores {
  private static final Pattern NOME_AUTOR = Pattern.compile(
    "[\\p{L}]+(?:[ .'-][\\p{L}]+){1,9}"
  );
  private static final Pattern CARGO_AUTOR = Pattern.compile(
    "(?i).*\\b(prefeit[oa]|vice-prefeit[oa]|secretári[oa]|subsecretári[oa]|"
      + "ministro|governador|presidente|diretor|procurador|assessor|chefe|"
      + "superintendente|coordenador|vereador|desembargador|juiz|conselheiro|"
      + "administrador|subprefeit[oa]|corregedor|controlador|ouvidor|"
      + "comandante|reitor|delegad[oa])\\b.*"
  );
  private final RepositorioBancoDados repositorioBancoDados = new RepositorioBancoDados();

  public void extrairAutores() throws SQLException {
    for (RepositorioBancoDados.NormaComIntegra norma : repositorioBancoDados.listarNormasComIntegra()) {
      List<Autor> autores = extrairAutoresDoHtml(norma.integra());

      for (Autor autor : autores) {
        long idAutor = repositorioBancoDados.salvarAutor(autor.nome());
        repositorioBancoDados.salvarNormaAutor(
          norma.id(),
          idAutor,
          autor.cargo()
        );
      }
    }

    System.out.println("""
      \n\n
      Extração de autores concluída com sucesso!
    """);
  }

  private List<Autor> extrairAutoresDoHtml(String html) {

    List<Autor> autores = new ArrayList<>();

    String htmlComQuebras = html.replaceAll("(?i)<br\\s*\\\\?\\s*/?>", "\n");
    Document document = Jsoup.parse(htmlComQuebras);
    String texto = document.body().wholeText()
      .replace('\u00a0', ' ')
      .replaceAll("[ \\t]+", " ");
    String textoParaBusca = texto.toUpperCase(Locale.ROOT);

    int posicaoInicio = ultimaOcorrencia(textoParaBusca,
      "PREFEITURA DO MUNICÍPIO DE SÃO PAULO",
      "PREFEITURA DO MUNICIPIO DE SAO PAULO",
      "CÂMARA MUNICIPAL DE SÃO PAULO",
      "CAMARA MUNICIPAL DE SAO PAULO"
    );

    if (posicaoInicio == -1) {
      return autores;
    }

    int posicaoFim = primeiraOcorrenciaDepoisDe(textoParaBusca, posicaoInicio,
      "PUBLICADO NA SECRETARIA DO GOVERNO MUNICIPAL",
      "PUBLICADA NA SECRETARIA DO GOVERNO MUNICIPAL",
      "PUBLICADO NA SECRETARIA",
      "PUBLICADA NA CASA CIVIL",
      "PUBLICADA NA SECRETARIA GERAL PARLAMENTAR",
      "PUBLICADO NA SECRETARIA GERAL PARLAMENTAR"
    );
    if (posicaoFim == -1) {
      posicaoFim = texto.length();
    }

    String blocoAssinaturas = texto.substring(
      posicaoInicio,
      posicaoFim
    );

    String[] linhas = blocoAssinaturas.split("\\R");

    for (int indice = 0; indice < linhas.length; indice++) {
      String linha = linhas[indice];
      linha = linha.trim();

      if (linha.isEmpty()) {
        continue;
      }

      int separador = linha.indexOf(',');

      if (separador <= 0 || separador == linha.length() - 1) {
        if (indice + 1 < linhas.length) {
          String nome = linha;
          String cargo = linhas[indice + 1].trim();
          adicionarAutor(autores, nome, cargo);
        }
        continue;
      }

      String nome = linha.substring(0, separador).trim();
      String cargo = linha.substring(separador + 1).trim();
      adicionarAutor(autores, nome, cargo);
    }

    return autores;
  }

  private void adicionarAutor(List<Autor> autores, String nome, String cargo) {
    if (NOME_AUTOR.matcher(nome).matches()
      && cargo.length() <= 120
      && !cargo.matches(".*\\d.*")
      && !cargo.contains("<")
      && !cargo.contains("http")
      && CARGO_AUTOR.matcher(cargo).matches()
      && !autores.contains(new Autor(nome, cargo))) {
      autores.add(new Autor(nome, cargo));
    }
  }

  private int ultimaOcorrencia(String texto, String... marcadores) {
    int ultimaPosicao = -1;
    for (String marcador : marcadores) {
      ultimaPosicao = Math.max(ultimaPosicao, texto.lastIndexOf(marcador));
    }
    return ultimaPosicao;
  }

  private int primeiraOcorrenciaDepoisDe(
    String texto,
    int posicaoInicio,
    String... marcadores
  ) {
    int primeiraPosicao = -1;
    for (String marcador : marcadores) {
      int posicao = texto.indexOf(marcador, posicaoInicio);
      if (posicao != -1 && (primeiraPosicao == -1 || posicao < primeiraPosicao)) {
        primeiraPosicao = posicao;
      }
    }
    return primeiraPosicao;
  }

    private record Autor(
      String nome,
      String cargo
    ) {}

}