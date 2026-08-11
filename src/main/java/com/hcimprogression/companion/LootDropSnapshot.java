package com.hcimprogression.companion;

import java.util.ArrayList;
import java.util.List;

public class LootDropSnapshot
{
    private String dropId;
    private long occurredAt;
    private String source;
    private int npcId;
    private String slayerTask;
    private long totalValue;
    private List<LootItemSnapshot> items = new ArrayList<>();

    public LootDropSnapshot()
    {
    }

    public LootDropSnapshot(
        String dropId,
        long occurredAt,
        String source,
        int npcId,
        String slayerTask,
        long totalValue,
        List<LootItemSnapshot> items)
    {
        this.dropId = dropId;
        this.occurredAt = occurredAt;
        this.source = source;
        this.npcId = npcId;
        this.slayerTask = slayerTask;
        this.totalValue = totalValue;
        this.items = items == null ? new ArrayList<>() : new ArrayList<>(items);
    }

    public String getDropId() { return dropId; }
    public long getOccurredAt() { return occurredAt; }
    public String getSource() { return source; }
    public int getNpcId() { return npcId; }
    public String getSlayerTask() { return slayerTask; }
    public long getTotalValue() { return totalValue; }
    public List<LootItemSnapshot> getItems() { return items; }

    public static class LootItemSnapshot
    {
        private int itemId;
        private String name;
        private int quantity;
        private int unitPrice;
        private long totalValue;

        public LootItemSnapshot()
        {
        }

        public LootItemSnapshot(int itemId, String name, int quantity, int unitPrice, long totalValue)
        {
            this.itemId = itemId;
            this.name = name;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.totalValue = totalValue;
        }

        public int getItemId() { return itemId; }
        public String getName() { return name; }
        public int getQuantity() { return quantity; }
        public int getUnitPrice() { return unitPrice; }
        public long getTotalValue() { return totalValue; }
    }
}
