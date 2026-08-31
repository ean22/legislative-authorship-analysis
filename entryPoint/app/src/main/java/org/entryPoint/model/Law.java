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

    private String data_original;
    private String data_publicacao;

    private String ementa;
    private String integra;

    private int numero;
    private String tipo_escrito;
    private String tipo_slug;

    private String titulo;
    private String slug;
    private String url;

    private String entidade_nome;
    private int local_id;
    private String local_slug;
}