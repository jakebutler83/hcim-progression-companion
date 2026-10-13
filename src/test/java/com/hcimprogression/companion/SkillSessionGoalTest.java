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
        assertFalse(goal.update(Skill.MINING, 1250)); assertEquals(25, goal.percent());
        assertEquals(750, goal.remaining()); assertEquals(250, goal.gained());
    }

    @Test public void completionAnnouncesExactlyOnceAndOverrunClampsPercent()
    {
        SkillSessionGoal goal = new SkillSessionGoal(); goal.start(Skill.PRAYER, 1000, 2000);
        assertTrue(goal.update(Skill.PRAYER, 2200)); assertEquals(100, goal.percent()); assertEquals(0, goal.remaining());
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
        assertFalse(goal.update(Skill.MINING, 1100)); assertNull(goal.skill()); assertEquals(0,goal.percent());
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
        goal.update(Skill.MINING,100_000_000); assertEquals(50,goal.percent());
    }

    @Test public void replacingCompletedGoalCreatesAFreshCompletionAndNewBaseline()
    {
        SkillSessionGoal goal = new SkillSessionGoal(); goal.start(Skill.MINING,0,10); assertTrue(goal.update(Skill.MINING,10));
        goal.start(Skill.MINING,10,20); assertEquals(0,goal.gained()); assertFalse(goal.complete());
        assertTrue(goal.update(Skill.MINING,20));
    }

    @Test public void localFeaturesDefaultOffAndNeedNoNetworkSettings()
    {
        HcimProgressionCompanionConfig config = new HcimProgressionCompanionConfig() {};
        assertFalse(config.livePackingChecksEnabled()); assertFalse(config.sessionSkillGoalsEnabled());
        assertFalse(config.skillGoalDesktopNotifications()); assertFalse(config.routineRoutesSyncEnabled());
        assertFalse(config.automaticAccountSyncEnabled());
    }
}
