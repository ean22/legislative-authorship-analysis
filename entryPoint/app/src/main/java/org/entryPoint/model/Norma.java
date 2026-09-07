package org.entryPoint.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class Norma {

    private long id;
    private int ano;
    private String cidade;
    private String estado;

    @JsonProperty("data_original")
    private String dataOriginal;

    @JsonProperty("data_publicacao")
    private String dataPublicacao;

    private String ementa;
    private String integra;

    private int numero;

    @JsonProperty("tipo_escrito")
    private String tipoEscrito;

    @JsonProperty("tipo_slug")
    private String tipoSlug;

    private String titulo;
    private String slug;
    private String url;

    @JsonProperty("entidade_nome")
    private String entidadeNome;

    @JsonProperty("local_id")
    private int localId;

    @JsonProperty("local_slug")
    private String localSlug;
}