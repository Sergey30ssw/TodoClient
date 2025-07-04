package com.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.*;

public class TodoClient {
    private static final String BASE_URL = "https://jsonplaceholder.typicode.com";
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) {
        try {
            System.out.println("Starting TodoClient with retry mechanism...");

            // Пытаемся выполнить запрос с 3 попытками
            Todo[] todos = executeWithRetry(TodoClient::getTodos, 3);

            if (todos != null) {
                System.out.println("\nFirst 5 todos:");
                for (int i = 0; i < Math.min(5, todos.length); i++) {
                    System.out.println("ID: " + todos[i].id + ", Title: " + todos[i].title);
                }
            } else {
                System.err.println("Failed to get todos after several attempts");
            }

        } catch (Exception e) {
            System.err.println("Fatal error: " + e.getMessage());
        }
    }

    private static Todo[] getTodos() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/todos"))
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), Todo[].class);
        }
        throw new IOException("GET failed with status: " + response.statusCode());
    }

    private static <T> T executeWithRetry(CheckedSupplier<T> supplier, int maxAttempts) {
        int attempt = 0;
        long delay = 2000; // Начальная задержка 2 секунды

        while (attempt < maxAttempts) {
            attempt++;
            try {
                System.out.printf("Attempt %d of %d...\n", attempt, maxAttempts);
                return supplier.get();
            } catch (Exception e) {
                System.err.println("Attempt failed: " + e.getMessage());
                if (attempt < maxAttempts) {
                    try {
                        Thread.sleep(delay);
                        delay *= 2; // Увеличиваем задержку при каждой попытке
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Operation interrupted", ie);
                    }
                }
            }
        }
        return null;
    }

    @FunctionalInterface
    private interface CheckedSupplier<T> {
        T get() throws IOException, InterruptedException;
    }

    record Todo(int id, String title, boolean completed) {}
}