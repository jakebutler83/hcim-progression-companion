package com.hcimprogression.companion;

import java.util.LinkedHashMap;
import java.util.Map;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Player;
import net.runelite.api.PlayerComposition;
import net.runelite.api.Varbits;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.kit.KitType;
import net.runelite.client.game.ItemManager;

public class SocialPresenceService
{
    private volatile Item[] latestWornItems;
    private static final Map<String, EquipmentInventorySlot> SHARED_SLOTS = new LinkedHashMap<>();
    private static final Map<String, KitType> COMPOSITION_FALLBACK_SLOTS = new LinkedHashMap<>();

    static
    {
        SHARED_SLOTS.put("head", EquipmentInventorySlot.HEAD);
        SHARED_SLOTS.put("cape", EquipmentInventorySlot.CAPE);
        SHARED_SLOTS.put("amulet", EquipmentInventorySlot.AMULET);
        SHARED_SLOTS.put("weapon", EquipmentInventorySlot.WEAPON);
        SHARED_SLOTS.put("body", EquipmentInventorySlot.BODY);
        SHARED_SLOTS.put("shield", EquipmentInventorySlot.SHIELD);
        SHARED_SLOTS.put("legs", EquipmentInventorySlot.LEGS);
        SHARED_SLOTS.put("gloves", EquipmentInventorySlot.GLOVES);
        SHARED_SLOTS.put("boots", EquipmentInventorySlot.BOOTS);
        SHARED_SLOTS.put("ring", EquipmentInventorySlot.RING);
        SHARED_SLOTS.put("ammo", EquipmentInventorySlot.AMMO);

        COMPOSITION_FALLBACK_SLOTS.put("head", KitType.HEAD);
        COMPOSITION_FALLBACK_SLOTS.put("cape", KitType.CAPE);
        COMPOSITION_FALLBACK_SLOTS.put("amulet", KitType.AMULET);
        COMPOSITION_FALLBACK_SLOTS.put("weapon", KitType.WEAPON);
        COMPOSITION_FALLBACK_SLOTS.put("body", KitType.TORSO);
        COMPOSITION_FALLBACK_SLOTS.put("shield", KitType.SHIELD);
        COMPOSITION_FALLBACK_SLOTS.put("legs", KitType.LEGS);
        COMPOSITION_FALLBACK_SLOTS.put("gloves", KitType.HANDS);
        COMPOSITION_FALLBACK_SLOTS.put("boots", KitType.BOOTS);
    }

    public void updateWornEquipment(ItemContainer container)
    {
        latestWornItems = container == null ? null : container.getItems().clone();
    }

    public SocialPresenceSnapshot createSnapshot(Client client, ItemManager itemManager, boolean includeExactCoordinates)
    {
        Player player = client.getLocalPlayer();
        if (player == null)
        {
            return null;
        }

        WorldPoint point = player.getWorldLocation();
        if (point == null)
        {
            return null;
        }

        SocialPresenceSnapshot snapshot = new SocialPresenceSnapshot();
        snapshot.setPlayerName(player.getName());
        snapshot.setWorld(client.getWorld());
        snapshot.setRegionId(point.getRegionID());
        snapshot.setRegionName(resolveRegion(point));
        snapshot.setCombatLevel(player.getCombatLevel());
        snapshot.setInWilderness(isInWilderness(client, point));
        snapshot.setActivity(resolveActivity(player, snapshot.getRegionName()));
        snapshot.setTimestamp(System.currentTimeMillis());
        snapshot.setExactLocationIncluded(includeExactCoordinates);

        if (includeExactCoordinates)
        {
            snapshot.setX(point.getX());
            snapshot.setY(point.getY());
            snapshot.setPlane(point.getPlane());
        }

        captureEquipment(client, itemManager, snapshot);
        return snapshot;
    }

