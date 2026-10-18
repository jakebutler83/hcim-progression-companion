package com.hcimprogression.companion;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

/** Client-thread state: bounded coalescing, explicit opt-in, immutable upload snapshots. */
final class SkillGoalSync
{
    interface Sender { void send(String name,String token,String api,Goal goal,BiConsumer<Response,String> callback); }
    private final BooleanSupplier enabled;
    private final Sender sender;
    private final Map<String,Goal> pending = new LinkedHashMap<>();
    private String name = "", token = "", api = "", identity = "";
    private long generation, nextSendAt, nextProgressAt;
    private boolean busy;
    private Goal current;
    private String savedId = "";
    private int savedSequence;
    private volatile String status = "Website sync off · goals stay local";

    SkillGoalSync(BooleanSupplier enabled,Sender sender) { this.enabled = enabled; this.sender = sender; }
    boolean connection(String nextName,String nextToken,String nextApi)
    {
        String next = nextName+":"+nextToken+":"+nextApi;
        if (identity.equals(next)) return false;
        generation++; pending.clear(); current = null; savedId = ""; savedSequence = 0; busy = false; nextSendAt = nextProgressAt = 0;
        name = nextName; token = nextToken; api = nextApi; identity = next;
        status = enabled.getAsBoolean() ? "Start a goal to sync this character" : "Website sync off · goals stay local";
        return true;
    }
    void reset() { connection("","",""); }
    Goal snapshot() { return current==null ? null : current.copy(); }
    void restore(Goal saved)
    {
        if (!SavedSkillGoals.valid(saved)) return;
        current=saved.copy(); current.sequence++;
        if ("active".equals(current.status) && current.currentXp>=current.targetXp) current.status="completed";
        // Keep the original website UUID. A stopped website goal stays stopped.
        if (!"stopped".equals(current.status)) request();
    }
    void start(String skill,boolean level,int levelNumber,int xp,int target)
    {
        stop(); current = new Goal(); current.id = UUID.randomUUID().toString(); current.skill = skill;
        current.mode = level ? "level" : "xp"; current.level = level ? levelNumber : null;
        current.startXp = current.currentXp = xp; current.targetXp = target; current.sequence = 1; current.status = "active";
        request();
    }
    void progress(int xp,boolean completed)
    {
        if (current == null || !"active".equals(current.status) || xp == current.currentXp) return;
        if (xp < current.currentXp) { stop(); return; }
        current = current.copy(); current.currentXp = xp; current.sequence++;
        if (completed) { current.status = "completed"; request(); }
    }
    void stop()
    {
        if (current == null || !"active".equals(current.status)) return;
        current = current.copy(); current.status = "stopped"; current.sequence++; enqueue();
    }
    void request()
    {
        if (!enabled.getAsBoolean()) { status = "Set Account tracking to On to sync"; return; }
        if (name.isEmpty() || token.isEmpty()) { status = "Link this character before syncing"; return; }
        if (current == null) { status = "Start a new local goal first"; return; }
        enqueue();
    }
    private void enqueue()
    {
        if (!enabled.getAsBoolean() || name.isEmpty() || token.isEmpty() || current == null) return;
        if (current.id.equals(savedId) && current.sequence <= savedSequence) return;
        if (pending.size() >= 8 && !pending.containsKey(current.id)) { status = "Sync backlog · retry this goal later"; return; }
        pending.put(current.id,current.copy()); status = "Goal queued for website";
    }
    void tick(long now)
    {
        if (!enabled.getAsBoolean()) { pending.clear(); status = "Website sync off · goals stay local"; return; }
        if (name.isEmpty() || token.isEmpty()) return;
        if (current != null && "active".equals(current.status) && now >= nextProgressAt) { enqueue(); nextProgressAt = now + 120_000L; }
        if (busy || pending.isEmpty() || now < nextSendAt) return;
        Goal sending = pending.values().iterator().next().copy(); long revision = generation;
        busy = true; nextSendAt = now + 2000L; status = "Syncing goal…";
        sender.send(name,token,api,sending,(response,error) -> {
            if (revision != generation) return;
            busy = false;
            if (!enabled.getAsBoolean()) { pending.clear(); status = "Website sync off · goals stay local"; return; }
            if (error != null || response == null || !response.ok) { nextSendAt = System.currentTimeMillis()+60_000L; status = "Sync unavailable · goal kept locally"; return; }
            Goal queued = pending.get(sending.id);
            if (current != null && current.id.equals(sending.id)) { savedId = sending.id; savedSequence = sending.sequence; }
            if (queued != null && queued.sequence <= sending.sequence) pending.remove(sending.id);
            if (current != null && current.id.equals(sending.id) && "stopped".equals(response.status))
            { current = current.copy(); current.status = "stopped"; pending.remove(sending.id); }
            status = "stopped".equals(response.status) ? "Stopped on website · local goal unchanged" : "Saved to Journey → Active tasks";
        });
    }
    String status() { return status; }
    static final class Response { boolean ok; String status; int sequence; }
    static final class Goal
    {
        String id,skill,mode,status;
        Integer level;
        int startXp,targetXp,currentXp,sequence;
        Goal copy()
        {
            Goal next = new Goal(); next.id = id; next.skill = skill; next.mode = mode; next.status = status; next.level = level;
            next.startXp = startXp; next.targetXp = targetXp; next.currentXp = currentXp; next.sequence = sequence; return next;
        }
    }
}
