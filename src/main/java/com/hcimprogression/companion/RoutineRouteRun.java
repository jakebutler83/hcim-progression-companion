package com.hcimprogression.companion;

import java.util.List;

/** Pure, manual checklist state. Completion never changes game timers or progress. */
final class RoutineRouteRun
{
    private RoutineRoutePlan plan;
    private int position = -1;
    boolean start(RoutineRoutePlan next)
    {
        if (next == null || !next.valid()) return false;
        plan = new RoutineRoutePlan(next.kind, next.items, next.stops, next.revision);
        position = -1; return true;
    }
    boolean active() { return plan != null; }
    int position() { return position; }
    String packingItems() { return plan == null ? "" : plan.items; }
    void stop() { plan = null; position = -1; }
    void next() { if (plan != null && ++position >= RoutineRoutePlan.lines(plan.stops).size()) stop(); }
    void back() { if (plan != null) position = Math.max(-1, position - 1); }
    String title()
    {
        if (plan == null) return "";
        return ("farm".equals(plan.kind) ? "Farm run" : "Birdhouse run")
            + (position < 0 ? " · packing" : " · " + (position + 1) + "/" + RoutineRoutePlan.lines(plan.stops).size());
    }
    String text()
    {
        if (plan == null) return "";
        List<String> entries = RoutineRoutePlan.lines(position < 0 ? plan.items : plan.stops);
        return position < 0 ? "Review the packing list in Route Helper, then Next." : entries.get(position);
    }
}
