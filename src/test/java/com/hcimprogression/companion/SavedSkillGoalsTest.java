package com.hcimprogression.companion;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import net.runelite.client.config.RuneScapeProfile;
import net.runelite.client.config.RuneScapeProfileType;
import org.junit.Test;
import static org.junit.Assert.*;

public class SavedSkillGoalsTest
{
    private static final class Memory implements SavedSkillGoals.Values
    {
        final Map<String,String> values=new HashMap<>(); int writes;
        public String read(String key) { return values.get(key); }
        public void write(String key,String value) { values.put(key,value); writes++; }
        public void remove(String key) { values.remove(key); }
    }
    static SkillGoalSync.Goal goal()
    {
        SkillGoalSync.Goal goal=new SkillGoalSync.Goal(); goal.id="aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
        goal.skill="Attack"; goal.mode="level"; goal.level=75; goal.startXp=900000; goal.targetXp=Experience.getXpForLevel(75);
        goal.currentXp=950000; goal.sequence=7; goal.status="active"; return goal;
    }
    @Test public void reloadingTheStorePreservesOriginalGoalAndSyncIdentity()
    {
        Memory memory=new Memory(); SavedSkillGoals first=new SavedSkillGoals(()->"character-a",memory);
        first.save("character-a",goal());
        SavedSkillGoals restarted=new SavedSkillGoals(()->"character-a",memory);
        SkillGoalSync.Goal restored=restarted.load("character-a");
        assertEquals(goal().id,restored.id); assertEquals(900000,restored.startXp);
        assertEquals(950000,restored.currentXp); assertEquals(75,restored.level.intValue());
        assertEquals(7,restored.sequence); assertEquals("active",restored.status);
        SkillSessionGoal local=new SkillSessionGoal(); local.restore(Skill.ATTACK,restored.startXp,restored.targetXp,975000);
        assertEquals(75000,local.gained()); assertEquals(restored.targetXp-975000,local.remaining());
    }
    @Test public void accountAndProfileSwitchesCannotReadWriteOrDeleteAnotherGoal()
    {
        Memory memory=new Memory(); AtomicReference<String> owner=new AtomicReference<>("profile-a:character-a");
        SavedSkillGoals store=new SavedSkillGoals(owner::get,memory);
        store.save(owner.get(),goal()); owner.set("profile-a:character-b");
        assertNull(store.load(owner.get())); assertNull(store.load("profile-a:character-a"));
        store.clear("profile-a:character-a"); store.save("profile-a:character-a",goal());
        assertEquals(1,memory.writes);
        owner.set("profile-b:character-a"); assertNull(store.load(owner.get()));
        owner.set("profile-a:character-a"); assertNotNull(store.load(owner.get()));
    }
    @Test public void repeatedProgressDoesNotRewriteTheSameConfigurationAndClearStaysCleared()
    {
        Memory memory=new Memory(); SavedSkillGoals store=new SavedSkillGoals(()->"a",memory);
        store.save("a",goal()); store.save("a",goal()); assertEquals(1,memory.writes);
        store.clear("a"); assertNull(new SavedSkillGoals(()->"a",memory).load("a"));
    }
    @Test public void unknownOwnerDoesNotPersistOrReadAnyData()
    {
        Memory memory=new Memory(); SavedSkillGoals store=new SavedSkillGoals(()->"",memory);
        store.save("",goal()); assertNull(store.load("")); assertEquals(0,memory.writes);
    }
    @Test public void malformedUnknownVersionAndUnreasonableRecordsAreIgnored()
    {
        String valid=SavedSkillGoals.encode(goal());
        for(String value:new String[]{null,"",valid.replaceFirst("1\\|","9|"),valid+"|extra",valid.replace("Attack","OVERALL"),
            valid.replace("950000","-1"),valid.replace("950000","200000001"),valid.replace("|7|","|0|"),
            valid.replace("|75|","|99|"),valid.replace(goal().id,"../other-account"),"x".repeat(257)})
            assertNull(SavedSkillGoals.decode(value));
    }
    @Test public void xpGoalsAndStoppedWebsiteGoalsRoundTripWithoutCredentials()
    {
        SkillGoalSync.Goal goal=goal(); goal.mode="xp"; goal.level=null; goal.status="stopped";
        String encoded=SavedSkillGoals.encode(goal);
        SkillGoalSync.Goal restored=SavedSkillGoals.decode(encoded);
        assertNotNull(restored); assertEquals("stopped",restored.status); assertNull(restored.level);
        assertFalse(encoded.contains("token")); assertFalse(encoded.contains("api"));
    }

    @Test public void scopeRequiresMatchingAccountHashProfileAndWorldTypeNotTheDisplayName()
    {
        java.util.List<RuneScapeProfile> profiles=java.util.Arrays.asList(
            new RuneScapeProfile("Old Name",RuneScapeProfileType.STANDARD,123L,"rsprofile.one"),
            new RuneScapeProfile("Old Name",RuneScapeProfileType.QUEST_SPEEDRUNNING,123L,"rsprofile.speed"));
        String first=SavedSkillGoals.ownerForProfile(1,"rsprofile.one",123L,RuneScapeProfileType.STANDARD,profiles);
        assertFalse(first.isEmpty()); assertTrue(first.matches("[A-Za-z0-9_-]+"));
        assertEquals("",SavedSkillGoals.ownerForProfile(1,"rsprofile.one",456L,RuneScapeProfileType.STANDARD,profiles));
        assertEquals("",SavedSkillGoals.ownerForProfile(1,"rsprofile.one",123L,RuneScapeProfileType.QUEST_SPEEDRUNNING,profiles));
        assertEquals("",SavedSkillGoals.ownerForProfile(1,null,123L,RuneScapeProfileType.STANDARD,profiles));
        assertNotEquals(first,SavedSkillGoals.ownerForProfile(2,"rsprofile.one",123L,RuneScapeProfileType.STANDARD,profiles));
        java.util.List<RuneScapeProfile> renamed=java.util.Collections.singletonList(
            new RuneScapeProfile("New Name",RuneScapeProfileType.STANDARD,123L,"rsprofile.one"));
        assertEquals(first,SavedSkillGoals.ownerForProfile(1,"rsprofile.one",123L,RuneScapeProfileType.STANDARD,renamed));
    }
}
