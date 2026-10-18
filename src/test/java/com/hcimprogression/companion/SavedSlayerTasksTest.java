package com.hcimprogression.companion;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import static org.junit.Assert.*;

public class SavedSlayerTasksTest
{
    private static final String ASSIGNMENT="12:0:4:9:49:10:0:0";
    private static SavedSlayerTasks.Record record()
    {
        return new SavedSlayerTasks.Record(ASSIGNMENT,100,123,"crush","Greater demon",
            new SlayerTaskProgress.Saved("Greater demons",40,38,250,500250));
    }
    private static final class Values implements SavedSkillGoals.Values
    {
        final Map<String,String> data=new HashMap<>(); int writes;
        public String read(String key) { return data.get(key); }
        public void write(String key,String value) { data.put(key,value); writes++; }
        public void remove(String key) { data.remove(key); }
    }
    @Test public void restartKeepsTaskCreditXpAndPreparationChoices()
    {
        Values values=new Values(); SavedSlayerTasks first=new SavedSlayerTasks(()->"character-a",values);
        first.save("character-a",record());
        SavedSlayerTasks.Record loaded=new SavedSlayerTasks(()->"character-a",values).load("character-a");
        assertNotNull(loaded); assertEquals(40,loaded.progress.total); assertEquals(38,loaded.progress.remaining);
        assertEquals(250,loaded.progress.xpGained); assertEquals("crush",loaded.style); assertEquals("Greater demon",loaded.monster);
        assertTrue(loaded.matches(ASSIGNMENT,100,123,38));
    }
    @Test public void anotherCharacterCannotReadWriteOrClearThePreviousTrackersRecord()
    {
        Values values=new Values(); AtomicReference<String> owner=new AtomicReference<>("profile-a:character-a");
        SavedSlayerTasks store=new SavedSlayerTasks(owner::get,values); store.save(owner.get(),record());
        owner.set("profile-a:character-b"); assertNull(store.load(owner.get()));
        assertNull(store.load("profile-a:character-a")); store.clear("profile-a:character-a"); store.save("profile-a:character-a",record());
        assertEquals(1,values.writes); owner.set("profile-a:character-a"); assertNotNull(store.load(owner.get()));
        owner.set("profile-b:character-a"); assertNull(store.load(owner.get()));
    }
    @Test public void stopDeletesTheCheckpointAndUnchangedTicksDoNotRewriteIt()
    {
        Values values=new Values(); SavedSlayerTasks store=new SavedSlayerTasks(()->"a",values);
        store.save("a",record()); store.save("a",record()); assertEquals(1,values.writes);
        store.clear("a"); assertNull(new SavedSlayerTasks(()->"a",values).load("a"));
    }
    @Test public void completedChangedReplacedOrAmbiguouslyExtendedAssignmentsDoNotResume()
    {
        SavedSlayerTasks.Record saved=record();
        assertFalse(saved.matches(ASSIGNMENT,100,123,0));
        assertFalse(saved.matches("13:0:4:9:49:10:0:0",100,123,38));
        assertFalse(saved.matches("12:0:5:9:49:10:0:0",100,123,38));
        assertFalse(saved.matches("12:0:4:9:50:10:0:0",100,123,38));
        assertFalse(saved.matches("12:0:4:9:49:11:0:0",100,123,38));
        assertFalse(saved.matches(ASSIGNMENT,120,123,38));
        assertFalse(saved.matches(ASSIGNMENT,100,93,38)); // Paid cancellation while not observed.
        assertFalse(saved.matches(ASSIGNMENT,100,123,39));
        assertTrue(saved.matches(ASSIGNMENT,100,123,30));
    }
    @Test public void malformedOrUnboundedRecordsAreIgnored()
    {
        String valid=SavedSlayerTasks.encode(record());
        for(String invalid:new String[]{null,"",valid.replaceFirst("1\\|","2|"),valid+"|extra",valid.replace("|crush|","|invalid|"),
            valid.replace("|40|38|","|40|41|"),valid.replace("|250|500250","|250|200000001"),"x".repeat(2049)})
            assertNull(SavedSlayerTasks.decode(invalid));
    }
    @Test public void unknownIdentityCannotLoadOrPersistATracker()
    {
        Values values=new Values(); SavedSlayerTasks store=new SavedSlayerTasks(()->"",values);
        store.save("",record()); assertNull(store.load("")); assertEquals(0,values.writes);
    }
}
