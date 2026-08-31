package org.entryPoint.model;

import java.net.URL;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LawSumary {
    private int scope;
    private int year;
    private String date;
    private String summary;
    private long id;
    private int location;
    private int number;
    private String slug;
    private int type;
    private String typeWritten;
    private String typeSlug;
    private URL url;

    // getters e setters
}
