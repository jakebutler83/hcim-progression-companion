package com.hcimprogression.companion;

import java.util.ArrayList;
import java.util.List;

/** Rune types and quantities currently stored in this character's rune pouch. */
public class RunePouchSnapshot
{
    private final List<RuneSnapshot> runes = new ArrayList<>();

    public List<RuneSnapshot> getRunes() { return runes; }

    public static class RuneSnapshot
    {
        private final int itemId;
        private final String name;
        private final int quantity;

        public RuneSnapshot(int itemId, String name, int quantity)
        {
            this.itemId = itemId;
            this.name = name;
            this.quantity = quantity;
        }

        public int getItemId() { return itemId; }
        public String getName() { return name; }
        public int getQuantity() { return quantity; }
    }
}
