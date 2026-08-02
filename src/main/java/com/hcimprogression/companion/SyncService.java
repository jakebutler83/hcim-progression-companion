package com.hcimprogression.companion;

import com.google.gson.Gson;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Slf4j
public class SyncService {
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final OkHttpClient httpClient;
    private final Gson gson;

    @Inject
    public SyncService(OkHttpClient httpClient, Gson gson) {
        this.httpClient = httpClient;
        this.gson = gson;
    }

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
                    String displayName = stringValue(body, "characterName");
                    if (displayName.isEmpty()) {
                        displayName = stringValue(body, "displayName");
                    }

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

    public void syncLive(
            String apiBaseUrl,
            String token,
            PlayerState location,
            SocialPresenceSnapshot presence,
            boolean locationEnabled,
            boolean presenceEnabled,
            Consumer<String> callback) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("locationEnabled", locationEnabled && location != null);
        payload.put("presenceEnabled", presenceEnabled && presence != null);
        if (locationEnabled && location != null) {
            payload.put("location", location);
        }
        if (presenceEnabled && presence != null) {
            payload.put("presence", presence);
        }

        boolean netlifyFunctions = apiBaseUrl != null && apiBaseUrl.contains("/.netlify/functions");
        String endpoint = netlifyFunctions
                ? "companion-live-batch"
                : "v2/live";
        post(apiBaseUrl, endpoint, token, gson.toJson(payload))
                .whenComplete((body, error) ->
                {
                    if (error != null) {
                        if (netlifyFunctions && friendly(error).contains("HTTP 404")) {
                            syncLegacyLive(apiBaseUrl, token, location, presence,
                                    locationEnabled, presenceEnabled, callback);
                            return;
                        }
                        callback.accept(friendly(error));
                    } else if (!body.contains("\"ok\":true")) {
                        callback.accept(errorValue(body));
                    } else {
                        callback.accept(null);
                    }
                });
    }

    private void syncLegacyLive(
            String apiBaseUrl,
            String token,
            PlayerState location,
            SocialPresenceSnapshot presence,
            boolean locationEnabled,
            boolean presenceEnabled,
            Consumer<String> callback) {
        int requestCount = (locationEnabled && location != null ? 1 : 0)
                + (presenceEnabled && presence != null ? 1 : 0);
        if (requestCount == 0) {
            callback.accept("No live-sync features were enabled.");
            return;
        }

        AtomicInteger remaining = new AtomicInteger(requestCount);
        AtomicReference<String> firstError = new AtomicReference<>();
        Consumer<String> completion = error -> {
            if (error != null) {
                firstError.compareAndSet(null, error);
            }
            if (remaining.decrementAndGet() == 0) {
                callback.accept(firstError.get());
            }
        };

        if (locationEnabled && location != null) {
            syncLocation(apiBaseUrl, token, location, completion);
        }
        if (presenceEnabled && presence != null) {
            syncSocialPresence(apiBaseUrl, token, presence, completion);
        }
    }

    public void syncSocialPresence(
            String apiBaseUrl,
            String token,
            SocialPresenceSnapshot snapshot,
            Consumer<String> callback) {
        StringBuilder equipment = new StringBuilder("{");
        boolean firstItem = true;

        for (Map.Entry<String, SocialPresenceSnapshot.EquipmentItem> entry
                : snapshot.getEquipment().entrySet()) {
            if (!firstItem) {
                equipment.append(',');
            }
            firstItem = false;
            equipment.append('\"')
                    .append(escape(entry.getKey()))
                    .append("\":{")
                    .append("\"itemId\":")
                    .append(entry.getValue().getItemId())
                    .append(',')
                    .append("\"rawItemId\":")
                    .append(entry.getValue().getRawItemId())
                    .append(',')
                    .append("\"name\":\"")
                    .append(escape(entry.getValue().getName()))
                    .append("\"}");
        }
        equipment.append('}');

        String json = "{"
                + "\"playerName\":\"" + escape(snapshot.getPlayerName()) + "\","
                + "\"world\":" + snapshot.getWorld() + ","
                + "\"regionId\":" + snapshot.getRegionId() + ","
                + "\"regionName\":\"" + escape(snapshot.getRegionName()) + "\","
                + "\"combatLevel\":" + snapshot.getCombatLevel() + ","
                + "\"activity\":\"" + escape(snapshot.getActivity()) + "\","
                + "\"inWilderness\":" + snapshot.isInWilderness() + ","
                + "\"exactLocationIncluded\":" + snapshot.isExactLocationIncluded() + ","
                + "\"x\":" + snapshot.getX() + ","
                + "\"y\":" + snapshot.getY() + ","
                + "\"plane\":" + snapshot.getPlane() + ","
                + "\"timestamp\":" + snapshot.getTimestamp() + ","
                + "\"equipment\":" + equipment
                + "}";

        post(apiBaseUrl, "companion-social-presence-sync", token, json)
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



    public void syncSocialClan(
            String apiBaseUrl,
            String token,
            SocialClanSnapshot snapshot,
            Consumer<String> callback) {
        StringBuilder members = new StringBuilder("[");
        boolean firstMember = true;
        for (SocialClanSnapshot.ClanMemberSnapshot member : snapshot.getMembers()) {
            if (!firstMember) {
                members.append(',');
            }
            firstMember = false;
            members.append('{')
                    .append("\"name\":\"").append(escape(member.getName())).append("\",")
                    .append("\"rank\":\"").append(escape(member.getRank())).append("\",")
                    .append("\"world\":").append(member.getWorld()).append(',')
                    .append("\"online\":").append(member.isOnline()).append(',')
                    .append("\"joinDate\":\"").append(escape(member.getJoinDate())).append("\"")
                    .append('}');
        }
        members.append(']');

        String json = "{"
                + "\"clanName\":\"" + escape(snapshot.getClanName()) + "\","
                + "\"playerRank\":\"" + escape(snapshot.getPlayerRank()) + "\","
                + "\"timestamp\":" + snapshot.getTimestamp() + ","
                + "\"members\":" + members
                + "}";

        post(apiBaseUrl, "companion-social-clan-sync", token, json)
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

    public void syncClanEvents(
            String apiBaseUrl,
            String token,
            SocialClanEventSnapshot snapshot,
            Consumer<String> callback) {
        StringBuilder events = new StringBuilder("[");
        boolean firstEvent = true;
        for (SocialClanEventSnapshot.ClanEventSnapshot clanEvent : snapshot.getEvents()) {
            if (!firstEvent) {
                events.append(',');
            }
            firstEvent = false;
            events.append('{')
                    .append("\"title\":\"").append(escape(clanEvent.getTitle())).append("\",")
                    .append("\"type\":\"").append(escape(clanEvent.getType())).append("\",")
                    .append("\"subType\":\"").append(escape(clanEvent.getSubType())).append("\",")
                    .append("\"requiredRank\":\"").append(escape(clanEvent.getRequiredRank())).append("\",")
                    .append("\"createdBy\":\"").append(escape(clanEvent.getCreatedBy())).append("\",")
                    .append("\"world\":").append(clanEvent.getWorld()).append(',')
                    .append("\"durationDays\":").append(clanEvent.getDurationDays()).append(',')
                    .append("\"startAt\":").append(clanEvent.getStartAt()).append(',')
                    .append("\"endAt\":").append(clanEvent.getEndAt())
                    .append('}');
        }
        events.append(']');

        String json = "{"
                + "\"clanName\":\"" + escape(snapshot.getClanName()) + "\","
                + "\"importedBy\":\"" + escape(snapshot.getImportedBy()) + "\","
                + "\"timestamp\":" + snapshot.getTimestamp() + ","
                + "\"events\":" + events
                + "}";

        post(apiBaseUrl, "companion-clan-events-sync", token, json)
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

        StringBuilder bossKillCounts = new StringBuilder("{");
        boolean firstBoss = true;
        for (Map.Entry<String, Integer> entry : snapshot.getBossKillCounts().entrySet()) {
            if (!firstBoss) bossKillCounts.append(',');
            firstBoss = false;
            bossKillCounts.append('"').append(escape(entry.getKey())).append("\":").append(entry.getValue());
        }
        bossKillCounts.append('}');

        StringBuilder diaryCompletions = new StringBuilder("{");
        boolean firstDiary = true;
        for (Map.Entry<String, Boolean> entry : snapshot.getDiaryCompletions().entrySet()) {
            if (!firstDiary) diaryCompletions.append(',');
            firstDiary = false;
            diaryCompletions.append('"').append(escape(entry.getKey())).append("\":").append(entry.getValue());
        }
        diaryCompletions.append('}');

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
                + "\"lootTrackingEnabled\":"
                + snapshot.isLootTrackingEnabled()
                + ","
                + "\"lootValueTotal\":"
                + snapshot.getLootValueTotal()
                + ","
                + "\"lootDropCountTotal\":"
                + snapshot.getLootDropCountTotal()
                + ","
                + "\"lootTrackedSince\":"
                + snapshot.getLootTrackedSince()
                + ","
                + "\"lastTearsVisitAt\":"
                + snapshot.getLastTearsVisitAt()
                + ",\"birdhouses\":"
                + birdhousesJson(snapshot.getBirdhouses())
                + ",\"tcg\":"
                + tcgJson(snapshot.getTcg())
                + ","
                + "\"skills\":"
                + skills
                + ","
                + "\"completedQuests\":"
                + quests
                + ",\"clueCounts\":"
                + clueCounts
                + ",\"bossKillCounts\":"
                + bossKillCounts
                + ",\"diaryCompletions\":"
                + diaryCompletions
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
                                    intValue(body, "taskUpdates"),
                                    intValue(body, "bossKillCountCount")
                            ),
                            null
                    );
                });
    }

    private String birdhousesJson(BirdhouseSnapshot snapshot)
    {
        if (snapshot == null) return "null";
        StringBuilder houses = new StringBuilder("[");
        for (int i = 0; i < snapshot.getHouses().size(); i++)
        {
            BirdhouseSnapshot.House house = snapshot.getHouses().get(i);
            if (i > 0) houses.append(',');
            houses.append('{')
                .append("\"name\":\"").append(escape(house.getName())).append("\",")
                .append("\"state\":\"").append(escape(house.getState())).append("\",")
                .append("\"lastChangedAt\":").append(house.getLastChangedAt()).append(',')
                .append("\"readyAt\":").append(house.getReadyAt())
                .append('}');
        }
        houses.append(']');
        return "{\"trackedCount\":" + snapshot.getTrackedCount()
            + ",\"seededCount\":" + snapshot.getSeededCount()
            + ",\"readyCount\":" + snapshot.getReadyCount()
            + ",\"nextReadyAt\":" + snapshot.getNextReadyAt()
            + ",\"houses\":" + houses + "}";
    }

    private String tcgJson(TcgCollectionSnapshot snapshot)
    {
        if (snapshot == null || !snapshot.isAvailable()) return "null";
        StringBuilder cards = new StringBuilder("[");
        for (int i = 0; i < snapshot.getCards().size(); i++)
        {
            if (i > 0) cards.append(',');
            TcgCollectionSnapshot.CardSnapshot card = snapshot.getCards().get(i);
            cards.append('{')
                .append("\"name\":\"").append(escape(card.getName())).append("\",")
                .append("\"foil\":").append(card.isFoil()).append(',')
                .append("\"quantity\":").append(card.getQuantity())
                .append('}');
        }
        cards.append(']');
        return "{"
            + "\"available\":true,"
            + "\"schemaVersion\":" + snapshot.getSchemaVersion() + ','
            + "\"credits\":" + snapshot.getCredits() + ','
            + "\"openedPacks\":" + snapshot.getOpenedPacks() + ','
            + "\"totalCardsOwned\":" + snapshot.getTotalCardsOwned() + ','
            + "\"uniqueOwned\":" + snapshot.getUniqueOwned() + ','
            + "\"uniqueFoilOwned\":" + snapshot.getUniqueFoilOwned() + ','
            + "\"totalCardPool\":" + snapshot.getTotalCardPool() + ','
            + "\"completionPct\":" + snapshot.getCompletionPct() + ','
            + "\"foilCompletionPct\":" + snapshot.getFoilCompletionPct() + ','
            + "\"updatedAt\":" + snapshot.getUpdatedAt() + ','
            + "\"cards\":" + cards
            + "}";
    }

    public void syncGroupStorage(
            String apiBaseUrl,
            String token,
            GroupStorageSnapshot snapshot,
            Consumer<String> callback) {
        post(apiBaseUrl, "companion-group-storage-sync", token, gson.toJson(snapshot))
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

    public void syncPersonalBank(
            String apiBaseUrl,
            String token,
            PersonalBankSnapshot snapshot,
            Consumer<String> callback) {
        post(apiBaseUrl, "companion-personal-bank-sync", token, gson.toJson(snapshot))
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

    private CompletableFuture<String> post(
            String baseUrl,
            String functionName,
            String token,
            String json) {
        String base = normalizeBaseUrl(baseUrl);
        Request.Builder builder = new Request.Builder()
                .url(base + "/" + functionName)
                .post(RequestBody.create(JSON, json));

        if (token != null && !token.isEmpty()) {
            builder.addHeader("Authorization", "Bearer " + token);
        }

        Request request = builder.build();

        log.debug("Sending POST to: {}", request.url());
        CompletableFuture<String> future = new CompletableFuture<>();
        httpClient.newCall(request).enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException error)
            {
                future.completeExceptionally(error);
            }

            @Override
            public void onResponse(Call call, Response response)
            {
                try (Response closeableResponse = response)
                {
                    String responseBody = closeableResponse.body() == null
                        ? ""
                        : closeableResponse.body().string();
                    log.debug("Response status: {}", closeableResponse.code());
                    log.debug("Response body: {}", responseBody);

                    if (!closeableResponse.isSuccessful()) {
                        future.completeExceptionally(new RuntimeException(
                                "HTTP "
                                        + closeableResponse.code()
                                        + ": "
                                        + errorValue(responseBody)
                        ));
                        return;
                    }
                    future.complete(responseBody);
                }
                catch (IOException error)
                {
                    future.completeExceptionally(error);
                }
            }
        });
        return future;
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
        private final int bossKillCountCount;

        public AccountSyncResult(
                int questUpdates,
                int taskUpdates,
                int bossKillCountCount) {
            this.questUpdates = questUpdates;
            this.taskUpdates = taskUpdates;
            this.bossKillCountCount = bossKillCountCount;
        }

        public int getQuestUpdates() {
            return questUpdates;
        }

        public int getTaskUpdates() {
            return taskUpdates;
        }

        public int getBossKillCountCount() {
            return bossKillCountCount;
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
