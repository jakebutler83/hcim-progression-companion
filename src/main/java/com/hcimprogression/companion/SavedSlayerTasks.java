package com.hcimprogression.companion;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.function.Supplier;
import net.runelite.api.Client;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.config.ConfigManager;

/** Character-scoped, bounded local tracking checkpoints. No bank data, tokens or Wiki requests. */
final class SavedSlayerTasks
{
    static final String GROUP="hcimprogressionslayer";
    private final Supplier<String> owner;
    private final SavedSkillGoals.Values values;
    SavedSlayerTasks(Supplier<String> owner,SavedSkillGoals.Values values) { this.owner=owner; this.values=values; }
    static SavedSlayerTasks forClient(Client client,ConfigManager config)
    {
        return new SavedSlayerTasks(() -> SavedSkillGoals.owner(client,config),new SavedSkillGoals.Values() {
            public String read(String key) { return config.getRSProfileConfiguration(GROUP,key); }
            public void write(String key,String value) { config.setRSProfileConfiguration(GROUP,key,value); }
            public void remove(String key) { config.unsetRSProfileConfiguration(GROUP,key); }
        });
    }
    static String assignmentKey(Client client)
    {
        return client.getVarpValue(VarPlayerID.SLAYER_TARGET)+":"+client.getVarbitValue(VarbitID.SLAYER_TARGET_BOSSID)
            +":"+client.getVarpValue(VarPlayerID.SLAYER_AREA)+":"+client.getVarbitValue(VarbitID.SLAYER_MASTER)
            +":"+client.getVarbitValue(VarbitID.SLAYER_TASKS_COMPLETED)+":"+client.getVarbitValue(VarbitID.SLAYER_WILDERNESS_TASKS_COMPLETED)
            +":"+client.getVarpValue(VarPlayerID.SLAYER_MORTIMER_TASKS_COMPLETED)+":"+client.getVarbitValue(VarbitID.SLAYER_TASK_IS_PARTNER);
    }
    private boolean current(String expected) { return expected!=null && !expected.isEmpty() && expected.equals(owner.get()); }
    Record load(String expected) { return current(expected)?decode(values.read("task_"+expected)):null; }
    void save(String expected,Record record)
    {
        if(!current(expected) || record==null || !record.valid()) return;
        String key="task_"+expected,encoded=encode(record);
        if(!encoded.equals(values.read(key))) values.write(key,encoded);
    }
    void clear(String expected) { if(current(expected)) values.remove("task_"+expected); }
    static final class Record
    {
        final String assignment,style,monster;
        final int original,points;
        final SlayerTaskProgress.Saved progress;
        Record(String assignment,int original,int points,String style,String monster,SlayerTaskProgress.Saved progress)
        { this.assignment=assignment; this.original=original; this.points=points; this.style=style; this.monster=monster; this.progress=progress; }
        boolean valid()
        {
            if(assignment==null || !assignment.matches("[0-9]{1,10}(:[0-9]{1,10}){7}") || original<0 || original>1_000_000
                || points<0 || points>1_000_000 || progress==null || !progress.valid()
                || !("melee".equals(style)||"stab".equals(style)||"crush".equals(style)||"ranged".equals(style)||"magic".equals(style)||"burst".equals(style))) return false;
            try { return monster!=null && SlayerWikiGuide.validTitle(monster).equals(monster); }
            catch(RuntimeException invalid) { return false; }
        }
        boolean matches(String liveAssignment,int liveOriginal,int livePoints,int remaining)
        {
            return valid() && assignment.equals(liveAssignment) && original==liveOriginal && points==livePoints
                && remaining>0 && remaining<=progress.remaining;
        }
    }
    private static String text(String value) { return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8)); }
    private static String untext(String value) { return new String(Base64.getUrlDecoder().decode(value),StandardCharsets.UTF_8); }
    static String encode(Record value)
    {
        SlayerTaskProgress.Saved p=value.progress;
        return "1|"+value.assignment+"|"+value.original+"|"+value.points+"|"+value.style+"|"+text(value.monster)
            +"|"+text(p.task)+"|"+p.total+"|"+p.remaining+"|"+p.xpGained+"|"+p.lastXp;
    }
    static Record decode(String encoded)
    {
        if(encoded==null || encoded.length()>2048) return null;
        try
        {
            String[] parts=encoded.split("\\|",-1); if(parts.length!=11 || !"1".equals(parts[0])) return null;
            Record value=new Record(parts[1],Integer.parseInt(parts[2]),Integer.parseInt(parts[3]),parts[4],untext(parts[5]),
                new SlayerTaskProgress.Saved(untext(parts[6]),Integer.parseInt(parts[7]),Integer.parseInt(parts[8]),Integer.parseInt(parts[9]),Integer.parseInt(parts[10])));
            return value.valid()?value:null;
        }
        catch(RuntimeException invalid) { return null; }
    }
}
