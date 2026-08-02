package com.hcimprogression.companion;

import java.util.LinkedHashMap;
import java.util.Map;
import net.runelite.api.Client;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.client.config.ConfigManager;

/** Reads the same four Fossil Island varps used by RuneLite's Time Tracking plugin. */
public class BirdhouseTracker
{
    private static final long DURATION_SECONDS = 50 * 60L;
    private static final String CONFIG_GROUP = "hcimprogression.birdhouses";
    private static final HouseDef[] HOUSES = {
        new HouseDef("Mushroom Meadow (North)", VarPlayerID.BIRDHOUSE_TRANSMIT_A),
        new HouseDef("Mushroom Meadow (South)", VarPlayerID.BIRDHOUSE_TRANSMIT_B),
        new HouseDef("Verdant Valley (Northeast)", VarPlayerID.BIRDHOUSE_TRANSMIT_C),
        new HouseDef("Verdant Valley (Southwest)", VarPlayerID.BIRDHOUSE_TRANSMIT_D)
    };

    private final ConfigManager configManager;
    private final Map<Integer, State> states = new LinkedHashMap<>();

    public BirdhouseTracker(ConfigManager configManager)
    {
        this.configManager = configManager;
    }

    public boolean update(Client client)
    {
        long now = System.currentTimeMillis() / 1000L;
        boolean changed = false;
        for (HouseDef house : HOUSES)
        {
            int value = client.getVarpValue(house.varp);
            State previous = states.get(house.varp);
            if (previous == null)
            {
                long stored = readTimestamp(house.varp, value);
                states.put(house.varp, new State(value, stored > 0 ? stored : now));
                changed = stored <= 0;
            }
            else if (previous.value != value)
            {
                State next = new State(value, now);
                states.put(house.varp, next);
                writeTimestamp(house.varp, value, now);
                changed = true;
            }
        }
        return changed;
    }

    public BirdhouseSnapshot snapshot()
    {
        BirdhouseSnapshot result = new BirdhouseSnapshot();
        long now = System.currentTimeMillis() / 1000L;
        long nextReady = Long.MAX_VALUE;
        for (HouseDef house : HOUSES)
        {
            State state = states.get(house.varp);
            if (state == null) continue;
            String status = status(state.value, state.changedAt, now);
            long readyAt = state.value > 0 && state.value % 3 == 0 ? state.changedAt + DURATION_SECONDS : 0;
            result.getHouses().add(new BirdhouseSnapshot.House(house.name, status, state.changedAt, readyAt));
            result.setTrackedCount(result.getTrackedCount() + 1);
            if (state.value > 0 && state.value % 3 == 0)
            {
                result.setSeededCount(result.getSeededCount() + 1);
                if (readyAt <= now) result.setReadyCount(result.getReadyCount() + 1);
                else nextReady = Math.min(nextReady, readyAt);
            }
        }
        result.setNextReadyAt(nextReady == Long.MAX_VALUE ? 0 : nextReady);
        return result;
    }

    private String status(int value, long changedAt, long now)
    {
        if (value <= 0) return "empty";
        if (value % 3 != 0) return "needs-seeds";
        return changedAt + DURATION_SECONDS <= now ? "ready" : "growing";
    }

    private long readTimestamp(int varp, int value)
    {
        String stored = configManager.getRSProfileConfiguration(CONFIG_GROUP, key(varp));
        if (stored == null) return 0;
        String[] parts = stored.split(":");
        if (parts.length != 2) return 0;
        try { return Integer.parseInt(parts[0]) == value ? Long.parseLong(parts[1]) : 0; }
        catch (NumberFormatException ignored) { return 0; }
    }

    private void writeTimestamp(int varp, int value, long timestamp)
    {
        configManager.setRSProfileConfiguration(CONFIG_GROUP, key(varp), value + ":" + timestamp);
    }

    private String key(int varp) { return "house." + varp; }

    private static class HouseDef
    {
        private final String name;
        private final int varp;
        private HouseDef(String name, int varp) { this.name = name; this.varp = varp; }
    }

    private static class State
    {
        private final int value;
        private final long changedAt;
        private State(int value, long changedAt) { this.value = value; this.changedAt = changedAt; }
    }
}
