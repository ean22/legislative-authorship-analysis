package org.entryPoint.repository;

import org.entryPoint.model.EmentaParaTopico;
import org.entryPoint.model.EmentaPreProcessada;
import org.entryPoint.model.Norma;
import org.entryPoint.model.NormaComEmenta;
import org.entryPoint.model.NormaComIntegra;
import org.entryPoint.model.NormaTopico;
import org.entryPoint.model.RelacionamentoNorma;
import org.entryPoint.model.TopicoTematico;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Fachada (Facade) consolidada que unifica o acesso aos repositórios especializados do sistema.
 */
public class RepositorioBancoDados {
  private final GerenciadorConexao gerenciadorConexao;
  private final RepositorioNormas repositorioNormas;
  private final RepositorioAutores repositorioAutores;
  private final RepositorioEmentasPreProcessadas repositorioEmentas;
  private final RepositorioRelacionamentos repositorioRelacionamentos;
  private final RepositorioTopicos repositorioTopicos;

  public RepositorioBancoDados() {
    this.gerenciadorConexao = new GerenciadorConexao();
    this.repositorioNormas = new RepositorioNormas(this.gerenciadorConexao);
    this.repositorioAutores = new RepositorioAutores(this.gerenciadorConexao);
    this.repositorioEmentas = new RepositorioEmentasPreProcessadas(this.gerenciadorConexao);
    this.repositorioRelacionamentos = new RepositorioRelacionamentos(this.gerenciadorConexao);
    this.repositorioTopicos = new RepositorioTopicos(this.gerenciadorConexao);
  }

  public RepositorioBancoDados(GerenciadorConexao gerenciadorConexao) {
    this.gerenciadorConexao = gerenciadorConexao;
    this.repositorioNormas = new RepositorioNormas(this.gerenciadorConexao);
    this.repositorioAutores = new RepositorioAutores(this.gerenciadorConexao);
    this.repositorioEmentas = new RepositorioEmentasPreProcessadas(this.gerenciadorConexao);
    this.repositorioRelacionamentos = new RepositorioRelacionamentos(this.gerenciadorConexao);
    this.repositorioTopicos = new RepositorioTopicos(this.gerenciadorConexao);
  }

  // Getters para repositórios específicos
  public GerenciadorConexao getGerenciadorConexao() {
    return gerenciadorConexao;
  }

  public RepositorioNormas getRepositorioNormas() {
    return repositorioNormas;
  }

  public RepositorioAutores getRepositorioAutores() {
    return repositorioAutores;
  }

  public RepositorioEmentasPreProcessadas getRepositorioEmentas() {
    return repositorioEmentas;
  }

  public RepositorioRelacionamentos getRepositorioRelacionamentos() {
    return repositorioRelacionamentos;
  }

  public RepositorioTopicos getRepositorioTopicos() {
    return repositorioTopicos;
  }

  // Delegações para Normas
  public void salvarNorma(Norma norma) {
    repositorioNormas.salvarNorma(norma);
  }

  public void listarNormas() {
    repositorioNormas.listarNormas();
  }

  public List<NormaComIntegra> listarNormasComIntegra() throws SQLException {
    return repositorioNormas.listarNormasComIntegra();
  }

  public List<NormaComEmenta> listarNormasComEmenta() throws SQLException {
    return repositorioNormas.listarNormasComEmenta();
  }

  public Set<Long> listarTodosIdsNormas() throws SQLException {
    return repositorioNormas.listarTodosIdsNormas();
  }

  public Map<String, Long> mapearChavesNormasParaId() throws SQLException {
    return repositorioNormas.mapearChavesNormasParaId();
  }

  public Map<Long, String> listarEmentasOriginais() throws SQLException {
    return repositorioNormas.listarEmentasOriginais();
  }

  // Delegações para Autores
  public long salvarAutor(String nome) throws SQLException {
    return repositorioAutores.salvarAutor(nome);
  }

