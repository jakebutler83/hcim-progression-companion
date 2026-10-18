package com.hcimprogression.companion;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import org.junit.Test;
import static org.junit.Assert.*;

public class SkillGoalSyncTest
{
    private static final class Harness
    {
        final AtomicBoolean enabled = new AtomicBoolean(true);
        final List<SkillGoalSync.Goal> sent = new ArrayList<>();
        final List<BiConsumer<SkillGoalSync.Response,String>> callbacks = new ArrayList<>();
        final SkillGoalSync sync = new SkillGoalSync(enabled::get,(name,token,api,goal,callback) -> {sent.add(goal); callbacks.add(callback);});
        Harness() {sync.connection("HP Shooter","fake-token","fake-api");}
        void accept(int index,String status) {SkillGoalSync.Response value = new SkillGoalSync.Response(); value.ok=true;value.status=status;value.sequence=sent.get(index).sequence;callbacks.get(index).accept(value,null);}
    }
    @Test public void startsOnceThenCoalescesXpAndSendsCompletionPromptly()
    {
        Harness h = new Harness(); h.sync.start("Mining",false,0,1000,2000); h.sync.tick(0); assertEquals(1,h.sent.size());h.accept(0,"active");
        h.sync.progress(1250,false);h.sync.progress(1500,false);h.sync.tick(3000);assertEquals(1,h.sent.size());
        h.sync.tick(120000);assertEquals(2,h.sent.size()); assertEquals(1500,h.sent.get(1).currentXp);h.accept(1,"active");
        h.sync.progress(2000,true);h.sync.tick(123000);assertEquals("completed",h.sent.get(2).status);
        assertEquals(1000,h.sent.get(0).currentXp); // In-flight snapshots are immutable.
    }
    @Test public void optOutNeverUploadsAndDisablingClearsPendingUpdates()
    {
        Harness h = new Harness();h.enabled.set(false);h.sync.start("Mining",false,0,1000,2000);h.sync.tick(0);assertTrue(h.sent.isEmpty());
        h.enabled.set(true);h.sync.request();h.sync.tick(3000);assertEquals(1,h.sent.size());
        h.sync.progress(2000,true);h.enabled.set(false);h.sync.tick(4000);h.accept(0,"active");h.sync.tick(8000);assertEquals(1,h.sent.size());
    }
    @Test public void lateCallbacksAndQueuedGoalsCannotFollowCharacterSwitch()
    {
        Harness h = new Harness();h.sync.start("Mining",false,0,1000,2000);h.sync.tick(0);
        h.sync.connection("SW Shooter","other-token","fake-api");h.accept(0,"active");h.sync.tick(130000);
        assertEquals(1,h.sent.size()); assertFalse(h.sync.status().contains("Saved to"));
        h.sync.start("Fishing",false,0,20,30);h.sync.tick(133000);assertEquals("Fishing",h.sent.get(1).skill);
    }
    @Test public void replacingAnActiveGoalStopsItBeforeSendingNewGoal()
    {
        Harness h = new Harness();h.sync.start("Mining",false,0,1000,2000);h.sync.tick(0);h.accept(0,"active");
        h.sync.start("Fishing",false,0,100,200);h.sync.tick(3000);assertEquals("stopped",h.sent.get(1).status);h.accept(1,"stopped");
        h.sync.tick(6000);assertEquals("Fishing",h.sent.get(2).skill);assertNotEquals(h.sent.get(0).id,h.sent.get(2).id);
    }
    @Test public void stoppedWebsiteGoalsAreNeverResurrectedByLaterXp()
    {
        Harness h = new Harness();h.sync.start("Mining",false,0,1000,2000);h.sync.tick(0);h.accept(0,"stopped");
        h.sync.progress(1500,false);h.sync.tick(130000);assertEquals(1,h.sent.size());
    }
    @Test public void unchangedGoalDoesNotMakeRepeatedNetworkRequests()
    {
        Harness h = new Harness();h.sync.start("Mining",false,0,1000,2000);h.sync.tick(0);h.accept(0,"active");
        h.sync.tick(130000);h.sync.tick(260000);assertEquals(1,h.sent.size());
    }

    @Test public void relogResumesTheOriginalWebsiteGoalWithoutCreatingAnotherTask()
    {
        Harness h=new Harness(); h.sync.start("Mining",false,0,1000,2000); h.sync.progress(1456,false);
        SkillGoalSync.Goal saved=h.sync.snapshot();
        h.sync.reset(); h.sync.connection("HP Shooter","fake-token","fake-api"); h.sync.restore(saved); h.sync.tick(3000);
        assertEquals(1,h.sent.size()); assertEquals(saved.id,h.sent.get(0).id);
        assertEquals(1000,h.sent.get(0).startXp); assertEquals(1456,h.sent.get(0).currentXp);
        assertTrue(h.sent.get(0).sequence>saved.sequence);
        assertEquals(saved.sequence, saved.copy().sequence);
    }

    @Test public void restoringAWebsiteStoppedGoalDoesNotResurrectItsUpload()
    {
        Harness h=new Harness(); SkillGoalSync.Goal saved=SavedSkillGoalsTest.goal(); saved.status="stopped";
        h.sync.restore(saved); h.sync.progress(saved.currentXp+1,false); h.sync.tick(3000);
        assertTrue(h.sent.isEmpty()); assertEquals("stopped",h.sync.snapshot().status);
    }

    @Test public void lateRequestsFromBeforeLogoutCannotChangeTheRestoredGoal()
    {
        Harness h=new Harness(); h.sync.start("Mining",false,0,1000,2000); h.sync.tick(0);
        SkillGoalSync.Goal saved=h.sync.snapshot(); h.sync.reset();
        h.sync.connection("HP Shooter","fake-token","fake-api"); h.sync.restore(saved);
        h.accept(0,"stopped"); assertEquals("active",h.sync.snapshot().status);
        h.sync.tick(3000); assertEquals(2,h.sent.size()); assertEquals(saved.id,h.sent.get(1).id);
    }
}
