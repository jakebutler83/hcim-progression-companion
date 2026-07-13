package com.hcimprogression.companion;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class SyncService
{
    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    public void exchangeCode(String apiBaseUrl, String code, BiConsumer<LinkResult, String> callback)
    {
        String normalized = code == null ? "" : code.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        String json = "{\"code\":\"" + escape(normalized) + "\"}";
        post(apiBaseUrl, "companion-link-exchange", null, json)
            .whenComplete((body, error) -> {
                if (error != null) {
                    callback.accept(null, friendly(error));
                    return;
                }
                String token = stringValue(body, "token");
                String displayName = stringValue(body, "displayName");
                if (token.isEmpty()) callback.accept(null, errorValue(body));
                else callback.accept(new LinkResult(token, displayName), null);
            });
    }

    public void syncLocation(String apiBaseUrl, String token, PlayerState state, Consumer<String> callback)
    {
        String json = "{"
            + "\"playerName\":\"" + escape(state.getPlayerName()) + "\","
            + "\"world\":" + state.getWorld() + ","
            + "\"regionId\":" + state.getRegionId() + ","
            + "\"x\":" + state.getX() + ","
            + "\"y\":" + state.getY() + ","
            + "\"plane\":" + state.getPlane() + ","
            + "\"timestamp\":" + state.getTimestamp()
            + "}";
        post(apiBaseUrl, "companion-location-sync", token, json)
            .whenComplete((body, error) -> {
                if (error != null) callback.accept(friendly(error));
                else if (!body.contains("\"ok\":true")) callback.accept(errorValue(body));
                else callback.accept(null);
            });
    }

    private CompletableFuture<String> post(String baseUrl, String functionName, String token, String json)
    {
        String base = normalizeBaseUrl(baseUrl);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create(base + "/" + functionName))
            .timeout(Duration.ofSeconds(15))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json));
        if (token != null && !token.isEmpty()) builder.header("Authorization", "Bearer " + token);

        return httpClient.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new RuntimeException(errorValue(response.body()));
                }
                return response.body();
            });
    }

    private String normalizeBaseUrl(String value)
    {
        String base = value == null ? "" : value.trim();
        while (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        if (!base.startsWith("https://")) throw new IllegalArgumentException("API URL must begin with https://");
        return base;
    }

    private static String escape(String value)
    {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ");
    }

    private static String stringValue(String json, String key)
    {
        if (json == null) return "";
        String marker = "\"" + key + "\":\"";
        int start = json.indexOf(marker);
        if (start < 0) return "";
        start += marker.length();
        int end = json.indexOf('"', start);
        return end < 0 ? "" : json.substring(start, end);
    }

    private static String errorValue(String json)
    {
        String value = stringValue(json, "error");
        return value.isEmpty() ? "Request failed." : value;
    }

    private static String friendly(Throwable error)
    {
        Throwable cause = error;
        while (cause.getCause() != null) cause = cause.getCause();
        String message = cause.getMessage();
        return message == null || message.isEmpty() ? "Network request failed." : message;
    }

    public static class LinkResult
    {
        private final String token;
        private final String displayName;

        public LinkResult(String token, String displayName)
        {
            this.token = token;
            this.displayName = displayName;
        }

        public String getToken() { return token; }
        public String getDisplayName() { return displayName; }
    }
}
