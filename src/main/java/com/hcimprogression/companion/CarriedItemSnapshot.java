package com.hcimprogression.companion;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.client.game.ItemManager;

/** Reads only inventory/equipment, on the client thread. Notes are not usable supplies. */
final class CarriedItemSnapshot
{
    static Map<String, Long> capture(ItemManager manager, ItemContainer inventory, ItemContainer equipment)
    {
        Map<String, Long> result = new HashMap<>();
        add(result, manager, inventory); add(result, manager, equipment);
        return Collections.unmodifiableMap(result);
    }

    private static void add(Map<String, Long> result, ItemManager manager, ItemContainer container)
    {
        if (container == null) return;
        for (Item item : container.getItems())
        {
            if (item == null || item.getId() < 0 || item.getQuantity() <= 0) continue;
            ItemComposition composition = manager.getItemComposition(item.getId());
            supply(result, composition.getName(), item.getQuantity(), composition.getNote() != -1,
                composition.getPlaceholderTemplateId() != -1);
        }
    }

    static void supply(Map<String, Long> result, String name, int quantity, boolean noted, boolean placeholder)
    {
        if (noted || placeholder || quantity <= 0 || name == null || name.isEmpty() || "null".equals(name)) return;
        result.merge(PackingChecklist.name(name), (long) quantity, Long::sum);
    }
}
