package com.hcimprogression.companion;

/** A game-reported duration, never a wall-clock estimate or a kill-count inference. */
final class PersonalBestRecord
{
    final String key, worldMode, source;
    final long durationMs;

    PersonalBestRecord(String key, String worldMode, String source, long durationMs)
    {
        this.key = key; this.worldMode = worldMode; this.source = source; this.durationMs = durationMs;
    }
}
