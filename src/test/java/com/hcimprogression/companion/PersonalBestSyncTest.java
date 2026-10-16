package com.hcimprogression.companion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import org.junit.Test;
import static org.junit.Assert.*;

public class PersonalBestSyncTest
{
    private static final class Harness
    {
        final AtomicBoolean enabled = new AtomicBoolean(true);
        final List<List<PersonalBestRecord>> sent = new ArrayList<>();
        final List<BiConsumer<Integer,String>> callbacks = new ArrayList<>();
        final long now=System.currentTimeMillis();
        final PersonalBestSync sync=new PersonalBestSync(enabled::get,(name,token,api,records,done)->{sent.add(records);callbacks.add(done);});
        Harness(){sync.connection("HP Shooter","token","https://example.invalid","profile-hp");}
        void offer(long ms,long at){sync.offer(Collections.singletonList(new PersonalBestRecord("corrupted gauntlet","STANDARD","runelite-personalbest",ms)),at);}
    }
    @Test public void debounceCoalescesRecordsAndNoHiscoreLookupIsRequired()
    {
        Harness h=new Harness();h.offer(500000,h.now);h.sync.tick(h.now+1000);assertEquals(0,h.sent.size());
        h.offer(490000,h.now+1000);h.sync.tick(h.now+2200);assertEquals(1,h.sent.size());assertEquals(490000,h.sent.get(0).get(0).durationMs);
        h.callbacks.get(0).accept(1,null);h.offer(490000,h.now+3000);h.sync.tick(h.now+12000);assertEquals(1,h.sent.size());
    }
    @Test public void improvementsDuringAnUploadAreNotLost()
    {
        Harness h=new Harness();h.offer(500000,h.now);h.sync.tick(h.now+1200);
        h.offer(480000,h.now+2000);h.callbacks.get(0).accept(1,null);h.sync.tick(h.now+4000);assertEquals(1,h.sent.size());
        h.sync.tick(h.now+11200);assertEquals(2,h.sent.size());assertEquals(480000,h.sent.get(1).get(0).durationMs);
    }
    @Test public void optOutAndLogoutNeverUploadPendingRecords()
    {
        Harness h=new Harness();h.offer(500000,h.now);h.enabled.set(false);h.sync.tick(h.now+2000);assertEquals(0,h.sent.size());
        h.enabled.set(true);h.offer(500000,h.now+3000);h.sync.reset();h.sync.tick(h.now+10000);assertEquals(0,h.sent.size());
    }
    @Test public void oldAccountCallbacksCannotOverwriteNewAccountState()
    {
        Harness h=new Harness();h.offer(500000,h.now);h.sync.tick(h.now+1200);
        h.sync.connection("SW Shooter","other-token","https://example.invalid","profile-sw");
        h.callbacks.get(0).accept(1,null);assertFalse(h.sync.status().contains("saved"));
        h.offer(450000,h.now+3000);h.sync.tick(h.now+4200);assertEquals(2,h.sent.size());
    }
    @Test public void failuresRespectServerRetryAfterAndKeepTheRecordQueued()
    {
        Harness h=new Harness();h.offer(500000,h.now);h.sync.tick(h.now+1200);
        h.callbacks.get(0).accept(null,"HTTP 429 Retry after 600 seconds.");
        h.sync.tick(h.now+300000);assertEquals(1,h.sent.size());
        h.sync.tick(h.now+602000);assertEquals(2,h.sent.size());assertTrue(h.sync.status().contains("Uploading"));
    }
}
