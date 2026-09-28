package org.entryPoint.repository;

import org.entryPoint.model.EmentaParaTopico;
import org.entryPoint.model.EmentaPreProcessada;
import org.entryPoint.model.Norma;
import org.entryPoint.model.NormaTopico;
import org.entryPoint.model.RelacionamentoNorma;
import org.entryPoint.model.TopicoTematico;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RepositoriosTest {

  private static RepositorioBancoDados repositorioFacade;
  private static RepositorioNormas repoNormas;
  private static RepositorioAutores repoAutores;
  private static RepositorioEmentasPreProcessadas repoEmentas;
  private static RepositorioRelacionamentos repoRelacionamentos;
  private static RepositorioTopicos repoTopicos;

  @BeforeAll
  static void setUp() throws SQLException {
    repositorioFacade = new RepositorioBancoDados();
    repoNormas = repositorioFacade.getRepositorioNormas();
    repoAutores = repositorioFacade.getRepositorioAutores();
    repoEmentas = repositorioFacade.getRepositorioEmentas();
    repoRelacionamentos = repositorioFacade.getRepositorioRelacionamentos();
    repoTopicos = repositorioFacade.getRepositorioTopicos();

    // Inserir norma base para os testes de chave estrangeira
    Norma norma = new Norma();
    norma.setId(999001L);
    norma.setNumero(9991);
    norma.setAno(2025);
    norma.setTipoEscrito("Lei Ordinária");
    norma.setTipoSlug("lei-ordinaria");
    norma.setTitulo("Lei Teste Unitário 999001");
    norma.setEmenta("Dispõe sobre teste unitário de normas.");
    norma.setIntegra("<p>Integra com link <a href=\"https://leis.org/norma/1\" data-id=\"1\">Lei 1</a></p>");
    norma.setDataOriginal("2025-01-01");
    norma.setDataPublicacao("2025-01-02");
    norma.setUrl("https://leis.org/norma/999001");
    norma.setCidade("São Paulo");
    norma.setEstado("SP");

    repoNormas.salvarNorma(norma);
  }

  @Test
  @Order(1)
  @DisplayName("Testa integridade e inicialização dos repositórios via Fachada")
  void testInicializacaoRepositorios() {
    assertNotNull(repositorioFacade.getGerenciadorConexao());
    assertNotNull(repoNormas);
    assertNotNull(repoAutores);
    assertNotNull(repoEmentas);
    assertNotNull(repoRelacionamentos);
    assertNotNull(repoTopicos);
  }

  @Test
  @Order(2)
  @DisplayName("Testa RepositorioNormas: inserção, busca e mapeamento")
  void testRepositorioNormas() throws SQLException {
    Set<Long> ids = repoNormas.listarTodosIdsNormas();
    assertTrue(ids.contains(999001L));

    Map<String, Long> chaves = repoNormas.mapearChavesNormasParaId();
    assertTrue(chaves.containsKey("lei-ordinaria:2025:9991"));
    assertEquals(999001L, chaves.get("lei-ordinaria:2025:9991"));

    Map<Long, String> ementas = repoNormas.listarEmentasOriginais();
    assertTrue(ementas.containsKey(999001L));
    assertEquals("Dispõe sobre teste unitário de normas.", ementas.get(999001L));
  }

  @Test
  @Order(3)
  @DisplayName("Testa RepositorioAutores: salvar autor, cargo e normalização")
  void testRepositorioAutores() throws SQLException {
    long idAutor = repoAutores.salvarAutor("Fulano de Tal Teste");
    assertTrue(idAutor > 0);

    // Salvar novamente o mesmo autor deve retornar o mesmo ID
    long mesmoId = repoAutores.salvarAutor("FULANO DE TAL TESTE");
    assertEquals(idAutor, mesmoId);

    repoAutores.salvarNormaAutor(999001L, idAutor, "PREFEITO TESTE");

    List<String> cargos = repoAutores.listarCargosDistintos();
    assertTrue(cargos.contains("PREFEITO TESTE"));

    // Atualização de cargo normalizado
    int atualizados = repoAutores.atualizarCargosNormaAutor(Map.of("PREFEITO TESTE", "PREFEITO"));
    assertTrue(atualizados >= 1);

    Map<Long, String> cargosNorm = repoAutores.listarCargosNormalizadosAutores();
    assertEquals("PREFEITO", cargosNorm.get(idAutor));

    Map<Long, List<Long>> autoresNormas = repoAutores.listarAutoresComNormas();
    assertTrue(autoresNormas.containsKey(idAutor));
    assertTrue(autoresNormas.get(idAutor).contains(999001L));
  }

  @Test
  @Order(4)
  @DisplayName("Testa RepositorioEmentasPreProcessadas: lote e contagem")
  void testRepositorioEmentas() throws SQLException {
    EmentaPreProcessada item = EmentaPreProcessada.builder()
        .idNorma(999001L)
        .stopwordsTfidfTopk("teste normas unitario")
        .svdLsa("lsa teste normas")
        .build();

    repoEmentas.salvarEmentasPreProcessadasEmLote(List.of(item));

    assertTrue(repoEmentas.contarEmentasPreProcessadas() > 0);

    List<EmentaParaTopico> paraTopicos = repoEmentas.listarEmentasParaTopicos();
    assertFalse(paraTopicos.isEmpty());
    boolean achou = paraTopicos.stream().anyMatch(e -> e.idNorma() == 999001L);
    assertTrue(achou);
  }

  @Test
  @Order(5)
  @DisplayName("Testa RepositorioRelacionamentos: lote, contagem e limpeza")
  void testRepositorioRelacionamentos() throws SQLException {
    RelacionamentoNorma rel = new RelacionamentoNorma(
        999001L,
        null,
        "ALTERACAO",
        "https://leis.org/norma/1",
        "lei-ordinaria",
        2020,
        1234,
        "Altera a lei 1234 de 2020"
    );

    repoRelacionamentos.salvarRelacionamentosEmLote(List.of(rel));
    assertTrue(repoRelacionamentos.contarRelacionamentos() > 0);
  }

  @Test
  @Order(6)
  @DisplayName("Testa RepositorioTopicos: rotulos, topicos e classificações")
  void testRepositorioTopicos() throws SQLException {
    int idRotulo = repoTopicos.obterOuCriarIdRotuloTopico("Tema Teste Unitário");
    assertTrue(idRotulo > 0);

    Map<String, Integer> rotulos = repoTopicos.mapearRotulosParaIds();
    assertTrue(rotulos.containsKey("Tema Teste Unitário"));

    TopicoTematico topico = new TopicoTematico(
        99,
        idRotulo,
        "Tema Teste Unitário",
        List.of("termo1", "termo2", "termo3"),
        10L,
        1.5
    );
    repoTopicos.salvarTopicosDescobertosEmLote(List.of(topico));

    List<TopicoTematico> salvos = repoTopicos.listarTopicosSalvos();
    assertTrue(salvos.stream().anyMatch(t -> t.id() == 99));

    NormaTopico nt = new NormaTopico(
        999001L,
        99,
        idRotulo,
        "Tema Teste Unitário",
        0.95,
        "termo1, termo2"
    );
    repoTopicos.salvarTopicosNormasEmLote(List.of(nt));

    Map<Long, NormaTopico> mapaNT = repoTopicos.listarNormasTopicosComoMapa();
    assertTrue(mapaNT.containsKey(999001L));
    assertEquals(99, mapaNT.get(999001L).idTopico());
  }
}
