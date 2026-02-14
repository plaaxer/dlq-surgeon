package dev.plaaxer.dlqsurgeon.client;

import dev.plaaxer.dlqsurgeon.cli.ConnectOptions;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;

/**
 * Low-level HTTP client for the RabbitMQ Management API.
 *
 * Handles connection setup, authentication, and response validation.
 * All methods return the raw response body as a String — JSON parsing
 * is left to the caller (ManagementClient).
 */
class ApiHttpClient {

    private final HttpClient http;
    private final String baseUrl;
    private final String authHeader;

    ApiHttpClient(ConnectOptions opts) {
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        this.baseUrl = "http://" + opts.host + ":" + opts.managementPort + "/api";

        String credentials = opts.user + ":" + new String(opts.password);
        this.authHeader = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());
    }

    String get(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Authorization", authHeader)
                .header("Accept", "application/json")
                .GET()
                .build();

        return send(request);
    }

    String post(String path, String jsonBody) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Authorization", authHeader)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        return send(request);
    }

    private String send(HttpRequest request) throws IOException, InterruptedException {
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            throw new RuntimeException("Management API error " + status + ": " + response.body());
        }
        return response.body();
    }
}