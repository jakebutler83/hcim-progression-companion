package com.hcimprogression.companion;

import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Preparation priorities, not a combat simulator. Ownership comes only from a real bank read. */
final class SlayerBankPlanner
{
    static final class Candidate
    {
        String name, ammo, set;
        boolean twoHanded;
        Map<String, Integer> requirements = Collections.emptyMap();
    }
    static final class Rule
    {
        Map<String, List<String>> slots = Collections.emptyMap();
        List<Protection> protection = Collections.emptyList();
    }
    static final class Protection { String slot, label; }
    static final class Catalog
    {
        String reviewed;
        Map<String, String> aliases;
        Map<String, Candidate> items;
        Map<String, Map<String, Rule>> profiles;
    }
    static final class Owned
    {
        final int id, quantity;
        final String name;
        Owned(int id, String name, int quantity) { this.id = id; this.name = name; this.quantity = quantity; }
    }
    static final class Plan
    {
        final Map<String, Owned> gear = new LinkedHashMap<>();
        final List<String> warnings = new ArrayList<>();
        boolean blocked;
        Set<Integer> highlights()
        {
            Set<Integer> ids = new LinkedHashSet<>();
            if (!blocked) gear.values().forEach(item -> ids.add(item.id));
            return Collections.unmodifiableSet(ids);
        }
    }
    private final Catalog catalog;
    SlayerBankPlanner(Catalog catalog) { this.catalog = catalog; }
    static SlayerBankPlanner load(Gson gson)
    {
        try (InputStreamReader reader = new InputStreamReader(
            SlayerBankPlanner.class.getResourceAsStream("/slayer-gear.json"), StandardCharsets.UTF_8))
        {
            return new SlayerBankPlanner(gson.fromJson(reader, Catalog.class));
        }
        catch (java.io.IOException error) { throw new IllegalStateException("Could not load Slayer gear priorities", error); }
    }
    String reviewed() { return catalog.reviewed; }
    String canonical(String name)
    {
        String value = name == null ? "" : name.trim().toLowerCase(Locale.ROOT).replace('’', '\'').replaceAll("\\s+", " ");
        value = value.replaceAll("^(amulet of glory|ring of wealth|combat bracelet|skills necklace)\\s*\\([0-6]\\)$", "$1");
        value = value.replaceAll("^black mask \\((?:[0-9]|10)\\)( \\(i\\))?$", "black mask$1");
        value = value.replaceAll("^blade of saeldor \\(c\\)(?: \\([a-z]+\\))?$", "blade of saeldor");
        value = value.replaceAll("^bow of faerdhinen \\(c\\)(?: \\([a-z]+\\))?$", "bow of faerdhinen");
        if (value.matches("^(ahrim|dharok|guthan|karil|torag|verac)'s .*")) value = value.replaceAll(" (100|75|50|25)$", "");
        return catalog.aliases.getOrDefault(value, value);
    }
    Plan plan(String task, String style, List<Owned> bank, Map<String, Integer> levels)
    {
        Plan result = new Plan();
        String target = canonical(task);
        if (!target.matches("(?:abyssal demon|black demon|greater demon|lesser demon|hellhound|bloodveld|mutated bloodveld|nechryael|greater nechryael|jelly|warped jelly|ankou|dagannoth|dark beast|fire giant|hill giant|moss giant|ice giant|gargoyle|dust devil|smoke devil|aberrant spectre|deviant spectre|basilisk|basilisk knight|cockatrice|wyrm|drake|hydra|blue dragon|black dragon|green dragon|red dragon|bronze dragon|iron dragon|steel dragon)s?"))
        {
            result.blocked = true;
            result.warnings.add("This monster has no reviewed preparation profile yet. Gear and bank highlights are withheld; use Wiki info for its stats, locations and special equipment requirements.");
            return result;
        }
        String profile = "generic";
        if (target.matches(".*(dust devil|smoke devil).*")) profile = "mask";
        else if (target.matches(".*(aberrant|deviant spectre).*")) profile = "spectre";
        else if (target.contains("basilisk") || target.contains("cockatrice")) profile = "mirror";
        else if (target.matches(".*(wyrm|drake|hydra).*")) profile = "heat";
        else if (target.contains("dragon")) profile = "dragon";
        Rule base = catalog.profiles.get("generic").get(style);
        if (base == null) { result.blocked = true; result.warnings.add("Choose a supported combat style."); return result; }
        Rule special = catalog.profiles.get(profile).get(style);
        Map<String, List<String>> priorities = new LinkedHashMap<>(base.slots);
        priorities.putAll(special.slots);
        boolean shieldRequired = "dragon".equals(profile) || special.protection.stream().anyMatch(required -> "shield".equals(required.slot));
        Map<String, Owned> owned = new LinkedHashMap<>();
        for (Owned item : bank) if (item.id > 0 && item.quantity > 0) owned.putIfAbsent(canonical(item.name), item);
        boolean ranged = "ranged".equals(style);
        Candidate weapon = null;
        for (String key : priorities.getOrDefault("weapon", Collections.emptyList()))
        {
            Candidate candidate = catalog.items.get(key);
            if (!usable(key, owned, levels)) continue;
            if (shieldRequired && candidate.twoHanded) continue;
            if ("crystal".equals(candidate.set)
                && (!usable("crystal body", owned, levels) || !usable("crystal legs", owned, levels))) continue;
            if (ranged && candidate.ammo != null && !"internal".equals(candidate.ammo) && !"crystal".equals(candidate.ammo)
                && first(ammo(candidate, priorities), owned, levels) == null) continue;
            weapon = candidate; result.gear.put("weapon", owned.get(key)); break;
        }
        if (weapon == null) { result.blocked = true; result.warnings.add("No supported usable weapon found in this bank for the chosen style."); }
        if (weapon != null && "crystal".equals(weapon.set))
        {
            priorities.put("body", Collections.singletonList("crystal body"));
            priorities.put("legs", Collections.singletonList("crystal legs"));
        }
        if (ranged) priorities.put("ammo", ammo(weapon, priorities));
        for (Map.Entry<String, List<String>> slot : priorities.entrySet())
        {
            if ("weapon".equals(slot.getKey()) || ("shield".equals(slot.getKey()) && weapon != null && weapon.twoHanded)) continue;
            String chosen = first(slot.getValue(), owned, levels);
            if (chosen != null) result.gear.put(slot.getKey(), owned.get(chosen));
        }
        for (Protection required : special.protection)
        {
            if (!result.gear.containsKey(required.slot)) { result.blocked = true; result.warnings.add("Missing protection: " + required.label); }
        }
        if ("dragon".equals(profile) && !result.gear.containsKey("shield"))
        {
            result.blocked = true;
            result.warnings.add("Missing bank-owned dragonfire shield or anti-dragon shield. This conservative profile requires a shield; potion-only setups are not supported.");
        }
        if ("dragon".equals(profile)) result.warnings.add("Check current dragonfire and antifire requirements on the Wiki; a shield alone is not a safety guarantee.");
        if ("heat".equals(profile)) result.warnings.add("Heat boots are for Mount Karuulm; verify the location and any diary exemption.");
        if (target.contains("gargoyle")) result.warnings.add("Bring a rock hammer or verify your unlocked finishing alternative.");
        if ("burst".equals(style)) result.warnings.add("Verify Ancient Magicks, the selected spell and its runes before travelling.");
        result.warnings.add("Supported priority list, not guaranteed best DPS. Check quests, charges, ammunition and special task items. Empty slots mean no supported usable bank item was found.");
        return result;
    }
    private boolean usable(String key, Map<String, Owned> bank, Map<String, Integer> levels)
    {
        Candidate item = catalog.items.get(key);
        return bank.containsKey(key) && item != null && item.requirements.entrySet().stream()
            .allMatch(requirement -> levels.getOrDefault(requirement.getKey(), -1) >= requirement.getValue());
    }
    private String first(List<String> rows, Map<String, Owned> bank, Map<String, Integer> levels)
    {
        for (String key : rows) if (usable(key, bank, levels)) return key;
        return null;
    }
    private List<String> ammo(Candidate weapon, Map<String, List<String>> priorities)
    {
        if (weapon == null) return Collections.emptyList();
        if ("internal".equals(weapon.ammo) || "crystal".equals(weapon.ammo)) return catalog.profiles.get("generic").get("melee").slots.get("ammo");
        if (weapon.ammo != null && weapon.ammo.contains("arrow"))
        {
            List<String> arrows = new ArrayList<>();
            for (String name : new String[]{"dragon arrow", "amethyst arrow", "rune arrow", "adamant arrow"})
                if ("arrow".equals(weapon.ammo) || "rune-arrow".equals(weapon.ammo) && ("rune arrow".equals(name) || "adamant arrow".equals(name))
                    || "adamant-arrow".equals(weapon.ammo) && "adamant arrow".equals(name)) arrows.add(name);
            return arrows;
        }
        List<String> bolts = new ArrayList<>(priorities.getOrDefault("ammo", Collections.emptyList()));
        if ("rune-bolt".equals(weapon.ammo)) bolts.removeIf(name -> name.contains("dragon"));
        return bolts;
    }
}
