package com.hcimprogression.companion;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Produces stable fingerprints for uploaded account snapshots.
 *
 * <p>Some capture timestamps change every time RuneLite reads the account even
 * when the underlying character data did not change. Those transport-only
 * timestamps are excluded so automatic sync can safely reuse the last
 * successful per-character snapshot.</p>
 */
final class AccountSnapshotFingerprint
{
    private AccountSnapshotFingerprint()
    {
    }

    static String create(Gson gson, AccountSnapshot snapshot)
    {
        JsonElement serialized = gson.toJsonTree(snapshot);
        if (!serialized.isJsonObject())
        {
            return sha256(gson.toJson(serialized));
        }

        JsonObject root = serialized.getAsJsonObject();
        removeCaptureTimestamp(root, "slayer");
        removeCaptureTimestamp(root, "tcg");
        return sha256(gson.toJson(root));
    }

    static String linkKey(String token)
    {
        return sha256(token == null ? "" : token).substring(0, 12);
    }

    private static void removeCaptureTimestamp(JsonObject root, String property)
    {
        JsonElement value = root.get(property);
        if (value != null && value.isJsonObject())
        {
            value.getAsJsonObject().remove("updatedAt");
        }
    }

    private static String sha256(String value)
    {
        try
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(bytes.length * 2);
            for (byte item : bytes)
            {
                result.append(String.format("%02x", item & 0xff));
            }
            return result.toString();
        }
        catch (NoSuchAlgorithmException error)
        {
            throw new IllegalStateException("SHA-256 is unavailable", error);
        }
    }
}
