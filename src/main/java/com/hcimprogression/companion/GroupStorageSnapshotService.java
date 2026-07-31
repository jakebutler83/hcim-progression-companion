package com.hcimprogression.companion;

import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Player;
import net.runelite.client.game.ItemManager;

public class GroupStorageSnapshotService
{
    public GroupStorageSnapshot createSnapshot(
        Client client,
        ItemManager itemManager,
        ItemContainer container)
    {
        Player player = client.getLocalPlayer();
        if (player == null || container == null)
        {
            return null;
        }

        GroupStorageSnapshot snapshot = new GroupStorageSnapshot();
        snapshot.setPlayerName(player.getName());
        snapshot.setTimestamp(System.currentTimeMillis());

        long totalQuantity = 0;
        int occupiedSlots = 0;
        Item[] items = container.getItems();
        if (items != null)
        {
            for (Item item : items)
            {
                if (item == null || item.getId() <= 0 || item.getQuantity() <= 0)
                {
                    continue;
                }

                int rawItemId = item.getId();
                int canonicalItemId = itemManager.canonicalize(rawItemId);
                if (canonicalItemId <= 0)
                {
                    canonicalItemId = rawItemId;
                }

                String name = itemManager.getItemComposition(rawItemId).getName();
                if (name == null || name.trim().isEmpty() || "null".equalsIgnoreCase(name))
                {
                    name = itemManager.getItemComposition(canonicalItemId).getName();
                }
                if (name == null || name.trim().isEmpty() || "null".equalsIgnoreCase(name))
                {
                    name = "Item " + canonicalItemId;
                }

                occupiedSlots++;
                totalQuantity += item.getQuantity();
                snapshot.getItems().add(new GroupStorageSnapshot.GroupStorageItem(
                    canonicalItemId,
                    rawItemId,
                    name.trim(),
                    item.getQuantity()
                ));
            }
        }

        snapshot.setOccupiedSlots(occupiedSlots);
        snapshot.setTotalQuantity(totalQuantity);
        return snapshot;
    }
}
