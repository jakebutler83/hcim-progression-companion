package com.hcimprogression.companion;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import net.runelite.client.RuneLite;

/** Reads the share-safe fields from the OSRS TCG plugin's local tcg.save file. */
public class TcgCollectionSnapshotService
{
    private static final String PREFIX = "RLTCG_v2:";
    private static final byte[] XOR_SALT = {
        (byte) 0x52, (byte) 0x4c, (byte) 0x54, (byte) 0x43, (byte) 0x47,
        (byte) 0x7c, (byte) 0x6f, (byte) 0x73, (byte) 0x72, (byte) 0x73,
        (byte) 0x2d, (byte) 0x74, (byte) 0x63, (byte) 0x67, (byte) 0x21
    };
    private static final int CARD_POOL = 6376;
    private static final int MAX_CARDS = 3000;

    public TcgCollectionSnapshot read(String rsProfileKey)
    {
        Path file = locateSave(rsProfileKey);
        if (file == null) return TcgCollectionSnapshot.unavailable();
        try
        {
            String encoded = Files.readString(file, StandardCharsets.UTF_8).trim();
            String json = decode(encoded);
            if (json.isEmpty()) return TcgCollectionSnapshot.unavailable();
            JsonObject root = new JsonParser().parse(json).getAsJsonObject();
            TcgCollectionSnapshot snapshot = new TcgCollectionSnapshot();
            snapshot.setAvailable(true);
            snapshot.setSchemaVersion(integer(root, "schemaVersion"));
            snapshot.setCredits(longValue(root, "credits"));
            snapshot.setOpenedPacks(longValue(root, "openedPacks"));
            snapshot.setTotalCardPool(CARD_POOL);
            snapshot.setUpdatedAt(Files.getLastModifiedTime(file).toMillis());

            Set<String> unique = new HashSet<>();
            Set<String> foilUnique = new HashSet<>();
            JsonArray entries = root.has("cardEntries") && root.get("cardEntries").isJsonArray()
                ? root.getAsJsonArray("cardEntries") : new JsonArray();
            for (JsonElement element : entries)
            {
                if (snapshot.getCards().size() >= MAX_CARDS || !element.isJsonObject()) continue;
                JsonObject entry = element.getAsJsonObject();
                String name = stringValue(entry, "cardName").trim();
                if (name.isEmpty() || name.length() > 80) continue;
                JsonArray variants = entry.has("variants") && entry.get("variants").isJsonArray()
                    ? entry.getAsJsonArray("variants") : new JsonArray();
                int normal = 0;
                int foil = 0;
                for (JsonElement variantElement : variants)
                {
                    if (!variantElement.isJsonObject()) continue;
                    JsonObject variant = variantElement.getAsJsonObject();
                    int quantity = Math.max(1, integer(variant, "quantity"));
                    if (variant.has("foil") && variant.get("foil").isJsonPrimitive() && variant.get("foil").getAsBoolean()) foil += quantity;
                    else normal += quantity;
                }
                if (normal > 0)
                {
                    snapshot.getCards().add(new TcgCollectionSnapshot.CardSnapshot(name, false, normal));
                    snapshot.setTotalCardsOwned(snapshot.getTotalCardsOwned() + normal);
                    unique.add(name.toLowerCase(Locale.ROOT));
                }
                if (foil > 0 && snapshot.getCards().size() < MAX_CARDS)
                {
                    snapshot.getCards().add(new TcgCollectionSnapshot.CardSnapshot(name, true, foil));
                    snapshot.setTotalCardsOwned(snapshot.getTotalCardsOwned() + foil);
                    unique.add(name.toLowerCase(Locale.ROOT));
                    foilUnique.add(name.toLowerCase(Locale.ROOT));
                }
            }
            snapshot.setUniqueOwned(unique.size());
            snapshot.setUniqueFoilOwned(foilUnique.size());
            snapshot.setCompletionPct(CARD_POOL == 0 ? 0d : unique.size() * 100d / CARD_POOL);
            snapshot.setFoilCompletionPct(CARD_POOL == 0 ? 0d : foilUnique.size() * 100d / CARD_POOL);
            return snapshot;
        }
        catch (Exception ignored)
        {
            return TcgCollectionSnapshot.unavailable();
        }
    }

    private Path locateSave(String profileKey)
    {
        Path root = RuneLite.RUNELITE_DIR.toPath().resolve("OSRS-TCG").resolve("backups");
        String hashed = sha256(profileKey);
        Path profile = hashed == null ? null : root.resolve(hashed).resolve("tcg.save");
        if (profile != null && Files.isRegularFile(profile)) return profile;
        Path fallback = root.resolve("default").resolve("tcg.save");
        return Files.isRegularFile(fallback) ? fallback : null;
    }

    private String decode(String stored) throws IOException
    {
        if (stored.length() <= PREFIX.length() || !stored.startsWith(PREFIX)) return "";
        byte[] compressed = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
        for (int i = 0; i < compressed.length; i++) compressed[i] ^= XOR_SALT[i % XOR_SALT.length];
        try (java.util.zip.GZIPInputStream gzip = new java.util.zip.GZIPInputStream(new java.io.ByteArrayInputStream(compressed)))
        {
            return new String(gzip.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String sha256(String value)
    {
        if (value == null || value.isEmpty()) return null;
        try
        {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder(64);
            for (byte b : digest) out.append(String.format("%02x", b));
            return out.toString();
        }
        catch (NoSuchAlgorithmException ignored) { return null; }
    }

    private static int integer(JsonObject object, String key)
    {
        try { return object.has(key) ? Math.max(0, object.get(key).getAsInt()) : 0; }
        catch (RuntimeException ignored) { return 0; }
    }

    private static long longValue(JsonObject object, String key)
    {
        try { return object.has(key) ? Math.max(0L, object.get(key).getAsLong()) : 0L; }
        catch (RuntimeException ignored) { return 0L; }
    }

    private static String stringValue(JsonObject object, String key)
    {
        try { return object.has(key) ? object.get(key).getAsString() : ""; }
        catch (RuntimeException ignored) { return ""; }
    }
}
