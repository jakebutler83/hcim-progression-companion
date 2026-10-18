package com.hcimprogression.companion;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.runelite.api.ChatMessageType;
import net.runelite.client.util.Text;

/** Client-thread-only observed rewards, not historical page imports. */
final class CollectionUnlockSync
{
    static final class Unlock
    {
        int itemId;
        String name;
        long observedAt;
        Unlock(String name, long at) { this.name = name; observedAt = at; }
    }
    interface Sender { void send(List<Unlock> items, BiConsumer<Boolean,String> done); }
    private final Map<String, Unlock> pending = new LinkedHashMap<>();
    private final Consumer<List<Unlock>> persist;
    private long generation, readyAt, nextSendAt;
    private boolean busy;
    private int failures;

    CollectionUnlockSync(Consumer<List<Unlock>> persist) { this.persist = persist; }
    static String chatName(ChatMessageType type, String message)
    {
        if (type != ChatMessageType.GAMEMESSAGE && type != ChatMessageType.SPAM) return "";
        String clean = clean(message), prefix = "New item added to your collection log: ";
        return clean.startsWith(prefix) ? validName(clean.substring(prefix.length())) : "";
    }
    static String popupName(String title, String message)
    {
        String clean = clean(message);
        return clean(title).equalsIgnoreCase("Collection log") && clean.startsWith("New item:")
            ? validName(clean.substring("New item:".length())) : "";
    }
    static String clean(String value) { return value == null ? "" : Text.removeTags(value).replace('\u00a0', ' ').trim(); }
    static String validName(String value)
    {
        String name = clean(value);
        return name.isEmpty() || name.length() > 80 || name.matches(".*[\\x00-\\x1f<>].*") ? "" : name;
    }
    static String key(String name) { return name.toLowerCase(Locale.ROOT); }
    void reset(List<Unlock> saved, long now)
    {
        generation++; pending.clear(); busy = false; failures = 0; nextSendAt = 0; readyAt = now + 1800;
        for (Unlock item : saved)
        {
            if (item != null && !validName(item.name).isEmpty() && item.observedAt >= now - 7 * 86400000L
                && item.observedAt <= now + 300000 && pending.size() < 40)
            {
                if (item.itemId < 0 || item.itemId > 500000) item.itemId = 0;
                pending.putIfAbsent(key(item.name), item);
            }
        }
    }
    void observe(String name, long now)
    {
        name = validName(name);
        if (name.isEmpty() || pending.containsKey(key(name)) || pending.size() >= 40) return;
        pending.put(key(name), new Unlock(name, now)); readyAt = now + 1800; save();
    }
    List<Unlock> items() { return new ArrayList<>(pending.values()); }
    void resolve(String name, int id)
    {
        Unlock item = pending.get(key(name));
        if (item != null && item.itemId == 0 && id > 0 && id <= 500000) { item.itemId = id; save(); }
    }
    private void save() { persist.accept(items()); }
    void tick(long now, Sender sender)
    {
        if (pending.values().removeIf(item -> item.observedAt < now - 7 * 86400000L)) save();
        if (busy || now < Math.max(readyAt, nextSendAt)) return;
        List<Unlock> batch = new ArrayList<>();
        for (Unlock item : pending.values()) if (item.itemId > 0) batch.add(item);
        if (batch.isEmpty()) return;
        long revision = generation; busy = true; nextSendAt = now + 10000;
        sender.send(batch, (ok, error) -> {
            if (revision != generation) return;
            busy = false;
            if (!Boolean.TRUE.equals(ok))
            {
                failures = Math.min(5, failures + 1);
                long delay = Math.min(300000L, 15000L * (1L << failures));
                java.util.regex.Matcher retry = java.util.regex.Pattern.compile("Retry after (\\d+) seconds", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(error == null ? "" : error);
                if (retry.find())
                {
                    try { delay = Math.max(delay, Math.min(86400L, Long.parseLong(retry.group(1))) * 1000); }
                    catch (NumberFormatException ignored) { /* Keep bounded backoff. */ }
                }
                nextSendAt = System.currentTimeMillis() + delay; return;
            }
            failures = 0;
            for (Unlock item : batch) if (pending.get(key(item.name)) == item) pending.remove(key(item.name));
            save();
        });
    }
}
