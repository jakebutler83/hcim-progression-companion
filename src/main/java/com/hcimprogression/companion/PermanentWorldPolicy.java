package com.hcimprogression.companion;

import java.util.Set;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.WorldType;

/** Only the permanent standard account may update its website progression. */
final class PermanentWorldPolicy
{
    static final String PAUSED = "Tracking paused on temporary/separate-mode worlds. Return to a normal world.";
    private PermanentWorldPolicy() { }

    static boolean allows(Set<WorldType> types)
    {
        if (types == null) return false;
        for (WorldType type : types)
        {
            switch (type)
            {
                case PVP_ARENA: case BETA_WORLD: case NOSAVE_MODE: case TOURNAMENT_WORLD:
                case LAST_MAN_STANDING: case QUEST_SPEEDRUNNING: case SEASONAL: case DEADMAN: case HARDCORE_MAIN:
                    return false;
                default: break;
            }
        }
        return true; // Regular members, F2P, PvP and high-risk worlds retain real progress.
    }

    static boolean allows(Client client)
    {
        return client.getGameState() == GameState.LOGGED_IN && allows(client.getWorldType());
    }

    /** Let login/hop stat packets settle before restoring permanent goals. */
    static final class Gate
    {
        private int stableTicks;
        void reset() { stableTicks = 0; }
        boolean observe(boolean loggedIn, Set<WorldType> types, boolean gameTick)
        {
            if (!loggedIn || !allows(types)) { reset(); return false; }
            if (gameTick) stableTicks = Math.min(2, stableTicks + 1);
            return stableTicks >= 2;
        }
    }
}
