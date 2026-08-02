package com.hcimprogression.companion;

import java.util.ArrayList;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.VarPlayer;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.gameval.VarPlayerID;

public class AccountSnapshotService
{
    public AccountSnapshot createSnapshot(Client client, CollectionLogCaptureService collectionLogCaptureService, BirdhouseTracker birdhouseTracker)
    {
        Player player = client.getLocalPlayer();
        if (player == null) return null;

        AccountSnapshot snapshot = new AccountSnapshot();
        snapshot.setPlayerName(player.getName());
        snapshot.setQuestPoints(Math.max(0, client.getVarpValue(VarPlayer.QUEST_POINTS)));

        for (Skill skill : Skill.values())
        {
            if (skill == Skill.OVERALL) continue;
            snapshot.getSkills().put(
                displaySkillName(skill),
                new AccountSnapshot.SkillSnapshot(
                    client.getRealSkillLevel(skill),
                    client.getSkillExperience(skill)
                )
            );
        }

        List<String> completed = new ArrayList<>();
        for (Quest quest : Quest.values())
        {
            try
            {
                if (quest.getState(client) == QuestState.FINISHED)
                {
                    completed.add(quest.getName());
                }
            }
            catch (RuntimeException ignored)
            {
                // A newly added quest can occasionally be unavailable on an older client revision.
            }
        }
        snapshot.setCompletedQuests(completed);
        applyAchievementDiaryCompletions(client, snapshot);
        snapshot.getCollectionLog().put("logged", client.getVarpValue(VarPlayer.CLOG_LOGGED));
        snapshot.getCollectionLog().put("total", client.getVarpValue(VarPlayer.CLOG_TOTAL));
        collectionLogCaptureService.applyTo(snapshot);
        if (birdhouseTracker != null) snapshot.setBirdhouses(birdhouseTracker.snapshot());
        snapshot.setSlayer(readSlayer(client));
        return snapshot;
    }

    private SlayerSnapshot readSlayer(Client client)
    {
        int targetId = Math.max(0, client.getVarpValue(VarPlayerID.SLAYER_TARGET));
        int remaining = Math.max(0, client.getVarpValue(VarPlayerID.SLAYER_COUNT));
        int points = Math.max(0, client.getVarbitValue(VarbitID.SLAYER_POINTS));
        int streak = Math.max(0, client.getVarbitValue(VarbitID.SLAYER_TASKS_COMPLETED));
        String task = targetId > 0 && client.getNpcDefinition(targetId) != null
            ? client.getNpcDefinition(targetId).getName() : (targetId > 0 ? "Task target #" + targetId : "No active task");
        int masterId = Math.max(0, client.getVarbitValue(VarbitID.SLAYER_MASTER));
        String master = masterId > 0 && client.getNpcDefinition(masterId) != null
            ? client.getNpcDefinition(masterId).getName() : "Unknown master";
        return new SlayerSnapshot(task, remaining, points, streak, master, targetId, System.currentTimeMillis());
    }

