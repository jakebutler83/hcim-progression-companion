package com.hcimprogression.companion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.VarPlayer;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.DBTableID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.Item;
import net.runelite.client.game.ItemManager;

public class AccountSnapshotService
{
    public AccountSnapshot createSnapshot(Client client, CollectionLogCaptureService collectionLogCaptureService, BirdhouseTracker birdhouseTracker, FarmRunTracker farmRunTracker, ItemManager itemManager)
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
        if (farmRunTracker != null)
        {
            snapshot.setFarmRuns(farmRunTracker.snapshot());
            System.out.println("[HCIM] farm snapshot patches=" + snapshot.getFarmRuns().getPatches().size());
        }
        snapshot.setSlayer(readSlayer(client));
        captureWornEquipment(client, itemManager, snapshot);
        return snapshot;
    }

    private void captureWornEquipment(Client client, ItemManager itemManager, AccountSnapshot snapshot)
    {
        if (itemManager == null || client.getItemContainer(InventoryID.WORN) == null) return;
        Item[] items = client.getItemContainer(InventoryID.WORN).getItems();
        for (Map.Entry<String, EquipmentInventorySlot> entry : new LinkedHashMap<String, EquipmentInventorySlot>() {{
            put("head", EquipmentInventorySlot.HEAD); put("cape", EquipmentInventorySlot.CAPE); put("amulet", EquipmentInventorySlot.AMULET); put("weapon", EquipmentInventorySlot.WEAPON); put("body", EquipmentInventorySlot.BODY); put("shield", EquipmentInventorySlot.SHIELD); put("legs", EquipmentInventorySlot.LEGS); put("gloves", EquipmentInventorySlot.GLOVES); put("boots", EquipmentInventorySlot.BOOTS); put("ring", EquipmentInventorySlot.RING); put("ammo", EquipmentInventorySlot.AMMO);
        }}.entrySet()) {
            int index = entry.getValue().getSlotIdx();
            if (index < 0 || index >= items.length || items[index] == null || items[index].getId() <= 0) continue;
            String name = itemManager.getItemComposition(items[index].getId()).getName();
            if (name != null && !name.isEmpty()) snapshot.getWornEquipment().put(entry.getKey(), name);
        }
    }

    private SlayerSnapshot readSlayer(Client client)
    {
        int targetId = Math.max(0, client.getVarpValue(VarPlayerID.SLAYER_TARGET));
        int remaining = Math.max(0, client.getVarpValue(VarPlayerID.SLAYER_COUNT));
        int points = Math.max(0, client.getVarbitValue(VarbitID.SLAYER_POINTS));
        int streak = Math.max(0, client.getVarbitValue(VarbitID.SLAYER_TASKS_COMPLETED));
        String task = readSlayerTask(client, targetId, remaining);
        int masterId = Math.max(0, client.getVarbitValue(VarbitID.SLAYER_MASTER));
        String master = slayerMasterName(masterId);
        return new SlayerSnapshot(task, remaining, points, streak, master, targetId, System.currentTimeMillis());
    }

    private String readSlayerTask(Client client, int targetId, int remaining)
    {
        if (targetId <= 0 || remaining <= 0) return "No active task";
        try
        {
            int taskRow;
            if (targetId == 98)
            {
                var bossRows = client.getDBRowsByValue(DBTableID.SlayerTaskSublist.ID,
                    DBTableID.SlayerTaskSublist.COL_TASK_SUBTABLE_ID, 0,
                    client.getVarbitValue(VarbitID.SLAYER_TARGET_BOSSID));
                if (bossRows.isEmpty()) return "Task target #" + targetId;
                taskRow = (Integer) client.getDBTableField(bossRows.get(0), DBTableID.SlayerTaskSublist.COL_TASK, 0)[0];
            }
            else
            {
                var taskRows = client.getDBRowsByValue(DBTableID.SlayerTask.ID,
                    DBTableID.SlayerTask.COL_ID, 0, targetId);
                if (taskRows.isEmpty()) return "Task target #" + targetId;
                taskRow = taskRows.get(0);
            }
            Object[] name = client.getDBTableField(taskRow, DBTableID.SlayerTask.COL_NAME_UPPERCASE, 0);
            if (name.length > 0 && name[0] instanceof String && !((String) name[0]).isEmpty()) return (String) name[0];
        }
        catch (RuntimeException ignored)
        {
            // Keep the sync alive if Jagex changes the DB schema between client revisions.
        }
        return "Task target #" + targetId;
    }

    private String slayerMasterName(int id)
    {
        switch (id)
        {
            case 0: return "Turael";
            case 1: return "Spria";
            case 2: return "Mazchna";
            case 4: return "Vannaka";
            case 5: return "Chaeldar";
            case 6: return "Nieve";
            case 7: return "Krystilia";
            case 8: return "Duradel";
            case 9: return "Konar";
            default: return "Unknown master";
        }
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
