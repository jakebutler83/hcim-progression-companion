package com.hcimprogression.companion;

import com.google.gson.Gson;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

public class AccountSnapshotFingerprintTest
{
    private final Gson gson = new Gson();

    @Test
    public void ignoresCaptureOnlyTimestamps()
    {
        AccountSnapshot first = snapshot(100L, 200L, "Blue dragons", 42);
        AccountSnapshot second = snapshot(101L, 201L, "Blue dragons", 42);

        assertEquals(
            AccountSnapshotFingerprint.create(gson, first),
            AccountSnapshotFingerprint.create(gson, second)
        );
    }

    @Test
    public void detectsMeaningfulSnapshotChanges()
    {
        AccountSnapshot first = snapshot(100L, 200L, "Blue dragons", 42);
        AccountSnapshot differentTask = snapshot(100L, 200L, "Shades", 42);
        AccountSnapshot differentCards = snapshot(100L, 200L, "Blue dragons", 43);

        assertNotEquals(
            AccountSnapshotFingerprint.create(gson, first),
            AccountSnapshotFingerprint.create(gson, differentTask)
        );
        assertNotEquals(
            AccountSnapshotFingerprint.create(gson, first),
            AccountSnapshotFingerprint.create(gson, differentCards)
        );
    }

    @Test
    public void createsStableNonSecretLinkKey()
    {
        String token = "private-link-token";
        String first = AccountSnapshotFingerprint.linkKey(token);

        assertEquals(first, AccountSnapshotFingerprint.linkKey(token));
        assertEquals(12, first.length());
        assertFalse(first.contains(token));
        assertNotEquals(first, AccountSnapshotFingerprint.linkKey("another-token"));
    }

    private static AccountSnapshot snapshot(long slayerUpdatedAt, long tcgUpdatedAt, String task, int totalCards)
    {
        AccountSnapshot snapshot = new AccountSnapshot();
        snapshot.setPlayerName("HP Shooter");
        snapshot.setSlayer(new SlayerSnapshot(task, 12, 100, 10, "Vannaka", 1, slayerUpdatedAt));

        TcgCollectionSnapshot tcg = new TcgCollectionSnapshot();
        tcg.setAvailable(true);
        tcg.setUpdatedAt(tcgUpdatedAt);
        tcg.setTotalCardsOwned(totalCards);
        snapshot.setTcg(tcg);
        return snapshot;
    }
}