    private void captureEquipment(Client client, ItemManager itemManager, SocialPresenceSnapshot snapshot)
    {
        Item[] items = latestWornItems;
        ItemContainer worn = client.getItemContainer(InventoryID.WORN);
        if (worn != null)
        {
            Item[] current = worn.getItems();
            if (current != null && current.length > 0)
            {
                items = current;
                latestWornItems = current.clone();
            }
        }

        if (items != null)
        {
            for (Map.Entry<String, EquipmentInventorySlot> entry : SHARED_SLOTS.entrySet())
            {
                int index = entry.getValue().getSlotIdx();
                if (index < 0 || index >= items.length)
                {
                    continue;
                }

                addEquipmentItem(snapshot, itemManager, entry.getKey(), items[index]);
            }
        }

        // Player composition is a useful fallback when the worn-item container is
        // temporarily incomplete during login, world hops, or equipment changes.
        Player player = client.getLocalPlayer();
        PlayerComposition composition = player == null ? null : player.getPlayerComposition();
        if (composition == null)
        {
            return;
        }

        for (Map.Entry<String, KitType> entry : COMPOSITION_FALLBACK_SLOTS.entrySet())
        {
            if (snapshot.getEquipment().containsKey(entry.getKey()))
            {
                continue;
            }

            int itemId = composition.getEquipmentId(entry.getValue());
            if (itemId <= 0)
            {
                continue;
            }

            addEquipmentItem(snapshot, itemManager, entry.getKey(), new Item(itemId, 1));
        }
    }

    private void addEquipmentItem(
        SocialPresenceSnapshot snapshot,
        ItemManager itemManager,
        String slot,
        Item item)
    {
        if (item == null || item.getId() <= 0)
        {
            return;
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
            name = "Item " + rawItemId;
        }

        snapshot.getEquipment().put(
            slot,
            new SocialPresenceSnapshot.EquipmentItem(canonicalItemId, rawItemId, name.trim())
        );
    }

    private String resolveActivity(Player player, String regionName)
    {
        Actor interacting = player.getInteracting();
        if (interacting != null && interacting.getName() != null && !interacting.getName().trim().isEmpty())
        {
            return "Fighting " + interacting.getName().trim();
        }

        if (player.getAnimation() != -1)
        {
            return "Active in " + regionName;
        }

        return "Exploring " + regionName;
    }

    @SuppressWarnings("deprecation")
    private boolean isInWilderness(Client client, WorldPoint point)
    {
        try
        {
            return client.getVarbitValue(Varbits.IN_WILDERNESS) == 1;
        }
        catch (RuntimeException ignored)
        {
            return point.getY() >= 3520 && point.getY() <= 3975 && point.getX() >= 2940 && point.getX() <= 3395;
        }
    }

    private String resolveRegion(WorldPoint point)
    {
        int x = point.getX();
        int y = point.getY();

        if (y >= 3520 && x >= 2940 && x <= 3395) return "Wilderness";
        if (x >= 1150 && x <= 2050 && y >= 3350 && y <= 4250) return "Kourend & Kebos";
        if (x >= 1200 && x <= 2050 && y >= 2750 && y < 3350) return "Varlamore";
        if (x >= 2050 && x < 2500 && y >= 3000 && y <= 3550) return "Tirannwn";
        if (x >= 2450 && x < 2950 && y >= 3550 && y <= 4250) return "Fremennik Province";
        if (x >= 2350 && x < 3050 && y >= 3000 && y < 3550) return "Kandarin";
        if (x >= 2750 && x < 3150 && y >= 2750 && y < 3250) return "Karamja";
        if (x >= 3000 && x < 3550 && y >= 2500 && y < 3050) return "Kharidian Desert";
        if (x >= 3350 && x <= 3950 && y >= 2750 && y <= 3700) return "Morytania";
        if (x >= 2750 && x < 3150 && y >= 3200 && y < 3700) return "Asgarnia";
        if (x >= 3050 && x < 3450 && y >= 3000 && y < 3550) return "Misthalin";
        if (x >= 2450 && x < 3000 && y >= 2750 && y < 3150) return "Feldip Hills";
        return "Gielinor";
    }
}
