package com.hcimprogression.companion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Local-only edge detection over character-scoped Time Tracking snapshots. */
final class RoutineReminderTracker
{
    private final Map<String, Timer> timers = new HashMap<>();

    void reset() { timers.clear(); }

    List<String> observe(BirdhouseSnapshot birdhouses, FarmRunSnapshot farming, long nowSeconds)
    {
        Set<String> observed = new HashSet<>();
        int birds = 0, patches = 0;
        String firstPatch = "";
        if (birdhouses != null)
        {
            for (BirdhouseSnapshot.House house : birdhouses.getHouses())
            {
                if (observed.size() >= 200) break;
                if (changed("bird:" + house.getName(), house.getState(), house.getLastChangedAt(),
                    house.getReadyAt(), nowSeconds, observed)) birds++;
            }
        }
        if (farming != null)
        {
            for (FarmRunSnapshot.Patch patch : farming.getPatches())
            {
                if (observed.size() >= 200) break;
                if ("Compost".equalsIgnoreCase(patch.getType())) continue;
                if (changed("farm:" + patch.getId(), patch.getState(), patch.getLastChangedAt(),
                    patch.getReadyAt(), nowSeconds, observed))
                {
                    patches++;
                    if (firstPatch.isEmpty()) firstPatch = ProgressionFeedback.clean(patch.getLocation() + " " + patch.getType());
                }
            }
        }
        // Missing/disabled tracking data cannot leave an old armed timer alive.
        timers.keySet().retainAll(observed);
        List<String> messages = new ArrayList<>();
        if (birds > 0) messages.add(birds + " birdhouse" + (birds == 1 ? " is" : "s are") + " ready to check. Time for a quick Fossil Island trip!");
        if (patches > 0) messages.add(patches == 1 ? firstPatch + " is ready to check."
            : patches + " tracked farming patches are ready to check. Grab your farming kit!");
        return messages;
    }

    private boolean changed(String id, String state, long cycle, long due, long now, Set<String> observed)
    {
        if (cycle <= 0 || cycle > now + 300 || id.length() > 160) return false;
        observed.add(id);
        Timer previous = timers.get(id);
        boolean growing = "growing".equals(state) && due > now;
        boolean ready = "ready".equals(state) || ("growing".equals(state) && due > 0 && due <= now);
        if (growing)
        {
            if (previous == null || previous.cycle != cycle) timers.put(id, new Timer(cycle, true));
            return false;
        }
        // A ready patch first seen at login is a baseline, not a new alert.
        boolean notify = ready && previous != null && previous.armed;
        timers.put(id, new Timer(cycle, false));
        return notify;
    }

    private static final class Timer
    {
        private final long cycle;
        private final boolean armed;
        private Timer(long cycle, boolean armed) { this.cycle = cycle; this.armed = armed; }
    }
}
