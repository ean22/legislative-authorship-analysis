package org.entryPoint.model;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RequestResult {
    private List<LawSumary> data;
    private String name_entity;
    private int hits;
    private int page;
    private int pages;
    private String status;
}
