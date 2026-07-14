package com.hcimprogression.companion;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class SyncService {
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public void exchangeCode(
            String apiBaseUrl,
            String code,
            BiConsumer<LinkResult, String> callback) {
        String normalized = code == null
                ? ""
                : code.replaceAll("[^A-Za-z0-9]", "").toUpperCase();

        String json = "{\"code\":\"" + escape(normalized) + "\"}";

        post(apiBaseUrl, "companion-link-exchange", null, json)
                .whenComplete((body, error) ->
                {
                    if (error != null) {
                        callback.accept(null, friendly(error));
                        return;
                    }

                    String token = stringValue(body, "token");
                    String displayName = stringValue(body, "displayName");

                    if (token.isEmpty()) {
                        callback.accept(null, errorValue(body));
                    } else {
                        callback.accept(
                                new LinkResult(token, displayName),
                                null
                        );
                    }
                });
    }

    public void syncLocation(
            String apiBaseUrl,
            String token,
            PlayerState state,
            Consumer<String> callback) {
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
                .whenComplete((body, error) ->
                {
                    if (error != null) {
                        callback.accept(friendly(error));
                    } else if (!body.contains("\"ok\":true")) {
                        callback.accept(errorValue(body));
                    } else {
                        callback.accept(null);
                    }
                });
    }

    public void syncAccount(
            String apiBaseUrl,
            String token,
            AccountSnapshot snapshot,
            BiConsumer<AccountSyncResult, String> callback) {
        StringBuilder skills = new StringBuilder("{");
        boolean firstSkill = true;

        for (Map.Entry<String, AccountSnapshot.SkillSnapshot> entry
                : snapshot.getSkills().entrySet()) {
            if (!firstSkill) {
                skills.append(',');
            }

            firstSkill = false;

            skills.append('"')
                    .append(escape(entry.getKey()))
                    .append("\":{")
                    .append("\"level\":")
                    .append(entry.getValue().getLevel())
                    .append(',')
                    .append("\"xp\":")
                    .append(entry.getValue().getXp())
                    .append('}');
        }

        skills.append('}');

        StringBuilder quests = new StringBuilder("[");

        if (snapshot.getCompletedQuests() != null) {
            for (int i = 0; i < snapshot.getCompletedQuests().size(); i++) {
                if (i > 0) {
                    quests.append(',');
                }

                quests.append('"')
                        .append(escape(snapshot.getCompletedQuests().get(i)))
                        .append('"');
            }
        }

        quests.append(']');

        StringBuilder clueCounts = new StringBuilder("{");
        boolean firstClue = true;
        for (Map.Entry<String, Integer> entry : snapshot.getClueCounts().entrySet()) {
            if (!firstClue) clueCounts.append(',');
            firstClue = false;
            clueCounts.append('"').append(escape(entry.getKey())).append("\":").append(entry.getValue());
        }
        clueCounts.append('}');

        StringBuilder collectionLog = new StringBuilder("{");
        boolean firstLog = true;
        for (Map.Entry<String, Integer> entry : snapshot.getCollectionLog().entrySet()) {
            if (!firstLog) collectionLog.append(',');
            firstLog = false;
            collectionLog.append('"').append(escape(entry.getKey())).append("\":").append(entry.getValue());
        }
        collectionLog.append('}');

        StringBuilder collectionLogItems = new StringBuilder("[");
        for (int i = 0; i < snapshot.getCollectionLogItems().size(); i++) {
            AccountSnapshot.CollectionLogItemSnapshot item = snapshot.getCollectionLogItems().get(i);
            if (i > 0) collectionLogItems.append(',');
            collectionLogItems.append('{')
                .append("\"itemId\":").append(item.getItemId()).append(',')
                .append("\"name\":\"").append(escape(item.getName())).append("\",")
                .append("\"category\":\"").append(escape(item.getCategory())).append("\",")
                .append("\"quantity\":").append(item.getQuantity())
                .append('}');
        }
        collectionLogItems.append(']');

        String json = "{"
                + "\"playerName\":\""
                + escape(snapshot.getPlayerName())
                + "\","
                + "\"questPoints\":"
                + snapshot.getQuestPoints()
                + ","
                + "\"skills\":"
                + skills
                + ","
                + "\"completedQuests\":"
                + quests
                + ",\"clueCounts\":"
                + clueCounts
                + ",\"collectionLog\":"
                + collectionLog
                + ",\"collectionLogItems\":"
                + collectionLogItems
                + "}";

        post(apiBaseUrl, "companion-account-sync", token, json)
                .whenComplete((body, error) ->
                {
                    if (error != null) {
                        callback.accept(null, friendly(error));
                        return;
                    }

                    if (!body.contains("\"ok\":true")) {
                        callback.accept(null, errorValue(body));
                        return;
                    }

                    callback.accept(
                            new AccountSyncResult(
                                    intValue(body, "questUpdates"),
                                    intValue(body, "taskUpdates")
                            ),
                            null
                    );
                });
    }

    private CompletableFuture<String> post(
            String baseUrl,
            String functionName,
            String token,
            String json) {
        String base = normalizeBaseUrl(baseUrl);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(base + "/" + functionName))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json));

        if (token != null && !token.isEmpty()) {
            builder.header("Authorization", "Bearer " + token);
        }

        HttpRequest request = builder.build();

        System.out.println(
                "[HCIM Companion] Sending POST to: " + request.uri()
        );

        return httpClient
                .sendAsync(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                )
                .thenApply(response ->
                {
                    System.out.println(
                            "[HCIM Companion] Response status: "
                                    + response.statusCode()
                    );

                    System.out.println(
                            "[HCIM Companion] Response body: "
                                    + response.body()
                    );

                    if (response.statusCode() < 200
                            || response.statusCode() >= 300) {
                        throw new RuntimeException(
                                "HTTP "
                                        + response.statusCode()
                                        + ": "
                                        + errorValue(response.body())
                        );
                    }

                    return response.body();
                });
    }

    private String normalizeBaseUrl(String value) {
        String base = value == null ? "" : value.trim();

        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }

        if (!base.startsWith("https://")) {
            throw new IllegalArgumentException(
                    "API URL must begin with https://"
            );
        }

        return base;
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", " ")
                .replace("\n", " ");
    }

    private static String stringValue(String json, String key) {
        if (json == null) {
            return "";
        }

        String marker = "\"" + key + "\":\"";
        int start = json.indexOf(marker);

        if (start < 0) {
            return "";
        }

        start += marker.length();
        int end = json.indexOf('"', start);

        return end < 0
                ? ""
                : json.substring(start, end);
    }

    private static int intValue(String json, String key) {
        if (json == null) {
            return 0;
        }

        String marker = "\"" + key + "\":";
        int start = json.indexOf(marker);

        if (start < 0) {
            return 0;
        }

        start += marker.length();

        while (start < json.length()
                && Character.isWhitespace(json.charAt(start))) {
            start++;
        }

        int end = start;

        while (end < json.length()
                && Character.isDigit(json.charAt(end))) {
            end++;
        }

        try {
            return Integer.parseInt(json.substring(start, end));
        } catch (RuntimeException ignored) {
            return 0;
        }
    }

    private static String errorValue(String json) {
        String value = stringValue(json, "error");

        return value.isEmpty()
                ? "Request failed."
                : value;
    }

    private static String friendly(Throwable error) {
        Throwable cause = error;

        while (cause.getCause() != null) {
            cause = cause.getCause();
        }

        String message = cause.getMessage();

        return message == null || message.isEmpty()
                ? "Network request failed."
                : message;
    }

    public static class AccountSyncResult {
        private final int questUpdates;
        private final int taskUpdates;

        public AccountSyncResult(
                int questUpdates,
                int taskUpdates) {
            this.questUpdates = questUpdates;
            this.taskUpdates = taskUpdates;
        }

        public int getQuestUpdates() {
            return questUpdates;
        }

        public int getTaskUpdates() {
            return taskUpdates;
        }
    }

    public static class LinkResult {
        private final String token;
        private final String displayName;

        public LinkResult(
                String token,
                String displayName) {
            this.token = token;
            this.displayName = displayName;
        }

        public String getToken() {
            return token;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}