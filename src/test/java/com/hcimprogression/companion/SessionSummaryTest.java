package com.hcimprogression.companion;

import java.util.Map;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import org.junit.Test;
import static org.junit.Assert.*;

public class SessionSummaryTest
{
    @Test public void countsLiveXpAndRealLevelsNotExistingProgress()
    {
        SessionSummary summary = new SessionSummary();
        int start = Experience.getXpForLevel(60), end = Experience.getXpForLevel(62);
        summary.start(Map.of(Skill.MINING,start,Skill.FISHING,1000),1000);
        assertEquals(0,summary.xp()); assertEquals(0,summary.levels());
        summary.update(Skill.MINING,end); summary.update(Skill.FISHING,1001);
        assertEquals(end - start + 1,summary.xp()); assertEquals(2,summary.levels());
        assertEquals(2,summary.minutes(121000));
    }

    @Test public void characterResetAndManualRestartDiscardAllPreviousCounters()
    {
        SessionSummary summary = new SessionSummary(); summary.start(Map.of(Skill.MINING,0),0);
        summary.update(Skill.MINING,1000); summary.goalCompleted(); assertEquals(1,summary.goals());
        summary.clear(); assertFalse(summary.active()); assertEquals(0,summary.xp()); assertEquals(0,summary.goals());
        summary.start(Map.of(Skill.MINING,1000),1000); summary.update(Skill.MINING,1010);
        assertEquals(10,summary.xp()); assertEquals(0,summary.goals()); assertEquals(0,summary.minutes(0));
    }

    @Test public void resetXpAndUnknownSkillsNeverInventProgress()
    {
        SessionSummary summary = new SessionSummary(); summary.start(Map.of(Skill.MINING,1000),0);
        summary.update(Skill.MINING,10); summary.update(Skill.FISHING,1000);
        assertEquals(0,summary.xp()); summary.update(Skill.MINING,20); assertEquals(10,summary.xp());
        summary.update(Skill.MINING,-1); assertEquals(10,summary.xp());
    }

    @Test public void virtualLevelsAreNotCountedAsRealLevels()
    {
        SessionSummary summary = new SessionSummary();
        summary.start(Map.of(Skill.MINING,Experience.getXpForLevel(99)),0);
        summary.update(Skill.MINING,Experience.MAX_SKILL_XP); assertEquals(0,summary.levels());
    }

    @Test public void totalXpAcrossSkillsUsesLongCounters()
    {
        java.util.EnumMap<Skill,Integer> xp = new java.util.EnumMap<>(Skill.class);
        for (Skill skill : Skill.values()) if (skill != Skill.OVERALL) xp.put(skill,0);
        SessionSummary summary = new SessionSummary(); summary.start(xp,0);
        for (Skill skill : xp.keySet()) summary.update(skill,Experience.MAX_SKILL_XP);
        assertEquals((long) xp.size() * Experience.MAX_SKILL_XP, summary.xp());
    }
}
