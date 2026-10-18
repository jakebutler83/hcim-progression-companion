package com.hcimprogression.companion;

import net.runelite.api.Experience;
import net.runelite.api.Skill;
import org.junit.Test;
import static org.junit.Assert.*;

public class SkillSessionGoalTest
{
    @Test public void xpGoalTracksOnlyItsSelectedSkill()
    {
        SkillSessionGoal goal = new SkillSessionGoal(); goal.start(Skill.MINING, 1000, 2000);
        assertFalse(goal.update(Skill.WOODCUTTING, 3000)); assertEquals(0, goal.gained());
        assertFalse(goal.update(Skill.MINING, 1250)); assertEquals(25.0, goal.percent(), 0.00001);
        assertEquals(750, goal.remaining()); assertEquals(250, goal.gained());
    }

    @Test public void completionAnnouncesExactlyOnceAndOverrunClampsPercent()
    {
        SkillSessionGoal goal = new SkillSessionGoal(); goal.start(Skill.PRAYER, 1000, 2000);
        assertTrue(goal.update(Skill.PRAYER, 2200)); assertEquals(100.0, goal.percent(), 0.00001); assertEquals(0, goal.remaining());
        assertFalse(goal.update(Skill.PRAYER, 2200)); assertFalse(goal.update(Skill.PRAYER, 2300));
        assertTrue(goal.complete());
    }

    @Test public void reachingLevelUsesTheRuneLiteXpTable()
    {
        assertEquals(Experience.getXpForLevel(70), SkillSessionGoal.target(1000, true, 70));
        assertEquals(1500, SkillSessionGoal.target(1000, false, 500));
    }

    @Test public void resetAndFallingXpCannotCompleteAPreviousCharactersGoal()
    {
        SkillSessionGoal goal = new SkillSessionGoal(); goal.start(Skill.MINING, 1000, 2000);
        goal.clear(); assertNull(goal.skill()); assertFalse(goal.update(Skill.MINING, 3000));
        goal.start(Skill.MINING, 1000, 2000); goal.update(Skill.MINING, 1200);
        assertFalse(goal.update(Skill.MINING, 1100)); assertNull(goal.skill()); assertEquals(0.0,goal.percent(),0.00001);
    }

    @Test public void invalidUnreachableOrAlreadyMetGoalsAreRejected()
    {
        for (int target : new int[]{-1, 1000, Experience.MAX_SKILL_XP + 1})
        {
            try { new SkillSessionGoal().start(Skill.MINING, 1000, target); fail(); }
            catch (IllegalArgumentException expected) { }
        }
        for (int level : new int[]{0, 1, 100, Integer.MAX_VALUE})
        {
            try { SkillSessionGoal.target(1000,true,level); fail(); }
            catch (IllegalArgumentException expected) { }
        }
        try { SkillSessionGoal.target(Experience.MAX_SKILL_XP - 1,false,100); fail(); }
        catch (IllegalArgumentException expected) { }
        try { new SkillSessionGoal().start(Skill.OVERALL,0,100); fail(); }
        catch (IllegalArgumentException expected) { }
    }

    @Test public void largeXpTargetsDoNotOverflowTheProgressBar()
    {
        SkillSessionGoal goal = new SkillSessionGoal(); goal.start(Skill.MINING,0,Experience.MAX_SKILL_XP);
        goal.update(Skill.MINING,100_000_000); assertEquals(50.0,goal.percent(),0.00001);
    }

    @Test public void replacingCompletedGoalCreatesAFreshCompletionAndNewBaseline()
    {
        SkillSessionGoal goal = new SkillSessionGoal(); goal.start(Skill.MINING,0,10); assertTrue(goal.update(Skill.MINING,10));
        goal.start(Skill.MINING,10,20); assertEquals(0,goal.gained()); assertFalse(goal.complete());
        assertTrue(goal.update(Skill.MINING,20));
    }

    @Test public void percentagesUseDynamicXpAndRoundToTenthsWithoutTruncatingTheBar()
    {
        SkillSessionGoal goal = new SkillSessionGoal(); goal.start(Skill.MINING,1000,11000);
        assertEquals("0.0%",goal.percentText());
        goal.update(Skill.MINING,5564); assertEquals(45.64,goal.percent(),0.00001);
        assertEquals("45.6%",goal.percentText()); assertEquals(456,SessionGoalHelper.goalBarValue(goal.percent()));
        goal.update(Skill.MINING,5565); assertEquals("45.7%",goal.percentText());
        assertEquals(457,SessionGoalHelper.goalBarValue(goal.percent())); assertFalse(goal.complete());
    }

    @Test public void restoredGoalKeepsItsOriginalBaselineAndCountsXpFromAnotherSession()
    {
        SkillSessionGoal goal=new SkillSessionGoal(); goal.restore(Skill.MINING,1000,2000,1456);
        assertEquals(456,goal.gained()); assertEquals("45.6%",goal.percentText());
        assertFalse(goal.update(Skill.MINING,1500)); assertEquals(500,goal.gained());
        assertTrue(goal.update(Skill.MINING,2000)); assertFalse(goal.update(Skill.MINING,2000));
    }

    @Test public void completedRestoreNeverReplaysACompletionNotification()
    {
        SkillSessionGoal goal=new SkillSessionGoal(); goal.restore(Skill.MINING,1000,2000,2001);
        assertTrue(goal.complete()); assertEquals("100.0%",goal.percentText());
        assertFalse(goal.update(Skill.MINING,2001)); assertFalse(goal.update(Skill.MINING,2002));
    }

    @Test public void restoringWithInvalidXpDoesNotLeaveAHalfRestoredGoal()
    {
        SkillSessionGoal goal=new SkillSessionGoal();
        try { goal.restore(Skill.MINING,1000,2000,999); fail(); } catch(IllegalArgumentException expected) { }
        assertNull(goal.skill());
    }

    @Test public void roundingCannotMarkAnUnfinishedGoalComplete()
    {
        SkillSessionGoal goal = new SkillSessionGoal(); goal.start(Skill.MINING,0,Experience.MAX_SKILL_XP);
        assertFalse(goal.update(Skill.MINING,Experience.MAX_SKILL_XP-1));
        assertEquals("99.9%",goal.percentText()); assertEquals(999,SessionGoalHelper.goalBarValue(goal.percent()));
        assertFalse(goal.complete());
        assertTrue(goal.update(Skill.MINING,Experience.MAX_SKILL_XP));
        assertEquals("100.0%",goal.percentText()); assertEquals(1000,SessionGoalHelper.goalBarValue(goal.percent()));
        goal.clear(); assertEquals("0.0%",goal.percentText());
    }

    @Test public void percentageFormattingDoesNotDependOnSystemDecimalSeparator()
    {
        java.util.Locale previous = java.util.Locale.getDefault();
        try
        {
            java.util.Locale.setDefault(java.util.Locale.GERMANY);
            SkillSessionGoal goal = new SkillSessionGoal(); goal.start(Skill.FISHING,0,1000);
            goal.update(Skill.FISHING,123); assertEquals("12.3%",goal.percentText());
        }
        finally { java.util.Locale.setDefault(previous); }
    }

    @Test public void localFeaturesDefaultOffAndNeedNoNetworkSettings()
    {
        HcimProgressionCompanionConfig config = new HcimProgressionCompanionConfig() {};
        assertFalse(config.livePackingChecksEnabled()); assertFalse(config.sessionSkillGoalsEnabled());
        assertFalse(config.skillGoalDesktopNotifications()); assertFalse(config.routineRoutesSyncEnabled());
        assertFalse(config.automaticAccountSyncEnabled());
    }
}
