package com.hcimprogression.companion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import okhttp3.HttpUrl;

/** Bounded factual fields only: no downloaded code, HTML rendering or combat instructions. */
final class SlayerWikiGuide
{
    static final String HOST = "oldschool.runescape.wiki";
    static final class Variant
    {
        final String name, stats;
        Variant(String name, String stats) { this.name = name; this.stats = stats; }
        @Override public String toString() { return name; }
    }
    final String title, url, locations;
    final long revision;
    final List<Variant> variants;
    SlayerWikiGuide(String title, long revision, String locations, List<Variant> variants)
    {
        this.title = title; this.url = url(title); this.revision = revision;
        this.locations = locations; this.variants = Collections.unmodifiableList(variants);
    }
    static String validTitle(String title)
    {
        String value = title == null ? "" : title.trim().replace('_', ' ');
        if (value.isEmpty() || value.length() > 100 || value.contains(":") || value.contains("/")
            || value.contains("\\") || value.contains("@") || value.contains("<") || value.contains(">")
            || value.chars().anyMatch(Character::isISOControl)) throw new IllegalArgumentException("Enter a monster's Wiki page name, not a URL.");
        return value;
    }
    static String url(String title)
    {
        return new HttpUrl.Builder().scheme("https").host(HOST).addPathSegment("w").addPathSegment(validTitle(title).replace(' ', '_')).build().toString();
    }
    /** Only unambiguous common task labels; broad categories stay editable. */
    static String monsterTitle(String task)
    {
        String name = validTitle(task);
        switch (name.toLowerCase(java.util.Locale.ROOT))
        {
            case "bloodveld": return "Bloodveld";
            case "nechryael": return "Nechryael";
            case "cockatrice": return "Cockatrice";
            case "ankou": return "Ankou";
            case "dagannoth": return "Dagannoth";
            case "dust devils": return "Dust devil";
            case "smoke devils": return "Smoke devil";
            case "aberrant spectres": return "Aberrant spectre";
            case "abyssal demons": return "Abyssal demon";
            case "black demons": return "Black demon";
            case "greater demons": return "Greater demon";
            case "lesser demons": return "Lesser demon";
            case "blue dragons": return "Blue dragon";
            case "black dragons": return "Black dragon";
            case "green dragons": return "Green dragon";
            case "red dragons": return "Red dragon";
            case "bronze dragons": return "Bronze dragon";
            case "iron dragons": return "Iron dragon";
            case "steel dragons": return "Steel dragon";
            case "basilisks": return "Basilisk";
            case "gargoyles": return "Gargoyle";
            case "jellies": return "Jelly";
            case "hellhounds": return "Hellhound";
            case "dark beasts": return "Dark beast";
            case "fire giants": return "Fire giant";
            case "hill giants": return "Hill giant";
            case "moss giants": return "Moss giant";
            case "ice giants": return "Ice giant";
            case "wyrms": return "Wyrm";
            case "drakes": return "Drake";
            case "hydras": return "Hydra";
            default: return name;
        }
    }
    static SlayerWikiGuide parse(String title, long revision, String source)
    {
        if (source == null || source.length() > 500_000) throw new IllegalArgumentException("Wiki page is too large.");
        Matcher start = Pattern.compile("\\{\\{\\s*Infobox[ _]Monster\\b", Pattern.CASE_INSENSITIVE).matcher(source);
        if (!start.find()) throw new IllegalArgumentException("This page has no monster infobox. Enter an exact monster page, such as Dust devil or Greater Nechryael.");
        int at = start.end(), depth = 1, end = at;
        while (end + 1 < source.length() && depth > 0)
        {
            String pair = source.substring(end, end + 2);
            if ("{{".equals(pair)) { depth++; end += 2; }
            else if ("}}".equals(pair)) { depth--; end += 2; }
            else end++;
        }
        if (depth != 0) throw new IllegalArgumentException("Wiki infobox was incomplete.");
        Map<String, String> fields = new LinkedHashMap<>();
        Matcher params = Pattern.compile("(?m)^\\|\\s*([^=\\n]+?)\\s*=\\s*([^\\n]*)").matcher(source.substring(at, end - 2));
        while (params.find()) fields.put(params.group(1).trim().toLowerCase(java.util.Locale.ROOT), clean(params.group(2)));
        List<Variant> variants = new ArrayList<>();
        int count = 0;
        for (String key : fields.keySet()) if (key.matches("version[0-9]{1,2}")) count = Math.max(count, Integer.parseInt(key.substring(7)));
        count = Math.max(1, Math.min(32, count));
        String[][] stats = {{"combat","Combat"},{"hitpoints","HP"},{"att","Attack"},{"str","Strength"},{"def","Defence"},
            {"mage","Magic"},{"range","Ranged"},{"slaylvl","Slayer required"},{"max hit","Max hit"},
            {"attack style","Attack style"},{"dstab","Stab defence"},{"dslash","Slash defence"},{"dcrush","Crush defence"},
            {"dmagic","Magic defence"},{"drange","Ranged defence"},{"dlight","Light ranged defence"},
            {"dstandard","Standard ranged defence"},{"dheavy","Heavy ranged defence"},{"weakness","Weakness"},
            {"elementalweaknesstype","Elemental weakness"},{"elementalweaknesspercent","Elemental weakness bonus (%)"}};
        for (int i = 1; i <= count; i++)
        {
            StringBuilder text = new StringBuilder();
            for (String[] field : stats)
            {
                String value = fields.getOrDefault(field[0] + i, fields.get(field[0]));
                if (value != null && !value.isEmpty()) text.append(field[1]).append(": ").append(value).append('\n');
            }
            if (!fields.containsKey("weakness" + i) && !fields.containsKey("weakness")
                && !fields.containsKey("elementalweaknesstype" + i) && !fields.containsKey("elementalweaknesstype"))
                text.append("Weakness: not listed in the parsed infobox. Defence values are not a DPS ranking.\n");
            variants.add(new Variant(fields.getOrDefault("version" + i, fields.getOrDefault("name", title)), text.toString().trim()));
        }
        List<String> locations = new ArrayList<>();
        Matcher loc = Pattern.compile("\\{\\{LocLine\\b([^}]{0,25000})}}", Pattern.CASE_INSENSITIVE).matcher(source);
        while (loc.find() && locations.size() < 12)
        {
            Matcher location = Pattern.compile("(?m)^\\|\\s*(?:location|loc)\\s*=([^\\n]+)", Pattern.CASE_INSENSITIVE).matcher(loc.group(1));
            if (location.find()) { String value = clean(location.group(1)); if (!value.isEmpty() && !locations.contains(value)) locations.add(value); }
        }
        String listed = fields.get("location");
        if (locations.isEmpty() && listed != null && !listed.isEmpty()) locations.add(listed);
        for (int i = 1; i <= count && locations.size() < 12; i++)
        {
            String value = fields.get("location" + i);
            if (value != null && !value.isEmpty() && !locations.contains(value)) locations.add(value);
        }
        return new SlayerWikiGuide(validTitle(title), revision, locations.isEmpty()
            ? "No structured locations found. Open the Wiki's Locations section; variant names may identify specific areas."
            : String.join("\n", locations), variants);
    }
    static String clean(String value)
    {
        String text = value.replaceAll("(?is)<ref\\b[^>]*>.*?</ref>", "").replaceAll("<[^>]*>", "")
            .replaceAll("\\[\\[([^]\\n|]+)\\|([^]\\n]+)]]", "$2").replaceAll("\\[\\[([^]\\n]+)]]", "$1")
            .replaceAll("\\{\\{[^}]*}}", "[see Wiki]").replace("'''", "").replace("''", "")
            .replace("&nbsp;", " ").replace("&amp;", "&").replaceAll("\\s+", " ").trim();
        return text.length() > 350 ? text.substring(0, 350) + "… [see Wiki]" : text;
    }
}
