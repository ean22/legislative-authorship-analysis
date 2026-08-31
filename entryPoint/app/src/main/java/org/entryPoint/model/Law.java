package org.entryPoint.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Law {

    private long id;
    private int ano;
    private String cidade;
    private String estado;

    private String dataOriginal;
    private String dataPublicacao;

    private String ementa;
    private String integra;

    private int numero;
    private String tipoEscrito;
    private String tipoSlug;

    private String titulo;
    private String slug;
    private String url;

    private String entidadeNome;
    private int localId;
    private String localSlug;
}