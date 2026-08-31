package org.entryPoint;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class LawsSource {  
public static void access() throws IOException, InterruptedException {

    String url =
        "https://api.v2.leismunicipais.com.br/v2/municipais/normas/_search";

    String json = """
        {
            "limit": 8,
            "page": 1,
            "q": "2026",
            "data_final":"2025-12-31",
            "data_inicial":"2025-01-01",
            "score_first": true,
            "cidade": 5298,
            "sort": ["ano", "numero"]
        }
        """;

    HttpClient client = HttpClient.newHttpClient();

    HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();

    HttpResponse<String> response =
            client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
            );

    System.out.println("Status: " + response.statusCode());
    System.out.println(response.body());
}
}
