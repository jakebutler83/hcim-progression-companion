package com.hcimprogression.companion;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.List;
import java.util.function.Supplier;
import net.runelite.api.Client;
import net.runelite.api.Experience;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.RuneScapeProfile;
import net.runelite.client.config.RuneScapeProfileType;

/** Bounded, credential-free records in RuneLite's cached per-RS-profile config.
 * ConfigManager owns persistence; no direct disk/network IO on ClientThread. */
final class SavedSkillGoals
{
    static final String GROUP = "hcimprogressiongoals";
    interface Values
    {
        String read(String key);
        void write(String key,String value);
        void remove(String key);
    }
    private final Supplier<String> owner;
    private final Values values;
    SavedSkillGoals(Supplier<String> owner,Values values) { this.owner=owner; this.values=values; }
    static SavedSkillGoals forClient(Client client,ConfigManager config)
    {
        return new SavedSkillGoals(() -> owner(client,config),new Values() {
            public String read(String key) { return config.getRSProfileConfiguration(GROUP,key); }
            public void write(String key,String value) { config.setRSProfileConfiguration(GROUP,key,value); }
            public void remove(String key) { config.unsetRSProfileConfiguration(GROUP,key); }
        });
    }
    static String owner(Client client,ConfigManager config)
    {
        if (client.getGameState()!=GameState.LOGGED_IN || client.getLocalPlayer()==null || config.getProfile()==null) return "";
        long hash=client.getAccountHash(); String profile=config.getRSProfileKey();
        RuneScapeProfileType type=RuneScapeProfileType.getCurrent(client);
        return ownerForProfile(config.getProfile().getId(),profile,hash,type,config.getRSProfiles());
    }
    static String ownerForProfile(long runeLiteProfile,String profile,long hash,RuneScapeProfileType type,List<RuneScapeProfile> profiles)
    {
        if (hash==0 || hash==-1 || profile==null || profile.isEmpty() || type==null || profiles==null) return "";
        for (RuneScapeProfile candidate:profiles)
        {
            if (profile.equals(candidate.getKey()) && candidate.getAccountHash()==hash && candidate.getType()==type)
                return Base64.getUrlEncoder().withoutPadding().encodeToString(
                    (runeLiteProfile+":"+profile+":"+Long.toUnsignedString(hash)).getBytes(StandardCharsets.UTF_8));
        }
        return ""; // A login/profile transition must never read the previous character's record.
    }
    String owner() { String key=owner.get(); return key==null ? "" : key; }
    private boolean current(String expected) { return expected!=null && !expected.isEmpty() && expected.equals(owner()); }
    SkillGoalSync.Goal load(String expected)
    {
        return current(expected) ? decode(values.read("skillGoal_"+expected)) : null;
    }
    void save(String expected,SkillGoalSync.Goal goal)
    {
        if (!current(expected) || !valid(goal)) return;
        String key="skillGoal_"+expected, encoded=encode(goal);
        if (!encoded.equals(values.read(key))) values.write(key,encoded);
    }
    void clear(String expected) { if(current(expected)) values.remove("skillGoal_"+expected); }
    static Skill skill(String name)
    {
        try { Skill value=Skill.valueOf(name.toUpperCase(Locale.ROOT).replace(' ','_')); return value==Skill.OVERALL ? null : value; }
        catch(RuntimeException invalid) { return null; }
    }
    static boolean valid(SkillGoalSync.Goal goal)
    {
        if (goal==null || goal.id==null || !goal.id.matches("[a-f0-9]{8}(-[a-f0-9]{4}){3}-[a-f0-9]{12}")
            || skill(goal.skill)==null || goal.sequence<1 || goal.sequence>=1_000_000_000
            || goal.startXp<0 || goal.targetXp<=goal.startXp || goal.targetXp>Experience.MAX_SKILL_XP
            || goal.currentXp<goal.startXp || goal.currentXp>Experience.MAX_SKILL_XP
            || "completed".equals(goal.status) && goal.currentXp<goal.targetXp
            || !("active".equals(goal.status)||"completed".equals(goal.status)||"stopped".equals(goal.status))) return false;
        if ("level".equals(goal.mode)) return goal.level!=null && goal.level>=2 && goal.level<=99
            && Experience.getXpForLevel(goal.level)==goal.targetXp;
        return "xp".equals(goal.mode) && goal.level==null;
    }
    static String encode(SkillGoalSync.Goal goal)
    {
        return "1|"+goal.id+"|"+goal.skill+"|"+goal.mode+"|"+(goal.level==null?0:goal.level)+"|"
            +goal.startXp+"|"+goal.targetXp+"|"+goal.currentXp+"|"+goal.sequence+"|"+goal.status;
    }
    static SkillGoalSync.Goal decode(String encoded)
    {
        if(encoded==null || encoded.length()>256) return null;
        try
        {
            String[] parts=encoded.split("\\|",-1);
            if(parts.length!=10 || !"1".equals(parts[0])) return null;
            SkillGoalSync.Goal goal=new SkillGoalSync.Goal(); goal.id=parts[1]; goal.skill=parts[2]; goal.mode=parts[3];
            int level=Integer.parseInt(parts[4]); goal.level=level==0?null:level;
            goal.startXp=Integer.parseInt(parts[5]); goal.targetXp=Integer.parseInt(parts[6]); goal.currentXp=Integer.parseInt(parts[7]);
            goal.sequence=Integer.parseInt(parts[8]); goal.status=parts[9];
            return valid(goal)?goal:null;
        }
        catch(RuntimeException invalid) { return null; }
    }
}
