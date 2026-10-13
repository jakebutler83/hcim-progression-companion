package com.hcimprogression.companion;

import net.runelite.api.Experience;
import net.runelite.api.Skill;

/** Client-thread owned session goal; XP, never boosted levels, proves progress. */
final class SkillSessionGoal
{
    private Skill skill;
    private int start, target, current;
    private boolean announced;

    void start(Skill nextSkill, int currentXp, int targetXp)
    {
        if (nextSkill == null || nextSkill == Skill.OVERALL || currentXp < 0
            || targetXp <= currentXp || targetXp > Experience.MAX_SKILL_XP)
            throw new IllegalArgumentException("Choose a target above your current XP, up to 200 million.");
        skill = nextSkill; start = current = currentXp; target = targetXp; announced = false;
    }

    boolean update(Skill nextSkill, int xp)
    {
        if (skill == null || skill != nextSkill) return false;
        if (xp < current) { clear(); return false; } // Never carry a baseline into reset/other-account XP.
        current = Math.min(Experience.MAX_SKILL_XP, xp);
        if (current >= target && !announced) { announced = true; return true; }
        return false;
    }

    void clear() { skill = null; announced = false; }
    Skill skill() { return skill; }
    int gained() { return skill == null ? 0 : Math.max(0, current - start); }
    int required() { return skill == null ? 0 : target - start; }
    int remaining() { return skill == null ? 0 : Math.max(0, target - current); }
    int percent() { return skill == null ? 0 : (int) Math.min(100L, 100L * gained() / required()); }
    boolean complete() { return skill != null && current >= target; }

    static int target(int xp, boolean level, int amount)
    {
        if (level)
        {
            if (amount < 2 || amount > Experience.MAX_REAL_LEVEL)
                throw new IllegalArgumentException("Choose a real level from 2 to 99.");
            return Experience.getXpForLevel(amount);
        }
        if (amount < 1 || (long) xp + amount > Experience.MAX_SKILL_XP)
            throw new IllegalArgumentException("Enter a positive XP gain without exceeding 200 million total XP.");
        return xp + amount;
    }
}
