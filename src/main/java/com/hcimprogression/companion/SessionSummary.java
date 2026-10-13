package com.hcimprogression.companion;

import java.util.EnumMap;
import java.util.Map;
import net.runelite.api.Experience;
import net.runelite.api.Skill;

/** Session-only counters. No historical backfill, persistence or network traffic. */
final class SessionSummary
{
    private final Map<Skill, Integer> baseline = new EnumMap<>(Skill.class);
    private final Map<Skill, Integer> current = new EnumMap<>(Skill.class);
    private long startedAt;
    private int goals;

    void start(Map<Skill, Integer> xp, long now)
    {
        clear(); startedAt = now;
        for (Map.Entry<Skill, Integer> entry : xp.entrySet())
        {
            if (entry.getKey() != Skill.OVERALL && entry.getValue() >= 0 && entry.getValue() <= Experience.MAX_SKILL_XP)
            { baseline.put(entry.getKey(), entry.getValue()); current.put(entry.getKey(), entry.getValue()); }
        }
    }
    void clear() { baseline.clear(); current.clear(); goals = 0; startedAt = 0; }
    boolean active() { return !baseline.isEmpty(); }
    boolean update(Skill skill, int xp)
    {
        if (!baseline.containsKey(skill) || xp < 0 || xp > Experience.MAX_SKILL_XP || current.get(skill) == xp) return false;
        if (xp < current.get(skill)) baseline.put(skill, xp); // Reset XP never creates fake gains.
        current.put(skill, xp);
        return true;
    }
    void goalCompleted() { if (active()) goals++; }
    int goals() { return goals; }
    long xp()
    {
        return current.entrySet().stream().mapToLong(entry -> Math.max(0, entry.getValue() - baseline.get(entry.getKey()))).sum();
    }
    int levels()
    {
        return current.entrySet().stream().mapToInt(entry -> Math.max(0,
            Math.min(Experience.MAX_REAL_LEVEL, Experience.getLevelForXp(entry.getValue()))
                - Math.min(Experience.MAX_REAL_LEVEL, Experience.getLevelForXp(baseline.get(entry.getKey()))))).sum();
    }
    long minutes(long now) { return active() ? Math.max(0, now - startedAt) / 60_000L : 0; }
}