    private void applyAchievementDiaryCompletions(Client client, AccountSnapshot snapshot)
    {
        putDiaryTiers(snapshot, "diary_ardy",
            completed(client, VarbitID.ARDOUGNE_DIARY_EASY_COMPLETE),
            completed(client, VarbitID.ARDOUGNE_DIARY_MEDIUM_COMPLETE),
            completed(client, VarbitID.ARDOUGNE_DIARY_HARD_COMPLETE),
            completed(client, VarbitID.ARDOUGNE_DIARY_ELITE_COMPLETE));
        putDiaryTiers(snapshot, "diary_lum",
            completed(client, VarbitID.LUMBRIDGE_DIARY_EASY_COMPLETE),
            completed(client, VarbitID.LUMBRIDGE_DIARY_MEDIUM_COMPLETE),
            completed(client, VarbitID.LUMBRIDGE_DIARY_HARD_COMPLETE),
            completed(client, VarbitID.LUMBRIDGE_DIARY_ELITE_COMPLETE));
        putDiaryTiers(snapshot, "diary_var",
            completed(client, VarbitID.VARROCK_DIARY_EASY_COMPLETE),
            completed(client, VarbitID.VARROCK_DIARY_MEDIUM_COMPLETE),
            completed(client, VarbitID.VARROCK_DIARY_HARD_COMPLETE),
            completed(client, VarbitID.VARROCK_DIARY_ELITE_COMPLETE));
        putDiaryTiers(snapshot, "diary_fal",
            completed(client, VarbitID.FALADOR_DIARY_EASY_COMPLETE),
            completed(client, VarbitID.FALADOR_DIARY_MEDIUM_COMPLETE),
            completed(client, VarbitID.FALADOR_DIARY_HARD_COMPLETE),
            completed(client, VarbitID.FALADOR_DIARY_ELITE_COMPLETE));
        putDiaryTiers(snapshot, "diary_kar",
            client.getVarbitValue(VarbitID.KARAMJA_EASY_COUNT) >= 10,
            client.getVarbitValue(VarbitID.KARAMJA_MED_COUNT) >= 19,
            client.getVarbitValue(VarbitID.KARAMJA_HARD_COUNT) >= 10,
            completed(client, VarbitID.KARAMJA_DIARY_ELITE_COMPLETE));
        putDiaryTiers(snapshot, "diary_kan",
            completed(client, VarbitID.KANDARIN_DIARY_EASY_COMPLETE),
            completed(client, VarbitID.KANDARIN_DIARY_MEDIUM_COMPLETE),
            completed(client, VarbitID.KANDARIN_DIARY_HARD_COMPLETE),
            completed(client, VarbitID.KANDARIN_DIARY_ELITE_COMPLETE));
        putDiaryTiers(snapshot, "diary_mor",
            completed(client, VarbitID.MORYTANIA_DIARY_EASY_COMPLETE),
            completed(client, VarbitID.MORYTANIA_DIARY_MEDIUM_COMPLETE),
            completed(client, VarbitID.MORYTANIA_DIARY_HARD_COMPLETE),
            completed(client, VarbitID.MORYTANIA_DIARY_ELITE_COMPLETE));
        putDiaryTiers(snapshot, "diary_frem",
            completed(client, VarbitID.FREMENNIK_DIARY_EASY_COMPLETE),
            completed(client, VarbitID.FREMENNIK_DIARY_MEDIUM_COMPLETE),
            completed(client, VarbitID.FREMENNIK_DIARY_HARD_COMPLETE),
            completed(client, VarbitID.FREMENNIK_DIARY_ELITE_COMPLETE));
        putDiaryTiers(snapshot, "diary_des",
            completed(client, VarbitID.DESERT_DIARY_EASY_COMPLETE),
            completed(client, VarbitID.DESERT_DIARY_MEDIUM_COMPLETE),
            completed(client, VarbitID.DESERT_DIARY_HARD_COMPLETE),
            completed(client, VarbitID.DESERT_DIARY_ELITE_COMPLETE));
        putDiaryTiers(snapshot, "diary_west",
            completed(client, VarbitID.WESTERN_DIARY_EASY_COMPLETE),
            completed(client, VarbitID.WESTERN_DIARY_MEDIUM_COMPLETE),
            completed(client, VarbitID.WESTERN_DIARY_HARD_COMPLETE),
            completed(client, VarbitID.WESTERN_DIARY_ELITE_COMPLETE));
        putDiaryTiers(snapshot, "diary_wild",
            completed(client, VarbitID.WILDERNESS_DIARY_EASY_COMPLETE),
            completed(client, VarbitID.WILDERNESS_DIARY_MEDIUM_COMPLETE),
            completed(client, VarbitID.WILDERNESS_DIARY_HARD_COMPLETE),
            completed(client, VarbitID.WILDERNESS_DIARY_ELITE_COMPLETE));
        putDiaryTiers(snapshot, "diary_kou",
            completed(client, VarbitID.KOUREND_DIARY_EASY_COMPLETE),
            completed(client, VarbitID.KOUREND_DIARY_MEDIUM_COMPLETE),
            completed(client, VarbitID.KOUREND_DIARY_HARD_COMPLETE),
            completed(client, VarbitID.KOUREND_DIARY_ELITE_COMPLETE));
    }

    private boolean completed(Client client, int varbitId)
    {
        return client.getVarbitValue(varbitId) > 0;
    }

    private void putDiaryTiers(AccountSnapshot snapshot, String prefix, boolean easy, boolean medium, boolean hard, boolean elite)
    {
        snapshot.getDiaryCompletions().put(prefix + "_easy", easy || medium || hard || elite);
        snapshot.getDiaryCompletions().put(prefix + "_med", medium || hard || elite);
        snapshot.getDiaryCompletions().put(prefix + "_hard", hard || elite);
        snapshot.getDiaryCompletions().put(prefix + "_elite", elite);
    }

    private String displaySkillName(Skill skill)
    {
        String lower = skill.getName().toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
