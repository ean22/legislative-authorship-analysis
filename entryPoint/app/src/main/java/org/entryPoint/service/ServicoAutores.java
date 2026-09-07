package org.entryPoint.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.entryPoint.repository.RepositorioBancoDados;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ServicoAutores {
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
  }

  private List<Autor> extrairAutoresDoHtml(String html) {

    List<Autor> autores = new ArrayList<>();

    Document document = Jsoup.parse(html);

  String texto = document
      .html()
      .replaceAll("(?i)<br\\s*/?>", "\n");

    texto = Jsoup.parse(texto).text();

    String inicio = "PREFEITURA DO MUNICÍPIO DE SÃO PAULO";
    String fim = "Publicado na Secretaria do Governo Municipal";

    int posicaoInicio = texto.indexOf(inicio);
    int posicaoFim = texto.indexOf(fim);

    if (posicaoInicio == -1 || posicaoFim == -1) {
      return autores;
    }

    String blocoAssinaturas = texto.substring(
      posicaoInicio + inicio.length(),
      posicaoFim
    );

    String[] linhas = blocoAssinaturas.split("\\n");

    for (String linha : linhas) {

      linha = linha.trim();

      if (linha.isEmpty()) {
        continue;
      }

      int separador = linha.indexOf(',');

      if (separador == -1) {
        continue;
      }

      String nome = linha.substring(0, separador).trim();
      String cargo = linha.substring(separador + 1).trim();

      if (!nome.isEmpty() && !cargo.isEmpty()) {
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