  public void salvarNormaAutor(long idNorma, long idAutor, String cargo) throws SQLException {
    repositorioAutores.salvarNormaAutor(idNorma, idAutor, cargo);
  }

  public List<String> listarCargosDistintos() throws SQLException {
    return repositorioAutores.listarCargosDistintos();
  }

  public List<String> listarCargosPendentesNormalizacao() throws SQLException {
    return repositorioAutores.listarCargosPendentesNormalizacao();
  }

  public int atualizarCargosNormaAutor(Map<String, String> mapaDePara) throws SQLException {
    return repositorioAutores.atualizarCargosNormaAutor(mapaDePara);
  }

  public Map<Long, List<Long>> listarAutoresComNormas() throws SQLException {
    return repositorioAutores.listarAutoresComNormas();
  }

  public Map<Long, String> listarNomesAutores() throws SQLException {
    return repositorioAutores.listarNomesAutores();
  }

  public Map<Long, String> listarCargosNormalizadosAutores() throws SQLException {
    return repositorioAutores.listarCargosNormalizadosAutores();
  }

  // Delegações para Ementas Pré-processadas
  public void salvarEmentasPreProcessadasEmLote(List<EmentaPreProcessada> lote) throws SQLException {
    repositorioEmentas.salvarEmentasPreProcessadasEmLote(lote);
  }

  public long contarEmentasPreProcessadas() throws SQLException {
    return repositorioEmentas.contarEmentasPreProcessadas();
  }

  public long contarEmbeddings() throws SQLException {
    return repositorioEmentas.contarEmbeddings();
  }

  public void salvarEmbeddingsEmLote(Map<Long, String> mapaEmbeddings) throws SQLException {
    repositorioEmentas.salvarEmbeddingsEmLote(mapaEmbeddings);
  }

  public List<org.entryPoint.model.NormaComEmenta> listarNormasParaGeracaoEmbedding(boolean apenasPendentes, int limite) throws SQLException {
    return repositorioEmentas.listarNormasParaGeracaoEmbedding(apenasPendentes, limite);
  }

  public List<org.entryPoint.model.EmbeddingNorma> listarEmbeddingsParaClusterizacao() throws SQLException {
    return repositorioEmentas.listarEmbeddingsParaClusterizacao();
  }

  public List<EmentaParaTopico> listarEmentasParaTopicos() throws SQLException {
    return repositorioEmentas.listarEmentasParaTopicos();
  }

  // Delegações para Relacionamentos
  public void salvarRelacionamentosEmLote(List<RelacionamentoNorma> lote) throws SQLException {
    repositorioRelacionamentos.salvarRelacionamentosEmLote(lote);
  }

  public long contarRelacionamentos() throws SQLException {
    return repositorioRelacionamentos.contarRelacionamentos();
  }

  public void limparRelacionamentos() throws SQLException {
    repositorioRelacionamentos.limparRelacionamentos();
  }

  // Delegações para Tópicos
  public int obterOuCriarIdRotuloTopico(String nomeRotulo) throws SQLException {
    return repositorioTopicos.obterOuCriarIdRotuloTopico(nomeRotulo);
  }

  public Map<String, Integer> mapearRotulosParaIds() throws SQLException {
    return repositorioTopicos.mapearRotulosParaIds();
  }

  public void salvarTopicosDescobertosEmLote(List<TopicoTematico> topicos) throws SQLException {
    repositorioTopicos.salvarTopicosDescobertosEmLote(topicos);
  }

  public void salvarTopicosNormasEmLote(List<NormaTopico> lote) throws SQLException {
    repositorioTopicos.salvarTopicosNormasEmLote(lote);
  }

  public List<NormaTopico> listarNormasTopicos() throws SQLException {
    return repositorioTopicos.listarNormasTopicos();
  }

  public Map<Long, NormaTopico> listarNormasTopicosComoMapa() throws SQLException {
    return repositorioTopicos.listarNormasTopicosComoMapa();
  }

  public List<TopicoTematico> listarTopicosSalvos() throws SQLException {
    return repositorioTopicos.listarTopicosSalvos();
  }
}