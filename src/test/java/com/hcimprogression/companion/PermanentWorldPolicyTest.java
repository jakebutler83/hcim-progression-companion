package com.hcimprogression.companion;

import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicReference;
import com.google.gson.Gson;
import net.runelite.api.WorldType;
import okhttp3.OkHttpClient;
import org.junit.Test;
import static org.junit.Assert.*;

public class PermanentWorldPolicyTest
{
    @Test public void loginAndReturnFromArenaWaitForTwoNormalTicks()
    {
        PermanentWorldPolicy.Gate gate = new PermanentWorldPolicy.Gate();
        java.util.Set<WorldType> normal = EnumSet.of(WorldType.MEMBERS);
        assertFalse(gate.observe(true,normal,false));
        assertFalse(gate.observe(true,normal,true));
        assertFalse(gate.observe(true,normal,false));
        assertTrue(gate.observe(true,normal,true));
        assertFalse(gate.observe(true,EnumSet.of(WorldType.PVP_ARENA),true));
        assertFalse(gate.observe(true,normal,false));
        assertFalse(gate.observe(true,normal,true));
        assertTrue(gate.observe(true,normal,true));
        gate.reset(); assertFalse(gate.observe(true,normal,false));
        assertFalse(gate.observe(false,normal,true));
    }
    @Test public void temporaryAndSeparateModesNeverWriteTheStandardCharacter()
    {
        for (WorldType type : WorldType.values())
        {
            boolean blocked = EnumSet.of(WorldType.PVP_ARENA, WorldType.BETA_WORLD, WorldType.NOSAVE_MODE,
                WorldType.TOURNAMENT_WORLD, WorldType.LAST_MAN_STANDING, WorldType.QUEST_SPEEDRUNNING,
                WorldType.SEASONAL, WorldType.DEADMAN).contains(type);
            assertEquals(type.name(), !blocked, PermanentWorldPolicy.allows(EnumSet.of(WorldType.MEMBERS,type)));
        }
        assertFalse(PermanentWorldPolicy.allows((java.util.Set<WorldType>)null));
        assertTrue(PermanentWorldPolicy.allows(EnumSet.noneOf(WorldType.class)));
        assertTrue(PermanentWorldPolicy.allows(EnumSet.of(WorldType.PVP,WorldType.HIGH_RISK)));
    }

    @Test public void pausedTransportRejectsQueuedGoalAndAccountUploadsWithoutHttp()
    {
        SyncService service = new SyncService(new OkHttpClient(),new Gson());
        service.trackingWorldAllowed(false);
        AtomicReference<String> error = new AtomicReference<>();
        service.syncSkillGoal("https://invalid.example", "fake-token", "Shiba Sitter", SavedSkillGoalsTest.goal(),
            (value,message) -> error.set(message));
        assertEquals(PermanentWorldPolicy.PAUSED,error.get());
        AccountSnapshot snapshot=new AccountSnapshot(); snapshot.setPlayerName("Shiba Sitter");
        service.syncAccount("https://invalid.example","fake-token",snapshot,"game-event",
            (value,message) -> error.set(message));
        assertEquals(PermanentWorldPolicy.PAUSED,error.get());
        service.trackingWorldAllowed(true);
        service.syncSkillGoal("http://invalid.example", "fake-token", "Shiba Sitter", SavedSkillGoalsTest.goal(),
            (value,message) -> error.set(message));
        assertEquals("API URL must begin with https://",error.get());
    }
}
