package com.hcimprogression.companion;

import java.util.ArrayList;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.VarPlayer;

public class AccountSnapshotService
{
    public AccountSnapshot createSnapshot(Client client, CollectionLogCaptureService collectionLogCaptureService)
    {
        Player player = client.getLocalPlayer();
        if (player == null) return null;

        AccountSnapshot snapshot = new AccountSnapshot();
        snapshot.setPlayerName(player.getName());

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
        snapshot.getCollectionLog().put("logged", client.getVarpValue(VarPlayer.CLOG_LOGGED));
        snapshot.getCollectionLog().put("total", client.getVarpValue(VarPlayer.CLOG_TOTAL));
        collectionLogCaptureService.applyTo(snapshot);
        return snapshot;
    }

    private String displaySkillName(Skill skill)
    {
        String lower = skill.getName().toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
