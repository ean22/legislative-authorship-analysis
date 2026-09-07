package org.entryPoint.model;

import java.net.URL;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResumoNorma {
  private int ambito;
  private int ano;
  private String data;
  private String ementa;
  private long id;
  private int local;
  private int numero;
  private String slug;
  private int tipo;

  @JsonProperty("tipo_escrito")
  private String tipoEscrito;

  @JsonProperty("tipo_slug")
  private String tipoSlug;
  private URL url;
}
