package org.entryPoint.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServicoAutores {

    private final Connection conexao;
    

    public ServicoAutores(Connection conexao) {
        this.conexao = conexao;
    }

    public void extrairAutores() throws SQLException {

        String sql = """
            SELECT id, integra
            FROM normas
            WHERE integra IS NOT NULL
              AND TRIM(integra) <> ''
            """;

        try (PreparedStatement statement = conexao.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {

                long idNorma = resultSet.getLong("id");
                String integra = resultSet.getString("integra");

                List<Autor> autores = extrairAutoresDoHtml(integra);

                for (Autor autor : autores) {
                    long idAutor = salvarAutor(autor.nome());
                    salvarNormaAutor(
                        idNorma,
                        idAutor,
                        autor.cargo()
                    );
                }
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

    private long salvarAutor(String nome) throws SQLException {

        String sql = """
            INSERT INTO autores (nome)
            VALUES (?)
            ON CONFLICT(nome) DO NOTHING
            """;

        try (PreparedStatement statement =
                 conexao.prepareStatement(sql)) {

            statement.setString(1, normalizarNome(nome));
            statement.executeUpdate();
        }

        String busca = """
            SELECT id
            FROM autores
            WHERE nome = ?
            """;

        try (PreparedStatement statement =
                 conexao.prepareStatement(busca)) {

            statement.setString(1, normalizarNome(nome));

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    return resultSet.getLong("id");
                }
            }
        }

        throw new SQLException(
            "Não foi possível obter o ID do autor: " + nome
        );
    }

    private void salvarNormaAutor(
        long idNorma,
        long idAutor,
        String cargo
    ) throws SQLException {

        String sql = """
            INSERT INTO norma_autor (
                id_norma,
                id_autor,
                cargo_autor
            )
            VALUES (?, ?, ?)
            ON CONFLICT(id_norma, id_autor) DO UPDATE SET
                cargo_autor = excluded.cargo_autor
            """;

        try (PreparedStatement statement =
                 conexao.prepareStatement(sql)) {

            statement.setLong(1, idNorma);
            statement.setLong(2, idAutor);
            statement.setString(3, cargo);

            statement.executeUpdate();
        }
    }

    private String normalizarNome(String nome) {

        return nome
            .trim()
            .replaceAll("\\s+", " ")
            .toUpperCase();
    }

    private record Autor(
        String nome,
        String cargo
    ) {}
}