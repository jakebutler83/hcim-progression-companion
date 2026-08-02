package com.hcimprogression.companion;

import java.util.ArrayList;
import java.util.List;

/** Share-safe summary of the OSRS TCG collection stored by the separate TCG plugin. */
public class TcgCollectionSnapshot
{
    private boolean available;
    private int schemaVersion;
    private long credits;
    private long openedPacks;
    private int totalCardsOwned;
    private int uniqueOwned;
    private int uniqueFoilOwned;
    private int totalCardPool;
    private double completionPct;
    private double foilCompletionPct;
    private long updatedAt;
    private final List<CardSnapshot> cards = new ArrayList<>();

    public static TcgCollectionSnapshot unavailable()
    {
        return new TcgCollectionSnapshot();
    }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
    public int getSchemaVersion() { return schemaVersion; }
    public void setSchemaVersion(int schemaVersion) { this.schemaVersion = schemaVersion; }
    public long getCredits() { return credits; }
    public void setCredits(long credits) { this.credits = credits; }
    public long getOpenedPacks() { return openedPacks; }
    public void setOpenedPacks(long openedPacks) { this.openedPacks = openedPacks; }
    public int getTotalCardsOwned() { return totalCardsOwned; }
    public void setTotalCardsOwned(int totalCardsOwned) { this.totalCardsOwned = totalCardsOwned; }
    public int getUniqueOwned() { return uniqueOwned; }
    public void setUniqueOwned(int uniqueOwned) { this.uniqueOwned = uniqueOwned; }
    public int getUniqueFoilOwned() { return uniqueFoilOwned; }
    public void setUniqueFoilOwned(int uniqueFoilOwned) { this.uniqueFoilOwned = uniqueFoilOwned; }
    public int getTotalCardPool() { return totalCardPool; }
    public void setTotalCardPool(int totalCardPool) { this.totalCardPool = totalCardPool; }
    public double getCompletionPct() { return completionPct; }
    public void setCompletionPct(double completionPct) { this.completionPct = completionPct; }
    public double getFoilCompletionPct() { return foilCompletionPct; }
    public void setFoilCompletionPct(double foilCompletionPct) { this.foilCompletionPct = foilCompletionPct; }
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
    public List<CardSnapshot> getCards() { return cards; }

    public static class CardSnapshot
    {
        private final String name;
        private final boolean foil;
        private final int quantity;

        public CardSnapshot(String name, boolean foil, int quantity)
        {
            this.name = name;
            this.foil = foil;
            this.quantity = quantity;
        }

        public String getName() { return name; }
        public boolean isFoil() { return foil; }
        public int getQuantity() { return quantity; }
    }
}
