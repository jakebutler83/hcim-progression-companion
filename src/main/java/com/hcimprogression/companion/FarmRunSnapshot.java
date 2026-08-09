package com.hcimprogression.companion;

import java.util.ArrayList;
import java.util.List;

/** RuneLite-side farming patch states used by the Farm Runs dashboard. */
public class FarmRunSnapshot
{
    private final List<Patch> patches = new ArrayList<>();
    public List<Patch> getPatches() { return patches; }

    public static class Patch
    {
        private final String id;
        private final String location;
        private final String type;
        private final String state;
        private final int rawState;
        private final long lastChangedAt;
        private final long readyAt;

        public Patch(String id, String location, String type, String state, int rawState, long lastChangedAt, long readyAt)
        {
            this.id = id; this.location = location; this.type = type; this.state = state;
            this.rawState = rawState; this.lastChangedAt = lastChangedAt; this.readyAt = readyAt;
        }
        public String getId() { return id; }
        public String getLocation() { return location; }
        public String getType() { return type; }
        public String getState() { return state; }
        public int getRawState() { return rawState; }
        public long getLastChangedAt() { return lastChangedAt; }
        public long getReadyAt() { return readyAt; }
    }
}
