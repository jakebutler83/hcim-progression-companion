package com.hcimprogression.companion;

import com.google.gson.Gson;
import com.google.inject.Guice;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

/** Pure ownership/planning fixtures. Never opens or operates RuneScape. */
public class SlayerBankPlannerTest
{
    // Inject the same resource deserializer type as production, rather than constructing Gson.
    private final SlayerBankPlanner planner = SlayerBankPlanner.load(Guice.createInjector().getInstance(Gson.class));
    private static Map<String, Integer> levels()
    {
        Map<String, Integer> result = new HashMap<>();
        for (String skill : new String[]{"Attack", "Strength", "Defence", "Ranged", "Magic", "Prayer", "Slayer", "Agility", "Hitpoints"}) result.put(skill, 99);
        return result;
    }
    private static List<SlayerBankPlanner.Owned> bank(String... names)
    {
        List<SlayerBankPlanner.Owned> result = new ArrayList<>();
        // Fixture-only IDs, not live game item IDs.
        for (int i = 0; i < names.length; i++) result.add(new SlayerBankPlanner.Owned(i + 1, names[i], 1));
        return result;
    }
    private SlayerBankPlanner.Plan plan(String target, String style, String... names) { return planner.plan(target, style, bank(names), levels()); }
    @Test public void emptyBankCannotInventAWornOrUnlockedWeapon()
    {
        SlayerBankPlanner.Plan result = plan("Abyssal demon", "melee");
        assertTrue(result.blocked); assertTrue(result.gear.isEmpty()); assertTrue(result.highlights().isEmpty());
    }
    @Test public void choosesHighestSupportedOwnedUsableGear()
    {
        SlayerBankPlanner.Plan result = plan("Abyssal demon", "melee", "Rune scimitar", "Abyssal whip", "Dragon boots", "Rune boots");
        assertFalse(result.blocked); assertEquals("Abyssal whip", result.gear.get("weapon").name);
        assertEquals("Dragon boots", result.gear.get("feet").name); assertEquals(2, result.highlights().size());
    }
    @Test public void filtersRequirementsAndFailsClosedWhenLevelUnknown()
    {
        Map<String, Integer> skills = levels(); skills.put("Attack", 50);
        SlayerBankPlanner.Plan result = planner.plan("Bloodveld", "melee", bank("Abyssal whip", "Rune scimitar"), skills);
        assertEquals("Rune scimitar", result.gear.get("weapon").name);
        assertTrue(planner.plan("Bloodveld", "melee", bank("Rune scimitar"), Collections.emptyMap()).blocked);
    }
    @Test public void excludesZeroQuantitiesAndInvalidIds()
    {
        assertTrue(planner.plan("Bloodveld", "melee", Arrays.asList(
            new SlayerBankPlanner.Owned(1, "Abyssal whip", 0), new SlayerBankPlanner.Owned(-1, "Rune scimitar", 2)), levels()).blocked);
    }
    @Test public void aliasesOnlyKnownUsableVariants()
    {
        assertEquals("slayer helmet (i)", planner.canonical("Black slayer helmet (i)"));
        assertEquals("amulet of glory", planner.canonical("Amulet of glory(6)"));
        assertEquals("verac's plateskirt", planner.canonical("Verac's plateskirt 75"));
        assertEquals("verac's plateskirt 0", planner.canonical("Verac's plateskirt 0"));
        assertEquals("black mask (i)", planner.canonical("Black mask (10) (i)"));
        assertEquals("bow of faerdhinen", planner.canonical("Bow of faerdhinen (c) (red)"));
        assertEquals("unknown armour (broken)", planner.canonical("Unknown armour (broken)"));
    }
    @Test public void twoHandedWeaponNeverIncludesShield()
    {
        SlayerBankPlanner.Plan result = plan("Abyssal demon", "melee", "Soulreaper axe", "Abyssal whip", "Dragon defender");
        assertEquals("Soulreaper axe", result.gear.get("weapon").name); assertFalse(result.gear.containsKey("shield"));
    }
    @Test public void bowsRequireCompatibleArrowsRatherThanBolts()
    {
        assertTrue(plan("Bloodveld", "ranged", "Magic shortbow", "Diamond bolts (e)").blocked);
        SlayerBankPlanner.Plan result = plan("Bloodveld", "ranged", "Magic shortbow", "Dragon arrow", "Rune arrow");
        assertFalse(result.blocked); assertEquals("Rune arrow", result.gear.get("ammo").name);
    }
    @Test public void runeCrossbowCannotUseDragonBolts()
    {
        assertTrue(plan("Bloodveld", "ranged", "Rune crossbow", "Diamond dragon bolts (e)").blocked);
        SlayerBankPlanner.Plan result = plan("Bloodveld", "ranged", "Rune crossbow", "Diamond dragon bolts (e)", "Diamond bolts (e)");
        assertFalse(result.blocked); assertEquals("Diamond bolts (e)", result.gear.get("ammo").name);
    }
    @Test public void incompleteCrystalSetFallsBackWithoutInventingPieces()
    {
        SlayerBankPlanner.Plan result = plan("Bloodveld", "ranged", "Bow of faerdhinen (c)", "Crystal body", "Rune crossbow", "Diamond bolts (e)");
        assertEquals("Rune crossbow", result.gear.get("weapon").name);
        result = plan("Bloodveld", "ranged", "Bow of faerdhinen (c)", "Crystal body", "Crystal legs", "Rune crossbow", "Diamond bolts (e)");
        assertEquals("Bow of faerdhinen (c)", result.gear.get("weapon").name);
        assertEquals("Crystal body", result.gear.get("body").name); assertEquals("Crystal legs", result.gear.get("legs").name);
    }
    @Test public void missingFaceProtectionWithholdsHighlights()
    {
        SlayerBankPlanner.Plan result = plan("Dust devil", "melee", "Abyssal whip", "Torva full helm");
        assertTrue(result.blocked); assertFalse(result.gear.containsKey("head")); assertTrue(result.highlights().isEmpty());
        result = plan("Dust devil", "melee", "Abyssal whip", "Torva full helm", "Facemask");
        assertFalse(result.blocked); assertEquals("Facemask", result.gear.get("head").name);
    }
    @Test public void mirrorTaskPrefersOneHandedWeaponAndRequiresMirror()
    {
        SlayerBankPlanner.Plan result = plan("Basilisk", "crush", "Abyssal bludgeon", "Zombie axe", "Mirror shield", "Dragon defender");
        assertFalse(result.blocked); assertEquals("Zombie axe", result.gear.get("weapon").name);
        assertEquals("Mirror shield", result.gear.get("shield").name);
        assertTrue(plan("Basilisk", "crush", "Zombie axe", "Dragon defender").blocked);
    }
    @Test public void dragonSetupRequiresActualDragonfireShieldAndNotDefender()
    {
        assertTrue(plan("Blue dragon", "melee", "Dragon hunter lance", "Dragon defender").blocked);
        SlayerBankPlanner.Plan result = plan("Blue dragon", "melee", "Dragon hunter lance", "Dragon defender", "Anti-dragon shield");
        assertFalse(result.blocked); assertEquals("Anti-dragon shield", result.gear.get("shield").name);
        assertTrue(String.join(" ", result.warnings).contains("not a safety guarantee"));
    }
    @Test public void unsupportedSpecialMonstersAndBossesDoNotReceiveGenericAdvice()
    {
        for (String target : new String[]{"Kurask", "Turoth", "Fever spider", "Killerwatt", "Cave horror", "Abyssal Sire", "Metal dragons", "Made up target"})
        {
            SlayerBankPlanner.Plan result = plan(target, "melee", "Abyssal whip", "Slayer helmet");
            assertTrue(target, result.blocked); assertTrue(target, result.gear.isEmpty());
        }
    }
    @Test public void allResourceProfilesResolveCandidatesAndSupportedStyles()
    {
        SlayerBankPlanner.Catalog catalog = Guice.createInjector().getInstance(Gson.class).fromJson(
            new java.io.InputStreamReader(getClass().getResourceAsStream("/slayer-gear.json"), java.nio.charset.StandardCharsets.UTF_8), SlayerBankPlanner.Catalog.class);
        for (Map<String, SlayerBankPlanner.Rule> profile : catalog.profiles.values())
        {
            assertEquals(6, profile.size());
            for (SlayerBankPlanner.Rule rule : profile.values()) for (List<String> slot : rule.slots.values())
                for (String name : slot) assertTrue(name, catalog.items.containsKey(name));
        }
        for (String target : new String[]{"Abyssal demon", "Dust devil", "Aberrant spectre", "Basilisk", "Wyrm", "Blue dragon"})
            for (String style : catalog.profiles.get("generic").keySet()) planner.plan(target, style, Collections.emptyList(), levels());
    }
    @Test public void highlightedIdsCannotBeMutatedByOverlayConsumer()
    {
        try { plan("Bloodveld", "melee", "Abyssal whip").highlights().clear(); fail("immutable"); }
        catch (UnsupportedOperationException expected) { }
    }
}
