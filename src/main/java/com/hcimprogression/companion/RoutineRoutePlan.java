package com.hcimprogression.companion;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/** Plain user-authored checklist data; never game input, coordinates or code. */
final class RoutineRoutePlan
{
    String kind;
    String items;
    String stops;
    int revision;

    RoutineRoutePlan(String kind, String items, String stops, int revision)
    {
        this.kind = kind; this.items = items; this.stops = stops; this.revision = revision;
    }

    static List<String> lines(String value)
    {
        return Arrays.stream((value == null ? "" : value).split("\\r?\\n"))
            .map(line -> line.replaceAll("[<>\\p{Cntrl}]", " ").trim())
            .filter(line -> !line.isEmpty()).collect(Collectors.toList());
    }

    boolean valid()
    {
        return ("farm".equals(kind) || "birdhouse".equals(kind)) && revision >= 0
            && items != null && items.length() <= 2400 && stops != null && stops.length() <= 3200
            && lines(items).size() <= 32 && !lines(stops).isEmpty() && lines(stops).size() <= 24
            && lines(items).stream().allMatch(line -> line.length() <= 160)
            && lines(stops).stream().allMatch(line -> line.length() <= 160);
    }

    static RoutineRoutePlan defaults(String kind)
    {
        if ("farm".equals(kind)) return new RoutineRoutePlan(kind,
            "Spade\nSeed dibber unless bare-handed planting unlocked\nRake unless patches stay weed-free\n4 herb seeds of your choice\n4 ultracompost uses or charged bottomless compost bucket\nMagic secateurs if owned\nYour chosen teleports",
            "Falador herb patch | Travel from Falador\nCatherby herb patch | Travel from Catherby\nArdougne herb patch | Travel from Ardougne\nHosidius herb patch | Use your unlocked travel method", 0);
        return new RoutineRoutePlan("birdhouse",
            "Hammer\nChisel\n4 logs matching your Hunter and Crafting levels\n40 low-level hop seeds\n4 clockworks for the first run; reuse returned clockworks on repeats\nDigsite pendant if Fossil Island teleport unlocked",
            "Verdant Valley east | Fossil Island pendant then unlocked Mushtree\nVerdant Valley west | Walk to the other birdhouse\nMushroom Meadow north | Use unlocked Mushtree\nTar Swamp entrance | Walk south to the final birdhouse", 0);
    }

    static final class Response { boolean ok; RoutineRoutePlan plan; }
}
