package org.entryPoint.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class RequestResult {
  private List<LawSumary> data;
  @JsonProperty("entidade_nome")
  private String name_entity;
  private int hits;
  private int page;
  private int pages;
  private String status;
}
