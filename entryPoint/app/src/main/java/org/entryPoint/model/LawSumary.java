package org.entryPoint.model;

import java.net.URL;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class LawSumary {
  @JsonProperty("ambito")
  private int scope;

  @JsonProperty("ano")
  private int year;

  @JsonProperty("data")
  private String date;

  @JsonProperty("ementa")
  private String summary;

  @JsonProperty("id")
  private long id;

  @JsonProperty("local")
  private int location;

  @JsonProperty("numero")
  private int number;

  @JsonProperty("slug")
  private String slug;

  @JsonProperty("tipo")
  private int type;

  @JsonProperty("tipo_escrito")
  private String typeWritten;

  @JsonProperty("tipo_slug")
  private String typeSlug;

  @JsonProperty("url")
  private URL url;
}
