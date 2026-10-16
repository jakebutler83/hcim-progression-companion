package com.hcimprogression.companion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

/** Client-thread-only coalescing. Network callbacks must return through ClientThread. */
final class PersonalBestSync
{
    interface Sender { void send(String name, String token, String api, List<PersonalBestRecord> records, BiConsumer<Integer,String> done); }
    private final BooleanSupplier enabled;
    private final Sender sender;
    private String connection = "", name = "", token = "", api = "", saved = "", pendingFingerprint = "";
    private List<PersonalBestRecord> pending = Collections.emptyList();
    private long generation, nextSendAt, heartbeatAt, readyAt;
    private boolean busy, queued;
    private int failures;
    private String status = "Off · enable Personal best sync";

    PersonalBestSync(BooleanSupplier enabled, Sender sender) { this.enabled = enabled; this.sender = sender; }
    void connection(String name, String token, String api, String profile)
    {
        String next = name + ":" + token + ":" + api + ":" + profile;
        if (next.equals(connection)) return;
        generation++; connection = next; this.name = name; this.token = token; this.api = api;
        saved = ""; pendingFingerprint = ""; pending = Collections.emptyList(); busy = queued = false;
        failures = 0; nextSendAt = heartbeatAt = readyAt = 0;
        status = enabled.getAsBoolean() ? "Waiting for this account's records" : "Off · enable Personal best sync";
    }
    void reset() { connection("", "", "", ""); }
    void offer(List<PersonalBestRecord> records, long now)
    {
        if (!enabled.getAsBoolean() || name.isEmpty() || token.isEmpty()) return;
        StringBuilder fingerprint = new StringBuilder();
        for (PersonalBestRecord record : records) fingerprint.append(record.worldMode).append('|').append(record.source).append('|').append(record.key).append('=').append(record.durationMs).append(';');
        String next = fingerprint.toString();
        if (next.equals(saved) && now < heartbeatAt) return;
        if (!queued || !next.equals(pendingFingerprint)) readyAt = now + 1200L;
        pending = new ArrayList<>(records); pendingFingerprint = next; queued = true;
        if (!busy) status = records.isEmpty() ? "Checking record availability…" : records.size() + " records queued";
    }
    void tick(long now)
    {
        if (!enabled.getAsBoolean())
        {
            if (queued || busy || !saved.isEmpty()) { generation++; busy = queued = false; saved = ""; pending = Collections.emptyList(); }
            status = "Off · enable Personal best sync"; return;
        }
        if (busy || !queued || now < Math.max(readyAt, nextSendAt)) return;
        final long revision = generation;
        final String fingerprint = pendingFingerprint;
        List<PersonalBestRecord> sending = new ArrayList<>(pending);
        queued = false; busy = true; nextSendAt = now + 10_000L; status = "Uploading personal records…";
        sender.send(name, token, api, sending, (count, error) -> {
            if (revision != generation) return;
            busy = false;
            if (!enabled.getAsBoolean()) { status = "Off · enable Personal best sync"; return; }
            if (error != null || count == null)
            {
                queued = true; failures = Math.min(5, failures + 1);
                long delay = Math.min(300_000L, 15_000L * (1L << failures));
                java.util.regex.Matcher retry = java.util.regex.Pattern.compile("Retry after (\\d+) seconds",java.util.regex.Pattern.CASE_INSENSITIVE).matcher(error == null ? "" : error);
                if (retry.find())
                {
                    try { delay = Math.max(delay, Math.min(86400L, Long.parseLong(retry.group(1))) * 1000L); }
                    catch (NumberFormatException ignored) { /* Keep the bounded fallback. */ }
                }
                nextSendAt = System.currentTimeMillis() + delay;
                status = "Upload unavailable · retrying safely"; return;
            }
            failures = 0; saved = fingerprint; heartbeatAt = System.currentTimeMillis() + 300_000L;
            status = sending.isEmpty() ? "No local PBs · enable Chat Commands" : count + " records saved to website";
        });
    }
    String status() { return status; }
}
