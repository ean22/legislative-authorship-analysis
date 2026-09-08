package org.entryPoint.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.entryPoint.repository.RepositorioBancoDados;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ServicoAutores {
  private final RepositorioBancoDados repositorioBancoDados = new RepositorioBancoDados();

  public void extrairAutores() throws SQLException {
    for (RepositorioBancoDados.NormaComIntegra norma : repositorioBancoDados.listarNormasComIntegra()) {
      List<Autor> autores = extrairAutoresDoHtml(norma.integra());

      for (Autor autor : autores) {

        System.out.println("Autor: " + autor.nome() + ", Cargo: " + autor.cargo());

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
    String texto = document.wholeText();

    String inicio = "PREFEITURA DO MUNICÍPIO DE SÃO PAULO";
    String fim = "PUBLICADO NA SECRETARIA DO GOVERNO MUNICIPAL";
    String textoParaBusca = texto.toUpperCase(Locale.ROOT);
    String inicioNormalizado = inicio.toUpperCase(Locale.ROOT);
    String fimNormalizado = fim.toUpperCase(Locale.ROOT);
    int posicaoFim = textoParaBusca.lastIndexOf(fimNormalizado);
    int posicaoInicio = textoParaBusca.lastIndexOf(inicioNormalizado, posicaoFim);

    if (posicaoInicio == -1 || posicaoFim == -1 || posicaoInicio >= posicaoFim) {
      return autores;
    }

    String blocoAssinaturas = texto.substring(
      posicaoInicio + inicio.length(),
      posicaoFim
    );

    String[] linhas = blocoAssinaturas.split("\\R");

    for (String linha : linhas) {
      linha = linha.trim();

      if (linha.isEmpty()) {
        continue;
      }

      int separador = linha.indexOf(',');

      if (separador <= 0 || separador == linha.length() - 1) {
        continue;
      }

      String nome = linha.substring(0, separador).trim();
      String cargo = linha.substring(separador + 1).trim();

      if (nome.matches("[\\p{L}][\\p{L} .'-]{2,}") && !cargo.isEmpty()) {
        autores.add(new Autor(nome, cargo));
      }
    }

    return autores;
  }

    private record Autor(
      String nome,
      String cargo
    ) {}

}