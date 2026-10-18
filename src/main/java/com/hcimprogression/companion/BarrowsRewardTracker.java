package com.hcimprogression.companion;

import java.util.function.IntConsumer;
import java.util.function.Supplier;

/** Dedupe by the game's chest count, not time or the remaining reward items. */
final class BarrowsRewardTracker
{
    private final Supplier<Integer> lastCapturedChest;
    private final IntConsumer saveCapturedChest;

    BarrowsRewardTracker(Supplier<Integer> lastCapturedChest, IntConsumer saveCapturedChest)
    {
        this.lastCapturedChest = lastCapturedChest;
        this.saveCapturedChest = saveCapturedChest;
    }

    boolean capture(int chestCount, Runnable recordLoot)
    {
        Integer previous = lastCapturedChest.get();
        if (chestCount <= 0 || (previous != null && chestCount <= previous))
        {
            return false;
        }
        recordLoot.run();
        // Save only after the loot recorder succeeds. The supplied storage is
        // RS-profile scoped, so restarts and character switches retain safety.
        saveCapturedChest.accept(chestCount);
        return true;
    }
}
