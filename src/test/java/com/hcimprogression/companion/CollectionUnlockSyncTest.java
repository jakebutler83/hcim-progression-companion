package com.hcimprogression.companion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import net.runelite.api.ChatMessageType;
import org.junit.Test;
import static org.junit.Assert.*;

public class CollectionUnlockSyncTest
{
    @Test public void onlyGameMessagesAndExactCollectionPopupsAreEvidence()
    {
        String message = "New item added to your collection log: <col=ff0000>Dragon defender</col>";
        assertEquals("Dragon defender", CollectionUnlockSync.chatName(ChatMessageType.GAMEMESSAGE, message));
        assertEquals("", CollectionUnlockSync.chatName(ChatMessageType.PUBLICCHAT, message));
        assertEquals("", CollectionUnlockSync.chatName(ChatMessageType.GAMEMESSAGE, "Your collection log entry is complete"));
        assertEquals("Dragon defender", CollectionUnlockSync.popupName("Collection log", "New item: Dragon defender"));
        assertEquals("", CollectionUnlockSync.popupName("Combat Task Completed!", "New item: Dragon defender"));
    }
    @Test public void coalescesChatAndPopupAndKeepsOtherRewardsDuringUpload()
    {
        List<CollectionUnlockSync.Unlock> saved = new ArrayList<>();
        CollectionUnlockSync sync = new CollectionUnlockSync(items -> { saved.clear(); saved.addAll(items); });
        sync.observe("Dragon defender", 1000); sync.observe("Dragon defender", 1001);
        assertEquals(1, saved.size());
        sync.resolve("Dragon defender", 12954);
        List<BiConsumer<Boolean,String>> callbacks = new ArrayList<>();
        sync.tick(2900, (items, done) -> { assertEquals(1, items.size()); callbacks.add(done); });
        sync.observe("Fighter torso", 3000); sync.resolve("Fighter torso", 10551);
        callbacks.get(0).accept(true, null);
        assertEquals("Fighter torso", sync.items().get(0).name);
        sync.tick(13000, (items, done) -> done.accept(true, null));
        assertTrue(saved.isEmpty());
    }
    @Test public void ignoresLateCallbacksAfterCharacterSwitchAndRetainsFailedBatch()
    {
        CollectionUnlockSync sync = new CollectionUnlockSync(items -> {});
        sync.observe("Dragon defender", 1000); sync.resolve("Dragon defender", 12954);
        List<BiConsumer<Boolean,String>> callbacks = new ArrayList<>();
        sync.tick(3000, (items, done) -> callbacks.add(done));
        callbacks.get(0).accept(false, "Retry after 60 seconds");
        assertEquals(1, sync.items().size());
        sync.reset(Collections.emptyList(), 4000);
        sync.observe("Fighter torso", 5000);
        callbacks.get(0).accept(true, null);
        assertEquals("Fighter torso", sync.items().get(0).name);
    }
    @Test public void unresolvedAndStaleEventsCannotBeSent()
    {
        CollectionUnlockSync sync = new CollectionUnlockSync(items -> {});
        sync.observe("Unknown item", 1000);
        sync.tick(9000, (items, done) -> fail("Unresolved item must not be guessed"));
        sync.reset(Collections.singletonList(new CollectionUnlockSync.Unlock("Old reward", 1)), 8 * 86400000L);
        assertTrue(sync.items().isEmpty());
    }
}
